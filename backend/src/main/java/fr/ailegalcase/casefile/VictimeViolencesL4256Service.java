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
public class VictimeViolencesL4256Service {

    private final VictimeViolencesL4256Repository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public VictimeViolencesL4256Service(VictimeViolencesL4256Repository repository,
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
    public VictimeViolencesL4256Response calculate(UUID caseFileId,
                                                   VictimeViolencesL4256Request request,
                                                   OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Titre L.425-6 — outil FRANCE uniquement (mécanisme spécifique français)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        int dureeProtectionMois = request.dureeProtectionMois() != null
                ? request.dureeProtectionMois()
                : VictimeViolencesL4256Analyzer.DUREE_PROTECTION_DEFAUT_MOIS;
        int enfantsAcharge = request.enfantsAcharge() != null ? request.enfantsAcharge() : 0;

        VictimeViolencesL4256Result result;
        try {
            result = VictimeViolencesL4256Analyzer.analyze(
                    request.dateOrdonnanceProtection(),
                    request.juridiction(),
                    dureeProtectionMois,
                    request.dateExpirationProtection(),
                    enfantsAcharge,
                    request.nationalite());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        VictimeViolencesL4256Analysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    VictimeViolencesL4256Analysis a = new VictimeViolencesL4256Analysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateOrdonnanceProtection(result.dateOrdonnanceProtection());
        entity.setJuridiction(result.juridiction());
        entity.setDureeProtectionMois(result.dureeProtectionMois());
        entity.setDateExpirationProtection(result.dateExpirationProtectionEffective());
        entity.setEnfantsAcharge(result.enfantsAcharge());
        entity.setNationalite(result.nationalite());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public VictimeViolencesL4256Response get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        VictimeViolencesL4256Analysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse L.425-6 trouvée pour ce dossier"));
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private VictimeViolencesL4256Result deserialize(String json) {
        try { return objectMapper.readValue(json, VictimeViolencesL4256Result.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private VictimeViolencesL4256Response toResponse(UUID caseFileId, String country,
                                                     VictimeViolencesL4256Result r) {
        return new VictimeViolencesL4256Response(
                caseFileId,
                r.dateOrdonnanceProtection(),
                r.juridiction(),
                r.dureeProtectionMois(),
                r.dateExpirationProtectionEffective(),
                r.enfantsAcharge(),
                r.nationalite(),
                country,
                r.eligibiliteScore(),
                r.criteresValides() != null ? r.criteresValides() : java.util.List.of(),
                r.criteresManquants() != null ? r.criteresManquants() : java.util.List.of(),
                r.dureeTitreSejourMois(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
