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
 * SF-FA-27-01 : service applicatif PMA / GPA / bioéthique (loi 2/8/2021 + Cass. 18/12/2022).
 * Gates : DROIT_FAMILLE + FRANCE.
 */
@Service
public class PmaGpaBioethiqueService {

    private final PmaGpaBioethiqueRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PmaGpaBioethiqueService(PmaGpaBioethiqueRepository repository,
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
    public PmaGpaBioethiqueResponse calculate(UUID caseFileId,
                                              PmaGpaBioethiqueRequest request,
                                              OidcUser oidcUser,
                                              Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil disponible uniquement en FRANCE — équivalent BE "
                            + "(loi 6/7/2007 PMA BE) au backlog");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }

        PmaGpaBioethiqueResult result;
        try {
            result = PmaGpaBioethiqueCalculator.compute(
                    request.dispositif(),
                    request.consentementsConjointsNotaire(),
                    request.dateReconnaissanceAnterieurePMA(),
                    request.datePMA(),
                    request.conditionsAccesPMA(),
                    request.paysGPALegal(),
                    request.parentBiologiqueAvere(),
                    request.decisionEtrangereProduite(),
                    request.adoptionDemande(),
                    request.dateDon(),
                    request.demandeAcces(),
                    request.ageDemandeur());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PmaGpaBioethiqueAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PmaGpaBioethiqueAnalysis a = new PmaGpaBioethiqueAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDispositif(result.dispositif());
        entity.setConsentementsConjointsNotaire(request.consentementsConjointsNotaire());
        entity.setDateReconnaissanceAnterieurePMA(request.dateReconnaissanceAnterieurePMA());
        entity.setDatePMA(request.datePMA());
        entity.setConditionsAccesPMA(request.conditionsAccesPMA());
        entity.setPaysGPALegal(request.paysGPALegal());
        entity.setParentBiologiqueAvere(request.parentBiologiqueAvere());
        entity.setDecisionEtrangereProduite(request.decisionEtrangereProduite());
        entity.setAdoptionDemande(request.adoptionDemande());
        entity.setDateDon(request.dateDon());
        entity.setDemandeAcces(request.demandeAcces());
        entity.setAgeDemandeur(request.ageDemandeur());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public PmaGpaBioethiqueResponse get(UUID caseFileId,
                                        OidcUser oidcUser,
                                        Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        PmaGpaBioethiqueAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de PMA/GPA/bioéthique trouvée pour ce dossier"));
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
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private PmaGpaBioethiqueResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, PmaGpaBioethiqueResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private PmaGpaBioethiqueResponse toResponse(UUID caseFileId, String country,
                                                PmaGpaBioethiqueResult r) {
        return new PmaGpaBioethiqueResponse(
                caseFileId,
                r.dispositif(),
                r.verdictRecevabilite(),
                r.proceduresAFollow() != null ? r.proceduresAFollow() : List.of(),
                r.risqueRefus(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.delaiInstructionMois(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
