package fr.ailegalcase.casefile.jurisprudence;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-242 / SF-242-01 — CRUD des citations de jurisprudence d'appui d'un dossier.
 *
 * <p>Toutes les opérations résolvent d'abord le dossier dans le workspace primaire de
 * l'utilisateur courant : un dossier inconnu ou appartenant à un autre workspace
 * renvoie {@code 404} (pas de fuite d'existence — même pattern que
 * {@code CaseConclusionCommandService}).</p>
 */
@Service
public class JurisprudenceCitationService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceCitationService.class);

    private static final int MAX_REFERENCE_LENGTH = 255;
    private static final int MAX_PORTEE_LENGTH = 500;
    private static final int MAX_POINT_JURIDIQUE_TEXTE_LENGTH = 2000;

    private final CaseFileRepository caseFileRepository;
    private final JurisprudenceCitationRepository citationRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public JurisprudenceCitationService(CaseFileRepository caseFileRepository,
                                        JurisprudenceCitationRepository citationRepository,
                                        CurrentUserResolver currentUserResolver,
                                        WorkspaceMemberRepository workspaceMemberRepository) {
        this.caseFileRepository = caseFileRepository;
        this.citationRepository = citationRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    /**
     * Liste les citations d'un dossier, regroupées par point juridique (ordre stable).
     *
     * @throws ResponseStatusException {@code 404} si le dossier est inconnu ou
     *                                 appartient à un autre workspace
     */
    @Transactional(readOnly = true)
    public JurisprudenceCitationListResponse list(UUID caseFileId, OidcUser oidcUser,
                                                  String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        resolveCaseFileInWorkspace(caseFileId, workspace);

        List<JurisprudenceCitationResponse> citations = citationRepository
                .findByCaseFileIdOrderByPointJuridiqueIndexAscCreatedAtAsc(caseFileId)
                .stream()
                .map(JurisprudenceCitationResponse::from)
                .toList();
        return new JurisprudenceCitationListResponse(citations);
    }

    /**
     * Crée une citation de jurisprudence d'appui rattachée à un point juridique du
     * dossier.
     *
     * @throws ResponseStatusException {@code 400} si la requête est invalide ;
     *                                 {@code 404} si le dossier est inconnu ou
     *                                 appartient à un autre workspace
     */
    @Transactional
    public JurisprudenceCitationResponse create(UUID caseFileId,
                                                CreateJurisprudenceCitationRequest request,
                                                OidcUser oidcUser, String provider,
                                                Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        CaseFile caseFile = resolveCaseFileInWorkspace(caseFileId, workspace);

        if (request == null) {
            throw badRequest("Le corps de la requête est requis.");
        }
        Integer index = request.pointJuridiqueIndex();
        if (index == null || index < 0) {
            throw badRequest("Le champ pointJuridiqueIndex est requis et doit être ≥ 0.");
        }
        String pointTexte = normalizeRequired(request.pointJuridiqueTexte(),
                "Le champ pointJuridiqueTexte est requis.", MAX_POINT_JURIDIQUE_TEXTE_LENGTH,
                "Le champ pointJuridiqueTexte ne peut pas dépasser "
                        + MAX_POINT_JURIDIQUE_TEXTE_LENGTH + " caractères.");
        String reference = normalizeRequired(request.reference(),
                "Le champ reference est requis.", MAX_REFERENCE_LENGTH,
                "Le champ reference ne peut pas dépasser " + MAX_REFERENCE_LENGTH + " caractères.");
        String portee = normalizeOptionalPortee(request.portee());

        JurisprudenceCitation citation = new JurisprudenceCitation();
        citation.setCaseFile(caseFile);
        citation.setWorkspace(workspace);
        citation.setPointJuridiqueIndex(index);
        citation.setPointJuridiqueTexte(pointTexte);
        citation.setReference(reference);
        citation.setPortee(portee);
        citation = citationRepository.save(citation);
        log.info("Jurisprudence citation created — caseFile={}, citation={}, point={}",
                caseFileId, citation.getId(), index);
        return JurisprudenceCitationResponse.from(citation);
    }

    /**
     * Met à jour la référence et la portée d'une citation existante.
     *
     * @throws ResponseStatusException {@code 400} si la requête est invalide ;
     *                                 {@code 404} si le dossier ou la citation est
     *                                 inconnu ou appartient à un autre workspace
     */
    @Transactional
    public JurisprudenceCitationResponse update(UUID caseFileId, UUID citationId,
                                                UpdateJurisprudenceCitationRequest request,
                                                OidcUser oidcUser, String provider,
                                                Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        resolveCaseFileInWorkspace(caseFileId, workspace);
        JurisprudenceCitation citation = resolveCitation(caseFileId, citationId);

        if (request == null) {
            throw badRequest("Le corps de la requête est requis.");
        }
        String reference = normalizeRequired(request.reference(),
                "Le champ reference est requis.", MAX_REFERENCE_LENGTH,
                "Le champ reference ne peut pas dépasser " + MAX_REFERENCE_LENGTH + " caractères.");
        String portee = normalizeOptionalPortee(request.portee());

        citation.setReference(reference);
        citation.setPortee(portee);
        citation = citationRepository.save(citation);
        log.info("Jurisprudence citation updated — caseFile={}, citation={}", caseFileId, citationId);
        return JurisprudenceCitationResponse.from(citation);
    }

    /**
     * Supprime une citation d'un dossier.
     *
     * @throws ResponseStatusException {@code 404} si le dossier ou la citation est
     *                                 inconnu ou appartient à un autre workspace
     */
    @Transactional
    public void delete(UUID caseFileId, UUID citationId, OidcUser oidcUser,
                       String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        resolveCaseFileInWorkspace(caseFileId, workspace);
        JurisprudenceCitation citation = resolveCitation(caseFileId, citationId);
        citationRepository.delete(citation);
        log.info("Jurisprudence citation deleted — caseFile={}, citation={}", caseFileId, citationId);
    }

    /** Valide un champ texte requis : non vide après strip, longueur bornée. */
    private static String normalizeRequired(String value, String requiredMessage,
                                            int maxLength, String tooLongMessage) {
        if (value == null || value.isBlank()) {
            throw badRequest(requiredMessage);
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw badRequest(tooLongMessage);
        }
        return trimmed;
    }

    /** Normalise une portée optionnelle : {@code null} si vide, longueur bornée sinon. */
    private static String normalizeOptionalPortee(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > MAX_PORTEE_LENGTH) {
            throw badRequest("Le champ portee ne peut pas dépasser "
                    + MAX_PORTEE_LENGTH + " caractères.");
        }
        return trimmed;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private Workspace resolveWorkspace(OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }

    /** Charge le dossier et vérifie l'isolation workspace — 404 (pas de fuite d'existence). */
    private CaseFile resolveCaseFileInWorkspace(UUID caseFileId, Workspace workspace) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }

    /**
     * Charge une citation par id en exigeant son rattachement au dossier — 404 sinon.
     * Le dossier ayant déjà été contrôlé pour le workspace, l'isolation est garantie.
     */
    private JurisprudenceCitation resolveCitation(UUID caseFileId, UUID citationId) {
        return citationRepository.findByIdAndCaseFileId(citationId, caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Citation not found"));
    }
}
