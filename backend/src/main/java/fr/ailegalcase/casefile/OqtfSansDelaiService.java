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
public class OqtfSansDelaiService {

    private final OqtfSansDelaiRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public OqtfSansDelaiService(OqtfSansDelaiRepository repository,
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
    public OqtfSansDelaiResponse calculate(UUID caseFileId, OqtfSansDelaiRequest request,
                                           OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "OQTF sans délai procédure FR uniquement — en Belgique voir SF-IM-08-05 "
                            + "(Annexe 13)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        boolean recoursForme = Boolean.TRUE.equals(request.recoursForme());
        boolean placementCra = Boolean.TRUE.equals(request.placementCra());

        OqtfSansDelaiResult result;
        try {
            result = OqtfSansDelaiCalculator.compute(
                    request.dateHeureNotificationOqtf(),
                    request.motifSansDelai(),
                    placementCra,
                    recoursForme,
                    request.dateHeureRecours());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        OqtfSansDelaiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    OqtfSansDelaiAnalysis a = new OqtfSansDelaiAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateHeureNotificationOqtf(result.dateHeureNotificationOqtf());
        entity.setMotifSansDelai(result.motifSansDelai());
        entity.setPlacementCra(result.placementCra());
        entity.setRecoursForme(result.recoursForme());
        entity.setDateHeureRecours(result.dateHeureRecours());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public OqtfSansDelaiResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        OqtfSansDelaiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse OQTF sans délai trouvée pour ce dossier"));
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

    private OqtfSansDelaiResult deserialize(String json) {
        try { return objectMapper.readValue(json, OqtfSansDelaiResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private OqtfSansDelaiResponse toResponse(UUID caseFileId, String country, OqtfSansDelaiResult r) {
        return new OqtfSansDelaiResponse(
                caseFileId,
                r.dateHeureNotificationOqtf(),
                r.motifSansDelai(),
                r.placementCra(),
                r.recoursForme(),
                r.dateHeureRecours(),
                country,
                r.dateHeureExpirationDelaiRecours(),
                r.heuresRestantes(),
                r.statutDelaiRecours(),
                r.dateHeureAudiencePrevisionnelle(),
                r.dateDecisionPrevisionnelle(),
                r.refereDisponibles() != null ? r.refereDisponibles() : java.util.List.of(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
