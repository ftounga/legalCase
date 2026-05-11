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
public class DivorceDcBeService {

    private final DivorceDcBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DivorceDcBeService(DivorceDcBeRepository repository,
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
    public DivorceDcBeResponse calculate(UUID caseFileId, DivorceDcBeRequest request,
                                         OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Divorce par consentement mutuel — outil BELGIQUE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        requireFlag(request.conventionLogement(), "conventionLogement");
        requireFlag(request.conventionBiens(), "conventionBiens");
        requireFlag(request.conventionGardeEnfants(), "conventionGardeEnfants");
        requireFlag(request.conventionContributions(), "conventionContributions");
        requireFlag(request.epouxConsentent(), "epouxConsentent");

        boolean enfantsMineursCommuns = Boolean.TRUE.equals(request.enfantsMineursCommuns());

        DivorceDcBeResult result;
        try {
            result = DivorceDcBeCalculator.compute(
                    request.dateSignatureConvention(),
                    request.dateAudienceHomologation(),
                    Boolean.TRUE.equals(request.conventionLogement()),
                    Boolean.TRUE.equals(request.conventionBiens()),
                    Boolean.TRUE.equals(request.conventionGardeEnfants()),
                    Boolean.TRUE.equals(request.conventionContributions()),
                    enfantsMineursCommuns,
                    Boolean.TRUE.equals(request.epouxConsentent()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DivorceDcBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DivorceDcBeAnalysis a = new DivorceDcBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateSignatureConvention(result.dateSignatureConvention());
        entity.setDateAudienceHomologation(result.dateAudienceHomologation());
        entity.setConventionLogement(result.conventionLogement());
        entity.setConventionBiens(result.conventionBiens());
        entity.setConventionGardeEnfants(result.conventionGardeEnfants());
        entity.setConventionContributions(result.conventionContributions());
        entity.setEnfantsMineursCommuns(result.enfantsMineursCommuns());
        entity.setEpouxConsentent(result.epouxConsentent());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DivorceDcBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DivorceDcBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse divorce DC BE trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
    }

    private void requireFlag(Boolean value, String name) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " est requis");
        }
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

    private DivorceDcBeResult deserialize(String json) {
        try { return objectMapper.readValue(json, DivorceDcBeResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DivorceDcBeResponse toResponse(UUID caseFileId, String country, DivorceDcBeResult r) {
        return new DivorceDcBeResponse(
                caseFileId,
                country,
                r.dateSignatureConvention(),
                r.dateAudienceHomologation(),
                r.delaiReflexionJours(),
                r.delaiReflexionRespecte(),
                r.conventionLogement(),
                r.conventionBiens(),
                r.conventionGardeEnfants(),
                r.conventionContributions(),
                r.conventionComplete(),
                r.enfantsMineursCommuns(),
                r.epouxConsentent(),
                r.verdict(),
                r.motifsIrrecevabilite() != null ? r.motifsIrrecevabilite() : java.util.List.of(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
