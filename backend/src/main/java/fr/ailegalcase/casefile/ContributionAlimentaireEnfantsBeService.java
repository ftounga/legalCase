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
 * SF-217-06 : service orchestrant l'estimation de la contribution alimentaire
 * des enfants belge + persistance snapshot (un seul résultat courant par dossier).
 */
@Service
public class ContributionAlimentaireEnfantsBeService {

    private final ContributionAlimentaireEnfantsBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ContributionAlimentaireEnfantsBeService(
            ContributionAlimentaireEnfantsBeRepository repository,
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
    public ContributionAlimentaireEnfantsBeResponse calculate(
            UUID caseFileId, ContributionAlimentaireEnfantsBeRequest request,
            OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();
        requireBelgique(country);

        ContributionAlimentaireEnfantsBeResult result;
        try {
            result = ContributionAlimentaireEnfantsBeCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ContributionAlimentaireEnfantsBeResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        ContributionAlimentaireEnfantsBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ContributionAlimentaireEnfantsBeAnalysis a =
                            new ContributionAlimentaireEnfantsBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ContributionAlimentaireEnfantsBeResponse get(UUID caseFileId,
                                                        OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        requireBelgique(caseFile.getWorkspace().getCountry());
        ContributionAlimentaireEnfantsBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de contribution alimentaire trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private void requireBelgique(String country) {
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Contribution alimentaire des enfants — outil disponible uniquement "
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

    private ContributionAlimentaireEnfantsBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ContributionAlimentaireEnfantsBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ContributionAlimentaireEnfantsBeResponse toResponse(
            UUID caseFileId, ContributionAlimentaireEnfantsBeRequest req,
            ContributionAlimentaireEnfantsBeResult r, Instant calculatedAt) {
        return new ContributionAlimentaireEnfantsBeResponse(
                caseFileId,
                req.nombreEnfants(),
                req.trancheAgeEnfants(),
                req.revenuMensuelParent1(),
                req.revenuMensuelParent2(),
                req.coutMensuelGlobalEnfants(),
                req.nuitsHebergementParent1(),
                req.nuitsHebergementParent2(),
                req.allocationsFamilialesMensuelles(),
                req.fraisExtraordinairesMensuels(),
                req.parentDebiteurEstParent1(),
                req.commentaire(),
                r.verdict(),
                r.coutMensuelRetenu(),
                r.coutNetApresAllocations(),
                r.quotePartParent1Pct(),
                r.quotePartParent2Pct(),
                r.partContributiveParent1(),
                r.partContributiveParent2(),
                r.partHebergementParent1(),
                r.partHebergementParent2(),
                r.contributionMensuelleNette(),
                r.parentDebiteur(),
                r.fraisExtraordinairesQuotePartParent1(),
                r.fraisExtraordinairesQuotePartParent2(),
                r.detailCalcul(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
