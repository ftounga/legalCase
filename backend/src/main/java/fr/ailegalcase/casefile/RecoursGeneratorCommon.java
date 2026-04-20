package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * SF-133-01 : helpers partagés par les 3 générateurs de recours spécialisés.
 * Contient uniquement des fonctions pures sans divergence selon le type.
 */
final class RecoursGeneratorCommon {

    private RecoursGeneratorCommon() {}

    /** Objet du recours — format générique FR/BE. */
    static String buildObjet(String autorite, LocalDate dateDecision, String reference) {
        String ref = reference != null && !reference.isBlank() ? " (réf. " + reference + ")" : "";
        return String.format("Recours contre la décision de %s en date du %s%s portant refus de titre de séjour.",
                autorite, dateDecision, ref);
    }

    /** Visa des textes applicables (préfixé "Vu"). */
    static String buildVisaTextes(RecoursType type) {
        return type.textesApplicables().stream()
                .map(t -> "Vu " + t + " ;")
                .collect(Collectors.joining("\n"));
    }

    /** Calcul de la date limite et avertissement éventuel si dépassée. */
    static DateLimiteInfo computeDateLimite(RecoursType type, LocalDate dateNotification) {
        LocalDate dateLimite = dateNotification.plusDays(type.delaiJours());
        boolean depassee = LocalDate.now().isAfter(dateLimite);
        String avertissement = depassee
                ? "ATTENTION : le délai de recours de " + type.delaiJours() + " jours est dépassé depuis le "
                  + dateLimite + ". Le recours risque d'être déclaré irrecevable."
                : null;
        return new DateLimiteInfo(dateLimite, depassee, avertissement);
    }

    record DateLimiteInfo(LocalDate dateLimite, boolean depassee, String avertissement) {}
}
