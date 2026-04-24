package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record AesEtudiantRequest(
        LocalDate dateEntreeFrance,
        Integer dureePresenceMois,
        Integer anneesScolariteEnFranceConsecutives,
        String niveauEtudesActuel,
        String resultatsAcademiques,
        Boolean inscriptionEtablissementReconnu,
        Boolean moyensSubsistance,
        Boolean menaceOrdrePublic,
        Boolean parcoursCoherent,
        LocalDate dateDepotDemande
) {}
