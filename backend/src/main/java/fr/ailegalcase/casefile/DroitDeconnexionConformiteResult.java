package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-218-53 : résultat interne business de l'analyse de conformité à l'obligation
 * relative au droit à la déconnexion (art. L.2242-17 7° CT, F-DT-83). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise.
 * @param delegueSyndicalPresent présence d'au moins un délégué syndical.
 * @param accordOuChartePresent présence d'un accord ou d'une charte.
 * @param plagesDeconnexionDefinies plages / modalités de déconnexion définies.
 * @param actionsSensibilisation actions de formation / sensibilisation prévues.
 * @param avisCseRecueilliPourCharte avis du CSE recueilli en cas de charte.
 * @param obligationDeNegocier true si l'obligation de négocier est déclenchée.
 * @param checklist liste des items de conformité.
 * @param itemsManquants nombre d'items applicables non conformes.
 * @param statut verdict de conformité.
 * @param notes notes / points de vigilance.
 * @param baseJuridique fondements juridiques applicables.
 */
public record DroitDeconnexionConformiteResult(
        int effectif,
        boolean delegueSyndicalPresent,
        boolean accordOuChartePresent,
        boolean plagesDeconnexionDefinies,
        boolean actionsSensibilisation,
        boolean avisCseRecueilliPourCharte,
        boolean obligationDeNegocier,
        List<DroitDeconnexionConformiteItem> checklist,
        int itemsManquants,
        DroitDeconnexionConformiteStatut statut,
        List<String> notes,
        String baseJuridique
) {}
