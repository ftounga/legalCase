package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-207-02 : projection complète (snapshot) renvoyée au client pour l'outil
 * de checklist C4 ONEM.
 *
 * <p>Inclut les inputs (pour pré-fill UI) et les outputs calculés.</p>
 */
public record C4OnemChecklistResponse(
        UUID caseFileId,
        // Inputs normalisés (pré-fill UI)
        String raisonSocialeEmployeur,
        String numeroBce,
        String nomSalarie,
        String numeroNationalRegistre,
        LocalDate dateEntreeService,
        LocalDate dateSortieService,
        String categorieOnem,
        String motifExplicite,
        boolean fauteGraveMentionnee,
        Integer preavisPresteJours,
        BigDecimal dernierSalaireMensuelBrut,
        // Outputs calculés
        C4OnemChecklistResult.Verdict verdict,
        List<C4OnemChecklistMention> mentionsManquantes,
        boolean fauteGraveDetectee,
        C4OnemChecklistResult.ExclusionOnemRange exclusionOnemRange,
        String lettreRectificativeProposee,
        String baseJuridique,
        C4OnemChecklistResult.EtapeSuivante etapeSuivante,
        // Workspace context (toujours BELGIQUE pour cet outil)
        String country
) {}
