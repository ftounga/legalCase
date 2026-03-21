package fr.ailegalcase.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class LocalAuthController {

    private final LocalAuthService localAuthService;

    public LocalAuthController(LocalAuthService localAuthService) {
        this.localAuthService = localAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<MeResponse> login(@Valid @RequestBody LocalLoginRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        MeResponse response = localAuthService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        localAuthService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "Si un compte existe pour cet email, vous recevrez un lien de réinitialisation."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        localAuthService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès."));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        localAuthService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        localAuthService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email validé avec succès."));
    }
}
