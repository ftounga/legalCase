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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-12-01 : service applicatif des mesures provisoires (AOMP). Gate
 * FR + DROIT_FAMILLE, isolation workspace, persistance JSON.
 */
@Service
public class MesuresProvisoiresService {

    private final MesuresProvisoiresRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MesuresProvisoiresService(MesuresProvisoiresRepository repository,
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
    public MesuresProvisoiresResponse calculate(UUID caseFileId,
                                                MesuresProvisoiresRequest request,
                                                OidcUser oidcUser,
                                                Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mesures provisoires : procédure propre au droit français "
                            + "(art. 254-256 Cciv). BE = F-FA-11.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        if (request.dateAudienceAOMP() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateAudienceAOMP est requis");
        }
        if (request.revenusEpouxDemandeurEur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "revenusEpouxDemandeurEur est requis");
        }
        if (request.revenusEpouxDefendeurEur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "revenusEpouxDefendeurEur est requis");
        }
        if (request.logementProprietaire() == null
                || request.logementProprietaire().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "logementProprietaire est requis");
        }
        if (request.souhaitResidenceEnfants() == null
                || request.souhaitResidenceEnfants().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "souhaitResidenceEnfants est requis");
        }

        boolean violences = Boolean.TRUE.equals(request.violencesAlleguees());
        boolean patrimoine = Boolean.TRUE.equals(request.patrimoineCommunIsignificatif());
        boolean demandeMC = Boolean.TRUE.equals(request.demandeMesureConservatoire());

        List<MesuresProvisoiresResult.EnfantMineurInfo> enfants = new ArrayList<>();
        if (request.enfantsMineurs() != null) {
            for (MesuresProvisoiresRequest.EnfantMineurInfo e : request.enfantsMineurs()) {
                if (e == null) continue;
                if (e.age() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Âge enfant manquant");
                }
                enfants.add(new MesuresProvisoiresResult.EnfantMineurInfo(
                        e.prenom(), e.age()));
            }
        }

        MesuresProvisoiresResult result;
        try {
            result = MesuresProvisoiresCalculator.compute(
                    request.dateAudienceAOMP(),
                    request.revenusEpouxDemandeurEur(),
                    request.revenusEpouxDefendeurEur(),
                    request.logementCommunDescription(),
                    request.logementProprietaire(),
                    enfants,
                    request.souhaitResidenceEnfants(),
                    violences,
                    patrimoine,
                    demandeMC);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        MesuresProvisoiresAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    MesuresProvisoiresAnalysis a = new MesuresProvisoiresAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateAudienceAOMP(result.dateAudienceAOMP());
        entity.setRevenusEpouxDemandeurEur(result.revenusEpouxDemandeurEur());
        entity.setRevenusEpouxDefendeurEur(result.revenusEpouxDefendeurEur());
        entity.setLogementProprietaire(result.logementProprietaire());
        entity.setSouhaitResidenceEnfants(result.souhaitResidenceEnfants());
        entity.setViolencesAlleguees(result.violencesAlleguees());
        entity.setPatrimoineCommunIsignificatif(result.patrimoineCommunIsignificatif());
        entity.setDemandeMesureConservatoire(result.demandeMesureConservatoire());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public MesuresProvisoiresResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        MesuresProvisoiresAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de mesures provisoires trouvée pour ce dossier"));
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

    private MesuresProvisoiresResult deserialize(String json) {
        try { return objectMapper.readValue(json, MesuresProvisoiresResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private MesuresProvisoiresResponse toResponse(UUID caseFileId, String country,
                                                  MesuresProvisoiresResult r) {
        return new MesuresProvisoiresResponse(
                caseFileId,
                r.dateAudienceAOMP(),
                r.revenusEpouxDemandeurEur(),
                r.revenusEpouxDefendeurEur(),
                r.logementCommunDescription(),
                r.logementProprietaire(),
                r.enfantsMineurs() != null ? r.enfantsMineurs() : List.of(),
                r.souhaitResidenceEnfants(),
                r.violencesAlleguees(),
                r.patrimoineCommunIsignificatif(),
                r.demandeMesureConservatoire(),
                country,
                r.differentielRevenus(),
                r.pensionAlimentairePropose(),
                r.attributionLogementRecommande(),
                r.residenceEnfantsRecommande(),
                r.contributionCharges(),
                r.mesureConservatoireRecommande(),
                r.scoreCohesionMesures(),
                r.verdictAcceptabilite(),
                r.formule(),
                r.baseJuridique(),
                r.messages() != null ? r.messages() : List.of()
        );
    }
}
