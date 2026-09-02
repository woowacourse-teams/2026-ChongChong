package withoutc.chongchong.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_PROFILE_IMAGE_URL_LENGTH = 2048;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "profile_image_url", length = MAX_PROFILE_IMAGE_URL_LENGTH)
    private String profileImageUrl;

    public static User create(
            String name,
            String profileImageUrl
    ) {
        return new User(name, profileImageUrl);
    }

    private User(
            String name,
            String profileImageUrl
    ) {
        validateName(name);
        validateProfileImageUrl(profileImageUrl);
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new UserException(UserErrorCode.INVALID_USER_NAME);
        }
    }

    private void validateProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl != null
                && (profileImageUrl.isBlank() || profileImageUrl.length() > MAX_PROFILE_IMAGE_URL_LENGTH)) {
            throw new UserException(UserErrorCode.INVALID_USER_PROFILE_IMAGE_URL);
        }
    }
}
