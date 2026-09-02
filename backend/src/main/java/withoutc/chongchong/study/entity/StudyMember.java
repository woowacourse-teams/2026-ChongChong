package withoutc.chongchong.study.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.user.entity.User;

@Entity
@Table(
        name = "study_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_study_members_study_user",
                columnNames = {"study_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMember extends BaseEntity {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_PROFILE_IMAGE_URL_LENGTH = 2048;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "profile_image_url", length = MAX_PROFILE_IMAGE_URL_LENGTH)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyMemberRole role;

    public static StudyMember create(
            Study study,
            User user,
            String name,
            String profileImageUrl,
            StudyMemberRole role
    ) {
        return new StudyMember(study, user, name, profileImageUrl, role);
    }

    private StudyMember(
            Study study,
            User user,
            String name,
            String profileImageUrl,
            StudyMemberRole role
    ) {
        validateRequiredValues(study, user, role);
        validateName(name);
        validateProfileImageUrl(profileImageUrl);
        this.study = study;
        this.user = user;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    public boolean isLeader() {
        return StudyMemberRole.LEADER.equals(role);
    }

    private void validateRequiredValues(Study study, User user, StudyMemberRole role) {
        if (study == null || user == null || role == null) {
            throw new StudyMemberException(StudyMemberErrorCode.INVALID_STUDY_MEMBER);
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new StudyMemberException(StudyMemberErrorCode.INVALID_STUDY_MEMBER_NAME);
        }
    }

    private void validateProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl != null
                && (profileImageUrl.isBlank() || profileImageUrl.length() > MAX_PROFILE_IMAGE_URL_LENGTH)) {
            throw new StudyMemberException(StudyMemberErrorCode.INVALID_STUDY_MEMBER_PROFILE_IMAGE_URL);
        }
    }
}
