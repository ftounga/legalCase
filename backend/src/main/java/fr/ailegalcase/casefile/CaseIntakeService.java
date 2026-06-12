package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * F-285 / SF-285-01 — qualification d'entrée du dossier (intake guidé).
 *
 * <p>Lecture / mise à jour des métadonnées de qualification d'un dossier (type de
 * litige, recevabilité, prescription, juridiction, valeur estimée). Isolation
 * workspace via le dossier (mirror {@link ContradictoireService}). Pose
 * {@code intakeQualifiedAt} au 1ᵉʳ enregistrement qualifiant et le préserve ensuite.
 *
 * <p>F-285 n'est <b>pas</b> un outil décisionnel : aucune simulation, aucun
 * pré-fill IA, pas de {@code TOOL_REGISTRY}. C'est un encart de métadonnée du dossier.
 */
@Service
public class CaseIntakeService {

    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public CaseIntakeService(CaseFileRepository caseFileRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             CurrentUserResolver currentUserResolver) {
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public CaseIntakeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        return CaseIntakeResponse.from(caseFile);
    }

    @Transactional
    public CaseIntakeResponse update(UUID caseFileId, CaseIntakeRequest request,
                                     OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        validate(request);

        caseFile.setIntakeDisputeType(trimToNull(request.disputeType()));
        caseFile.setIntakeAdmissibility(request.admissibility());
        caseFile.setIntakePrescriptionStatus(request.prescriptionStatus());
        caseFile.setIntakePrescriptionDeadline(request.prescriptionDeadline());
        caseFile.setIntakeJurisdiction(trimToNull(request.jurisdiction()));
        caseFile.setIntakeEstimatedValueCents(request.estimatedValueCents());
        caseFile.setIntakeNotes(trimToNull(request.notes()));

        // Horodatage de la 1ʳᵉ qualification : posé une fois, préservé ensuite.
        if (caseFile.getIntakeQualifiedAt() == null && CaseIntakeResponse.from(caseFile).qualified()) {
            caseFile.setIntakeQualifiedAt(Instant.now());
        }

        return CaseIntakeResponse.from(caseFileRepository.save(caseFile));
    }

    private void validate(CaseIntakeRequest request) {
        if (request.estimatedValueCents() != null && request.estimatedValueCents() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "estimatedValueCents must be zero or positive");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        return caseFile;
    }
}
