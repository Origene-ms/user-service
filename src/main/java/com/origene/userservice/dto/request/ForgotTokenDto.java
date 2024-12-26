package com.origene.userservice.dto.request;

import lombok.Data;

@Data
public class ForgotTokenDto {
    private String email;
    private String token;
    private String password;
}
