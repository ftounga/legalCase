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

/**
 * SF-DT-15-01 : service orchestrant le calcul d'indemnité de licenciement pour inaptitude (FR + BE).
 */
@Service
public class InaptitudeService {

    private final InaptitudeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public InaptitudeService(InaptitudeRepository repository,
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
    public InaptitudeResponse calculate(UUID caseFileId, InaptitudeRequest request,
                                        OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.origineInaptitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Origine de l'inaptitude requise");
        }
        if (request.ancienneteAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ancienneté requise");
        }
        if (request.reclassementRespecte() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indication de respect du reclassement requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        InaptitudeResult result;
        try {
            result = InaptitudeCalculator.compute(
                    request.salaireMensuelReference(),
                    request.ancienneteAnnees(),
                    request.origineInaptitude(),
                    request.reclassementRespecte(),
                    request.avisMedecinTravailDate(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        InaptitudeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    InaptitudeAnalysis a = new InaptitudeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setSalaireMensuelReference(result.salaireMensuelReference());
        entity.setAncienneteAnnees(result.ancienneteAnnees());
        entity.setOrigineInaptitude(result.origineInaptitude());
        entity.setReclassementRespecte(result.reclassementRespecte());
        entity.setAvisMedecinTravailDate(result.avisMedecinTravailDate());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public InaptitudeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        InaptitudeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'inaptitude trouvée pour ce dossier"));
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

    private InaptitudeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, InaptitudeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private InaptitudeResponse toResponse(UUID caseFileId, InaptitudeResult r) {
        return new InaptitudeResponse(
                caseFileId,
                r.salaireMensuelReference(),
                r.ancienneteAnnees(),
                r.origineInaptitude(),
                r.reclassementRespecte(),
                r.avisMedecinTravailDate(),
                r.country(),
                r.indemniteLegale(),
                r.indemniteCompensatricePreavis(),
                r.damagesReclassement(),
                r.total(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
