package withoutc.chongchong.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;

@ActiveProfiles("test")
@Transactional
@Import(JpaAuditingConfigTest.FixedClockConfig.class)
@SpringBootTest
class JpaAuditingConfigTest {

    private static final Instant NOW = Instant.parse("2026-08-27T10:00:00Z");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @Autowired
    private StudyRepository studyRepository;

    @Test
    @DisplayName("JPA Auditing 생성 시각은 애플리케이션 Clock을 사용한다")
    void createAuditingTimestampUsingApplicationClock() {
        Study study = studyRepository.saveAndFlush(Study.create("스터디", "설명"));

        assertThat(study.getCreatedAt())
                .isEqualTo(LocalDateTime.ofInstant(NOW, ZONE_ID));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZONE_ID);
        }
    }
}
