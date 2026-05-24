package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-35 : réponse de l'endpoint d'analyse de la conformité d'un PDV ou
 * d'une RCC (F-DT-46-pdv-rcc-conformite, FRANCE — L. 1237-17 à L. 1237-19-14
 * CT — RCC instituée par l'ord. n°2017-1387 du 22/09/2017 ; décret
 * 2017-1718 du 20/12/2017 ; circulaire DGT du 19/09/2018).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (verdict, score, points d'irrégularité,
 * confirmation du droit ARE, bases juridiques).</p>
 */
public record PdvRccConformiteResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        PdvRccConformiteInput.TypeDispositif typeDispositif,
        boolean accordCollectifMajoritaire,
        boolean validationDREETSObtenue,
        Boolean delaiValidation15JRespectee,
        boolean indemnitesAuMoinsLegales,
        boolean adhesionVolontaireIndividuelle,
        boolean informationIndividuelleComplete,
        // --- Outputs calculés ---
        PdvRccConformiteCalculator.AnalysePdvRcc analysePdvRcc,
        int scoreConformite,
        List<PdvRccConformiteCalculator.PointIrregularite> pointsIrregularite,
        boolean droitAreConfirme,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
