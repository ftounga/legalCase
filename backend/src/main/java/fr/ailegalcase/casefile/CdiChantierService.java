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
 * SF-218-25 : service applicatif de l'outil "Licenciement CDI de chantier /
 * d'opération" (F-DT-37) — apprécie la validité du recours (accord de branche
 * étendu / usage constant BTP-ingénierie), qualifie le motif de fin de chantier
 * (cause réelle et sérieuse, art. L.1236-8) et calcule l'indemnité de
 * licenciement (R.1234-2). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>dates requises et cohérentes, fondement / secteur / chantier / salaire
 *       requis (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class CdiChantierService {

    private final CdiChantierRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CdiChantierService(CdiChantierRepository repository,
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
    public CdiChantierResponse analyze(UUID caseFileId,
                                       CdiChantierRequest request,
                                       OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Licenciement CDI de chantier — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        CdiChantierResult result;
        try {
            result = CdiChantierAnalyzer.analyze(
                    request.dateEntree(),
                    request.dateRupture(),
                    request.fondementRecours(),
                    request.secteur(),
                    request.chantierAcheve(),
                    request.salaireMensuelMoyen(),
                    request.reclassementAutreChantierPropose());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CdiChantierAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CdiChantierAnalysis a = new CdiChantierAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateEntree(result.dateEntree());
        entity.setDateRupture(result.dateRupture());
        entity.setFondementRecours(result.fondementRecours());
        entity.setSecteur(result.secteur());
        entity.setChantierAcheve(result.chantierAcheve());
        entity.setSalaireMensuelMoyen(result.salaireMensuelMoyen());
        entity.setReclassementAutreChantierPropose(result.reclassementAutreChantierPropose());
        entity.setAncienneteAnnees(result.ancienneteAnnees());
        entity.setRecoursValide(result.recoursValide());
        entity.setMotifLicenciement(result.motifLicenciement());
        entity.setIndemniteLicenciement(result.indemniteLicenciement());
        entity.setVerdictGlobal(result.verdictGlobal());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public CdiChantierResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        CdiChantierAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de licenciement CDI de chantier trouvée pour ce dossier"));
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

    private CdiChantierResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CdiChantierResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CdiChantierResponse toResponse(UUID caseFileId, String country, CdiChantierResult r) {
        return new CdiChantierResponse(
                caseFileId,
                r.dateEntree(),
                r.dateRupture(),
                r.fondementRecours(),
                r.secteur(),
                r.chantierAcheve(),
                r.salaireMensuelMoyen(),
                r.reclassementAutreChantierPropose(),
                r.ancienneteAnnees(),
                r.recoursValide(),
                r.motifRecours(),
                r.motifLicenciement(),
                r.indemniteLicenciement(),
                r.procedureRequise(),
                r.verdictGlobal(),
                r.consequences(),
                r.motif(),
                country,
                r.baseJuridique());
    }
}
