package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-207-01 : résultat brut produit par {@link PrescriptionBeLitigeTravailCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en JSON
 * dans la colonne {@code prescription_be_litige_travail_analyses.result_data}.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code verdict} — {@code PRESCRIT} | {@code IMMINENT} | {@code NON_PRESCRIT}.</li>
 *   <li>{@code dateLimitePrescription} — date butoir (point de départ + délai).</li>
 *   <li>{@code joursRestants} — différence en jours pleins entre la date limite
 *       et la date d'action envisagée (peut être négatif si déjà prescrit).</li>
 *   <li>{@code regleAppliquee} — code synthétique de la règle appliquée.</li>
 *   <li>{@code baseJuridique} — texte unique citant les bases juridiques (Loi
 *       03/07/1978 art. 15 ; CCT 109 art. 11 le cas échéant).</li>
 *   <li>{@code formuleCalcul} — explication textuelle du calcul (transparence
 *       pour l'avocat).</li>
 *   <li>{@code dateRupture}, {@code typeCreance}, {@code dateActionEnvisagee} —
 *       inputs normalisés persistés pour pré-fill GET.</li>
 * </ul>
 */
public record PrescriptionBeLitigeTravailResult(
        // Inputs normalisés (persistés pour survie au reload)
        LocalDate dateRupture,
        String typeCreance,
        LocalDate dateActionEnvisagee,
        // Outputs calculés
        String verdict,
        LocalDate dateLimitePrescription,
        long joursRestants,
        String regleAppliquee,
        String baseJuridique,
        String formuleCalcul
) {}
