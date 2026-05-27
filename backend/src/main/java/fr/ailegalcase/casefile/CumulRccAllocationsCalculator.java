package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * SF-219-04 : analyseur du cumul RCC BE + allocations chômage / pension
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>CCT n° 17 du 19/12/1974</b> (Conseil National du Travail) —
 *       instaure l'indemnité complémentaire à charge de l'employeur.
 *       Plafond du cumul : le total {@code allocation chômage +
 *       indemnité complémentaire} ne peut pas dépasser la dernière
 *       rémunération nette de référence.</li>
 *   <li><b>AR du 25/11/1991</b> portant réglementation du chômage —
 *       conditions de cumul, compatibilité avec activités accessoires
 *       (indépendant accessoire, bénévolat), obligation de déclaration
 *       préalable à l'ONEM.</li>
 *   <li><b>AR du 03/05/2007</b> art. 22 et suivants — régimes de
 *       disponibilité ajustée selon l'âge à la prise de cours du RCC.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>BASCULE_PENSION_LEGALE</b> — si l'âge légal est atteint à la
 *       date de référence, le RCC s'éteint automatiquement (prime sur
 *       toutes les autres règles).</li>
 *   <li><b>INCOMPATIBLE_ACTIVITE_NON_AUTORISEE</b> — toute reprise non
 *       déclarée fait sortir du régime (prime sur le plafond).</li>
 *   <li><b>CUMUL_PLAFOND_DEPASSE</b> — total &gt; rémunération nette de
 *       référence : indemnité complémentaire à réduire de la différence.</li>
 *   <li><b>CUMUL_CONFORME</b> — toutes conditions OK.</li>
 * </ol>
 *
 * <h2>Régime de disponibilité (informationnel, ne change pas le verdict)</h2>
 * <ul>
 *   <li>Âge &lt; 62 ans, carrière &lt; 42 ans → DISPONIBILITE_ADAPTEE.</li>
 *   <li>Âge &ge; 62 ans OU carrière &ge; 42 ans → DISPONIBILITE_PASSIVE.</li>
 *   <li>Très proche de la pension légale (≤ 1 an) → DISPENSE_RECHERCHE_EMPLOI.</li>
 * </ul>
 *
 * <h2>Bascule pension</h2>
 * <p>La date de référence pour le calcul de l'âge est la <b>date de
 * début du RCC</b>. L'âge légal par défaut est <b>65 ans</b>
 * (configurable via {@code ageLegalPension} dans la requête pour les
 * scénarios prospectifs 66/67 ans).</p>
 */
public final class CumulRccAllocationsCalculator {

    static final String BASE_JURIDIQUE =
            "CCT n° 17 du 19/12/1974 (CNT — indemnité complémentaire + "
                    + "plafond du cumul) ; AR du 25/11/1991 (chômage — "
                    + "compatibilité activités accessoires + déclaration "
                    + "ONEM) ; AR du 03/05/2007 art. 22 et suiv. (régimes "
                    + "de disponibilité ajustée selon l'âge)";

    static final String AVERT_MONTANTS_INDICATIFS =
            "Le calcul du plafond et de la réduction est indicatif — "
                    + "montants définitifs à confirmer par les services "
                    + "ONSS / ONEM compte tenu du précompte professionnel, "
                    + "des cotisations spéciales et du barème en vigueur.";

    /** Âge légal par défaut de la pension (Belgique — 2026). */
    static final int AGE_LEGAL_PENSION_DEFAUT = 65;

    /** Seuil d'âge pour la disponibilité passive (AR 03/05/2007 art. 22). */
    private static final int AGE_DISPONIBILITE_PASSIVE = 62;

    /** Seuil de carrière pour la disponibilité passive longue carrière. */
    private static final int CARRIERE_DISPONIBILITE_PASSIVE = 42;

    private CumulRccAllocationsCalculator() {
    }

    public static CumulRccAllocationsResult analyze(
            CumulRccAllocationsRequest request) {
        validateInputs(request);

        int ageDebut = ageAt(request.dateNaissance(), request.dateDebutRcc());
        int ageLegal = request.ageLegalPension() != null
                ? request.ageLegalPension()
                : AGE_LEGAL_PENSION_DEFAUT;

        BigDecimal cumul = request.allocationChomageMensuelle()
                .add(request.indemniteComplementaireMensuelle())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal plafond = request.remunerationNetteReference()
                .setScale(2, RoundingMode.HALF_UP);
        boolean plafondOk = cumul.compareTo(plafond) <= 0;
        BigDecimal reduction = plafondOk
                ? null
                : cumul.subtract(plafond).setScale(2, RoundingMode.HALF_UP);

        boolean pensionAtteinte = ageDebut >= ageLegal;

        CumulRccAllocationsDisponibiliteRegime regime =
                determinerRegimeDisponibilite(ageDebut, request.anneesCarriereTotale(),
                        ageLegal);

        // Hiérarchie : pension > activité non autorisée > plafond > conforme.
        CumulRccAllocationsVerdict verdict;
        if (pensionAtteinte) {
            verdict = CumulRccAllocationsVerdict.BASCULE_PENSION_LEGALE;
        } else if (request.activiteComplementaire()
                == CumulRccAllocationsActiviteAutorisee.ACTIVITE_NON_DECLAREE) {
            verdict = CumulRccAllocationsVerdict.INCOMPATIBLE_ACTIVITE_NON_AUTORISEE;
        } else if (!plafondOk) {
            verdict = CumulRccAllocationsVerdict.CUMUL_PLAFOND_DEPASSE;
        } else {
            verdict = CumulRccAllocationsVerdict.CUMUL_CONFORME;
        }
        boolean conforme = verdict == CumulRccAllocationsVerdict.CUMUL_CONFORME;

        String synthese = buildSynthese(verdict, plafond, cumul, reduction,
                regime, ageDebut, ageLegal);

        return new CumulRccAllocationsResult(
                request.dateNaissance(),
                request.dateDebutRcc(),
                request.allocationChomageMensuelle(),
                request.indemniteComplementaireMensuelle(),
                request.remunerationNetteReference(),
                request.anneesCarriereTotale(),
                request.activiteComplementaire(),
                request.ageLegalPension(),
                verdict,
                conforme,
                ageDebut,
                cumul,
                plafondOk,
                reduction,
                regime,
                pensionAtteinte,
                synthese,
                BASE_JURIDIQUE,
                AVERT_MONTANTS_INDICATIFS);
    }

