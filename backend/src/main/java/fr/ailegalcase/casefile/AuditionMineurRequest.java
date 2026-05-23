package fr.ailegalcase.casefile;

/**
 * SF-216-13 : body POST /api/v1/case-files/{id}/audition-mineur.
 *
 * <p>Outil single-country FRANCE — art. 388-1 Cciv (droit à l'audition du
 * mineur capable de discernement) + art. 1074-1 à 1074-3 CPC (modalités
 * procédurales) + CIDE art. 12 + Cass. 1ère civ., 18/3/2015, n°14-11.392
 * (refus d'audition doit être spécialement motivé).</p>
 *
 * <p>Le service rejette : âge négatif ou > 18, absence des champs requis,
 * country != FRANCE.</p>
 *
 * @param ageEnfant             âge du mineur en années (0-17). Requis.
 * @param capaciteDiscernement  capacité de discernement appréciée
 *                              (CERTAINE / PROBABLE / DOUTEUSE / INCONNUE).
 *                              Requis.
 * @param demandeFormalisee     true si une demande d'audition a déjà été
 *                              formellement présentée au juge.
 * @param demandeParEnfantLuiMeme true si l'enfant a demandé lui-même son
 *                                audition (art. 388-1 al. 1 Cciv — droit
 *                                propre de l'enfant).
 * @param refusMotive           true si le juge a déjà refusé l'audition
 *                              (optionnel).
 * @param motivationRefus       motif invoqué par le juge pour refuser
 *                              l'audition (texte libre, optionnel).
 * @param procedureEnCours      type de procédure civile en cours.
 */
public record AuditionMineurRequest(
        Integer ageEnfant,
        CapaciteDiscernementEnum capaciteDiscernement,
        Boolean demandeFormalisee,
        Boolean demandeParEnfantLuiMeme,
        Boolean refusMotive,
        String motivationRefus,
        ProcedureAuditionEnum procedureEnCours
) {}
