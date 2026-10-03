package com.microvault.auth.serviceimpl;

import com.microvault.auth.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${microvault.mail.enabled:false}")
    private boolean enabled;

    @Value("${microvault.mail.from:noreply@microvault.local}")
    private String from;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean sendOtp(String email, String otp) {
        if (!enabled) {
            log.info("Mail is disabled. Password reset code for {} was generated locally.", email);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(from);
        message.setSubject("MicroVault password reset code");
        message.setText("Your MicroVault verification code is " + otp
                + ". It expires in 10 minutes. If you did not ask for this, ignore the email.");
        mailSender.send(message);
        return true;
    }
}
