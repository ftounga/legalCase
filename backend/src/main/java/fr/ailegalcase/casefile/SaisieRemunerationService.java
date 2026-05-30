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
 * SF-218-07 : service applicatif de l'outil "Saisie sur rémunération" — calcule
 * la quotité saisissable d'une rémunération selon le barème annuel par tranches
 * (art. R. 3252-2 CT), avec majoration par personne à charge (art. R. 3252-3 CT)
 * et fraction absolument insaisissable égale au montant forfaitaire RSA
 * (art. L. 3252-3 CT). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>remunerationNetteMensuelle &gt; 0, creanceTotale &gt; 0,
 *       nombrePersonnesACharge ≥ 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class SaisieRemunerationService {

    private final SaisieRemunerationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public SaisieRemunerationService(SaisieRemunerationRepository repository,
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
    public SaisieRemunerationResponse analyze(UUID caseFileId, SaisieRemunerationRequest request,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Saisie sur rémunération (art. R. 3252-2 CT) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        SaisieRemunerationResult result;
        try {
            result = SaisieRemunerationCalculator.calculate(
                    request.remunerationNetteMensuelle(),
                    request.nombrePersonnesACharge(),
                    request.creanceTotale(),
                    request.creanceAlimentaire());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        SaisieRemunerationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    SaisieRemunerationAnalysis a = new SaisieRemunerationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setRemunerationNetteMensuelle(result.remunerationNetteMensuelle());
        entity.setNombrePersonnesACharge(result.nombrePersonnesACharge());
        entity.setCreanceTotale(result.creanceTotale());
        entity.setCreanceAlimentaire(result.creanceAlimentaire());
        entity.setQuotiteSaisissableMensuelle(result.quotiteSaisissableMensuelle());
        entity.setVerdict(result.verdict());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public SaisieRemunerationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        SaisieRemunerationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de saisie sur rémunération trouvée pour ce dossier"));
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

    private SaisieRemunerationResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, SaisieRemunerationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private SaisieRemunerationResponse toResponse(UUID caseFileId, String country,
                                                  SaisieRemunerationResult r) {
        return new SaisieRemunerationResponse(
                caseFileId,
                r.remunerationNetteMensuelle(),
                r.nombrePersonnesACharge(),
                r.creanceTotale(),
                r.creanceAlimentaire(),
                r.quotiteSaisissableMensuelle(),
                r.montantLaisseAuSalarie(),
                r.fractionInsaisissable(),
                r.nombreMoisRecouvrement(),
                r.verdict(),
                country,
                r.baseJuridique());
    }
}
