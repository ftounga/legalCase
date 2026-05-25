package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-29 : réponse de l'endpoint d'analyse du congé maternité / paternité
 * (F-DT-77-conge-paternite-maternite, FRANCE — L. 1225-1 à L. 1225-40 CT ;
 * L. 331-3 CSS ; loi du 16/03/2021).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (durée, IJ estimées, période de protection,
 * alertes licenciement / retour poste, bases juridiques).</p>
 */
public record CongeMaternitePaterniteResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        CongeMaternitePaterniteInput.TypeConge typeConge,
        int rangEnfant,
        boolean naissanceMultiple,
        double salaireMensuelBrutEuros,
        LocalDate dateDebutConge,
        boolean licenciementPendantConge,
        boolean retourPosteDifferent,
        // --- Outputs calculés ---
        int dureeCongeJours,
        LocalDate dateFinCongeTheorique,
        double ijJournaliereEstimeeEuros,
        double ijTotaleEstimeeEuros,
        LocalDate protectionLicenciementJusqua,
        boolean alerteLicenciementIllegal,
        boolean alerteRetourPosteIllegal,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
