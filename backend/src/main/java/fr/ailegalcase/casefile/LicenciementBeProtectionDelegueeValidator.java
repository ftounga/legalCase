package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-213-08 : analyseur de la validité du licenciement d'un délégué syndical
 * / candidat non élu BE — fonction <b>pure</b> (paramétrable par un
 * {@link Clock} pour la testabilité du délai de réintégration).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 19 mars 1991</b> relative à la protection contre le
 *       licenciement des représentants des travailleurs dans les
 *       entreprises avec CPPT et CE :
 *     <ul>
 *       <li>Délégués élus + <b>candidats non élus</b> protégés pendant
 *           2 ans après les élections.</li>
 *       <li>Licenciement interdit sauf motif grave (art. 35 Loi 03/07/1978)
 *           ou raisons d'ordre économique/technique reconnues par la
 *           juridiction compétente.</li>
 *       <li>Procédure de réintégration : le délégué licencié peut demander
 *           la réintégration dans les <b>30 jours</b>.</li>
 *       <li>Si l'employeur refuse : <b>indemnité forfaitaire = 2 ans</b> de
 *           rémunération brute (délégué élu, ≥ 2 ans ancienneté).</li>
 *       <li><b>Récidive</b> ou circonstances aggravantes : 4 ans.</li>
 *     </ul>
 *   </li>
 *   <li><b>CCT n° 5 du 24 mai 1971</b> relative au statut des délégations
 *       syndicales : complète la loi de 1991 pour les délégués syndicaux
 *       non liés au CPPT/CE.</li>
 * </ul>
 *
 * <h2>Logique fenêtre de protection</h2>
 * <ul>
 *   <li>Élus (DELEGUE_SYNDICAL_ELU / CONSEILLER_CPPT) : durée du mandat
 *       4 ans (cycle élections sociales).</li>
 *   <li>Candidats non élus + CCT 5 (CANDIDAT_NON_ELU / DELEGUE_CCT5) :
 *       2 ans après les élections.</li>
 *   <li>{@code dateFinProtection = dateElectionOuMandat + dureeProtection}.</li>
 *   <li>{@code licenciementDansProtection = dateLicenciement <= dateFinProtection}.</li>
 * </ul>
 *
 * <h2>Calcul de l'indemnité forfaitaire</h2>
 * <p>{@code annees = circonstancesAggravantes ? 4 : 2}<br>
 * {@code indemniteForfaitaire = remunerationAnnuelleBrute × annees}</p>
 *
 * <h2>Délai de réintégration (art. 14 Loi 19/03/1991)</h2>
 * <ul>
 *   <li>{@code dateLimiteDemandeReintegration = dateLicenciement + 30 j}.</li>
 *   <li>{@code joursRestantsReintegration = today → dateLimite}.</li>
 *   <li>{@code delaiReintegrationDepasse = joursRestants <= 0}.</li>
 * </ul>
 */
public final class LicenciementBeProtectionDelegueeValidator {

    static final String BASE_JURIDIQUE =
            "Loi du 19/03/1991 (protection représentants CE/CPPT) ; "
                    + "CCT n° 5 du 24/05/1971 (statut délégations syndicales)";

    static final String AVERT_DELAI_DEPASSE =
            "Délai de 30 jours pour demander la réintégration expiré — "
                    + "recours direct au tribunal du travail (CJ art. 578)";

    /** Durée mandat élus CE/CPPT (cycle élections sociales). */
    private static final int DUREE_MANDAT_ELUS_ANNEES = 4;

    /** Durée protection candidats non élus + CCT n° 5. */
    private static final int DUREE_PROTECTION_NON_ELUS_ANNEES = 2;

    /** Délai légal pour demander la réintégration (art. 14 Loi 19/03/1991). */
    private static final int DELAI_REINTEGRATION_JOURS = 30;

    /** Barème indemnité forfaitaire — base 2 ans. */
    private static final int FORFAIT_BASE_ANNEES = 2;

    /** Barème indemnité forfaitaire — récidive / circonstances aggravantes. */
    private static final int FORFAIT_AGGRAVE_ANNEES = 4;

    private LicenciementBeProtectionDelegueeValidator() {
    }

    public static LicenciementBeProtectionDelegueeResult analyze(
            LicenciementBeProtectionDelegueeRequest request) {
        return analyze(request, Clock.systemDefaultZone());
    }

