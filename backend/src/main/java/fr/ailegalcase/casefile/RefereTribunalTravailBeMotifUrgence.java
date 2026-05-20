package fr.ailegalcase.casefile;

/**
 * SF-207-05 : énumération des motifs d'urgence type ouvrant le référé devant
 * le président du tribunal du travail belge (CJ art. 584).
 *
 * <p>Liste non exhaustive — la valeur {@link #AUTRE} couvre les cas
 * atypiques (transfert d'entreprise, mesures conservatoires inédites, etc.)
 * et impose, dans le {@link RefereTribunalTravailBeCalculator}, une
 * description circonstanciée (> 30 caractères) pour caractériser l'urgence.</p>
 */
public enum RefereTribunalTravailBeMotifUrgence {

    /** Harcèlement moral ou sexuel persistant (Loi 04/08/1996 bien-être au travail). */
    HARCELEMENT,

    /** Non-paiement de salaire ou d'indemnité due — péril immédiat sur la subsistance. */
    SALAIRE_IMPAYE,

    /** Modification unilatérale d'un élément essentiel du contrat par l'employeur. */
    MODIFICATION_UNILATERALE,

    /** Autre motif d'urgence — description circonstanciée requise (> 30 car.). */
    AUTRE
}
