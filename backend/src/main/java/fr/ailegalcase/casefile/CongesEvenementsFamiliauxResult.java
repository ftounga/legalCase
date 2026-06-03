package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-218-43 : résultat interne business de l'analyse du congé pour évènement
 * familial (art. L.3142-1 à L.3142-5 CT, F-DT-76). Outil <b>FRANCE
 * UNIQUEMENT</b>.
 *
 * @param typeEvenement nature de l'évènement familial.
 * @param conventionPlusFavorable la CCN prévoit une durée plus favorable.
 * @param dureeConventionnelleJours durée conventionnelle (jours), null si non
 *        renseignée.
 * @param dureeLegaleJours durée légale minimale (L.3142-4) pour cet évènement.
 * @param dureeApplicableJours durée de congé applicable (la plus favorable).
 * @param base base de calcul retenue (LEGALE ou CONVENTIONNELLE).
 * @param maintienSalaire true — congé assimilé à du temps de travail effectif,
 *        maintien intégral du salaire.
 * @param assimileTempsTravailEffectif true — assimilation au temps de travail
 *        effectif (pas de réduction des droits à congés payés).
 * @param dureeMajoreePossible true si une durée légale majorée est possible
 *        (décès d'enfant : 7 jours ouvrés dans les cas renforcés).
 * @param notes notes / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record CongesEvenementsFamiliauxResult(
        CongesEvenementsFamiliauxTypeEvenement typeEvenement,
        boolean conventionPlusFavorable,
        Integer dureeConventionnelleJours,
        int dureeLegaleJours,
        int dureeApplicableJours,
        CongesEvenementsFamiliauxBase base,
        boolean maintienSalaire,
        boolean assimileTempsTravailEffectif,
        boolean dureeMajoreePossible,
        List<String> notes,
        String baseJuridique
) {}
