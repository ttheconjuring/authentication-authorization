package bg.softuni.demo.service;

import bg.softuni.demo.model.User;
import bg.softuni.demo.model.dto.LoginRequest;
import bg.softuni.demo.model.dto.RegisterRequest;
import bg.softuni.demo.model.dto.VerifiedUserDTO;
import bg.softuni.demo.repository.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public User register(RegisterRequest registerRequest) {
        User user = User.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .password(this.passwordEncoder.encode(registerRequest.getPassword()))
                .verificationCode(generateVerificationCode())
                .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .enabled(false)
                .build();
        sendVerificationEmail(user);
        return this.userRepository.saveAndFlush(user);
    }

    public User login(LoginRequest loginRequest) {
        User user = this.userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!user.isEnabled()) {
            throw new DisabledException("Account is not verified. Please check your email!");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        return user;
    }

    public void verifyUser(VerifiedUserDTO verifiedUserDTO) {
        Optional<User> optionalUser = this.userRepository.findByEmail(verifiedUserDTO.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            LocalDateTime verificationCodeExpiresAt = user.getVerificationCodeExpiresAt();
            if (verificationCodeExpiresAt == null) {
                throw new RuntimeException("Verification code is null! You might have already verified your account!");
            }
            if (verificationCodeExpiresAt.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired!");
            }
            if (user.getVerificationCode().equals(verifiedUserDTO.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                this.userRepository.saveAndFlush(user);
            } else {
                throw new RuntimeException("Incorrect verification code!");
            }
        } else {
            throw new RuntimeException("User not found!");
        }
    }

    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = this.userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified!");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
            sendVerificationEmail(user);
            this.userRepository.saveAndFlush(user);
        } else {
            throw new RuntimeException("User not found!");
        }
    }

    public void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String htmlMessage =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; background-color: #f4f4f9; color: #333; }" +
                        ".email-container { width: 100%; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff; border: 1px solid #ddd; border-radius: 8px; }" +
                        ".header { text-align: center; padding: 10px 0; }" +
                        ".header h1 { color: #4CAF50; }" +
                        ".content { padding: 20px; line-height: 1.6; }" +
                        ".code-container { text-align: center; font-size: 24px; font-weight: bold; padding: 15px; background-color: #f9f9f9; border: 1px dashed #4CAF50; margin-top: 20px; border-radius: 5px; color: #333; }" +
                        ".footer { text-align: center; font-size: 12px; color: #888; padding-top: 20px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='email-container'>" +
                        "    <div class='header'>" +
                        "        <h1>Welcome to Our Service!</h1>" +
                        "    </div>" +
                        "    <div class='content'>" +
                        "        <p>Hi " + user.getUsername() + ",</p>" +
                        "        <p>Thank you for registering with us! Please use the following code to verify your email address:</p>" +
                        "    </div>" +
                        "    <div class='code-container'>" +
                        "        " + user.getVerificationCode() +
                        "    </div>" +
                        "    <div class='content'>" +
                        "        <p>If you did not sign up for an account, please ignore this email.</p>" +
                        "    </div>" +
                        "    <div class='footer'>" +
                        "        <p>&copy; 2024 Your Company Name. All rights reserved.</p>" +
                        "    </div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        try {
            this.emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    public String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

}
