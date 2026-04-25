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
public class CreditTempsBeService {

    private final CreditTempsBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CreditTempsBeService(CreditTempsBeRepository repository,
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
    public CreditTempsBeResponse calculate(UUID caseFileId,
                                           CreditTempsBeRequest request,
                                           OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Crédit-temps BE uniquement (CCT 103 + AR 29/10/1997)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.regime() == null || request.regime().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "regime est requis");
        }
        if (request.ancienneteEntrepriseMois() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ancienneteEntrepriseMois est requis");
        }
        if (request.tailleEntrepriseEtp() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tailleEntrepriseEtp est requis");
        }
        if (request.dureeReductionType() == null || request.dureeReductionType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureeReductionType est requis");
        }
        if (request.ageDemandeurAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ageDemandeurAnnees est requis");
        }
        if (request.dateDemande() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateDemande est requise");
        }

        CreditTempsBeResult result;
        try {
            result = CreditTempsBeCalculator.compute(
                    request.regime(),
                    request.motif(),
                    request.ancienneteEntrepriseMois(),
                    request.tailleEntrepriseEtp(),
                    request.dureeReductionType(),
                    request.ageDemandeurAnnees(),
                    request.dateDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CreditTempsBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CreditTempsBeAnalysis a = new CreditTempsBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setRegime(request.regime());
        entity.setMotif(request.motif());
        entity.setAncienneteEntrepriseMois(request.ancienneteEntrepriseMois());
        entity.setTailleEntrepriseEtp(request.tailleEntrepriseEtp());
        entity.setDureeReductionType(request.dureeReductionType());
        entity.setAgeDemandeurAnnees(request.ageDemandeurAnnees());
        entity.setDateDemande(request.dateDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public CreditTempsBeResponse get(UUID caseFileId,
                                     OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        CreditTempsBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse crédit-temps BE trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private CreditTempsBeResult deserialize(String json) {
        try { return objectMapper.readValue(json, CreditTempsBeResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CreditTempsBeResponse toResponse(UUID caseFileId, CreditTempsBeResult r) {
        return new CreditTempsBeResponse(
                caseFileId,
                r.regime(),
                r.eligible(),
                r.scoreGlobal(),
                r.verdictEligibilite(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.indemniteOnemMensuelle(),
                r.dureeMaximaleMois(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : java.util.List.of());
    }
}
