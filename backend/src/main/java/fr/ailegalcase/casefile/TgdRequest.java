package fr.ailegalcase.casefile;

/**
 * SF-222-02 : body POST /api/v1/case-files/{id}/tgd-analysis.
 *
 * <p>Outil décisionnel TGD (téléphone grave danger — éligibilité, art. 41-3-1
 * CPP), Famille FRANCE uniquement. Évalue l'éligibilité d'une victime de
 * violences au dispositif TGD. L'outil <b>conseille</b> ; l'attribution relève
 * du procureur de la République.</p>
 *
 * <p>5 critères booléens d'éligibilité :</p>
 * <ul>
 *   <li>{@code dangerGrave} — danger grave caractérisé pour la victime ;</li>
 *   <li>{@code violencesAvereesOuVraisemblables} — violences avérées ou vraisemblables ;</li>
 *   <li>{@code interdictionContactProcedure} — interdiction de contact issue d'une
 *       procédure (ordonnance de protection / contrôle judiciaire / SME / condamnation) ;</li>
 *   <li>{@code nonCohabitation} — auteur et victime ne cohabitent pas ;</li>
 *   <li>{@code consentementVictime} — consentement exprès de la victime au dispositif.</li>
 * </ul>
 *
 * <p>Anti-doublon F-FA-14 (ordonnance de protection) : cet outil <b>n'établit
 * pas</b> la recommandation de mesures de protection (qui relève de F-FA-14, où
 * le TGD figure comme mesure {@code OrdonnanceProtectionMesureType.TGD}). Il se
 * borne à analyser l'éligibilité au dispositif TGD.</p>
 */
public record TgdRequest(
        Boolean dangerGrave,
        Boolean violencesAvereesOuVraisemblables,
        Boolean interdictionContactProcedure,
        Boolean nonCohabitation,
        Boolean consentementVictime
) {}
