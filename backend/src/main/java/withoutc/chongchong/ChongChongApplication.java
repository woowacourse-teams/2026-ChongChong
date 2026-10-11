package withoutc.chongchong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChongChongApplication {

    static void main(String[] args) {
        SpringApplication.run(ChongChongApplication.class, args);
    }

}
