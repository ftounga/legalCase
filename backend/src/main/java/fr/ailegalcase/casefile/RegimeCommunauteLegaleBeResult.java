package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-217-01 : résultat structuré de l'analyse du régime de communauté légale belge.
 */
public record RegimeCommunauteLegaleBeResult(
        RegimeCommunauteLegaleBeVerdict verdict,
        List<BienQualifie> biensQualifies,
        List<DetteQualifiee> dettesQualifiees,
        SyntheseComposition syntheseComposition,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {

    /** Bien qualifié par le calculateur — structure exposée dans la réponse API. */
    public record BienQualifie(
            String libelle,
            QualificationBienBe qualification,
            ModeGestionBe modeGestion,
            String fondement,
            String explication
    ) {}

    /** Dette qualifiée par le calculateur — structure exposée dans la réponse API. */
    public record DetteQualifiee(
            String libelle,
            QualificationDetteBe qualification,
            String fondement,
            String explication
    ) {}

    /** Synthèse comptable de la composition du patrimoine. */
    public record SyntheseComposition(
            int nbBiensCommuns,
            int nbBiensPropres,
            int nbBiensMixtesAvecRecompense,
            int nbDettesCommunes,
            int nbDettesPropres
    ) {}
}
