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
import java.util.UUID;

@Service
public class Belgian40terService {

    private final Belgian40terRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public Belgian40terService(Belgian40terRepository repository,
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
    public Belgian40terResponse calculate(UUID caseFileId,
                                          Belgian40terRequest request,
                                          OidcUser oidcUser,
                                          Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime 40ter propre à la Belgique (Loi 15/12/1980 art. 40ter)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        if (request.lienFamilial() == null || request.lienFamilial().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "lienFamilial est requis");
        }
        if (request.revenusMensuelsNetsEur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "revenusMensuelsNetsEur est requis");
        }

        boolean regroupantBelge = Boolean.TRUE.equals(request.regroupantBelge());
        boolean assuranceMaladie = Boolean.TRUE.equals(request.assuranceMaladie());
        boolean logementSuffisant = Boolean.TRUE.equals(request.logementSuffisant());
        boolean menaceOrdrePublic = Boolean.TRUE.equals(request.menaceOrdrePublic());

        Belgian40terResult result;
        try {
            result = Belgian40terCalculator.compute(
                    request.lienFamilial(),
                    regroupantBelge,
                    request.revenusMensuelsNetsEur(),
                    request.seuil120PctRisEur(),
                    assuranceMaladie,
                    logementSuffisant,
                    menaceOrdrePublic,
                    request.dateDepotDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Belgian40terAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    Belgian40terAnalysis a = new Belgian40terAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setLienFamilial(result.lienFamilial());
        entity.setRegroupantBelge(result.regroupantBelge());
        entity.setRevenusMensuelsNetsEur(result.revenusMensuelsNetsEur());
        entity.setSeuil120PctRisEur(result.seuil120PctRisEur());
        entity.setAssuranceMaladie(result.assuranceMaladie());
        entity.setLogementSuffisant(result.logementSuffisant());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setDateDepotDemande(result.dateDepotDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public Belgian40terResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        Belgian40terAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse 40ter familial Belge BE trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private Belgian40terResult deserialize(String json) {
        try { return objectMapper.readValue(json, Belgian40terResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private Belgian40terResponse toResponse(UUID caseFileId, String country,
                                            Belgian40terResult r) {
        return new Belgian40terResponse(
                caseFileId,
                r.lienFamilial(),
                r.regroupantBelge(),
                r.revenusMensuelsNetsEur(),
                r.seuil120PctRisEur(),
                r.assuranceMaladie(),
                r.logementSuffisant(),
                r.menaceOrdrePublic(),
                r.dateDepotDemande(),
                country,
                r.lienValide(),
                r.regroupantBelgeOk(),
                r.revenusSuffisantsOk(),
                r.assuranceOk(),
                r.logementOk(),
                r.pasMenace(),
                r.differentielRevenus(),
                r.scoreGlobal(),
                r.verdictProbabiliteAcceptation(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.dateExpirationInstructionSiDemande(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
