package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-14-03 : résultat de l'analyse de regroupement familial d'un citoyen UE
 * (non-Belge) en Belgique — art. 40bis Loi 15/12/1980.
 */
public record Belgian40bisResult(
        String lienFamilial,
        boolean regroupantCitoyenUe,
        String regroupantActiviteCategorie,
        boolean ressourcesSuffisantes,
        boolean assuranceMaladieUe,
        boolean logementSuffisant,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        boolean lienValide,
        boolean regroupantValide,
        boolean ressourcesOk,
        boolean assuranceOk,
        boolean logementOk,
        boolean pasMenace,
        int scoreGlobal,
        String verdictProbabilite,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstruction,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
