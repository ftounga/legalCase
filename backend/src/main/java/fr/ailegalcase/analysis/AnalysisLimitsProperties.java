package fr.ailegalcase.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Limites de troncature du pipeline IA, configurables par domaine juridique.
 * Lues depuis application.yml sous legalcase.pipeline.limits.
 */
@Component
@ConfigurationProperties("legalcase.pipeline.limits")
public class AnalysisLimitsProperties {

    private DomainLimits droitDuTravail = new DomainLimits();
    private DomainLimits droitImmigration = new DomainLimits();
    private DomainLimits droitFamille = new DomainLimits();

    /**
     * Retourne les limites pour le domaine donné.
     * Le domaine est une enum contrainte — tous les cas sont couverts.
     */
    public DomainLimits forDomain(String legalDomain) {
        return switch (legalDomain) {
            case "DROIT_IMMIGRATION" -> droitImmigration;
            case "DROIT_FAMILLE"     -> droitFamille;
            default                  -> droitDuTravail; // DROIT_DU_TRAVAIL
        };
    }

    public DomainLimits getDroitDuTravail() { return droitDuTravail; }
    public void setDroitDuTravail(DomainLimits v) { this.droitDuTravail = v; }

    public DomainLimits getDroitImmigration() { return droitImmigration; }
    public void setDroitImmigration(DomainLimits v) { this.droitImmigration = v; }

    public DomainLimits getDroitFamille() { return droitFamille; }
    public void setDroitFamille(DomainLimits v) { this.droitFamille = v; }

    public static class DomainLimits {
        private LevelLimits document = new LevelLimits();
        private LevelLimits dossier  = new LevelLimits();

        public LevelLimits getDocument() { return document; }
        public void setDocument(LevelLimits v) { this.document = v; }

        public LevelLimits getDossier() { return dossier; }
        public void setDossier(LevelLimits v) { this.dossier = v; }
    }

    public static class LevelLimits {
        private int faits;
        private int pointsJuridiques;
        private int risques;
        private int questionsOuvertes;
        private int timeline;

        public int getFaits() { return faits; }
        public void setFaits(int v) { this.faits = v; }

        public int getPointsJuridiques() { return pointsJuridiques; }
        public void setPointsJuridiques(int v) { this.pointsJuridiques = v; }

        public int getRisques() { return risques; }
        public void setRisques(int v) { this.risques = v; }

        public int getQuestionsOuvertes() { return questionsOuvertes; }
        public void setQuestionsOuvertes(int v) { this.questionsOuvertes = v; }

        public int getTimeline() { return timeline; }
        public void setTimeline(int v) { this.timeline = v; }
    }
}
