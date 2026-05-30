package fr.ailegalcase.casefile;

import java.util.UUID;

/**
 * SF-218-07 : réponse de l'analyse de la saisie sur rémunération (quotité
 * saisissable — art. R. 3252-2 et s. Code du travail). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public record SaisieRemunerationResponse(
        UUID caseFileId,
        double remunerationNetteMensuelle,
        int nombrePersonnesACharge,
        double creanceTotale,
        boolean creanceAlimentaire,
        double quotiteSaisissableMensuelle,
        double montantLaisseAuSalarie,
        double fractionInsaisissable,
        int nombreMoisRecouvrement,
        SaisieRemunerationVerdict verdict,
        String country,
        String baseJuridique
) {}
