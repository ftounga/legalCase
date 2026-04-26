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
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-05 : service orchestrant l'analyse de validité d'une donation entre
 * vifs (FR — DROIT_FAMILLE — art. 893-958 + 902-906 + 920+ Cciv).
 */
@Service
public class DonationService {

    private final DonationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DonationService(DonationRepository repository,
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
    public DonationResponse calculate(UUID caseFileId,
                                      DonationRequest request,
                                      OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.formeDonation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Forme de la donation requise");
        }
        if (request.dateDonation() == null || request.dateDonation().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date de la donation requise");
        }
        if (request.ageDonateurAns() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Âge du donateur requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        DonationResult result;
        try {
            result = DonationCalculator.compute(
                    request.formeDonation(),
                    request.dateDonation(),
                    request.ageDonateurAns(),
                    request.saineDEsprit(),
                    request.capaciteDonateur(),
                    request.capaciteRecipiendaire(),
                    request.consentementLibre(),
                    request.objetDeterminé(),
                    request.respectFormalisme(),
                    request.respectQuotiteDisponible(),
                    request.acteAuthentique(),
                    request.acceptationExpresse(),
                    request.remiseEffective(),
                    request.bienMeuble(),
                    request.intentionLiberale(),
                    request.actePrincipalNeutre(),
                    request.apparenceOnerueuse(),
                    request.prixIncoherent(),
                    request.vicesConsentementDol(),
                    request.erreurSubstantielle(),
                    request.ingratitudeAvere(),
                    request.inexecutionCharge(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DonationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DonationAnalysis a = new DonationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setFormeDonation(result.formeDonation());
        entity.setDateDonation(result.dateDonation());
        entity.setAgeDonateurAns(result.ageDonateurAns());
        entity.setVerdictValidite(result.verdictValidite());
        entity.setActionEnReductionPossible(result.actionEnReductionPossible());
        entity.setRevocationPossible(result.revocationPossible());
        entity.setScoreEligibilite(result.scoreEligibilite());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public DonationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        DonationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de donation trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private DonationResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, DonationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private DonationResponse toResponse(UUID caseFileId, DonationResult r) {
        return new DonationResponse(
                caseFileId,
                r.formeDonation(),
                r.verdictValidite(),
                r.risquesRequalification() != null ? r.risquesRequalification() : List.of(),
                r.actionEnReductionPossible(),
                r.revocationPossible(),
                r.delaiContestationAns(),
                r.scoreEligibilite(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
