package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-17 : résultat du calcul Adoption internationale FR (art. 370-3 à
 * 370-5 Cciv + Convention La Haye 1993).
 *
 * <ul>
 *   <li>{@code conditionsRemplies} — true si l'agrément est présent et
 *       qu'aucun verdict bloquant n'est posé.</li>
 *   <li>{@code voieProcedure} — voie procédurale demandée / déterminée.</li>
 *   <li>{@code conventionApplicable} — true si le pays d'origine est
 *       signataire de la Convention La Haye 1993.</li>
 *   <li>{@code alerteKafala} — true si pays d'origine = Maroc / Algérie /
 *       Tunisie (kafala ≠ adoption en droit FR, art. 370-3 al. 3 Cciv).</li>
 *   <li>{@code exequaturRequis} — true si une décision étrangère doit
 *       passer par un exequatur TJ (art. 370-5 Cciv).</li>
 *   <li>{@code delaiEstime} — fourchette d'estimation des délais
 *       procéduraux (texte libre).</li>
 *   <li>{@code verdict} — code du verdict ("AGREMENT_REQUIS",
 *       "KAFALA_INCOMPATIBLE", "OAA_CONVENTION", "VOIE_AUTONOME_RISQUE",
 *       "EXEQUATUR_REQUIS", "OK").</li>
 *   <li>{@code baseLegale} — articles Cciv et Convention applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record AdoptionInternationaleResult(
        boolean conditionsRemplies,
        VoieProcedureAdoptionEnum voieProcedure,
        boolean conventionApplicable,
        boolean alerteKafala,
        boolean exequaturRequis,
        String delaiEstime,
        String verdict,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
