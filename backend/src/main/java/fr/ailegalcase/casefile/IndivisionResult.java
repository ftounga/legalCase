package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-22-01 : résultat de l'analyse d'éligibilité au partage judiciaire d'une
 * indivision post-communautaire (art. 815 Cciv + art. 1364 CPC). Outil
 * single-country FR — BE non couvert.
 */
public record IndivisionResult(
        LocalDate dateOrigineIndivision,
        List<String> natureBiens,
        BigDecimal valeurEstimeeTotaleEur,
        int nbIndivisaires,
        List<BigDecimal> quotesPart,
        List<String> tentativesPartageAmiable,
        boolean consentementPartageGlobal,
        boolean occupationBienParUnIndivisaire,
        int indivisionDureeAnnees,
        boolean demandeMesuresConservatoires,
        boolean conflitOuvertEntreIndivisaires,
        int scoreEligibilitePartageJudiciaire,
        String verdictRecommandation,
        BigDecimal indemniteOccupationDueEur,
        boolean expertiseNotarialeRecommandee,
        boolean licitationRecommandee,
        int delaiProcedurePartageJudiciaireMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
