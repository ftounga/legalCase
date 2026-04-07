package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Résultat de l'analyse du droit au travail pour un titre de séjour donné.
 */
public record WorkRightResult(
        String titreType,
        String titreLabel,
        String country,
        String droitTravail,
        String conditions,
        List<String> obligationsEmployeur,
        String baseJuridique
) {
    public static final String OUI = "OUI";
    public static final String NON = "NON";
    public static final String CONDITIONNEL = "CONDITIONNEL";
}
