package fr.ailegalcase.casefile;

/**
 * SF-219-15 : code de la commission paritaire (CP) sectorielle de
 * l'entreprise utilisatrice — détermine la CCT sectorielle applicable
 * pour le calcul d'une éventuelle <b>prime de fin d'année (13e mois)</b>
 * due à l'intérimaire.
 *
 * <p>La CP est l'organe paritaire belge regroupant employeurs et
 * organisations syndicales d'un même secteur, instituée par la
 * Loi du 05/12/1968 sur les CCT et les commissions paritaires
 * (art. 26 et s.). C'est <b>la CP de l'utilisateur</b>, et non celle
 * de l'ETI (qui est toujours CP 322 Travail intérimaire pour les
 * ETI), qui détermine les avantages conventionnels (prime fin
 * d'année, chèques-repas, éco-chèques, …) dus à l'intérimaire en
 * application stricte de l'art. 10 Loi 24/07/1987 (parité salariale)
 * et de la CCT n° 322 du 14/06/2010.</p>
 *
 * <h2>CP recensées V1 (les plus fréquentes en pratique intérim)</h2>
 * <ul>
 *   <li>{@link #CP_124_CONSTRUCTION} — Commission paritaire de la
 *       construction. Prime fin d'année réglée par la CCT du 12/06/2014
 *       (rendue obligatoire par AR du 11/02/2015, M.B. 04/03/2015) :
 *       9,12 % du salaire brut de référence, payée via le Fonds de
 *       sécurité d'existence (Constructiv).</li>
 *   <li>{@link #CP_200_AUXILIAIRE_EMPLOYES} — CP auxiliaire pour
 *       employés (anc. CP 218 jusqu'au 01/04/2015). Prime fin d'année
 *       réglée par la CCT du 09/06/2016 (rendue obligatoire par AR
 *       du 13/06/2017) : 1/12 de la rémunération annuelle brute,
 *       condition d'ancienneté = 6 mois ininterrompus dans le même
 *       secteur (art. 4 CCT).</li>
 *   <li>{@link #CP_218_NON_MARCHAND} — CP 218 employés du non-marchand
 *       (santé, enseignement libre, secteur socio-culturel). Prime
 *       fin d'année généralement réglée par CCT sectorielle interne
 *       (variable selon sous-commission).</li>
 *   <li>{@link #CP_322_INTERIM} — CP 322 (Travail intérimaire) : la
 *       CCT n° 322 du 14/06/2010 et ses CCT exécutoires (n° 322bis
 *       vacances, n° 322ter pension complémentaire, n° 322quater
 *       chèques-repas) gèrent les avantages communs aux intérimaires.
 *       Pas de prime fin d'année propre à la CP 322 (renvoi à la CP
 *       de l'utilisateur).</li>
 *   <li>{@link #CP_140_TRANSPORT_LOGISTIQUE} — CP 140 (transport et
 *       logistique). Prime fin d'année 8,33 % du brut annuel via
 *       Fonds Social Transport (CCT 27/01/2011 RA 30/03/2011).</li>
 *   <li>{@link #CP_209_METAUX_FABRICATIONS_METALLIQUES} — CP 209
 *       métaux et fabrications métalliques. Prime fin d'année
 *       conventionnelle selon CCT sectorielle (variable).</li>
 *   <li>{@link #AUTRE_NON_RECENSE} — toute autre CP non recensée
 *       par l'outil V1 → verdict {@code A_ANALYSER_SECTEUR_NON_RECONNU}
 *       et renvoi avocat.</li>
 * </ul>
 *
 * <p><b>Note</b> : les taux et conditions exacts varient selon les
 * CCT en vigueur à la date de la mission — un avocat BE spécialisé
 * doit confirmer la CCT applicable et son taux pour la période
 * considérée. L'outil applique des taux par défaut paramétrables.</p>
 */
public enum InterimBeCommissionParitaire {
    /** CP 124 — Construction. */
    CP_124_CONSTRUCTION,

    /** CP 200 — Commission auxiliaire pour employés (anc. CP 218). */
    CP_200_AUXILIAIRE_EMPLOYES,

    /** CP 218 — Employés du non-marchand. */
    CP_218_NON_MARCHAND,

    /** CP 322 — Travail intérimaire (rare comme utilisateur). */
    CP_322_INTERIM,

    /** CP 140 — Transport et logistique. */
    CP_140_TRANSPORT_LOGISTIQUE,

    /** CP 209 — Métaux et fabrications métalliques. */
    CP_209_METAUX_FABRICATIONS_METALLIQUES,

    /** Toute autre CP — analyse au cas par cas. */
    AUTRE_NON_RECENSE
}
