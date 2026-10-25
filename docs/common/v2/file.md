# 파일 업로드와 다운로드

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 현재 구현된 범위

로그인한 사용자는 `POST /api/files/presigned-url`로 S3에 직접 파일을 올릴 수 있는 URL을 발급받는다.
요청에는 원본 파일명, Content-Type, 파일 크기를 보낸다. 파일명과 Content-Type은 공백만 입력할 수 없고 각각 최대 255자다. 크기는 0 이상의 정수이며 최대 크기는 기본 5MiB이고 설정으로 변경할 수 있다. 첨부 가능한 MIME 유형은 연결할 게시물의 정책에서 검증한다.

서버는 요청 사용자 소유의 파일 메타데이터를 업로드 대기(`PENDING`) 상태로 저장하고, `fileId`, `uploadUrl`, `expiresAt`을 반환한다.
클라이언트는 발급받은 URL에 같은 Content-Type과 `If-None-Match: *` 헤더로 파일 본문을 PUT한다. URL 유효기간은 설정값을 사용하며 기본값은 10분이다.

## 업로드 완료와 첨부 연결

URL 발급 성공은 실제 업로드 완료를 뜻하지 않는다. 요청의 크기는 클라이언트가 선언한 값이며 업로드 결과를 검증한 값이 아니다.
업로드 후 완료 API를 호출하면 서버가 소유권과 파일 상태, S3 객체의 존재 여부·실제 크기·요청 크기 일치 여부를 검증하고 `COMPLETED`로 전환한다. 완료된 파일 ID를 공지·과제·제출물 요청의 `attachmentFileIds`에 전달해 연결한다.

## 과제·제출물 다운로드 (#567)

2026-10-23 작업 브랜치에서 과제 상세·내 제출·제출물 상세의 첨부 응답에 `downloadUrl`을 추가했다. 배포 및 웹 다운로드 연결 완료를 의미하지 않는다.
기존 상세 조회 권한을 통과하면 모든 첨부파일에 다운로드용 S3 서명 URL을 반환한다. 클라이언트는 이 URL로 S3에서 직접 내려받는다. 원본 파일명은 UTF-8로 지정하고, 제어 문자와 경로 구분자는 밑줄로 바꾼다.
JPEG·PNG·WebP는 미리보기용 `imageUrl`도 반환하며, 일반 문서는 `imageUrl`이 null이다. 다운로드 URL은 `Content-Disposition: attachment`와 `application/octet-stream` 응답을 지정한다.
URL은 `aws.s3.url-validity` 동안 유효하며 만료되면 상세를 다시 조회한다. 발급된 URL을 가진 사람은 만료 전까지 사용할 수 있으므로 공개 캐시에 저장하지 않는다. 권한 변경이 이미 발급된 URL을 즉시 무효화하지는 않는다.
이번 작업은 백엔드와 명세서에 한정하며 웹 다운로드 동작과 실제 S3 다운로드는 별도 검증이 필요하다.

다운로드 근거: [URL 생성기](../../../backend/src/main/java/withoutc/chongchong/file/service/FileUrlGenerator.java), [첨부 API 스키마](../../../backend/docs/openapi/components/schemas/assignment-attachments.yaml), [첨부 계약](../../../backend/docs/assignment-attachments.md).

근거: [파일 컨트롤러](../../../backend/src/main/java/withoutc/chongchong/file/controller/FileController.java), [요청 검증](../../../backend/src/main/java/withoutc/chongchong/file/controller/dto/PresignedUrlCreateRequest.java), [파일 서비스](../../../backend/src/main/java/withoutc/chongchong/file/service/FileService.java), [API 명세](../../../backend/docs/openapi/paths/files/presigned-url.yaml).
