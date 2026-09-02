# 0024. 하드 삭제 시 데이터베이스 외래 키 CASCADE로 종속 데이터를 삭제한다

- 날짜: 2026-09-02
- 관련 이슈: #241
- 대체 관계: [0011. 스터디 삭제 시 하위 데이터를 서비스에서 명시적으로 삭제한다](0011-explicitly-delete-study-dependencies.md), [0022. 스터디 멤버 제거 시 종속 데이터를 명시적으로 삭제한다](0022-explicitly-delete-study-member-dependencies.md)의 서비스 수동 삭제 방식

## 배경

스터디와 스터디 멤버는 여러 하위 데이터에서 외래 키로 참조된다.

- `StudyMember.study`
- `Notice.study`
- `Assignment.study`
- `Notification.study`
- `Notification.recipient`
- `NoticeRecipient.member`
- `AssignmentSubmission.member`
- `NoticeRecipient.notice`
- `AssignmentSubmission.assignment`
- `NoticeReminder.notice`
- `AssignmentReminder.assignment`

기존에는 `StudyService`와 `StudyMemberService`에서 하위 데이터를 외래 키 의존 순서에 따라 직접 삭제했다. 그러나 새로운 종속 엔티티가 추가될 때마다 서비스 삭제 로직과 순서를 함께 수정해야 하며, `Notification`처럼 삭제 대상에서 누락되면 외래 키 제약조건 위반으로 전체 트랜잭션이 실패할 수 있다.

또한 `Study` 엔티티에는 하위 엔티티를 가리키는 `@OneToMany` 연관관계가 없다. JPA Cascade를 적용하기 위해 모든 하위 엔티티와 양방향 연관관계를 추가하면 `Study` 엔티티가 모든 하위 데이터의 구조를 직접 알아야 한다.

제품 정책상 스터디와 스터디 멤버는 하드 삭제한다. 스터디 삭제 시 스터디에 속한 공지, 과제, 공지 읽음 현황, 과제 제출물, 알림과 리마인더도 함께 삭제한다. 멤버 탈퇴·방출 시에는 멤버에 종속된 알림, 공지 수신·읽음 현황, 과제 제출물을 삭제하지만 공지와 과제 원본은 유지한다.

공지와 과제는 특정 `StudyMember`의 생명주기에 종속되지 않는다. 따라서 `Notice`와 `Assignment`는 `StudyMember`를 작성자로 참조하지 않고 스터디의 데이터로 관리한다. 현재 제품 정책에서는 공지와 과제를 스터디 리더가 작성한 것으로 간주한다.

## 결정

하드 삭제 대상의 종속 데이터 삭제를 데이터베이스 외래 키의 `ON DELETE CASCADE`로 처리한다.

### 외래 키 CASCADE 범위

| 부모 데이터 | CASCADE 대상 |
|---|---|
| `studies` | `study_members`, `notices`, `assignments`, `notifications` |
| `study_members` | `notice_recipients`, `assignment_submissions`, `notifications` |
| `notices` | `notice_recipients`, `notice_reminders` |
| `assignments` | `assignment_submissions`, `assignment_reminders` |
| `users` | `study_members` |

`NoticeRecipient`와 `AssignmentSubmission`처럼 여러 외래 키를 가진 데이터도 부모 데이터 삭제 시 데이터베이스가 연쇄적으로 삭제한다.

### 애플리케이션 삭제 책임

`StudyService.deleteStudy`는 리더 권한을 검증한 뒤 `Study`만 삭제한다. 삭제 직전에 `EntityManager.clear()`를 호출해 인증 과정에서 조회된 `StudyMember`가 삭제된 `Study`를 참조한 상태로 flush되지 않도록 한다.

스터디 멤버 탈퇴·방출도 종속 데이터를 Repository에서 수동 삭제하지 않고, 최종적으로 `StudyMember`만 삭제하도록 구성한다. 종속 데이터 삭제는 데이터베이스 CASCADE에 맡긴다.

`Study`에 삭제 목적의 `@OneToMany` 연관관계를 추가하지 않는다. 자식 엔티티의 `@ManyToOne`에 `CascadeType.REMOVE`를 추가하지도 않는다. 자식에서 부모로 삭제가 전파되는 잘못된 방향을 방지하기 위해서다.

`Notice`와 `Assignment` 내부의 리마인더, 수신자, 제출물에 대한 JPA Cascade는 생성·수정·고아 객체 제거에 필요한 범위에서만 유지한다. 스터디와 스터디 멤버 삭제의 책임은 DB Cascade로 통일한다.

`Notice`와 `Assignment`에서는 `StudyMember`를 가리키는 `writer_id` 외래 키와 연관관계를 제거한다. 공지와 과제는 스터디의 생명주기를 따르며, 멤버 삭제만으로 원본이 삭제되지 않도록 한다.

### Flyway 적용

현재 스키마를 `V1__baseline.sql`로 기준선에 등록하고, `V2__add_study_delete_cascade.sql`에서 외래 키의 `ON DELETE CASCADE` 정책과 기존 `writer_id` 제거를 관리한다.

Hibernate는 스키마를 자동 변경하지 않고 `ddl-auto=validate`로 migration 결과를 검증한다. Flyway migration이 실제 대상 데이터베이스에 적용되기 전에는 CASCADE 삭제 동작을 보장하지 않는다.

