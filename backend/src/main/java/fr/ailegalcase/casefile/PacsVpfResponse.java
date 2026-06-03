package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-220-04 : réponse de l'analyse VPF au titre d'un PACS L.423-23
 * (F-IM-50-pacs-vpf-fr). Outil single-country FR.
 */
public record PacsVpfResponse(
        UUID caseFileId,
        boolean pacsConclu,
        LocalDate datePacs,
        String partenaireStatut,
        Integer dureeVieCommuneMois,
        String intensiteCommunauteVie,
        boolean autresLiensPrivesFamiliaux,
        String country,
        String eligibilite,
        List<String> elementsFavorables,
        List<String> elementsManquants,
        List<String> basesJuridiques,
        List<String> messages
) {}
