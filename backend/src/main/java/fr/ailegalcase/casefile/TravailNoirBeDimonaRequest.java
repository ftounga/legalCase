package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-26 : payload de création / mise à jour de l'analyse
 * <i>Travail noir BE — DIMONA, requalification et sanctions</i>
 * (Loi-programme du 24/12/2002 art. 167-184 + AR du 05/11/2002 +
 * Code pénal social art. 181 niveau 4).
 *
 * <h2>Logique métier — éléments cumulatifs</h2>
 * <ol>
 *   <li><b>Statut DIMONA</b> ({@code statutDimona}) — voir
 *       {@link TravailNoirBeDimonaStatutDimona}. Détermine la
 *       branche du verdict (CONFORME / TARDIVE / ABSENTE /
 *       INDEPENDANT).</li>
 *   <li><b>Date début occupation</b> ({@code dateDebutOccupation})
 *       — date à laquelle le travailleur a effectivement commencé
 *       à travailler chez l'employeur. Référence pour calculer la
 *       durée non déclarée et les cotisations rétroactives.</li>
 *   <li><b>Date DIMONA effective</b> ({@code dateDimonaEffective})
 *       — date à laquelle la déclaration a été enregistrée à
 *       l'ONSS (si applicable, sinon null).</li>
 *   <li><b>Date du contrôle</b> ({@code dateControle}) — date du
 *       contrôle inspection sociale (SPF Emploi / ONSS / Auditorat).
 *       Borne supérieure de la période non déclarée.</li>
 *   <li><b>Salaire brut mensuel</b> ({@code salaireBrutMensuel}) —
 *       rémunération brute moyenne perçue par le travailleur sur
 *       la période non déclarée. Base de calcul des cotisations
 *       ONSS rétroactives (~25 % employeur + 13,07 % travailleur).</li>
 *   <li><b>Nombre de travailleurs concernés</b>
 *       ({@code nombreTravailleursConcernes}) — multiplicateur de
 *       l'amende pénale art. 103 § 2 C. pén. soc. (plafonné 100 ×
 *       maximum unitaire personne morale art. 105 § 1).</li>
 *   <li><b>Personne morale</b> ({@code personneMorale}) — majoration
 *       × 5 art. 105 § 1 C. pén. soc.</li>
 *   <li><b>Récidive ≤ 1 an</b> ({@code recidiveDansLAn}) — doublement
 *       du plafond d'amende art. 110 C. pén. soc.</li>
 *   <li><b>Éléments de subordination</b>
 *       ({@code elementsSubordination}) — caractérisation des
 *       éléments de subordination juridique (horaires, instructions,
 *       intégration). Active la présomption de salariat art. 328
 *       § 1 C. pén. soc. lorsque OUI + absence DIMONA.</li>
 * </ol>
 */
public record TravailNoirBeDimonaRequest(

        @NotNull(message = "statutDimona est requis")
        TravailNoirBeDimonaStatutDimona statutDimona,

        @NotNull(message = "dateDebutOccupation est requis")
        LocalDate dateDebutOccupation,

        LocalDate dateDimonaEffective,

        @NotNull(message = "dateControle est requis")
        LocalDate dateControle,

        @NotNull(message = "salaireBrutMensuel est requis")
        @DecimalMin(value = "0.00", message = "salaireBrutMensuel doit être positif ou nul")
        @Digits(integer = 9, fraction = 2,
                message = "salaireBrutMensuel : 9 chiffres avant la virgule, 2 après")
        BigDecimal salaireBrutMensuel,

        @NotNull(message = "nombreTravailleursConcernes est requis")
        @Min(value = 1, message = "nombreTravailleursConcernes doit être ≥ 1")
        @Max(value = 1000000,
                message = "nombreTravailleursConcernes plafonné à 1 000 000")
        Integer nombreTravailleursConcernes,

        @NotNull(message = "personneMorale est requis")
        Boolean personneMorale,

        @NotNull(message = "recidiveDansLAn est requis")
        Boolean recidiveDansLAn,

        @NotNull(message = "elementsSubordination est requis")
        TravailNoirBeDimonaElementsSubordination elementsSubordination
) {
}
