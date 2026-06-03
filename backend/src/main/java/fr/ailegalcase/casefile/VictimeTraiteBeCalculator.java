package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-221-06 — calculateur du titre de séjour « victime de la traite des êtres humains »
 * en Belgique (art. 61/2 et s. Loi du 15/12/1980 ; circulaire du 26/09/2008).
 *
 * <p>Sources <i>(à vérifier par avocat BE 2026)</i> :
 * <ul>
 *   <li>Loi du 15/12/1980 art. 61/2 et s. — titre de séjour spécifique pour les victimes
 *       de la traite des êtres humains qui coopèrent avec la justice et rompent avec le
 *       réseau, accompagnées par un centre spécialisé agréé.</li>
 *   <li>Circulaire du 26/09/2008 — procédure en 3 phases : délai de réflexion (~45 j) →
 *       titre temporaire (déclaration faite) → titre lié à l'utilité de la procédure pénale.</li>
 *   <li>Centres spécialisés agréés : PAG-ASA (Bruxelles), Sürya (Wallonie), Payoke (Flandre).</li>
 * </ul>
 *
 * <p><b>Régime BE PROPRE</b>, DISTINCT du pendant FR
 * {@code F-IM-35-victime-traite-l4251-fr} (régime L. 425-1 CESEDA). Outil
 * <b>BELGIQUE UNIQUEMENT</b> — un outil = une situation.
 */
public final class VictimeTraiteBeCalculator {

    /** Centres spécialisés agréés vers lesquels orienter la victime (référentiel BE). */
    public static final String CENTRES_SPECIALISES = "PAG-ASA (Bruxelles), Sürya (Wallonie), Payoke (Flandre)";

    private static final List<String> BASES_JURIDIQUES = List.of(
            "Loi du 15/12/1980 art. 61/2 et s. (titre de séjour victime de la traite des êtres "
                    + "humains — coopération judiciaire, rupture avec le réseau, accompagnement "
                    + "par un centre spécialisé agréé) (à vérifier par avocat)",
            "Circulaire du 26/09/2008 (procédure en 3 phases : délai de réflexion → titre "
                    + "temporaire → titre lié à l'utilité de la procédure pénale) (à vérifier par avocat)");

    private VictimeTraiteBeCalculator() {
    }

    /**
     * Calcule le verdict de l'analyse victime de la traite BE.
     *
     * @param phaseProcedure phase de la procédure (requise — voir {@link VictimeTraiteBePhase}).
     * @param ruptureAvecReseau true si la victime a rompu avec le réseau.
     * @param cooperationJudiciaire true si la victime coopère avec la justice.
     * @param accompagnementCentreSpecialise true si la victime est accompagnée par un centre agréé.
     * @param dateDebutAccompagnement date de début de l'accompagnement (nullable, non future).
     */
    public static VictimeTraiteBeResult compute(VictimeTraiteBePhase phaseProcedure,
                                                Boolean ruptureAvecReseau,
                                                Boolean cooperationJudiciaire,
                                                Boolean accompagnementCentreSpecialise,
                                                LocalDate dateDebutAccompagnement) {
        return compute(phaseProcedure, ruptureAvecReseau, cooperationJudiciaire,
                accompagnementCentreSpecialise, dateDebutAccompagnement, LocalDate.now());
    }

