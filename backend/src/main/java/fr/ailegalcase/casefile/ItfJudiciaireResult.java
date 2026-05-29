package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-37 : résultat interne business de l'analyse d'une interdiction du
 * territoire français (ITF) prononcée par un juge pénal comme peine
 * complémentaire (C. pén. 131-30 à 131-30-2).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b>. Distinct de l'IRTF administrative
 * (préfet, L. 612-6 CESEDA) couverte par F-IM-20.
 *
 * @param dateCondamnation date de la condamnation pénale ayant prononcé l'ITF.
 * @param dureeITFAnnees durée de l'ITF en années (0 si définitive / non renseignée).
 * @param infractionPrincipale infraction ayant motivé l'ITF (≤ 200 car., nullable).
 * @param condamnationDefinitive true si la condamnation est définitive.
 * @param dateEcheanceReleve date minimale de recevabilité d'une requête en
 *        relèvement (condamnation + 5 ans, C. pén. 131-30-1 / 702-1).
 * @param voiesRecours voies de recours disponibles (appel 10 j, cassation 5 j,
 *        requête en relèvement après délai).
 * @param requisReleve conditions requises pour la requête en relèvement.
 * @param distinctionItfVsIrtf texte explicatif ITF (juge pénal) vs IRTF (préfet).
 * @param statut statut au regard des recours et du relèvement.
 * @param baseJuridique fondements juridiques applicables.
 */
public record ItfJudiciaireResult(
        LocalDate dateCondamnation,
        int dureeITFAnnees,
        String infractionPrincipale,
        boolean condamnationDefinitive,
        LocalDate dateEcheanceReleve,
        List<String> voiesRecours,
        List<String> requisReleve,
        String distinctionItfVsIrtf,
        ItfJudiciaireStatut statut,
        String baseJuridique
) {}
