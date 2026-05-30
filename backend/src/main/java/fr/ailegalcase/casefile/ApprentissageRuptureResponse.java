package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-23 : réponse de l'analyse de la rupture du contrat d'apprentissage
 * (art. L.6222-18 et s. CT, F-DT-110). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record ApprentissageRuptureResponse(
        UUID caseFileId,
        LocalDate dateDebutContrat,
        LocalDate dateRupture,
        ApprentissageAuteurRupture auteurRupture,
        ApprentissageMotifRupture motifRupture,
        boolean apprentiMajeur,
        long joursDepuisDebut,
        ApprentissagePeriodeRupture periode,
        boolean dansPeriodeLibre,
        ApprentissageRuptureValidite validite,
        ApprentissageRuptureVerdict verdictGlobal,
        List<String> consequences,
        String motif,
        String country,
        String baseJuridique
) {}
