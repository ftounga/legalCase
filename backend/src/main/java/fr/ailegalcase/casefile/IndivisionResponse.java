package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-22-01 : réponse de l'API indivision post-communautaire (art. 815 Cciv).
 */
public record IndivisionResponse(
        UUID caseFileId,
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
        List<String> messages,
        String country
) {}
