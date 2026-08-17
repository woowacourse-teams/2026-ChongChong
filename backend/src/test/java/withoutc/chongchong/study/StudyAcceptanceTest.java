package withoutc.chongchong.study;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.isEmptyString;

import io.restassured.http.ContentType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StudyAcceptanceTest {

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @LocalServerPort
    private int port;

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("스터디 생성 요청을 보내면 201을 반환하고 Study를 저장한다")
    void createStudyTest() {
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "자바 스터디",
                          "description": "매주 월요일에 진행한다."
                        }
                        """)
                .when()
                .post("/studies")
                .then()
                .statusCode(201)
                .body(isEmptyString());

        assertThat(studyRepository.findAll())
                .extracting(Study::getName, Study::getDescription)
                .contains(tuple("자바 스터디", "매주 월요일에 진행한다."));
    }

    @Test
    @DisplayName("스터디 초대 링크 조회 요청을 보내면 초대 링크를 반환한다")
    void getInviteLinkTest() throws IOException, InterruptedException {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        assertThat(study.getId()).isNotNull();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/studies/" + study.getId() + "/invite-link"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"inviteLink\":\"https://chongchong.app/join?token=");
    }
}
