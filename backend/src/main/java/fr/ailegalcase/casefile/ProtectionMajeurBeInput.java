package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-217-14 : input pour l'outil décisionnel BE de protection du majeur
 * incapable (loi du 17/03/2013, statut unique d'administration).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.</p>
 */
public record ProtectionMajeurBeInput(
        ProtectionMajeurBeCalculator.NatureAlterationBe natureAlteration,
        ProtectionMajeurBeCalculator.GraviteIncapaciteBe graviteIncapacite,
        boolean mandatExtraJudiciaireSigne,
        LocalDate mandatExtraJudiciaireDateSignature,
        boolean declarationAnticipeeExiste,
        boolean environnementFamilialProtecteur,
        ProtectionMajeurBeCalculator.NiveauUrgenceProtectionBe niveauUrgence,
        ProtectionMajeurBeCalculator.ModeSaisineProtectionBe modeSaisineEnvisage,
        String commentaire
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private ProtectionMajeurBeCalculator.NatureAlterationBe natureAlteration;
        private ProtectionMajeurBeCalculator.GraviteIncapaciteBe graviteIncapacite;
        private boolean mandatExtraJudiciaireSigne = false;
        private LocalDate mandatExtraJudiciaireDateSignature;
        private boolean declarationAnticipeeExiste = false;
        private boolean environnementFamilialProtecteur = false;
        private ProtectionMajeurBeCalculator.NiveauUrgenceProtectionBe niveauUrgence;
        private ProtectionMajeurBeCalculator.ModeSaisineProtectionBe modeSaisineEnvisage;
        private String commentaire;

        public Builder natureAlteration(ProtectionMajeurBeCalculator.NatureAlterationBe v) { this.natureAlteration = v; return this; }
        public Builder graviteIncapacite(ProtectionMajeurBeCalculator.GraviteIncapaciteBe v) { this.graviteIncapacite = v; return this; }
        public Builder mandatExtraJudiciaireSigne(boolean v) { this.mandatExtraJudiciaireSigne = v; return this; }
        public Builder mandatExtraJudiciaireDateSignature(LocalDate v) { this.mandatExtraJudiciaireDateSignature = v; return this; }
        public Builder declarationAnticipeeExiste(boolean v) { this.declarationAnticipeeExiste = v; return this; }
        public Builder environnementFamilialProtecteur(boolean v) { this.environnementFamilialProtecteur = v; return this; }
        public Builder niveauUrgence(ProtectionMajeurBeCalculator.NiveauUrgenceProtectionBe v) { this.niveauUrgence = v; return this; }
        public Builder modeSaisineEnvisage(ProtectionMajeurBeCalculator.ModeSaisineProtectionBe v) { this.modeSaisineEnvisage = v; return this; }
        public Builder commentaire(String v) { this.commentaire = v; return this; }

        public ProtectionMajeurBeInput build() {
            return new ProtectionMajeurBeInput(
                    natureAlteration, graviteIncapacite,
                    mandatExtraJudiciaireSigne, mandatExtraJudiciaireDateSignature,
                    declarationAnticipeeExiste, environnementFamilialProtecteur,
                    niveauUrgence, modeSaisineEnvisage, commentaire);
        }
    }
}
