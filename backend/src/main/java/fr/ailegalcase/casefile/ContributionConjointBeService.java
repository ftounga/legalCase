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
 * SF-217-08 : service orchestrant l'analyse de la pension alimentaire entre
 * ex-époux belge + persistance snapshot (un seul résultat courant par dossier).
 */
@Service
public class ContributionConjointBeService {

    private final ContributionConjointBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ContributionConjointBeService(ContributionConjointBeRepository repository,
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
    public ContributionConjointBeResponse calculate(UUID caseFileId,
                                                    ContributionConjointBeRequest request,
                                                    OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();
        requireBelgique(country);

        ContributionConjointBeResult result;
        try {
            result = ContributionConjointBeCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ContributionConjointBeResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        ContributionConjointBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ContributionConjointBeAnalysis a = new ContributionConjointBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ContributionConjointBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        requireBelgique(caseFile.getWorkspace().getCountry());
        ContributionConjointBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de pension alimentaire entre ex-époux trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private void requireBelgique(String country) {
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Pension alimentaire entre ex-époux — outil disponible uniquement "
                            + "pour les workspaces BELGIQUE");
        }
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ce dossier n'est pas un dossier de droit de la famille");
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

    private ContributionConjointBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ContributionConjointBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ContributionConjointBeResponse toResponse(UUID caseFileId,
                                                      ContributionConjointBeRequest req,
                                                      ContributionConjointBeResult r,
                                                      Instant calculatedAt) {
        return new ContributionConjointBeResponse(
                caseFileId,
                req.typeDivorce(),
                req.renonciationPensionConvention(),
                req.creancierEnEtatDeBesoin(),
                req.fauteGraveCreancier(),
                req.dureeMariageAnnees(),
                req.revenuMensuelCreancier(),
                req.revenuMensuelDebiteur(),
                req.degradationEconomiqueLieeAuMariage(),
                req.commentaire(),
                r.verdict(),
                r.dureeMaximaleMois(),
                r.montantMensuelIndicatif(),
                r.plafondTiersRevenusDebiteur(),
                r.motifsExclusion(),
                r.detailCalcul(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
