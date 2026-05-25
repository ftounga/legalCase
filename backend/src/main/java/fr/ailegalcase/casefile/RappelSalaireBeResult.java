package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-02 : résultat brut produit par {@link RappelSalaireBeCalculator}.
 *
 * <p>Fonction pure — n'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link RappelSalaireBeService} dans {@link RappelSalaireBeResponse}).</p>
 */
public record RappelSalaireBeResult(
        BigDecimal montantBrut,
        LocalDate dateDebutPeriode,
        LocalDate dateFinPeriode,
        LocalDate dateRupture,
        LocalDate dateActionEnvisagee,
        RappelSalaireBeTypeArriereEnum typeArriere,
        BigDecimal interetsCourus,
        String tauxMoratoire,
        BigDecimal totalReclame,
        LocalDate dateLimitePrescription,
        long joursRestantsAvantPrescription,
        RappelSalaireBeStatutPrescription statutPrescription,
        String baseJuridique,
        String formuleCalcul,
        String avertissement
) {}
