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
 * SF-DT-31-01 : service orchestrant l'analyse de validité d'un protocole
 * transactionnel + persistance snapshot.
 */
@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository repository,
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
    public TransactionResponse calculate(UUID caseFileId,
                                         TransactionRequest request,
                                         OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        TransactionResult result;
        try {
            result = TransactionCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        TransactionResponse response = toResponse(caseFileId, request, result);

        TransactionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    TransactionAnalysis a = new TransactionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        TransactionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de transaction trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
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

    private TransactionResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, TransactionResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private TransactionResponse toResponse(UUID caseFileId,
                                           TransactionRequest req,
                                           TransactionResult r) {
        return new TransactionResponse(
                caseFileId,
                req.dateSignature(),
                req.concessionsEmployeur() == null ? java.util.List.of() : java.util.List.copyOf(req.concessionsEmployeur()),
                req.concessionsSalarie() == null ? java.util.List.of() : java.util.List.copyOf(req.concessionsSalarie()),
                req.indemniteTransactionnelleEur(),
                req.salaireMensuelBrutEur(),
                req.ancienneteAnnees(),
                req.renonciationActionExpresse(),
                req.delaiReflexion15jOk(),
                req.rupturePrealable(),
                req.presenceAvocatAssistance(),
                req.viceConsentementAllegue(),
                r.concessionsReciproquesCaracterisees(),
                r.ratioConcessionsEmployeurPct(),
                r.indemniteTransactionnelleSuperieureMacron(),
                r.scoreValidite(),
                r.verdictValiditeContrat(),
                r.risqueNulliteRetenu(),
                r.baseJuridique(),
                r.formule(),
                r.messages(),
                r.country()
        );
    }
}
