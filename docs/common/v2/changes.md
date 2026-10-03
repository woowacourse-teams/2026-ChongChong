# v2 요구사항과 현재 구현

[← v2 개요](README.md)

> 2026-09-30, `b3e3e51`의 backend·frontend 소스를 기준으로 갱신했다.
> Figma 요구사항은 기존 2026-09-14 기록을 유지했다. 아래의 연결은 코드 확인이며 운영 환경 검증이나 배포 완료를 뜻하지 않는다.
> 제출물 공개는 현재 작업 브랜치의 서버 변경이다.

## 웹과 서버의 구현 현황

| 기능 | 현재 구현 | 남은 작업 |
| --- | --- | --- |
| [로그인](login.md) | 카카오 로그인과 인증 갱신 | Google·Apple 실제 연동, 네이티브 앱 권한 흐름 |
| [마이페이지](account.md) | 이름·프로필 조회, 정책·지원 링크 | 계정 이름 수정 API·화면 |
| [로그아웃](account.md) | 메뉴에서 현재 브라우저 푸시 구독 해제 후 로그아웃 | 네이티브 앱 세션·권한 흐름은 별도 확인 |
| [회원탈퇴](account.md) | API·확인 화면 연결, 리더 탈퇴 거부, 사용자와 종속 데이터 삭제 | 리더 양도 기능과 안내 연결 |
| [스터디별 프로필](settings.md) | 가입 시 이름 저장 | 소속별 이름 수정 API·화면 |
| [스터디 정보 수정](settings.md) | 리더 수정 API | 웹 수정 화면 |
| [리더 양도](member.md) | 구현 없음 | 권한 이전·동시성·활동 대상 승계 정책과 API·화면 |
| [공지·과제 이미지](notice.md) | 도메인 첨부 연결 없음 | 업로드·소유권·연결·조회·제거·정리 |
| [제출 파일](assignment.md) | 내용·링크 제출 | 파일 첨부와 공개 범위에 따른 접근 제어 |
| [리더 과제 참여](assignment.md) | 대상 선택·리더 내 제출·수정, 서버 집계·리마인드 반영 | 리더 양도 시 기존 제출 대상 처리 |
| [제출물 공개](assignment.md) | 서버 공개 설정과 제출 완료 목록·상세 조회 권한 | 웹 선택·요청·응답 처리와 일반 멤버 공개 화면 |
| [가입 전 글 열람](roles.md) | 목록·상세 열람과 NOT_ASSIGNED 응답·화면 처리 | 할당과 조회 구분을 향후 화면에도 유지 |
| [리마인드](notification.md) | 예약 저장·대상 선정·알림 생성·발송 | 웹 날짜·시간 예약 입력 |
| [푸시 수신](notification.md) | Web Push 구독 등록·비활성화, 브라우저 권한·서비스 워커, 발송 워커·재시도 | 실제 운영 수신 검증, 네이티브 앱·계정 전체 수신 설정 |
| [알림 목록](notification.md) | 사용자별 목록·읽음 API, 웹 목록·상태 점·관련 화면 이동 | 보관 기간 정책 |
| [과제 제출 알림](notification.md) | 최초 제출 시 본인을 제외한 리더에게 생성 | 공지 읽음 알림 확대는 별도 검토 |
| [공지·과제 상세](assignment.md) | 리더 요약·상세 탭, 읽음 진행률·완료 표시, 내 제출·편집 | 첨부와 일반 멤버 공개 제출물 화면 |
| [기존 운영 기능](member.md) | 스터디·공지·과제 CRUD, 방출·탈퇴 | 새 디자인의 설정 진입 구조와 연결 대조 |

## 현재 계약 불일치

- **과제 생성:** 서버는 `submissionVisibility`를 필수로 요구하지만 웹 폼·요청 타입에는 없다. 현재 브랜치의 생성 API와 웹 요청이 맞지 않는다.
- **공개 과제 현황:** 디자인은 일반 멤버에게 완료·미완료 현황을 보여주지만 서버 현황 API는 리더 전용이다. 공개 범위는 제출 완료 목록·상세만 확장한다.
- **제출물 상세:** 서버의 미제출 `createdAt`과 선택 `content`는 null일 수 있지만 웹 상세 응답 검증은 문자열을 요구한다.
- 공지·과제 제목 100자, 리더 참여 선택, `NOT_ASSIGNED` 상태, 로그아웃·알림함·Web Push는 최신 웹에 반영되어 있으므로 신규 구현 항목으로 두지 않는다.

## 추가 결정이 필요한 항목

- 첨부의 형식·크기·개수, 허용 제출 형식 선택 UI.
- 리더 양도 후 역할·활동 대상 승계와 방출 후 재가입 제한 여부.
- 공지 읽음 버튼 문구와 현재 자동 읽음 동작의 디자인 정합성.
- 알림 보관 기간, 공지 읽음 알림 확대, 계정 전체 수신 설정.
- 마감 후 제출은 현재 허용한다. 차단 정책을 도입하려면 별도 결정한다.

## 구현 근거

- [과제 입력·요청](../../../frontend/src/features/assignment/components/AssignmentForm.tsx), [타입](../../../frontend/src/features/assignment/types.ts), [응답 검증](../../../frontend/src/features/assignment/responseSchemas.ts), [서버 생성 계약](../../../backend/src/main/java/withoutc/chongchong/assignment/controller/dto/AssignmentCreateRequest.java).
- [과제 권한](../../../backend/src/main/java/withoutc/chongchong/assignment/policy/AssignmentAccessPolicy.java), [과제 서비스](../../../backend/src/main/java/withoutc/chongchong/assignment/service/AssignmentService.java), [공지 서비스](../../../backend/src/main/java/withoutc/chongchong/notice/service/NoticeService.java).
- [마이페이지](../../../frontend/src/features/mypage/pages/MyPage.tsx), [사용자 서비스](../../../backend/src/main/java/withoutc/chongchong/user/service/UserService.java).
- [알림 서비스](../../../backend/src/main/java/withoutc/chongchong/notification/service/NotificationService.java), [웹 푸시](../../../frontend/src/features/notification/push.ts), [알림함](../../../frontend/src/features/notification/pages/NotificationListPage.tsx).

## 2026-10-03 과제 제출 변경 반영

- 백엔드: 과제 목록·내 제출 조회에 `LATE_SUBMITTED`, `MISSING`을 추가하고 제출물 목록에 `submissionStatus`를 제공한다. 지각 제출도 완료 집계에 포함한다.
- 확정 정책: 과제 조회의 5개 제출 상태 구분과 마감 후 제출 허용은 사용자 확인에 따라 Figma에도 반영되었다. 프론트엔드는 해당 타입·표시를 연동하며 실제 구현·배포 완료 여부는 별도 검증 대상이다.
- 알림: 현재 구현은 최초 제출 시 제출자를 제외한 리더에게 알림을 생성하고, 재제출·수정 시에는 추가하지 않는다. 리더 제출 시 수신 대상 정책은 TODO가 남아 있다.
- 검증: 관련 테스트 155개 통과. 명세 보드의 관련 조회 API 3개는 완료로 표시한다. 배포 여부를 뜻하지 않는다.

상태 경계와 집계 기준은 [과제의 백엔드 구현 반영](assignment.md#2026-10-03-백엔드-구현-반영)을 따른다.
