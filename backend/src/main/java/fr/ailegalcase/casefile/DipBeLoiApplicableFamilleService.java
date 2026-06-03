package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * SF-223-07 : service orchestrant la détermination de la loi applicable en droit
 * de la famille BE (DIP : Rome III ; Règl. UE 2016/1103 ; Règl. UE 650/2012).
 * Gate BELGIQUE + DROIT_FAMILLE (400), validation ISO-2 / date (400), isolation
 * workspace (404), upsert 1:1 par dossier (snapshot écrasé au recalcul).
 */
@Service
public class DipBeLoiApplicableFamilleService {

    private final DipBeLoiApplicableFamilleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DipBeLoiApplicableFamilleService(
            DipBeLoiApplicableFamilleRepository repository,
            CaseFileRepository caseFileRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            CurrentUserResolver currentUserResolver,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DipBeLoiApplicableFamilleResponse calculate(
            UUID caseFileId,
            DipBeLoiApplicableFamilleRequest request,
            OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        DipBeLoiApplicableFamilleResult result;
        try {
            result = DipBeLoiApplicableFamilleCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DipBeLoiApplicableFamilleResponse response =
                toResponse(caseFileId, request, result, country, Instant.now());

        DipBeLoiApplicableFamilleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DipBeLoiApplicableFamilleAnalysis a = new DipBeLoiApplicableFamilleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public DipBeLoiApplicableFamilleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        DipBeLoiApplicableFamilleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de loi applicable (DIP famille) trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    /**
     * Résout le dossier : 404 dossier inexistant / d'un autre workspace, 400
     * domaine != DROIT_FAMILLE. Le gate pays (BELGIQUE) est appliqué par le
     * Calculator (400).
     */
    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable");
        }
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille.");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation.");
        }
    }

    private DipBeLoiApplicableFamilleResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, DipBeLoiApplicableFamilleResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private DipBeLoiApplicableFamilleResponse toResponse(
            UUID caseFileId,
            DipBeLoiApplicableFamilleRequest req,
            DipBeLoiApplicableFamilleResult r,
            String country,
            Instant calculatedAt) {
        return new DipBeLoiApplicableFamilleResponse(
                caseFileId,
                req.matiere(),
                req.residenceHabituelleCommune(),
                req.nationaliteCommune(),
                req.choixLoiParLesParties(),
                req.dateMariageOuDeces(),
                req.lieuSituationBiensImmobiliers(),
                r.verdict(),
                r.loiApplicableDeterminee(),
                r.fondementRattachement(),
                r.motifs(),
                r.conseils(),
                r.basesJuridiques(),
                r.messages(),
                country,
                calculatedAt);
    }
}
