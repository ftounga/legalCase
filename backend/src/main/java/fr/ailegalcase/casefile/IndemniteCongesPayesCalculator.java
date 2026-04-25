package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-DT-26-01 : calcul de l'indemnité compensatrice de congés payés
 * (art. L.3141-26 et L.3141-28 Code du travail).
 *
 * <p>Compare les deux méthodes légales et retient la plus favorable au salarié
 * (jurisprudence constante), sauf si l'avocat force explicitement une méthode.</p>
 *
 * <ul>
 *   <li>Méthode du dixième : 10 % × rémunération totale brute de la période de référence
 *       (art. L.3141-24 I).</li>
 *   <li>Méthode du maintien : (salaire_mensuel / 30) × jours dus
 *       (méthode du trentième jours calendaires, art. L.3141-24 II).</li>
 * </ul>
 */
public final class IndemniteCongesPayesCalculator {

    private static final BigDecimal DIX_POURCENT = new BigDecimal("0.10");
    private static final BigDecimal TRENTE = new BigDecimal("30");
    private static final String BASE_JURIDIQUE = "Art. L.3141-26 et L.3141-28 Code du travail";

    private IndemniteCongesPayesCalculator() {}

    public static IndemniteCongesPayesResult compute(BigDecimal totalRemunerationPeriodeEur,
                                                     Integer joursAcquisAnnee,
                                                     Integer joursPris,
                                                     BigDecimal salaireMensuelBrutEur,
                                                     LocalDate dateRupture,
                                                     IndemniteCongesPayesMethode methodeForcee) {
        if (totalRemunerationPeriodeEur == null || totalRemunerationPeriodeEur.signum() <= 0) {
            throw new IllegalArgumentException(
                    "totalRemunerationPeriodeEur requis et strictement positif");
        }
        if (salaireMensuelBrutEur == null || salaireMensuelBrutEur.signum() <= 0) {
            throw new IllegalArgumentException(
                    "salaireMensuelBrutEur requis et strictement positif");
        }
        if (joursAcquisAnnee == null || joursAcquisAnnee < 0) {
            throw new IllegalArgumentException(
                    "joursAcquisAnnee requis et >= 0");
        }
        if (joursPris == null || joursPris < 0) {
            throw new IllegalArgumentException(
                    "joursPris requis et >= 0");
        }
        if (joursPris > joursAcquisAnnee) {
            throw new IllegalArgumentException(
                    "joursPris ne peut exceder joursAcquisAnnee (incoherence)");
        }

        BigDecimal totalRounded = totalRemunerationPeriodeEur.setScale(2, RoundingMode.HALF_UP);
        BigDecimal salaireRounded = salaireMensuelBrutEur.setScale(2, RoundingMode.HALF_UP);
        int joursDus = joursAcquisAnnee - joursPris;

        BigDecimal montantDixPourcent = totalRounded
                .multiply(DIX_POURCENT)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montantMaintien = salaireRounded
                .divide(TRENTE, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(joursDus))
                .setScale(2, RoundingMode.HALF_UP);

        IndemniteCongesPayesMethode methodeRetenue;
        List<String> messages = new ArrayList<>();
        if (methodeForcee != null) {
            methodeRetenue = methodeForcee;
            messages.add("Methode forcee par l'avocat : "
                    + (methodeForcee == IndemniteCongesPayesMethode.DIX_POURCENT
                            ? "regle du dixieme (art. L.3141-24 I)."
                            : "maintien de salaire (art. L.3141-24 II)."));
        } else {
            int comparison = montantDixPourcent.compareTo(montantMaintien);
            if (comparison >= 0) {
                methodeRetenue = IndemniteCongesPayesMethode.DIX_POURCENT;
            } else {
                methodeRetenue = IndemniteCongesPayesMethode.MAINTIEN;
            }
            messages.add("Methode la plus favorable au salarie retenue (art. L.3141-28).");
        }

        BigDecimal montantIndemnite = methodeRetenue == IndemniteCongesPayesMethode.DIX_POURCENT
                ? montantDixPourcent
                : montantMaintien;

        if (joursDus == 0) {
            messages.add("Aucun jour de conge du (jours acquis = jours pris).");
        }

        String formule = String.format(
                "10 %% x %s EUR = %s EUR (vs maintien %d j x %s / 30 = %s EUR) -> methode retenue : %s",
                formatMontant(totalRounded),
                formatMontant(montantDixPourcent),
                joursDus,
                formatMontant(salaireRounded),
                formatMontant(montantMaintien),
                methodeRetenue == IndemniteCongesPayesMethode.DIX_POURCENT
                        ? "regle du dixieme"
                        : "maintien de salaire");

        return new IndemniteCongesPayesResult(
                totalRounded,
                joursAcquisAnnee,
                joursPris,
                salaireRounded,
                dateRupture,
                methodeForcee,
                joursDus,
                montantDixPourcent,
                montantMaintien,
                methodeRetenue,
                montantIndemnite,
                BASE_JURIDIQUE,
                formule,
                List.copyOf(messages)
        );
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
