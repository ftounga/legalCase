package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-37 : analyseur de la <b>monétisation de jours de RTT</b> (rachat de jours
 * de RTT — loi n° 2022-1157 du 16/08/2022 (LFR 2022) art. 5, dispositif prolongé
 * jusqu'au 31/12/2026, F-DT-51). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ; distinct de
 * F-DT-19 heures supplémentaires et de F-DT-80 acquisition de JRTT) :
 * <ul>
 *   <li><b>Applicabilité</b> — sur demande du salarié et avec accord de
 *       l'employeur, les jours/demi-journées de RTT acquis entre le 01/01/2022 et
 *       le 31/12/2026 peuvent être renoncés contre rémunération majorée. Si
 *       {@code joursAcquisDansFenetre = false} → {@code NON_ELIGIBLE}, aucun
 *       montant calculé.</li>
 *   <li><b>Borne du taux</b> — {@code tauxMajorationConventionnel} ramené dans
 *       [10, 25] : un taux &lt; 10 est porté à 10 ; un taux &gt; 25 est plafonné à
 *       25 (régime des heures supplémentaires).</li>
 *   <li><b>Montant</b> — {@code montantBrut = nombreJoursRttRenonces ×
 *       salaireJournalierBrut × (1 + tauxApplique / 100)} (2 décimales).</li>
 *   <li><b>Régime</b> — aligné sur celui des heures supplémentaires (exonération
 *       de cotisations salariales et d'impôt sur le revenu dans le plafond
 *       applicable).</li>
 * </ul>
 *
 * <p>Base juridique annotée « à vérifier par avocat ».
 */
public final class RttMonetisationAnalyzer {

    /** Taux de majoration par défaut (régime des heures supplémentaires). */
    static final double TAUX_DEFAUT = 25d;

    /** Borne basse du taux de majoration (taux de la 1re heure supplémentaire). */
    static final double TAUX_MIN = 10d;

    /** Borne haute du taux de majoration (plafond du régime des heures sup). */
    static final double TAUX_MAX = 25d;

    static final String REGIME_SOCIAL_FISCAL = "ALIGNE_HEURES_SUPPLEMENTAIRES";

    static final String BASE_JURIDIQUE =
            "loi n° 2022-1157 du 16/08/2022 de finances rectificative pour 2022, "
                    + "art. 5 — monétisation des jours de RTT : renonciation à des "
                    + "jours ou demi-journées de RTT acquis entre le 01/01/2022 et le "
                    + "31/12/2026, sur demande du salarié et avec accord de l'employeur, "
                    + "contre rémunération majorée (taux au moins égal au taux de la "
                    + "première heure supplémentaire, 10–25 %) ; régime social et fiscal "
                    + "aligné sur celui des heures supplémentaires (exonération de "
                    + "cotisations salariales et d'impôt sur le revenu dans le plafond "
                    + "applicable) ; dispositif prolongé jusqu'au 31/12/2026 "
                    + "(à vérifier par avocat)";

    private RttMonetisationAnalyzer() {
    }

    /**
     * Analyse l'éligibilité et, le cas échéant, le montant brut majoré de la
     * monétisation de jours de RTT.
     */
    public static RttMonetisationResult analyze(
            Integer nombreJoursRttRenonces,
            BigDecimal salaireJournalierBrut,
            Double tauxMajorationConventionnel,
            Boolean joursAcquisDansFenetre) {

        validate(nombreJoursRttRenonces, salaireJournalierBrut, joursAcquisDansFenetre);

        int jours = nombreJoursRttRenonces;
        BigDecimal salaire = salaireJournalierBrut;
        boolean dansFenetre = joursAcquisDansFenetre;

        // ── Borne du taux dans [10, 25] ─────────────────────────────────────
        List<String> notes = new ArrayList<>();
        double tauxDemande = tauxMajorationConventionnel != null
                ? tauxMajorationConventionnel
                : TAUX_DEFAUT;
        double tauxApplique = tauxDemande;
        if (tauxDemande < TAUX_MIN) {
            tauxApplique = TAUX_MIN;
            notes.add("Taux de majoration relevé au minimum de 10 % : le taux de "
                    + "monétisation ne peut être inférieur au taux de majoration de la "
                    + "première heure supplémentaire applicable dans l'entreprise "
                    + "(art. 5 LFR 2022).");
        } else if (tauxDemande > TAUX_MAX) {
            tauxApplique = TAUX_MAX;
            notes.add("Majoration plafonnée au régime des heures supplémentaires "
                    + "(25 %) : le bénéfice du régime social et fiscal de faveur ne "
                    + "s'applique que dans cette limite (art. 5 LFR 2022).");
        }

        // ── Applicabilité : hors fenêtre → NON_ELIGIBLE ─────────────────────
        if (!dansFenetre) {
            notes.add("Jours hors de la fenêtre du dispositif de monétisation "
                    + "(01/01/2022 → 31/12/2026, loi LFR 2022) : la renonciation à des "
                    + "jours de RTT contre rémunération majorée n'est pas ouverte pour "
                    + "ces jours.");
            return new RttMonetisationResult(
                    jours,
                    salaire,
                    tauxApplique,
                    false,
                    null,
                    REGIME_SOCIAL_FISCAL,
                    RttMonetisationStatut.NON_ELIGIBLE,
                    List.copyOf(notes),
                    BASE_JURIDIQUE);
        }

        // ── Montant brut majoré ─────────────────────────────────────────────
        BigDecimal facteurMajoration = BigDecimal.ONE.add(
                BigDecimal.valueOf(tauxApplique).movePointLeft(2));
        BigDecimal montantBrut = salaire
                .multiply(BigDecimal.valueOf(jours))
                .multiply(facteurMajoration)
                .setScale(2, RoundingMode.HALF_UP);

        notes.add("Régime social et fiscal aligné sur celui des heures "
                + "supplémentaires : exonération de cotisations salariales et d'impôt "
                + "sur le revenu dans le plafond applicable aux heures supplémentaires "
                + "(à vérifier par avocat).");

        return new RttMonetisationResult(
                jours,
                salaire,
                tauxApplique,
                true,
                montantBrut,
                REGIME_SOCIAL_FISCAL,
                RttMonetisationStatut.ELIGIBLE,
                List.copyOf(notes),
                BASE_JURIDIQUE);
    }

    private static void validate(Integer nombreJoursRttRenonces,
                                 BigDecimal salaireJournalierBrut,
                                 Boolean joursAcquisDansFenetre) {
        if (nombreJoursRttRenonces == null) {
            throw new IllegalArgumentException("nombreJoursRttRenonces est requis");
        }
        if (nombreJoursRttRenonces <= 0) {
            throw new IllegalArgumentException(
                    "nombreJoursRttRenonces doit être strictement positif");
        }
        if (salaireJournalierBrut == null) {
            throw new IllegalArgumentException("salaireJournalierBrut est requis");
        }
        if (salaireJournalierBrut.signum() <= 0) {
            throw new IllegalArgumentException(
                    "salaireJournalierBrut doit être strictement positif");
        }
        if (joursAcquisDansFenetre == null) {
            throw new IllegalArgumentException("joursAcquisDansFenetre est requis");
        }
    }
}
