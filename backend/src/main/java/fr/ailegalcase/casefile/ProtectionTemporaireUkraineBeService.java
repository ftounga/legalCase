package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-215-19 : service applicatif de l'outil "Protection temporaire Ukraine BE"
 * (F-IM-34). Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>dateArrivee non antérieure au 24/02/2022 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 *
 * <p>La date de fin de la protection temporaire est paramétrable via
 * {@code protection.temporaire.ukraine.date-fin} (défaut {@code 2027-03-04} —
 * prochaine échéance annuelle connue de la prolongation de la directive 2001/55/CE).
 * <b>À mettre à jour annuellement</b> au gré des prolongations successives.
 */
@Service
public class ProtectionTemporaireUkraineBeService {

    private final ProtectionTemporaireUkraineBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;
    private final LocalDate dateFinProtection;

    public ProtectionTemporaireUkraineBeService(
            ProtectionTemporaireUkraineBeRepository repository,
            CaseFileRepository caseFileRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            CurrentUserResolver currentUserResolver,
            ObjectMapper objectMapper,
            @Value("${protection.temporaire.ukraine.date-fin:2027-03-04}") LocalDate dateFinProtection) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
        this.dateFinProtection = dateFinProtection;
    }

    @Transactional
    public ProtectionTemporaireUkraineBeResponse calculate(UUID caseFileId,
                                                           ProtectionTemporaireUkraineBeRequest request,
                                                           OidcUser oidcUser,
                                                           Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Protection temporaire Ukraine — outil BELGIQUE uniquement "
                            + "(directive 2001/55/CE, Loi 15/12/1980 art. 57/29).");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        ProtectionTemporaireUkraineBeResult result;
        try {
            result = ProtectionTemporaireUkraineBeCalculator.compute(
                    request.dateArrivee(),
                    request.nationaliteUkrainienne(),
                    request.residenceUkraineAvant24Fev2022(),
                    request.apatridesUkraine(),
                    request.membreFamilleProtege(),
                    request.titreSejourBE(),
                    LocalDate.now(),
                    dateFinProtection);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ProtectionTemporaireUkraineBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ProtectionTemporaireUkraineBeAnalysis a = new ProtectionTemporaireUkraineBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateArrivee(result.dateArrivee());
        entity.setNationaliteUkrainienne(result.nationaliteUkrainienne());
        entity.setResidenceUkraineAvant24Fev2022(result.residenceUkraineAvant24Fev2022());
        entity.setApatridesUkraine(result.apatridesUkraine());
        entity.setMembreFamilleProtege(result.membreFamilleProtege());
        entity.setTitreSejourBE(result.titreSejourBE());
        entity.setEligible(result.eligible());
        entity.setDateFinProtection(result.dateFinProtection());
        entity.setDureeProtectionRestante(result.dureeProtectionRestante());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public ProtectionTemporaireUkraineBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ProtectionTemporaireUkraineBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Protection temporaire Ukraine BE trouvée pour ce dossier"));
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private ProtectionTemporaireUkraineBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ProtectionTemporaireUkraineBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ProtectionTemporaireUkraineBeResponse toResponse(UUID caseFileId,
                                                            ProtectionTemporaireUkraineBeResult r) {
        return new ProtectionTemporaireUkraineBeResponse(
                caseFileId,
                r.dateArrivee(),
                r.nationaliteUkrainienne(),
                r.residenceUkraineAvant24Fev2022(),
                r.apatridesUkraine(),
                r.membreFamilleProtege(),
                r.titreSejourBE(),
                r.eligible(),
                r.dateFinProtection(),
                r.dureeProtectionRestante(),
                r.prochainRenouvellement(),
                r.droitsTravail(),
                r.droitsAides(),
                r.cheminProcedure(),
                r.recommandation(),
                r.baseJuridique()
        );
    }
}
