package fr.ailegalcase.consent;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.consent.dto.ConsentAcceptanceRequest;
import fr.ailegalcase.consent.dto.ConsentAcceptanceResponse;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * F-240 SF-240-01 : enregistre les acceptations contractuelles (click-wrap).
 *
 * <p>Validation explicite des entrées (types autorisés, version), résolution
 * de l'IP / user-agent depuis la requête HTTP, rattachement automatique au
 * workspace primary de l'utilisateur quand il existe. Chaque {@code consentType}
 * du corps produit une ligne d'audit distincte — aucune déduplication.</p>
 */
@Service
public class ConsentAcceptanceService {

    /** Types de consentement reconnus — aligné sur la contrainte CHECK de la migration 232. */
    static final Set<String> ALLOWED_CONSENT_TYPES =
            Set.of("SIGNUP_TERMS", "PRIVACY_POLICY", "PAYMENT_TERMS", "DPA_DOWNLOAD");

    private static final int MAX_CONSENT_TYPES = 10;
    private static final int MAX_VERSION_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final int MAX_IP_LENGTH = 45;

    private final UserConsentAcceptanceRepository acceptanceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ConsentAcceptanceService(UserConsentAcceptanceRepository acceptanceRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository) {
        this.acceptanceRepository = acceptanceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public ConsentAcceptanceResponse accept(ConsentAcceptanceRequest request,
                                            User user,
                                            HttpServletRequest httpRequest) {
        List<String> consentTypes = normalizeConsentTypes(request);
        String version = normalizeVersion(request);

        String ip = truncate(resolveClientIp(httpRequest), MAX_IP_LENGTH);
        String userAgent = truncate(resolveUserAgent(httpRequest), MAX_USER_AGENT_LENGTH);
        UUID workspaceId = resolvePrimaryWorkspaceId(user);
        Instant acceptedAt = Instant.now();

        List<ConsentAcceptanceResponse.Acceptance> acceptances = consentTypes.stream()
                .map(type -> {
                    UserConsentAcceptance entity = new UserConsentAcceptance();
                    entity.setUserId(user.getId());
                    entity.setConsentType(type);
                    entity.setVersion(version);
                    entity.setAcceptedAt(acceptedAt);
                    entity.setAcceptanceIp(ip);
                    entity.setAcceptanceUserAgent(userAgent);
                    entity.setWorkspaceId(workspaceId);
                    UserConsentAcceptance saved = acceptanceRepository.save(entity);
                    return new ConsentAcceptanceResponse.Acceptance(
                            saved.getId(),
                            saved.getUserId(),
                            saved.getConsentType(),
                            saved.getVersion(),
                            saved.getAcceptedAt(),
                            saved.getAcceptanceIp(),
                            saved.getAcceptanceUserAgent(),
                            saved.getWorkspaceId());
                })
                .toList();

        return new ConsentAcceptanceResponse(acceptances);
    }

    private List<String> normalizeConsentTypes(ConsentAcceptanceRequest request) {
        List<String> raw = request == null ? null : request.consentTypes();
        if (raw == null || raw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "consentTypes must contain at least one entry");
        }
        if (raw.size() > MAX_CONSENT_TYPES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "consentTypes must not exceed " + MAX_CONSENT_TYPES + " entries");
        }
        return raw.stream()
                .map(this::normalizeConsentType)
                .toList();
    }

    private String normalizeConsentType(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "consentTypes must not contain blank entries");
        }
        String normalized = value.trim().toUpperCase();
        if (!ALLOWED_CONSENT_TYPES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown consent type '" + value + "'. Allowed: "
                            + "SIGNUP_TERMS, PRIVACY_POLICY, PAYMENT_TERMS, DPA_DOWNLOAD");
        }
        return normalized;
    }

    private String normalizeVersion(ConsentAcceptanceRequest request) {
        String raw = request == null ? null : request.version();
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "version must not be blank");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_VERSION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "version must not exceed " + MAX_VERSION_LENGTH + " characters");
        }
        return trimmed;
    }

    /**
     * Résout l'IP cliente : premier élément de {@code X-Forwarded-For} (chaîné
     * par l'Ingress K8s), sinon {@code RemoteAddr}.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private String resolveUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && !userAgent.isBlank() ? userAgent : "unknown";
    }

    private UUID resolvePrimaryWorkspaceId(User user) {
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(member -> member.getWorkspace().getId())
                .orElse(null);
    }

    private String truncate(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
