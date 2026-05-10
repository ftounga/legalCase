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

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-210-03 : service orchestrant l'analyse d'option successorale (FR — art. 768+).
 * Gate FRANCE + DROIT_FAMILLE strict.
 */
@Service
public class AcceptationRenonciationSuccessionService {

    private final AcceptationRenonciationSuccessionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AcceptationRenonciationSuccessionService(
            AcceptationRenonciationSuccessionRepository repository,
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
    public AcceptationRenonciationSuccessionResponse calculate(
            UUID caseFileId,
            AcceptationRenonciationSuccessionRequest request,
            OidcUser oidcUser,
            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Acceptation/renonciation succession FR uniquement (art. 768+ Cciv).");
        }
        if (request == null || request.dateOuvertureSuccession() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateOuvertureSuccession est requise");
        }
        double actif = request.actifBrutEur() != null ? request.actifBrutEur() : 0d;
        double passif = request.passifEur() != null ? request.passifEur() : 0d;
        boolean actes = Boolean.TRUE.equals(request.actesEquivalentAcceptationDejaPosesDetected());
        boolean inv = Boolean.TRUE.equals(request.inventaireRealise());
        boolean dettesIncert = Boolean.TRUE.equals(request.dettesIncertainesDetected());

        AcceptationRenonciationSuccessionResult result;
        try {
            result = AcceptationRenonciationSuccessionCalculator.compute(
                    request.dateOuvertureSuccession(),
                    request.qualiteHeritier(),
                    actif,
                    passif,
                    actes,
                    inv,
                    dettesIncert,
                    request.intentionExprimee(),
                    LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AcceptationRenonciationSuccessionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AcceptationRenonciationSuccessionAnalysis a = new AcceptationRenonciationSuccessionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateOuvertureSuccession(result.dateOuvertureSuccession());
        entity.setQualiteHeritier(result.qualiteHeritier());
        entity.setActifBrutEur(BigDecimal.valueOf(result.actifBrutEur()));
        entity.setPassifEur(BigDecimal.valueOf(result.passifEur()));
        entity.setActesEquivalentAcceptation(result.actesEquivalentAcceptationDejaPosesDetected());
        entity.setInventaireRealise(result.inventaireRealise());
        entity.setDettesIncertaines(result.dettesIncertainesDetected());
        entity.setIntentionExprimee(result.intentionExprimee());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AcceptationRenonciationSuccessionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AcceptationRenonciationSuccessionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'option successorale trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
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
                    "Ce dossier n'est pas un dossier de droit de la famille");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private AcceptationRenonciationSuccessionResult deserializeResult(String json) {
        try { return objectMapper.readValue(json, AcceptationRenonciationSuccessionResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AcceptationRenonciationSuccessionResponse toResponse(UUID caseFileId, String country,
                                                                 AcceptationRenonciationSuccessionResult r) {
        return new AcceptationRenonciationSuccessionResponse(
                caseFileId,
                r.dateOuvertureSuccession(),
                r.qualiteHeritier(),
                r.actifBrutEur(),
                r.passifEur(),
                r.actesEquivalentAcceptationDejaPosesDetected(),
                r.inventaireRealise(),
                r.dettesIncertainesDetected(),
                r.intentionExprimee(),
                r.optionsOuvertes() != null ? r.optionsOuvertes() : List.of(),
                r.optionRecommandee(),
                r.delaiRestantJours(),
                r.delaiTotalJours(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country);
    }
}
