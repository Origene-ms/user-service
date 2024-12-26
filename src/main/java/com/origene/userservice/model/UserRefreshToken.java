package com.origene.userservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "userRefreshTokens")
public class UserRefreshToken {
    @Id
    private String id;
    
    private String userId;
    private String token;
    private Date createdAt;
}