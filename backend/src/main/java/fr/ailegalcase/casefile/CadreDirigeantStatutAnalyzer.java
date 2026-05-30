package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-218-19 : analyseur de qualification du <b>cadre dirigeant</b> au sens de
 * l'art. L.3111-2 du code du travail. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>3 critères cumulatifs</b> (art. L.3111-2 CT) :
 *     <ol>
 *       <li>responsabilités impliquant une grande indépendance dans
 *           l'organisation de son emploi du temps ;</li>
 *       <li>habilitation à prendre des décisions de façon largement
 *           autonome ;</li>
 *       <li>rémunération se situant dans les niveaux les plus élevés des
 *           systèmes de rémunération de l'entreprise.</li>
 *     </ol>
 *     Le champ {@code criteresRemplis} (0..3) compte les critères satisfaits.</li>
 *   <li><b>Indice complémentaire</b> : la jurisprudence (Cass. soc. post-2012)
 *       exige une <b>participation effective à la direction de l'entreprise</b>.
 *       Son absence fragilise la qualification même lorsque les 3 critères
 *       légaux sont réunis, mais l'absence des 3 critères suffit à écarter le
 *       statut.</li>
 *   <li><b>Verdict de qualification</b> :
 *     <ul>
 *       <li>3 critères + participation effective → {@code CADRE_DIRIGEANT} ;</li>
 *       <li>3 critères sans participation effective →
 *           {@code CADRE_DIRIGEANT_FRAGILE} ;</li>
 *       <li>&lt; 3 critères → {@code NON_CADRE_DIRIGEANT}.</li>
 *     </ul></li>
 *   <li><b>Conséquence</b> : {@code exclusionDureeTravail} = {@code EXCLU} pour
 *       les deux qualifications de cadre dirigeant (le statut emporte exclusion
 *       des règles de durée du travail), {@code NON_EXCLU} sinon.</li>
 *   <li><b>Risque de rappel d'heures supplémentaires</b> : {@code FAIBLE} si
 *       {@code CADRE_DIRIGEANT}, {@code MODERE} si {@code CADRE_DIRIGEANT_FRAGILE},
 *       {@code ELEVE} si {@code NON_CADRE_DIRIGEANT}.</li>
 * </ul>
 *
 * <p>Hors périmètre : chiffrage du rappel d'heures supplémentaires (calculateur
 * dédié), forfait-jours (F-DT-50), cadre intégré / autonome (catégories hors
 * L.3111-2). Base juridique annotée « à vérifier par avocat ».
 */
public final class CadreDirigeantStatutAnalyzer {

    static final String CRITERE_1 =
            "Responsabilités impliquant une grande indépendance dans l'organisation de l'emploi du temps (art. L.3111-2 CT)";
    static final String CRITERE_2 =
            "Habilitation à prendre des décisions de façon largement autonome (art. L.3111-2 CT)";
    static final String CRITERE_3 =
            "Rémunération se situant dans les niveaux les plus élevés des systèmes de rémunération de l'entreprise (art. L.3111-2 CT)";

    static final String BASE_JURIDIQUE =
            "art. L.3111-2 du code du travail — cadre dirigeant : trois critères "
                    + "cumulatifs (grande indépendance dans l'organisation de l'emploi "
                    + "du temps ; habilitation à prendre des décisions de façon largement "
                    + "autonome ; rémunération dans les niveaux les plus élevés de "
                    + "l'entreprise) ; exclusion des dispositions sur la durée du travail, "
                    + "le repos et les jours fériés (sauf congés payés). Cass. soc. — "
                    + "exigence d'une participation effective à la direction de "
                    + "l'entreprise (jurisprudence restrictive post-2012). Qualification "
                    + "et conséquences à vérifier par l'avocat (à vérifier par avocat)";

    private CadreDirigeantStatutAnalyzer() {
    }

