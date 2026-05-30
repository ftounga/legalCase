package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-11 : réponse de l'analyse de la rupture d'un VRP statutaire (statut,
 * préavis, indemnité de clientèle — art. L.7311-1 et s. CT). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public record VrpIndemniteClienteleResponse(
        UUID caseFileId,
        LocalDate dateEntree,
        LocalDate dateRupture,
        VrpCauseRupture causeRupture,
        VrpTypeVrp typeVrp,
        BigDecimal commissionsAnnuellesMoyennes,
        BigDecimal salaireMensuelMoyen,
        boolean clienteleDeveloppee,
        long ancienneteMois,
        int dureePreavisMois,
        VrpEligibiliteClientele eligibiliteClientele,
        String motifNonDue,
        BigDecimal indemniteClienteleMin,
        BigDecimal indemniteClienteleMax,
        BigDecimal indemniteLegaleLicenciement,
        VrpOptionRecommandee optionRecommandee,
        String country,
        String baseJuridique
) {}
