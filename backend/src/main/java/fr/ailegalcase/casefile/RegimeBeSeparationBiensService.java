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
 * SF-223-06 : service orchestrant la qualification du régime de séparation de
 * biens BE (Livre 3 CC ; loi du 22/07/2018). Gate BELGIQUE + DROIT_FAMILLE
 * (400), isolation workspace (404), upsert 1:1 par dossier (snapshot écrasé au
 * recalcul).
 */
@Service
public class RegimeBeSeparationBiensService {

    private final RegimeBeSeparationBiensRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RegimeBeSeparationBiensService(
            RegimeBeSeparationBiensRepository repository,
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
    public RegimeBeSeparationBiensResponse calculate(
            UUID caseFileId,
            RegimeBeSeparationBiensRequest request,
            OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        RegimeBeSeparationBiensResult result;
        try {
            result = RegimeBeSeparationBiensCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RegimeBeSeparationBiensResponse response =
                toResponse(caseFileId, request, result, country, Instant.now());

        RegimeBeSeparationBiensAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RegimeBeSeparationBiensAnalysis a = new RegimeBeSeparationBiensAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public RegimeBeSeparationBiensResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RegimeBeSeparationBiensAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse du régime de séparation de biens trouvée pour ce dossier"));
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

    private RegimeBeSeparationBiensResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, RegimeBeSeparationBiensResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private RegimeBeSeparationBiensResponse toResponse(
            UUID caseFileId,
            RegimeBeSeparationBiensRequest req,
            RegimeBeSeparationBiensResult r,
            String country,
            Instant calculatedAt) {
        return new RegimeBeSeparationBiensResponse(
                caseFileId,
                req.varianteRegime(),
                Boolean.TRUE.equals(req.contratMariageNotarie()),
                req.clauseParticipationPrevue(),
                Boolean.TRUE.equals(req.disproportionPatrimonialeAllegee()),
                req.dateContrat(),
                req.patrimoinePropreEpoux1Eur(),
                req.patrimoinePropreEpoux2Eur(),
                r.verdict(),
                r.motifs(),
                r.effetsPatrimoniaux(),
                r.basesJuridiques(),
                r.messages(),
                country,
                calculatedAt);
    }
}
