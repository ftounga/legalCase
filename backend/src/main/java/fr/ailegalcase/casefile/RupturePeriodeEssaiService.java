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
import java.time.Instant;
import java.util.UUID;

/**
 * SF-DT-38-01 : service orchestrant la qualification d'une rupture pendant la
 * période d'essai + persistance snapshot (un seul résultat courant par dossier).
 */
@Service
public class RupturePeriodeEssaiService {

    private final RupturePeriodeEssaiRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RupturePeriodeEssaiService(RupturePeriodeEssaiRepository repository,
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
    public RupturePeriodeEssaiResponse calculate(UUID caseFileId,
                                                 RupturePeriodeEssaiRequest request,
                                                 OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        RupturePeriodeEssaiResult result;
        try {
            result = RupturePeriodeEssaiCalculator.compute(request.toInput(), country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RupturePeriodeEssaiResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        RupturePeriodeEssaiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RupturePeriodeEssaiAnalysis a = new RupturePeriodeEssaiAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public RupturePeriodeEssaiResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        RupturePeriodeEssaiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de rupture de période d'essai trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce dossier appartient à un autre workspace");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
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

    private RupturePeriodeEssaiResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, RupturePeriodeEssaiResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RupturePeriodeEssaiResponse toResponse(UUID caseFileId,
                                                   RupturePeriodeEssaiRequest req,
                                                   RupturePeriodeEssaiResult r,
                                                   Instant calculatedAt) {
        return new RupturePeriodeEssaiResponse(
                caseFileId,
                req.categorieSocioProfessionnelle(),
                req.typeContrat(),
                req.dureeCddMois(),
                req.dateDebutContrat(),
                req.dateRupture(),
                req.dureePeriodeEssaiContractuelleMois(),
                req.renouvellementInvoque(),
                req.accordBrancheRenouvellement(),
                req.accordEcritSalarieRenouvellement(),
                req.auteurRupture(),
                req.delaiPrevenanceJoursAppliques(),
                req.motifInvoque(),
                req.motifLieAuxCompetencesProfessionnelles(),
                req.motifEconomiqueOuOrganisationnel(),
                req.discriminationInvoquee(),
                req.grossesseAuMomentRupture(),
                req.arretAccidentTravailEnCours(),
                req.atteinteLiberteFondamentale(),
                req.lettreRuptureMotivee(),
                req.motifsAveresParPieces(),
                req.conventionCollectiveApplicable(),
                req.conventionCollectivePlusFavorableRespectee(),
                req.salaireMensuelBrut(),
                // SF-252-01 — 7 protections nullité additionnelles
                req.salarieProtege(),
                req.autorisationInspectionTravailObtenue(),
                req.lanceurAlerte(),
                req.temoinOuVictimeHarcelement(),
                req.droitDeRetraitExerce(),
                req.grossesseDeclareePostRupture(),
                req.dateNotificationGrossesse(),
                // SF-252b-01 — barème CDD/INTERIM précis
                req.dureePeriodeEssaiContractuelleJours(),
                r.verdict(),
                r.scoreIrregularite(),
                r.ancienneteJoursAuMomentRupture(),
                r.dureeLegaleMaximaleMois(),
                r.delaiPrevenanceLegalJours(),
                r.delaiPrevenanceRespecte(),
                r.anomaliesDetectees(),
                r.indemniteEstimee(),
                r.remedeReintegration(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt,
                // SF-252b-01 — Outputs additionnels
                r.dureeLegaleMaximaleJours(),
                r.indemnitePrevenanceEuros()
        );
    }
}
