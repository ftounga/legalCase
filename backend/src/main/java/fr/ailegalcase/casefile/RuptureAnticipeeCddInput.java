package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-17 : données d'entrée du calculateur d'analyse de la rupture anticipée
 * d'un CDD (F-DT-43-rupture-anticipee-cdd, FRANCE — fondements :
 * L. 1243-1 CT — motifs autorisés de rupture anticipée (accord des parties,
 * faute grave, force majeure, inaptitude médicale) ;
 * L. 1243-2 CT — rupture anticipée pour embauche en CDI ailleurs ;
 * L. 1243-3 CT — rupture par accord des parties ;
 * L. 1243-4 CT — sanctions de la rupture irrégulière par l'employeur
 *   (indemnité au moins égale aux salaires restants jusqu'au terme +
 *   indemnité de précarité) ;
 * jurisprudence Cass. soc. constante — sanctions de la rupture irrégulière
 * par le salarié (dommages-intérêts à l'employeur, préjudice réel).
 *
 * @param auteurRupture                       partie qui a rompu le contrat
 *                                            (EMPLOYEUR : licenciement avant
 *                                            terme ; SALARIE : démission /
 *                                            départ anticipé)
 * @param motifRupture                        motif invoqué pour la rupture
 *                                            anticipée — voir énum {@link MotifRupture}
 * @param dateTermeCdd                        date de fin normale du CDD
 *                                            (terme prévu au contrat) — base
 *                                            de calcul des mois restants
 * @param dateRupture                         date effective de la rupture
 *                                            anticipée — début du préjudice
 *                                            pour le salarié
 * @param salaireMensuelBrutEuros             salaire mensuel brut de référence
 *                                            en euros, base de l'indemnité
 *                                            "salaires restants jusqu'au terme"
 *                                            si rupture irrégulière par
 *                                            l'employeur
 * @param remunerationBruteTotaleContratEuros rémunération brute totale perçue
 *                                            depuis le début du CDD (somme
 *                                            des salaires + primes) — base de
 *                                            calcul de l'indemnité de précarité
 *                                            10 % L. 1243-8 CT
 */
public record RuptureAnticipeeCddInput(
        AuteurRupture auteurRupture,
        MotifRupture motifRupture,
        LocalDate dateTermeCdd,
        LocalDate dateRupture,
        double salaireMensuelBrutEuros,
        double remunerationBruteTotaleContratEuros
) {

    /** Auteur de la rupture anticipée. */
    public enum AuteurRupture {
        EMPLOYEUR,
        SALARIE
    }

    /**
     * Motif invoqué pour la rupture anticipée du CDD.
     *
     * <ul>
     *   <li>{@code ACCORD_PARTIES} — rupture par accord mutuel
     *       (L. 1243-1 CT, L. 1243-3 CT) — légitime des deux côtés.</li>
     *   <li>{@code FAUTE_GRAVE} — faute grave du salarié (L. 1243-1 CT) —
     *       légitime côté employeur, illégitime côté salarié (un salarié ne
     *       peut pas rompre pour faute grave employeur sans qualifier la
     *       rupture en force majeure ou autre).</li>
     *   <li>{@code FORCE_MAJEURE} — force majeure rendant impossible
     *       l'exécution (L. 1243-1 CT) — légitime des deux côtés.</li>
     *   <li>{@code INAPTITUDE} — inaptitude médicale du salarié
     *       (L. 1226-4-2 CT applicable au CDD) — légitime côté employeur
     *       avec procédure de reclassement.</li>
     *   <li>{@code CDI_EMBAUCHE} — embauche en CDI ailleurs par le salarié
     *       (L. 1243-2 CT) — légitime SEULEMENT côté salarié, avec préavis.</li>
     *   <li>{@code AUTRE} — aucun motif légal invoqué — rupture irrégulière
     *       par défaut, déclenche les sanctions L. 1243-4 CT.</li>
     * </ul>
     */
    public enum MotifRupture {
        ACCORD_PARTIES,
        FAUTE_GRAVE,
        FORCE_MAJEURE,
        INAPTITUDE,
        CDI_EMBAUCHE,
        AUTRE
    }
}
