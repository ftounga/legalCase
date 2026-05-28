package fr.ailegalcase.casefile;

/**
 * SF-219-13 : statut du candidat à l'occupation étudiant jobiste BE
 * (Loi du 03/07/1978, art. 120 — conditions d'accès au régime).
 *
 * <p>Le contrat d'occupation étudiant est <b>strictement réservé</b>
 * aux personnes ayant le <b>statut d'étudiant à titre principal</b> :
 * inscription régulière en enseignement secondaire, supérieur, ou en
 * doctorat. Sont assimilés certains parcours en interruption courte
 * (entre cycles, ≤ 12 mois consécutifs entre fin d'un cycle et
 * inscription au cycle suivant — tolérance jurisprudence sociale BE).</p>
 *
 * <ul>
 *   <li>{@link #ETUDIANT_PRINCIPAL} — inscription régulière en cours
 *       d'année académique ; statut étudiant pleinement reconnu (accès
 *       au régime jobiste sans restriction de fond).</li>
 *   <li>{@link #INTERRUPTION_COURTE_MOINS_12_MOIS} — entre deux cycles
 *       (p. ex. juillet-août-septembre entre secondaire et supérieur,
 *       ou entre bachelier et master) ; tolérance ≤ 12 mois consécutifs
 *       — accès maintenu.</li>
 *   <li>{@link #VIDE_PEDAGOGIQUE_PROLONGE} — interruption &gt; 12 mois
 *       mais inscription confirmée pour l'année suivante ; statut en
 *       <b>zone grise</b> nécessitant une analyse juridique (avocat BE).</li>
 *   <li>{@link #NON_ETUDIANT} — pas d'inscription régulière, pas
 *       d'interruption assimilable ; <b>pas de statut jobiste</b>
 *       possible (catégorie résiduelle).</li>
 * </ul>
 *
 * <p>Cas spéciaux non couverts V1 : étudiants étrangers hors UE
 * (permis de travail spécifique requis), élèves de l'enseignement à
 * horaire réduit ou en alternance (régime spécifique CEFA / IFAPME /
 * Syntra), apprentis classes moyennes (régime stage rémunéré).</p>
 */
public enum EtudiantJobisteBeStatutEtudiant {
    /** Inscription régulière en cours — étudiant à titre principal. */
    ETUDIANT_PRINCIPAL,

    /** Interruption ≤ 12 mois entre cycles — statut maintenu. */
    INTERRUPTION_COURTE_MOINS_12_MOIS,

    /** Interruption &gt; 12 mois mais réinscription confirmée — zone grise. */
    VIDE_PEDAGOGIQUE_PROLONGE,

    /** Pas d'inscription régulière, pas d'interruption assimilable. */
    NON_ETUDIANT
}
