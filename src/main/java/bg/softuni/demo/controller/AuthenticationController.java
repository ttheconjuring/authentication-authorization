package bg.softuni.demo.controller;

import bg.softuni.demo.model.User;
import bg.softuni.demo.model.dto.LoginRequest;
import bg.softuni.demo.model.dto.LoginResponse;
import bg.softuni.demo.model.dto.RegisterRequest;
import bg.softuni.demo.model.dto.VerifiedUserDTO;
import bg.softuni.demo.service.AuthenticationService;
import bg.softuni.demo.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok().body(this.authenticationService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        User authenticatedUser = this.authenticationService.login(loginRequest);
        String token = this.jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .expiresIn(this.jwtService.getJwtExpiration())
                .build();
        return ResponseEntity.ok().body(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifiedUserDTO verifiedUserDTO) {
        try {
            this.authenticationService.verifyUser(verifiedUserDTO);
            return ResponseEntity.ok("Account verified successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resend(@RequestParam String email) {
        try {
            this.authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok().body("Verification code resend successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
