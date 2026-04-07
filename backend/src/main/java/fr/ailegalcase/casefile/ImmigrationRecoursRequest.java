package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record ImmigrationRecoursRequest(
        String recoursType,
        LocalDate dateNotification,
        RequerantData requerant,
        DecisionContesteeData decisionContestee,
        String exposeFaits
) {
    public record RequerantData(String nom, String prenom, String nationalite, String adresse) {}
    public record DecisionContesteeData(String autorite, LocalDate date, String reference) {}
}
