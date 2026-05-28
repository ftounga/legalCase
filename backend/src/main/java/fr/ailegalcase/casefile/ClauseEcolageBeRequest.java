package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-17 : payload de création / mise à jour de l'analyse <i>clause
 * d'écolage BE</i> (art. 22bis Loi 03/07/1978 introduit par Loi
 * 27/12/2006, M.B. 28/12/2006).
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Type de formation</b> ({@code typeFormation}) : SPECIFIQUE
 *       (clause possible), OBLIGATOIRE_LEGALE (clause nulle) ou
 *       INDETERMINE (analyse cas par cas).</li>
 *   <li><b>Forme écrite à l'entrée en formation</b>
 *       ({@code clauseEcriteAvantEntreeFormation}) : art. 22bis § 1er,
 *       al. 1er.</li>
 *   <li><b>Coût réel de la formation</b>
 *       ({@code coutReelFormationEuros}) : doit être &gt; ½ × RMMMG
 *       mensuel — art. 22bis § 2, al. 3, 2°.</li>
 *   <li><b>RMMMG mensuel</b>
 *       ({@code rmmmgMensuelEuros}) : revenu minimum mensuel moyen garanti
 *       (CCT n° 43 du CNT) au moment de la signature — paramétrable car
 *       indexé annuellement.</li>
 *   <li><b>Durée d'efficacité de la clause</b>
 *       ({@code dureeEfficaciteMois}) : ≤ 36 mois (3 ans, art. 22bis § 4
 *       al. 1er).</li>
 *   <li><b>Dates</b> : signature de la clause, fin de formation, départ
 *       effectif du travailleur — déterminent dans quel tiers de la
 *       durée d'efficacité tombe le départ (dégressivité 100/66/33 %).</li>
 *   <li><b>Motif de départ</b>
 *       ({@code motifDepart}) : conditionne l'opposabilité de la clause
 *       (art. 22bis § 3).</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code typeFormation}.</li>
 *   <li>{@code clauseEcriteAvantEntreeFormation}.</li>
 *   <li>{@code coutReelFormationEuros} ≥ 0 €.</li>
 *   <li>{@code rmmmgMensuelEuros} &gt; 0 €.</li>
 *   <li>{@code dureeEfficaciteMois} ≥ 1.</li>
 *   <li>{@code dateFinFormation}, {@code dateDepartTravailleur} (dates).</li>
 *   <li>{@code motifDepart}.</li>
 * </ul>
 */
public record ClauseEcolageBeRequest(

        @NotNull(message = "typeFormation est requis")
        ClauseEcolageBeTypeFormation typeFormation,

        @NotNull(message = "clauseEcriteAvantEntreeFormation est requise")
        Boolean clauseEcriteAvantEntreeFormation,

        @NotNull(message = "coutReelFormationEuros est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "coutReelFormationEuros doit être positif ou nul")
        BigDecimal coutReelFormationEuros,

        @NotNull(message = "rmmmgMensuelEuros est requis")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "rmmmgMensuelEuros doit être strictement positif")
        BigDecimal rmmmgMensuelEuros,

        @NotNull(message = "dureeEfficaciteMois est requise")
        @Min(value = 1, message = "dureeEfficaciteMois doit être ≥ 1")
        Integer dureeEfficaciteMois,

        @NotNull(message = "dateFinFormation est requise")
        LocalDate dateFinFormation,

        @NotNull(message = "dateDepartTravailleur est requise")
        LocalDate dateDepartTravailleur,

        @NotNull(message = "motifDepart est requis")
        ClauseEcolageBeMotifDepart motifDepart
) {
}
