package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-216-19 : body POST /api/v1/case-files/{id}/indignite-successorale.
 *
 * <p>Outil single-country FRANCE — art. 726-729-1 Cciv (déchéance des droits
 * successoraux d'un héritier pour crime / délit envers le défunt).</p>
 *
 * <p>Le service rejette : valeurs négatives, absence de {@code motifIndignite},
 * absence de {@code dateOuvertureSuccession}, date d'ouverture future,
 * country != FRANCE.</p>
 *
 * @param motifIndignite             motif de l'indignité (cause invoquée). Requis.
 * @param condamnationPrononcee      true si une condamnation pénale définitive
 *                                   a été prononcée. Requis.
 * @param indigniteJudiciaireDemandee true si les autres héritiers / le ministère
 *                                   public ont engagé une demande d'indignité
 *                                   judiciaire devant le TJ.
 * @param pardonTestamentaireDetecte true si le défunt a explicitement pardonné
 *                                   l'héritier indigne dans son testament
 *                                   (art. 728 Cciv).
 * @param dateOuvertureSuccession    date d'ouverture de la succession
 *                                   (point de départ du délai 5 ans, art. 729 Cciv).
 *                                   Requis.
 * @param nbCoheritiersRestants      nombre de cohéritiers restants après
 *                                   exclusion éventuelle de l'indigne
 *                                   (optionnel, utilisé pour l'effet sur la
 *                                   dévolution).
 */
public record IndigniteSuccessoraleRequest(
        MotifIndigniteEnum motifIndignite,
        Boolean condamnationPrononcee,
        Boolean indigniteJudiciaireDemandee,
        Boolean pardonTestamentaireDetecte,
        LocalDate dateOuvertureSuccession,
        Integer nbCoheritiersRestants
) {}
