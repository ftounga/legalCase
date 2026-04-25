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
 * SF-FA-23-01 : service orchestrant le calcul de probabilité d'obtention d'une
 * ordonnance sur requête (mesures urgentes familiales — art. 493 CPC FR / 1025 CJ BE).
 *
 * <p>Gates appliqués :</p>
 * <ul>
 *   <li>{@code legalDomain == DROIT_FAMILLE} (sinon 400)</li>
 *   <li>workspace de l'utilisateur correspond au case file (sinon 404)</li>
 * </ul>
 *
 * <p>Pas de gate pays : la procédure est applicable FR + BE.</p>
 */
@Service
public class OrdonnanceRequeteService {

    private final OrdonnanceRequeteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public OrdonnanceRequeteService(OrdonnanceRequeteRepository repository,
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
    public OrdonnanceRequeteResponse calculate(UUID caseFileId,
                                                OrdonnanceRequeteRequest request,
                                                OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.motifRequete() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motif de requête requis");
        }
        if (request.urgenceJustifiee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Urgence justifiée requise (true/false)");
        }
        if (request.derogationContradictoireJustifiee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dérogation au contradictoire justifiée requise (true/false)");
        }
        if (request.pieceJustificativeFournie() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pièce justificative fournie requise (true/false)");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();
        boolean presenceEnfants = Boolean.TRUE.equals(request.presenceEnfants());

        OrdonnanceRequeteResult result;
        try {
            result = OrdonnanceRequeteCalculator.compute(
                    request.motifRequete(),
                    request.urgenceJustifiee(),
                    request.derogationContradictoireJustifiee(),
                    request.pieceJustificativeFournie(),
                    presenceEnfants,
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        OrdonnanceRequeteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    OrdonnanceRequeteAnalysis a = new OrdonnanceRequeteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setMotifRequete(result.motifRequete());
        entity.setUrgenceJustifiee(result.urgenceJustifiee());
        entity.setDerogationContradictoireJustifiee(result.derogationContradictoireJustifiee());
        entity.setPieceJustificativeFournie(result.pieceJustificativeFournie());
        entity.setPresenceEnfants(result.presenceEnfants());
        entity.setCommentaireUrgence(request.commentaireUrgence());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public OrdonnanceRequeteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        OrdonnanceRequeteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Ordonnance sur requête trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private OrdonnanceRequeteResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, OrdonnanceRequeteResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private OrdonnanceRequeteResponse toResponse(UUID caseFileId, OrdonnanceRequeteResult r) {
        return new OrdonnanceRequeteResponse(
                caseFileId,
                r.motifRequete(),
                r.scoreEligibilite(),
                r.verdictAccordeProbabilite(),
                r.criteresRemplis() != null ? r.criteresRemplis() : List.of(),
                r.criteresManquants() != null ? r.criteresManquants() : List.of(),
                r.delaiTypiqueJoursMin(),
                r.delaiTypiqueJoursMax(),
                r.recoursAdverseDelaiJours(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
