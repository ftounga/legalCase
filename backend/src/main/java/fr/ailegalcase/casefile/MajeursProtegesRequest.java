package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-25-01 : requête de calcul d'éligibilité à une mesure de protection
 * des majeurs (art. 433-441 + 494-1 et s. Cciv).
 */
public record MajeursProtegesRequest(
        String regimeProtectionDemande,
        Boolean altertationFacultesMentales,
        Boolean altertationFacultesPhysiques,
        Boolean certificatMedicalCirconstancie,
        LocalDate dateCertificatMedical,
        Boolean consentementPersonneAProteger,
        String demandeurFamilial,
        List<String> actesEnvisages,
        Boolean urgencePatrimoniale,
        Boolean patrimoineSignificatif,
        Boolean isolementSocial
) {}
