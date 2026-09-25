package withoutc.chongchong.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceType {
    NOTICE("공지"),
    ASSIGNMENT("과제"),
    ASSIGNMENT_SUBMISSION("제출물");

    private final String name;
}
