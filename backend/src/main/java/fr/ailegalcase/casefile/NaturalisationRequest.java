package fr.ailegalcase.casefile;

/**
 * SF-IM-13-01 : requête pour l'analyse d'une demande de naturalisation française
 * (Code civil art. 21-15 et s. — décret, déclaration, etc.).
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent belge (Code de la nationalité belge,
 * art. 12bis CNB et s.) sera traité dans une feature jumelle au backlog (F-IM-13-BE).
 *
 * @param voieNaturalisation              code voie : DECRET / MARIAGE / ASCENDANT / MINEUR
 *                                        / REINTEGRATION / OPPOSITION
 * @param dureeResidenceReguliereAnnees   nombre d'années de résidence régulière en France
 * @param dureeMariageAnnees              nombre d'années de mariage (voie MARIAGE)
 * @param cohabitationContinue            cohabitation continue depuis le mariage (impacte 4/5 ans)
 * @param ageDemandeur                    âge du demandeur (voie ASCENDANT requiert ≥ 65)
 * @param ascendantDirectFrancais         le demandeur est ascendant direct d'un français
 *                                        (voie ASCENDANT)
 * @param parentAcquiertNationalite       un parent du mineur acquiert la nationalité française
 *                                        (voie MINEUR)
 * @param vitAvecParentAcquereur          le mineur vit habituellement avec le parent acquéreur
 *                                        (voie MINEUR)
 * @param ancienFrancais                  le demandeur a été français par le passé (REINTEGRATION)
 * @param casierJudiciaireVierge          casier judiciaire vierge (Boolean tristate, default true)
 * @param assimilationLangueB1            assimilation à la communauté française (langue B1)
 * @param ressourcesStables               ressources stables et suffisantes (DECRET)
 * @param oppositionGouvernementaleActive opposition gouvernementale active (bloque toute voie)
 * @param etudesSuperieuresFrance         études supérieures en France (réduit DECRET à 2 ans)
 */
public record NaturalisationRequest(
        String voieNaturalisation,
        Integer dureeResidenceReguliereAnnees,
        Integer dureeMariageAnnees,
        Boolean cohabitationContinue,
        Integer ageDemandeur,
        Boolean ascendantDirectFrancais,
        Boolean parentAcquiertNationalite,
        Boolean vitAvecParentAcquereur,
        Boolean ancienFrancais,
        Boolean casierJudiciaireVierge,
        Boolean assimilationLangueB1,
        Boolean ressourcesStables,
        Boolean oppositionGouvernementaleActive,
        Boolean etudesSuperieuresFrance
) {}
