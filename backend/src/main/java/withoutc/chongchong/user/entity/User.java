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

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "profile_image_url", length = 2048)
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
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }
}
