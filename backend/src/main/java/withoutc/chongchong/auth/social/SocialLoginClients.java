package withoutc.chongchong.auth.social;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;

@Component
public class SocialLoginClients {

    private final Map<SocialProvider, SocialLoginClient> clients;

    public SocialLoginClients(List<SocialLoginClient> clients) {
        this.clients = createClientMap(clients);
    }

    public SocialUserInfo authenticate(SocialLoginCommand command) {
        validateCommand(command);
        SocialLoginClient client = findClient(command.provider());
        SocialUserInfo socialUserInfo = client.authenticate(command);
        validateAuthenticatedUser(command.provider(), socialUserInfo);
        return socialUserInfo;
    }

    private Map<SocialProvider, SocialLoginClient> createClientMap(List<SocialLoginClient> clients) {
        if (clients == null) {
            throw new IllegalArgumentException("소셜 로그인 Client 목록은 필수입니다.");
        }

        Map<SocialProvider, SocialLoginClient> clientMap = new EnumMap<>(SocialProvider.class);
        for (SocialLoginClient client : clients) {
            addClient(clientMap, client);
        }
        return Collections.unmodifiableMap(clientMap);
    }

    private void addClient(
            Map<SocialProvider, SocialLoginClient> clientMap,
            SocialLoginClient client
    ) {
        if (client == null || client.provider() == null) {
            throw new IllegalArgumentException("소셜 로그인 Client와 제공자는 필수입니다.");
        }
        if (clientMap.putIfAbsent(client.provider(), client) != null) {
            throw new IllegalStateException("같은 제공자의 소셜 로그인 Client를 중복 등록할 수 없습니다.");
        }
    }

    private void validateCommand(SocialLoginCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("소셜 로그인 요청은 필수입니다.");
        }
    }

    private SocialLoginClient findClient(SocialProvider provider) {
        SocialLoginClient client = clients.get(provider);
        if (client == null) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
        }
        return client;
    }

    private void validateAuthenticatedUser(
            SocialProvider requestedProvider,
            SocialUserInfo socialUserInfo
    ) {
        if (socialUserInfo == null || socialUserInfo.provider() != requestedProvider) {
            throw new IllegalStateException("소셜 로그인 Client가 요청한 제공자와 다른 결과를 반환했습니다.");
        }
    }
}
