package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-214-15 : analyseur distinguant les droits attachés au récépissé (séjour +
 * travail) vs l'attestation de prolongation d'instruction (séjour seul, PAS de
 * droit au travail). Outil single-country FR.
 *
 * <p>Source juridique :
 * <ul>
 *   <li>R. 311-4 CESEDA — récépissé autorisant le séjour et, le cas échéant,
 *       le travail (selon la mention portée).</li>
 *   <li>R. 311-6 CESEDA — attestation de prolongation d'instruction : autorise
 *       le séjour seulement, sans ouvrir de droit au travail.</li>
 *   <li>L. 8253-1 Code du travail — sanctions pesant sur l'employeur qui
 *       emploie un étranger sans autorisation de travail.</li>
 * </ul>
 *
 * <p>Confusion fréquente source de sanctions employeur : l'attestation de
 * prolongation est souvent prise pour un récépissé alors qu'elle n'ouvre PAS
 * de droit au travail.</p>
 */
public final class RecepisseAttestationAnalyzer {

    public static final String TYPE_RECEPISSE = "RECEPISSE";
    public static final String TYPE_ATTESTATION_PROLONGATION = "ATTESTATION_PROLONGATION";
    public static final String TYPE_INCONNU = "INCONNU";

    public static final Set<String> TYPES_VALIDES = Set.of(
            TYPE_RECEPISSE, TYPE_ATTESTATION_PROLONGATION, TYPE_INCONNU);

    private static final String BASE_JURIDIQUE =
            "CESEDA R. 311-4 (récépissé : séjour + travail) ; "
            + "R. 311-6 (attestation de prolongation : séjour seul, sans travail) ; "
            + "Code du travail L. 8253-1 (sanctions employeur emploi sans autorisation)";

    private RecepisseAttestationAnalyzer() {}

    /**
     * Analyse les droits attachés au document de séjour transitoire.
     *
     * @param typeDocument               RECEPISSE | ATTESTATION_PROLONGATION | INCONNU
     * @param dateDelivrance             date de délivrance du document (peut être null)
     * @param dateExpiration             date d'expiration du document (peut être null)
     * @param mentionAutorisationTravail mention « autorise à travailler » portée sur la
     *                                   pièce, optionnelle (null si non renseignée)
     * @return résultat de l'analyse
     */
    public static RecepisseAttestationResult analyze(String typeDocument,
                                                     LocalDate dateDelivrance,
                                                     LocalDate dateExpiration,
                                                     Boolean mentionAutorisationTravail) {
        validateInputs(typeDocument, dateDelivrance, dateExpiration);

        // Droit au séjour : toujours ouvert, que ce soit un récépissé ou une
        // attestation de prolongation (R. 311-4 / R. 311-6).
        boolean droitSejour = true;

        // Droit au travail : ouvert par le récépissé (R. 311-4) ; fermé par
        // l'attestation de prolongation (R. 311-6). INCONNU → fermé par prudence.
        boolean droitTravail = switch (typeDocument) {
            case TYPE_RECEPISSE -> true;
            case TYPE_ATTESTATION_PROLONGATION -> false;
            default -> false; // INCONNU
        };

        // Risque employeur : caractérisé quand le document n'ouvre PAS de droit
        // au travail mais est présenté comme tel — cas de l'attestation de
        // prolongation (sanctions L. 8253-1).
        boolean risqueEmployeur = TYPE_ATTESTATION_PROLONGATION.equals(typeDocument);

        Long dureeValiditeJours = null;
        if (dateDelivrance != null && dateExpiration != null) {
            dureeValiditeJours = ChronoUnit.DAYS.between(dateDelivrance, dateExpiration);
        }

        List<String> recommandations = buildRecommandations(
                typeDocument, mentionAutorisationTravail);

        return new RecepisseAttestationResult(
                typeDocument,
                dateDelivrance != null ? dateDelivrance.toString() : null,
                dateExpiration != null ? dateExpiration.toString() : null,
                mentionAutorisationTravail,
                droitSejour,
                droitTravail,
                dureeValiditeJours,
                risqueEmployeur,
                recommandations,
                BASE_JURIDIQUE);
    }

    private static List<String> buildRecommandations(String typeDocument,
                                                     Boolean mentionAutorisationTravail) {
        List<String> recommandations = new ArrayList<>();
        switch (typeDocument) {
            case TYPE_RECEPISSE -> {
                recommandations.add("Le récépissé autorise le séjour et le travail (R. 311-4 CESEDA) — "
                        + "vérifier la mention exacte portée sur la pièce.");
                if (Boolean.FALSE.equals(mentionAutorisationTravail)) {
                    recommandations.add("La pièce ne porte pas la mention « autorise à travailler » : "
                            + "certains récépissés (1re demande hors renouvellement) n'ouvrent pas de "
                            + "droit au travail — confirmer auprès de la préfecture.");
                }
            }
            case TYPE_ATTESTATION_PROLONGATION -> {
                recommandations.add("L'attestation de prolongation d'instruction autorise le séjour mais "
                        + "PAS le travail (R. 311-6 CESEDA).");
                recommandations.add("Risque employeur : l'emploi sur la base de cette attestation expose "
                        + "aux sanctions L. 8253-1 Code du travail — alerter l'employeur.");
                recommandations.add("Vérifier le type de procédure pour obtenir, si possible, un récépissé "
                        + "ouvrant le droit au travail.");
            }
            default -> { // INCONNU
                recommandations.add("Type de document indéterminé : identifier précisément la pièce "
                        + "(récépissé R. 311-4 vs attestation de prolongation R. 311-6).");
                recommandations.add("Examiner la mention portée sur le document et la nature de la procédure "
                        + "pour déterminer si le travail est autorisé.");
                recommandations.add("Par prudence, considérer le travail comme NON autorisé tant que la "
                        + "nature exacte de la pièce n'est pas confirmée.");
            }
        }
        return recommandations;
    }

    private static void validateInputs(String typeDocument,
                                       LocalDate dateDelivrance,
                                       LocalDate dateExpiration) {
        if (typeDocument == null || !TYPES_VALIDES.contains(typeDocument)) {
            throw new IllegalArgumentException(
                    "typeDocument inconnu — valeurs attendues : " + TYPES_VALIDES);
        }
        if (dateDelivrance != null && dateExpiration != null
                && dateExpiration.isBefore(dateDelivrance)) {
            throw new IllegalArgumentException(
                    "dateExpiration ne peut pas être antérieure à dateDelivrance");
        }
    }
}
