package com.origene.userservice.dto.response;

import com.origene.userservice.model.User;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserLoginResponse extends BaseResponse{
    private String accessToken;
    private String refreshToken;
    private String adminToken;
    private User user;

    public UserLoginResponse(int status, String message) {
        super(status, message);
    }

}