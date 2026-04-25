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
 * SF-FA-11-01 : service de l'outil décisionnel "Divorce pour désunion
 * irrémédiable" BE (art. 229 §3 CC belge, Loi 27/04/2007). Gate
 * country=BELGIQUE + legalDomain=DROIT_FAMILLE. Équivalent belge de
 * F-FA-08 / F-FA-10 (procédures distinctes en droit français).
 */
@Service
public class DivorceDesunionIrremediableBeService {

    private final DivorceDesunionIrremediableBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DivorceDesunionIrremediableBeService(DivorceDesunionIrremediableBeRepository repository,
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
    public DivorceDesunionIrremediableBeResponse calculate(UUID caseFileId,
                                                            DivorceDesunionIrremediableBeRequest request,
                                                            OidcUser oidcUser,
                                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Divorce pour désunion irrémédiable : procédure propre au droit "
                            + "belge (art. 229 §3 CC). FR = F-FA-08 (altération) / "
                            + "F-FA-10 (accepté).");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        boolean separationConsentue = Boolean.TRUE.equals(request.separationConsentue());
        boolean preuvesSeparation = Boolean.TRUE.equals(request.preuvesSeparation());
        boolean preuvesDocumentaires = Boolean.TRUE.equals(request.preuvesDocumentaires());
        boolean tentativesReconciliation = Boolean.TRUE.equals(request.tentativesReconciliation());

        DivorceDesunionIrremediableBeResult result;
        try {
            result = DivorceDesunionIrremediableBeCalculator.compute(
                    request.dateSeparation(),
                    separationConsentue,
                    preuvesSeparation,
                    preuvesDocumentaires,
                    tentativesReconciliation,
                    request.dateAssignation());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DivorceDesunionIrremediableBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DivorceDesunionIrremediableBeAnalysis a = new DivorceDesunionIrremediableBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateSeparation(result.dateSeparation());
        entity.setSeparationConsentue(result.separationConsentue());
        entity.setPreuvesSeparation(result.preuvesSeparation());
        entity.setPreuvesDocumentaires(result.preuvesDocumentaires());
        entity.setTentativesReconciliation(result.tentativesReconciliation());
        entity.setDateAssignation(result.dateAssignation());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DivorceDesunionIrremediableBeResponse get(UUID caseFileId,
                                                      OidcUser oidcUser,
                                                      Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DivorceDesunionIrremediableBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse divorce désunion irrémédiable BE trouvée pour ce dossier"));
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private DivorceDesunionIrremediableBeResult deserialize(String json) {
        try { return objectMapper.readValue(json, DivorceDesunionIrremediableBeResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DivorceDesunionIrremediableBeResponse toResponse(UUID caseFileId,
                                                              String country,
                                                              DivorceDesunionIrremediableBeResult r) {
        return new DivorceDesunionIrremediableBeResponse(
                caseFileId,
                r.dateSeparation(),
                r.separationConsentue(),
                r.preuvesSeparation(),
                r.preuvesDocumentaires(),
                r.tentativesReconciliation(),
                r.dateAssignation(),
                r.dureeSeparationMois(),
                r.seuilSeparationMois(),
                r.delaiObjectifOk(),
                r.conditionsReunies(),
                r.scoreGlobal(),
                r.verdictProbabilite(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : java.util.List.of(),
                country
        );
    }
}
