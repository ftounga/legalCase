package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-25-01 : requête de calcul d'éligibilité à une mesure de protection
 * des majeurs (art. 433-441 + 494-1 et s. Cciv).
 *
 * <p>SF-FA-25-03 : ajout du champ optionnel
 * {@code incapaciteGestionQuotidienne} (art. 472 Cciv) qui distingue la
 * curatelle renforcée de la curatelle simple. Backward compatible : un POST
 * sans ce champ reste valide (default false).
 *
 * <p>SF-FA-25-04 : ajout du champ optionnel {@code altertationGrave}
 * (art. 440 al. 3 Cciv) qui distingue la tutelle (altération grave et
 * durable empêchant la personne de pourvoir seule à ses intérêts) de la
 * curatelle. Backward compatible : un POST sans ce champ reste valide
 * (default false).
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
        Boolean isolementSocial,
        Boolean incapaciteGestionQuotidienne,
        Boolean altertationGrave
) {}
