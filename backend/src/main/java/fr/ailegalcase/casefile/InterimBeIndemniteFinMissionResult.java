package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-15 : résultat brut produit par
 * {@link InterimBeIndemniteFinMissionCalculator} — fonction <b>pure</b>.
 *
 * <p>Outputs : décomposition détaillée des indemnités exigibles à la
 * fin d'une mission intérimaire belge :</p>
 * <ul>
 *   <li>{@code peculeVacancesInterim} — 15,38 % du brut total prestations
 *       (sauf si déjà versé par le FSI auquel cas 0,00 €).</li>
 *   <li>{@code primeFinAnneeSectorielle} — taux selon CP utilisateur,
 *       conditionnée à ancienneté ≥ 65 jours.</li>
 *   <li>{@code indemniteRuptureAnticipee} — montant brut restant à courir
 *       jusqu'au terme prévu (rupture anticipée Cass. BE).</li>
 *   <li>{@code sursalaireHeuresSupplementaires} — sursalaire 50 % semaine
 *       + 100 % dimanche/férié sur heures sup déclarées.</li>
 *   <li>{@code totalIndemnitesBrutes} — somme.</li>
 *   <li>{@code primePrecaritePresumee} — toujours 0,00 € en BE
 *       (contraste FR). Snapshot explicite pour démonstration.</li>
 * </ul>
 */
public record InterimBeIndemniteFinMissionResult(
        // Inputs (snapshot)
        LocalDate dateDebutMission,
        LocalDate dateFinPrevue,
        LocalDate dateFinReelle,
        int dureeReellePrestationJours,
        int dureePrevueJours,
        BigDecimal salaireHoraireBrut,
        BigDecimal heuresPrestees,
        BigDecimal heuresSupplementairesSemaine,
        BigDecimal heuresSupplementairesDimancheFerie,
        boolean peculeDejaVerseParFsi,
        boolean ruptureAnticipeeParEtiSansMotifGrave,
        InterimBeCommissionParitaire commissionParitaireUtilisateur,
        int ancienneteSectorielleJours,

        // Outputs ventilés
        InterimBeIndemniteFinMissionVerdict verdict,
        BigDecimal salaireBrutTotalPrestations,
        BigDecimal peculeVacancesInterim,
        BigDecimal primeFinAnneeSectorielle,
        BigDecimal indemniteRuptureAnticipee,
        BigDecimal sursalaireHeuresSupplementaires,
        BigDecimal totalIndemnitesBrutes,
        BigDecimal primePrecaritePresumee,
        BigDecimal tauxPrimeFinAnneeApplique,
        int joursRestantsACourirJusquAuTerme,
        boolean ancienneteSectorielleSuffisante,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
