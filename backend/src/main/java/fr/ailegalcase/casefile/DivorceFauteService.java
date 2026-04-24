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
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-09-01 : service orchestrant l'analyse divorce pour faute FR
 * (art. 242-246, 266, 270 Cciv). Gate FRANCE + DROIT_FAMILLE strict.
 */
@Service
public class DivorceFauteService {

    private final DivorceFauteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DivorceFauteService(DivorceFauteRepository repository,
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
    public DivorceFauteResponse calculate(UUID caseFileId,
                                          DivorceFauteRequest request,
                                          OidcUser oidcUser,
                                          Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Divorce pour faute propre à la France (art. 242 Cciv)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.preuvesDocumentaires() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "preuvesDocumentaires est requis");
        }
        if (request.tortsAdverseInvoques() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tortsAdverseInvoques est requis");
        }
        if (request.dureeMariageAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dureeMariageAnnees est requis");
        }

        DivorceFauteResult result;
        try {
            result = DivorceFauteCalculator.compute(
                    request.fautesInvoquees(),
                    request.preuvesDocumentaires(),
                    request.tortsAdverseInvoques(),
                    request.dureeMariageAnnees(),
                    request.revenusAnnuelsDemandeurEur(),
                    request.revenusAnnuelsDefendeurEur(),
                    request.dateDepotAssignation());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DivorceFauteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DivorceFauteAnalysis a = new DivorceFauteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setFautesInvoquees(serialize(result.fautesInvoquees()));
        entity.setPreuvesDocumentaires(result.preuvesDocumentaires());
        entity.setTortsAdverseInvoques(result.tortsAdverseInvoques());
        entity.setDureeMariageAnnees(result.dureeMariageAnnees());
        entity.setRevenusAnnuelsDemandeurEur(result.revenusAnnuelsDemandeurEur());
        entity.setRevenusAnnuelsDefendeurEur(result.revenusAnnuelsDefendeurEur());
        entity.setDateDepotAssignation(result.dateDepotAssignation());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DivorceFauteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DivorceFauteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse divorce pour faute trouvée pour ce dossier"));
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

    private DivorceFauteResult deserializeResult(String json) {
        try { return objectMapper.readValue(json, DivorceFauteResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DivorceFauteResponse toResponse(UUID caseFileId, String country,
                                            DivorceFauteResult r) {
        return new DivorceFauteResponse(
                caseFileId,
                r.fautesInvoquees() != null ? r.fautesInvoquees() : List.of(),
                r.preuvesDocumentaires(),
                r.tortsAdverseInvoques(),
                r.dureeMariageAnnees(),
                r.revenusAnnuelsDemandeurEur(),
                r.revenusAnnuelsDefendeurEur(),
                r.dateDepotAssignation(),
                country,
                r.nombreFautesInvoquees(),
                r.solidariteeFautesOk(),
                r.risqueTortsPartages(),
                r.scoreGlobal(),
                r.verdictProbabiliteDivorceFaute(),
                r.verdictTortsEstimes(),
                r.damagesInteretsArt266FourchetteMin(),
                r.damagesInteretsArt266FourchetteMax(),
                r.prestationCompensatoireFourchetteMin(),
                r.prestationCompensatoireFourchetteMax(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : List.of(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}
