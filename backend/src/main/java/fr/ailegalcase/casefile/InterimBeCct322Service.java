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
 * SF-219-14 : service orchestrant l'analyse <i>statut intérim BE —
 * CCT n° 322</i> (Loi du 24/07/1987 + CCT n° 322 du 14/06/2010 du CNT).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>Le checker sous-jacent ({@link InterimBeCct322Checker}) est une
 * fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class InterimBeCct322Service {

    private final InterimBeCct322AnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public InterimBeCct322Service(
            InterimBeCct322AnalysisRepository repository,
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
    public InterimBeCct322Response analyze(
            UUID caseFileId,
            InterimBeCct322Request request,
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
        // l'existence de l'outil côté FR — pattern miroir SF-219-06/08/10/12.
        // Le régime français du « travail temporaire » (C. trav. art.
        // L. 1251-1 et suiv.) partage la structure tripartite mais avec
        // des motifs (CDD d'usage, accroissement d'activité,
        // remplacement), des durées (18 mois max en règle générale art.
        // L. 1251-12), des indemnités (IFM 10 %, ICCP 10 %) et une
        // jurisprudence Cass. soc. radicalement distincts. Restitution
        // séparée.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        InterimBeCct322Result result;
        try {
            result = InterimBeCct322Checker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        InterimBeCct322Response response = toResponse(caseFileId, result);

        InterimBeCct322Analysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            InterimBeCct322Analysis a =
                                    new InterimBeCct322Analysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public InterimBeCct322Response get(
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

        InterimBeCct322Analysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse intérim BE CCT 322 trouvée"
                                        + " pour ce dossier"));
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

    private InterimBeCct322Response deserialize(String json) {
        try {
            return objectMapper.readValue(json, InterimBeCct322Response.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private InterimBeCct322Response toResponse(
            UUID caseFileId, InterimBeCct322Result r) {
        return new InterimBeCct322Response(
                caseFileId,
                r.dateDebutMission(),
                r.motifMission(),
                r.remplacementGreveOuLockout(),
                r.dureeTotaleMissionJours(),
                r.dureeMaxLegaleJours(),
                r.salaireHoraireIntimaireBrut(),
                r.salaireHoraireReferenceBrut(),
                r.contratEcritSigne(),
                r.dimonaDeclareeParEti(),
                r.verdict(),
                r.motifAutorise(),
                r.dureeRespectee(),
                r.pariteRespectee(),
                r.formalismeRespecte(),
                r.joursExcedentaires(),
                r.ecartPariteSalariale(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
