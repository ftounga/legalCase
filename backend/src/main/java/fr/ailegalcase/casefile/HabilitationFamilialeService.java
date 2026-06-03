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
 * SF-222-03 : service orchestrant l'analyse des conditions de l'habilitation
 * familiale (art. 494-1 et s. Cciv). Gate FRANCE + DROIT_FAMILLE. Upsert 1:1 par
 * dossier.
 *
 * <p>Anti-doublon F-FA-25 : cet outil cadre les conditions PROPRES de
 * l'habilitation familiale (alternative simplifiée à consensus familial), il ne
 * re-sélectionne pas le régime de protection (curatelle / tutelle), qui relève
 * de F-FA-25.</p>
 */
@Service
public class HabilitationFamilialeService {

    private final HabilitationFamilialeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public HabilitationFamilialeService(
            HabilitationFamilialeRepository repository,
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
    public HabilitationFamilialeResponse calculate(UUID caseFileId,
                                                   HabilitationFamilialeRequest request,
                                                   OidcUser oidcUser,
                                                   Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        HabilitationFamilialeResult result;
        try {
            result = HabilitationFamilialeCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        HabilitationFamilialeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    HabilitationFamilialeAnalysis a = new HabilitationFamilialeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public HabilitationFamilialeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        HabilitationFamilialeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse habilitation familiale trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(HabilitationFamilialeRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-HABILITATION-FAMILIALE applicable uniquement en France (art. 494-1 et s. Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille.");
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation.");
        }
    }

    private HabilitationFamilialeResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, HabilitationFamilialeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private HabilitationFamilialeResponse toResponse(UUID caseFileId, String country, HabilitationFamilialeResult r) {
        return new HabilitationFamilialeResponse(
                caseFileId,
                r.verdict().name(),
                r.modalite() != null ? r.modalite().name() : null,
                r.actesCouverts(),
                r.conditionsManquantes(),
                r.basesJuridiques(),
                r.messages(),
                country
        );
    }
}
