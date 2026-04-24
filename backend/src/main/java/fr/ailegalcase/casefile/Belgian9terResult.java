package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-14-02 : résultat de l'analyse régularisation 9ter médical BE
 * (art. 9ter Loi 15/12/1980). Outil single-country BELGIQUE.
 */
public record Belgian9terResult(
        LocalDate dateDebutSymptomes,
        boolean maladieGraveCertifiee,
        boolean soinsNecessairesDisponiblesBe,
        boolean soinsInaccessiblesPaysOrigine,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
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
