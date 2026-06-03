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
 * SF-218-49 : service applicatif de l'outil "RTT — acquisition selon accord
 * d'aménagement" (art. L.3121-41 à L.3121-44 CT, F-DT-80) — calcule le nombre
 * théorique de JRTT acquis (sans majoration) ou renvoie au régime des heures
 * supplémentaires à défaut d'accord d'aménagement. Outil <b>FRANCE
 * UNIQUEMENT</b>, distinct des heures supplémentaires (F-DT-19) et de la
 * monétisation de RTT (F-DT-51).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>champs requis présents et valides, horaire collectif &gt; 35 et &le; 48
 *       (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class RttAcquisitionService {

    private final RttAcquisitionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RttAcquisitionService(RttAcquisitionRepository repository,
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
    public RttAcquisitionResponse analyze(UUID caseFileId,
                                          RttAcquisitionRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "RTT acquisition — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        RttAcquisitionResult result;
        try {
            result = RttAcquisitionAnalyzer.analyze(
                    request.horaireHebdomadaireCollectif(),
                    request.accordCollectifPresent(),
                    request.semainesTravailleesAn());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RttAcquisitionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RttAcquisitionAnalysis a = new RttAcquisitionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setStatut(result.statut());
        entity.setHoraireHebdomadaireCollectif(result.horaireHebdomadaireCollectif());
        entity.setAccordCollectifPresent(result.accordCollectifPresent());
        entity.setSemainesTravailleesAn(result.semainesTravailleesAn());
        entity.setNombreJrttTheorique(result.nombreJrttTheorique());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public RttAcquisitionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RttAcquisitionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'acquisition de RTT trouvée pour ce dossier"));
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

    private RttAcquisitionResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, RttAcquisitionResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RttAcquisitionResponse toResponse(UUID caseFileId, String country,
                                              RttAcquisitionResult r) {
        return new RttAcquisitionResponse(
                caseFileId,
                r.statut(),
                r.horaireHebdomadaireCollectif(),
                r.accordCollectifPresent(),
                r.semainesTravailleesAn(),
                r.nombreJrttTheorique(),
                r.base(),
                r.notes(),
                country,
                r.baseJuridique());
    }
}
