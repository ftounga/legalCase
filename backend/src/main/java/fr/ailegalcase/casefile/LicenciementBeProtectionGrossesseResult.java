package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-05 : résultat brut produit par
 * {@link LicenciementBeProtectionGrossesseValidator}.
 *
 * <p>Fonction pure — n'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link LicenciementBeProtectionGrossesseService} dans
 * {@link LicenciementBeProtectionGrossesseResponse}).</p>
 *
 * <p>Le champ {@code indemniteForfaitaire} est {@code null} pour le verdict
 * {@link LicenciementBeProtectionGrossesseVerdict#HORS_PERIODE_PROTECTION}
 * (pas d'indemnité due) ; il vaut {@code remunerationMensuelleBrute × 6}
 * (arrondi HALF_UP 2 décimales) pour les 3 autres verdicts.</p>
 *
 * <p>Le champ {@code dateFinProtection} peut être {@code null} si ni
 * {@code dateAccouchement} ni {@code dateCongeMaterniteFinale} ne sont
 * renseignées dans la requête — dans ce cas, le champ {@code avertissement}
 * porte un message explicite et la présomption de protection est ouverte
 * (la fenêtre est traitée comme infinie côté droite).</p>
 */
public record LicenciementBeProtectionGrossesseResult(

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateDebutGrossesse,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateAccouchement,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateCongeMaterniteDebut,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateCongeMaterniteFinale,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateLicenciement,

        boolean grossesseNotifieeParEcrit,
        BigDecimal remunerationMensuelleBrute,
        String motifInvoqueParEmployeur,

        LicenciementBeProtectionGrossesseVerdict verdict,
        boolean licenciementDansLaPeriodeProtegee,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateDebutProtection,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateFinProtection,

        BigDecimal indemniteForfaitaire,
        boolean chargePreuveEmployeur,
        String baseJuridique,
        String formuleCalcul,
        String avertissement
) {}
