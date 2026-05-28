package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-15 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/interim-be-indemnite-fin-mission}.
 *
 * <p>Snapshot complet (inputs + décomposition + verdict) — pattern
 * uniforme avec les autres outils décisionnels BE de la F-219
 * (miroir {@link InterimBeCct322Response} SF-219-14).</p>
 */
public record InterimBeIndemniteFinMissionResponse(
        UUID caseFileId,

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

        // Outputs
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
