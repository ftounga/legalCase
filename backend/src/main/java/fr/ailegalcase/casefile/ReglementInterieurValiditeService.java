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
 * SF-218-35 : service applicatif de l'outil "Règlement intérieur — validité"
 * (F-DT-100) — apprécie la conformité (contenu obligatoire, absence de clauses
 * interdites) et l'opposabilité (procédure de mise en place) d'un règlement
 * intérieur (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT). Outil <b>FRANCE
 * UNIQUEMENT</b>, distinct de la validité d'une sanction disciplinaire fondée sur
 * le RI et du régime du lanceur d'alerte (F-DT-61).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>booléens requis présents, effectif &gt; 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class ReglementInterieurValiditeService {

    private final ReglementInterieurValiditeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ReglementInterieurValiditeService(ReglementInterieurValiditeRepository repository,
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
    public ReglementInterieurValiditeResponse analyze(UUID caseFileId,
                                                      ReglementInterieurValiditeRequest request,
                                                      OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Règlement intérieur — validité — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        ReglementInterieurValiditeResult result;
        try {
            result = ReglementInterieurValiditeAnalyzer.analyze(
                    request.effectif(),
                    request.reglementExiste(),
                    request.contenuHygieneSecurite(),
                    request.contenuDiscipline(),
                    request.contenuDroitsDefense(),
                    request.contenuHarcelementAgissements(),
                    request.clauseAtteinteLibertesNonJustifiee(),
                    request.clauseSanctionPecuniaire(),
                    request.consultationCseRealisee(),
                    request.transmissionInspectionTravail(),
                    request.depotGreffeCph());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ReglementInterieurValiditeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ReglementInterieurValiditeAnalysis a = new ReglementInterieurValiditeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setEffectif(result.effectif());
        entity.setReglementExiste(result.reglementExiste());
        entity.setItemsObligatoiresManquants(result.itemsObligatoiresManquants());
        entity.setClausesInterditesPresentes(result.clausesInterditesPresentes());
        entity.setStatut(result.statut());
        entity.setOpposabilite(result.opposabilite());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ReglementInterieurValiditeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ReglementInterieurValiditeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de validité de règlement intérieur trouvée pour ce dossier"));
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

    private ReglementInterieurValiditeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ReglementInterieurValiditeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ReglementInterieurValiditeResponse toResponse(UUID caseFileId, String country,
                                                          ReglementInterieurValiditeResult r) {
        return new ReglementInterieurValiditeResponse(
                caseFileId,
                r.effectif(),
                r.reglementExiste(),
                r.checklist(),
                r.itemsObligatoiresManquants(),
                r.clausesInterditesPresentes(),
                r.statut(),
                r.opposabilite(),
                r.consequences(),
                country,
                r.baseJuridique());
    }
}
