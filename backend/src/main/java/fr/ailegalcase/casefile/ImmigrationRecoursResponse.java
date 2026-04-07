package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ImmigrationRecoursResponse(
        UUID caseFileId,
        String recoursType,
        String recoursLabel,
        LocalDate dateNotification,
        LocalDate dateLimite,
        boolean dateLimiteDepassee,
        String avertissement,
        RequerantData requerant,
        DecisionContesteeData decisionContestee,
        String exposeFaits,
        DocumentData document
) {
    public record RequerantData(String nom, String prenom, String nationalite, String adresse) {}
    public record DecisionContesteeData(String autorite, LocalDate date, String reference) {}
    public record DocumentData(
            String enTete,
            String objetDemande,
            String visaTextes,
            String exposeFaits,
            String moyensDroit,
            String conclusions,
            List<String> piecesJointes
    ) {}
}
