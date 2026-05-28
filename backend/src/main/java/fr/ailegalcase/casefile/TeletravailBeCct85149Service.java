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
 * SF-219-16 : service orchestrant l'analyse <i>télétravail BE — CCT
 * n° 85 (structurel) / CCT n° 149 (occasionnel)</i>.
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14) +
 * isolation workspace standard + gate domaine
 * {@code DROIT_DU_TRAVAIL} (400) + persistance JSON snapshot.</p>
 *
 * <p>Le checker sous-jacent ({@link TeletravailBeCct85149Checker}) est
 * une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class TeletravailBeCct85149Service {

    private final TeletravailBeCct85149AnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public TeletravailBeCct85149Service(
            TeletravailBeCct85149AnalysisRepository repository,
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
    public TeletravailBeCct85149Response analyze(
            UUID caseFileId,
            TeletravailBeCct85149Request request,
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
        // SF-219-06/08/10/12/14. Le régime français du télétravail
        // (ANI 19/07/2005 transposé, Loi 22/03/2012, ordonnances Macron
        // 22/09/2017 — C. trav. art. L. 1222-9 et suivants) partage
        // l'idée mais avec un cadre conventionnel / charte différent,
        // une indemnité forfaitaire régie par la circulaire URSSAF,
        // et un droit à la déconnexion (Loi El Khomri 08/08/2016,
        // C. trav. art. L. 2242-17) propre. Restitution séparée par
        // outil.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        TeletravailBeCct85149Result result;
        try {
            result = TeletravailBeCct85149Checker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        TeletravailBeCct85149Response response = toResponse(caseFileId, result);

        TeletravailBeCct85149Analysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            TeletravailBeCct85149Analysis a =
                                    new TeletravailBeCct85149Analysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public TeletravailBeCct85149Response get(
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

        TeletravailBeCct85149Analysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse télétravail BE CCT 85/149"
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

    private TeletravailBeCct85149Response deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    TeletravailBeCct85149Response.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private TeletravailBeCct85149Response toResponse(
            UUID caseFileId, TeletravailBeCct85149Result r) {
        return new TeletravailBeCct85149Response(
                caseFileId,
                r.dateDebutTeletravail(),
                r.typeTeletravail(),
                r.volontariatReciproque(),
                r.conventionEcriteIndividuelle(),
                r.equipementFourniOuIndemnise(),
                r.indemniteForfaitaireMensuelleEuros(),
                r.plafondIndemniteMensuelleEuros(),
                r.droitsSociauxMaintenus(),
                r.deconnexionDefinie(),
                r.effectifEntreprise(),
                r.verdict(),
                r.conventionRespectee(),
                r.equipementRespecte(),
                r.droitsRespectes(),
                r.deconnexionRespectee(),
                r.indemniteExcedentaire(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
