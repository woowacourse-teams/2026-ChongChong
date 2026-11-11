package withoutc.chongchong.file.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadPurpose {
    STUDY_IMAGE("study-images/"),
    NOTICE_ATTACHMENT("notice-attachments/"),
    ASSIGNMENT_ATTACHMENT("assignment-attachments/"),
    SUBMISSION_ATTACHMENT("submission-attachments/");

    private final String prefix;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UploadPurpose from(String value) {
        return value == null ? null : valueOf(value);
    }
}
