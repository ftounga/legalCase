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
 * SF-FA-24-13 : service orchestrant le calcul du rapport à succession (FR —
 * DROIT_FAMILLE — art. 843-863 + 919 Cciv).
 */
@Service
public class RapportSuccessionService {

    private final RapportSuccessionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RapportSuccessionService(RapportSuccessionRepository repository,
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
    public RapportSuccessionResponse calculate(UUID caseFileId,
                                               RapportSuccessionRequest request,
                                               OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.donationsRecuesEur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "donationsRecuesEur requis");
        }
        if (request.valeurAuJourPartage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "valeurAuJourPartage requise");
        }
        if (request.dateDonation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateDonation requise");
        }
        if (request.qualiteHeritier() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "qualiteHeritier requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        RapportSuccessionResult result;
        try {
            result = RapportSuccessionCalculator.compute(
                    request.donationsRecuesEur(),
                    request.dateDonation(),
                    request.valeurAuJourPartage(),
                    request.donationDispenseDeRapport(),
                    request.naturePresumeeNonRapportable(),
                    request.qualiteHeritier(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RapportSuccessionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RapportSuccessionAnalysis a = new RapportSuccessionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDonationsRecuesEur(result.donationsRecuesEur());
        entity.setDateDonation(result.dateDonation());
        entity.setValeurAuJourPartage(result.valeurAuJourPartage());
        entity.setDonationDispenseDeRapport(result.donationDispenseDeRapport());
        entity.setNaturePresumeeNonRapportable(result.naturePresumeeNonRapportable());
        entity.setQualiteHeritier(result.qualiteHeritier());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public RapportSuccessionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RapportSuccessionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Rapport à succession trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
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

    private RapportSuccessionResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, RapportSuccessionResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private RapportSuccessionResponse toResponse(UUID caseFileId, RapportSuccessionResult r) {
        return new RapportSuccessionResponse(
                caseFileId,
                r.donationsRecuesEur(),
                r.dateDonation(),
                r.valeurAuJourPartage(),
                r.donationDispenseDeRapport(),
                r.naturePresumeeNonRapportable(),
                r.qualiteHeritier(),
                r.verdictObligation(),
                r.modeRapportRecommande(),
                r.montantRapportable(),
                r.delaiPrescriptionAns(),
                r.scoreEligibilite(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
