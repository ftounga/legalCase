package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-216-21 : body POST /api/v1/case-files/{id}/recel-succession.
 *
 * <p>Outil single-country FRANCE — art. 778 Cciv (recel successoral +
 * sanction civile : privation sur le bien recelé + obligation de rapporter
 * sans émolument).</p>
 *
 * <p>Le service rejette : absence de {@code typeRecel}, absence de
 * {@code dateOuvertureSuccession}, date d'ouverture future,
 * country != FRANCE, valeurs négatives.</p>
 *
 * @param typeRecel                  type de recel envisagé (cause invoquée). Requis.
 * @param bienCeleValeurEur          valeur en euros du bien / de la créance
 *                                   recelé(e) (optionnel).
 * @param preuveRecel                nature de la preuve dont disposent les
 *                                   cohéritiers demandeurs. Requis.
 * @param receleurQualite            qualité du receleur envisagé (héritier /
 *                                   légataire / donataire / tiers).
 * @param dateOuvertureSuccession    date d'ouverture de la succession
 *                                   (point de départ du délai 5 ans). Requis.
 * @param actionIntentee             true si une action en recel a déjà
 *                                   été engagée par les cohéritiers
 *                                   (optionnel).
 */
public record RecelSuccessionRequest(
        TypeRecelEnum typeRecel,
        Integer bienCeleValeurEur,
        PreuveRecelEnum preuveRecel,
        ReceleurQualiteEnum receleurQualite,
        LocalDate dateOuvertureSuccession,
        Boolean actionIntentee
) {}
