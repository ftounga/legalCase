package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-25-01 : réponse de l'API mesures de protection des majeurs
 * (art. 433-441 + 494-1 et s. Cciv).
 *
 * <p>SF-FA-25-03 : ajout des champs {@code incapaciteGestionQuotidienne}
 * (art. 472), {@code eligible} et {@code criteresNonRemplis} pour les
 * régimes curatelle simple/renforcée.
 *
 * <p>SF-FA-25-04 : ajout du champ {@code altertationGrave} (art. 440 al. 3)
 * pour le régime tutelle.
 */
public record MajeursProtegesResponse(
        UUID caseFileId,
        String regimeProtectionDemande,
        boolean altertationFacultesMentales,
        boolean altertationFacultesPhysiques,
        boolean certificatMedicalCirconstancie,
        LocalDate dateCertificatMedical,
        boolean consentementPersonneAProteger,
        String demandeurFamilial,
        List<String> actesEnvisages,
        boolean urgencePatrimoniale,
        boolean patrimoineSignificatif,
        boolean isolementSocial,
        boolean incapaciteGestionQuotidienne,
        boolean altertationGrave,
        int scoreEligibilite,
        String regimeOptimalRecommande,
        String verdictAcceptabiliteJaf,
        int delaiProcedureMoisPrevisionnel,
        boolean auditionPersonneObligatoire,
        boolean expertisePsyComplementaireRecommandee,
        boolean eligible,
        List<String> criteresNonRemplis,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
