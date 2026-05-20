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
 * SF-206-03 : service orchestrant le chiffrage du rappel de congés payés
 * acquis pendant un arrêt maladie (FR) + persistance snapshot (un seul
 * résultat courant par dossier).
 */
@Service
public class CongesPayesArretMaladieService {

    private final CongesPayesArretMaladieRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CongesPayesArretMaladieService(CongesPayesArretMaladieRepository repository,
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
    public CongesPayesArretMaladieResponse calculate(UUID caseFileId,
                                                     CongesPayesArretMaladieRequest request,
                                                     OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        CongesPayesArretMaladieResult result;
        try {
            result = CongesPayesArretMaladieCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            // FR-only et input invalide → 422 (outil hors pays) ou 400 (input).
            if (e.getMessage() != null && e.getMessage().contains("FRANCE")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CongesPayesArretMaladieResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        CongesPayesArretMaladieAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CongesPayesArretMaladieAnalysis a = new CongesPayesArretMaladieAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public CongesPayesArretMaladieResponse get(UUID caseFileId,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        CongesPayesArretMaladieAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun chiffrage de congés payés sur arrêt maladie trouvé pour ce dossier"));
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

    private CongesPayesArretMaladieResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, CongesPayesArretMaladieResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CongesPayesArretMaladieResponse toResponse(UUID caseFileId,
                                                       CongesPayesArretMaladieRequest req,
                                                       CongesPayesArretMaladieResult r,
                                                       Instant calculatedAt) {
        return new CongesPayesArretMaladieResponse(
                caseFileId,
                req.typeArret(),
                req.nombreMoisArret(),
                req.salarieEncoreEnPoste(),
                req.dateRuptureContrat(),
                req.joursCpDejaAccordes(),
                req.salaireBrutMensuel(),
                r.verdict(),
                r.joursCpAcquis(),
                r.joursCpRappel(),
                r.valorisationIndicativeEur(),
                r.dateLimiteAction(),
                r.actionEncoreOuverte(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
