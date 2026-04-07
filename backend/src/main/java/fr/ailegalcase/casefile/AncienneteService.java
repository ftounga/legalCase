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
public class AncienneteService {

    private final AncienneteAnalysisRepository analysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AncienneteService(AncienneteAnalysisRepository analysisRepository,
                              CaseFileRepository caseFileRepository,
                              WorkspaceMemberRepository workspaceMemberRepository,
                              CurrentUserResolver currentUserResolver,
                              ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AncienneteResponse calculate(UUID caseFileId,
                                         AncienneteRequest request,
                                         OidcUser oidcUser,
                                         Principal principal) {
        validateRequest(request);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        AncienneteResult result = AncienneteCalculator.calculate(
                request.conventionCode(),
                request.dateEntree(),
                request.salaireBase(),
                request.congesContrat(),
                request.primeContrat()
        );

        AncienneteAnalysis entity = analysisRepository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AncienneteAnalysis a = new AncienneteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });

        entity.setConventionCode(request.conventionCode());
        entity.setDateEntree(request.dateEntree());
        entity.setResultData(serialize(result));
        analysisRepository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public AncienneteResponse get(UUID caseFileId,
                                   OidcUser oidcUser,
                                   Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);

        AncienneteAnalysis entity = analysisRepository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'ancienneté trouvée pour ce dossier"));

        AncienneteResult result = deserialize(entity.getResultData(), AncienneteResult.class);
        return toResponse(caseFileId, result);
    }

    private void validateRequest(AncienneteRequest request) {
        if (!ConventionBaremeReferentiel.isCodeValid(request.conventionCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Convention inconnue : " + request.conventionCode());
        }
        if (request.dateEntree() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date d'entrée requise");
        }
        if (request.salaireBase() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salaire de base requis");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_DU_TRAVAIL".equals(caseFile.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        return caseFile;
    }

    private String serialize(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation"); }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try { return objectMapper.readValue(json, clazz); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation"); }
    }

    private AncienneteResponse toResponse(UUID caseFileId, AncienneteResult result) {
        return new AncienneteResponse(
                caseFileId,
                result.conventionCode(), result.conventionLabel(), result.country(),
                result.ancienneteAnnees(), result.ancienneteMois(),
                result.congesLegauxJours(), result.congesSupplementairesJours(), result.congesTotalJours(),
                result.primeAnciennetePourcentage(), result.primeAncienneteMontant(),
                result.ecarts().stream().map(e -> new AncienneteResponse.EcartData(
                        e.champ(), e.attendu(), e.contractuel(), e.verdict())).toList()
        );
    }
}
