package com.origene.userservice.service;

import com.origene.userservice.enums.ActiveStatus;
import com.origene.userservice.model.User;
import com.origene.userservice.model.UserForgotPasswordToken;
import com.origene.userservice.model.UserVerificationToken;
import com.origene.userservice.repository.UserForgotPasswordTokenRepository;
import com.origene.userservice.repository.UserVerificationTokenRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

@Component
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${app.sender.email}")
    private String senderEmail;

    @Value("${app.host}")
    private String appHost;

    private final UserVerificationTokenRepository userVerificationTokenRepository;
    private final UserForgotPasswordTokenRepository userForgotPasswordTokenRepository;

    public EmailService(UserVerificationTokenRepository userVerificationTokenRepository, UserForgotPasswordTokenRepository userForgotPasswordTokenRepository) {
        this.userVerificationTokenRepository = userVerificationTokenRepository;
        this.userForgotPasswordTokenRepository = userForgotPasswordTokenRepository;
    }

    @Transactional
    public void saveAndSendVerificationTokenEmail(User user) {
        try {
            // Create a verification token for this user
            String token = generateRandomToken();
            UserVerificationToken verificationToken = new UserVerificationToken(user, ActiveStatus.ACTIVE.name());
            userVerificationTokenRepository.save(verificationToken);

            String subject = "Orizeen Account Verification Token";
            String text = String.format("Hello %s,\n\nPlease verify your account by clicking the link: \n%s/user/confirmation?token=%s", user.getName(), appHost, token);

            sendEmail(user.getEmail(), subject, text);
        } catch (Exception e) {
            // Handle the exception (e.g., log the error, retry, etc.)
            System.err.println("Error occurred while saving token or sending email: " + e.getMessage());
            // Optionally, rethrow the exception or handle it as needed
        }
    }

    @Transactional
    public void saveAndSendForgotPasswordTokenEmail(User user) {
        try {
            // Invalidate old token
            userForgotPasswordTokenRepository.findByUserIdAndActiveStatus(user.getId(), ActiveStatus.ACTIVE.name())
                    .doOnNext(oldToken -> {
                        oldToken.setActiveStatus(ActiveStatus.INACTIVE.name());
                        userForgotPasswordTokenRepository.save(oldToken).block();
                    }).block();

            // Create a new token
            String token = String.valueOf(100000 + new SecureRandom().nextInt(900000));
            UserForgotPasswordToken forgotPasswordToken = new UserForgotPasswordToken(user, token, ActiveStatus.ACTIVE.name());
            userForgotPasswordTokenRepository.save(forgotPasswordToken).block();

            String subject = "Orizene Forgot Password Token";
            String text = String.format("Hello %s,\n\nPlease use the following token to reset your password via app: \n%s", user.getName(), token);

            sendEmail(user.getEmail(), subject, text);
        } catch (Exception e) {
            // Handle the exception (e.g., log the error, retry, etc.)
            System.err.println("Error occurred while saving token or sending email: " + e.getMessage());
            // Optionally, rethrow the exception or handle it as needed
        }
    }

    public void sendEmail(String to, String subject, String text) throws IOException {
        Email from = new Email(senderEmail);
        Email toEmail = new Email(to);
        Content content = new Content("text/plain", text);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        sg.api(request);
    }

    private static String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Arrays.toString(bytes);
    }

}