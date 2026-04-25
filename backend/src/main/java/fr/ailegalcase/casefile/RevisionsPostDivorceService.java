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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-13-01 : service orchestrant l'analyse de révision post-divorce FR
 * (art. 209 / 373-2-13 / 373-2-9 / 279 / 373-2 Cciv). Gate FRANCE +
 * DROIT_FAMILLE strict.
 */
@Service
public class RevisionsPostDivorceService {

    private final RevisionsPostDivorceRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RevisionsPostDivorceService(RevisionsPostDivorceRepository repository,
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
    public RevisionsPostDivorceResponse calculate(UUID caseFileId,
                                                  RevisionsPostDivorceRequest request,
                                                  OidcUser oidcUser,
                                                  Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil propre au droit français (art. 209 / 373-2-13 / 373-2-9 / 279 / 373-2 Cciv)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.typeRevision() == null || request.typeRevision().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "typeRevision est requis");
        }

        RevisionsPostDivorceResult result;
        try {
            result = RevisionsPostDivorceCalculator.compute(
                    request.typeRevision(),
                    request.dateDecisionInitiale(),
                    request.changementCirconstance(),
                    request.revenusInitialsDebiteurEur(),
                    request.revenusActuelsDebiteurEur(),
                    request.revenusInitialsCreancierEur(),
                    request.revenusActuelsCreancierEur(),
                    request.nbEnfantsACharge(),
                    request.ageEnfants(),
                    request.modeResidenceActuel(),
                    request.modeResidenceDemande(),
                    request.informationPrealable(),
                    request.distanceDemenagementKm(),
                    LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RevisionsPostDivorceAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RevisionsPostDivorceAnalysis a = new RevisionsPostDivorceAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTypeRevision(result.typeRevision());
        entity.setDateDecisionInitiale(result.dateDecisionInitiale());
        entity.setChangementCirconstance(result.changementCirconstance());
        entity.setRevenusInitialsDebiteurEur(result.revenusInitialsDebiteurEur());
        entity.setRevenusActuelsDebiteurEur(result.revenusActuelsDebiteurEur());
        entity.setRevenusInitialsCreancierEur(result.revenusInitialsCreancierEur());
        entity.setRevenusActuelsCreancierEur(result.revenusActuelsCreancierEur());
        entity.setNbEnfantsACharge(result.nbEnfantsACharge());
        entity.setAgeEnfants(serialize(result.ageEnfants()));
        entity.setModeResidenceActuel(result.modeResidenceActuel());
        entity.setModeResidenceDemande(result.modeResidenceDemande());
        entity.setInformationPrealable(result.informationPrealable());
        entity.setDistanceDemenagementKm(result.distanceDemenagementKm());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public RevisionsPostDivorceResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RevisionsPostDivorceAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de révision post-divorce trouvée pour ce dossier"));
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

    private RevisionsPostDivorceResult deserializeResult(String json) {
        try { return objectMapper.readValue(json, RevisionsPostDivorceResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RevisionsPostDivorceResponse toResponse(UUID caseFileId, String country,
                                                    RevisionsPostDivorceResult r) {
        return new RevisionsPostDivorceResponse(
                caseFileId,
                r.typeRevision(),
                r.dateDecisionInitiale(),
                r.changementCirconstance(),
                r.revenusInitialsDebiteurEur(),
                r.revenusActuelsDebiteurEur(),
                r.revenusInitialsCreancierEur(),
                r.revenusActuelsCreancierEur(),
                r.nbEnfantsACharge(),
                r.ageEnfants() != null ? r.ageEnfants() : List.of(),
                r.modeResidenceActuel(),
                r.modeResidenceDemande(),
                r.informationPrealable(),
                r.distanceDemenagementKm(),
                country,
                r.ecartRevenusPct(),
                r.ancienneteDecisionMois(),
                r.modificationSubstantielle(),
                r.motivationSuffisante(),
                r.scoreGlobal(),
                r.verdictRevisionPossible(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}