    /**
     * Variante testable injectant un {@link Clock} explicite — permet de
     * fixer "aujourd'hui" pour vérifier le calcul du délai de 30 jours
     * indépendamment de l'horloge système.
     */
    public static LicenciementBeProtectionDelegueeResult analyze(
            LicenciementBeProtectionDelegueeRequest request, Clock clock) {
        validateInputs(request);

        // 1. Fenêtre de protection : durée selon le statut
        int dureeProtection = dureeProtectionAnnees(request.statutProtege());
        LocalDate dateFinProtection = request.dateElectionOuMandat()
                .plusYears(dureeProtection);
        boolean licenciementDansProtection =
                !request.dateLicenciement().isAfter(dateFinProtection);

        // 2. Verdict
        LicenciementBeProtectionDelegueeVerdict verdict = licenciementDansProtection
                ? LicenciementBeProtectionDelegueeVerdict.LICENCIEMENT_INTERDIT_SANS_PROCEDURE
                : LicenciementBeProtectionDelegueeVerdict.HORS_PERIODE_PROTECTION;

        // 3. Indemnité forfaitaire — uniquement si dans la protection
        BigDecimal indemniteForfaitaire = null;
        Integer anneesForfait = null;
        if (licenciementDansProtection) {
            anneesForfait = request.circonstancesAggravantes()
                    ? FORFAIT_AGGRAVE_ANNEES : FORFAIT_BASE_ANNEES;
            indemniteForfaitaire = request.remunerationAnnuelleBrute()
                    .multiply(BigDecimal.valueOf(anneesForfait));
        }

        // 4. Délai de demande de réintégration (30 j depuis le licenciement)
        LocalDate dateLimiteDemandeReintegration = request.dateLicenciement()
                .plusDays(DELAI_REINTEGRATION_JOURS);
        LocalDate today = LocalDate.now(clock);
        long joursRestantsLong = ChronoUnit.DAYS.between(
                today, dateLimiteDemandeReintegration);
        Integer joursRestantsReintegration = (int) joursRestantsLong;
        boolean delaiReintegrationDepasse = joursRestantsReintegration <= 0;

        // 5. Avertissement (priorité : délai expiré)
        String avertissement = delaiReintegrationDepasse
                ? AVERT_DELAI_DEPASSE : null;

        return new LicenciementBeProtectionDelegueeResult(
                request.statutProtege(),
                request.ancienneteAnnees(),
                request.remunerationAnnuelleBrute(),
                request.dateLicenciement(),
                request.dateElectionOuMandat(),
                request.demandeReintegrationDansTrente(),
                request.employeurRefuseReintegration(),
                request.circonstancesAggravantes(),
                verdict,
                licenciementDansProtection,
                dateFinProtection,
                indemniteForfaitaire,
                anneesForfait,
                dateLimiteDemandeReintegration,
                joursRestantsReintegration,
                delaiReintegrationDepasse,
                BASE_JURIDIQUE,
                avertissement);
    }

    // ── Durée de protection par statut ─────────────────────────────────────

    private static int dureeProtectionAnnees(LicenciementBeStatutProtegeEnum statut) {
        return switch (statut) {
            case DELEGUE_SYNDICAL_ELU, CONSEILLER_CPPT -> DUREE_MANDAT_ELUS_ANNEES;
            case CANDIDAT_NON_ELU, DELEGUE_CCT5 -> DUREE_PROTECTION_NON_ELUS_ANNEES;
        };
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(LicenciementBeProtectionDelegueeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.statutProtege() == null) {
            throw new IllegalArgumentException("statutProtege est requis");
        }
        if (request.ancienneteAnnees() == null) {
            throw new IllegalArgumentException("ancienneteAnnees est requise");
        }
        if (request.ancienneteAnnees() < 0) {
            throw new IllegalArgumentException(
                    "ancienneteAnnees ne peut pas être négative");
        }
        if (request.remunerationAnnuelleBrute() == null) {
            throw new IllegalArgumentException("remunerationAnnuelleBrute est requise");
        }
        if (request.remunerationAnnuelleBrute().signum() <= 0) {
            throw new IllegalArgumentException(
                    "remunerationAnnuelleBrute doit être strictement positive");
        }
        if (request.dateLicenciement() == null) {
            throw new IllegalArgumentException("dateLicenciement est requise");
        }
        if (request.dateElectionOuMandat() == null) {
            throw new IllegalArgumentException("dateElectionOuMandat est requise");
        }
        // Cohérence procédure réintégration : refus employeur ⇒ demande
        // préalable (sinon état logique invalide — Loi 19/03/1991 art. 14)
        if (request.employeurRefuseReintegration()
                && !request.demandeReintegrationDansTrente()) {
            throw new IllegalArgumentException(
                    "employeurRefuseReintegration=true incohérent sans "
                            + "demandeReintegrationDansTrente=true");
        }
    }
}
