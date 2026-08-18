TRUNCATE TABLE notice, assignments, study_members, studies, users RESTART IDENTITY CASCADE;

INSERT INTO users (name, profile_image_url)
VALUES ('바니', 'https://example.com/profiles/bunny.png'),
       ('총총', 'https://example.com/profiles/chongchong.png'),
       ('코코', NULL);

INSERT INTO studies (name, description)
VALUES ('자바 스터디', '매주 월요일에 진행하는 자바 스터디'),
       ('스프링 스터디', 'Spring Boot 프로젝트를 함께 만드는 스터디');

INSERT INTO study_members (study_id, user_id, name, profile_image_url, role)
VALUES (1, 1, '바니', 'https://example.com/profiles/bunny.png', 'LEADER'),
       (1, 2, '총총', 'https://example.com/profiles/chongchong.png', 'MEMBER'),
       (2, 2, '총총', 'https://example.com/profiles/chongchong.png', 'LEADER'),
       (2, 3, '코코', NULL, 'MEMBER');

INSERT INTO assignments (study_id, title, content, submission_method, close_at)
VALUES (1, '1주차 자바 과제', '객체지향 생활 체조 원칙을 적용해 리팩터링한다.', 'GitHub PR', TIMESTAMP '2026-08-24 23:59:59'),
       (1, '2주차 자바 과제', '컬렉션 프레임워크를 활용한 기능을 구현한다.', 'GitHub PR', TIMESTAMP '2026-08-31 23:59:59'),
       (2, 'Spring Data JPA 과제', '연관관계 매핑과 조회 기능을 구현한다.', 'GitHub Repository', TIMESTAMP '2026-09-07 23:59:59');

INSERT INTO notice (study_id, member_id, title, content)
VALUES (1, 1, '첫 모임 안내', '첫 모임은 월요일 오후 8시에 진행합니다.'),
       (1, 2, '과제 제출 안내', '과제는 일요일 자정까지 제출해 주세요.'),
       (2, 3, '스터디 일정 변경', '이번 주만 화요일 오후 9시에 진행합니다.'),
       (2, 4, '자료 공유', '학습 자료를 스터디 문서에 정리했습니다.');
