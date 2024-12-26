package com.origene.userservice.service;

import com.origene.userservice.dto.request.UserDTO;
import com.origene.userservice.dto.response.UserLoginResponse;
import com.origene.userservice.enums.ActiveStatus;
import com.origene.userservice.exception.RequestValidationException;
import com.origene.userservice.mapper.UserMapper;
import com.origene.userservice.model.User;
import com.origene.userservice.model.UserRefreshToken;
import com.origene.userservice.repository.UserForgotPasswordTokenRepository;
import com.origene.userservice.repository.UserRefreshTokenRepository;
import com.origene.userservice.repository.UserRepository;
import com.origene.userservice.repository.UserVerificationTokenRepository;
import com.origene.userservice.util.JwtUtil;
import com.origene.userservice.util.PasswordUtil;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final UserVerificationTokenRepository userVerificationTokenRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final UserForgotPasswordTokenRepository userForgotPasswordTokenRepository;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil, EmailService emailService, UserVerificationTokenRepository userVerificationTokenRepository, UserRefreshTokenRepository userRefreshTokenRepository, UserForgotPasswordTokenRepository userForgotPasswordTokenRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.userVerificationTokenRepository = userVerificationTokenRepository;
        this.userRefreshTokenRepository = userRefreshTokenRepository;
        this.userForgotPasswordTokenRepository = userForgotPasswordTokenRepository;
    }

    public Mono<User> signup(UserDTO userDTO) {
        return userRepository.findByEmail(userDTO.getEmail())
                .flatMap(existingUser -> Mono.<User>error(new RequestValidationException("User with associated email already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    User newUser = UserMapper.toUser(userDTO);
                    newUser.setPassword(PasswordUtil.encryptPassword((userDTO.getPassword())));
                    newUser.setActiveStatus(ActiveStatus.INACTIVE.name());
                    return userRepository.save(newUser)
                            .flatMap(savedUser -> {
                                emailService.saveAndSendVerificationTokenEmail(savedUser);
                                return Mono.just(savedUser);
                            });

                }));
    }

    public Mono<User> login(String email, String password) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("User does not exist")))
                .flatMap(user -> {
                    if (!PasswordUtil.isPasswordMatch(password, user.getPassword())) {
                        return Mono.error(new RuntimeException("Incorrect Password!"));
                    }

                    if (user.getActiveStatus().equals(ActiveStatus.UNVERIFIED.name())) {
                        return Mono.error(new RuntimeException("Please verify your email to proceed with login"));
                    }

                    String accessToken = jwtUtil.generateToken(user.getId());
                    String refreshToken = jwtUtil.generateToken(user.getId());

                    UserRefreshToken userRefreshToken = new UserRefreshToken();

                    return userRefreshTokenRepository.save(userRefreshToken)
                        .then(Mono.just(user))
                        .doOnNext(existingUser -> {
                            if (ActiveStatus.INACTIVE.name().equals(user.getActiveStatus())) {
                                user.setActiveStatus(ActiveStatus.ACTIVE.name());
                                userRepository.save(user).subscribe();
                            }
                        })
                        .flatMap(updatedUser -> {
                            UserLoginResponse userLoginResponse = new UserLoginResponse();
                            userLoginResponse.setStatus(200);
                            userLoginResponse.setAccessToken(accessToken);
                            userLoginResponse.setRefreshToken(refreshToken);
                            if (user.isAdmin()) {
                                String adminToken = jwtUtil.generateToken(user.getId());
                                userLoginResponse.setAdminToken(adminToken);
                            }

                            return Mono.just(updatedUser);
                        });
                });
    }

    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Flux<User> getAllUsersExceptOne(String excludedUserId) {
        return userRepository.findAll()
                .filter(user -> !user.getId().equals(excludedUserId)); // Exclude the user with the given ID
    }

    public Mono<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Mono<User> getMyUser() {
        return getCurrentUser()
                .flatMap(user -> {
                    user.setLastActiveTime(LocalDateTime.now()); // Update lastActiveTime
                    return userRepository.save(user); // Save the updated user
                })
                .onErrorResume(e -> Mono.error(new RequestValidationException(e.getMessage()))); // Handle errors gracefully
    }

    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication().getCredentials().toString()) // Extract JWT token
                .flatMap(jwtToken -> {
                    String userId = jwtUtil.getUsernameFromToken(jwtToken); // Extract userId from JWT
                    return userRepository.findById(userId);
                });
    }

    public Flux<User> getAllUsersByIds(List<String> id) {
        return userRepository.findAllById(id);
    }

    public Mono<User> getUserByEmail(String id) {
        return userRepository.findByEmail(id);
    }

    public Flux<User> getAllAdminUsers() {
        return userRepository.findAll()
                .filter(User::isAdmin);
    }

    public Mono<User> updatePassword(String email, String currentPassword, String updatedPassword) {
        return userRepository.findByEmail(email)
                .flatMap(user -> {
                    if (PasswordUtil.isPasswordMatch(currentPassword, user.getPassword())) {
                        user.setPassword(PasswordUtil.encryptPassword((updatedPassword)));
                        return userRepository.save(user);
                    } else {
                        return Mono.error(new RuntimeException("Invalid current password"));
                    }
                });
    }

    @Transactional
    public Mono<String> forgotPassword(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RequestValidationException("User with email not found")))
                .flatMap(user -> {
                    String resetToken = UUID.randomUUID().toString(); // Generate a unique token
                    user.setToken(resetToken);
                    return userRepository.save(user)
                            .then(Mono.fromCallable(() -> {
                                try {
                                    emailService.sendEmail(
                                            user.getEmail(),
                                            "Password Reset Request",
                                            "Click here to reset your password: https://yourdomain.com/reset?token=" + resetToken
                                    );
                                    return "Password reset email sent successfully";
                                } catch (IOException e) {
                                    throw new RuntimeException("Failed to send email", e); // Handle IOException
                                }
                            }));
                });
    }

    public Mono<User> updateForgottenPassword(String email, String token, String password) {
        return userRepository.findByEmailAndActiveStatus(email, ActiveStatus.ACTIVE.name())
                .switchIfEmpty(Mono.error(new RuntimeException("User not found or inactive")))
                .flatMap(user ->
                        userForgotPasswordTokenRepository.findByUserIdAndTokenAndActiveStatus(user.getId(), token, ActiveStatus.ACTIVE.name())
                                .switchIfEmpty(Mono.error(new RuntimeException("Verification token expired or invalid")))
                                .flatMap(tokenObject -> {
                                    // Encrypt the new password
                                    user.setPassword(PasswordUtil.encryptPassword(password));

                                    // Mark the token as inactive
                                    tokenObject.setActiveStatus(ActiveStatus.INACTIVE.name());

                                    // Save the user and token updates reactively
                                    return userRepository.save(user)
                                            .then(userForgotPasswordTokenRepository.save(tokenObject))
                                            .thenReturn(user);
                                })
                );
    }


    public Mono<User> deactivateUser(String email) {
        return userRepository.findByEmail(email)
                .flatMap(user -> {
                    user.setActiveStatus(ActiveStatus.INACTIVE.name());
                    return userRepository.save(user);
                });
    }

    public Mono<User> activateUser(String email) {
        return userRepository.findByEmail(email)
                .flatMap(user -> {
                    user.setActiveStatus(ActiveStatus.ACTIVE.name());
                    return userRepository.save(user);
                });
    }

    public Mono<String> signUpTokenConfirmation(String token) {
        return userVerificationTokenRepository.findByToken(token)
                .flatMap(tokenObject -> userRepository.findById(tokenObject.getId()))
                .map(user -> {
                    user.setActiveStatus(ActiveStatus.ACTIVE.name());
                    return userRepository.save(user);
                })
                .map(user -> "User verification complete. You may now log in to the application. <br>"
                        + "<a href=\"http://origene-ms.com\"> Navigate to Orizene App </a>")
                .switchIfEmpty(Mono.just("Unable to verify token. Try resending a new token"))
                .onErrorResume(error -> Mono.just("Unable to verify token. Try resending a new token"));
    }

    public Mono<String> resendToken(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RequestValidationException("User not found with email: " + email)))
                .flatMap(existingUser -> {
                    emailService.saveAndSendVerificationTokenEmail(existingUser);
                    return Mono.just("Verification token resent successfully.");
                });
    }

    public Mono<String> refreshAccessToken(String userId, String refreshToken) {
        return userRefreshTokenRepository.findByUserIdAndToken(userId, refreshToken)
                .switchIfEmpty(Mono.error(new RuntimeException("Unauthorized refresh token")))
                .flatMap(existingToken -> {
                    String newAccessToken = jwtUtil.generateToken(userId);
                    String newRefreshToken = jwtUtil.generateToken(userId);

                    UserRefreshToken newUserRefreshToken = new UserRefreshToken();

                    return userRefreshTokenRepository.delete(existingToken)
                            .then(userRefreshTokenRepository.save(newUserRefreshToken))
                            .then(Mono.just(newAccessToken + "," + newRefreshToken));
                });
    }

}
