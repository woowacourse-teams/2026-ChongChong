package withoutc.chongchong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.user.entity.User;

class UserProfileImageUrlResolverTest {

    private final FileUrlGenerator fileUrlGenerator = mock(FileUrlGenerator.class);
    private final UserProfileImageUrlResolver resolver = new UserProfileImageUrlResolver(fileUrlGenerator);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"https://example.com/social.png", "legacy-profile-image"})
    @DisplayName("업로드 파일이 없으면 기존 외부 URL이나 기본 이미지 null을 그대로 사용한다")
    void preservesExistingProfileWithoutSigning(String externalUrl) {
        User user = User.create("사용자", externalUrl);

        assertThat(resolver.resolve(user)).isEqualTo(externalUrl);
        verifyNoInteractions(fileUrlGenerator);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"https://example.com/social.png"})
    @DisplayName("업로드 파일의 긴 조회 URL을 우선 반환하고 기존 프로필 데이터에 저장하지 않는다")
    void prefersUploadedFileWithoutPersistingSignedUrl(String externalUrl) {
        User user = User.create("사용자", externalUrl);
        FileMetadata file = FileMetadata.create(user, "avatar.webp", "uploads/avatar.webp", "image/webp", 1024);
        file.completeUpload();
        user.changeProfileImage(file);
        String signedUrl = "https://example.com/avatar.webp?signature=" + "a".repeat(4096);
        when(fileUrlGenerator.generateImageUrl("uploads/avatar.webp", "image/webp")).thenReturn(signedUrl);

        assertThat(resolver.resolve(user)).isEqualTo(signedUrl);
        assertThat(user.getProfileImageUrl()).isEqualTo(externalUrl);
        assertThat(user.getProfileImageFile()).isSameAs(file);
        verify(fileUrlGenerator).generateImageUrl("uploads/avatar.webp", "image/webp");
    }
}
