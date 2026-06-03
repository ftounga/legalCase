package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-218-53 : réponse de l'analyse de conformité à l'obligation relative au droit
 * à la déconnexion (art. L.2242-17 7° CT, F-DT-83). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public record DroitDeconnexionConformiteResponse(
        UUID caseFileId,
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
        String country,
        String baseJuridique
) {}
