package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-21 : résultat du calcul Recel successoral FR (art. 778 Cciv).
 *
 * <ul>
 *   <li>{@code qualificationRecel} — "CARACTERISE" (recel établi par preuves
 *       suffisantes), "PROBABLE" (faisceau d'indices ou preuve indirecte),
 *       "INSUFFISANT" (preuves insuffisantes, voie risquée), "PAS_DE_RECEL"
 *       (motif ne caractérise pas le recel) ou "HORS_PERIMETRE" (qualité du
 *       receleur exclue — ex. tiers complice).</li>
 *   <li>{@code verdictRecel} — code court résumant la conclusion
 *       ("RECEL_CARACTERISE", "RECEL_PROBABLE_FAISCEAU",
 *       "PREUVE_INSUFFISANTE", "QUALITE_RECELEUR_EXCLUE",
 *       "DELAI_FORCLOS", "PAS_DE_RECEL").</li>
 *   <li>{@code sanctionCivile} — texte décrivant la sanction civile
 *       (art. 778 al. 2 : privation sur le bien recelé + obligation de
 *       rapporter sans émolument).</li>
 *   <li>{@code preuveRequise} — texte décrivant la preuve requise et la
 *       charge probatoire (Cass. 1ère civ. — faisceau d'indices admis).</li>
 *   <li>{@code delaiAction} — texte décrivant le délai (5 ans depuis
 *       l'ouverture de la succession ou la découverte du recel).</li>
 *   <li>{@code delaiForclos} — true si le délai 5 ans est manifestement
 *       expiré depuis l'ouverture.</li>
 *   <li>{@code voletPenalSignale} — true si un volet pénal est signalé
 *       (art. 441-8 CP — destruction de testament). Hors scope LegalCase V1.</li>
 *   <li>{@code baseLegale} — articles Cciv applicables.</li>
 *   <li>{@code messages} — informations contextuelles.</li>
 *   <li>{@code alertes} — points d'attention bloquants ou de vigilance.</li>
 * </ul>
 */
public record RecelSuccessionResult(
        String qualificationRecel,
        String verdictRecel,
        String sanctionCivile,
        String preuveRequise,
        String delaiAction,
        boolean delaiForclos,
        boolean voletPenalSignale,
        String baseLegale,
        List<String> messages,
        List<String> alertes
) {}
