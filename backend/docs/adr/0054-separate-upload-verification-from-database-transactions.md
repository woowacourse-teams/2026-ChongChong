# 0054. 업로드 검증의 S3 호출과 DB 트랜잭션을 분리한다

- 날짜: 2026-10-21
- 관련 이슈: [#544](https://github.com/woowacourse-teams/2026-ChongChong/issues/544)
- 선행 결정: [ADR-0052](0052-use-presigned-urls-for-file-uploads.md)
- 관련 결정: [ADR-0015](0015-use-records-for-read-projections.md)

## 배경

업로드 완료 처리는 파일 메타데이터 조회, S3의 실제 파일 존재·크기 확인, DB의 COMPLETED 전환 순서로
진행한다. S3 조회는 네트워크를 사용하는 외부 API 호출이며 응답 지연이나 실패가 발생할 수 있다.
전체를 하나의 DB 트랜잭션으로 묶으면 최초 조회에서 확보한 DB 연결을 S3 응답 대기 중에도 유지할 수 있다.
파일 행의 잠금까지 먼저 잡으면 다른 요청의 상태 변경도 그동안 기다려야 한다.
DB 트랜잭션으로 S3까지 함께 커밋하거나 롤백할 수 있는 것은 아니다.

반대로 S3 조회 동안 트랜잭션과 잠금을 유지하지 않으면, 다른 요청이 먼저 완료하거나 파일을 삭제할 수 있다.
최초 조회의 PENDING은 조회 당시의 상태일 뿐, 완료 저장 시점에도 유효하다는 보장이 없다.
DB 연결 점유를 줄이면서 마지막 상태 변경의 정합성을 보장할 경계가 필요하다.

## 결정

### 조회와 완료 저장에 각각 짧은 트랜잭션을 적용한다

```text
FileService.completeUpload(userId, fileId)
  → 읽기 트랜잭션: 파일 정보 조회 및 소유권·상태 검증
  → 트랜잭션 밖: S3 HeadObject 조회 및 실제 크기 검증
  → 쓰기 트랜잭션: 파일 행 잠금, 재조회·재검증, COMPLETED 전환
```

FileService의 완료 메서드는 NOT_SUPPORTED로 클래스 수준의 트랜잭션 설정을 덮어쓴다.
별도 빈인 FileUploadTransactionService의 getOwnedFile에는 읽기 전용 트랜잭션을,
complete에는 쓰기 트랜잭션을 적용한다. 서로 다른 빈의 호출이 Spring 프록시를 통과하게 한다.

완료 저장은 PESSIMISTIC_WRITE로 파일 행을 잠근 뒤 소유권과 현재 상태를 다시 확인한다.
이미 COMPLETED이면 성공으로 처리하고, 삭제되었거나 DELETE_PENDING이면 완료 처리하지 않는다.
아직 PENDING이면 선언 크기도 다시 확인한 뒤 엔티티의 completeUpload로 상태를 변경한다.

### 최초 조회에는 엔티티 대신 값만 담은 projection을 사용한다

최초 조회는 objectKey, size, status, ownerId를 FileUploadSnapshotProjection record로 가져온다.
이 객체는 읽은 시점의 값을 전달할 뿐이며, 스스로 최신 값으로 갱신되지 않는다.

JPA는 영속성 컨텍스트에 이미 있는 엔티티를 재사용할 수 있다. 특히 OSIV가 활성화된 요청에서는
트랜잭션을 나누더라도 영속성 컨텍스트가 요청 끝까지 유지될 수 있다.
잠금 조회를 실행한다는 사실만으로 기존 관리 엔티티의 필드가 자동 갱신된다고 가정하지 않는다.

이 경로의 최초 조회는 FileMetadata 엔티티를 관리 대상으로 등록하지 않는다. 따라서 최종 잠금 조회가
DB에서 읽은 값으로 엔티티를 구성할 수 있다. 향후 이 경로 앞에 FileMetadata 엔티티 조회를 추가한다면
이 전제를 다시 검토하고 명시적 refresh 또는 영속성 컨텍스트 분리 필요성을 확인한다.

## 선택 이유

S3 응답 대기 시간만큼 DB 연결이나 파일 행 잠금을 불필요하게 유지하지 않는다.
대신 마지막 쓰기 구간에서 현재 상태를 다시 검증해, 외부 조회 도중의 변경을 덮어쓰지 않는다.
이는 파일 메타데이터의 상태 전환을 보호하는 방식이며 DB와 S3를 하나의 원자적 작업으로 만드는 방식은 아니다.

트랜잭션 경계는 별도 서비스의 어노테이션으로 표현한다. FileService에서는 처리 순서를 읽고,
FileUploadTransactionService에서는 잠금과 DB 작업 범위를 확인할 수 있다.
서비스 이름에 Transaction을 명시해 별도 업무 기능이 아니라 트랜잭션 경계를 위한 분리임을 드러낸다.

## 검토한 대안

### 전체 메서드를 하나의 트랜잭션으로 묶는다

코드가 짧지만 S3 응답을 기다리는 동안 DB 연결을 점유할 수 있다. 행 잠금을 먼저 획득하면 잠금 시간도
길어진다. 외부 조회를 DB 트랜잭션에 포함해 얻는 원자성 보장이 없으므로 선택하지 않는다.

### FileService에서 TransactionTemplate을 사용한다

읽기·S3 조회·쓰기 흐름과 트랜잭션 경계를 한 메서드에서 확인할 수 있고 보조 서비스가 필요 없다.
다만 업무 흐름에 트랜잭션 생성과 콜백 코드가 섞인다. 기존 서비스의 어노테이션 방식과 일치시키고
전체 처리 순서를 간결하게 유지하기 위해 별도 서비스를 선택한다. 두 방식의 성능 우열을 근거로 삼지는 않는다.

### 최초부터 엔티티를 조회하고 마지막에 다시 조회한다

projection 없이 구현할 수 있지만 동일 영속성 컨텍스트에서 오래된 엔티티를 재사용할 가능성을 다뤄야 한다.
명시적 refresh나 별도 영속성 컨텍스트로 해결할 수도 있다. 현재 필요한 값만 읽는 projection이
이 경로의 제약을 더 간단하게 표현하므로 선택한다.

## 영향

### 긍정적 영향

- S3 지연과 파일 행 잠금 시간을 분리한다.
- 중복 완료 요청과 조회 중 삭제·상태 변경을 마지막 트랜잭션에서 구분한다.
- projection은 데이터 전달, FilePolicy는 공통 검증, FileMetadata는 상태 전환을 담당한다.

### 부정적 영향과 위험

- 두 번의 DB 조회와 별도 트랜잭션 서비스가 필요하다.
- 최초 검증 이후 값이 바뀔 수 있으므로 최종 검증을 생략하면 안 된다.
- 잠금은 DB 행만 보호한다. S3 객체의 덮어쓰기·삭제는 별도 정책이 필요하다.
- NOT_SUPPORTED가 호출자의 기존 트랜잭션을 중단해도 그 트랜잭션이 보유한 자원까지 반환한다고 보장하지 않는다.
  현재 완료 API는 외부 트랜잭션이 없는 컨트롤러에서 호출하며, 다른 트랜잭션 서비스에서 재사용할 때는 재검토한다.

## 미확정 사항

- 실제 AWS S3 지연·실패 환경과 운영 DB의 부하 상황은 별도 검증이 필요하다.
- 향후 고아 정리 작업은 동일한 행 잠금 아래 상태·첨부 여부를 재확인하고 삭제 대상으로 전환한 뒤
  S3를 삭제하도록 완료 처리와 계약을 맞춰야 한다.

## 후속 작업

- 파일 API 테스트에서 확인한 S3 호출 중 트랜잭션 비활성, 동시 완료, 조회 중 삭제·상태 변경 사례를 유지한다.
  현재 검증은 로컬 HTTP 서버와 PostgreSQL 컨테이너를 사용하며 S3 응답은 테스트용으로 대체한다.
- 실제 환경의 잠금과 S3 접근 권한·조건부 업로드는 배포 전후 검증 항목으로 남긴다.

## 구현 및 검증 근거

완료 시 잠금 조회는 동일 연결에서 `SET LOCAL lock_timeout = '3s'`를 적용한다.
JPA 잠금 힌트에 의존하지 않고 PostgreSQL이 개별 잠금 대기를 제한하도록 한다.
설정은 현재 트랜잭션에만 적용되며, 잠금 경합 예외는 쓰기 트랜잭션 롤백 후
FileService에서 `503 FILE_UPLOAD_BUSY`로 변환한다. 자동 재시도는 하지 않는다.
HTTP 테스트로 잠금 대기 시간 초과, PENDING 유지와 잠금 해제 후 재요청 성공을 확인한다.

- [전체 완료 흐름](../../src/main/java/withoutc/chongchong/file/service/FileService.java)
- [트랜잭션 경계와 최종 재검증](../../src/main/java/withoutc/chongchong/file/service/FileUploadTransactionService.java)
- [projection 및 잠금 조회](../../src/main/java/withoutc/chongchong/file/repository/FileMetaDataRepository.java)
- [HTTP·동시 요청 테스트](../../src/test/java/withoutc/chongchong/file/FileApiTest.java)
- [업로드 완료 운영 고려사항](../file-upload-completion.md)
