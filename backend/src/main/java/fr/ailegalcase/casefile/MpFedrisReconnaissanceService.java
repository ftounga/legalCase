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
 * SF-219-28 : service orchestrant l'analyse <i>Reconnaissance d'une
 * maladie professionnelle Fedris BE</i> (Lois coordonnees du 03/06/1970
 * + AR du 28/03/1969 liste fermee modifie 06/12/2018 + AR du 16/12/1985
 * systeme ouvert + Loi du 11/01/2018 reformant Fedris).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 cote FR — reponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/.../26) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>La France a un regime distinct (CPAM, tableaux art. L. 461-2 CSS,
 * CRRMP art. L. 461-1, delai prescription 2 ans art. L. 461-5 CSS).
 * Restitution separee par outil — pas de tentative d'harmonisation
 * FR/BE.</p>
 *
 * <p>Le checker sous-jacent ({@link MpFedrisReconnaissanceChecker}) est
 * une fonction pure independante du contexte HTTP / persistance.</p>
 */
@Service
public class MpFedrisReconnaissanceService {

    private final MpFedrisReconnaissanceAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MpFedrisReconnaissanceService(
            MpFedrisReconnaissanceAnalysisRepository repository,
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
    public MpFedrisReconnaissanceResponse analyze(
            UUID caseFileId,
            MpFedrisReconnaissanceRequest request,
            OidcUser oidcUser,
            Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer
        // l'existence de l'outil cote FR — pattern miroir vagues F-219.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        MpFedrisReconnaissanceResult result;
        try {
            result = MpFedrisReconnaissanceChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        MpFedrisReconnaissanceResponse response = toResponse(caseFileId, result);

        MpFedrisReconnaissanceAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            MpFedrisReconnaissanceAnalysis a =
                                    new MpFedrisReconnaissanceAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public MpFedrisReconnaissanceResponse get(
            UUID caseFileId,
            OidcUser oidcUser,
            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        MpFedrisReconnaissanceAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse de reconnaissance MP Fedris"
                                        + " trouvee pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId()
                        .equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }
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
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private MpFedrisReconnaissanceResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    MpFedrisReconnaissanceResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private MpFedrisReconnaissanceResponse toResponse(
            UUID caseFileId, MpFedrisReconnaissanceResult r) {
        return new MpFedrisReconnaissanceResponse(
                caseFileId,
                r.typeMaladie(),
                r.codeMaladieListe(),
                r.libelleMaladie(),
                r.dateConstatationMedicale(),
                r.dateConnaissanceProfessionnel(),
                r.dateDeclarationEnvisagee(),
                r.exposition(),
                r.causalite(),
                r.verdict(),
                r.presomptionApplicable(),
                r.causaliteRequise(),
                r.datePrescription(),
                r.prescriteAuJour(),
                r.joursAvantPrescription(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
