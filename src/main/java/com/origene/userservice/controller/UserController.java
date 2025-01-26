package com.origene.userservice.controller;

import com.origene.userservice.dto.request.RefreshTokenDTO;
import com.origene.userservice.dto.request.ForgotTokenDTO;
import com.origene.userservice.dto.request.UpdatePasswordDTO;
import com.origene.userservice.dto.request.UserDTO;
import com.origene.userservice.dto.response.UserLoginResponse;
import com.origene.userservice.enums.ActiveStatus;
import com.origene.userservice.model.User;
import com.origene.userservice.service.impl.UserServiceImpl;
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
  private UserServiceImpl userServiceImpl;

  @PostMapping("/signup")
  public Mono<ResponseEntity<UserDTO>> signup(@RequestBody UserDTO user) {
    return userServiceImpl.signup(user)
            .map(savedUser -> ResponseEntity.status(201).body(savedUser))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/login")
  public Mono<ResponseEntity<UserLoginResponse>> login(@RequestBody UserDTO userDTO) {
    return userServiceImpl.login(userDTO.getEmail(), userDTO.getPassword())
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @GetMapping("/confirmation")
  public Mono<ResponseEntity<String>> signUpTokenConfirmation(@RequestParam String token) {
    return userServiceImpl.signUpTokenConfirmation(token)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/resendVerificationToken")
  public Mono<ResponseEntity<String>> signupTokenResend(@RequestBody String email) {
    return userServiceImpl.resendToken(email)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/refreshToken")
  public Mono<ResponseEntity<String>> refreshNewToken(@RequestBody RefreshTokenDTO refreshToken) {
    return userServiceImpl.refreshAccessToken(refreshToken.getUserId(), refreshToken.getRefreshToken())
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @GetMapping("/me")
  public Mono<ResponseEntity<User>> getMyUser() {
    return userServiceImpl.getMyUser()
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/usersByIds")
  public Flux<User> getUsersByIds(@RequestBody List<String> ids) {
    return userServiceImpl.getAllUsersByIds(ids);
  }

  @GetMapping("/userById/{id}")
  public Mono<ResponseEntity<User>> getById(@PathVariable String id) {
    return userServiceImpl.getUserById(id)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @GetMapping("/userByEmail/{email}")
  public Mono<ResponseEntity<User>> getByEmail(@PathVariable String email) {
    return userServiceImpl.getUserByEmail(email)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @GetMapping("/all")
  public Mono<ResponseEntity<List<User>>> getAllUsers() {
    return userServiceImpl.getAllUsers()
            .collectList()
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @GetMapping("/all-except-one/{id}")
  public Mono<ResponseEntity<List<User>>> getAllUsersExceptOne(@PathVariable String id) {
    return userServiceImpl.getAllUsersExceptOne(id)
            .collectList()
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PutMapping("/updatePassword")
  public Mono<ResponseEntity<UserDTO>> updatePassword(@RequestBody UpdatePasswordDTO updatePasswordDTO) {
    return userServiceImpl.updatePassword(updatePasswordDTO.getEmail(), updatePasswordDTO.getCurrentPassword(), updatePasswordDTO.getUpdatedPassword())
            .map(ResponseEntity::ok)
            .onErrorResume(e -> {
              // Log the error and propagate it up
              log.error("Error occurred while updating password for user: {}", updatePasswordDTO, e);
              return Mono.error(e);
            });
  }


  @PostMapping("/forgotPassword")
  public Mono<ResponseEntity<String>> forgotPassword(@RequestParam String email) {
    return userServiceImpl.forgotPassword(email)
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(e.getMessage())));
  }

  @PostMapping("/updateForgottenPassword")
  public Mono<ResponseEntity<UserDTO>> updateForgottenPassword(@RequestBody ForgotTokenDTO forgotTokenDto) {
    return userServiceImpl.updateForgottenPassword(forgotTokenDto.getEmail(), forgotTokenDto.getToken(), forgotTokenDto.getPassword())
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/deactivate")
  public Mono<ResponseEntity<User>> deactivateUser(@RequestBody Map<String, String> data) {
    return userServiceImpl.deactivateUser(data.get("reason"))
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/activate")
  public Mono<ResponseEntity<User>> activateUser() {
    return userServiceImpl.activateUser(ActiveStatus.ACTIVE.name())
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @GetMapping("/admin-users")
  public Mono<ResponseEntity<List<User>>> getAllAdminUsers() {
    return userServiceImpl.getAllAdminUsers()
            .collectList()
            .map(ResponseEntity::ok)
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }
}