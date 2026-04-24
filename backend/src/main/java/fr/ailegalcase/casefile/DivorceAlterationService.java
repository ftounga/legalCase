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

@Service
public class DivorceAlterationService {

    private final DivorceAlterationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DivorceAlterationService(DivorceAlterationRepository repository,
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
    public DivorceAlterationResponse calculate(UUID caseFileId,
                                               DivorceAlterationRequest request,
                                               OidcUser oidcUser,
                                               Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Divorce pour altération définitive du lien conjugal : procédure "
                            + "propre au droit français (art. 237-238 Cciv). BE = F-FA-11.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dureeMariageAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureeMariageAnnees est requis");
        }

        boolean preuvesSeparationDocumentaires = Boolean.TRUE.equals(request.preuvesSeparationDocumentaires());
        boolean tentativesReconciliation = Boolean.TRUE.equals(request.tentativesReconciliation());
        boolean patrimoineCommunSignificatif = Boolean.TRUE.equals(request.patrimoineCommunSignificatif());

        DivorceAlterationResult result;
        try {
            result = DivorceAlterationCalculator.compute(
                    request.dateCessationVieCommune(),
                    preuvesSeparationDocumentaires,
                    tentativesReconciliation,
                    request.dureeMariageAnnees(),
                    request.revenusAnnuelsEpoux1Eur(),
                    request.revenusAnnuelsEpoux2Eur(),
                    patrimoineCommunSignificatif,
                    request.dateAssignation());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DivorceAlterationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DivorceAlterationAnalysis a = new DivorceAlterationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateCessationVieCommune(result.dateCessationVieCommune());
        entity.setPreuvesSeparationDocumentaires(result.preuvesSeparationDocumentaires());
        entity.setTentativesReconciliation(result.tentativesReconciliation());
        entity.setDureeMariageAnnees(result.dureeMariageAnnees());
        entity.setRevenusAnnuelsEpoux1Eur(result.revenusAnnuelsEpoux1Eur());
        entity.setRevenusAnnuelsEpoux2Eur(result.revenusAnnuelsEpoux2Eur());
        entity.setPatrimoineCommunSignificatif(result.patrimoineCommunSignificatif());
        entity.setDateAssignation(result.dateAssignation());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DivorceAlterationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DivorceAlterationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse divorce altération trouvée pour ce dossier"));
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private DivorceAlterationResult deserialize(String json) {
        try { return objectMapper.readValue(json, DivorceAlterationResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DivorceAlterationResponse toResponse(UUID caseFileId, String country,
                                                 DivorceAlterationResult r) {
        return new DivorceAlterationResponse(
                caseFileId,
                r.dateCessationVieCommune(),
                r.preuvesSeparationDocumentaires(),
                r.tentativesReconciliation(),
                r.dureeMariageAnnees(),
                r.revenusAnnuelsEpoux1Eur(),
                r.revenusAnnuelsEpoux2Eur(),
                r.patrimoineCommunSignificatif(),
                r.dateAssignation(),
                country,
                r.dureeSeparationAnnees(),
                r.delaiObjectifOk(),
                r.absencePreuveReconciliation(),
                r.conditionsReunies(),
                r.scoreGlobal(),
                r.verdictProbabilite(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.prestationCompensatoireFourchetteMin(),
                r.prestationCompensatoireFourchetteMax(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : java.util.List.of()
        );
    }
}
