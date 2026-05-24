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
import java.time.Instant;
import java.util.UUID;

/**
 * SF-212-17 : service orchestrant l'analyse de la rupture anticipée d'un CDD
 * (F-DT-43-rupture-anticipee-cdd, FRANCE — L. 1243-1 à L. 1243-4 CT ;
 * L. 1243-8 CT ; Cass. soc. constante) + persistance snapshot (un seul
 * résultat courant par dossier — UNIQUE (case_file_id) côté migration 333).
 */
@Service
public class RuptureAnticipeeCddService {

    private final RuptureAnticipeeCddRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RuptureAnticipeeCddService(RuptureAnticipeeCddRepository repository,
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
    public RuptureAnticipeeCddResponse calculate(UUID caseFileId,
                                                 RuptureAnticipeeCddRequest request,
                                                 OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        RuptureAnticipeeCddCalculator.Result result;
        try {
            result = RuptureAnticipeeCddCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("FRANCE")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RuptureAnticipeeCddResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        RuptureAnticipeeCddAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RuptureAnticipeeCddAnalysis a = new RuptureAnticipeeCddAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public RuptureAnticipeeCddResponse get(UUID caseFileId,
                                           OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RuptureAnticipeeCddAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de rupture anticipée CDD trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce dossier appartient à un autre workspace");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
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

    private RuptureAnticipeeCddResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, RuptureAnticipeeCddResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RuptureAnticipeeCddResponse toResponse(UUID caseFileId,
                                                   RuptureAnticipeeCddRequest req,
                                                   RuptureAnticipeeCddCalculator.Result r,
                                                   Instant calculatedAt) {
        return new RuptureAnticipeeCddResponse(
                caseFileId,
                req.auteurRupture(),
                req.motifRupture(),
                req.dateTermeCdd(),
                req.dateRupture(),
                req.salaireMensuelBrutEuros(),
                req.remunerationBruteTotaleContratEuros(),
                r.analyseRuptureAnticipee(),
                r.moisRestantsContrat(),
                r.pointsAnalyse(),
                r.indemnitesRuptureAnticipeeEuros(),
                r.indemnitePrecariteEuros(),
                r.totalDuSalarieEuros(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
