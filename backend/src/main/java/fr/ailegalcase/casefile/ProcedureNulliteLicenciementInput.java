package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-DT-36-01 : input du calcul d'analyse des nullités de procédure de licenciement (FR).
 *
 * <p>Tous les booléens sont nullables (interprétés comme {@code false} par défaut)
 * sauf indication contraire ; les dates et commentaires sont nullables. Le pays est
 * dérivé du workspace côté service — pas transmis ici.</p>
 */
public record ProcedureNulliteLicenciementInput(
        Boolean convocationEnvoyee,
        LocalDate dateConvocationPresentee,
        LocalDate dateEntretienPrealable,
        Boolean entretienTenu,
        LocalDate dateNotificationLicenciement,
        Boolean lettreLicenciementEcrite,
        Boolean lettreMotivee,
        Boolean motivationSuffisante,
        String motivationCommentaire,
        Boolean licenciementPourMotifGrave,
        Boolean licenciementCollectif,
        Boolean procedureCseRespectee,
        Boolean conventionCollectiveApplicable,
        Boolean conventionCollectiveRespectee,
        String conventionCollectiveCommentaire
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private Boolean convocationEnvoyee;
        private LocalDate dateConvocationPresentee;
        private LocalDate dateEntretienPrealable;
        private Boolean entretienTenu;
        private LocalDate dateNotificationLicenciement;
        private Boolean lettreLicenciementEcrite;
        private Boolean lettreMotivee;
        private Boolean motivationSuffisante;
        private String motivationCommentaire;
        private Boolean licenciementPourMotifGrave;
        private Boolean licenciementCollectif;
        private Boolean procedureCseRespectee;
        private Boolean conventionCollectiveApplicable;
        private Boolean conventionCollectiveRespectee;
        private String conventionCollectiveCommentaire;

        public Builder convocationEnvoyee(Boolean v) { this.convocationEnvoyee = v; return this; }
        public Builder dateConvocationPresentee(LocalDate v) { this.dateConvocationPresentee = v; return this; }
        public Builder dateEntretienPrealable(LocalDate v) { this.dateEntretienPrealable = v; return this; }
        public Builder entretienTenu(Boolean v) { this.entretienTenu = v; return this; }
        public Builder dateNotificationLicenciement(LocalDate v) { this.dateNotificationLicenciement = v; return this; }
        public Builder lettreLicenciementEcrite(Boolean v) { this.lettreLicenciementEcrite = v; return this; }
        public Builder lettreMotivee(Boolean v) { this.lettreMotivee = v; return this; }
        public Builder motivationSuffisante(Boolean v) { this.motivationSuffisante = v; return this; }
        public Builder motivationCommentaire(String v) { this.motivationCommentaire = v; return this; }
        public Builder licenciementPourMotifGrave(Boolean v) { this.licenciementPourMotifGrave = v; return this; }
        public Builder licenciementCollectif(Boolean v) { this.licenciementCollectif = v; return this; }
        public Builder procedureCseRespectee(Boolean v) { this.procedureCseRespectee = v; return this; }
        public Builder conventionCollectiveApplicable(Boolean v) { this.conventionCollectiveApplicable = v; return this; }
        public Builder conventionCollectiveRespectee(Boolean v) { this.conventionCollectiveRespectee = v; return this; }
        public Builder conventionCollectiveCommentaire(String v) { this.conventionCollectiveCommentaire = v; return this; }

        public ProcedureNulliteLicenciementInput build() {
            return new ProcedureNulliteLicenciementInput(
                    convocationEnvoyee, dateConvocationPresentee, dateEntretienPrealable,
                    entretienTenu, dateNotificationLicenciement,
                    lettreLicenciementEcrite, lettreMotivee, motivationSuffisante, motivationCommentaire,
                    licenciementPourMotifGrave, licenciementCollectif, procedureCseRespectee,
                    conventionCollectiveApplicable, conventionCollectiveRespectee, conventionCollectiveCommentaire
            );
        }
    }
}
