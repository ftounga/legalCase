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
public class Annexe13BeService {

    private final Annexe13BeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public Annexe13BeService(Annexe13BeRepository repository,
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
    public Annexe13BeResponse calculate(UUID caseFileId, Annexe13BeRequest request,
                                        OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Annexe 13 procédure BE uniquement — en France voir OQTF (SF-IM-08-01/03)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.delaiDepartImposeJours() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "delaiDepartImposeJours est requis");
        }

        boolean transfertImminent = Boolean.TRUE.equals(request.transfertImminent());
        boolean recoursForme = Boolean.TRUE.equals(request.recoursForme());

        Annexe13BeResult result;
        try {
            result = Annexe13BeCalculator.compute(
                    request.dateNotificationAnnexe13(),
                    request.delaiDepartImposeJours(),
                    request.motifOqt(),
                    transfertImminent,
                    recoursForme,
                    request.dateRecours(),
                    request.typeRecours());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Annexe13BeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    Annexe13BeAnalysis a = new Annexe13BeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateNotificationAnnexe13(result.dateNotificationAnnexe13());
        entity.setDelaiDepartImposeJours(result.delaiDepartImposeJours());
        entity.setMotifOqt(result.motifOqt());
        entity.setTransfertImminent(result.transfertImminent());
        entity.setRecoursForme(result.recoursForme());
        entity.setDateRecours(result.dateRecours());
        entity.setTypeRecours(result.typeRecours());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public Annexe13BeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        Annexe13BeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse annexe 13 BE trouvée pour ce dossier"));
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

    private Annexe13BeResult deserialize(String json) {
        try { return objectMapper.readValue(json, Annexe13BeResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private Annexe13BeResponse toResponse(UUID caseFileId, String country, Annexe13BeResult r) {
        return new Annexe13BeResponse(
                caseFileId,
                r.dateNotificationAnnexe13(),
                r.delaiDepartImposeJours(),
                r.motifOqt(),
                r.transfertImminent(),
                r.recoursForme(),
                r.dateRecours(),
                r.typeRecours(),
                country,
                r.dateExpirationDelaiDepart(),
                r.dateExpirationRecoursAnnulation(),
                r.dateExpirationRecoursExtremeUrgence(),
                r.joursRestantsAvantExpirationAnnulation(),
                r.statutRecoursAnnulation(),
                r.dateAudiencePrevisionnelle(),
                r.dateDecisionPrevisionnelle(),
                r.referedDisponibles() != null ? r.referedDisponibles() : java.util.List.of(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
