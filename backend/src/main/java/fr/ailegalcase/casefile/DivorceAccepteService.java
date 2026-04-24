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
import java.util.UUID;

@Service
public class DivorceAccepteService {

    private final DivorceAccepteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DivorceAccepteService(DivorceAccepteRepository repository,
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
    public DivorceAccepteResponse calculate(UUID caseFileId,
                                            DivorceAccepteRequest request,
                                            OidcUser oidcUser,
                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime divorce accepté propre à la France (art. 233-234 Cciv)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dureeMariageAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureeMariageAnnees est requis");
        }

        boolean acceptationPrincipeSignee = Boolean.TRUE.equals(request.acceptationPrincipeSignee());
        boolean patrimoineCommun = Boolean.TRUE.equals(request.patrimoineCommun());
        BigDecimal r1 = request.revenusAnnuelsEpoux1Eur() != null
                ? request.revenusAnnuelsEpoux1Eur() : BigDecimal.ZERO;
        BigDecimal r2 = request.revenusAnnuelsEpoux2Eur() != null
                ? request.revenusAnnuelsEpoux2Eur() : BigDecimal.ZERO;

        DivorceAccepteResult result;
        try {
            result = DivorceAccepteCalculator.compute(
                    acceptationPrincipeSignee,
                    request.dateAcceptationPV(),
                    request.dureeMariageAnnees(),
                    r1,
                    r2,
                    patrimoineCommun,
                    request.dateAssignation());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DivorceAccepteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DivorceAccepteAnalysis a = new DivorceAccepteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setAcceptationPrincipeSignee(result.acceptationPrincipeSignee());
        entity.setDateAcceptationPV(result.dateAcceptationPV());
        entity.setDureeMariageAnnees(result.dureeMariageAnnees());
        entity.setRevenusAnnuelsEpoux1Eur(result.revenusAnnuelsEpoux1Eur());
        entity.setRevenusAnnuelsEpoux2Eur(result.revenusAnnuelsEpoux2Eur());
        entity.setPatrimoineCommun(result.patrimoineCommun());
        entity.setDateAssignation(result.dateAssignation());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DivorceAccepteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DivorceAccepteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse divorce accepté trouvée pour ce dossier"));
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

    private DivorceAccepteResult deserialize(String json) {
        try { return objectMapper.readValue(json, DivorceAccepteResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DivorceAccepteResponse toResponse(UUID caseFileId, String country,
                                              DivorceAccepteResult r) {
        return new DivorceAccepteResponse(
                caseFileId,
                r.acceptationPrincipeSignee(),
                r.dateAcceptationPV(),
                r.dureeMariageAnnees(),
                r.revenusAnnuelsEpoux1Eur(),
                r.revenusAnnuelsEpoux2Eur(),
                r.patrimoineCommun(),
                r.dateAssignation(),
                country,
                r.acceptationValide(),
                r.ordrePublic(),
                r.eligibilite(),
                r.scoreGlobal(),
                r.verdictEligibilite(),
                r.delaiProcedureMoisPrevisionnel(),
                r.prestationCompensatoireFourchetteMin(),
                r.prestationCompensatoireFourchetteMax(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
