package fr.ailegalcase.casefile;

/**
 * SF-219-15 : verdict de l'outil <i>indemnité de fin de mission
 * intérim BE</i> (Loi du 24/07/1987 + CCT n° 322 + CCT n° 95 + CCT
 * sectorielles + jurisprudence Cass. BE).
 *
 * <h2>Spécificité BE vs FR</h2>
 * <p>Contrairement au régime français qui impose une <b>indemnité de
 * fin de mission (IFM) forfaitaire de 10 %</b> du salaire brut total
 * (C. trav. fr. art. L. 1251-32, à compter de la Loi du 27/05/1989),
 * <b>le droit belge n'impose AUCUNE prime de précarité de fin de
 * mission équivalente</b>. Cass. (BE) a refusé toute généralisation
 * d'une indemnité forfaitaire de précarité pour l'intérim
 * (cf. notamment Cass. 03/05/2010, S.09.0086.F ; Cass. 23/06/2003,
 * S.02.0103.F). Les indemnités effectivement dues à la fin d'une
 * mission intérimaire belge se réduisent donc aux composantes
 * <b>salariales et conventionnelles ordinaires</b> :</p>
 * <ol>
 *   <li><b>Pécule de vacances intérim</b> — payé par l'ETI via le
 *       <i>Fonds Social pour les Intérimaires</i> (FSI), calculé en
 *       application de l'AR du 30/03/1967 sur les vacances annuelles
 *       (article 19) + CCT n° 322bis du 16/12/2010 fixant la
 *       responsabilité ETI : taux légal 15,38 % du brut prestations
 *       de l'exercice de vacances précédent (pécule simple 7,67 %
 *       + pécule double 7,71 %, arrondi 15,38 %).</li>
 *   <li><b>Prime de fin d'année sectorielle (« 13e mois »)</b> — due
 *       UNIQUEMENT si la CCT sectorielle applicable à l'utilisateur
 *       le prévoit (CP 124 construction, CP 218.01 employés
 *       non-marchand, CP 200 commission auxiliaire pour employés,
 *       CP 322 propre intérim Wallonie/Flandre depuis 2010 art. 17, …)
 *       et si l'intérimaire remplit la condition d'ancienneté
 *       sectorielle (généralement 65 jours de prestations dans le
 *       même secteur sur l'exercice de référence — cf. CCT propres).
 *       Calcul : généralement 1/12 du salaire annuel brut sectoriel,
 *       proratisé sur la durée de prestation.</li>
 *   <li><b>Indemnité compensatoire de préavis</b> — uniquement si la
 *       mission est rompue avant terme par l'ETI <i>sans motif grave</i>
 *       (Loi du 03/07/1978 art. 39 et s. appliquée par renvoi de la
 *       Loi 24/07/1987 art. 8). Pour une mission inférieure à 6 mois,
 *       le préavis est de 3 jours calendrier (CCT n° 75 du
 *       20/12/1999 — non applicable aux intérimaires en tant que
 *       tels mais sert de référence Cass.). Cass. (BE) a précisé
 *       que la rupture anticipée injustifiée d'une mission intérim
 *       à durée déterminée ouvre droit aux rémunérations restant
 *       à courir jusqu'au terme prévu (Cass. 04/02/1991,
 *       S.90.0024.F + Cass. 16/12/2002, S.01.0124.F).</li>
 *   <li><b>Heures supplémentaires + sursalaires</b> — Loi 16/03/1971
 *       sur le travail, art. 29 (sursalaire 50 % en semaine, 100 %
 *       le dimanche / jour férié), si effectuées et non encore
 *       payées.</li>
 *   <li><b>Indemnité pour défaut de prise de congés</b> — pour les
 *       jours de vacances restants impossibles à prendre avant la
 *       fin de mission (très rare pour intérim).</li>
 * </ol>
 *
 * <p><b>Sanction explicite</b> : aucune prime de précarité de 10 %
 * style FR (art. L. 1251-32) n'est due. L'outil avertit explicitement
 * l'avocat pour éviter une transposition mécanique du droit FR.</p>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #INDEMNITES_DUES} — au moins une composante d'indemnité
 *       (pécule de vacances OU prime fin d'année OU rupture anticipée
 *       OU heures sup) est non nulle et calculée.</li>
 *   <li>{@link #RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR} — la
 *       mission a été rompue avant son terme par l'ETI sans motif
 *       grave : l'intérimaire a droit au salaire restant à courir
 *       jusqu'à la date prévue (Cass. 04/02/1991). Prime sur le
 *       calcul ordinaire car indemnité spécifique substituée.</li>
 *   <li>{@link #AUCUNE_INDEMNITE_DUE} — fin de mission au terme prévu,
 *       pécule de vacances déjà payé par le FSI, pas de CCT
 *       sectorielle applicable, pas d'heures sup. Le verdict explique
 *       au lecteur l'absence de prime de précarité 10 % en BE
 *       (contraste FR).</li>
 *   <li>{@link #A_ANALYSER_SECTEUR_NON_RECONNU} — le code commission
 *       paritaire de l'utilisateur est {@code AUTRE_NON_RECENSE} :
 *       prime fin d'année non calculable en automatique, renvoi
 *       avocat CCT sectorielle.</li>
 * </ul>
 */
public enum InterimBeIndemniteFinMissionVerdict {
    /** Au moins une indemnité de fin de mission est due (cas standard). */
    INDEMNITES_DUES,

    /**
     * Mission rompue avant terme par l'ETI sans motif grave —
     * intérimaire a droit aux rémunérations restant à courir
     * jusqu'au terme prévu (jurisprudence Cass. 04/02/1991).
     */
    RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR,

    /**
     * Aucune indemnité due — fin de mission au terme prévu, pécule
     * vacances FSI déjà payé, pas de CCT sectorielle prévoyant prime
     * fin d'année, pas d'heures sup en suspens. Pas de prime de
     * précarité 10 % style FR en droit belge.
     */
    AUCUNE_INDEMNITE_DUE,

    /**
     * Commission paritaire utilisateur non recensée dans le référentiel
     * outil — prime de fin d'année à analyser manuellement par avocat
     * (CCT sectorielle propre à confirmer).
     */
    A_ANALYSER_SECTEUR_NON_RECONNU
}