    /**
     * Analyse la qualification de cadre dirigeant : évalue les 3 critères
     * cumulatifs de l'art. L.3111-2 CT, compte les critères remplis, applique
     * l'exigence jurisprudentielle de participation effective à la direction,
     * puis détermine la qualification, l'exclusion des règles de durée du travail
     * et le risque de rappel d'heures supplémentaires.
     *
     * @param independanceEmploiDuTemps critère 1 (requis).
     * @param autonomieDecision critère 2 (requis).
     * @param remunerationParmiPlusElevees critère 3 (requis).
     * @param participationDirectionEntreprise indice complémentaire (défaut false).
     * @param niveauRemunerationConstate rémunération mensuelle constatée (optionnel, ≥ 0).
     */
    public static CadreDirigeantStatutResult analyze(Boolean independanceEmploiDuTemps,
                                                     Boolean autonomieDecision,
                                                     Boolean remunerationParmiPlusElevees,
                                                     Boolean participationDirectionEntreprise,
                                                     BigDecimal niveauRemunerationConstate) {
        validate(independanceEmploiDuTemps, autonomieDecision, remunerationParmiPlusElevees,
                niveauRemunerationConstate);

        boolean c1 = independanceEmploiDuTemps;
        boolean c2 = autonomieDecision;
        boolean c3 = remunerationParmiPlusElevees;
        boolean participation = participationDirectionEntreprise != null && participationDirectionEntreprise;

        List<CadreDirigeantCritere> criteres = List.of(
                new CadreDirigeantCritere(CRITERE_1, c1, commentaireCritere(c1,
                        "le salarié organise librement son emploi du temps",
                        "l'organisation de l'emploi du temps reste contrainte / encadrée")),
                new CadreDirigeantCritere(CRITERE_2, c2, commentaireCritere(c2,
                        "le salarié décide de façon largement autonome",
                        "le pouvoir de décision autonome n'est pas établi")),
                new CadreDirigeantCritere(CRITERE_3, c3, commentaireCritere(c3,
                        "la rémunération figure parmi les plus élevées de l'entreprise",
                        "la rémunération ne figure pas parmi les plus élevées de l'entreprise")));

        int criteresRemplis = (c1 ? 1 : 0) + (c2 ? 1 : 0) + (c3 ? 1 : 0);
        boolean tousCriteres = criteresRemplis == 3;

        CadreDirigeantQualification qualification;
        CadreDirigeantExclusionDureeTravail exclusion;
        CadreDirigeantRisqueRappelHeuresSupp risque;
        if (tousCriteres && participation) {
            qualification = CadreDirigeantQualification.CADRE_DIRIGEANT;
            exclusion = CadreDirigeantExclusionDureeTravail.EXCLU;
            risque = CadreDirigeantRisqueRappelHeuresSupp.FAIBLE;
        } else if (tousCriteres) {
            qualification = CadreDirigeantQualification.CADRE_DIRIGEANT_FRAGILE;
            exclusion = CadreDirigeantExclusionDureeTravail.EXCLU;
            risque = CadreDirigeantRisqueRappelHeuresSupp.MODERE;
        } else {
            qualification = CadreDirigeantQualification.NON_CADRE_DIRIGEANT;
            exclusion = CadreDirigeantExclusionDureeTravail.NON_EXCLU;
            risque = CadreDirigeantRisqueRappelHeuresSupp.ELEVE;
        }

        return new CadreDirigeantStatutResult(
                c1, c2, c3, participation,
                niveauRemunerationConstate,
                criteres,
                criteresRemplis,
                qualification,
                exclusion,
                risque,
                BASE_JURIDIQUE);
    }

    private static String commentaireCritere(boolean rempli, String siRempli, String siNon) {
        return (rempli ? "Critère rempli : " : "Critère non rempli : ") + (rempli ? siRempli : siNon) + ".";
    }

    private static void validate(Boolean independanceEmploiDuTemps,
                                 Boolean autonomieDecision,
                                 Boolean remunerationParmiPlusElevees,
                                 BigDecimal niveauRemunerationConstate) {
        if (independanceEmploiDuTemps == null) {
            throw new IllegalArgumentException("independanceEmploiDuTemps est requis");
        }
        if (autonomieDecision == null) {
            throw new IllegalArgumentException("autonomieDecision est requis");
        }
        if (remunerationParmiPlusElevees == null) {
            throw new IllegalArgumentException("remunerationParmiPlusElevees est requis");
        }
        if (niveauRemunerationConstate != null
                && niveauRemunerationConstate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("niveauRemunerationConstate ne peut pas être négatif");
        }
    }
}
