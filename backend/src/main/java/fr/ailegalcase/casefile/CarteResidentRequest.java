package fr.ailegalcase.casefile;

/**
 * SF-214-23 : requête POST pour l'analyse d'éligibilité à la carte de résident
 * 10 ans L. 426-1 CESEDA. Outil single-country FR.
 *
 * <p>{@code typesTitresAnterieurs} est optionnel (peut être null).</p>
 */
public record CarteResidentRequest(
        Integer dureeSejourRegulierAnnees,
        String typesTitresAnterieurs,
        String niveauIntegration,
        Double ressourcesMensuellesNettes,
        Boolean condamnationsPenalesGraves
) {}
