package fr.ailegalcase.casefile;

/**
 * SF-219-31 : statut professionnel du travailleur au regard du droit
 * au congé paternité / naissance BE (Loi du 03/07/1978 art. 30 § 2 +
 * Loi du 12/08/2000 + Loi du 07/04/2023 réforme Deal pour l'emploi).
 *
 * <p>Le régime du congé de naissance BE est ouvert au travailleur
 * salarié sous contrat de travail (Loi 03/07/1978 art. 30 § 2) ainsi
 * qu'au travailleur indépendant (régime distinct Loi 13/04/2019 et AR
 * 23/05/2019 — congé de naissance indépendant 20 jours forfaitaires
 * indemnisé par l'INASTI). Le travailleur public statutaire relève d'un
 * régime sectoriel (AR 19/11/1998 art. 23 et suivants pour la fonction
 * publique fédérale). Le travailleur en interruption de carrière, en
 * incapacité de travail ou en chômage temporaire ne bénéficie pas du
 * congé tant que le contrat n'est pas suspendu d'office (Cass. BE
 * 12/05/2014 S.13.0103.F sur l'articulation des suspensions).</p>
 */
public enum CongePaterniteNaissanceBeStatutTravailleur {
    /** Salarié sous contrat de travail (Loi 03/07/1978 art. 30 § 2). */
    SALARIE,

    /** Indépendant (Loi 13/04/2019, AR 23/05/2019 — 20 jours forfaitaires INASTI). */
    INDEPENDANT,

    /** Agent public statutaire (AR 19/11/1998 art. 23 et suivants). */
    AGENT_PUBLIC_STATUTAIRE,

    /** Autre statut (à qualifier par avocat). */
    AUTRE
}
