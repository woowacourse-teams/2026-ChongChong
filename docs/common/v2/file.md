# 파일 업로드

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 현재 구현된 범위

로그인한 사용자는 `POST /api/files/presigned-url`로 S3에 직접 파일을 올릴 수 있는 URL을 발급받는다.
요청에는 원본 파일명, Content-Type, 파일 크기를 보낸다. 파일명과 Content-Type은 공백만 입력할 수 없고 각각 최대 255자다. 크기는 0 이상의 정수이며 현재 API에 최대 크기나 허용 MIME 유형 목록은 없다.

서버는 요청 사용자 소유의 파일 메타데이터를 업로드 대기(`PENDING`) 상태로 저장하고, `fileId`, `uploadUrl`, `expiresAt`을 반환한다.
클라이언트는 발급받은 URL에 같은 Content-Type으로 파일 본문을 PUT한다. URL 유효기간은 설정값을 사용하며 기본값은 10분이다.

## 아직 연결되지 않은 범위

URL 발급 성공은 실제 업로드 완료를 뜻하지 않는다. 요청의 크기는 클라이언트가 선언한 값이며 업로드 결과를 검증한 값이 아니다.
현재는 업로드 완료 확인 API, 공지·과제·제출물에 파일을 연결하는 계약, 웹의 파일 선택·첨부 화면이 없다. 따라서 사용자에게 제공하는 첨부 기능이 완료된 것으로 보지 않는다.

근거: [파일 컨트롤러](../../../backend/src/main/java/withoutc/chongchong/file/controller/FileController.java), [요청 검증](../../../backend/src/main/java/withoutc/chongchong/file/controller/dto/PresignedUrlCreateRequest.java), [파일 서비스](../../../backend/src/main/java/withoutc/chongchong/file/service/FileService.java), [API 명세](../../../backend/docs/openapi/paths/files/presigned-url.yaml).
