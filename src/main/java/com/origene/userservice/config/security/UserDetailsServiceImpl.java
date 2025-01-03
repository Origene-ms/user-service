package com.origene.userservice.config.security;

import com.origene.userservice.model.User;
import com.origene.userservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
            .switchIfEmpty(Mono.error(new UsernameNotFoundException("User Not Found with email: " + email)))
            .block(); // Blocking here is safe for authentication purposes

        assert user != null;
        return UserDetailsImpl.build(user);
  }
}
