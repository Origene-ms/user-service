package com.origene.userservice.service.impl;

import com.origene.userservice.config.security.UserDetailsImpl;
import com.origene.userservice.dto.request.UserDTO;
import com.origene.userservice.dto.response.UserLoginResponse;
import com.origene.userservice.enums.ActiveStatus;
import com.origene.userservice.exception.RequestValidationException;
import com.origene.userservice.exception.ResourceAlreadyExistsException;
import com.origene.userservice.exception.ResourceNotFoundException;
import com.origene.userservice.mapper.UserMapper;
import com.origene.userservice.model.User;
import com.origene.userservice.model.UserRefreshToken;
import com.origene.userservice.repository.UserForgotPasswordTokenRepository;
import com.origene.userservice.repository.UserRefreshTokenService;
import com.origene.userservice.repository.UserRepository;
import com.origene.userservice.service.UserService;
import com.origene.userservice.service.UserVerificationTokenService;
import com.origene.userservice.util.JwtUtil;
import com.origene.userservice.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

  @Value("${app.host}")
  private String appHost;

  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;
  private final EmailServiceImpl emailServiceImpl;
  private final UserVerificationTokenService userVerificationTokenService;
  private final UserRefreshTokenService userRefreshTokenService;
  private final UserForgotPasswordTokenRepository userForgotPasswordTokenRepository;

  private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);


  public UserServiceImpl(UserRepository userRepository, JwtUtil jwtUtil, EmailServiceImpl emailServiceImpl,
                         UserVerificationTokenService userVerificationTokenService,
                         UserRefreshTokenService userRefreshTokenService,
                         UserForgotPasswordTokenRepository userForgotPasswordTokenRepository) {
    this.userRepository = userRepository;
    this.jwtUtil = jwtUtil;
    this.emailServiceImpl = emailServiceImpl;
    this.userVerificationTokenService = userVerificationTokenService;
    this.userRefreshTokenService = userRefreshTokenService;
    this.userForgotPasswordTokenRepository = userForgotPasswordTokenRepository;
  }

  @Override
  public Mono<UserDTO> signup(UserDTO userDTO) {
    return userRepository.findByEmail(userDTO.getEmail())
            .flatMap(existingUser -> Mono.<UserDTO>error(new ResourceAlreadyExistsException("User with associated email already exists")))
            .switchIfEmpty(Mono.defer(() -> {
              User newUser = UserMapper.toUser(userDTO);
              newUser.setPassword(PasswordUtil.encryptPassword(userDTO.getPassword()));
              newUser.setActiveStatus(ActiveStatus.INACTIVE.name());
              return userRepository.save(newUser)
                      .flatMap(savedUser -> emailServiceImpl.saveAndSendVerificationTokenEmail(savedUser)
                              .thenReturn(UserMapper.toUserDTO(savedUser)));
            }));
  }

  @Override
  public Mono<UserLoginResponse> login(String email, String password) {
    logger.info("User attempting to log in with email: {}", email);
    return userRepository.findByEmailAndActiveStatusIn(email,
                    Arrays.asList(ActiveStatus.ACTIVE.name(), ActiveStatus.INACTIVE.name(), ActiveStatus.UNVERIFIED.name()))
            .switchIfEmpty(Mono.error(new RuntimeException("User does not exist")))
            .flatMap(user -> {
              if (!PasswordUtil.isPasswordMatch(password, user.getPassword())) {
                logger.warn("Incorrect password attempt for email: {}", email);
                return Mono.error(new RuntimeException("Incorrect Password!"));
              }

              if (user.getActiveStatus().equals(ActiveStatus.UNVERIFIED.name())) {
                logger.warn("Unverified email login attempt: {}", email);
                return Mono.error(new RuntimeException("Please verify your email to proceed with login"));
              }

              String accessToken = jwtUtil.generateToken(user.getId());
              String refreshToken = jwtUtil.generateToken(user.getId());
              UserRefreshToken userRefreshToken = new UserRefreshToken();
              userRefreshToken.setUserId(user.getId());
              userRefreshToken.setToken(refreshToken);

              return userRefreshTokenService.save(userRefreshToken)
                      .then(Mono.just(user))
                      .doOnNext(existingUser -> {
                        if (ActiveStatus.INACTIVE.name().equals(user.getActiveStatus())) {
                          user.setActiveStatus(ActiveStatus.ACTIVE.name());
                          userRepository.save(user).subscribe();
                          logger.info("Activated inactive user with email: {}", email);
                        }
                      })
                      .flatMap(updatedUser -> {
                        UserLoginResponse userLoginResponse = new UserLoginResponse(200, "Login Successful!");
                        userLoginResponse.setAccessToken(accessToken);
                        userLoginResponse.setRefreshToken(refreshToken);
                        userLoginResponse.setUser(UserMapper.toUserDTO(user));

                        if (user.isAdmin()) {
                          String adminToken = jwtUtil.generateToken(user.getId());
                          userLoginResponse.setAdminToken(adminToken);
                          logger.debug("Admin token generated for user with email: {}", email);
                        }

                        logger.info("User with email {} logged in successfully.", email);
                        return Mono.just(userLoginResponse);
                      });
            })
            .doOnError(e -> logger.error("Login error for email {}: {}", email, e.getMessage()));
  }

  @Override
  public Flux<User> getAllUsers() {
    logger.info("Fetching all users.");
    return userRepository.findAll()
            .doOnComplete(() -> logger.info("Successfully fetched all users."))
            .doOnError(e -> logger.error("Error fetching all users: {}", e.getMessage()));
  }

  @Override
  public Flux<User> getAllUsersExceptOne(String excludedUserId) {
    logger.info("Fetching all users except user with ID: {}", excludedUserId);
    return userRepository.findAll()
            .filter(user -> !user.getId().equals(excludedUserId))
            .doOnComplete(() -> logger.info("Successfully fetched all users except user with ID: {}", excludedUserId))
            .doOnError(e -> logger.error("Error fetching users: {}", e.getMessage()));
  }

  @Override
  public Mono<User> getUserById(String id) {
    logger.info("Fetching user with ID: {}", id);
    return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + id)))
            .doOnSuccess(user -> logger.info("Successfully fetched user with ID: {}", id))
            .doOnError(e -> logger.error("Error fetching user with ID {}: {}", id, e.getMessage()));
  }

  @Override
  public Mono<User> getMyUser() {
    logger.info("Attempting to retrieve the current authenticated user.");
    return getCurrentUser()
            .flatMap(user -> {
              user.setLastActiveTime(LocalDateTime.now());
              return userRepository.save(user)
                      .doOnSuccess(savedUser -> logger.info("Updated last active time for user with ID: {}", savedUser.getId()))
                      .doOnError(e -> logger.error("Failed to update last active time for user with ID: {}", user.getId(), e));
            })
            .onErrorResume(e -> {
              logger.error("Error retrieving current user: {}", e.getMessage());
              return Mono.error(new ResourceNotFoundException(e.getMessage()));
            });
  }

  @Override
  public Mono<User> getCurrentUser() {
    logger.info("Fetching the current authenticated user from the security context.");
    return ReactiveSecurityContextHolder.getContext()
            .flatMap(context -> {
              Authentication authentication = context.getAuthentication();
              if (authentication != null && authentication.isAuthenticated()) {
                UserDetails userDetails = (UserDetailsImpl) authentication.getPrincipal();
                logger.debug("Authenticated user email: {}", userDetails.getUsername());
                return userRepository.findByEmail(userDetails.getUsername())
                        .flatMap(user -> {
                          user.setLastActiveTime(LocalDateTime.now());
                          return userRepository.save(user)
                                  .doOnSuccess(savedUser -> logger.info("Updated last active time for user with ID: {}", savedUser.getId()))
                                  .doOnError(e -> logger.error("Failed to update last active time for user with ID: {}", user.getId(), e));
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                          logger.warn("User not found with email: {}", userDetails.getUsername());
                          return Mono.error(new ResourceNotFoundException("User not found"));
                        }));
              } else {
                logger.warn("No authenticated user found in the security context.");
                return Mono.error(new ResourceNotFoundException("User not authenticated."));
              }
            });
  }

  @Override
  public Flux<User> getAllUsersByIds(List<String> ids) {
    logger.info("Fetching users with IDs: {}", ids);
    return userRepository.findAllById(ids)
            .doOnComplete(() -> logger.info("Successfully fetched users with IDs: {}", ids))
            .doOnError(e -> logger.error("Error fetching users with IDs: {}", ids, e));
  }

  @Override
  public Mono<User> getUserByEmail(String email) {
    logger.info("Fetching user with email: {}", email);
    return userRepository.findByEmail(email)
            .doOnSuccess(user -> {
              if (user != null) {
                logger.info("Found user with email: {}", email);
              } else {
                logger.warn("No user found with email: {}", email);
              }
            })
            .doOnError(e -> logger.error("Error fetching user with email: {}", email, e));
  }

  @Override
  public Flux<User> getAllAdminUsers() {
    logger.info("Fetching all admin users.");
    return userRepository.findAll()
            .filter(User::isAdmin)
            .doOnComplete(() -> logger.info("Successfully fetched all admin users."))
            .doOnError(e -> logger.error("Error fetching admin users.", e));
  }

  @Override
  public Mono<UserDTO> updatePassword(String email, String currentPassword, String updatedPassword) {
    logger.info("Updating password for user with email: {}", email);
    return userRepository.findByEmail(email)
            .flatMap(user -> {
              // Validate current password
              if (PasswordUtil.isPasswordMatch(currentPassword, user.getPassword())) {
                // Encrypt and save the updated password
                user.setPassword(PasswordUtil.encryptPassword(updatedPassword));
                return userRepository.save(user)
                        .map(savedUser -> {
                          logger.info("Password updated successfully for user with email: {}", email);
                          return UserMapper.toUserDTO(savedUser); // Map to UserDTO
                        })
                        .doOnError(e -> logger.error("Failed to save updated password for user: {}", email, e));
              } else {
                logger.warn("Invalid current password provided for user with email: {}", email);
                return Mono.error(new RuntimeException("Invalid current password"));
              }
            })
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with email: " + email))) // Handle missing user
            .onErrorResume(e -> {
              // Log the error and propagate it up
              logger.error("Error occurred while updating password for user: {}", email, e);
              return Mono.error(e);
            });
  }


  @Override
  @Transactional
  public Mono<String> forgotPassword(String email) {
    logger.info("Processing forgot password request for email: {}", email);
    return userRepository.findByEmail(email)
            .switchIfEmpty(Mono.defer(() -> {
              logger.warn("User with email not found: {}", email);
              return Mono.error(new RequestValidationException("User with email not found"));
            }))
            .flatMap(user -> {
              emailServiceImpl.saveAndSendForgotPasswordTokenEmail(user);
              return Mono.just("Forgot password token sent via email");
            });
  }

  @Override
  public Mono<UserDTO> updateForgottenPassword(String email, String token, String password) {
    logger.info("Updating forgotten password for user with email: {}", email);
    return userRepository.findByEmailAndActiveStatus(email, ActiveStatus.ACTIVE.name())
            .switchIfEmpty(Mono.defer(() -> {
              logger.warn("User not found or inactive with email: {}", email);
              return Mono.error(new RuntimeException("User not found or inactive"));
            }))
            .flatMap(user ->
                    userForgotPasswordTokenRepository.findByUserIdAndTokenAndActiveStatus(user.getId(), token, ActiveStatus.ACTIVE.name())
                            .switchIfEmpty(Mono.defer(() -> {
                              logger.warn("Verification token expired or invalid for user with email: {}", email);
                              return Mono.error(new RuntimeException("Verification token expired or invalid"));
                            }))
                            .flatMap(tokenObject -> {
                              user.setPassword(PasswordUtil.encryptPassword(password));
                              tokenObject.setActiveStatus(ActiveStatus.INACTIVE.name());
                              return userRepository.save(user)
                                      .then(userForgotPasswordTokenRepository.save(tokenObject))
                                      .thenReturn(UserMapper.toUserDTO(user))
                                      .doOnSuccess(updatedUser -> logger.info("Successfully updated forgotten password for user with email: {}", email))
                                      .doOnError(e -> logger.error("Failed to update forgotten password for user with email: {}", email, e));
                            })
            );
  }

  @Override
  public Mono<User> deactivateUser(String email) {
    logger.info("Attempting to deactivate user with email: {}", email);
    return userRepository.findByEmail(email)
            .flatMap(user -> {
              user.setActiveStatus(ActiveStatus.INACTIVE.name());
              return userRepository.save(user)
                      .doOnSuccess(deactivatedUser -> logger.info("Successfully deactivated user with email: {}", email))
                      .doOnError(error -> logger.error("Error deactivating user with email: {}", email, error));
            })
            .switchIfEmpty(Mono.defer(() -> {
              logger.warn("User with email: {} not found for deactivation", email);
              return Mono.error(new ResourceNotFoundException("User not found with email: " + email));
            }));
  }

  @Override
  public Mono<User> activateUser(String email) {
    logger.info("Attempting to activate user with email: {}", email);
    return userRepository.findByEmail(email)
            .flatMap(user -> {
              user.setActiveStatus(ActiveStatus.ACTIVE.name());
              return userRepository.save(user)
                      .doOnSuccess(activatedUser -> logger.info("Successfully activated user with email: {}", email))
                      .doOnError(error -> logger.error("Error activating user with email: {}", email, error));
            })
            .switchIfEmpty(Mono.defer(() -> {
              logger.warn("User with email: {} not found for activation", email);
              return Mono.error(new ResourceNotFoundException("User not found with email: " + email));
            }));
  }

  @Override
  public Mono<String> signUpTokenConfirmation(String token) {
    logger.info("Attempting to confirm sign-up with token: {}", token);
    return userVerificationTokenService.getToken(token)
            .flatMap(tokenObject -> userRepository.findById(tokenObject.getUser().getId())
                    .flatMap(user -> {
                      user.setActiveStatus(ActiveStatus.ACTIVE.name());
                      return userRepository.save(user)
                              .doOnSuccess(updatedUser -> logger.info("User with ID: {} activated successfully", updatedUser.getId()))
                              .doOnError(error -> logger.error("Error activating user with ID: {}", user.getId(), error));
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                      logger.warn("User not found for token: {}", token);
                      return Mono.error(new ResourceNotFoundException("User not found for the provided token."));
                    }))
            )
            .map(user -> "User verification complete. You may now log in to the application. <br>"
                    + "<a href=\"" + appHost + "\"> Navigate to Orizene App </a>")
            .switchIfEmpty(Mono.defer(() -> {
              logger.warn("Verification token: {} not found or already used", token);
              return Mono.just("Unable to verify token. Try resending a new token");
            }))
            .onErrorResume(error -> {
              logger.error("Error during sign-up token confirmation for token: {}", token, error);
              return Mono.just("Unable to verify token. Try resending a new token");
            });
  }

  @Override
  public Mono<String> resendToken(String email) {
    logger.info("Attempting to resend verification token to email: {}", email);
    return userRepository.findByEmail(email)
            .flatMap(existingUser -> {
              emailServiceImpl.saveAndSendVerificationTokenEmail(existingUser);
              logger.info("Verification token resent successfully to email: {}", email);
              return Mono.just("Verification token resent successfully.");
            })
            .switchIfEmpty(Mono.defer(() -> {
              logger.warn("User not found with email: {}. Cannot resend verification token.", email);
              return Mono.error(new RequestValidationException("User not found with email: " + email));
            }))
            .onErrorResume(error -> {
              logger.error("Error resending verification token to email: {}", email, error);
              return Mono.just("Error resending verification token. Please try again later.");
            });
  }

  @Override
  public Mono<String> refreshAccessToken(String userId, String refreshToken) {
    logger.info("Attempting to refresh access token for user ID: {}", userId);
    return userRefreshTokenService.findByUserIdAndToken(userId, refreshToken)
            .flatMap(existingToken -> {
              String newAccessToken = jwtUtil.generateToken(userId);
              String newRefreshToken = jwtUtil.generateToken(userId);

              UserRefreshToken newUserRefreshToken = new UserRefreshToken();
              newUserRefreshToken.setUserId(userId);
              newUserRefreshToken.setToken(newRefreshToken);

              return userRefreshTokenService.delete(existingToken)
                      .then(userRefreshTokenService.save(newUserRefreshToken))
                      .doOnSuccess(savedToken -> logger.info("Successfully refreshed tokens for user ID: {}", userId))
                      .doOnError(error -> logger.error("Error refreshing tokens for user ID: {}", userId, error))
                      .then(Mono.just(newAccessToken + "," + newRefreshToken));
            })
            .switchIfEmpty(Mono.defer(() -> {
              logger.warn("Unauthorized refresh token attempt for user ID: {}", userId);
              return Mono.error(new RuntimeException("Unauthorized refresh token"));
            }));
  }
}
