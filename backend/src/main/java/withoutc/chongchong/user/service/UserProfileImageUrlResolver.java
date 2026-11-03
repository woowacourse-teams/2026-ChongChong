package withoutc.chongchong.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.user.entity.User;

@Component
@RequiredArgsConstructor
public class UserProfileImageUrlResolver {

    private final FileUrlGenerator fileUrlGenerator;

    public String resolve(User user) {
        FileMetadata file = user.getProfileImageFile();
        if (file == null) {
            return user.getProfileImageUrl();
        }
        return fileUrlGenerator.generateImageUrl(file.getObjectKey(), file.getContentType());
    }
}
