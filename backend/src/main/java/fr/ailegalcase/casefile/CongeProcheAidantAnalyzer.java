package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-47 : analyseur du <b>congé de proche aidant</b> (art. L.3142-16 à
 * L.3142-27 CT, loi n° 2020-220 du 06/03/2020, F-DT-79). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ; distinct du
 * congé parental d'éducation F-DT-78 et des congés pour évènements familiaux
 * F-DT-76) :
 * <ul>
 *   <li><b>Éligibilité (L.3142-16)</b> — le lien (y compris la personne avec
 *       laquelle le salarié réside ou entretient des liens étroits et stables)
 *       ouvre droit au congé si la personne aidée réside en France/EEE de façon
 *       stable et régulière. Sinon {@code statut = NON_ELIGIBLE}.</li>
 *   <li><b>Durée maximale (L.3142-19)</b> — {@code dureeMaxMois = 12} (3 mois
 *       renouvelable, dans la limite d'un an sur l'ensemble de la carrière).
 *       {@code dureeRetenueMois = min(dureeSouhaiteeMois, 12)}.</li>
 *   <li><b>Estimation AJPA</b> — si demandée : montant journalier ≈ 64,54 €
 *       (2026, à vérifier), plafond 66 jours indemnisés sur la carrière.
 *       {@code estimationAjpa = ajpaJournaliere × min(joursOuvrables, 66)} avec
 *       ~22 jours ouvrables par mois.</li>
 *   <li><b>Protection</b> — protection de l'emploi / réintégration et congé non
 *       imputable sur les congés payés (L.3142-20 et s.).</li>
 * </ul>
 *
 * <p>Base juridique annotée « à vérifier par avocat ».
 */
public final class CongeProcheAidantAnalyzer {

    /** Durée maximale du congé en mois (un an sur l'ensemble de la carrière, L.3142-19). */
    static final int DUREE_MAX_MOIS = 12;

    /** Nombre de jours ouvrables retenus par mois pour l'estimation AJPA. */
    static final int JOURS_OUVRABLES_PAR_MOIS = 22;

    /** Plafond de jours indemnisés au titre de l'AJPA sur l'ensemble de la carrière. */
    static final int PLAFOND_JOURS_AJPA = 66;

    /** Montant journalier de l'AJPA (≈ 64,54 € en 2026 — montant à vérifier). */
    static final BigDecimal AJPA_JOURNALIERE = new BigDecimal("64.54");

    static final String BASE_JURIDIQUE =
            "art. L.3142-16 à L.3142-27 du Code du travail — congé de proche aidant : "
                    + "droit ouvert au salarié pour s'occuper d'une personne en situation de "
                    + "handicap ou de perte d'autonomie d'une particulière gravité, résidant "
                    + "en France de façon stable et régulière (art. L.3142-16) ; durée de "
                    + "3 mois renouvelable, dans la limite d'un an pour l'ensemble de la "
                    + "carrière (art. L.3142-19) ; protection de l'emploi, non-imputation sur "
                    + "les congés payés et réintégration (art. L.3142-20 à L.3142-25) ; "
                    + "allocation journalière du proche aidant (AJPA) versée par la CAF, "
                    + "plafonnée à 66 jours indemnisés sur la carrière (loi n° 2020-220 du "
                    + "06/03/2020) — montant journalier ≈ 64,54 € en 2026 (montant à "
                    + "vérifier) (à vérifier par avocat)";

    private CongeProcheAidantAnalyzer() {
    }

