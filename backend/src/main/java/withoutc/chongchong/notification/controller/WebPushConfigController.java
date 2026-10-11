package withoutc.chongchong.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.notification.controller.dto.WebPushConfigResponse;
import withoutc.chongchong.notification.service.WebPushConfigService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/web-push")
public class WebPushConfigController {

    private final WebPushConfigService webPushConfigService;

    @GetMapping("/config")
    public ResponseEntity<WebPushConfigResponse> getConfig() {
        WebPushConfigResponse response = webPushConfigService.getConfig();

        return ResponseEntity.ok(response);
    }

}
