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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-IM-20-01 : service de l'analyse de mesure d'éloignement administrative française
 * (Expulsion / IRTF / IAT). Gates : DROIT_IMMIGRATION + FRANCE.
 */
@Service
public class MesuresEloignementService {

    private final MesuresEloignementRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MesuresEloignementService(MesuresEloignementRepository repository,
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
    public MesuresEloignementResponse calculate(UUID caseFileId,
                                                MesuresEloignementRequest request,
                                                OidcUser oidcUser,
                                                Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Régime des mesures d'éloignement propre à la France (CESEDA L.631+)");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        MesuresEloignementResult result;
        try {
            result = MesuresEloignementCalculator.compute(
                    request.dispositif(),
                    request.motifMenace(),
                    request.procedureCommissionRespectee(),
                    request.urgenceAbsolueJustifiee(),
                    request.dureeCircularitePrecaire(),
                    request.dureePresenceIrreguliereMois(),
                    request.comportementAggravant(),
                    request.recoursDelai(),
                    LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        MesuresEloignementAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    MesuresEloignementAnalysis a = new MesuresEloignementAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDispositif(result.dispositif());
        entity.setMotifMenace(result.motifMenace());
        entity.setProcedureCommissionRespectee(request.procedureCommissionRespectee());
        entity.setUrgenceAbsolueJustifiee(request.urgenceAbsolueJustifiee());
        entity.setDureeCircularitePrecaire(request.dureeCircularitePrecaire());
        entity.setDureePresenceIrreguliereMois(request.dureePresenceIrreguliereMois());
        entity.setComportementAggravant(request.comportementAggravant());
        entity.setRecoursDelai(request.recoursDelai());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, request, result);
    }

    @Transactional(readOnly = true)
    public MesuresEloignementResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        MesuresEloignementAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de mesure d'éloignement trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        MesuresEloignementResult result = deserialize(entity.getResultData());
        MesuresEloignementRequest reconstructed = new MesuresEloignementRequest(
                entity.getDispositif(),
                entity.getMotifMenace(),
                entity.getProcedureCommissionRespectee(),
                entity.getUrgenceAbsolueJustifiee(),
                entity.getDureeCircularitePrecaire(),
                entity.getDureePresenceIrreguliereMois(),
                entity.getComportementAggravant(),
                entity.getRecoursDelai());
        return toResponse(caseFileId, country, reconstructed, result);
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

    private MesuresEloignementResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, MesuresEloignementResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private MesuresEloignementResponse toResponse(UUID caseFileId, String country,
                                                  MesuresEloignementRequest request,
                                                  MesuresEloignementResult r) {
        return new MesuresEloignementResponse(
                caseFileId,
                country,
                r.dispositif(),
                r.dispositifRecommande(),
                r.motifMenace(),
                request.procedureCommissionRespectee(),
                request.urgenceAbsolueJustifiee(),
                request.dureeCircularitePrecaire(),
                request.dureePresenceIrreguliereMois(),
                request.comportementAggravant(),
                request.recoursDelai(),
                r.verdictLegalite(),
                r.risqueAnnulation() != null ? r.risqueAnnulation() : List.of(),
                r.delaiRecoursJours(),
                r.juridictionRecours(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}
