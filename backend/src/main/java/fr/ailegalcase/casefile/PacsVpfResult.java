package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-220-04 : résultat de l'analyse VPF au titre d'un PACS L.423-23
 * (F-IM-50-pacs-vpf-fr). Outil single-country FR.
 */
public record PacsVpfResult(
        boolean pacsConclu,
        String partenaireStatut,
        Integer dureeVieCommuneMois,
        String intensiteCommunauteVie,
        boolean autresLiensPrivesFamiliaux,
        String eligibilite,
        List<String> elementsFavorables,
        List<String> elementsManquants,
        List<String> basesJuridiques,
        List<String> messages
) {}
