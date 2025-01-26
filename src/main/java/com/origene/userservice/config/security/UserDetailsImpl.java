package com.origene.userservice.config.security;

import com.origene.userservice.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom implementation of Spring Security's UserDetails interface.
 * Represents a user's details for authentication and authorization.
 */
public class UserDetailsImpl implements UserDetails {

  @Getter
  private final String id;
  private final String email;
  private final String password;
  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * Private constructor for UserDetailsImpl.
   */
  private UserDetailsImpl(String id, String email, String password, Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.authorities = authorities;
  }

  /**
   * Static factory method to build a UserDetailsImpl object from a User entity.
   *
   * @param user The User entity.
   * @return A UserDetailsImpl instance.
   */
  public static UserDetailsImpl build(User user) {
    if (user == null) {
      throw new IllegalArgumentException("User cannot be null.");
    }

    List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.name()))
            .collect(Collectors.toList());

    return new UserDetailsImpl(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            authorities
    );
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  /**
   * Account is considered non-expired by default.
   * Customize this logic if necessary.
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * Account is considered non-locked by default.
   * Customize this logic if necessary.
   */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /**
   * Credentials are considered non-expired by default.
   * Customize this logic if necessary.
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * Account is enabled by default.
   * Customize this logic if necessary.
   */
  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String toString() {
    return "UserDetailsImpl{" +
            "id='" + id + '\'' +
            ", email='" + email + '\'' +
            ", authorities=" + authorities +
            '}';
  }
}