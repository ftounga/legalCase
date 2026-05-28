package fr.ailegalcase.casefile;

/**
 * SF-219-09 : verdict de l'outil <i>élections sociales BE</i> selon
 * la Loi du 04/12/2007 (relative aux élections sociales) + AR du
 * 25/05/2012 (modalités d'organisation).
 *
 * <p>L'outil détermine l'obligation d'organiser des élections sociales
 * pour le Conseil d'Entreprise (CE) et/ou le Comité pour la
 * Prévention et la Protection au Travail (CPPT) en fonction de
 * l'effectif moyen calculé sur la période de référence (4 trimestres
 * précédents le 1er trimestre de l'année des élections).</p>
 *
 * <p>Seuils Loi 04/12/2007 art. 9 § 1er + Loi 04/08/1996 art. 49 :</p>
 * <ul>
 *   <li><b>≥ 100 travailleurs ETP</b> → CE obligatoire + CPPT obligatoire.</li>
 *   <li><b>50 à 99 travailleurs ETP</b> → CPPT seul obligatoire (pas
 *       de CE — le CPPT exerce les compétences d'information /
 *       consultation par défaut).</li>
 *   <li><b>&lt; 50 travailleurs ETP</b> → aucune obligation d'élections
 *       sociales (la délégation syndicale CCT n° 5 peut subsister
 *       hors scope).</li>
 * </ul>
 *
 * <ul>
 *   <li>{@link #OBLIGATION_CE_ET_CPPT} — l'entreprise atteint le seuil
 *       de 100 ETP : double scrutin CE + CPPT à organiser dans la
 *       fenêtre Y imposée par AR (4-17/05/2024, 11-24/05/2028).</li>
 *   <li>{@link #OBLIGATION_CPPT_SEUL} — l'entreprise atteint le seuil
 *       intermédiaire 50-99 ETP : scrutin CPPT seul.</li>
 *   <li>{@link #NON_APPLICABLE_SEUIL} — effectif &lt; 50 ETP : aucune
 *       obligation procédurale Loi 04/12/2007.</li>
 *   <li>{@link #EFFECTIF_A_RECALCULER} — l'effectif déclaré est aux
 *       limites des seuils (49-51 ou 99-101 ETP) : un recalcul ETP
 *       précis sur 4 trimestres est indispensable avant tout
 *       déclenchement de procédure (le seuil exact peut basculer
 *       l'obligation, sanction pénale art. 32 si manquement).</li>
 * </ul>
 */
public enum ElectionsSocialesBeVerdict {
    /** ≥ 100 ETP : CE + CPPT obligatoires. */
    OBLIGATION_CE_ET_CPPT,

    /** 50-99 ETP : CPPT seul obligatoire. */
    OBLIGATION_CPPT_SEUL,

    /** &lt; 50 ETP : pas d'élections sociales obligatoires. */
    NON_APPLICABLE_SEUIL,

    /** Effectif limite (49-51 ou 99-101 ETP) — recalcul ETP requis. */
    EFFECTIF_A_RECALCULER
}
