package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-22 : payload de création / mise à jour de l'analyse <i>égalité
 * salariale femmes / hommes BE</i> (Loi du 22/04/2012 visant à lutter
 * contre l'écart salarial entre hommes et femmes M.B. 28/08/2012 +
 * AR du 17/08/2013 portant exécution de l'art. 2 + AR du 25/04/2014
 * modifiant les formulaires).
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Effectif entreprise</b> ({@code effectifEntreprise}) —
 *       art. 2 § 1er : seuil de 50 travailleurs (ETP, Loi 04/12/2007
 *       art. 7) pour déclencher l'obligation rapport biennal.</li>
 *   <li><b>Statut rapport</b> ({@code statutRapport}) — aiguille le
 *       verdict (déposé complet / déposé incomplet / en préparation /
 *       non déposé / indéterminé).</li>
 *   <li><b>Date dépôt rapport</b> ({@code dateDepotRapport}) — date de
 *       dépôt au CE ou DS (formulaire AR 25/04/2014 ann. I ou II).</li>
 *   <li><b>Ventilation niveau fonction</b>
 *       ({@code ventilationNiveauFonctionFournie}) — art. 4 AR
 *       17/08/2013 : ventilation par classification conventionnelle.</li>
 *   <li><b>Ventilation ancienneté</b>
 *       ({@code ventilationAncienneteFournie}) — art. 4 AR 17/08/2013.</li>
 *   <li><b>Ventilation qualification</b>
 *       ({@code ventilationQualificationFournie}) — art. 4 AR
 *       17/08/2013 : niveau de qualification requis du poste.</li>
 *   <li><b>Ventilation régime travail</b>
 *       ({@code ventilationRegimeTravailFournie}) — art. 4 AR
 *       17/08/2013 : temps plein vs temps partiel.</li>
 *   <li><b>Ventilation composants rémunération</b>
 *       ({@code ventilationComposantsRemunerationFournie}) — art. 4
 *       AR 17/08/2013 : salaire de base, primes, avantages
 *       complémentaires séparés.</li>
 *   <li><b>Écart salarial non justifié constaté</b>
 *       ({@code ecartSalarialNonJustifieConstate}) — résultat de
 *       l'analyse art. 5 Loi 22/04/2012.</li>
 *   <li><b>Pourcentage écart constaté</b>
 *       ({@code pourcentageEcartConstate}) — magnitude indicative de
 *       l'écart (0 à 100), uniquement informatif (le verdict ne
 *       dépend pas du seuil de l'écart, simplement de son existence
 *       et du plan d'action).</li>
 *   <li><b>Plan d'action établi</b>
 *       ({@code planActionEtabli}) — plan d'action concret avec
 *       objectifs chiffrés et calendrier établi dans les délais
 *       (art. 5 Loi 22/04/2012 + CCT sectorielle ou défaut 6 mois).</li>
 *   <li><b>Médiateur désigné</b>
 *       ({@code mediateurDesigne}) — désignation facultative d'un
 *       médiateur dans l'entreprise (art. 14 Loi 22/04/2012 + CCT
 *       n° 25). Indicateur recommandation bonne pratique.</li>
 *   <li><b>Plainte IEFH ou inspection en cours</b>
 *       ({@code plainteIefhOuInspectionEnCours}) — indicateur d'un
 *       contentieux en cours (Institut pour l'égalité des femmes et
 *       des hommes / SPF Emploi inspection).</li>
 * </ol>
 */
public record EgaliteFemmesHommesBeRequest(

        @NotNull(message = "effectifEntreprise est requis")
        @Min(value = 0, message = "effectifEntreprise doit être positif ou nul")
        @Max(value = 1000000, message = "effectifEntreprise plafonné à 1 000 000")
        Integer effectifEntreprise,

        @NotNull(message = "statutRapport est requis")
        EgaliteFemmesHommesBeStatutRapport statutRapport,

        LocalDate dateDepotRapport,

        @NotNull(message = "ventilationNiveauFonctionFournie est requis")
        Boolean ventilationNiveauFonctionFournie,

        @NotNull(message = "ventilationAncienneteFournie est requis")
        Boolean ventilationAncienneteFournie,

        @NotNull(message = "ventilationQualificationFournie est requis")
        Boolean ventilationQualificationFournie,

        @NotNull(message = "ventilationRegimeTravailFournie est requis")
        Boolean ventilationRegimeTravailFournie,

        @NotNull(message = "ventilationComposantsRemunerationFournie est requis")
        Boolean ventilationComposantsRemunerationFournie,

        @NotNull(message = "ecartSalarialNonJustifieConstate est requis")
        Boolean ecartSalarialNonJustifieConstate,

        @DecimalMin(value = "0.0", message = "pourcentageEcartConstate doit être positif ou nul")
        @DecimalMax(value = "100.0", message = "pourcentageEcartConstate doit être ≤ 100")
        @Digits(integer = 3, fraction = 2, message = "pourcentageEcartConstate au format XXX.XX")
        BigDecimal pourcentageEcartConstate,

        @NotNull(message = "planActionEtabli est requis")
        Boolean planActionEtabli,

        @NotNull(message = "mediateurDesigne est requis")
        Boolean mediateurDesigne,

        @NotNull(message = "plainteIefhOuInspectionEnCours est requis")
        Boolean plainteIefhOuInspectionEnCours
) {
}
