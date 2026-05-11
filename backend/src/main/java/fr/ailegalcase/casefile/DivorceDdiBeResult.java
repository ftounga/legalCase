package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-211-02 : résultat de l'analyse du divorce pour désunion irrémédiable belge.
 * CC art. 229 §§1, 2, 3 + Loi 27/04/2007.
 * Outil <b>single-country BE</b>.
 */
public record DivorceDdiBeResult(
        LocalDate dateSeparation,
        String natureDemande,
        long joursSeparation,
        boolean preuvesDesunionDisponibles,
        boolean voie1Ouverte,
        boolean voie2Ouverte,
        boolean voie3Ouverte,
        String voieRecommandee,
        long joursRestantsVoie2,
        long joursRestantsVoie3,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
