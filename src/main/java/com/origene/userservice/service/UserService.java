package com.origene.userservice.service;

import com.origene.userservice.dto.request.UserDTO;
import com.origene.userservice.dto.response.UserLoginResponse;
import com.origene.userservice.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserService {
    Mono<UserDTO> signup(UserDTO userDTO);

    Mono<UserLoginResponse> login(String email, String password);

    Flux<User> getAllUsers();

    Flux<User> getAllUsersExceptOne(String excludedUserId);

    Mono<User> getUserById(String id);

    Mono<User> getMyUser();

    Mono<User> getCurrentUser();

    Flux<User> getAllUsersByIds(List<String> ids);

    Mono<User> getUserByEmail(String email);

    Flux<User> getAllAdminUsers();

    Mono<UserDTO> updatePassword(String email, String currentPassword, String updatedPassword);

    Mono<String> forgotPassword(String email);

    Mono<UserDTO> updateForgottenPassword(String email, String token, String password);

    Mono<User> deactivateUser(String email);

    Mono<User> activateUser(String email);

    Mono<String> signUpTokenConfirmation(String token);

    Mono<String> resendToken(String email);

    Mono<String> refreshAccessToken(String userId, String refreshToken);
}
