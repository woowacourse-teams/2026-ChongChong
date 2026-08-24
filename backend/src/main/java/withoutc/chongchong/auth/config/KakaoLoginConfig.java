package withoutc.chongchong.auth.config;

import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import withoutc.chongchong.auth.social.kakao.KakaoTokenClient;

@Configuration
@EnableConfigurationProperties(KakaoLoginProperties.class)
public class KakaoLoginConfig {

    @Bean
    RestClient kakaoRestClient(KakaoLoginProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    KakaoTokenClient kakaoTokenClient(
            @Qualifier("kakaoRestClient") RestClient kakaoRestClient,
            KakaoLoginProperties properties
    ) {
        return new KakaoTokenClient(kakaoRestClient, properties);
    }
}
