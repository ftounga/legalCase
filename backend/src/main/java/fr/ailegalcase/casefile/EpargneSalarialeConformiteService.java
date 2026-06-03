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
 * SF-218-41 : service applicatif de l'outil "épargne salariale — conformité"
 * (intéressement / participation / dispositif de partage de la valeur — art.
 * L.3311-1 et s., L.3321-1 et s., L.3322-2 CT + loi n° 2023-1107 du 29/11/2023,
 * F-DT-53) — apprécie la conformité aux obligations (participation obligatoire au
 * delà de 50 salariés, dispositif de partage de la valeur pour les entreprises de
 * 11 à 49 salariés rentables) et rend une checklist + un verdict. Outil
 * <b>FRANCE UNIQUEMENT</b>, distinct de F-DT-52 PPV exonération.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>champs requis présents, effectif &gt; 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class EpargneSalarialeConformiteService {

    private final EpargneSalarialeConformiteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public EpargneSalarialeConformiteService(EpargneSalarialeConformiteRepository repository,
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
    public EpargneSalarialeConformiteResponse analyze(UUID caseFileId,
                                                      EpargneSalarialeConformiteRequest request,
                                                      OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Épargne salariale — conformité — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        EpargneSalarialeConformiteResult result;
        try {
            result = EpargneSalarialeConformiteAnalyzer.analyze(
                    request.effectif(),
                    request.accordParticipationPresent(),
                    request.accordInteressementPresent(),
                    request.beneficeNetFiscalPositif3Ans(),
                    request.entreprise11a49());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        EpargneSalarialeConformiteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    EpargneSalarialeConformiteAnalysis a = new EpargneSalarialeConformiteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setEffectif(result.effectif());
        entity.setAccordParticipationPresent(result.accordParticipationPresent());
        entity.setAccordInteressementPresent(result.accordInteressementPresent());
        entity.setBeneficeNetFiscalPositif3Ans(result.beneficeNetFiscalPositif3Ans());
        entity.setEntreprise11a49(result.entreprise11a49());
        entity.setObligationsApplicables(result.obligationsApplicables());
        entity.setObligationsNonRemplies(result.obligationsNonRemplies());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public EpargneSalarialeConformiteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        EpargneSalarialeConformiteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de conformité d'épargne salariale trouvée pour ce dossier"));
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

    private EpargneSalarialeConformiteResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, EpargneSalarialeConformiteResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private EpargneSalarialeConformiteResponse toResponse(UUID caseFileId, String country,
                                                          EpargneSalarialeConformiteResult r) {
        return new EpargneSalarialeConformiteResponse(
                caseFileId,
                r.effectif(),
                r.accordParticipationPresent(),
                r.accordInteressementPresent(),
                r.beneficeNetFiscalPositif3Ans(),
                r.entreprise11a49(),
                r.checklist(),
                r.obligationsApplicables(),
                r.obligationsNonRemplies(),
                r.statut(),
                r.notes(),
                country,
                r.baseJuridique());
    }
}
