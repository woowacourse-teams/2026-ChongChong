package withoutc.chongchong.study.entity;

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
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

@Entity
@Table(name = "studies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Study extends BaseEntity {

    private static final int MAX_STUDY_NAME_SIZE = 15;
    private static final int MAX_STUDY_DESCRIPTION_SIZE = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    public static Study create(
            String name,
            String description
    ) {
        return create(name, description, false);
    }

    public static Study create(
            String name,
            String description,
            boolean isPublic
    ) {
        return new Study(name, description, isPublic);
    }

    public void update(
            String name,
            String description,
            Boolean isPublic
    ) {
        if (name != null) {
            validateName(name);
            this.name = name;
        }
        if (description != null) {
            validateDescription(description);
            this.description = description;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

    private Study(
            String name,
            String description,
            boolean isPublic
    ) {
        validateName(name);
        this.name = name;
        if (description != null) {
            validateDescription(description);
            this.description = description;
        }
        this.isPublic = isPublic;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_STUDY_NAME_SIZE) {
            throw new StudyException(StudyErrorCode.INVALID_NAME);
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_STUDY_DESCRIPTION_SIZE) {
            throw new StudyException(StudyErrorCode.INVALID_DESCRIPTION);
        }
    }
}
