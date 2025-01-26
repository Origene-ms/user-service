package com.origene.userservice.service;

import com.origene.userservice.model.User;
import reactor.core.publisher.Mono;

public interface EmailService {
    Mono<Void> saveAndSendVerificationTokenEmail(User user);
    Mono<Void> saveAndSendForgotPasswordTokenEmail(User user);
    Mono<Void> sendEmail(String to, String subject, String text);
}
