package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.CreanceType;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.ModeDissolution;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.RegimeBiens;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class PacsDissolutionService {

    private final PacsDissolutionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PacsDissolutionService(PacsDissolutionRepository repository,
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
    public PacsDissolutionResponse calculate(UUID caseFileId,
                                             PacsDissolutionRequest request,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dissolution PACS propre à la France (art. 515-7 Cciv)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.modeDissolution() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "modeDissolution est requis");
        }
        if (request.regimeBiens() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "regimeBiens est requis");
        }
        if (request.dureeUnionAnnees() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureeUnionAnnees est requis");
        }

        boolean patrimoineCommunSignificatif = Boolean.TRUE.equals(request.patrimoineCommunSignificatif());
        int enfantsCommuns = request.enfantsCommuns() != null ? request.enfantsCommuns() : 0;
        List<CreanceType> creances = request.creancesAlleguees() != null
                ? request.creancesAlleguees() : Collections.emptyList();

        PacsDissolutionResult result;
        try {
            result = PacsDissolutionCalculator.compute(
                    request.dateConclusionPacs(),
                    request.modeDissolution(),
                    request.dateDissolution(),
                    request.dureeUnionAnnees(),
                    request.regimeBiens(),
                    patrimoineCommunSignificatif,
                    creances,
                    enfantsCommuns,
                    request.dateNotificationPartenaire());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PacsDissolutionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PacsDissolutionAnalysis a = new PacsDissolutionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateConclusionPacs(result.dateConclusionPacs());
        entity.setModeDissolution(result.modeDissolution() != null ? result.modeDissolution().name() : null);
        entity.setDateDissolution(result.dateDissolution());
        entity.setDureeUnionAnnees(result.dureeUnionAnnees());
        entity.setRegimeBiens(result.regimeBiens() != null ? result.regimeBiens().name() : null);
        entity.setPatrimoineCommunSignificatif(result.patrimoineCommunSignificatif());
        entity.setEnfantsCommuns(result.enfantsCommuns());
        entity.setDateNotificationPartenaire(result.dateNotificationPartenaire());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public PacsDissolutionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        PacsDissolutionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse dissolution PACS trouvée pour ce dossier"));
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

    private PacsDissolutionResult deserialize(String json) {
        try { return objectMapper.readValue(json, PacsDissolutionResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private PacsDissolutionResponse toResponse(UUID caseFileId, String country,
                                               PacsDissolutionResult r) {
        return new PacsDissolutionResponse(
                caseFileId,
                r.dateConclusionPacs(),
                r.modeDissolution(),
                r.dateDissolution(),
                r.dureeUnionAnnees(),
                r.regimeBiens(),
                r.patrimoineCommunSignificatif(),
                r.creancesAlleguees() != null ? r.creancesAlleguees() : List.<CreanceType>of(),
                r.enfantsCommuns(),
                r.dateNotificationPartenaire(),
                r.dissolutionValide(),
                r.delaiNotificationOk(),
                r.dureeUnionEligibleCreances(),
                r.scoreCreancesProbables(),
                r.verdictRecommandation(),
                r.creancesPotentielleVisibles() != null ? r.creancesPotentielleVisibles() : List.<CreanceType>of(),
                r.delaiPrescriptionAnnees(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.<String>of(),
                country
        );
    }
}
