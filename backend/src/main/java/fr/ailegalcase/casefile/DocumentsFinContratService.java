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
 * SF-DT-32-01 : service orchestrant la conformité documents de fin de contrat + persistance snapshot.
 */
@Service
public class DocumentsFinContratService {

    private final DocumentsFinContratRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DocumentsFinContratService(DocumentsFinContratRepository repository,
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
    public DocumentsFinContratResponse calculate(UUID caseFileId,
                                                 DocumentsFinContratRequest request,
                                                 OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        DocumentsFinContratResult result;
        try {
            result = DocumentsFinContratCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DocumentsFinContratResponse response = toResponse(caseFileId, request, result);

        DocumentsFinContratAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DocumentsFinContratAnalysis a = new DocumentsFinContratAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public DocumentsFinContratResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        DocumentsFinContratAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de documents de fin de contrat trouvée pour ce dossier"));
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

    private DocumentsFinContratResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, DocumentsFinContratResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DocumentsFinContratResponse toResponse(UUID caseFileId,
                                                   DocumentsFinContratRequest req,
                                                   DocumentsFinContratResult r) {
        return new DocumentsFinContratResponse(
                caseFileId,
                req.dateFinContrat(),
                req.certificatTravailRemis(),
                req.dateCertificatTravail(),
                req.attestationFranceTravailRemise(),
                req.dateAttestationFranceTravail(),
                req.souldeToutCompteSigne(),
                req.dateSouldeToutCompte(),
                req.salaireMensuelBrutEur(),
                r.certificatRemisDansDelai(),
                r.attestationRemiseDansDelai(),
                r.souldeToutCompteValide(),
                r.souldeToutCompteContestable(),
                r.totalSanctionsCumulables(),
                r.indemniteRetardCertificatEur(),
                r.indemniteRetardAttestationEur(),
                r.scoreConformiteEmployeur(),
                r.verdictRisqueContentieux(),
                r.baseJuridique(),
                r.formule(),
                r.messages(),
                r.messagesContentieux(),
                r.country()
        );
    }
}
