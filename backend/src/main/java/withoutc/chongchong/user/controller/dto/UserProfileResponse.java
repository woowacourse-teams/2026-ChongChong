package withoutc.chongchong.user.controller.dto;

import withoutc.chongchong.user.entity.User;

public record UserProfileResponse(
        String name,
        String profileImageUrl
) {

    public static UserProfileResponse from(User user) {
        return from(user.getName(), user.getProfileImageUrl());
    }

    public static UserProfileResponse from(String name, String profileImageUrl) {
        return new UserProfileResponse(
                name,
                profileImageUrl
        );
    }
}