    /**
     * Variante testable avec date de référence injectée.
     *
     * @param today date de référence (généralement {@code LocalDate.now()}).
     */
    public static VictimeTraiteBeResult compute(VictimeTraiteBePhase phaseProcedure,
                                                Boolean ruptureAvecReseau,
                                                Boolean cooperationJudiciaire,
                                                Boolean accompagnementCentreSpecialise,
                                                LocalDate dateDebutAccompagnement,
                                                LocalDate today) {
        validate(phaseProcedure, dateDebutAccompagnement, today);

        boolean rupture = Boolean.TRUE.equals(ruptureAvecReseau);
        boolean cooperation = Boolean.TRUE.equals(cooperationJudiciaire);
        boolean accompagnement = Boolean.TRUE.equals(accompagnementCentreSpecialise);

        List<String> messages = new ArrayList<>();
        VictimeTraiteBeVerdict verdict;
        String etapeProcedure;

        boolean declarationOuPenale = phaseProcedure == VictimeTraiteBePhase.DECLARATION_FAITE
                || phaseProcedure == VictimeTraiteBePhase.PROCEDURE_PENALE_EN_COURS;

        if (phaseProcedure == VictimeTraiteBePhase.AUCUNE) {
            verdict = VictimeTraiteBeVerdict.A_ORIENTER_CENTRE;
            etapeProcedure = "Aucune démarche engagée";
            messages.add("Aucune phase de la procédure n'est engagée : orienter d'abord la "
                    + "victime vers un centre spécialisé agréé (" + CENTRES_SPECIALISES + "). "
                    + "L'accompagnement par un tel centre est un préalable au titre de séjour "
                    + "victime de la traite (art. 61/2 et s. Loi 15/12/1980 — à vérifier par avocat).");
        } else if (!rupture || !accompagnement) {
            verdict = VictimeTraiteBeVerdict.CONDITIONS_NON_REUNIES;
            etapeProcedure = "Conditions de fond non réunies";
            if (!rupture) {
                messages.add("La rupture avec le réseau d'exploitation n'est pas établie : "
                        + "condition de fond du titre victime de la traite non réunie "
                        + "(à vérifier par avocat).");
            }
            if (!accompagnement) {
                messages.add("L'accompagnement par un centre spécialisé agréé ("
                        + CENTRES_SPECIALISES + ") n'est pas en place : condition de procédure "
                        + "non réunie. Orienter la victime vers un de ces centres (à vérifier par avocat).");
            }
        } else if (phaseProcedure == VictimeTraiteBePhase.REFLEXION_45J) {
            verdict = VictimeTraiteBeVerdict.DELAI_REFLEXION;
            etapeProcedure = "Délai de réflexion (~45 jours)";
            messages.add("La victime est dans le délai de réflexion (~45 jours, circulaire "
                    + "du 26/09/2008) : période durant laquelle elle décide de coopérer ou non "
                    + "avec la justice, sous l'accompagnement obligatoire d'un centre spécialisé agréé "
                    + "(" + CENTRES_SPECIALISES + "). Le titre temporaire pourra être sollicité "
                    + "après déclaration (à vérifier par avocat).");
        } else if (cooperation && phaseProcedure == VictimeTraiteBePhase.PROCEDURE_PENALE_EN_COURS) {
            verdict = VictimeTraiteBeVerdict.ELIGIBLE_SOUS_PROCEDURE_PENALE;
            etapeProcedure = "Procédure pénale en cours";
            messages.add("La victime coopère avec la justice et une procédure pénale est en "
                    + "cours : le titre de séjour est lié à l'UTILITÉ de la déclaration pour "
                    + "l'enquête / les poursuites (art. 61/2 et s. Loi 15/12/1980, circulaire "
                    + "du 26/09/2008 — à vérifier par avocat). Le renouvellement suit l'avancée "
                    + "de la procédure pénale.");
        } else {
            // declarationOuPenale && rupture && accompagnement, sans la condition spécifique
            // de coopération en procédure pénale -> titre temporaire.
            verdict = VictimeTraiteBeVerdict.ELIGIBLE_TITRE_TEMPORAIRE;
            etapeProcedure = (phaseProcedure == VictimeTraiteBePhase.PROCEDURE_PENALE_EN_COURS)
                    ? "Procédure pénale en cours" : "Déclaration faite";
            messages.add("La rupture avec le réseau et l'accompagnement par un centre spécialisé "
                    + "agréé (" + CENTRES_SPECIALISES + ") sont établis, déclaration faite : la "
                    + "victime paraît éligible à un titre de séjour temporaire (art. 61/2 et s. "
                    + "Loi 15/12/1980, circulaire du 26/09/2008 — à vérifier par avocat).");
            if (phaseProcedure == VictimeTraiteBePhase.PROCEDURE_PENALE_EN_COURS) {
                messages.add("Une procédure pénale est en cours sans que la coopération judiciaire "
                        + "soit documentée : confirmer l'utilité de la déclaration pour sécuriser "
                        + "le renouvellement du titre (à vérifier par avocat).");
            }
        }

        if (dateDebutAccompagnement != null) {
            messages.add("Accompagnement par un centre spécialisé débuté le "
                    + dateDebutAccompagnement + ".");
        }
        messages.add("Régime BELGE propre (3 phases : délai de réflexion → titre temporaire → "
                + "titre lié à la procédure pénale), distinct du régime français L. 425-1 CESEDA "
                + "(F-IM-35).");

        return new VictimeTraiteBeResult(
                phaseProcedure,
                rupture,
                cooperation,
                accompagnement,
                dateDebutAccompagnement,
                verdict,
                etapeProcedure,
                BASES_JURIDIQUES,
                Collections.unmodifiableList(messages));
    }

    private static void validate(VictimeTraiteBePhase phaseProcedure,
                                 LocalDate dateDebutAccompagnement,
                                 LocalDate today) {
        if (phaseProcedure == null) {
            throw new IllegalArgumentException("phaseProcedure est requise");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateDebutAccompagnement != null && dateDebutAccompagnement.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateDebutAccompagnement ne peut pas être dans le futur");
        }
    }
}
