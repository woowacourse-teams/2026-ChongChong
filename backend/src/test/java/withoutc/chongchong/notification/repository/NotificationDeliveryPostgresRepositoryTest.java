package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import withoutc.chongchong.notification.entity.NotificationDelivery;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationDeliveryPostgresRepositoryTest extends NotificationDeliveryRepositoryContractTest {

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
