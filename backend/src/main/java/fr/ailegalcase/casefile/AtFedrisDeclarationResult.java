package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-207-04 : résultat brut produit par {@link AtFedrisDeclarationCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en
 * JSON dans la colonne {@code at_fedris_declaration_analyses.result_data}.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code verdict} — l'un des 5 codes énumérés :
 *     <ul>
 *       <li>{@link Verdict#DELAI_OUVERT} (prospectif, joursRestants &gt; 2)</li>
 *       <li>{@link Verdict#DELAI_IMMINENT} (prospectif, 0 ≤ joursRestants ≤ 2)</li>
 *       <li>{@link Verdict#DELAI_DEPASSE} (prospectif, joursRestants &lt; 0)</li>
 *       <li>{@link Verdict#DECLARATION_DANS_LES_TEMPS} (rétrospectif,
 *           dateDeclarationEffectuee ≤ dateLimite)</li>
 *       <li>{@link Verdict#DECLARATION_HORS_DELAI} (rétrospectif,
 *           dateDeclarationEffectuee &gt; dateLimite)</li>
 *     </ul>
 *   </li>
 *   <li>{@code dateLimiteDeclaration} — date butoir =
 *       {@code (dateConnaissanceEmployeur ?? dateAccident) + 8 jours}.</li>
 *   <li>{@code joursRestants} — mode prospectif :
 *       {@code dateLimite - dateActionEnvisagee} (négatif si dépassé).
 *       Mode rétrospectif : {@code dateLimite - dateDeclarationEffectuee}
 *       (positif si dans les temps, négatif = jours de retard).</li>
 *   <li>{@code regleAppliquee} — code synthétique de la règle appliquée
 *       (constante {@link AtFedrisDeclarationCalculator#REGLE_8_JOURS_LOI_1971_ART_62}).</li>
 *   <li>{@code baseJuridique} — texte unique citant Loi 1971 art. 62 +
 *       AR 21/12/1971 art. 25.</li>
 *   <li>{@code formuleCalcul} — explication textuelle du calcul (transparence
 *       pour l'avocat).</li>
 *   <li>{@code consequencesNonRespect} — phrase rappelant les conséquences
 *       du dépassement du délai (préjudice salarié + intérêts moratoires +
 *       responsabilité civile employeur).</li>
 *   <li>{@code dateAccident}, {@code dateConnaissanceEmployeur},
 *       {@code dateActionEnvisagee}, {@code dateDeclarationEffectuee} —
 *       inputs normalisés (persistés pour pré-fill GET).</li>
 * </ul>
 */
public record AtFedrisDeclarationResult(
        // Inputs normalisés (persistés pour survie au reload)
        LocalDate dateAccident,
        LocalDate dateConnaissanceEmployeur,
        LocalDate dateActionEnvisagee,
        LocalDate dateDeclarationEffectuee,
        // Outputs calculés
        String verdict,
        LocalDate dateLimiteDeclaration,
        long joursRestants,
        String regleAppliquee,
        String baseJuridique,
        String formuleCalcul,
        String consequencesNonRespect
) {

    /**
     * Enum des verdicts possibles. Exposé en {@code String} dans le record
     * pour rester compatible Jackson sans configuration spéciale (cohérent
     * avec {@link PrescriptionBeLitigeTravailResult}).
     */
    public enum Verdict {
        /** Mode prospectif — délai encore confortablement ouvert (&gt; 2 jours restants). */
        DELAI_OUVERT,
        /** Mode prospectif — délai imminent (0 ≤ joursRestants ≤ 2). */
        DELAI_IMMINENT,
        /** Mode prospectif — délai dépassé (joursRestants &lt; 0). */
        DELAI_DEPASSE,
        /** Mode rétrospectif — déclaration transmise dans les délais. */
        DECLARATION_DANS_LES_TEMPS,
        /** Mode rétrospectif — déclaration transmise hors délai. */
        DECLARATION_HORS_DELAI
    }
}
