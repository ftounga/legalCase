package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-12-01 : réponse de l'API mesures provisoires (AOMP).
 */
public record MesuresProvisoiresResponse(
        UUID caseFileId,
        LocalDate dateAudienceAOMP,
        BigDecimal revenusEpouxDemandeurEur,
        BigDecimal revenusEpouxDefendeurEur,
        String logementCommunDescription,
        String logementProprietaire,
        List<MesuresProvisoiresResult.EnfantMineurInfo> enfantsMineurs,
        String souhaitResidenceEnfants,
        boolean violencesAlleguees,
        boolean patrimoineCommunIsignificatif,
        boolean demandeMesureConservatoire,
        String country,
        BigDecimal differentielRevenus,
        BigDecimal pensionAlimentairePropose,
        String attributionLogementRecommande,
        String residenceEnfantsRecommande,
        BigDecimal contributionCharges,
        boolean mesureConservatoireRecommande,
        int scoreCohesionMesures,
        String verdictAcceptabilite,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
