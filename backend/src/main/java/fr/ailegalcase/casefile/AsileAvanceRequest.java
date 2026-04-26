package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-IM-12-01 : requête pour l'analyse d'asile avancé (CESEDA Livre V — France).
 *
 * <p>Outil <b>single-country FR</b>. Le régime belge (CGRA + Loi 15/12/1980) sera traité
 * dans une feature jumelle au backlog (F-IM-12-BE) si le besoin métier est confirmé.
 *
 * @param dispositifAsile             enum : DUBLIN_III / PROCEDURE_ACCELEREE / REEXAMEN
 *                                    / APATRIDIE / PROTECTION_SUBSIDIAIRE
 * @param dateDecisionAnterieure      date du rejet précédent (REEXAMEN)
 * @param elementsNouveaux            éléments nouveaux personnels postérieurs au rejet (REEXAMEN)
 * @param paysOrigineDansListeSurs    pays d'origine inscrit sur la liste OFPRA (PROCEDURE_ACCELEREE)
 * @param empreintesEurodacAutresEm   empreintes EURODAC déjà prises dans un autre EM (DUBLIN_III)
 * @param demandeurEnFuite            demandeur en fuite (passe le délai Dublin de 6 à 18 mois)
 * @param motifsExclusion             motifs d'exclusion (crime de guerre, sécurité publique)
 * @param traitementsGravesEtablis    crainte fondée de traitements graves (PROTECTION_SUBSIDIAIRE)
 * @param fraudeDocumentaireAvere     fraude documentaire avérée (PROCEDURE_ACCELEREE)
 * @param refusPriseEmpreintes        refus de prise d'empreintes (PROCEDURE_ACCELEREE)
 * @param presenceReguliere           présence régulière en France (APATRIDIE)
 */
public record AsileAvanceRequest(
        String dispositifAsile,
        LocalDate dateDecisionAnterieure,
        Boolean elementsNouveaux,
        Boolean paysOrigineDansListeSurs,
        Boolean empreintesEurodacAutresEm,
        Boolean demandeurEnFuite,
        Boolean motifsExclusion,
        Boolean traitementsGravesEtablis,
        Boolean fraudeDocumentaireAvere,
        Boolean refusPriseEmpreintes,
        Boolean presenceReguliere
) {}
