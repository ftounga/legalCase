package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-211-01 : calculateur du divorce par consentement mutuel (DC) belge.
 * CJ art. 1287-1304 + Loi du 27/04/2007 réformant le divorce + CC art. 229 §1.
 *
 * <p>Vérifie :
 * <ul>
 *   <li>Convention préalable complète (logement, biens, garde enfants, contributions)</li>
 *   <li>Délai de réflexion ≥ 3 mois (90 jours) entre signature et audience d'homologation</li>
 *   <li>Consentement des deux époux</li>
 * </ul>
 *
 * <p>Outil <b>single-country BE</b>.
 */
public final class DivorceDcBeCalculator {

    public static final int DELAI_REFLEXION_JOURS_MIN = 90;

    public static final String VERDICT_RECEVABLE = "RECEVABLE";
    public static final String VERDICT_IRRECEVABLE = "IRRECEVABLE";
    public static final String VERDICT_NON_CONCERNE = "NON_CONCERNE";

    private static final String BASE_JURIDIQUE =
            "CJ art. 1287-1304 ; Loi du 27/04/2007 ; CC art. 229 §1.";

    private DivorceDcBeCalculator() {
    }

    public static DivorceDcBeResult compute(LocalDate dateSignatureConvention,
                                            LocalDate dateAudienceHomologation,
                                            boolean conventionLogement,
                                            boolean conventionBiens,
                                            boolean conventionGardeEnfants,
                                            boolean conventionContributions,
                                            boolean enfantsMineursCommuns,
                                            boolean epouxConsentent) {
        return compute(dateSignatureConvention, dateAudienceHomologation,
                conventionLogement, conventionBiens, conventionGardeEnfants, conventionContributions,
                enfantsMineursCommuns, epouxConsentent,
                Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static DivorceDcBeResult compute(LocalDate dateSignatureConvention,
                                            LocalDate dateAudienceHomologation,
                                            boolean conventionLogement,
                                            boolean conventionBiens,
                                            boolean conventionGardeEnfants,
                                            boolean conventionContributions,
                                            boolean enfantsMineursCommuns,
                                            boolean epouxConsentent,
                                            Clock clock) {
        validateInputs(dateSignatureConvention, dateAudienceHomologation, clock);

        boolean conventionComplete = conventionLogement && conventionBiens
                && conventionGardeEnfants && conventionContributions;

        long delaiReflexionJours = -1;
        boolean delaiReflexionRespecte = false;
        if (dateAudienceHomologation != null) {
            delaiReflexionJours = ChronoUnit.DAYS.between(
                    dateSignatureConvention, dateAudienceHomologation);
            delaiReflexionRespecte = delaiReflexionJours >= DELAI_REFLEXION_JOURS_MIN;
        }

        List<String> motifs = new ArrayList<>();
        if (!epouxConsentent) {
            motifs.add("Consentement mutuel manquant — voie DC inapplicable, envisager DDI (CC art. 229 §2/§3).");
        }
        if (!conventionLogement) {
            motifs.add("Convention sur le logement absente — art. 1287 CJ.");
        }
        if (!conventionBiens) {
            motifs.add("Convention sur la liquidation des biens absente — art. 1287 CJ.");
        }
        if (!conventionGardeEnfants) {
            motifs.add("Convention sur la garde / hébergement des enfants absente — art. 1287 CJ.");
        }
        if (!conventionContributions) {
            motifs.add("Convention sur les contributions alimentaires absente — art. 1287 CJ.");
        }
        if (dateAudienceHomologation != null && !delaiReflexionRespecte) {
            motifs.add(String.format(
                    "Délai de réflexion insuffisant — %d jours observés, minimum 90 jours (art. 1294 CJ).",
                    delaiReflexionJours));
        }

        String verdict;
        if (!epouxConsentent) {
            verdict = VERDICT_NON_CONCERNE;
        } else if (motifs.isEmpty()) {
            verdict = VERDICT_RECEVABLE;
        } else {
            verdict = VERDICT_IRRECEVABLE;
        }

        List<String> messages = buildMessages(verdict, conventionComplete,
                enfantsMineursCommuns, dateAudienceHomologation == null);
        String formule = buildFormule(dateSignatureConvention, dateAudienceHomologation,
                verdict, delaiReflexionJours);

        return new DivorceDcBeResult(
                dateSignatureConvention,
                dateAudienceHomologation,
                delaiReflexionJours,
                delaiReflexionRespecte,
                conventionLogement,
                conventionBiens,
                conventionGardeEnfants,
                conventionContributions,
                conventionComplete,
                enfantsMineursCommuns,
                epouxConsentent,
                verdict,
                motifs,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateSignatureConvention,
                                       LocalDate dateAudienceHomologation,
                                       Clock clock) {
        if (dateSignatureConvention == null) {
            throw new IllegalArgumentException("dateSignatureConvention est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateSignatureConvention.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateSignatureConvention ne peut pas être dans le futur");
        }
        if (dateAudienceHomologation != null
                && dateAudienceHomologation.isBefore(dateSignatureConvention)) {
            throw new IllegalArgumentException(
                    "dateAudienceHomologation ne peut pas être antérieure à dateSignatureConvention");
        }
    }

    private static List<String> buildMessages(String verdict, boolean conventionComplete,
                                              boolean enfantsMineursCommuns,
                                              boolean audienceNonPlanifiee) {
        List<String> messages = new ArrayList<>();
        messages.add("Procédure DC : requête signée des 2 époux + convention complète (art. 1287 CJ).");
        messages.add("Délai minimum 3 mois entre dépôt et homologation (art. 1294 CJ).");
        messages.add("Compétence : tribunal de la famille (art. 572bis CJ).");
        if (enfantsMineursCommuns) {
            messages.add("Présence d'enfants mineurs : vérification renforcée de l'intérêt de l'enfant (art. 1289 CJ).");
        }
        if (audienceNonPlanifiee && conventionComplete) {
            messages.add("Audience d'homologation non encore planifiée — anticiper le délai 3 mois.");
        }
        switch (verdict) {
            case VERDICT_RECEVABLE -> messages.add(
                    "Toutes les conditions DC réunies — procédure recevable, demander homologation.");
            case VERDICT_IRRECEVABLE -> messages.add(
                    "Conditions DC non réunies — corriger les motifs listés ou bascule vers DDI.");
            case VERDICT_NON_CONCERNE -> messages.add(
                    "Sans consentement mutuel, la voie DC n'est pas applicable — voir DDI (CC art. 229 §2/§3).");
            default -> { /* unreachable */ }
        }
        return messages;
    }

    private static String buildFormule(LocalDate signature, LocalDate homologation,
                                       String verdict, long delaiJours) {
        if (homologation != null) {
            return String.format(
                    "Divorce par consentement mutuel — convention signée le %s, audience d'homologation prévue le %s (%d jours de réflexion). Verdict : %s.",
                    signature, homologation, delaiJours, verdict);
        }
        return String.format(
                "Divorce par consentement mutuel — convention signée le %s, audience non planifiée. Verdict : %s.",
                signature, verdict);
    }
}
