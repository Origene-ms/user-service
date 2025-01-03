package com.origene.userservice.dto.request;

import com.origene.userservice.enums.Role;
import com.origene.userservice.model.Community;
import com.origene.userservice.model.PlaceDetails;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String password;
    private LocalDate dateOfBirth;
    private Community community;
    private String pictureName;
    private  String thumbnailName;
    private String country;
    private String address;
    private String activeStatus;
    private LocalDateTime lastActiveTime;
    private List<Role> roles;
    private boolean isAdmin;
    private List<PlaceDetails> placeDetails;
}
