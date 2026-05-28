package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-12 : payload de création / mise à jour de l'analyse <i>flexi-job
 * BE</i> (Loi-programme du 26/12/2013 + extensions 2015/2018/2023).
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Travailleur</b> ({@code statutTravailleur}) : pensionné OU
 *       salarié ≥ 4/5 ETP chez un autre employeur au trimestre T-3.</li>
 *   <li><b>Employeur</b> ({@code secteurEmployeur}) : commission paritaire
 *       listée dans les lois-programmes successives (HoReCa 2013 + ext.
 *       2015 boulangerie/coiffure + ext. 2018 commerce/agriculture/soins
 *       + ext. 2023 sport/culture/secteur public limité).</li>
 *   <li><b>Cumul interdit même employeur</b>
 *       ({@code dejaOccupeChezMemeEmployeur}) : un travailleur ne peut
 *       pas être à la fois salarié principal et flexi chez le même
 *       employeur (anti-substitution).</li>
 *   <li><b>Formalisme</b> : contrat-cadre écrit
 *       ({@code contratCadreSigne}) + déclaration Dimona spécifique FLX
 *       ({@code dimonaFlxDeclaree}).</li>
 *   <li><b>Rémunération</b> : {@code flexiSalaireHoraireBrut} doit être
 *       ≥ minimum légal indexé (~12,33 €/h au 01/05/2024 — paramétrable
 *       par {@code flexiSalaireMinimumApplicable}) ; cumul annuel
 *       ({@code revenuAnnuelFlexiCumule}) sous le plafond exonéré
 *       (~12 000 € au 01/05/2024, paramétrable par
 *       {@code plafondAnnuelExonere}).</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code datePremiereOccupationFlexi} — date de début de la
 *       première prestation flexi pour cet employeur.</li>
 *   <li>{@code statutTravailleur} — catégorie du candidat flexi.</li>
 *   <li>{@code secteurEmployeur} — secteur d'activité de l'employeur.</li>
 *   <li>{@code dejaOccupeChezMemeEmployeur} — drapeau cumul interdit.</li>
 *   <li>{@code contratCadreSigne} — formalisme contrat-cadre écrit.</li>
 *   <li>{@code dimonaFlxDeclaree} — déclaration Dimona FLX faite.</li>
 *   <li>{@code flexiSalaireHoraireBrut} — taux horaire brut convenu (€).</li>
 *   <li>{@code flexiSalaireMinimumApplicable} — minimum légal applicable
 *       à la date d'occupation (€).</li>
 *   <li>{@code revenuAnnuelFlexiCumule} — revenu flexi cumulé sur l'année
 *       civile (€).</li>
 *   <li>{@code plafondAnnuelExonere} — plafond annuel exonéré applicable
 *       (€). Le plafond ne s'applique pas aux pensionnés.</li>
 * </ul>
 */
public record FlexiJobBeRequest(

        @NotNull(message = "datePremiereOccupationFlexi est requise")
        LocalDate datePremiereOccupationFlexi,

        @NotNull(message = "statutTravailleur est requis")
        FlexiJobBeStatutTravailleur statutTravailleur,

        @NotNull(message = "secteurEmployeur est requis")
        FlexiJobBeSecteurEmployeur secteurEmployeur,

        @NotNull(message = "dejaOccupeChezMemeEmployeur est requis")
        Boolean dejaOccupeChezMemeEmployeur,

        @NotNull(message = "contratCadreSigne est requis")
        Boolean contratCadreSigne,

        @NotNull(message = "dimonaFlxDeclaree est requis")
        Boolean dimonaFlxDeclaree,

        @NotNull(message = "flexiSalaireHoraireBrut est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "flexiSalaireHoraireBrut doit être positif ou nul")
        BigDecimal flexiSalaireHoraireBrut,

        @NotNull(message = "flexiSalaireMinimumApplicable est requis")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "flexiSalaireMinimumApplicable doit être strictement positif")
        BigDecimal flexiSalaireMinimumApplicable,

        @NotNull(message = "revenuAnnuelFlexiCumule est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "revenuAnnuelFlexiCumule doit être positif ou nul")
        BigDecimal revenuAnnuelFlexiCumule,

        @NotNull(message = "plafondAnnuelExonere est requis")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "plafondAnnuelExonere doit être strictement positif")
        BigDecimal plafondAnnuelExonere
) {
}
