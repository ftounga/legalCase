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
 * SF-220-06 : service de l'outil décisionnel « contestation / radiation d'un
 * signalement SIS aux fins de non-admission » (Règl. UE 2018/1860 / CESEDA
 * L.312-3, F-IM-52-signalement-sis-fr). Outil single-country FR.
 *
 * <p>Pattern miroir de {@link DecheanceNationaliteService} (SF-220-05).</p>
 */
@Service
public class SignalementSisService {

    private final SignalementSisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public SignalementSisService(SignalementSisRepository repository,
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
    public SignalementSisResponse analyze(UUID caseFileId, SignalementSisRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Signalement SIS — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.etatSignalant() != null
                && !SignalementSisAnalyzer.ETAT_SIGNALANT_VALEURS.contains(request.etatSignalant())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ etatSignalant doit être l'une des valeurs "
                            + SignalementSisAnalyzer.ETAT_SIGNALANT_VALEURS);
        }
        if (request.motifSignalement() != null
                && !SignalementSisAnalyzer.MOTIF_SIGNALEMENT_VALEURS.contains(request.motifSignalement())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ motifSignalement doit être l'une des valeurs "
                            + SignalementSisAnalyzer.MOTIF_SIGNALEMENT_VALEURS);
        }

        SignalementSisResult result = SignalementSisAnalyzer.analyze(
                request.signalementConnu(),
                request.etatSignalant(),
                request.motifSignalement(),
                request.titreSejourValide());

        SignalementSisAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    SignalementSisAnalysis a = new SignalementSisAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setSignalementConnu(request.signalementConnu());
        entity.setEtatSignalant(request.etatSignalant());
        entity.setMotifSignalement(request.motifSignalement());
        entity.setTitreSejourValide(request.titreSejourValide());
        entity.setDateSignalement(request.dateSignalement());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, request, result);
    }

    @Transactional(readOnly = true)
    public SignalementSisResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        SignalementSisAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de signalement SIS trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        SignalementSisResult r = deserialize(entity.getResultData());
        return new SignalementSisResponse(
                caseFileId,
                entity.getSignalementConnu(),
                entity.getEtatSignalant(),
                entity.getMotifSignalement(),
                entity.getTitreSejourValide(),
                entity.getDateSignalement(),
                country,
                r.actionPossible(),
                r.demarches(),
                r.autoriteCompetente(),
                r.basesJuridiques(),
                r.messages());
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private SignalementSisResult deserialize(String json) {
        try { return objectMapper.readValue(json, SignalementSisResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private SignalementSisResponse toResponse(UUID caseFileId, String country,
                                              SignalementSisRequest request,
                                              SignalementSisResult r) {
        return new SignalementSisResponse(
                caseFileId,
                r.signalementConnu(),
                r.etatSignalant(),
                r.motifSignalement(),
                r.titreSejourValide(),
                request.dateSignalement(),
                country,
                r.actionPossible(),
                r.demarches(),
                r.autoriteCompetente(),
                r.basesJuridiques(),
                r.messages());
    }
}
