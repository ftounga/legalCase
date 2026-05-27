package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * SF-213-10 : payload de création / mise à jour de l'analyse CCT n° 109
 * (score d'indemnisation du licenciement manifestement déraisonnable — outil
 * BELGIQUE uniquement, fondement : art. 9 CCT 12/02/2014).
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code motifCommunique} (Boolean) — le motif a-t-il été communiqué
 *       par écrit au salarié (mécanisme CCT 109 art. 5–8) ?</li>
 *   <li>{@code motifLieAPersonne} — qualification du motif (cf.
 *       {@link LicenciementBeCct109MotifLieAPersonneEnum}).</li>
 *   <li>{@code remunerationHebdomadaireBrute} — base du calcul de
 *       l'indemnité (semaines × salaire hebdo). Doit être &gt; 0.</li>
 * </ul>
 *
 * <h2>Conditionnels / défauts</h2>
 * <ul>
 *   <li>{@code discriminationSuspectee} — défaut {@code false}.</li>
 *   <li>{@code represaillesSuspectees} — défaut {@code false}.</li>
 *   <li>{@code proceduresRespectees} — défaut {@code true} (procédures
 *       d'audition / notification respectées).</li>
 *   <li>{@code argumentsPatronal} — optionnel (contexte IA, ne change pas
 *       le calcul).</li>
 * </ul>
 */
public record LicenciementBeCct109DeraisonnableRequest(

        @NotNull(message = "motifCommunique est requis")
        Boolean motifCommunique,

        @NotNull(message = "motifLieAPersonne est requis")
        LicenciementBeCct109MotifLieAPersonneEnum motifLieAPersonne,

        boolean discriminationSuspectee,

        boolean represaillesSuspectees,

        boolean proceduresRespectees,

        @NotNull(message = "remunerationHebdomadaireBrute est requise")
        BigDecimal remunerationHebdomadaireBrute,

        String argumentsPatronal
) {
}
