package fr.ailegalcase.email;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * F-248 SF-248-01 : gestion du désabonnement des emails non-transactionnels.
 *
 * <p>L'accès est public et autorisé par le seul {@code marketingUnsubscribeToken} :
 * un token n'identifie qu'un seul utilisateur, il fait office d'autorisation.
 * Le service ne dépend d'aucun {@code Principal}.
 */
@Service
public class EmailSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(EmailSubscriptionService.class);

    private final UserRepository userRepository;

    public EmailSubscriptionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Désinscrit l'utilisateur porteur du token des emails non-transactionnels.
     * Idempotent : un utilisateur déjà désinscrit reste désinscrit, sans erreur.
     *
     * @return l'état d'opt-out après l'opération (toujours {@code true})
     */
    @Transactional
    public boolean unsubscribe(String token) {
        User user = resolveByToken(token);
        if (!user.isMarketingEmailsOptedOut()) {
            user.setMarketingEmailsOptedOut(true);
            userRepository.save(user);
            log.info("User {} unsubscribed from marketing emails", user.getId());
        } else {
            log.info("User {} already unsubscribed from marketing emails — no-op", user.getId());
        }
        return user.isMarketingEmailsOptedOut();
    }

    /**
     * Réinscrit l'utilisateur porteur du token aux emails non-transactionnels.
     * Idempotent.
     *
     * @return l'état d'opt-out après l'opération (toujours {@code false})
     */
    @Transactional
    public boolean resubscribe(String token) {
        User user = resolveByToken(token);
        if (user.isMarketingEmailsOptedOut()) {
            user.setMarketingEmailsOptedOut(false);
            userRepository.save(user);
            log.info("User {} resubscribed to marketing emails", user.getId());
        } else {
            log.info("User {} already subscribed to marketing emails — no-op", user.getId());
        }
        return user.isMarketingEmailsOptedOut();
    }

    /**
     * Renvoie l'état d'opt-out courant de l'utilisateur porteur du token.
     *
     * @return {@code true} si l'utilisateur est désinscrit
     */
    @Transactional(readOnly = true)
    public boolean subscriptionStatus(String token) {
        return resolveByToken(token).isMarketingEmailsOptedOut();
    }

    /**
     * Résout l'utilisateur à partir du token brut.
     *
     * @throws ResponseStatusException 400 si le token est absent / mal formé,
     *                                 404 si le token est inconnu en base.
     */
    private User resolveByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token requis");
        }
        UUID parsedToken;
        try {
            parsedToken = UUID.fromString(token.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide");
        }
        return userRepository.findByMarketingUnsubscribeToken(parsedToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lien de désinscription invalide"));
    }
}
