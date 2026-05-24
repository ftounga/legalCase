package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-15 : réponse de l'endpoint d'analyse de la conformité du dispositif
 * de télétravail et des litiges courants (F-DT-82-teletravail-accord,
 * FRANCE — L. 1222-9 à L. 1222-11 CT ; ANI télétravail 26/11/2020).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (verdict, score, points d'analyse, alertes
 * AT/refus-licenciement, estimation indemnité due, bases juridiques).</p>
 */
public record TeletravailAccordResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        TeletravailAccordInput.CadreTeletravail cadreTeletravail,
        boolean doubleVolontariatRespectee,
        boolean indemniteOccupationVersee,
        Double montantIndemniteJournalierEuros,
        boolean accidentDomicileDetecte,
        boolean retourBureauImposeUnilateralement,
        boolean refusTeletravailCauseIncrimination,
        // --- Outputs calculés ---
        TeletravailAccordCalculator.AnalyseTeletravail analyseTeletravail,
        int scoreTeletravail,
        List<TeletravailAccordCalculator.PointTeletravail> pointsTeletravail,
        boolean alerteAccidentTravailDomicile,
        boolean alerteRefusTeletravailLicenciement,
        Double indemniteDueEstimeeEuros,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
