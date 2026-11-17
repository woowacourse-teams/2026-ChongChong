# 0061. 고아 파일을 표시 후 수거 방식의 배치로 정리한다

- 날짜: 2026-11-16
- 상태: 초안
- 관련 이슈: [#708](https://github.com/woowacourse-teams/2026-ChongChong/issues/708)
- 선행 결정: [ADR-0052](0052-use-presigned-urls-for-file-uploads.md), [ADR-0054](0054-separate-upload-verification-from-database-transactions.md)
- 관련 결정: [ADR-0024](0024-use-database-cascades-for-hard-delete.md), [ADR-0031](0031-run-postgresql-specific-tests-with-testcontainers.md)

## 배경

파일은 `file_metadata` 행과 S3 객체로 구성된다. 현재 S3 객체를 삭제하는 코드는 없고,
`FileUploadStatus.DELETE_PENDING`과 `FileMetadata.markForDeletion()`도 호출되는 곳이 없다.
따라서 아래 파일의 행과 객체가 계속 남는다.

| 종류 | 발생 경로 |
| --- | --- |
| 미완료 업로드 | URL 발급 후 완료 API를 호출하지 않음 (`PENDING` 유지) |
| 연결되지 않은 업로드 | 완료했지만 공지·과제·제출물·스터디·프로필 어디에도 연결하지 않음 |
| 연결이 끊긴 파일 | 이미지 교체, 첨부 제거, 상위 리소스 삭제 시 첨부 행 CASCADE 삭제 (ADR-0024) |

연결이 끊기는 경로 상당수는 DB의 `ON DELETE CASCADE`로 일어난다. 애플리케이션은 이 시점을
관찰할 수 없으므로 "연결을 끊는 코드에서 삭제를 함께 요청하는" 방식으로는 누락을 막을 수 없다.

DB와 S3는 하나의 트랜잭션으로 묶을 수 없다. [업로드 완료 처리 문서](../file-upload-completion.md)에서 정한 것처럼,
정리 작업은 파일 행 잠금 아래 상태와 연결 여부를 재확인하고 `DELETE_PENDING`으로 전환한 뒤에
S3 객체를 삭제해야 한다. S3를 먼저 삭제하면 완료 검증·첨부 연결과의 경합을 막을 수 없다.

## 결정

### 고아 파일을 정의한다

| 대상 | 조건 |
| --- | --- |
| 만료된 업로드 | `status = PENDING`, 참조 없음, `created_at < now - pendingGrace` |
| 미사용 파일 | `status = COMPLETED`, 참조 없음, `created_at < now - unreferencedGrace` |

- 현재 첨부 연결은 `COMPLETED`만 허용하지만, 과거 데이터에 대비해 `PENDING`에도 참조 검사를 적용한다.
- `pendingGrace` 기본값은 1시간이다. 시작 시 두 유예 시간이 모두 `aws.s3.url-validity + 시계 오차 여유(5분)`보다 긴지 검증하고,
  어기면 애플리케이션을 띄우지 않는다. 삭제된 키에 만료 전 업로드 URL로 다시 PUT하면 행 없는 객체가 생기기 때문이다.
- `unreferencedGrace` 기본값은 24시간이다. 업로드 후 폼을 작성하는 동안 아직 연결되지 않은 파일을 보호한다.
- "참조"는 `file_metadata(id)`를 가리키는 모든 외래 키다. 현재 `notice_attachments`, `assignment_attachments`,
  `submission_attachments`, `studies.image_file_id`, `users.profile_image_file_id`의 5곳이다.
  각 컬럼에 유니크 제약이 있으므로 `NOT EXISTS` 검사는 인덱스로 처리된다.

### 2단계로 처리한다

```text
[1단계: 표시]  (DB만 사용)
  후보 id 조회 (잠금 없음, id 커서, 배치 크기 100)
  → 트랜잭션: SELECT ... WHERE id IN (...) FOR UPDATE SKIP LOCKED
             별도 문장으로 상태·경과 시간·참조 여부 재확인
             통과한 행만 DELETE_PENDING 전환

[2단계: 삭제]
  트랜잭션 A: DELETE_PENDING 중 미선점 또는 선점 만료 행을 FOR UPDATE SKIP LOCKED로 선점
             delete_claimed_at = now, delete_attempts += 1
  트랜잭션 밖: S3 DeleteObjects (키별 결과 확인)
  트랜잭션 B: S3 삭제가 성공한 id 중 여전히 DELETE_PENDING인 행만 DELETE
```

#### 표시 단계가 경합에 안전한 이유

첨부 연결(`StudyImageService`, `NoticeAttachmentService`, `AssignmentAttachmentService`, `UserProfileImageService`)과
업로드 완료(`FileUploadTransactionService.complete`)는 모두 파일 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 상태를 검사한다.
표시 단계도 같은 행 잠금을 잡으므로 두 작업은 직렬화된다.

- 연결이 먼저 커밋되면 → 표시 단계의 재확인에서 참조가 보여 건너뛴다.
- 표시가 먼저 커밋되면 → 연결은 `COMPLETED`가 아니므로 `FILE_NOT_ATTACHABLE`, 완료는 `FILE_UPLOAD_NOT_PENDING`으로 거부된다.

재확인은 잠금 문장과 **별도의 문장**으로 실행한다. 잠금 문장의 `WHERE`에 참조 검사를 함께 넣으면,
문장 시작 시점의 스냅숏으로 첨부 테이블을 읽는다. 첨부 연결은 파일 행을 잠그기만 하고 수정하지 않으므로
PostgreSQL이 잠금 획득 후 행을 재평가하지 않고, 스냅숏과 잠금 획득 사이에 커밋된 연결을 놓친다.
READ COMMITTED에서는 문장마다 새 스냅숏을 쓰므로, 잠금을 얻은 뒤 실행한 재확인 문장에서는 그 연결이 보인다.

`SKIP LOCKED`를 쓰므로 사용자 요청이 잡고 있는 행은 기다리지 않고 다음 실행으로 미룬다.
사용자 요청 쪽의 `lock_timeout = 3s`에 정리 작업이 영향을 주지 않도록, 표시 트랜잭션은 배치 단위로 짧게 끝낸다.

`FileMetadata.markForDeletion()`은 `PENDING`에서도 전환할 수 있도록 변경한다.

#### 삭제 단계가 장애에 안전한 이유

| 장애 지점 | 결과 | 복구 |
| --- | --- | --- |
| 선점 후 S3 호출 전 중단 | 행은 `DELETE_PENDING` 유지 | 선점 만료(`deleteLease`, 기본 10분) 후 다시 선점 |
| S3 일부 키 실패 | 실패 키의 행은 유지, `delete_attempts` 증가 | 다음 실행에서 재시도 |
| S3 성공 후 행 삭제 전 중단 | 객체는 없고 행만 남음 | 재선점 시 S3 `DeleteObject`가 없는 키도 성공으로 응답하므로 행 삭제까지 완료 |
| 여러 인스턴스 동시 실행 | `SKIP LOCKED`와 선점 시각으로 같은 행을 나눠 가지지 않음 | 선점 만료가 겹쳐도 S3 삭제가 멱등이므로 중복 실행은 무해 |

- S3 호출 중에는 DB 연결과 행 잠금을 잡지 않는다 (ADR-0054와 같은 원칙).
- `delete_attempts >= maxAttempts`(기본 5)인 행은 더 이상 선점하지 않고 지표로 알린다.
  권한 오류(`AccessDenied`)처럼 재시도로 해결되지 않는 실패가 무한 반복되는 것을 막는다.
- 삭제할 키가 `aws.s3.upload-prefix`로 시작하지 않으면 S3 호출 없이 실패로 기록한다.

### 스키마를 변경한다

```sql
ALTER TABLE file_metadata ADD COLUMN delete_claimed_at TIMESTAMP;
ALTER TABLE file_metadata ADD COLUMN delete_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE file_metadata
    ADD CONSTRAINT ck_file_metadata_delete_attempts CHECK (delete_attempts >= 0);

CREATE INDEX idx_file_metadata_status_created
    ON file_metadata (status, created_at, id);
```

컬럼 추가는 기본값이 상수이므로 PostgreSQL 11 이상에서 테이블을 다시 쓰지 않는다.
일부 테스트가 H2(PostgreSQL 호환 모드)에서도 Flyway를 실행하므로 부분 인덱스 대신 일반 복합 인덱스를 쓴다.
이 인덱스 하나로 `PENDING` 만료 조회와 `DELETE_PENDING` 선점 조회를 함께 처리한다.
운영 데이터가 커지면 `CREATE INDEX CONCURRENTLY`로 분리한다.

### 실행과 설정

```yaml
file:
  cleanup:
    enabled: false        # 전체 스위치
    dry-run: true         # 표시 단계에서 후보를 집계·로그만 하고 상태를 바꾸지 않음
    fixed-delay: 10m
    initial-delay: 1m
    batch-size: 100      # S3 DeleteObjects 한도(1000) 이하
    max-batches-per-run: 20
    pending-grace: 1h
    unreferenced-grace: 24h
    delete-lease: 10m
    max-attempts: 5
```

알림 워커와 같이 `@Scheduled(fixedDelay)`로 실행하되, S3 호출을 포함하므로 리마인더가 쓰는 공용 스케줄러와 분리한
전용 스케줄러(`fileCleanupScheduler`)를 쓴다. 다중 인스턴스 조정은 `SKIP LOCKED`로 처리하므로
별도 분산 락은 도입하지 않는다. 한 번 실행할 때 처리하는 배치 수에 상한을 두어 최초 실행 시
쌓인 데이터를 여러 번에 나누어 처리한다.

### 관측

| 지표 | 종류 | 태그 |
| --- | --- | --- |
| `file.cleanup.marked` | Counter | `reason=pending_expired\|unreferenced` |
| `file.cleanup.deleted` | Counter | |
| `file.cleanup.delete.failed` | Counter | `error` |
| `file.cleanup.delete.pending` | Gauge | 재시도 가능한 `DELETE_PENDING` 행 수 |
| `file.cleanup.delete.exhausted` | Gauge | `delete_attempts >= maxAttempts` 행 수 |
| `file.cleanup.dry.run.candidates` | Gauge | 드라이런 마지막 실행의 후보 수 |
| `file.cleanup.last.success.timestamp` | Gauge | 마지막 성공 시각. 작업이 멈췄는지 알 수 있다 |

실행마다 후보·표시·건너뜀·삭제·실패 건수와 소요 시간을 구조화 로그로 남긴다 (ADR-0041).
행 삭제가 외래 키에 막히면 `file_cleanup_reference_violation`을 ERROR로 남긴다.

### 참조 목록이 어긋나지 않도록 테스트로 고정한다

파일을 참조하는 새 테이블이 생겼는데 정리 쿼리에 반영하지 않으면, 사용 중인 파일의 S3 객체가 삭제된다.
트랜잭션 B의 행 삭제는 FK 위반으로 실패하지만, 그때는 이미 객체가 지워진 뒤다.

이를 막기 위해 PostgreSQL 카탈로그(`pg_constraint`)에서 `file_metadata(id)`를 참조하는 외래 키를 모두 조회하고,
정리 작업이 검사하는 참조 목록과 일치하는지 단언하는 테스트를 둔다.
정리 쿼리는 `FileReferences` 목록에서 참조 조건을 조립한다. 첨부 연결 시 쓰는 `existsAnyUsageByFileId`는
기존 `FileMetaDataUsageRepositoryTest`가 참조별로 검증한다.

## 선택 이유

- 연결을 끊는 경로를 추적하지 않고 "지금 참조가 없는가"만 판단하므로 DB CASCADE로 끊긴 연결도 빠짐없이 처리한다.
- 기존 첨부 연결·업로드 완료 흐름이 이미 파일 행 잠금을 기준으로 직렬화되어 있어, 같은 잠금을 쓰는 것만으로 경합을 막는다. 사용자 요청 경로의 코드를 바꿀 필요가 없다.
- 상태를 DB에 먼저 확정하고 S3를 나중에 삭제하므로 어느 지점에서 중단되어도 "사용 중인 파일의 객체 삭제"는 일어나지 않고, 최악의 경우 다음 실행에서 이어서 처리된다.
- 알림 발송의 선점·복구 방식과 같은 형태여서 팀이 이미 아는 패턴으로 운영할 수 있다.

## 검토한 대안

### 연결을 끊는 서비스 코드에서 즉시 삭제

변경이 일어난 곳에서 바로 처리하므로 지연이 없다. 그러나 CASCADE 삭제는 애플리케이션이 관찰할 수 없고,
사용자 요청 안에서 S3 장애를 처리해야 하며, 연결 경로가 늘어날 때마다 삭제 호출을 빠뜨릴 위험이 있다.

### 첨부 테이블 DELETE 트리거로 정리 큐 적재

CASCADE까지 포착할 수 있다. 다만 업무 규칙이 DB 트리거에 숨어 코드 리뷰와 테스트에서 드러나지 않고,
업로드 후 연결하지 않은 파일과 미완료 업로드는 여전히 별도 탐색이 필요하다.

### S3 수명 주기 규칙만 사용

미완료 업로드는 객체 태그와 수명 주기 규칙으로 만료시킬 수 있다. 하지만 S3는 DB 참조 여부를 알 수 없어
연결이 끊긴 파일을 처리하지 못하고, `file_metadata` 행이 객체 없이 남는다.

## 영향

### 긍정적 영향

- 사용되지 않는 S3 객체와 메타데이터 행이 일정 시간 안에 정리된다.
- 정리 대기량, 실패, 재시도 소진을 지표로 확인할 수 있다.

### 부정적 영향과 위험

- 참조 판정이 틀리면 사용 중인 파일이 삭제된다. 카탈로그 테스트, 드라이런 배포, S3 버전 관리로 대비한다.
- 미사용 파일 탐색은 `COMPLETED` 행 전체를 id 커서로 훑는다. 파일 수가 커지면 실행 시간을 측정해 재검토한다.
- 연결을 끊은 파일을 나중에 다시 연결하는 흐름은 지원하지 않는다. 생성 후 24시간이 지난 파일은 연결이 끊기면 다음 실행에서 정리된다.

## 미확정 사항

- 표시와 삭제를 한 번의 실행에서 이어서 하므로, 미사용 파일은 유예 시간이 지난 뒤 다음 실행에서 바로 S3까지 삭제된다.
  표시와 삭제 사이에 별도 대기 시간을 둘지 결정이 필요하다.

- 운영 버킷에 버전 관리와 이전 버전 만료(예: 7일) 수명 주기 규칙을 적용할지. 잘못된 삭제를 복구할 수 있는 유일한 수단이다.
- 애플리케이션 IAM 역할에 업로드 접두사 범위의 `s3:DeleteObject` 권한을 부여할지.
- 행 없이 S3에만 존재하는 객체(수동 작업 등)를 S3 Inventory로 대사할지. 이번 범위에서는 제외한다.
- 연결이 끊긴 파일을 다시 연결하는 제품 요구가 있는지. 있다면 생성 시각 대신 고아로 처음 발견된 시각(`orphaned_at`)을 기준으로 유예한다.

## 후속 작업

1. 마이그레이션 `V34`: 선점 컬럼과 부분 인덱스 추가
2. `FileMetadata.markForDeletion()`의 `PENDING` 허용, `claimDeletion()` 추가
3. `FileCleanupTransactionService`, `S3FileDeleter`, `FileCleanupWorker`, 설정 프로퍼티와 시작 시 검증 구현
4. Testcontainers 테스트 (`FileCleanupPostgresTest`, `FileReferencesCatalogTest`)
   - 유예 시간 경계
   - 첨부 연결과 표시의 경합 (양쪽 순서 모두, 두 연결과 래치 사용)
   - 업로드 완료와 표시의 경합 (삭제 대기 파일의 완료 거부는 `FileMetadataTest`로 검증)
   - S3 실패 주입: 행 유지, 시도 횟수 증가, 재시도 성공
   - S3 성공 후 행 삭제 전 중단: 선점 만료 후 재실행으로 완료
   - 두 워커 동시 실행 시 같은 행을 중복 선점하지 않음
   - `file_metadata` 참조 외래 키 카탈로그 일치
   - 업로드 접두사 밖 키 거부
5. 배포: `dry-run`으로 후보 수 확인 → 표시 활성화 → 삭제 활성화
6. [업로드 완료 처리 문서](../file-upload-completion.md)의 정리 작업 문단을 이 ADR로 연결
