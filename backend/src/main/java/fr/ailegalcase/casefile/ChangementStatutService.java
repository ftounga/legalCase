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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * SF-IM-11-01 : service du changement de statut CESEDA. Gates : DROIT_IMMIGRATION + FRANCE.
 */
@Service
public class ChangementStatutService {

    private final ChangementStatutRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ChangementStatutService(ChangementStatutRepository repository,
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
    public ChangementStatutResponse calculate(UUID caseFileId,
                                              ChangementStatutRequest request,
                                              OidcUser oidcUser,
                                              Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime de changement de statut propre à la France (CESEDA L.421+)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dureeRestanteSurTitreActuelMois() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dureeRestanteSurTitreActuelMois est requis");
        }

        boolean justificatif = Boolean.TRUE.equals(request.documentJustificatifFourni());
        // Default true : casier vierge sauf information contraire explicite
        boolean casierVierge = !Boolean.FALSE.equals(request.casierJudiciaireVierge());

        ChangementStatutResult result;
        try {
            result = ChangementStatutCalculator.compute(
                    request.titreActuel(),
                    request.titreEnvisage(),
                    request.dureeRestanteSurTitreActuelMois(),
                    justificatif,
                    request.remunerationContratEur(),
                    casierVierge);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ChangementStatutAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ChangementStatutAnalysis a = new ChangementStatutAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTitreActuel(result.titreActuel());
        entity.setTitreEnvisage(result.titreEnvisage());
        entity.setDureeRestanteMois(result.dureeRestanteMois());
        entity.setDocumentJustificatifFourni(result.documentJustificatifFourni());
        entity.setRemunerationContratEur(result.remunerationContratEur());
        entity.setCasierJudiciaireVierge(result.casierJudiciaireVierge());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ChangementStatutResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ChangementStatutAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de changement de statut trouvée pour ce dossier"));
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
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

    private ChangementStatutResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ChangementStatutResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private ChangementStatutResponse toResponse(UUID caseFileId, String country,
                                                ChangementStatutResult r) {
        BigDecimal remu = r.remunerationContratEur();
        return new ChangementStatutResponse(
                caseFileId,
                country,
                r.titreActuel(),
                r.titreEnvisage(),
                r.nouveauTitreEnvisage(),
                r.dureeRestanteMois(),
                r.documentJustificatifFourni(),
                remu,
                r.casierJudiciaireVierge(),
                r.verdictTransition(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.risqueRefus() != null ? r.risqueRefus() : List.of(),
                r.delaiInstructionMois(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}
