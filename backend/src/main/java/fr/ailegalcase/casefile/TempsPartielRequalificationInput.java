package fr.ailegalcase.casefile;

/**
 * SF-212-33 : données d'entrée du calculateur de requalification d'un
 * contrat à temps partiel en temps complet (F-DT-49-temps-partiel-requalification,
 * FRANCE — L. 3123-1 à L. 3123-20 CT, L. 3123-6 mentions obligatoires,
 * L. 3123-9 plafond heures complémentaires 1/3, L. 3245-1 prescription
 * triennale rappel salaire, Cass. soc. 22/01/1992 présomption réfragable
 * de temps complet en l'absence des mentions obligatoires).
 *
 * <p>Le seuil légal des heures complémentaires sans requalification est
 * fixé à 1/3 de la durée contractuelle (L. 3123-9) ; au-delà, le salarié
 * peut prétendre à la requalification en temps complet et au rappel de
 * salaire sur 3 ans (L. 3245-1).</p>
 *
 * @param dureeContractuelleHeures            durée hebdomadaire contractuelle
 *                                            en heures (ex. 24h, 28h)
 * @param mentionsDureePresentes              true si le contrat écrit
 *                                            mentionne la durée (L. 3123-6)
 * @param mentionsRepartitionPresentes        true si le contrat écrit
 *                                            mentionne la répartition entre
 *                                            jours/semaines (L. 3123-6)
 * @param mentionsHCPresentes                 true si le contrat écrit
 *                                            mentionne les limites des
 *                                            heures complémentaires
 * @param heuresComplementairesReelleMoyenneHeures heures complémentaires
 *                                            effectuées en moyenne par
 *                                            semaine (cumul sur la période
 *                                            de référence / 52)
 * @param modificationRepartitionUnilaterale  true si l'employeur a modifié
 *                                            unilatéralement la répartition
 *                                            sans respecter la procédure
 *                                            (L. 3123-12 délai 7 jours
 *                                            ouvrés et clause contractuelle)
 * @param ancienneteMois                      ancienneté du salarié en mois
 *                                            (borne le rappel de salaire à
 *                                            36 mois max — L. 3245-1)
 * @param salaireMensuelContractuelEuros      salaire mensuel brut contractuel
 *                                            (base du calcul du rappel)
 */
public record TempsPartielRequalificationInput(
        Double dureeContractuelleHeures,
        Boolean mentionsDureePresentes,
        Boolean mentionsRepartitionPresentes,
        Boolean mentionsHCPresentes,
        Double heuresComplementairesReelleMoyenneHeures,
        Boolean modificationRepartitionUnilaterale,
        Integer ancienneteMois,
        Double salaireMensuelContractuelEuros
) {
}
