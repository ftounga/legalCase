package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

public record ReferesAdminRequest(
        String typeRefere,
        String decisionContestee,
        LocalDate dateNotificationDecision,
        Boolean urgenceCaracterisee,
        Boolean atteinteLiberteFondamentale,
        Boolean doutesSerieuxLegalite,
        List<String> preuvesUrgence,
        Boolean demandeurDejaPrived
) {}
