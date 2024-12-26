package com.origene.userservice.dto.request;

import lombok.Data;

@Data
public class RefreshTokenDTO {
    String refreshToken;
    String userId;
}
