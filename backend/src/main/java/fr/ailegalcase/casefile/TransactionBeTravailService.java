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

/**
 * SF-213-06 : service orchestrant l'analyse de validité d'une transaction
 * de fin de contrat en droit belge (art. 2044 Code civil belge + Loi
 * 03/07/1978 art. 6).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable d'un
 * dossier inconnu, pattern miroir SF-213-05) + isolation workspace
 * standard + persistance JSON snapshot.</p>
 *
 * <p>Le validateur sous-jacent ({@link TransactionBeTravailValidator}) est
 * une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class TransactionBeTravailService {

    private final TransactionBeTravailAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public TransactionBeTravailService(
            TransactionBeTravailAnalysisRepository repository,
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
    public TransactionBeTravailResponse analyze(
            UUID caseFileId,
            TransactionBeTravailRequest request,
            OidcUser oidcUser,
            Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer l'existence
        // de l'outil côté FR — réponse indistinguable d'un dossier inconnu.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        TransactionBeTravailResult result;
        try {
            result = TransactionBeTravailValidator.validate(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        TransactionBeTravailResponse response = toResponse(caseFileId, result);

        TransactionBeTravailAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    TransactionBeTravailAnalysis a = new TransactionBeTravailAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public TransactionBeTravailResponse get(UUID caseFileId,
                                            OidcUser oidcUser,
                                            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        TransactionBeTravailAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse transaction BE travail trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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

    private TransactionBeTravailResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, TransactionBeTravailResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private TransactionBeTravailResponse toResponse(UUID caseFileId, TransactionBeTravailResult r) {
        return new TransactionBeTravailResponse(
                caseFileId,
                r.montantTransactionBrut(),
                r.indemniteLegaleEtimee(),
                r.concessionsEmployeurDescrites(),
                r.renonciationsListees(),
                r.renonciationOrdrePublicDetectee(),
                r.mentionContestation(),
                r.verdict(),
                r.raisonInvalidite(),
                r.ratioPourcentage(),
                r.avertissement(),
                r.checklistRenonciations(),
                r.baseJuridique());
    }
}
