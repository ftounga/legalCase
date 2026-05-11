package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-211-02 : calculateur du divorce pour désunion irrémédiable (DDI) belge — 3 voies.
 * CC art. 229 §§1, 2, 3 + Loi du 27/04/2007.
 *
 * <ul>
 *   <li>§1 — constatation par juge (preuves de désunion irrémédiable)</li>
 *   <li>§2 — commune demande après ≥ 6 mois de séparation</li>
 *   <li>§3 — unilatérale après ≥ 1 an de séparation</li>
 * </ul>
 *
 * <p>Outil <b>single-country BE</b>.
 */
public final class DivorceDdiBeCalculator {

    public static final int SEUIL_VOIE2_JOURS = 180; // 6 mois
    public static final int SEUIL_VOIE3_JOURS = 365; // 1 an

    public static final String NATURE_COMMUNE = "COMMUNE_APRES_SEPARATION";
    public static final String NATURE_UNILATERALE = "UNILATERALE_APRES_SEPARATION";
    public static final String NATURE_CONSTATATION = "CONSTATATION_PAR_JUGE";

    public static final Set<String> NATURES_VALIDES = Set.of(
            NATURE_COMMUNE, NATURE_UNILATERALE, NATURE_CONSTATATION);

    public static final String VOIE_1 = "VOIE_1_CONSTATATION";
    public static final String VOIE_2 = "VOIE_2_COMMUNE_6_MOIS";
    public static final String VOIE_3 = "VOIE_3_UNILATERALE_1_AN";
    public static final String VOIE_AUCUNE = "AUCUNE";

    private static final String BASE_JURIDIQUE =
            "CC art. 229 §§1, 2, 3 ; Loi du 27/04/2007.";

    private DivorceDdiBeCalculator() {
    }

    public static DivorceDdiBeResult compute(LocalDate dateSeparation,
                                             String natureDemande,
                                             boolean preuvesDesunionDisponibles) {
        return compute(dateSeparation, natureDemande, preuvesDesunionDisponibles,
                Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static DivorceDdiBeResult compute(LocalDate dateSeparation,
                                             String natureDemande,
                                             boolean preuvesDesunionDisponibles,
                                             Clock clock) {
        validateInputs(dateSeparation, natureDemande, clock);

        LocalDate today = LocalDate.now(clock);
        long joursSeparation = ChronoUnit.DAYS.between(dateSeparation, today);
        if (joursSeparation < 0) joursSeparation = 0;

        boolean voie1Ouverte = preuvesDesunionDisponibles;
        boolean voie2Ouverte = NATURE_COMMUNE.equals(natureDemande)
                && joursSeparation >= SEUIL_VOIE2_JOURS;
        boolean voie3Ouverte = NATURE_UNILATERALE.equals(natureDemande)
                && joursSeparation >= SEUIL_VOIE3_JOURS;

        long joursRestantsVoie2 = Math.max(0L, SEUIL_VOIE2_JOURS - joursSeparation);
        long joursRestantsVoie3 = Math.max(0L, SEUIL_VOIE3_JOURS - joursSeparation);

        String voieRecommandee;
        if (voie1Ouverte) {
            voieRecommandee = VOIE_1;
        } else if (voie2Ouverte) {
            voieRecommandee = VOIE_2;
        } else if (voie3Ouverte) {
            voieRecommandee = VOIE_3;
        } else {
            voieRecommandee = VOIE_AUCUNE;
        }

        List<String> messages = buildMessages(natureDemande, voieRecommandee,
                joursSeparation, joursRestantsVoie2, joursRestantsVoie3);
        String formule = buildFormule(dateSeparation, natureDemande, joursSeparation,
                voieRecommandee);

        return new DivorceDdiBeResult(
                dateSeparation,
                natureDemande,
                joursSeparation,
                preuvesDesunionDisponibles,
                voie1Ouverte,
                voie2Ouverte,
                voie3Ouverte,
                voieRecommandee,
                joursRestantsVoie2,
                joursRestantsVoie3,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateSeparation, String natureDemande, Clock clock) {
        if (dateSeparation == null) {
            throw new IllegalArgumentException("dateSeparation est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateSeparation.isAfter(today)) {
            throw new IllegalArgumentException("dateSeparation ne peut pas être dans le futur");
        }
        if (natureDemande == null || natureDemande.isBlank()) {
            throw new IllegalArgumentException("natureDemande est requise");
        }
        if (!NATURES_VALIDES.contains(natureDemande)) {
            throw new IllegalArgumentException(
                    "natureDemande inconnue — valeurs attendues : " + NATURES_VALIDES);
        }
    }

    private static List<String> buildMessages(String nature, String voie,
                                              long joursSeparation,
                                              long joursRestantsV2,
                                              long joursRestantsV3) {
        List<String> messages = new ArrayList<>();
        messages.add("Voie §1 (CC art. 229 §1) : constatation directe par le juge sur preuves "
                + "de désunion irrémédiable — sans seuil temporel.");
        messages.add("Voie §2 (CC art. 229 §2) : demande commune après ≥ 6 mois de séparation.");
        messages.add("Voie §3 (CC art. 229 §3) : demande unilatérale après ≥ 1 an de séparation.");
        messages.add("Compétence : tribunal de la famille (art. 572bis CJ).");

        switch (voie) {
            case VOIE_1 -> messages.add(
                    "Voie §1 recommandée : preuves de désunion disponibles — pas de délai à attendre.");
            case VOIE_2 -> messages.add(
                    "Voie §2 recommandée : demande commune éligible (≥ 6 mois écoulés).");
            case VOIE_3 -> messages.add(
                    "Voie §3 recommandée : demande unilatérale éligible (≥ 1 an écoulé).");
            case VOIE_AUCUNE -> {
                if (NATURE_COMMUNE.equals(nature)) {
                    messages.add(String.format(
                            "Voie §2 non encore ouverte — attendre encore %d jours (6 mois requis).",
                            joursRestantsV2));
                } else if (NATURE_UNILATERALE.equals(nature)) {
                    messages.add(String.format(
                            "Voie §3 non encore ouverte — attendre encore %d jours (1 an requis).",
                            joursRestantsV3));
                } else if (NATURE_CONSTATATION.equals(nature)) {
                    messages.add("Constatation par juge envisagée mais sans preuves disponibles — "
                            + "réunir les éléments factuels avant saisine.");
                }
            }
            default -> { /* unreachable */ }
        }
        return messages;
    }

    private static String buildFormule(LocalDate dateSeparation, String nature,
                                       long joursSeparation, String voie) {
        return String.format(
                "Désunion irrémédiable — séparation depuis le %s (%d jours), nature de la demande %s. "
                        + "Voie recommandée : %s.",
                dateSeparation, joursSeparation, nature, voie);
    }
}
