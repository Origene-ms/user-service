package com.origene.userservice.mapper;

import com.origene.userservice.dto.request.UserDTO;
import com.origene.userservice.model.User;

import java.time.LocalDateTime;
import java.util.Collections;

public class UserMapper {

  public static User toUser(UserDTO userDTO) {
    if (userDTO == null) {
      return null;
    }

    User user = new User();
    user.setName(userDTO.getName());
    user.setEmail(userDTO.getEmail());
    user.setPhone(userDTO.getPhone());
    user.setPassword(userDTO.getPassword());
    user.setDateOfBirth(userDTO.getDateOfBirth());
    user.setCommunity(userDTO.getCommunity());
    user.setPictureName(userDTO.getPictureName());
    user.setCountry(userDTO.getCountry());
    user.setAddress(userDTO.getAddress());
    user.setPlaceDetails(userDTO.getPlaceDetails());

    // Default or additional fields in User
    user.setActiveStatus(userDTO.getActiveStatus()); // Default active status
    user.setLastActiveTime(LocalDateTime.now()); // Set current time
    user.setAdmin(false); // Default admin status
    user.setRoles(Collections.emptyList()); // Default roles
    user.setToken(null); // No token initially
    user.setFacebook(null); // No social account linked initially
    user.setGoogle(null); // No social account linked initially

    return user;
  }

  public static UserDTO toUserDTO(User user) {
    if (user == null) {
      return null;
    }

    UserDTO userDTO = new UserDTO();
    userDTO.setId(user.getId());
    userDTO.setName(user.getName());
    userDTO.setEmail(user.getEmail());
    userDTO.setPhone(user.getPhone());
    userDTO.setDateOfBirth(user.getDateOfBirth());
    userDTO.setCommunity(user.getCommunity());
    userDTO.setPictureName(user.getPictureName());
    userDTO.setCountry(user.getCountry());
    userDTO.setAddress(user.getAddress());
    userDTO.setPlaceDetails(user.getPlaceDetails());
    userDTO.setActiveStatus(user.getActiveStatus()); // Default active status
    userDTO.setLastActiveTime(LocalDateTime.now()); // Set current time
    userDTO.setAdmin(false); // Default admin status
    userDTO.setRoles(Collections.emptyList()); // Default roles

    return userDTO;
  }
}
