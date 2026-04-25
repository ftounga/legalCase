package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
 * SF-FA-19-05 : service applicatif désaccords parentaux (art. 373-2-10 Cciv).
 * Gate FR + DROIT_FAMILLE, isolation workspace, persistance JSON.
 */
@Service
public class DesaccordsParentauxService {

    private final DesaccordsParentauxRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DesaccordsParentauxService(DesaccordsParentauxRepository repository,
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
    public DesaccordsParentauxResponse calculate(UUID caseFileId,
                                                 DesaccordsParentauxRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Désaccords parentaux : procédure propre au droit français "
                            + "(art. 373-2-10 Cciv). F-FA-19 BE non couverte.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        if (request.domaineDesaccord() == null || request.domaineDesaccord().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "domaineDesaccord est requis");
        }
        if (request.intensiteDesaccord() == null || request.intensiteDesaccord().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "intensiteDesaccord est requis");
        }
        if (request.dateRequete() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateRequete est requise");
        }

        boolean interetSuperieur = Boolean.TRUE.equals(request.interetSuperieurInvoque());
        boolean expertiseDeja = Boolean.TRUE.equals(request.expertiseDejaRealisee());
        boolean urgence = Boolean.TRUE.equals(request.urgence());

        DesaccordsParentauxResult result;
        try {
            result = DesaccordsParentauxCalculator.compute(
                    request.domaineDesaccord(),
                    request.intensiteDesaccord(),
                    request.tentativesMediation(),
                    request.ageEnfantsConcernes(),
                    interetSuperieur,
                    expertiseDeja,
                    urgence,
                    request.dateRequete());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DesaccordsParentauxAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DesaccordsParentauxAnalysis a = new DesaccordsParentauxAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDomaineDesaccord(result.domaineDesaccord());
        entity.setIntensiteDesaccord(result.intensiteDesaccord());
        entity.setTentativesMediation(serialize(result.tentativesMediation()));
        entity.setAgeEnfantsConcernes(serialize(result.ageEnfantsConcernes()));
        entity.setInteretSuperieurInvoque(result.interetSuperieurInvoque());
        entity.setExpertiseDejaRealisee(result.expertiseDejaRealisee());
        entity.setUrgence(result.urgence());
        entity.setDateRequete(result.dateRequete());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DesaccordsParentauxResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DesaccordsParentauxAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de désaccord parental trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
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
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    @SuppressWarnings("unused")
    private List<String> deserializeStringList(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private DesaccordsParentauxResult deserialize(String json) {
        try { return objectMapper.readValue(json, DesaccordsParentauxResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private DesaccordsParentauxResponse toResponse(UUID caseFileId, String country,
                                                   DesaccordsParentauxResult r) {
        return new DesaccordsParentauxResponse(
                caseFileId,
                r.domaineDesaccord(),
                r.intensiteDesaccord(),
                r.tentativesMediation() != null ? r.tentativesMediation() : List.of(),
                r.ageEnfantsConcernes() != null ? r.ageEnfantsConcernes() : List.of(),
                r.interetSuperieurInvoque(),
                r.expertiseDejaRealisee(),
                r.urgence(),
                r.dateRequete(),
                r.scoreEligibiliteJaf(),
                r.verdictProbabiliteAcceptation(),
                r.mediationPrealableRecommandee(),
                r.auditionEnfantsRecommandee(),
                r.expertisePsyRecommandee(),
                r.delaiTraitementJoursPrevisionnel(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
