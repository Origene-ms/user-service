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
        user.setActiveStatus("INACTIVE"); // Default active status
        user.setLastActiveTime(LocalDateTime.now()); // Set current time
        user.setAdmin(false); // Default admin status
        user.setRoles(Collections.emptyList()); // Default roles
        user.setToken(null); // No token initially
        user.setFacebook(null); // No social account linked initially
        user.setGoogle(null); // No social account linked initially

        return user;
    }
}
