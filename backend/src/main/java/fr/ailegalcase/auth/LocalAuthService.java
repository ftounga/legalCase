package fr.ailegalcase.auth;

import fr.ailegalcase.workspace.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LocalAuthService {

    private static final Logger log = LoggerFactory.getLogger(LocalAuthService.class);

    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public LocalAuthService(UserRepository userRepository,
                            AuthAccountRepository authAccountRepository,
                            EmailVerificationTokenRepository emailVerificationTokenRepository,
                            PasswordResetTokenRepository passwordResetTokenRepository,
                            PasswordEncoder passwordEncoder,
                            EmailService emailService) {
        this.userRepository = userRepository;
        this.authAccountRepository = authAccountRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    private static final String INVALID_CREDENTIALS = "Identifiants invalides.";
    private static final HttpSessionSecurityContextRepository SESSION_REPO =
            new HttpSessionSecurityContextRepository();

    @Transactional(readOnly = true)
    public MeResponse login(LocalLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.email().toLowerCase().trim();

        AuthAccount account = authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        if (!account.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Veuillez valider votre email avant de vous connecter.");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        SESSION_REPO.saveContext(context, httpRequest, httpResponse);

        User user = account.getUser();
        return new MeResponse(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), "LOCAL", user.isSuperAdmin());
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet email est déjà utilisé. Si vous avez un compte Google ou Microsoft associé, connectez-vous via le bouton correspondant.");
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("LOCAL");
        account.setProviderUserId(email);
        account.setProviderEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setEmailVerified(false);
        authAccountRepository.save(account);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(token);
        verificationToken.setExpiresAt(Instant.now().plusSeconds(86400));
        emailVerificationTokenRepository.save(verificationToken);

        try {
            emailService.sendEmailVerification(email, token);
        } catch (Exception e) {
            log.error("Failed to send verification email to {} — inscription réussie malgré tout", email, e);
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase().trim();

        authAccountRepository.findByProviderAndProviderUserId("LOCAL", email).ifPresent(account -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(account.getUser());
            resetToken.setToken(token);
            resetToken.setExpiresAt(Instant.now().plusSeconds(86400));
            passwordResetTokenRepository.save(resetToken);

            try {
                emailService.sendPasswordReset(email, token);
            } catch (Exception e) {
                log.error("Failed to send password reset email to {} — demande enregistrée malgré tout", email, e);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide ou inconnu."));

        if (resetToken.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien de réinitialisation a déjà été utilisé.");
        }

        if (Instant.now().isAfter(resetToken.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien de réinitialisation a expiré.");
        }

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        AuthAccount account = authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", resetToken.getUser().getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compte associé introuvable."));
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        authAccountRepository.save(account);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide ou inconnu."));

        if (verificationToken.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien de validation a déjà été utilisé.");
        }

        if (Instant.now().isAfter(verificationToken.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien de validation a expiré. Veuillez vous réinscrire.");
        }

        verificationToken.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(verificationToken);

        AuthAccount account = authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", verificationToken.getUser().getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compte associé introuvable."));
        account.setEmailVerified(true);
        authAccountRepository.save(account);
    }
}
