package withoutc.chongchong.study;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.study.controller.dto.StudyCreateRequest;
import withoutc.chongchong.study.controller.dto.StudyCreateResponse;
import withoutc.chongchong.study.controller.dto.StudyInviteTokenRequest;
import withoutc.chongchong.study.controller.dto.StudyMemberJoinResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.service.StudyMemberService;
import withoutc.chongchong.study.service.StudyService;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:study-membership-concurrency-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"
})
@ActiveProfiles("test")
class StudyMembershipConcurrencyTest {

    @Autowired
    private StudyService studyService;

    @Autowired
    private StudyMemberService studyMemberService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyInviteTokenProvider studyInviteTokenProvider;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("동시에 두 명이 가입해도 스터디 정원을 초과하지 않는다")
    void doesNotExceedStudyMemberLimitWhenJoiningConcurrently() throws Exception {
        Study study = createStudyWithMembers(29);
        User firstUser = saveUser("첫 번째 참여자");
        User secondUser = saveUser("두 번째 참여자");
        String inviteToken = studyInviteTokenProvider.generate(study.getId());

        List<ConcurrentResult<StudyMemberJoinResponse>> results = runConcurrently(
                () -> studyMemberService.join(
                        firstUser.getId(), new StudyInviteTokenRequest(inviteToken)
                ),
                () -> studyMemberService.join(
                        secondUser.getId(), new StudyInviteTokenRequest(inviteToken)
                )
        );

        assertThat(results).hasSize(2);
        assertThat(results.stream().filter(ConcurrentResult::succeeded).toList()).hasSize(1);
        ConcurrentResult<StudyMemberJoinResponse> failure = results.stream()
                .filter(result -> !result.succeeded())
                .findFirst()
                .orElseThrow();
        assertThat(failure.failure()).isInstanceOf(StudyMemberException.class);
        assertThat(((StudyMemberException) failure.failure()).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        assertThat(studyMemberRepository.countByStudyId(study.getId())).isEqualTo(30);
    }

    @Test
    @DisplayName("동시에 스터디를 생성해도 사용자의 가입 스터디 수를 초과하지 않는다")
    void doesNotExceedUserStudyLimitWhenCreatingConcurrently() throws Exception {
        User user = saveUser("스터디 생성 사용자");
        createStudiesForUser(user, 49);

        List<ConcurrentResult<StudyCreateResponse>> results = runConcurrently(
                () -> studyService.createStudy(
                        user.getId(), new StudyCreateRequest("첫 번째 스터디", "설명")
                ),
                () -> studyService.createStudy(
                        user.getId(), new StudyCreateRequest("두 번째 스터디", "설명")
                )
        );

        assertThat(results).hasSize(2);
        assertThat(results.stream().filter(ConcurrentResult::succeeded).toList()).hasSize(1);
        ConcurrentResult<StudyCreateResponse> failure = results.stream()
                .filter(result -> !result.succeeded())
                .findFirst()
                .orElseThrow();
        assertThat(failure.failure()).isInstanceOf(StudyMemberException.class);
        assertThat(((StudyMemberException) failure.failure()).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.JOINED_STUDY_LIMIT_EXCEEDED);
        assertThat(studyMemberRepository.countByUserId(user.getId())).isEqualTo(50);
        assertThat(studyRepository.count()).isEqualTo(50);
    }

    @Test
    @DisplayName("동시에 서로 다른 스터디 초대를 수락해도 사용자의 가입 스터디 수를 초과하지 않는다")
    void doesNotExceedUserStudyLimitWhenJoiningConcurrently() throws Exception {
        User user = saveUser("초대 수락 사용자");
        createStudiesForUser(user, 49);
        Study firstStudy = createStudyWithLeader("첫 번째 초대 스터디");
        Study secondStudy = createStudyWithLeader("두 번째 초대 스터디");
        String firstInviteToken = studyInviteTokenProvider.generate(firstStudy.getId());
        String secondInviteToken = studyInviteTokenProvider.generate(secondStudy.getId());

        List<ConcurrentResult<StudyMemberJoinResponse>> results = runConcurrently(
                () -> studyMemberService.join(
                        user.getId(), new StudyInviteTokenRequest(firstInviteToken)
                ),
                () -> studyMemberService.join(
                        user.getId(), new StudyInviteTokenRequest(secondInviteToken)
                )
        );

        assertThat(results).hasSize(2);
        assertThat(results.stream().filter(ConcurrentResult::succeeded).toList()).hasSize(1);
        ConcurrentResult<StudyMemberJoinResponse> failure = results.stream()
                .filter(result -> !result.succeeded())
                .findFirst()
                .orElseThrow();
        assertThat(failure.failure()).isInstanceOf(StudyMemberException.class);
        assertThat(((StudyMemberException) failure.failure()).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.JOINED_STUDY_LIMIT_EXCEEDED);
        assertThat(studyMemberRepository.countByUserId(user.getId())).isEqualTo(50);
    }

    @Test
    @DisplayName("같은 사용자가 같은 스터디 초대를 동시에 수락해도 한 번만 가입한다")
    void joinsStudyOnlyOnceWhenJoiningConcurrently() throws Exception {
        Study study = createStudyWithLeader("중복 수락 스터디");
        User user = saveUser("중복 수락 사용자");
        String inviteToken = studyInviteTokenProvider.generate(study.getId());

        List<ConcurrentResult<StudyMemberJoinResponse>> results = runConcurrently(
                () -> studyMemberService.join(
                        user.getId(), new StudyInviteTokenRequest(inviteToken)
                ),
                () -> studyMemberService.join(
                        user.getId(), new StudyInviteTokenRequest(inviteToken)
                )
        );

        assertThat(results).hasSize(2);
        assertThat(results.stream().filter(ConcurrentResult::succeeded).toList()).hasSize(1);
        ConcurrentResult<StudyMemberJoinResponse> failure = results.stream()
                .filter(result -> !result.succeeded())
                .findFirst()
                .orElseThrow();
        assertThat(failure.failure()).isInstanceOf(StudyMemberException.class);
        assertThat(((StudyMemberException) failure.failure()).getErrorCode())
                .isEqualTo(StudyMemberErrorCode.ALREADY_JOINED_STUDY);
        assertThat(studyMemberRepository.countByStudyId(study.getId())).isEqualTo(2);
        assertThat(studyMemberRepository.countByUserId(user.getId())).isOne();
    }

    private Study createStudyWithMembers(int memberCount) {
        Study study = studyRepository.saveAndFlush(Study.create("정원 스터디", "설명"));
        for (int i = 0; i < memberCount; i++) {
            User user = saveUser("스터디 멤버 " + i);
            studyMemberRepository.saveAndFlush(
                    StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                            i == 0 ? StudyMemberRole.LEADER : StudyMemberRole.MEMBER)
            );
        }
        return study;
    }