## 선택 이유

데이터베이스 CASCADE는 JPA를 통하지 않는 SQL 삭제에도 동일하게 적용되므로 외래 키 무결성을 데이터베이스에서 일관되게 보장할 수 있다.

또한 `Study`에 모든 하위 엔티티의 양방향 연관관계를 추가하지 않아도 되므로 엔티티 구조를 단순하게 유지할 수 있다. 새로운 하위 데이터가 추가될 때도 해당 외래 키에 CASCADE를 설정하면 기존 서비스 삭제 로직을 수정하지 않아도 된다.

스터디와 멤버의 하드 삭제 정책에도 부합한다. 공지, 과제, 알림, 읽음 현황, 제출물은 삭제 대상이며, 별도의 Soft Delete 상태나 이력 조회 정책을 추가하지 않는다.

## 검토한 대안

### 서비스에서 하위 데이터를 명시적으로 삭제

삭제 범위와 순서가 애플리케이션 코드에 드러난다는 장점이 있다. 그러나 새로운 종속 데이터가 추가될 때마다 서비스 로직을 수정해야 하고, 삭제 대상이 누락되면 외래 키 제약조건 위반이 발생한다. 기존 방식이므로 대체한다.

### `Study`에 `@OneToMany(cascade = CascadeType.REMOVE)` 추가

JPA가 하위 엔티티 삭제를 관리할 수 있다. 하지만 `Study`에 여러 양방향 연관관계를 추가해야 하고, 하위 데이터가 늘어날수록 `Study` 엔티티의 책임이 커진다. 대량 삭제 시 자식 엔티티를 영속성 컨텍스트에 로드할 가능성도 있어 채택하지 않는다.

### 자식의 `@ManyToOne`에 `CascadeType.REMOVE` 추가

자식 삭제 시 부모 삭제로 전파될 수 있다. `Notification` 삭제가 `Study` 삭제로 이어지는 등 의도와 반대 방향의 데이터 삭제가 발생할 수 있으므로 사용하지 않는다.

### `StudyMember` Soft Delete

활동 이력을 보존할 수 있지만, 모든 조회·집계·접근 권한 쿼리에 활성 멤버 조건을 추가해야 한다. 재가입 시 기존 멤버십을 복구할지 새로운 멤버를 만들지도 결정해야 한다. 현재 제품 요구보다 범위가 크므로 채택하지 않는다.

## 영향

### 긍정적 영향

- 스터디와 멤버 삭제 시 고아 데이터와 외래 키 오류를 방지한다.
- 하위 데이터 삭제 순서를 서비스 코드에서 직접 관리하지 않아도 된다.
- `Study` 엔티티에 삭제 목적의 양방향 연관관계를 추가하지 않아도 된다.
- SQL, JPA 등 삭제 경로와 관계없이 외래 키 무결성이 보장된다.
- 멤버 삭제 시 공지와 과제 원본은 유지할 수 있다.
- 스터디 삭제 시 알림, 공지 읽음 현황, 과제 제출물까지 함께 삭제된다.

### 부정적 영향과 위험

- 삭제 범위가 Java 코드보다 데이터베이스 스키마에 숨겨진다.
- 개발·테스트·운영 데이터베이스에 동일한 CASCADE 설정이 적용되어야 한다.
- Flyway migration이 실제 데이터베이스에 적용되기 전에는 삭제 API가 정상 동작한다고 보장할 수 없다.
- DB Cascade로 삭제된 하위 엔티티에는 JPA `@PreRemove` 같은 엔티티 생명주기 이벤트가 실행되지 않는다.
- 하드 삭제된 멤버의 활동 이력은 복구할 수 없다.
- `EntityManager.clear()`는 영속성 컨텍스트의 모든 엔티티를 detach하므로 삭제 직전과 직후에 다른 변경 작업을 추가하지 않아야 한다.
- 현재 `users`를 참조하는 `auth_sessions`, `push_tokens`, `social_accounts`의 삭제 정책은 별도로 결정해야 한다.

## 미확정 사항

- 사용자 삭제 시 `auth_sessions`, `push_tokens`, `social_accounts`를 CASCADE로 삭제할지 여부
- 리더 변경 기능이 추가될 경우 공지·과제 작성자 관계를 어떻게 표현할지 여부

## 후속 작업

- Flyway `V2` migration을 대상 데이터베이스에 적용한다.
- 운영 반영 전 Flyway 연결 대상과 `baseline-on-migrate` 설정을 확인한다.
- `StudyMemberRemover`의 종속 데이터 수동 삭제 로직을 제거하고 `StudyMember` 삭제만 수행하도록 변경한다.
- `Notice`와 `Assignment`에서 `writer_id` 연관관계를 제거한다.
- 스터디 삭제 테스트에서 `Notification`, `NoticeRecipient`, `AssignmentSubmission`을 생성하고 삭제 후 잔여 행이 0개인지 검증한다.
- 멤버 탈퇴·방출 테스트에서 멤버 종속 데이터는 삭제되고 공지·과제 원본은 유지되는지 검증한다.
- 실제 PostgreSQL 환경에서 CASCADE 외래 키와 스터디 삭제 API를 검증한다.
- 이 ADR을 `docs/adr/README.md` 목록에 추가한다.
