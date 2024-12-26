package com.origene.userservice.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BaseResponse {
    private int status;
    private String message;
    public BaseResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