    private void createStudiesForUser(User user, int studyCount) {
        for (int i = 0; i < studyCount; i++) {
            Study study = studyRepository.saveAndFlush(Study.create("기존 스터디 " + i, "설명"));
            studyMemberRepository.saveAndFlush(
                    StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                            StudyMemberRole.LEADER)
            );
        }
    }

    private Study createStudyWithLeader(String name) {
        Study study = studyRepository.saveAndFlush(Study.create(name, "설명"));
        User leader = saveUser(name + " 리더");
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );
        return study;
    }

    private User saveUser(String name) {
        return userRepository.saveAndFlush(User.create(name, null));
    }

    private <T> List<ConcurrentResult<T>> runConcurrently(
            Callable<T> firstTask,
            Callable<T> secondTask
    ) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ConcurrentResult<T>> firstFuture = executorService.submit(
                    () -> runAfterSignal(firstTask, ready, start)
            );
            Future<ConcurrentResult<T>> secondFuture = executorService.submit(
                    () -> runAfterSignal(secondTask, ready, start)
            );

            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();

            return List.of(
                    firstFuture.get(10, SECONDS),
                    secondFuture.get(10, SECONDS)
            );
        } finally {
            start.countDown();
            executorService.shutdownNow();
            assertThat(executorService.awaitTermination(10, SECONDS)).isTrue();
        }
    }

    private <T> ConcurrentResult<T> runAfterSignal(
            Callable<T> task,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            if (!start.await(5, SECONDS)) {
                throw new IllegalStateException("동시성 테스트 시작 신호를 기다리는 시간이 초과되었습니다.");
            }
            return ConcurrentResult.success(task.call());
        } catch (Throwable throwable) {
            return ConcurrentResult.failure(throwable);
        }
    }

    private record ConcurrentResult<T>(T value, Throwable failure) {

        private static <T> ConcurrentResult<T> success(T value) {
            return new ConcurrentResult<>(value, null);
        }

        private static <T> ConcurrentResult<T> failure(Throwable failure) {
            return new ConcurrentResult<>(null, failure);
        }

        private boolean succeeded() {
            return failure == null;
        }
    }
}
