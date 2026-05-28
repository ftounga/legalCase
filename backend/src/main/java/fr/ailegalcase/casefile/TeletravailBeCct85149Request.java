package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-16 : payload de création / mise à jour de l'analyse <i>télétravail
 * BE — CCT n° 85 (structurel) / CCT n° 149 (occasionnel)</i>.
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Type de télétravail</b> ({@code typeTeletravail}) : structurel
 *       CCT n° 85, occasionnel CCT n° 149 ou indéterminé.</li>
 *   <li><b>Volontariat réciproque</b> ({@code volontariatReciproque}) :
 *       art. 5 CCT n° 85 (structurel uniquement).</li>
 *   <li><b>Convention individuelle écrite</b> ({@code conventionEcriteIndividuelle}) :
 *       art. 6 CCT n° 85 — obligatoire pour le structurel.</li>
 *   <li><b>Équipement fourni / indemnisé</b> ({@code equipementFourniOuIndemnise}) :
 *       art. 9 CCT n° 85 — obligation employeur.</li>
 *   <li><b>Indemnité forfaitaire mensuelle</b>
 *       ({@code indemniteForfaitaireMensuelleEuros}) : usage admis, sous
 *       plafond ONSS / SPF Finances révisé annuellement (le plafond est
 *       passé en input pour rester paramétrable).</li>
 *   <li><b>Droits sociaux maintenus</b> ({@code droitsSociauxMaintenus}) :
 *       art. 4 CCT n° 85 — accès aux mêmes droits et formations.</li>
 *   <li><b>Déconnexion définie</b> ({@code deconnexionDefinie}) : Loi
 *       du 03/10/2022 — entreprises ≥ 20 travailleurs.</li>
 *   <li><b>Effectif entreprise</b> ({@code effectifEntreprise}) : utilisé
 *       pour gating de la Loi 03/10/2022 (déconnexion exigible &gt;= 20).</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateDebutTeletravail} — date d'entrée en vigueur de la
 *       formule.</li>
 *   <li>{@code typeTeletravail}.</li>
 *   <li>{@code volontariatReciproque}, {@code conventionEcriteIndividuelle},
 *       {@code equipementFourniOuIndemnise}, {@code droitsSociauxMaintenus},
 *       {@code deconnexionDefinie}.</li>
 *   <li>{@code indemniteForfaitaireMensuelleEuros} ≥ 0 (€).</li>
 *   <li>{@code plafondIndemniteMensuelleEuros} &gt; 0 (€) — plafond
 *       admis (ONSS / SPF Finances) applicable à la période.</li>
 *   <li>{@code effectifEntreprise} ≥ 0.</li>
 * </ul>
 */
public record TeletravailBeCct85149Request(

        @NotNull(message = "dateDebutTeletravail est requise")
        LocalDate dateDebutTeletravail,

        @NotNull(message = "typeTeletravail est requis")
        TeletravailBeCct85149TypeTeletravail typeTeletravail,

        @NotNull(message = "volontariatReciproque est requis")
        Boolean volontariatReciproque,

        @NotNull(message = "conventionEcriteIndividuelle est requise")
        Boolean conventionEcriteIndividuelle,

        @NotNull(message = "equipementFourniOuIndemnise est requis")
        Boolean equipementFourniOuIndemnise,

        @NotNull(message = "indemniteForfaitaireMensuelleEuros est requise")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "indemniteForfaitaireMensuelleEuros doit être positive ou nulle")
        BigDecimal indemniteForfaitaireMensuelleEuros,

        @NotNull(message = "plafondIndemniteMensuelleEuros est requis")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "plafondIndemniteMensuelleEuros doit être strictement positif")
        BigDecimal plafondIndemniteMensuelleEuros,

        @NotNull(message = "droitsSociauxMaintenus est requis")
        Boolean droitsSociauxMaintenus,

        @NotNull(message = "deconnexionDefinie est requise")
        Boolean deconnexionDefinie,

        @NotNull(message = "effectifEntreprise est requis")
        @Min(value = 0, message = "effectifEntreprise doit être positif ou nul")
        Integer effectifEntreprise
) {
}
