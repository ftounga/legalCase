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
 * SF-212-27 : service orchestrant l'évaluation des chances de
 * reconnaissance du burn-out comme maladie professionnelle hors tableau
 * (F-DT-64-burnout-reconnaissance-mp, FRANCE — L. 461-1 al. 4 et 5 CSS).
 * Persistance snapshot — un seul résultat courant par dossier
 * (UNIQUE(case_file_id) côté migration 341).
 */
@Service
public class BurnoutReconnaissanceMpService {

    private final BurnoutReconnaissanceMpRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public BurnoutReconnaissanceMpService(BurnoutReconnaissanceMpRepository repository,
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
    public BurnoutReconnaissanceMpResponse calculate(UUID caseFileId,
                                                     BurnoutReconnaissanceMpRequest request,
                                                     OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        BurnoutReconnaissanceMpCalculator.Result result;
        try {
            result = BurnoutReconnaissanceMpCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("FRANCE")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        BurnoutReconnaissanceMpResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        BurnoutReconnaissanceMpAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    BurnoutReconnaissanceMpAnalysis a = new BurnoutReconnaissanceMpAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public BurnoutReconnaissanceMpResponse get(UUID caseFileId,
                                               OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        BurnoutReconnaissanceMpAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse burn-out / reconnaissance MP trouvée pour ce dossier"));
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

    private BurnoutReconnaissanceMpResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, BurnoutReconnaissanceMpResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private BurnoutReconnaissanceMpResponse toResponse(UUID caseFileId,
                                                       BurnoutReconnaissanceMpRequest req,
                                                       BurnoutReconnaissanceMpCalculator.Result r,
                                                       Instant calculatedAt) {
        return new BurnoutReconnaissanceMpResponse(
                caseFileId,
                req.diagnosticBurnoutPose(),
                req.tauxIPPEstime(),
                req.anneesExpositionProfessionnelle(),
                req.surchargeChargeDocumentee(),
                req.manquementsSecuriteDocumentes(),
                req.harcelementConcomitant(),
                req.arretsMaladieMultiples(),
                req.lienCausalDirectEtabli(),
                r.analyseChancesCRRMP(),
                r.scoreChances(),
                r.conditionIPPRemplie(),
                r.alerteIPPInsuffisante(),
                r.delaiInstructionMois(),
                r.facteursDossier(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
