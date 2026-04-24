package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Belgian9terResponse(
        UUID caseFileId,
        LocalDate dateDebutSymptomes,
        boolean maladieGraveCertifiee,
        boolean soinsNecessairesDisponiblesBe,
        boolean soinsInaccessiblesPaysOrigine,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        String country,
        boolean certificatMedicalType1Ok,
        boolean soinsRequisOk,
        boolean inaccessibiliteOk,
        boolean pasMenace,
        int scoreGlobal,
        String verdictProbabiliteAcceptation,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstruction,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