    // ── Régime de disponibilité ─────────────────────────────────────────────

    private static CumulRccAllocationsDisponibiliteRegime determinerRegimeDisponibilite(
            int age, int carriere, int ageLegal) {
        // Dispense totale si très proche de la pension légale (≤ 1 an).
        if (age >= ageLegal - 1) {
            return CumulRccAllocationsDisponibiliteRegime.DISPENSE_RECHERCHE_EMPLOI;
        }
        if (age >= AGE_DISPONIBILITE_PASSIVE
                || carriere >= CARRIERE_DISPONIBILITE_PASSIVE) {
            return CumulRccAllocationsDisponibiliteRegime.DISPONIBILITE_PASSIVE;
        }
        return CumulRccAllocationsDisponibiliteRegime.DISPONIBILITE_ADAPTEE;
    }

    // ── Synthèse en langage avocat ──────────────────────────────────────────

    private static String buildSynthese(
            CumulRccAllocationsVerdict verdict,
            BigDecimal plafond,
            BigDecimal cumul,
            BigDecimal reduction,
            CumulRccAllocationsDisponibiliteRegime regime,
            int age,
            int ageLegal) {
        String suffixDispo = " Régime de disponibilité : " + libelleRegime(regime) + ".";
        return switch (verdict) {
            case CUMUL_CONFORME -> "Cumul conforme — total mensuel " + format(cumul)
                    + " € ≤ rémunération nette de référence " + format(plafond) + " €"
                    + " (plafond CCT n° 17 respecté)." + suffixDispo;
            case CUMUL_PLAFOND_DEPASSE -> "Cumul au-delà du plafond — total " + format(cumul)
                    + " € > référence " + format(plafond) + " €. Réduire l'indemnité "
                    + "complémentaire de " + format(reduction) + " € (CCT n° 17 :"
                    + " l'employeur ne peut maintenir un revenu net supérieur à la"
                    + " dernière rémunération nette)." + suffixDispo;
            case BASCULE_PENSION_LEGALE -> "Âge légal pension atteint (" + age
                    + " ans ≥ " + ageLegal + " ans) — le RCC s'éteint à compter de la"
                    + " date de prise de cours de la pension légale. Le cumul des"
                    + " allocations ONEM + indemnité complémentaire n'a plus d'objet.";
            case INCOMPATIBLE_ACTIVITE_NON_AUTORISEE -> "Activité non déclarée à l'ONEM"
                    + " — sortie automatique du régime, restitution des allocations"
                    + " indûment perçues (AR 25/11/1991). Régulariser sans délai"
                    + " auprès de l'ONEM.";
        };
    }

    private static String libelleRegime(CumulRccAllocationsDisponibiliteRegime r) {
        return switch (r) {
            case DISPONIBILITE_ADAPTEE -> "disponibilité adaptée (recherche active allégée)";
            case DISPONIBILITE_PASSIVE -> "disponibilité passive (dispense recherche, "
                    + "inscription maintenue)";
            case DISPENSE_RECHERCHE_EMPLOI -> "dispense totale de recherche d'emploi"
                    + " (proche de la pension légale)";
        };
    }

    private static String format(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static int ageAt(LocalDate naissance, LocalDate reference) {
        return Period.between(naissance, reference).getYears();
    }

    // ── Validation conditionnelle ───────────────────────────────────────────

    private static void validateInputs(CumulRccAllocationsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateNaissance() == null) {
            throw new IllegalArgumentException("dateNaissance est requise");
        }
        if (request.dateDebutRcc() == null) {
            throw new IllegalArgumentException("dateDebutRcc est requise");
        }
        if (request.dateDebutRcc().isBefore(request.dateNaissance())) {
            throw new IllegalArgumentException(
                    "dateDebutRcc ne peut pas précéder dateNaissance");
        }
        if (request.allocationChomageMensuelle() == null
                || request.allocationChomageMensuelle().signum() < 0) {
            throw new IllegalArgumentException(
                    "allocationChomageMensuelle requise et ≥ 0");
        }
        if (request.indemniteComplementaireMensuelle() == null
                || request.indemniteComplementaireMensuelle().signum() < 0) {
            throw new IllegalArgumentException(
                    "indemniteComplementaireMensuelle requise et ≥ 0");
        }
        if (request.remunerationNetteReference() == null
                || request.remunerationNetteReference().signum() < 0) {
            throw new IllegalArgumentException(
                    "remunerationNetteReference requise et ≥ 0");
        }
        if (request.anneesCarriereTotale() == null
                || request.anneesCarriereTotale() < 0) {
            throw new IllegalArgumentException(
                    "anneesCarriereTotale requise et ≥ 0");
        }
        if (request.activiteComplementaire() == null) {
            throw new IllegalArgumentException(
                    "activiteComplementaire est requise");
        }
        if (request.ageLegalPension() != null
                && (request.ageLegalPension() < 60
                || request.ageLegalPension() > 70)) {
            throw new IllegalArgumentException(
                    "ageLegalPension doit être entre 60 et 70 ans");
        }
    }
}
