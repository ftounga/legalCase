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
 * SF-FA-24-03 : service orchestrant l'analyse de validité d'un testament
 * (FR — DROIT_FAMILLE — art. 967-1035 + 901-911 Cciv).
 */
@Service
public class TestamentValiditeService {

    private final TestamentValiditeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public TestamentValiditeService(TestamentValiditeRepository repository,
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
    public TestamentValiditeResponse calculate(UUID caseFileId,
                                               TestamentValiditeRequest request,
                                               OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.formeTestament() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Forme du testament requise");
        }
        if (request.dateRedaction() == null || request.dateRedaction().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date de rédaction du testament requise");
        }
        if (request.ageTestateurAnsRedaction() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Âge du testateur à la rédaction requis");
        }
        if (request.saineDEsprit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "État sain d'esprit (oui/non) requis (art. 901 Cciv)");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        TestamentValiditeResult result;
        try {
            result = TestamentValiditeCalculator.compute(
                    request.formeTestament(),
                    request.dateRedaction(),
                    request.ageTestateurAnsRedaction(),
                    request.saineDEsprit(),
                    request.majeurProtegeAvecAssistance(),
                    request.ecritureManuscritIntegrale(),
                    request.dateComplete(),
                    request.signatureTestateur(),
                    request.presenceNotaireEtTemoinsConforme(),
                    request.dicteEnPresence(),
                    request.lectureFinaleAuTestateur(),
                    request.signaturesCompletes(),
                    request.remiseSousPliCache(),
                    request.declarationDevant2Temoins(),
                    request.acteSuscriptionNotaire(),
                    request.respecteFormeWashington(),
                    request.vicesConsentementDol(),
                    request.erreurSubstantielle(),
                    request.testamentPosterieurContradictoire(),
                    request.dechirureVolontaireOriginal(),
                    request.legsExcedeQuotiteDisponible(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        TestamentValiditeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    TestamentValiditeAnalysis a = new TestamentValiditeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setFormeTestament(result.formeTestament());
        entity.setDateRedaction(result.dateRedaction());
        entity.setAgeTestateurAnsRedaction(result.ageTestateurAnsRedaction());
        entity.setVerdictValidite(result.verdictValidite());
        entity.setActionEnReductionPossible(result.actionEnReductionPossible());
        entity.setScoreEligibilite(result.scoreEligibilite());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public TestamentValiditeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        TestamentValiditeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de validité de testament trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
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
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private TestamentValiditeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, TestamentValiditeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private TestamentValiditeResponse toResponse(UUID caseFileId, TestamentValiditeResult r) {
        return new TestamentValiditeResponse(
                caseFileId,
                r.formeTestament(),
                r.verdictValidite(),
                r.vicesIdentifies() != null ? r.vicesIdentifies() : List.of(),
                r.actionEnReductionPossible(),
                r.delaiContestationAns(),
                r.scoreEligibilite(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
