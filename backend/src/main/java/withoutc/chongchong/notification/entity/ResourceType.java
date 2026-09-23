package withoutc.chongchong.notification.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ResourceType {
    NOTICE("공지"),
    ASSIGNMENT("과제"),
    ASSIGNMENT_SUBMISSION("제출물");

    public final String name;
}
