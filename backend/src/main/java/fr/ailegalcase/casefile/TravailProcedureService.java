package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.referential.LegalReferentialService;
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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SF-136-02 : service de résolution des jalons procéduraux travail
 * (FR + BE) — wrapper avec validations de surface au-dessus de
 * {@link LegalReferentialService#getTravailProcedureJalons(String, String)}.
 *
 * <p>Stateless — pas de persistance. La SF-136-01 a seedé les 6 procédures
 * dans {@code legal_referentials}, ce service ne fait que valider les
 * paramètres entrants, dériver le pays du suffixe du code et retourner la
 * liste de jalons (avec date cible calculée si {@code dateDeclencheur}
 * fournie).
 */
@Service
public class TravailProcedureService {

    /** Codes valides — alignés sur {@link TravailProcedureReferentiel}. */
    static final Set<String> CODES_FR = Set.of(
            TravailProcedureReferentiel.PRUDHOMMES_FR,
            TravailProcedureReferentiel.APPEL_CA_SOCIALE_FR,
            TravailProcedureReferentiel.CASSATION_SOCIALE_FR
    );

    static final Set<String> CODES_BE = Set.of(
            TravailProcedureReferentiel.TRIBUNAL_TRAVAIL_BE,
            TravailProcedureReferentiel.COUR_TRAVAIL_BE,
            TravailProcedureReferentiel.CASSATION_BE
    );

    private final LegalReferentialService legalReferentialService;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public TravailProcedureService(LegalReferentialService legalReferentialService,
                                   CaseFileRepository caseFileRepository,
                                   WorkspaceMemberRepository workspaceMemberRepository,
                                   CurrentUserResolver currentUserResolver) {
        this.legalReferentialService = legalReferentialService;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public List<TravailProcedureJalonResponse> getJalons(UUID caseFileId,
                                                         String typeProcedure,
                                                         String dateDeclencheurIso,
                                                         OidcUser oidcUser,
                                                         Principal principal) {
        // 1. Auth + isolation workspace.
        User user = currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");

        // 2. Validation typeProcedure parmi les 6 codes.
        if (typeProcedure == null || typeProcedure.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "typeProcedure est requis");
        }
        String code = typeProcedure.trim().toUpperCase();
        String country;
        if (CODES_FR.contains(code)) {
            country = TravailProcedureReferentiel.COUNTRY_FR;
        } else if (CODES_BE.contains(code)) {
            country = TravailProcedureReferentiel.COUNTRY_BE;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "typeProcedure invalide. Valeurs autorisées : " +
                    "PRUDHOMMES_FR, APPEL_CA_SOCIALE_FR, CASSATION_SOCIALE_FR, " +
                    "TRIBUNAL_TRAVAIL_BE, COUR_TRAVAIL_BE, CASSATION_BE");
        }

        // 3. Validation dateDeclencheur (optionnel).
        LocalDate dateDeclencheur = null;
        if (dateDeclencheurIso != null && !dateDeclencheurIso.isBlank()) {
            try {
                dateDeclencheur = LocalDate.parse(dateDeclencheurIso.trim());
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "dateDeclencheur doit être au format ISO YYYY-MM-DD");
            }
        }

        // 4. Lecture du référentiel (DB-first, fallback Java).
        List<TravailProcedureReferentiel.ProcedureJalon> jalons =
                legalReferentialService.getTravailProcedureJalons(code, country);

        // 5. Mapping vers DTO + calcul dateCible si applicable.
        final LocalDate pivot = dateDeclencheur;
        return jalons.stream()
                .map(j -> new TravailProcedureJalonResponse(
                        code,
                        j.label(),
                        j.offsetDays(),
                        j.articleRef(),
                        pivot != null ? pivot.plusDays(j.offsetDays()) : null))
                .toList();
    }
}
