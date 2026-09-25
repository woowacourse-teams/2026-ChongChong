package withoutc.chongchong.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import withoutc.chongchong.notification.config.WebPushVapidProperties;
import withoutc.chongchong.notification.controller.dto.WebPushConfigResponse;

@Service
@RequiredArgsConstructor
public class WebPushConfigService {

    private final WebPushVapidProperties vapidProperties;

    public WebPushConfigResponse getConfig() {
        return new WebPushConfigResponse(vapidProperties.publicKey());
    }
}
