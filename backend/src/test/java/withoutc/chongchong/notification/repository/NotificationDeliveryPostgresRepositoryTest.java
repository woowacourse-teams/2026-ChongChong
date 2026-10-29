package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.DeliveryStatus;
import withoutc.chongchong.notification.worker.DeliveryClaimService;
import withoutc.chongchong.support.TestDatabaseCleaner;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationDeliveryPostgresRepositoryTest extends NotificationDeliveryRepositoryContractTest {

    @Autowired
    private DeliveryClaimService deliveryClaimService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("두 워커는 잠금 중과 커밋 후 모두 같은 발송 작업을 중복 선점하지 않는다")
    void concurrentWorkersClaimDeliveryOnlyOnce() throws Exception {
        databaseCleaner.clean();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Long deliveryId = transaction.execute(status -> saveDelivery(saveFixture()).getId());
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CompletableFuture<Void> firstClaimed = new CompletableFuture<>();
        var executor = Executors.newSingleThreadExecutor();
        try {
            var firstWorker = executor.submit(() -> {
                try {
                    return transaction.execute(status -> {
                        var claimed = deliveryClaimService.claimBatch(1);
                        assertThat(claimed).hasSize(1);
                        firstClaimed.complete(null);
                        try {
                            if (!releaseFirst.await(10, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("첫 워커 해제 대기 시간 초과");
                            }
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("첫 워커 대기 중 인터럽트", exception);
                        }
                        return claimed.getFirst().id();
                    });
                } catch (RuntimeException | Error exception) {
                    firstClaimed.completeExceptionally(exception);
                    throw exception;
                }
            });
            firstClaimed.get(10, TimeUnit.SECONDS);
            assertThat(deliveryClaimService.claimBatch(1)).isEmpty();
            releaseFirst.countDown();
            assertThat(firstWorker.get(10, TimeUnit.SECONDS)).isEqualTo(deliveryId);
            assertThat(deliveryClaimService.claimBatch(1)).isEmpty();
            NotificationDelivery delivery = notificationDeliveryRepository.findById(deliveryId).orElseThrow();
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PROCESSING);
            assertThat(delivery.getAttemptCount()).isOne();
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            databaseCleaner.clean();
        }
    }

    @Test
    @DisplayName("만료된 PROCESSING 발송 기록만 조회한다")
    void findStuckProcessingDeliveries() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 26, 10, 0);
        DeliveryFixture staleFixture = saveFixture();
        DeliveryFixture freshFixture = saveFixture();

        NotificationDelivery staleDelivery = saveDelivery(staleFixture);
        staleDelivery.claim(now.minusMinutes(6));
        notificationDeliveryRepository.saveAndFlush(staleDelivery);

        NotificationDelivery freshDelivery = saveDelivery(freshFixture);
        freshDelivery.claim(now.minusMinutes(4));
        notificationDeliveryRepository.saveAndFlush(freshDelivery);

        assertThat(notificationDeliveryRepository.findStuckProcessingForUpdate(
                now.minusMinutes(5), 100
        )).extracting(NotificationDelivery::getId)
                .containsExactly(staleDelivery.getId());
    }

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    )
            .withDatabaseName("chongchong")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
