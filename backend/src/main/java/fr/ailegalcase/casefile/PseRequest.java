package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-14-01 : requête d'analyse de validité d'un Plan de Sauvegarde de l'Emploi (PSE).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record PseRequest(
        Integer tailleEntrepriseSalaries,
        Integer nombreLicenciementsEnvisages,
        Integer periodeJours,
        PseCalculator.ModeAdoption modeAdoption,
        PseCalculator.AvisCse csaeConsulteAvis,
        PseCalculator.StatutDreets dreetsStatut,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateNotificationDreets,
        List<PseCalculator.ContenuMesure> contenuMesures,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateProjet
) {}
