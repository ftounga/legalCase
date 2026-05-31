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
 * SF-218-27 : service applicatif de l'outil "Harcèlement — procédure interne de
 * traitement d'un signalement" (F-DT-59) — apprécie la conformité de la procédure
 * employeur (référent CSE selon effectif, référent employeur ≥ 250, information /
 * affichage, enquête contradictoire à réception d'un signalement, délai de
 * réaction raisonnable) et le risque de responsabilité (obligation de sécurité
 * L.4121-1). Outil <b>FRANCE UNIQUEMENT</b>, distinct de F-DT-11.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>booléens requis présents, effectif &gt; 0, dates cohérentes (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class HarcelementProcedureInterneService {

    private final HarcelementProcedureInterneRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public HarcelementProcedureInterneService(HarcelementProcedureInterneRepository repository,
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
    public HarcelementProcedureInterneResponse analyze(UUID caseFileId,
                                                       HarcelementProcedureInterneRequest request,
                                                       OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Harcèlement — procédure interne — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        HarcelementProcedureInterneResult result;
        try {
            result = HarcelementProcedureInterneAnalyzer.analyze(
                    request.effectif(),
                    request.referentCseDesigne(),
                    request.referentEmployeurDesigne(),
                    request.informationAffichageRealisee(),
                    request.signalementRecu(),
                    request.dateSignalement(),
                    request.dateOuvertureEnquete(),
                    request.enqueteContradictoire(),
                    request.mesuresConservatoiresPrises());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        HarcelementProcedureInterneAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    HarcelementProcedureInterneAnalysis a = new HarcelementProcedureInterneAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setEffectif(result.effectif());
        entity.setSignalementRecu(result.signalementRecu());
        entity.setDelaiReactionJours(result.delaiReactionJours());
        entity.setDelaiRaisonnable(result.delaiRaisonnable());
        entity.setItemsObligatoiresManquants(result.itemsObligatoiresManquants());
        entity.setStatut(result.statut());
        entity.setRisqueResponsabiliteEmployeur(result.risqueResponsabiliteEmployeur());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public HarcelementProcedureInterneResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        HarcelementProcedureInterneAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de procédure interne harcèlement trouvée pour ce dossier"));
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

    private HarcelementProcedureInterneResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, HarcelementProcedureInterneResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private HarcelementProcedureInterneResponse toResponse(UUID caseFileId, String country,
                                                           HarcelementProcedureInterneResult r) {
        return new HarcelementProcedureInterneResponse(
                caseFileId,
                r.effectif(),
                r.signalementRecu(),
                r.checklist(),
                r.delaiReactionJours(),
                r.delaiRaisonnable(),
                r.itemsObligatoiresManquants(),
                r.statut(),
                r.risqueResponsabiliteEmployeur(),
                r.consequences(),
                country,
                r.baseJuridique());
    }
}
