package withoutc.chongchong.study.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class StudyUpdateRequest {

    @JsonProperty
    @Size(max = 15, message = "스터디 이름은 {max}자 이내여야 합니다.")
    private final String name;

    @JsonProperty
    @Size(max = 100, message = "스터디 설명은 {max}자 이내여야 합니다.")
    private final String description;

    @JsonProperty
    private final Boolean isPublic;

    @JsonProperty
    @Positive(message = "스터디 이미지 파일 ID는 양수여야 합니다.")
    private Long imageFileId;

    @JsonIgnore
    private boolean imageFileIdPresent;

    @JsonCreator
    public StudyUpdateRequest(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("isPublic") Boolean isPublic
    ) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
    }

    public StudyUpdateRequest(String name, String description, Boolean isPublic, Long imageFileId) {
        this(name, description, isPublic);
        setImageFileId(imageFileId);
    }

    @JsonSetter(value = "imageFileId", nulls = Nulls.SET)
    public void setImageFileId(Long imageFileId) {
        this.imageFileId = imageFileId;
        this.imageFileIdPresent = true;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    @JsonProperty("isPublic")
    public Boolean isPublic() {
        return isPublic;
    }

    public Long imageFileId() {
        return imageFileId;
    }

    public boolean hasImageFileId() {
        return imageFileIdPresent;
    }
}
