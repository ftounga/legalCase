package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-03 : résultat brut produit par
 * {@link LicenciementBeStatutUniquePreavisCalculator}.
 *
 * <p>Fonction pure — n'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link LicenciementBeStatutUniquePreavisService} dans
 * {@link LicenciementBeStatutUniquePreavisResponse}).</p>
 */
public record LicenciementBeStatutUniquePreavisResult(
        int ancienneteAnnees,
        int ancienneteMoisSupplementaires,
        BigDecimal salaireHebdomadaireBrut,
        LocalDate dateNotificationLicenciement,
        boolean partieStatutUniqueSeulement,
        int dureePreavisEnSemaines,
        LocalDate dateFinPreavis,
        BigDecimal indemniteCompensatoire,
        String formuleCalcul,
        String baseJuridique,
        String avertissement
) {}
