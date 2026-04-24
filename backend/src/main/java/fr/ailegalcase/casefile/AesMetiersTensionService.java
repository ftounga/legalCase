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
public class AesMetiersTensionService {

    private final AesMetiersTensionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AesMetiersTensionService(AesMetiersTensionRepository repository,
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
    public AesMetiersTensionResponse calculate(UUID caseFileId,
                                               AesMetiersTensionRequest request,
                                               OidcUser oidcUser,
                                               Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime AES métier en tension propre à la France (loi 26/01/2024)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        if (request.moisActiviteSalarieeDernieres24Mois() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "moisActiviteSalarieeDernieres24Mois est requis");
        }
        boolean metierEstEnTension = Boolean.TRUE.equals(request.metierEstEnTension());
        boolean menaceOrdrePublic = Boolean.TRUE.equals(request.menaceOrdrePublic());
        boolean contratOuPromesseValide = Boolean.TRUE.equals(request.contratOuPromesseValide());

        AesMetiersTensionResult result;
        try {
            result = AesMetiersTensionCalculator.compute(
                    request.dateEntreeFrance(),
                    request.moisActiviteSalarieeDernieres24Mois(),
                    metierEstEnTension,
                    request.codeMetier(),
                    menaceOrdrePublic,
                    contratOuPromesseValide,
                    request.dateDepotDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AesMetiersTensionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AesMetiersTensionAnalysis a = new AesMetiersTensionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateEntreeFrance(result.dateEntreeFrance());
        entity.setMoisActiviteSalarieeDernieres24Mois(result.moisActiviteSalarieeDernieres24Mois());
        entity.setMetierEstEnTension(result.metierEstEnTension());
        entity.setCodeMetier(result.codeMetier());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setContratOuPromesseValide(result.contratOuPromesseValide());
        entity.setDateDepotDemande(result.dateDepotDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AesMetiersTensionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AesMetiersTensionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse AES métier en tension trouvée pour ce dossier"));
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

    private AesMetiersTensionResult deserialize(String json) {
        try { return objectMapper.readValue(json, AesMetiersTensionResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AesMetiersTensionResponse toResponse(UUID caseFileId, String country,
                                                 AesMetiersTensionResult r) {
        return new AesMetiersTensionResponse(
                caseFileId,
                r.dateEntreeFrance(),
                r.moisActiviteSalarieeDernieres24Mois(),
                r.metierEstEnTension(),
                r.codeMetier(),
                r.menaceOrdrePublic(),
                r.contratOuPromesseValide(),
                r.dateDepotDemande(),
                country,
                r.presence3Ans(),
                r.activite12MoisOk(),
                r.conditionsReunies(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.dateEligibiliteAtteinte(),
                r.dateExpirationInstructionSiDemande(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
