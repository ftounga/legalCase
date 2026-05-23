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
 * SF-212-03 : service orchestrant l'analyse de validité du forfait jours
 * (FRANCE — L. 3121-58 à L. 3121-66 CT ; Cass. soc. 29/06/2011 n°09-71.107)
 * + persistance snapshot (un seul résultat courant par dossier).
 */
@Service
public class ForfaitJoursValiditeService {

    private final ForfaitJoursValiditeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ForfaitJoursValiditeService(ForfaitJoursValiditeRepository repository,
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
    public ForfaitJoursValiditeResponse calculate(UUID caseFileId,
                                                   ForfaitJoursValiditeRequest request,
                                                   OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        ForfaitJoursValiditeCalculator.Result result;
        try {
            result = ForfaitJoursValiditeCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("FRANCE")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ForfaitJoursValiditeResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        ForfaitJoursValiditeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ForfaitJoursValiditeAnalysis a = new ForfaitJoursValiditeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ForfaitJoursValiditeResponse get(UUID caseFileId,
                                            OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ForfaitJoursValiditeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de validité de forfait jours trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce dossier appartient à un autre workspace");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private ForfaitJoursValiditeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ForfaitJoursValiditeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ForfaitJoursValiditeResponse toResponse(UUID caseFileId,
                                                     ForfaitJoursValiditeRequest req,
                                                     ForfaitJoursValiditeCalculator.Result r,
                                                     Instant calculatedAt) {
        return new ForfaitJoursValiditeResponse(
                caseFileId,
                req.accordCollectifExiste(),
                req.accordGarantitSuiviCharge(),
                req.entretienAnnuelRealise(),
                req.documentControleMensuelExiste(),
                req.categorieAutonomeConfirmee(),
                req.nbJoursForfait(),
                req.ancienneteMois(),
                req.salaireMensuelBrutEuros(),
                req.hsEstimeesParSemaine(),
                req.nbSemainesParAn(),
                r.validiteForfait(),
                r.scoreValidite(),
                r.facteursInvalidite(),
                r.rappelHsEstimeEuros(),
                r.prescriptionRappelAns(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
