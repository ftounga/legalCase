package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-39 : analyseur de l'<b>exonération de la prime de partage de la valeur
 * (PPV)</b> (loi n° 2022-1158 du 16/08/2022 art. 1 — création de la PPV ; loi
 * n° 2023-1107 du 29/11/2023 sur le partage de la valeur, F-DT-52). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ; distinct de
 * F-DT-53 intéressement / participation) :
 * <ul>
 *   <li><b>Plafond social</b> — {@code 6000 €} si {@code accordInteressementPresent}
 *       OU {@code effectifMoins50} (entreprises de moins de 50 salariés dotées
 *       d'un dispositif de partage de la valeur) ; sinon {@code 3000 €}.</li>
 *   <li><b>Test plafond</b> — si {@code montantPrime > plafondSocial} →
 *       {@code montantExonere = plafondSocial}, {@code montantImposable =
 *       montantPrime − plafondSocial}, {@code statut = PLAFOND_DEPASSE}. Sinon
 *       {@code montantExonere = montantPrime}, {@code montantImposable = 0},
 *       {@code statut = CONFORME}.</li>
 *   <li><b>Exonération fiscale (IR)</b> — {@code true} si {@code effectifMoins50}
 *       ET {@code remunerationAnnuelleBrute < 3 × SMIC annuel} (jusqu'au
 *       31/12/2026) ; sinon {@code false} (la part exonérée socialement reste
 *       imposable à l'IR sauf affectation à un plan d'épargne).</li>
 * </ul>
 *
 * <p>Base juridique annotée « à vérifier par avocat ».
 */
public final class PpvExonerationAnalyzer {

    /** Plafond d'exonération sociale de droit commun (par bénéficiaire / année). */
    static final BigDecimal PLAFOND_SOCIAL_BASE = new BigDecimal("3000");

    /**
     * Plafond d'exonération sociale majoré (accord d'intéressement, ou
     * entreprise de moins de 50 salariés dotée d'un dispositif de partage de la
     * valeur).
     */
    static final BigDecimal PLAFOND_SOCIAL_MAJORE = new BigDecimal("6000");

    /**
     * SMIC annuel brut de référence pour le test « rémunération &lt; 3 SMIC » de
     * l'exonération fiscale IR. Valeur paramétrée (SMIC mensuel brut 1 801,80 €
     * × 12 = 21 621,60 €, base 35 h) — le calcul exact du SMIC de référence est
     * hors périmètre de l'outil (cf. mini-spec).
     */
    static final BigDecimal SMIC_ANNUEL_BRUT = new BigDecimal("21621.60");

    /** Seuil de rémunération ouvrant l'exonération fiscale IR : 3 SMIC annuels. */
    static final BigDecimal SEUIL_3_SMIC = SMIC_ANNUEL_BRUT.multiply(BigDecimal.valueOf(3));

    static final String BASE_JURIDIQUE =
            "loi n° 2022-1158 du 16/08/2022, art. 1 — création de la prime de "
                    + "partage de la valeur (PPV) : exonération de cotisations sociales "
                    + "dans la limite de 3 000 € par bénéficiaire et par année civile, "
                    + "portée à 6 000 € en présence d'un accord d'intéressement (ou pour "
                    + "les entreprises de moins de 50 salariés dotées d'un dispositif de "
                    + "partage de la valeur) ; loi n° 2023-1107 du 29/11/2023 sur le "
                    + "partage de la valeur — pérennisation, possibilité de deux primes "
                    + "par an, affectation à un plan d'épargne salariale, exonération "
                    + "d'impôt sur le revenu maintenue jusqu'au 31/12/2026 pour les "
                    + "salariés des entreprises de moins de 50 salariés dont la "
                    + "rémunération est inférieure à 3 SMIC (à vérifier par avocat)";

    private PpvExonerationAnalyzer() {
    }

    /**
     * Analyse la conformité de la PPV au plafond d'exonération sociale et
     * détermine la part exonérée, la part imposable et l'exonération fiscale IR.
     */
    public static PpvExonerationResult analyze(
            BigDecimal montantPrime,
            Boolean accordInteressementPresent,
            BigDecimal remunerationAnnuelleBrute,
            Boolean effectifMoins50,
            Boolean versementPlanEpargne) {

        validate(montantPrime, accordInteressementPresent, remunerationAnnuelleBrute,
                effectifMoins50);

        BigDecimal prime = montantPrime.setScale(2, RoundingMode.HALF_UP);
        boolean accordInteressement = accordInteressementPresent;
        BigDecimal remuneration = remunerationAnnuelleBrute.setScale(2, RoundingMode.HALF_UP);
        boolean moins50 = effectifMoins50;
        boolean planEpargne = versementPlanEpargne != null && versementPlanEpargne;

        List<String> notes = new ArrayList<>();

        // ── Plafond social applicable ───────────────────────────────────────
        BigDecimal plafondSocial;
        if (accordInteressement || moins50) {
            plafondSocial = PLAFOND_SOCIAL_MAJORE;
            if (accordInteressement) {
                notes.add("Plafond d'exonération sociale porté à 6 000 € : un accord "
                        + "d'intéressement est en vigueur dans l'entreprise (loi "
                        + "n° 2022-1158, art. 1).");
            } else {
                notes.add("Plafond d'exonération sociale porté à 6 000 € : entreprise "
                        + "de moins de 50 salariés dotée d'un dispositif de partage de "
                        + "la valeur (à vérifier par avocat).");
            }
        } else {
            plafondSocial = PLAFOND_SOCIAL_BASE;
            notes.add("Plafond d'exonération sociale de droit commun de 3 000 € par "
                    + "bénéficiaire et par année civile (loi n° 2022-1158, art. 1).");
        }

        // ── Test du plafond ─────────────────────────────────────────────────
        BigDecimal montantExonere;
        BigDecimal montantImposable;
        PpvExonerationStatut statut;
        if (prime.compareTo(plafondSocial) > 0) {
            montantExonere = plafondSocial;
            montantImposable = prime.subtract(plafondSocial).setScale(2, RoundingMode.HALF_UP);
            statut = PpvExonerationStatut.PLAFOND_DEPASSE;
            notes.add("Montant de la PPV (" + prime.toPlainString() + " €) supérieur au "
                    + "plafond d'exonération sociale (" + plafondSocial.toPlainString()
                    + " €) : la fraction excédentaire (" + montantImposable.toPlainString()
                    + " €) est réintégrée dans l'assiette des cotisations et de l'impôt "
                    + "(à vérifier par avocat).");
        } else {
            montantExonere = prime;
            montantImposable = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            statut = PpvExonerationStatut.CONFORME;
            notes.add("Montant de la PPV intégralement exonéré de cotisations sociales "
                    + "dans la limite du plafond applicable.");
        }

        // ── Exonération fiscale (IR) ────────────────────────────────────────
        boolean exonerationFiscaleIr = moins50 && remuneration.compareTo(SEUIL_3_SMIC) < 0;
        if (exonerationFiscaleIr) {
            notes.add("Exonération d'impôt sur le revenu dans la limite du plafond "
                    + "social applicable : entreprise de moins de 50 salariés et "
                    + "rémunération annuelle brute inférieure à 3 SMIC (jusqu'au "
                    + "31/12/2026, loi n° 2023-1107) — à vérifier par avocat.");
        } else {
            notes.add("Pas d'exonération d'impôt sur le revenu de la part exonérée "
                    + "socialement : les conditions (effectif < 50 et rémunération "
                    + "< 3 SMIC, jusqu'au 31/12/2026) ne sont pas réunies ; la part "
                    + "exonérée de cotisations reste imposable à l'IR sauf affectation "
                    + "à un plan d'épargne salariale.");
        }

        // ── Note plan d'épargne ─────────────────────────────────────────────
        if (planEpargne) {
            notes.add("La fraction affectée à un plan d'épargne salariale bénéficie "
                    + "d'une exonération d'impôt sur le revenu (à vérifier par avocat).");
        }

        return new PpvExonerationResult(
                prime,
                accordInteressement,
                remuneration,
                moins50,
                planEpargne,
                plafondSocial,
                montantExonere,
                montantImposable,
                exonerationFiscaleIr,
                statut,
                List.copyOf(notes),
                BASE_JURIDIQUE);
    }

    private static void validate(BigDecimal montantPrime,
                                 Boolean accordInteressementPresent,
                                 BigDecimal remunerationAnnuelleBrute,
                                 Boolean effectifMoins50) {
        if (montantPrime == null) {
            throw new IllegalArgumentException("montantPrime est requis");
        }
        if (montantPrime.signum() <= 0) {
            throw new IllegalArgumentException("montantPrime doit être strictement positif");
        }
        if (accordInteressementPresent == null) {
            throw new IllegalArgumentException("accordInteressementPresent est requis");
        }
        if (remunerationAnnuelleBrute == null) {
            throw new IllegalArgumentException("remunerationAnnuelleBrute est requis");
        }
        if (remunerationAnnuelleBrute.signum() <= 0) {
            throw new IllegalArgumentException(
                    "remunerationAnnuelleBrute doit être strictement positif");
        }
        if (effectifMoins50 == null) {
            throw new IllegalArgumentException("effectifMoins50 est requis");
        }
    }
}
