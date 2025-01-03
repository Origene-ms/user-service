package com.origene.userservice.controller;

import com.origene.userservice.dto.request.RefreshTokenDTO;
import com.origene.userservice.dto.request.ForgotTokenDto;
import com.origene.userservice.dto.request.UpdatePasswordDTO;
import com.origene.userservice.dto.request.UserDTO;
import com.origene.userservice.dto.response.UserLoginResponse;
import com.origene.userservice.enums.ActiveStatus;
import com.origene.userservice.model.User;
import com.origene.userservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Slf4j
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public Mono<ResponseEntity<User>> signup(@RequestBody UserDTO user) {
        return userService.signup(user)
                .map(savedUser -> ResponseEntity.status(201).body(savedUser))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<UserLoginResponse>> login(@RequestBody UserDTO userDTO) {
        return userService.login(userDTO.getEmail(), userDTO.getPassword())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/confirmation")
    public Mono<ResponseEntity<String>> signUpTokenConfirmation(@RequestParam String token) {
        return userService.signUpTokenConfirmation(token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/resendVerificationToken")
    public Mono<ResponseEntity<String>> signupTokenResend(@RequestBody String email) {
        return userService.resendToken(email)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/refreshToken")
    public Mono<ResponseEntity<String>> refreshNewToken(@RequestBody RefreshTokenDTO refreshToken) {
        return userService.refreshAccessToken(refreshToken.getUserId(), refreshToken.getRefreshToken())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<User>> getMyUser() {
        return userService.getMyUser()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/usersByIds")
    public Flux<User> getUsersByIds(@RequestBody List<String> ids) {
        return userService.getAllUsersByIds(ids);
    }

    @GetMapping("/userById/{id}")
    public Mono<ResponseEntity<User>> getById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/userByEmail/{email}")
    public Mono<ResponseEntity<User>> getByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/all")
    public Mono<ResponseEntity<List<User>>> getAllUsers() {
        return userService.getAllUsers()
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/all-except-one/{id}")
    public Mono<ResponseEntity<List<User>>> getAllUsersExceptOne(@PathVariable String id) {
        return userService.getAllUsersExceptOne(id)
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/updatePassword")
    public Mono<ResponseEntity<User>> updatePassword(@RequestBody UpdatePasswordDTO password) {
        return userService.updatePassword(password.getEmail(), password.getCurrentPassword(), password.getUpdatedPassword())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/forgotPassword")
    public Mono<ResponseEntity<String>> forgotPassword(@RequestParam String email) {
        return userService.forgotPassword(email)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
    }

    @PostMapping("/updateForgottenPassword")
    public Mono<ResponseEntity<User>> updateForgottenPassword(@RequestBody ForgotTokenDto forgotTokenDto) {
        return userService.updateForgottenPassword(forgotTokenDto.getEmail(), forgotTokenDto.getToken(), forgotTokenDto.getPassword())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/deactivate")
    public Mono<ResponseEntity<User>> deactivateUser(@RequestBody Map<String, String> data) {
        return userService.deactivateUser(data.get("reason"))
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/activate")
    public Mono<ResponseEntity<User>> activateUser() {
        return userService.activateUser(ActiveStatus.ACTIVE.name())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/admin-users")
    public Mono<ResponseEntity<List<User>>> getAllAdminUsers() {
        return userService.getAllAdminUsers()
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }
}