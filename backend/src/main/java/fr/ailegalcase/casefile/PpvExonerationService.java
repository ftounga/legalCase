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
 * SF-218-39 : service applicatif de l'outil "PPV — exonération" (prime de partage
 * de la valeur — loi n° 2022-1158 du 16/08/2022 art. 1 + loi n° 2023-1107 du
 * 29/11/2023, F-DT-52) — apprécie la conformité au plafond d'exonération sociale
 * (3 000 € / 6 000 €), calcule la part exonérée et la part imposable, et
 * détermine l'exonération fiscale IR conditionnelle. Outil <b>FRANCE
 * UNIQUEMENT</b>, distinct de F-DT-53 intéressement / participation.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>champs requis présents, montantPrime &gt; 0, remunerationAnnuelleBrute
 *       &gt; 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class PpvExonerationService {

    private final PpvExonerationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PpvExonerationService(PpvExonerationRepository repository,
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
    public PpvExonerationResponse analyze(UUID caseFileId,
                                          PpvExonerationRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PPV — exonération — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        PpvExonerationResult result;
        try {
            result = PpvExonerationAnalyzer.analyze(
                    request.montantPrime(),
                    request.accordInteressementPresent(),
                    request.remunerationAnnuelleBrute(),
                    request.effectifMoins50(),
                    request.versementPlanEpargne());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PpvExonerationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PpvExonerationAnalysis a = new PpvExonerationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setMontantPrime(result.montantPrime());
        entity.setAccordInteressementPresent(result.accordInteressementPresent());
        entity.setRemunerationAnnuelleBrute(result.remunerationAnnuelleBrute());
        entity.setEffectifMoins50(result.effectifMoins50());
        entity.setVersementPlanEpargne(result.versementPlanEpargne());
        entity.setPlafondSocialApplique(result.plafondSocialApplique());
        entity.setMontantExonere(result.montantExonere());
        entity.setMontantImposable(result.montantImposable());
        entity.setExonerationFiscaleIr(result.exonerationFiscaleIr());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public PpvExonerationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        PpvExonerationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'exonération de PPV trouvée pour ce dossier"));
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
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
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

    private PpvExonerationResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, PpvExonerationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private PpvExonerationResponse toResponse(UUID caseFileId, String country,
                                              PpvExonerationResult r) {
        return new PpvExonerationResponse(
                caseFileId,
                r.montantPrime(),
                r.accordInteressementPresent(),
                r.remunerationAnnuelleBrute(),
                r.effectifMoins50(),
                r.versementPlanEpargne(),
                r.plafondSocialApplique(),
                r.montantExonere(),
                r.montantImposable(),
                r.exonerationFiscaleIr(),
                r.statut(),
                r.notes(),
                country,
                r.baseJuridique());
    }
}
