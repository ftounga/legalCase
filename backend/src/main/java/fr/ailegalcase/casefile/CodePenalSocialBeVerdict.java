package fr.ailegalcase.casefile;

/**
 * SF-219-24 : verdict de l'outil <i>Code pénal social BE —
 * qualification d'infraction + niveau de sanction 1 à 4</i> (Loi du
 * 06/06/2010 introduisant le Code pénal social, M.B. 01/07/2010,
 * e.e.v. 01/07/2011).
 *
 * <h2>Échelle des sanctions à 4 niveaux (art. 101-103 C. pén. soc.)</h2>
 * <p>Le législateur a remplacé l'éparpillement des sanctions de
 * l'ancien droit social pénal par un barème unique fondé sur quatre
 * niveaux croissants de gravité, chacun assorti d'amendes
 * (administratives ET pénales pour les niveaux 2/3/4 ; uniquement
 * administratives au niveau 1) et, pour les niveaux 3-4, d'une
 * peine d'emprisonnement alternative.</p>
 *
 * <p>Les montants sont indexés sur le coefficient des amendes pénales
 * (art. 1<sup>er</sup> L. 05/03/1952, +0,80 décimes additionnels
 * jusqu'au 31/12/2024 ; +0,90 décimes additionnels depuis le
 * 01/01/2025 par AR 27/12/2024) — l'outil restitue les montants à
 * leur valeur de base, l'avocat indexant in concreto à la date des
 * faits ou de la décision.</p>
 *
 * <ul>
 *   <li>{@link #SANCTION_NIVEAU_1} — uniquement amende administrative
 *       10 € à 100 € (montant non indexé, art. 101 § 1 C. pén. soc.).
 *       Pas de peine pénale ; pas d'inscription au casier judiciaire.</li>
 *   <li>{@link #SANCTION_NIVEAU_2} — amende administrative 50 € à
 *       500 € OU amende pénale 50 € à 500 € (art. 101 § 1 + 102
 *       C. pén. soc.) au choix de l'auditorat du travail (art. 73-78
 *       C. pén. soc. — voie administrative ou pénale).</li>
 *   <li>{@link #SANCTION_NIVEAU_3} — amende administrative 100 € à
 *       1 000 € OU amende pénale 100 € à 1 000 € (art. 101 § 1 + 102
 *       C. pén. soc.). Pas d'emprisonnement au niveau 3.</li>
 *   <li>{@link #SANCTION_NIVEAU_4} — amende administrative 300 € à
 *       3 000 € OU amende pénale 600 € à 6 000 € ET / OU
 *       emprisonnement 6 mois à 3 ans (art. 101 § 1 + 102 + 103
 *       C. pén. soc.). Niveau réservé aux infractions les plus
 *       graves (travail non déclaré, faux social, accident grave
 *       non déclaré). Multiplication par le nombre de travailleurs
 *       concernés (art. 103 § 2 C. pén. soc.), plafonné à 100 × le
 *       maximum de l'amende pour une personne morale (art. 105
 *       C. pén. soc.).</li>
 *   <li>{@link #A_QUALIFIER} — type d'infraction incertain ou
 *       AUTRE_QUALIFICATION sans niveau renseigné. L'avocat doit
 *       établir la qualification précise.</li>
 * </ul>
 *
 * <h2>Récidive (art. 110 C. pén. soc.)</h2>
 * <p>Récidive dans le délai d'un an depuis la condamnation
 * antérieure définitive ou la décision administrative définitive
 * pour une infraction de même niveau : doublement du plafond de
 * l'amende. L'outil signale la majoration mais ne la chiffre pas
 * (l'indexation in concreto reste à charge de l'avocat).</p>
 *
 * <h2>Personne morale (art. 105 C. pén. soc.)</h2>
 * <p>Lorsque l'infraction est imputée à une personne morale, les
 * amendes sont multipliées par cinq (× 5). Lorsque la personne
 * physique poursuivie est l'employeur, son préposé ou son mandataire,
 * les amendes administratives / pénales sont multipliées par le
 * nombre de travailleurs concernés (art. 103 C. pén. soc.) avec un
 * plafond global de 100 × le maximum unitaire de l'amende.</p>
 */
public enum CodePenalSocialBeVerdict {
    /** Sanction administrative seule 10 € - 100 € (non indexée). */
    SANCTION_NIVEAU_1,

    /** Sanction administrative OU pénale 50 € - 500 € (indexée). */
    SANCTION_NIVEAU_2,

    /** Sanction administrative OU pénale 100 € - 1 000 € (indexée). */
    SANCTION_NIVEAU_3,

    /** Sanction administrative OU pénale 300/600 € - 3 000/6 000 € ET/OU emprisonnement 6 m - 3 ans. */
    SANCTION_NIVEAU_4,

    /** Qualification ouverte — analyse cas par cas par l'avocat. */
    A_QUALIFIER
}
