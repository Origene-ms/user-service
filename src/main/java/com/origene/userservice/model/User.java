package com.origene.userservice.model;

import com.origene.userservice.enums.Role;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
  @Id
  private String id;
  private String name;
  private String email;
  private String phone;
  private String password;
  private LocalDate dateOfBirth;
  private String activeStatus;
  private Community community;
  private LocalDateTime lastActiveTime;
  private SocialAccount facebook;
  private SocialAccount google;
  private String pictureName;
  private Country country;
  private String address;
  private List<PlaceDetails> placeDetails;
  private boolean isAdmin;
  private String token;
  private List<Role> roles;
}

@Data
class SocialAccount {
  private String id;
  private String token;
  private String email;
  private String name;
}