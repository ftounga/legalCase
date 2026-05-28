package fr.ailegalcase.casefile;

/**
 * SF-219-13 : verdict de l'outil <i>étudiant jobiste BE</i> (régime
 * du contrat d'occupation d'étudiants — <b>Loi du 03/07/1978</b>,
 * Titre VII, art. 120 à 130ter, modifié par la <b>Loi-programme du
 * 24/12/2002</b> (instauration des cotisations de solidarité
 * réduites), <b>AR du 14/07/1995</b> (modalités d'exécution), <b>Loi
 * du 18/12/2016</b> (passage des 50 jours à 475 heures), <b>Loi-
 * programme du 28/12/2022 + AR du 06/03/2023</b> (relèvement
 * transitoire à 600h/an pour 2023-2024), <b>Loi-programme du
 * 22/12/2023</b> (M.B. 29/12/2023) pérennisant le quota à 600h/an.
 *
 * <h2>Conditions cumulatives V1</h2>
 * <ol>
 *   <li><b>Statut étudiant</b> à titre principal (inscription
 *       régulière en enseignement secondaire / supérieur / doctorat)
 *       OU interruption courte ≤ 12 mois entre cycles.</li>
 *   <li><b>Quota annuel 600 heures</b> (compteur consultable sur
 *       Student@work — student.be) à respecter sur l'année civile.</li>
 *   <li><b>Contrat d'occupation étudiant écrit</b> (art. 123 Loi
 *       03/07/1978) signé au plus tard au début des prestations.</li>
 *   <li><b>Dimona spécifique type STU</b> (déclaration immédiate de
 *       l'emploi à l'ONSS, AR du 05/11/2002) avant le début des
 *       prestations.</li>
 *   <li><b>Cotisations de solidarité ONSS réduites</b> : 5,42 %
 *       patronale + 2,71 % personnelle (total 8,13 %) — base
 *       rémunération brute (AR 14/07/1995, montants confirmés par
 *       AR 22/12/2023).</li>
 * </ol>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #ELIGIBLE} — toutes les conditions cumulatives sont
 *       réunies, occupation étudiant <b>régulière</b>.</li>
 *   <li>{@link #INELIGIBLE_STATUT_NON_ETUDIANT} — pas étudiant à titre
 *       principal ni en interruption courte ≤ 12 mois — pas de statut
 *       jobiste possible.</li>
 *   <li>{@link #INELIGIBLE_QUOTA_DEPASSE} — les heures du contrat font
 *       basculer le compteur annuel au-delà du quota 600h ; la part
 *       excédentaire passe au régime ordinaire (cotisations ~50 %).</li>
 *   <li>{@link #FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} — statut + quota
 *       OK mais contrat écrit absent ou Dimona STU non déclarée —
 *       statut formellement irrégulier (risque de requalification
 *       ONSS au régime normal + récupération cotisations).</li>
 *   <li>{@link #FRAGILE_COTISATIONS_NON_REDUITES} — statut + formalisme
 *       OK mais cotisations déclarées hors barème AR 14/07/1995 (5,42 %
 *       + 2,71 %) — risque de redressement ONSS pour calcul erroné.</li>
 *   <li>{@link #A_ANALYSER} — vide pédagogique &gt; 12 mois mais
 *       inscription confirmée pour l'année suivante — zone grise
 *       jurisprudentielle, analyse avocat BE requise.</li>
 * </ul>
 */
public enum EtudiantJobisteBeVerdict {
    /** Occupation étudiant pleinement régulière. */
    ELIGIBLE,

    /** Statut formellement défaillant (contrat / Dimona STU). */
    FRAGILE_CONTRAT_OU_DIMONA_MANQUANT,

    /** Cotisations ONSS déclarées hors barème AR 14/07/1995 — risque de redressement. */
    FRAGILE_COTISATIONS_NON_REDUITES,

    /** Pas d'inscription régulière, pas d'interruption assimilable. */
    INELIGIBLE_STATUT_NON_ETUDIANT,

    /** Quota annuel 600h dépassé — la part excédentaire bascule au régime ordinaire. */
    INELIGIBLE_QUOTA_DEPASSE,

    /** Vide pédagogique &gt; 12 mois avec réinscription confirmée — zone grise. */
    A_ANALYSER
}
