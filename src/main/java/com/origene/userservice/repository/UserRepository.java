package com.origene.userservice.repository;

import com.origene.userservice.model.User;
import lombok.NonNull;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByEmail(String email);
    @NonNull
    Mono<User> findById(@NonNull String id);
    @NonNull
    Flux<User> findAllById(@NonNull Iterable<String> ids);
    Mono<User> findByEmailAndActiveStatus(String email, String activeStatus);
    Mono<User> findByEmailAndActiveStatusIn(String email, List<String> statuses);
}