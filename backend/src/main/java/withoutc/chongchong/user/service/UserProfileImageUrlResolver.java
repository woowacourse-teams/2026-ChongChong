package withoutc.chongchong.user.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberProfileImageProjection;
import withoutc.chongchong.user.entity.User;

@Component
@RequiredArgsConstructor
public class UserProfileImageUrlResolver {

    private final FileUrlGenerator fileUrlGenerator;
    private final StudyMemberRepository studyMemberRepository;

    public String resolve(User user) {
        FileMetadata file = user.getProfileImageFile();
        if (file == null) {
            return user.getProfileImageUrl();
        }
        return fileUrlGenerator.generateImageUrl(file.getObjectKey(), file.getContentType());
    }

    public Map<Long, String> resolveForMembers(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> imageUrls = new HashMap<>();
        List<StudyMemberProfileImageProjection> images = studyMemberRepository.findProfileImagesByMemberIds(memberIds);
        for (StudyMemberProfileImageProjection image : images) {
            imageUrls.put(image.memberId(), resolve(image));
        }
        return imageUrls;
    }

    private String resolve(StudyMemberProfileImageProjection image) {
        if (image.objectKey() == null) {
            return image.externalUrl();
        }
        return fileUrlGenerator.generateImageUrl(image.objectKey(), image.contentType());
    }
}
