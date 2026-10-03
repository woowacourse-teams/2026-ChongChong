package withoutc.chongchong.user.controller.dto;

import withoutc.chongchong.user.entity.User;

public record UserProfileNameUpdateRequest(
        String name,
        String profileImgUrl
) {

    public static UserProfileNameUpdateRequest from(User user) {
        return new UserProfileNameUpdateRequest(
                user.getName(),
                user.getProfileImageUrl()
        );
    }
}
