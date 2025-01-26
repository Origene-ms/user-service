package com.origene.userservice.service.impl;

import com.origene.userservice.enums.ActiveStatus;
import com.origene.userservice.model.User;
import com.origene.userservice.model.UserForgotPasswordToken;
import com.origene.userservice.model.UserVerificationToken;
import com.origene.userservice.repository.UserForgotPasswordTokenRepository;
import com.origene.userservice.repository.UserVerificationTokenRepository;
import com.origene.userservice.service.EmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EmailServiceImpl implements EmailService {

  private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

  @Value("${sendgrid.api.key}")
  private String sendGridApiKey;

  @Value("${app.sender.email}")
  private String senderEmail;

  @Value("${app.host}")
  private String appHost;

  private final UserVerificationTokenRepository userVerificationTokenRepository;
  private final UserForgotPasswordTokenRepository userForgotPasswordTokenRepository;

  public EmailServiceImpl(UserVerificationTokenRepository userVerificationTokenRepository, UserForgotPasswordTokenRepository userForgotPasswordTokenRepository) {
    this.userVerificationTokenRepository = userVerificationTokenRepository;
    this.userForgotPasswordTokenRepository = userForgotPasswordTokenRepository;
  }

  @Transactional
  @Override
  public Mono<Void> saveAndSendVerificationTokenEmail(User user) {
    return Mono.fromRunnable(() -> {
      // Perform email sending logic here
      try {
        logger.info("Generating verification token for user: {}", user.getEmail());

        // Create a verification token for this user
        String token = generateRandomToken();
        UserVerificationToken verificationToken = new UserVerificationToken(user, ActiveStatus.ACTIVE.name());
        userVerificationTokenRepository.save(verificationToken);

        logger.info("Verification token generated and saved for user: {}", user.getEmail());

        String subject = "Orizene Account Verification Token";
        String text = String.format("Hello %s,\n\nPlease verify your account by clicking the link: \n%s/user/confirmation?token=%s",
                user.getName(), appHost, token);

        sendEmail(user.getEmail(), subject, text);

        logger.info("Verification email sent to user: {}", user.getEmail());
      } catch (Exception e) {
        logger.error("Error occurred while saving token or sending verification email for user: {}", user.getEmail(), e);
        throw new RuntimeException("Failed to send verification email", e);
      }
    });
  }

  @Transactional
  @Override
  public Mono<Void> saveAndSendForgotPasswordTokenEmail(User user) {
    return Mono.fromRunnable(() -> {
      try {
        logger.info("Generating forgot password token for user: {}", user.getEmail());

        // Invalidate old token
        userForgotPasswordTokenRepository.findByUserIdAndActiveStatus(user.getId(), ActiveStatus.ACTIVE.name())
                .doOnNext(oldToken -> {
                  oldToken.setActiveStatus(ActiveStatus.INACTIVE.name());
                  userForgotPasswordTokenRepository.save(oldToken).block();
                  logger.info("Invalidated old forgot password token for user: {}", user.getEmail());
                }).block();

        // Create a new token
        String token = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        UserForgotPasswordToken forgotPasswordToken = new UserForgotPasswordToken(user, token, ActiveStatus.ACTIVE.name());
        userForgotPasswordTokenRepository.save(forgotPasswordToken).block();

        logger.info("Forgot password token generated and saved for user: {}", user.getEmail());

        String subject = "Orizene Forgot Password Token";
        String text = String.format("Hello %s,\n\nPlease use the following token to reset your password via app: \n%s",
                user.getName(), token);

        sendEmail(user.getEmail(), subject, text);

        logger.info("Forgot password email sent to user: {}", user.getEmail());
      } catch (Exception e) {
        logger.error("Error occurred while saving token or sending forgot password email for user: {}", user.getEmail(), e);
        throw new RuntimeException("Failed to send forgot password email", e);
      }
    });
  }

  @Override
  public Mono<Void> sendEmail(String to, String subject, String text) {
    return Mono.fromRunnable(() -> {
      Email from = new Email(senderEmail);
      Content content = new Content("text/plain", text);
      Mail mail = new Mail(from, subject, new Email(to), content);

      SendGrid sg = new SendGrid(sendGridApiKey);
      Request request = new Request();
      try {
        logger.info("Sending email to: {}", to);

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response response = sg.api(request);

        logger.info("Email sent to: {} with status code: {}", to, response.getStatusCode());
      } catch (IOException ex) {
        logger.error("Failed to send email to: {}", to, ex);
        throw new RuntimeException("Failed to send email", ex);
      }
    });
  }

  private static String generateRandomToken() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
