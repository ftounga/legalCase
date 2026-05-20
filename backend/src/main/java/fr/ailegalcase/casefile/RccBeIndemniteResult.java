package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-207-07 : résultat brut produit par {@link RccBeIndemniteCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en
 * JSON dans la colonne {@code rcc_be_indemnite_analyses.result_data}.</p>
 *
 * <p>Le calculateur est une fonction <b>pure</b> sans verdict — la
 * juridiction se limite au calcul de l'indemnité complémentaire RCC due
 * mensuellement par l'employeur (formule CCT 17 art. 5) et de son montant
 * total jusqu'à l'âge légal de la pension.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code indemniteMensuelleAvantPlancher} — formule CCT 17 art. 5
 *       brute, plancher 0 € (jamais à charge du salarié).</li>
 *   <li>{@code indemniteMensuelleEmployeur} — indemnité finale après
 *       application du plancher sectoriel éventuel
 *       (max(indemniteAvantPlancher, planchersSectoriel)).</li>
 *   <li>{@code planchersSectorielApplique} — true si le plancher sectoriel
 *       a effectivement modifié l'indemnité (strictement supérieur).</li>
 *   <li>{@code moisRestantsJusquaPension} — nombre de mois entre
 *       {@code dateDebutRcc} et l'âge légal de la pension du salarié
 *       (plancher 0).</li>
 *   <li>{@code montantTotalEmployeur} — indemnité mensuelle × mois
 *       restants (€, 2 décimales).</li>
 *   <li>{@code dateAgeLegalPension} — date à laquelle le salarié atteint
 *       l'âge légal de la pension (dateNaissance + ageLegalPension ans).</li>
 *   <li>{@code ageLegalPensionApplique} — âge légal effectivement utilisé
 *       (default 66 si non renseigné).</li>
 *   <li>{@code baseJuridique} — texte unique citant CCT 17 art. 5 + Loi
 *       03/07/1978 + AR 03/05/2007.</li>
 *   <li>{@code formuleCalcul} — explication textuelle synthétique (transparence
 *       pour l'avocat).</li>
 *   <li>Champs <i>input</i> normalisés persistés pour pré-fill GET.</li>
 * </ul>
 */
public record RccBeIndemniteResult(
        // Inputs normalisés (persistés pour survie au reload)
        BigDecimal remunerationNetteReference,
        BigDecimal allocationOnemMensuelle,
        LocalDate dateNaissanceSalarie,
        LocalDate dateDebutRcc,
        int ageLegalPensionApplique,
        BigDecimal planchersSectoriel,
        // Outputs calculés
        BigDecimal indemniteMensuelleAvantPlancher,
        BigDecimal indemniteMensuelleEmployeur,
        boolean planchersSectorielApplique,
        LocalDate dateAgeLegalPension,
        long moisRestantsJusquaPension,
        BigDecimal montantTotalEmployeur,
        String baseJuridique,
        String formuleCalcul
) {}
