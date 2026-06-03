package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-218-43 : réponse de l'analyse du congé pour évènement familial (art.
 * L.3142-1 à L.3142-5 CT, F-DT-76). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record CongesEvenementsFamiliauxResponse(
        UUID caseFileId,
        CongesEvenementsFamiliauxTypeEvenement typeEvenement,
        boolean conventionPlusFavorable,
        Integer dureeConventionnelleJours,
        int dureeLegaleJours,
        int dureeApplicableJours,
        CongesEvenementsFamiliauxBase base,
        boolean maintienSalaire,
        boolean assimileTempsTravailEffectif,
        boolean dureeMajoreePossible,
        List<String> notes,
        String country,
        String baseJuridique
) {}
