package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.referential.LegalReferentialService;
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
    private final LegalReferentialService referentialService;

    public AncienneteService(AncienneteAnalysisRepository analysisRepository,
                              CaseFileRepository caseFileRepository,
                              WorkspaceMemberRepository workspaceMemberRepository,
                              CurrentUserResolver currentUserResolver,
                              ObjectMapper objectMapper,
                              LegalReferentialService referentialService) {
        this.analysisRepository = analysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
        this.referentialService = referentialService;
    }

    @Transactional
    public AncienneteResponse calculate(UUID caseFileId, AncienneteRequest request,
                                         OidcUser oidcUser, Principal principal) {
        validateRequest(request);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        ConventionBareme bareme = referentialService.getConventionBareme(request.conventionCode());
        AncienneteResult result = AncienneteCalculator.calculate(
                request.conventionCode(), request.dateEntree(), request.salaireBase(),
                request.congesContrat(), request.primeContrat(), bareme);

        AncienneteAnalysis entity = analysisRepository.findByCaseFileId(caseFileId)
                .orElseGet(() -> { AncienneteAnalysis a = new AncienneteAnalysis(); a.setCaseFile(caseFile); return a; });
        entity.setConventionCode(request.conventionCode());
        entity.setDateEntree(request.dateEntree());
        entity.setSalaireBase(request.salaireBase());
        entity.setCongesContrat(request.congesContrat());
        entity.setPrimeContrat(request.primeContrat());
        entity.setResultData(serialize(result));
        analysisRepository.save(entity);

        return toResponse(caseFileId, result, entity);
    }

    @Transactional(readOnly = true)
    public AncienneteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        AncienneteAnalysis entity = analysisRepository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'ancienneté trouvée pour ce dossier"));
        AncienneteResult result = deserialize(entity.getResultData(), AncienneteResult.class);
        return toResponse(caseFileId, result, entity);
    }

    private void validateRequest(AncienneteRequest request) {
        if (!ConventionBaremeReferentiel.isCodeValid(request.conventionCode()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Convention inconnue : " + request.conventionCode());
        if (request.dateEntree() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date d'entrée requise");
        if (request.salaireBase() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salaire de base requis");
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
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce dossier n'est pas un dossier de droit du travail");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation"); }
    }
    private <T> T deserialize(String json, Class<T> c) {
        try { return objectMapper.readValue(json, c); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation"); }
    }

    private AncienneteResponse toResponse(UUID caseFileId, AncienneteResult r, AncienneteAnalysis entity) {
        return new AncienneteResponse(caseFileId, r.conventionCode(),
                entity.getDateEntree(), entity.getSalaireBase(),
                entity.getCongesContrat(), entity.getPrimeContrat(),
                r.conventionLabel(), r.country(),
                r.ancienneteAnnees(), r.ancienneteMois(), r.congesLegauxJours(), r.congesSupplementairesJours(),
                r.congesTotalJours(), r.primeAnciennetePourcentage(), r.primeAncienneteMontant(),
                r.baremePrimePourcentage(), r.baremeCongesTotal(),
                r.ecarts().stream().map(e -> new AncienneteResponse.EcartData(
                        e.champ(), e.attendu(), e.contractuel(), e.verdict())).toList());
    }
}
