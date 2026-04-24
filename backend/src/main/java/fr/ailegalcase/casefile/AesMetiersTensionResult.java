package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-09-01 : résultat de l'analyse AES métier en tension (art. L.435-4 CESEDA).
 * Outil single-country FR — pas d'équivalent BE standard.
 */
public record AesMetiersTensionResult(
        LocalDate dateEntreeFrance,
        int moisActiviteSalarieeDernieres24Mois,
        boolean metierEstEnTension,
        String codeMetier,
        boolean menaceOrdrePublic,
        boolean contratOuPromesseValide,
        LocalDate dateDepotDemande,
        boolean presence3Ans,
        boolean activite12MoisOk,
        boolean conditionsReunies,
        List<String> criteresNonRemplis,
        LocalDate dateEligibiliteAtteinte,
        LocalDate dateExpirationInstructionSiDemande,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
