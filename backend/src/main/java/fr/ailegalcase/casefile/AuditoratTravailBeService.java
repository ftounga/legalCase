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
 * SF-219-25 : service orchestrant l'analyse <i>Auditorat du travail
 * BE — orientation et checklist de saisine du parquet spécialisé en
 * droit social pénal</i> (Code judiciaire art. 138bis + Code
 * d'instruction criminelle art. 24 + Loi du 03/08/1992 sur le Code
 * judiciaire + Loi du 06/06/2010 introduisant le Code pénal social).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14/15/16/
 * 17/18/19/20/21/22/23/24) + isolation workspace standard + gate
 * domaine {@code DROIT_DU_TRAVAIL} (400) + persistance JSON snapshot.</p>
 *
 * <p>L'auditorat du travail est un concept structurel <b>spécifique
 * au système judiciaire belge</b> (parquet spécialisé du tribunal du
 * travail). La France n'a pas d'équivalent direct : les infractions
 * au droit du travail sont poursuivies par le procureur de la
 * République (parquet généraliste) sur PV de l'inspection du travail
 * (Code du travail art. L. 8112-1 et s.). Pas de tentative
 * d'harmonisation FR/BE — outil BE-only.</p>
 *
 * <p>Le checker sous-jacent ({@link AuditoratTravailBeChecker}) est
 * une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class AuditoratTravailBeService {

    private final AuditoratTravailBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AuditoratTravailBeService(
            AuditoratTravailBeAnalysisRepository repository,
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
    public AuditoratTravailBeResponse analyze(
            UUID caseFileId,
            AuditoratTravailBeRequest request,
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
        // l'existence de l'outil côté FR — pattern miroir
        // SF-219-06/08/10/12/14/15/16/17/18/19/20/21/22/23/24. La
        // France n'a pas d'auditorat du travail (parquet spécialisé) :
        // les infractions au droit du travail sont poursuivies par le
        // procureur de la République (parquet généraliste) sur PV de
        // l'inspection du travail (Code du travail art. L. 8112-1 et
        // s.). Pas de tentative d'harmonisation FR/BE — outil BE-only.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        AuditoratTravailBeResult result;
        try {
            result = AuditoratTravailBeChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        AuditoratTravailBeResponse response = toResponse(caseFileId, result);

        AuditoratTravailBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            AuditoratTravailBeAnalysis a =
                                    new AuditoratTravailBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public AuditoratTravailBeResponse get(
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

        AuditoratTravailBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse Auditorat travail BE"
                                        + " trouvée pour ce dossier"));
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

    private AuditoratTravailBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    AuditoratTravailBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private AuditoratTravailBeResponse toResponse(
            UUID caseFileId, AuditoratTravailBeResult r) {
        return new AuditoratTravailBeResponse(
                caseFileId,
                r.natureFait(),
                r.modeSaisineEnvisage(),
                r.dateFaits(),
                r.faitsPrescrits(),
                r.inspectionDejaSaisie(),
                r.plainteCivileDeposee(),
                r.urgenceSecuritePersonnes(),
                r.recoursPenalEnvisage(),
                r.employeurPersonneMorale(),
                r.verdict(),
                r.modeSaisineRecommande(),
                r.constitutionPartieCivileRecommandee(),
                r.inspectionPrealableRequise(),
                r.voieCivileConcurrente(),
                r.prescriptionBloquante(),
                r.urgenceSignalee(),
                r.unaViaApplicable(),
                r.avisAuditeurObligatoireCivil(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
