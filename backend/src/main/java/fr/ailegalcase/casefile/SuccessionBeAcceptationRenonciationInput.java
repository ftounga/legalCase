package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-217-12 : input pour l'outil décisionnel BE de choix d'option successorale
 * (acceptation pure / acceptation sous bénéfice d'inventaire / renonciation).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.</p>
 */
public record SuccessionBeAcceptationRenonciationInput(
        LocalDate dateDeces,
        SuccessionBeAcceptationRenonciationCalculator.QualiteHeritierBe qualiteHeritier,
        SuccessionBeAcceptationRenonciationCalculator.EtatPatrimoineSuccessoralBe etatPatrimoineSuccessoral,
        List<SuccessionBeAcceptationRenonciationCalculator.ActeHeritierBe> actesAccomplis,
        SuccessionBeAcceptationRenonciationCalculator.VolonteClientSuccessionBe volonteClient,
        boolean miseEnDemeureCreancier,
        LocalDate dateMiseEnDemeureCreancier,
        String commentaire
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private LocalDate dateDeces;
        private SuccessionBeAcceptationRenonciationCalculator.QualiteHeritierBe qualiteHeritier;
        private SuccessionBeAcceptationRenonciationCalculator.EtatPatrimoineSuccessoralBe etatPatrimoineSuccessoral;
        private List<SuccessionBeAcceptationRenonciationCalculator.ActeHeritierBe> actesAccomplis = List.of();
        private SuccessionBeAcceptationRenonciationCalculator.VolonteClientSuccessionBe volonteClient;
        private boolean miseEnDemeureCreancier = false;
        private LocalDate dateMiseEnDemeureCreancier;
        private String commentaire;

        public Builder dateDeces(LocalDate v) { this.dateDeces = v; return this; }
        public Builder qualiteHeritier(SuccessionBeAcceptationRenonciationCalculator.QualiteHeritierBe v) { this.qualiteHeritier = v; return this; }
        public Builder etatPatrimoineSuccessoral(SuccessionBeAcceptationRenonciationCalculator.EtatPatrimoineSuccessoralBe v) { this.etatPatrimoineSuccessoral = v; return this; }
        public Builder actesAccomplis(List<SuccessionBeAcceptationRenonciationCalculator.ActeHeritierBe> v) { this.actesAccomplis = v; return this; }
        public Builder volonteClient(SuccessionBeAcceptationRenonciationCalculator.VolonteClientSuccessionBe v) { this.volonteClient = v; return this; }
        public Builder miseEnDemeureCreancier(boolean v) { this.miseEnDemeureCreancier = v; return this; }
        public Builder dateMiseEnDemeureCreancier(LocalDate v) { this.dateMiseEnDemeureCreancier = v; return this; }
        public Builder commentaire(String v) { this.commentaire = v; return this; }

        public SuccessionBeAcceptationRenonciationInput build() {
            return new SuccessionBeAcceptationRenonciationInput(
                    dateDeces, qualiteHeritier, etatPatrimoineSuccessoral, actesAccomplis,
                    volonteClient, miseEnDemeureCreancier, dateMiseEnDemeureCreancier, commentaire);
        }
    }
}
