# 0068. 스터디 리더 권한 검사를 접근 정책으로 옮기되 기존 오류 코드를 유지한다

- 날짜: 2026-11-26
- 관련 결정: [ADR-0026](0026-separate-application-service-and-access-policy-responsibilities.md)의 기능별 Access Policy

## 배경

ADR-0026은 여러 객체를 조합한 권한 판단을 기능별 `AccessPolicy`로 옮기고, 거부할 때는 `AuthException`과
`AuthErrorCode`를 던지도록 정했다. 후속 작업으로 "공지와 스터디 서비스의 권한 조건을 Policy로 이동한다"를 남겼고,
공지는 `NoticeAccessPolicy`로 옮겼다.

스터디 쪽에는 ADR-0026 이전 모양이 남아 있었다.

| 위치 | 검사 | 거부 응답 |
| --- | --- | --- |
| `StudyService.updateStudy`, `deleteStudy` | private `validateLeaderRole` | 403 `NOT_STUDY_LEADER` |
| `StudyMemberService.expel` | private `validateLeader` | 403 `NOT_STUDY_LEADER` |

ADR-0026을 그대로 따르면 거부 응답이 403 `ACCESS_DENIED`로 바뀐다. 상태 코드는 같지만 오류 코드가 달라진다.
`NOT_STUDY_LEADER`는 OpenAPI 명세(`errors.yaml`)와 인수 테스트에서 쓰이고, 프론트엔드도 이 코드를 기준으로 화면을
만든다.

## 결정

- `study/policy/StudyAccessPolicy`를 추가하고, 스터디 서비스의 리더 검사를 옮긴다.
  - `requireCanUpdateStudy`, `requireCanDeleteStudy`, `requireCanExpelMember`
  - 다른 Access Policy처럼 `@Component`이고 상태가 없으며, 서비스가 조회한 `StudyMember`만 받는다.
- 거부할 때는 `AuthException(ACCESS_DENIED)`가 아니라 기존과 같은
  `StudyMemberException(StudyMemberErrorCode.NOT_STUDY_LEADER)`를 던진다. API 응답(403, `NOT_STUDY_LEADER`)은
  바뀌지 않는다.
- 이 예외는 ADR-0026의 "Policy는 `AuthException`을 던진다"에 대한 스터디 Policy만의 예외다. 새로 추가하는
  스터디 권한 검사도 같은 Policy에 두고, 리더 전용 행위는 같은 오류 코드를 쓴다.
- 정원 초과, 리더 방출·탈퇴 금지처럼 권한이 아닌 업무 제약은 지금처럼 서비스·도메인 예외로 둔다.

## 선택 이유

이번 작업의 목적은 권한 판단의 위치를 ADR-0026에 맞추는 것이고, API 계약을 바꾸는 것이 아니다. 오류 코드를
바꾸면 명세와 프론트엔드를 함께 고쳐야 하는데, 그만큼 얻는 것이 없다. 위치만 옮기면 서비스는 유스케이스 순서만
조율하고, 리더 판단은 Policy 한 곳에서 단위 테스트할 수 있다는 ADR-0026의 효과는 그대로 얻는다.

## 검토한 대안

### ADR-0026대로 `AuthException(ACCESS_DENIED)`로 바꾼다

공지·과제·댓글 Policy와 예외 타입이 같아진다. 그러나 클라이언트가 받는 오류 코드가 `NOT_STUDY_LEADER`에서
`ACCESS_DENIED`로 바뀌어 명세·인수 테스트·프론트엔드를 함께 바꿔야 한다. 리팩터링 범위를 넘으므로 선택하지 않았다.

### 서비스 private 메서드에 그대로 둔다

변경이 없다. 그러나 ADR-0026이 정한 위치와 다르고, 리뷰 기준(권한 조건을 서비스 private 메서드에서 검사하는가)에
계속 걸린다. 스터디 권한이 늘어날 때 서비스마다 같은 검사가 흩어진다.

## 영향

### 긍정적 영향

- 스터디 서비스는 조회 → Policy 호출 → 도메인 행위 순서만 조율한다.
- 스터디 리더 판단을 Spring 없이 `StudyAccessPolicyTest`로 검증한다.
- API 응답이 바뀌지 않아 명세와 프론트엔드를 고칠 필요가 없다.

### 부정적 영향과 위험

- 리더가 아닐 때 공지·과제는 `ACCESS_DENIED`, 스터디는 `NOT_STUDY_LEADER`를 돌려주는 차이가 남는다.
- Policy가 인증·인가 예외가 아닌 도메인 예외를 던지므로, 예외 타입만 보고 권한 거부인지 판단할 수 없다.

## 미확정 사항

- 스터디 리더 거부 코드를 언젠가 `ACCESS_DENIED`로 합칠지. 합친다면 프론트엔드와 함께 정한다.

## 후속 작업

- `StudyAccessPolicy`, `StudyAccessPolicyTest` 추가
- `StudyService`, `StudyMemberService`의 private 리더 검사를 Policy 호출로 교체하고 서비스 테스트 정리