    /**
     * Analyse l'éligibilité au congé de proche aidant, sa durée maximale et
     * l'estimation indicative de l'AJPA.
     */
    public static CongeProcheAidantResult analyze(
            CongeProcheAidantLien lienPersonneAidee,
            Boolean personneAideeResideFrance,
            Integer dureeSouhaiteeMois,
            Boolean ajpaDemandee) {

        validate(lienPersonneAidee, personneAideeResideFrance, dureeSouhaiteeMois);

        boolean ajpa = Boolean.TRUE.equals(ajpaDemandee);
        List<String> notes = new ArrayList<>();

        if (!personneAideeResideFrance) {
            notes.add("La personne aidée doit résider en France/EEE de façon stable et "
                    + "régulière pour ouvrir droit au congé de proche aidant (art. L.3142-16 CT). "
                    + "Condition non remplie : statut NON_ELIGIBLE.");
            return new CongeProcheAidantResult(
                    CongeProcheAidantStatut.NON_ELIGIBLE,
                    lienPersonneAidee,
                    false,
                    dureeSouhaiteeMois,
                    DUREE_MAX_MOIS,
                    null,
                    ajpa,
                    null,
                    null,
                    true,
                    true,
                    List.copyOf(notes),
                    BASE_JURIDIQUE);
        }

        int dureeRetenueMois = Math.min(dureeSouhaiteeMois, DUREE_MAX_MOIS);

        switch (lienPersonneAidee) {
            case CONJOINT -> notes.add("Lien retenu : conjoint, concubin ou partenaire de PACS "
                    + "(art. L.3142-16 CT).");
            case ASCENDANT -> notes.add("Lien retenu : ascendant (art. L.3142-16 CT).");
            case DESCENDANT -> notes.add("Lien retenu : descendant (art. L.3142-16 CT).");
            case COLLATERAL -> notes.add("Lien retenu : collatéral jusqu'au 4e degré "
                    + "(art. L.3142-16 CT).");
            case SANS_LIEN_RESIDENCE_COMMUNE -> notes.add("Lien retenu : personne avec laquelle "
                    + "le salarié réside ou entretient des liens étroits et stables, à qui il "
                    + "vient en aide de manière régulière et fréquente à titre non professionnel "
                    + "(art. L.3142-16 CT).");
        }

        notes.add("Durée maximale du congé : 12 mois (3 mois renouvelable, dans la limite d'un "
                + "an pour l'ensemble de la carrière, art. L.3142-19 CT).");
        if (dureeSouhaiteeMois > DUREE_MAX_MOIS) {
            notes.add("Durée souhaitée (" + dureeSouhaiteeMois + " mois) plafonnée à un an sur "
                    + "la carrière : durée retenue = " + dureeRetenueMois + " mois (art. L.3142-19 CT).");
        }

        BigDecimal ajpaJournaliere = null;
        BigDecimal estimationAjpa = null;
        if (ajpa) {
            ajpaJournaliere = AJPA_JOURNALIERE;
            long joursEstimes = (long) dureeRetenueMois * JOURS_OUVRABLES_PAR_MOIS;
            long joursIndemnises = Math.min(joursEstimes, PLAFOND_JOURS_AJPA);
            estimationAjpa = AJPA_JOURNALIERE
                    .multiply(BigDecimal.valueOf(joursIndemnises))
                    .setScale(2, RoundingMode.HALF_UP);
            notes.add("Estimation AJPA indicative : " + estimationAjpa + " € = " + AJPA_JOURNALIERE
                    + " €/jour × " + joursIndemnises + " jours indemnisés (≈ "
                    + JOURS_OUVRABLES_PAR_MOIS + " jours ouvrables/mois sur " + dureeRetenueMois
                    + " mois, plafond 66 jours sur la carrière) — montant journalier 2026 à "
                    + "vérifier (loi n° 2020-220 du 06/03/2020, AJPA versée par la CAF).");
            if (joursEstimes > PLAFOND_JOURS_AJPA) {
                notes.add("Le plafond de 66 jours indemnisés sur l'ensemble de la carrière est "
                        + "atteint : l'estimation AJPA est plafonnée à 66 jours.");
            }
        } else {
            notes.add("AJPA non demandée : aucune estimation d'allocation journalière du proche "
                    + "aidant n'est calculée.");
        }

        notes.add("Le congé de proche aidant n'est pas imputable sur les congés payés et le "
                + "salarié retrouve son emploi ou un emploi similaire à l'issue du congé "
                + "(art. L.3142-20 à L.3142-25 CT).");

        return new CongeProcheAidantResult(
                CongeProcheAidantStatut.ELIGIBLE,
                lienPersonneAidee,
                true,
                dureeSouhaiteeMois,
                DUREE_MAX_MOIS,
                dureeRetenueMois,
                ajpa,
                ajpaJournaliere,
                estimationAjpa,
                true,
                true,
                List.copyOf(notes),
                BASE_JURIDIQUE);
    }

    private static void validate(CongeProcheAidantLien lienPersonneAidee,
                                 Boolean personneAideeResideFrance,
                                 Integer dureeSouhaiteeMois) {
        if (lienPersonneAidee == null) {
            throw new IllegalArgumentException("lienPersonneAidee est requis");
        }
        if (personneAideeResideFrance == null) {
            throw new IllegalArgumentException("personneAideeResideFrance est requis");
        }
        if (dureeSouhaiteeMois == null) {
            throw new IllegalArgumentException("dureeSouhaiteeMois est requis");
        }
        if (dureeSouhaiteeMois <= 0) {
            throw new IllegalArgumentException("dureeSouhaiteeMois doit être strictement positif");
        }
    }
}
