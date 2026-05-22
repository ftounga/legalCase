package fr.ailegalcase.casefile;

/**
 * SF-216-17 : body POST /api/v1/case-files/{id}/adoption-internationale.
 *
 * <p>Outil single-country FRANCE — art. 370-3 à 370-5 Cciv + Convention La
 * Haye du 29/5/1993 + Loi n°2001-111 du 6/2/2001 (agrément obligatoire) +
 * Loi n°2022-219 du 21/2/2022 (réforme adoption).</p>
 *
 * <p>Le service rejette : valeurs négatives, absence de
 * {@code paysOrigineEnfant} ou {@code agrement2025}, country != FRANCE.</p>
 *
 * @param paysOrigineEnfant            code ISO-3166 alpha-2 ou nom du pays
 *                                     d'origine de l'enfant (ex. "MAROC",
 *                                     "VIETNAM", "COLOMBIE"). Requis.
 * @param conventionLaHayeApplicable   true si le pays d'origine a ratifié la
 *                                     Convention La Haye 1993.
 * @param agrement2025                 true si l'adoptant détient un
 *                                     agrément valide (≤ 5 ans). Requis —
 *                                     condition sine qua non.
 * @param ageAdoptant                  âge de l'adoptant en années. Requis.
 * @param ageAdopte                    âge de l'adopté en années. Requis.
 * @param adoptantMarie                true si l'adoptant est marié.
 * @param voieProcedure                voie procédurale envisagée
 *                                     (OAA_AGREE / VOIE_AUTONOME /
 *                                     ORGANISME_ETAT).
 * @param formeAdoptionDemandee        forme d'adoption demandée
 *                                     (PLENIERE / SIMPLE).
 * @param exequaturRequis              true si une décision d'adoption a
 *                                     déjà été rendue à l'étranger et
 *                                     qu'un exequatur est nécessaire
 *                                     (art. 370-5 Cciv).
 */
public record AdoptionInternationaleRequest(
        String paysOrigineEnfant,
        Boolean conventionLaHayeApplicable,
        Boolean agrement2025,
        Integer ageAdoptant,
        Integer ageAdopte,
        Boolean adoptantMarie,
        VoieProcedureAdoptionEnum voieProcedure,
        FormeAdoptionEnum formeAdoptionDemandee,
        Boolean exequaturRequis
) {}
