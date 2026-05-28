package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-15 : payload de création / mise à jour de l'analyse <i>indemnité
 * de fin de mission intérim BE</i> (Loi du 24/07/1987 + CCT n° 322 +
 * CCT sectorielles + jurisprudence Cass. BE).
 *
 * <h2>Logique métier</h2>
 *
 * <p>L'outil calcule <b>les indemnités exigibles à la fin d'une mission
 * intérimaire belge</b> en additionnant les composantes effectivement
 * dues :</p>
 *
 * <ol>
 *   <li><b>Pécule de vacances intérim</b> (15,38 % du brut total
 *       prestations, sauf si déjà versé par le FSI — AR 30/03/1967
 *       art. 19 + CCT n° 322bis).</li>
 *   <li><b>Prime de fin d'année sectorielle</b> (taux variable selon
 *       {@code commissionParitaireUtilisateur} et selon l'ancienneté
 *       sectorielle ≥ 65 jours par défaut).</li>
 *   <li><b>Indemnité de rupture anticipée</b> — si l'ETI a rompu la
 *       mission avant le terme prévu sans motif grave, la jurisprudence
 *       Cass. BE (04/02/1991 + 16/12/2002) ouvre droit aux rémunérations
 *       restant à courir jusqu'au terme.</li>
 *   <li><b>Heures supplémentaires non payées</b> — sursalaire 50 %
 *       semaine + 100 % dimanche/férié (Loi 16/03/1971 art. 29).</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateDebutMission}, {@code dateFinReelle}, {@code dateFinPrevue}
 *       — ancrages temporels.</li>
 *   <li>{@code dureeReellePrestationJours} — nombre de jours
 *       effectivement prestés (≥ 0).</li>
 *   <li>{@code dureePrevueJours} — durée initialement prévue
 *       (&gt; 0).</li>
 *   <li>{@code salaireHoraireBrut} — salaire brut horaire de
 *       l'intérimaire (&gt; 0).</li>
 *   <li>{@code heuresPrestees} — heures normales prestées (≥ 0).</li>
 *   <li>{@code heuresSupplementairesSemaine} — heures sup en semaine
 *       (≥ 0).</li>
 *   <li>{@code heuresSupplementairesDimancheFerie} — heures sup
 *       dimanche/férié (≥ 0).</li>
 *   <li>{@code peculeDejaVerseParFsi} — drapeau pécule de vacances
 *       intérim déjà versé par le Fonds Social Intérimaires.</li>
 *   <li>{@code ruptureAnticipeeParEtiSansMotifGrave} — drapeau
 *       rupture anticipée injustifiée déclenchant indemnité Cass.</li>
 *   <li>{@code commissionParitaireUtilisateur} — code CP sectorielle
 *       déterminant la prime fin d'année conventionnelle applicable.</li>
 *   <li>{@code ancienneteSectorielleJours} — ancienneté sectorielle
 *       cumulée chez le même utilisateur (≥ 0).</li>
 * </ul>
 */
public record InterimBeIndemniteFinMissionRequest(

        @NotNull(message = "dateDebutMission est requise")
        LocalDate dateDebutMission,

        @NotNull(message = "dateFinPrevue est requise")
        LocalDate dateFinPrevue,

        @NotNull(message = "dateFinReelle est requise")
        LocalDate dateFinReelle,

        @NotNull(message = "dureeReellePrestationJours est requise")
        @Min(value = 0, message = "dureeReellePrestationJours doit être positive ou nulle")
        Integer dureeReellePrestationJours,

        @NotNull(message = "dureePrevueJours est requise")
        @Min(value = 1, message = "dureePrevueJours doit être strictement positive")
        Integer dureePrevueJours,

        @NotNull(message = "salaireHoraireBrut est requis")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "salaireHoraireBrut doit être strictement positif")
        BigDecimal salaireHoraireBrut,

        @NotNull(message = "heuresPrestees est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "heuresPrestees doit être positif ou nul")
        BigDecimal heuresPrestees,

        @NotNull(message = "heuresSupplementairesSemaine est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "heuresSupplementairesSemaine doit être positif ou nul")
        BigDecimal heuresSupplementairesSemaine,

        @NotNull(message = "heuresSupplementairesDimancheFerie est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "heuresSupplementairesDimancheFerie doit être positif ou nul")
        BigDecimal heuresSupplementairesDimancheFerie,

        @NotNull(message = "peculeDejaVerseParFsi est requis")
        Boolean peculeDejaVerseParFsi,

        @NotNull(message = "ruptureAnticipeeParEtiSansMotifGrave est requis")
        Boolean ruptureAnticipeeParEtiSansMotifGrave,

        @NotNull(message = "commissionParitaireUtilisateur est requise")
        InterimBeCommissionParitaire commissionParitaireUtilisateur,

        @NotNull(message = "ancienneteSectorielleJours est requise")
        @Min(value = 0, message = "ancienneteSectorielleJours doit être positif ou nul")
        Integer ancienneteSectorielleJours
) {
}
