package withoutc.chongchong.user.controller.dto;

import withoutc.chongchong.user.entity.User;

public record UserProfileResponse(
        String name,
        String profileImageUrl
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getName(),
                user.getProfileImageUrl()
        );
    }
}