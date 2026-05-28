package fr.ailegalcase.casefile;

/**
 * SF-219-31 : lien de filiation ou parental ouvrant droit au congé de
 * naissance BE (Loi du 03/07/1978 art. 30 § 2 modifié par la Loi du
 * 07/04/2023).
 *
 * <p>Depuis la réforme Loi du 07/04/2023 (Deal pour l'emploi 2022,
 * entrée en vigueur 01/01/2023), le bénéfice du congé de naissance est
 * ouvert à tout co-parent du nouveau-né indépendamment du genre — y
 * compris pour les couples de même sexe, sous réserve d'établissement
 * juridique de la filiation (reconnaissance, comaternité, adoption ou
 * cohabitation légale art. 325/2 et 325/4 C. civ.). La preuve du lien
 * est apportée par l'acte de naissance, l'acte de reconnaissance,
 * l'acte de comaternité ou tout document équivalent (Cass. BE
 * 25/11/2019 S.18.0086.F).</p>
 *
 * <ul>
 *   <li>{@link #PERE_BIOLOGIQUE} — père de l'enfant établi par
 *       reconnaissance ou présomption de paternité dans le mariage
 *       art. 315 C. civ.</li>
 *   <li>{@link #COPARENT_COMATERNITE} — co-mère par comaternité art.
 *       325/2 C. civ. (couples de femmes mariées ou cohabitantes
 *       légales).</li>
 *   <li>{@link #COPARENT_RECONNAISSANCE} — co-parent ayant reconnu
 *       l'enfant art. 327/1 C. civ.</li>
 *   <li>{@link #COHABITANT_LEGAL_OU_MARIE} — cohabitant légal ou
 *       conjoint de la mère sans lien biologique direct (Loi
 *       07/04/2023).</li>
 *   <li>{@link #AUTRE} — autre situation à qualifier par avocat.</li>
 * </ul>
 */
public enum CongePaterniteNaissanceBeLienFiliation {
    /** Père biologique — présomption de paternité ou reconnaissance. */
    PERE_BIOLOGIQUE,

    /** Co-parent par comaternité art. 325/2 C. civ. */
    COPARENT_COMATERNITE,

    /** Co-parent par reconnaissance art. 327/1 C. civ. */
    COPARENT_RECONNAISSANCE,

    /** Cohabitant légal ou conjoint de la mère sans filiation propre. */
    COHABITANT_LEGAL_OU_MARIE,

    /** Autre situation à qualifier. */
    AUTRE
}
