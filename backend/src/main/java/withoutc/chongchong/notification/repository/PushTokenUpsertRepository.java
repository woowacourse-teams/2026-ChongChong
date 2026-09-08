package withoutc.chongchong.notification.repository;

public interface PushTokenUpsertRepository {

    void upsert(
            Long userId,
            String installationId,
            String provider,
            String token,
            String platform
    );
}
