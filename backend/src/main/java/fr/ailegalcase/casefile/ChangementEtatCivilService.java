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
 * SF-FA-26-01 : service applicatif changement d'état civil — nom, prénom,
 * sexe (art. 60 / 61-3-1 / 61-5 Cciv). Gate FR + DROIT_FAMILLE, isolation
 * workspace, persistance JSON.
 */
@Service
public class ChangementEtatCivilService {

    private final ChangementEtatCivilRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ChangementEtatCivilService(ChangementEtatCivilRepository repository,
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
    public ChangementEtatCivilResponse calculate(UUID caseFileId,
                                                 ChangementEtatCivilRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil disponible uniquement en FRANCE — équivalent BE "
                            + "(art. 370 CJ + loi 2017) en cours de scoping");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        if (request.typeChangement() == null
                || request.typeChangement().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "typeChangement est requis");
        }
        if (request.motifInvoque() == null
                || request.motifInvoque().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "motifInvoque est requis");
        }

        boolean majeur = request.majeurDemandeur() == null
                ? true : request.majeurDemandeur();
        boolean consentement = Boolean.TRUE.equals(request.consentementParental());
        boolean datesConcordants = Boolean.TRUE.equals(request.datesDocsConcordants());
        boolean dejaChange = Boolean.TRUE.equals(request.dejaChangeAuparavant());

        ChangementEtatCivilResult result;
        try {
            result = ChangementEtatCivilCalculator.compute(
                    request.typeChangement(),
                    request.motifInvoque(),
                    request.preuvesProduites(),
                    majeur,
                    consentement,
                    datesConcordants,
                    dejaChange,
                    request.dateNaissanceDemandeur(),
                    request.departementDeclaration());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ChangementEtatCivilAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ChangementEtatCivilAnalysis a = new ChangementEtatCivilAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTypeChangement(result.typeChangement());
        entity.setMotifInvoque(result.motifInvoque());
        entity.setPreuvesProduites(serialize(result.preuvesProduites()));
        entity.setMajeurDemandeur(result.majeurDemandeur());
        entity.setConsentementParental(result.consentementParental());
        entity.setDatesDocsConcordants(result.datesDocsConcordants());
        entity.setDejaChangeAuparavant(result.dejaChangeAuparavant());
        entity.setDateNaissanceDemandeur(result.dateNaissanceDemandeur());
        entity.setDepartementDeclaration(result.departementDeclaration());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ChangementEtatCivilResponse get(UUID caseFileId,
                                           OidcUser oidcUser,
                                           Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ChangementEtatCivilAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de changement d'état civil trouvée "
                                + "pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private ChangementEtatCivilResult deserialize(String json) {
        try { return objectMapper.readValue(json, ChangementEtatCivilResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private ChangementEtatCivilResponse toResponse(UUID caseFileId, String country,
                                                   ChangementEtatCivilResult r) {
        return new ChangementEtatCivilResponse(
                caseFileId,
                r.typeChangement(),
                r.motifInvoque(),
                r.preuvesProduites() != null ? r.preuvesProduites() : List.of(),
                r.majeurDemandeur(),
                r.consentementParental(),
                r.datesDocsConcordants(),
                r.dejaChangeAuparavant(),
                r.dateNaissanceDemandeur(),
                r.departementDeclaration(),
                r.competenceProcedure(),
                r.delaiInstructionMoisPrevisionnel(),
                r.scoreAcceptabilite(),
                r.verdictAcceptabilite(),
                r.documentsRequisManquants() != null
                        ? r.documentsRequisManquants() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
