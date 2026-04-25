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
 * SF-DT-30-01 : service orchestrant le calcul de régularité de la procédure
 * de licenciement d'un salarié protégé (FR — art. L.2411-1 et s.).
 */
@Service
public class ProtectionRpService {

    private final ProtectionRpRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ProtectionRpService(ProtectionRpRepository repository,
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
    public ProtectionRpResponse calculate(UUID caseFileId,
                                          ProtectionRpRequest request,
                                          OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.statutProtege() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut protégé requis");
        }
        if (request.dateExpirationMandat() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date d'expiration du mandat requise");
        }
        if (request.datePresumeeRupture() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date présumée de rupture requise");
        }
        if (request.procedureSuivie() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Procédure suivie requise");
        }
        if (request.motifLicenciement() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motif de licenciement requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        double salaireBrut = request.salaireMensuelBrutEur() != null
                ? request.salaireMensuelBrutEur() : 0.0;

        ProtectionRpResult result;
        try {
            result = ProtectionRpCalculator.compute(
                    request.statutProtege(),
                    request.dateExpirationMandat(),
                    request.datePresumeeRupture(),
                    request.procedureSuivie(),
                    request.motifLicenciement(),
                    salaireBrut,
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ProtectionRpAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ProtectionRpAnalysis a = new ProtectionRpAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setStatutProtege(result.statutProtege());
        entity.setDateExpirationMandat(result.dateExpirationMandat());
        entity.setDatePresumeeRupture(result.datePresumeeRupture());
        entity.setProcedureSuivie(result.procedureSuivie());
        entity.setMotifLicenciement(result.motifLicenciement());
        entity.setSalaireMensuelBrutEur(result.salaireMensuelBrutEur());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public ProtectionRpResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ProtectionRpAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Protection RP trouvée pour ce dossier"));
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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

    private ProtectionRpResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ProtectionRpResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ProtectionRpResponse toResponse(UUID caseFileId, ProtectionRpResult r) {
        return new ProtectionRpResponse(
                caseFileId,
                r.salarieEncoreProtege(),
                r.scoreConformite(),
                r.verdictLegalite(),
                r.criteresRemplis() != null ? r.criteresRemplis() : List.of(),
                r.criteresManquants() != null ? r.criteresManquants() : List.of(),
                r.indemniteForfaitaireMinEur(),
                r.salaireEvictionPotentielEur(),
                r.delaiContestationJours(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
