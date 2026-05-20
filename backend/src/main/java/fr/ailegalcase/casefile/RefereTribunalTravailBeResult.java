package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-207-05 : résultat brut produit par {@link RefereTribunalTravailBeCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en
 * JSON dans la colonne {@code refere_tribunal_travail_be_analyses.result_data}.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code verdict} — {@link Verdict#REFERE_ELIGIBLE} (score = 5),
 *       {@link Verdict#REFERE_INCERTAIN} (score 3-4) ou
 *       {@link Verdict#REFERE_NON_ELIGIBLE} (score ≤ 2).</li>
 *   <li>{@code conditionsNonRemplies} — sous-ensemble des 5 conditions
 *       (cf. {@link RefereTribunalTravailBeCondition}) NON satisfaites,
 *       préservant l'ordre déclaratif de l'enum.</li>
 *   <li>{@code scoreConditions} — entier 0-5 (nb de conditions remplies).</li>
 *   <li>{@code requeteSquelette} — template texte de la requête en référé
 *       (non null si {@code REFERE_ELIGIBLE} ou {@code REFERE_INCERTAIN} ;
 *       null si {@code REFERE_NON_ELIGIBLE}).</li>
 *   <li>{@code baseJuridique} — citation des bases juridiques BE applicables.</li>
 *   <li>{@code etapeSuivante} — orientation procédurale ({@link EtapeSuivante}).</li>
 *   <li>Champs <i>input</i> normalisés persistés pour pré-fill GET.</li>
 * </ul>
 */
public record RefereTribunalTravailBeResult(
        // Inputs normalisés (persistés pour survie au reload)
        RefereTribunalTravailBeMotifUrgence motifUrgence,
        String motifUrgenceDescription,
        LocalDate dateFaitGenerateur,
        LocalDate dateDemarcheAmiable,
        boolean preuveUrgenceJointe,
        String mesureProvisoireDemandee,
        boolean perilEnDemeure,
        boolean competenceTerritorialeIdentifiee,
        // Outputs calculés
        Verdict verdict,
        List<RefereTribunalTravailBeCondition> conditionsNonRemplies,
        int scoreConditions,
        String requeteSquelette,
        String baseJuridique,
        EtapeSuivante etapeSuivante
) {

    /**
     * Verdict d'éligibilité au référé devant le président du tribunal du
     * travail BE.
     *
     * <p>Lecture du score (sur 5) :
     * <ul>
     *   <li>5 → {@link #REFERE_ELIGIBLE} (toutes conditions cumulatives) ;</li>
     *   <li>3-4 → {@link #REFERE_INCERTAIN} (dossier renforçable) ;</li>
     *   <li>≤ 2 → {@link #REFERE_NON_ELIGIBLE} (procédure au fond
     *       recommandée).</li>
     * </ul></p>
     */
    public enum Verdict {
        REFERE_ELIGIBLE,
        REFERE_INCERTAIN,
        REFERE_NON_ELIGIBLE
    }

    /**
     * Orientation procédurale recommandée à l'avocat selon le verdict.
     *
     * <ul>
     *   <li>{@link #DEPOSER_REQUETE} si {@code REFERE_ELIGIBLE} — utiliser
     *       le squelette généré et déposer la requête au greffe du tribunal
     *       du travail compétent.</li>
     *   <li>{@link #RENFORCER_DOSSIER} si {@code REFERE_INCERTAIN} — combler
     *       les conditions manquantes avant de déposer (pièces, mise en
     *       demeure, précision de la mesure).</li>
     *   <li>{@link #ALTERNATIVE_PROCEDURE_FOND} si {@code REFERE_NON_ELIGIBLE}
     *       — basculer sur la procédure au fond (citation devant le tribunal
     *       du travail, voie ordinaire).</li>
     * </ul>
     */
    public enum EtapeSuivante {
        DEPOSER_REQUETE,
        RENFORCER_DOSSIER,
        ALTERNATIVE_PROCEDURE_FOND
    }
}
