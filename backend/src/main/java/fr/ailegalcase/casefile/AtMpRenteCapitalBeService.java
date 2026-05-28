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
 * SF-219-29 : service orchestrant l'analyse <i>Rente AT/MP vs
 * capitalisation BE</i> (Loi du 10/04/1971 art. 24 ; Lois coordonnees
 * du 03/06/1970 art. 35 ; AR du 21/12/1971 + AR 24/02/2005 - bareme).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 cote FR - reponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/.../28) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>La France a un regime distinct (rente AT/MP CPAM art. L. 434-1 et
 * s. CSS, seuils 10% / 50% au lieu de 19%, bareme indicatif art. R.
 * 434-2 different, voies de recours TASS / Pole social TJ).
 * Restitution separee par outil - pas d'harmonisation FR/BE.</p>
 *
 * <p>Le checker sous-jacent ({@link AtMpRenteCapitalBeChecker}) est
 * une fonction pure independante du contexte HTTP / persistance.</p>
 */
@Service
public class AtMpRenteCapitalBeService {

    private final AtMpRenteCapitalBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AtMpRenteCapitalBeService(
            AtMpRenteCapitalBeAnalysisRepository repository,
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
    public AtMpRenteCapitalBeResponse analyze(
            UUID caseFileId,
            AtMpRenteCapitalBeRequest request,
            OidcUser oidcUser,
            Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requete requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer
        // l'existence de l'outil cote FR - pattern miroir vagues F-219.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        AtMpRenteCapitalBeResult result;
        try {
            result = AtMpRenteCapitalBeChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        AtMpRenteCapitalBeResponse response = toResponse(caseFileId, result);

        AtMpRenteCapitalBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            AtMpRenteCapitalBeAnalysis a =
                                    new AtMpRenteCapitalBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public AtMpRenteCapitalBeResponse get(
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

        AtMpRenteCapitalBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse de rente / capital AT/MP"
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
                    "Erreur de serialisation");
        }
    }

    private AtMpRenteCapitalBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    AtMpRenteCapitalBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de deserialisation");
        }
    }

    private AtMpRenteCapitalBeResponse toResponse(
            UUID caseFileId, AtMpRenteCapitalBeResult r) {
        return new AtMpRenteCapitalBeResponse(
                caseFileId,
                r.origine(),
                r.statutReconnaissance(),
                r.dateConsolidation(),
                r.tauxIpp(),
                r.remunerationBaseAnnuelle(),
                r.dateNaissance(),
                r.demandeConversionPartielle(),
                r.dateDemandeConversion(),
                r.ageConsolidationOverride(),
                r.verdict(),
                r.capitalisationDoffice(),
                r.conversionPartiellePossible(),
                r.ageConsolidationCalcule(),
                r.coefficientAge(),
                r.renteAnnuelle(),
                r.capitalCapitalisation(),
                r.capitalConversionPartielle(),
                r.renteResiduelleApresConversion(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
