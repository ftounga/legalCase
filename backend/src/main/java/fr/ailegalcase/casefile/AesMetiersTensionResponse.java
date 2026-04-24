package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AesMetiersTensionResponse(
        UUID caseFileId,
        LocalDate dateEntreeFrance,
        int moisActiviteSalarieeDernieres24Mois,
        boolean metierEstEnTension,
        String codeMetier,
        boolean menaceOrdrePublic,
        boolean contratOuPromesseValide,
        LocalDate dateDepotDemande,
        String country,
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
