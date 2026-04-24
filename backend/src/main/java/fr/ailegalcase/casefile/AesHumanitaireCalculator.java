package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-09-03 : calculateur pour l'Admission Exceptionnelle au Séjour (AES)
 * voie humanitaire — article L.435-2 du CESEDA.
 *
 * <p>L.435-2 dispose que le préfet peut, à titre exceptionnel, admettre au séjour
 * un étranger pour motifs humanitaires, après consultation de la commission du
 * titre de séjour (L.432-14). Le régime repose sur un faisceau d'indices.
 *
 * <p>Motifs humanitaires fréquents :
 * <ul>
 *   <li>Risques personnels en cas de retour au pays (même sans asile)</li>
 *   <li>Isolement total (pas de famille au pays)</li>
 *   <li>Victime de violences (femmes, minorités)</li>
 *   <li>Victime de traite / proxénétisme (connexe L.425-1)</li>
 *   <li>Situation médicale précaire non couverte par L.425-9 (étranger malade)</li>
 * </ul>
 *
 * <p>Délai d'instruction : 6 mois à compter du dépôt (silence vaut rejet).
 *
 * <p>Outil <b>single-country FR</b> — pas d'équivalent standard en droit belge.
 */
public final class AesHumanitaireCalculator {

    /** Délai d'instruction de la demande (mois). */
    public static final int DELAI_INSTRUCTION_MOIS = 6;

    private static final String BASE_JURIDIQUE =
            "Art. L.435-2 CESEDA + L.432-14 (commission titre séjour)";

    private AesHumanitaireCalculator() {
    }

    public static AesHumanitaireResult compute(LocalDate dateEntreeFrance,
                                               MotifHumanitaire motifHumanitaireDominant,
                                               boolean preuvesMedicales,
                                               boolean preuvesViolencesOuTraite,
                                               boolean demandeAsileDeposeeEtRejetee,
                                               boolean commissionTitreSejourSaisie,
                                               boolean menaceOrdrePublic,
                                               LocalDate dateDepotDemande) {
        return compute(dateEntreeFrance, motifHumanitaireDominant,
                preuvesMedicales, preuvesViolencesOuTraite,
                demandeAsileDeposeeEtRejetee, commissionTitreSejourSaisie,
                menaceOrdrePublic, dateDepotDemande,
                Clock.system(ZoneId.of("Europe/Paris")));
    }

    public static AesHumanitaireResult compute(LocalDate dateEntreeFrance,
                                               MotifHumanitaire motifHumanitaireDominant,
                                               boolean preuvesMedicales,
                                               boolean preuvesViolencesOuTraite,
                                               boolean demandeAsileDeposeeEtRejetee,
                                               boolean commissionTitreSejourSaisie,
                                               boolean menaceOrdrePublic,
                                               LocalDate dateDepotDemande,
                                               Clock clock) {
        validateInputs(dateEntreeFrance, motifHumanitaireDominant, dateDepotDemande, clock);

        boolean motifEligible = motifHumanitaireDominant != MotifHumanitaire.AUTRE_HUMANITAIRE
                || preuvesMedicales || preuvesViolencesOuTraite;

        boolean preuvesAdaptees = computePreuvesAdaptees(motifHumanitaireDominant,
                preuvesMedicales, preuvesViolencesOuTraite);

        boolean commissionRequise = computeCommissionRequise(motifHumanitaireDominant);

        boolean pasMenace = !menaceOrdrePublic;

        int scoreGlobal = computeScore(motifEligible, preuvesAdaptees,
                pasMenace, commissionTitreSejourSaisie, commissionRequise,
                demandeAsileDeposeeEtRejetee, motifHumanitaireDominant);

        String verdict = computeVerdict(scoreGlobal, pasMenace);

        LocalDate dateExpirationInstruction = dateDepotDemande != null
                ? dateDepotDemande.plusMonths(DELAI_INSTRUCTION_MOIS)
                : null;

        List<String> criteresNonRemplis = buildCriteresNonRemplis(
                motifEligible, preuvesAdaptees, pasMenace,
                motifHumanitaireDominant, commissionRequise, commissionTitreSejourSaisie);

        List<String> messages = buildMessages(motifHumanitaireDominant,
                preuvesAdaptees, commissionRequise, commissionTitreSejourSaisie,
                demandeAsileDeposeeEtRejetee);

        String formule = buildFormule(scoreGlobal, verdict, motifHumanitaireDominant,
                pasMenace, dateExpirationInstruction);

        return new AesHumanitaireResult(
                dateEntreeFrance,
                motifHumanitaireDominant,
                preuvesMedicales,
                preuvesViolencesOuTraite,
                demandeAsileDeposeeEtRejetee,
                commissionTitreSejourSaisie,
                menaceOrdrePublic,
                dateDepotDemande,
                motifEligible,
                preuvesAdaptees,
                commissionRequise,
                pasMenace,
                scoreGlobal,
                verdict,
                criteresNonRemplis,
                dateExpirationInstruction,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateEntreeFrance,
                                       MotifHumanitaire motif,
                                       LocalDate dateDepotDemande,
                                       Clock clock) {
        if (dateEntreeFrance == null) {
            throw new IllegalArgumentException("dateEntreeFrance est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateEntreeFrance.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateEntreeFrance ne peut pas être dans le futur");
        }
        if (motif == null) {
            throw new IllegalArgumentException("motifHumanitaireDominant est requis");
        }
        if (dateDepotDemande != null && dateDepotDemande.isBefore(dateEntreeFrance)) {
            throw new IllegalArgumentException(
                    "dateDepotDemande ne peut pas être antérieure à dateEntreeFrance");
        }
    }

    private static boolean computePreuvesAdaptees(MotifHumanitaire motif,
                                                  boolean preuvesMedicales,
                                                  boolean preuvesViolencesOuTraite) {
        return switch (motif) {
            case SITUATION_MEDICALE_PRECAIRE_HORS_L425_9 -> preuvesMedicales;
            case VICTIME_VIOLENCES, VICTIME_TRAITE -> preuvesViolencesOuTraite;
            case RISQUES_AU_RETOUR, ISOLEMENT_TOTAL -> true;
            case AUTRE_HUMANITAIRE -> preuvesMedicales || preuvesViolencesOuTraite;
        };
    }

    private static boolean computeCommissionRequise(MotifHumanitaire motif) {
        return switch (motif) {
            case SITUATION_MEDICALE_PRECAIRE_HORS_L425_9,
                 VICTIME_VIOLENCES,
                 VICTIME_TRAITE,
                 RISQUES_AU_RETOUR -> true;
            case ISOLEMENT_TOTAL, AUTRE_HUMANITAIRE -> false;
        };
    }

    private static int computeScore(boolean motifEligible,
                                    boolean preuvesAdaptees,
                                    boolean pasMenace,
                                    boolean commissionSaisie,
                                    boolean commissionRequise,
                                    boolean asileRejete,
                                    MotifHumanitaire motif) {
        if (!pasMenace) {
            return 0;
        }
        int score = 0;
        if (motifEligible) score += 25;
        if (preuvesAdaptees) score += 30;
        if (commissionSaisie) score += 15;
        else if (commissionRequise) score -= 5;
        if (asileRejete) score += 10;
        score += bonusMotif(motif);
        return Math.max(0, Math.min(100, score));
    }

    private static int bonusMotif(MotifHumanitaire motif) {
        return switch (motif) {
            case VICTIME_TRAITE -> 20;
            case VICTIME_VIOLENCES, SITUATION_MEDICALE_PRECAIRE_HORS_L425_9 -> 15;
            case RISQUES_AU_RETOUR, ISOLEMENT_TOTAL -> 10;
            case AUTRE_HUMANITAIRE -> 0;
        };
    }

    private static String computeVerdict(int score, boolean pasMenace) {
        if (!pasMenace) return "FAIBLE";
        if (score >= 70) return "ELEVEE";
        if (score >= 45) return "MOYENNE";
        return "FAIBLE";
    }

    private static List<String> buildCriteresNonRemplis(boolean motifEligible,
                                                        boolean preuvesAdaptees,
                                                        boolean pasMenace,
                                                        MotifHumanitaire motif,
                                                        boolean commissionRequise,
                                                        boolean commissionSaisie) {
        List<String> criteres = new ArrayList<>();
        if (!motifEligible) {
            criteres.add("Motif humanitaire insuffisamment caractérisé");
        }
        if (!preuvesAdaptees) {
            criteres.add("Preuves non adaptées au motif « " + motif.name() + " »");
        }
        if (!pasMenace) {
            criteres.add("Menace ordre public présumée");
        }
        if (commissionRequise && !commissionSaisie) {
            criteres.add("Commission du titre de séjour (L.432-14) non saisie");
        }
        return criteres;
    }

    private static List<String> buildMessages(MotifHumanitaire motif,
                                              boolean preuvesAdaptees,
                                              boolean commissionRequise,
                                              boolean commissionSaisie,
                                              boolean asileRejete) {
        List<String> messages = new ArrayList<>();
        messages.add("Voie exceptionnelle L.435-2 : faisceau d'indices, décision "
                + "discrétionnaire du préfet. Délai d'instruction 6 mois (silence vaut rejet).");

        switch (motif) {
            case RISQUES_AU_RETOUR -> {
                messages.add("Constituer attestations, articles de presse, rapports "
                        + "d'ONG, témoignages étayant la crainte personnelle de retour.");
                if (!asileRejete) {
                    messages.add("Alternative à envisager : demande d'asile OFPRA/CNDA "
                            + "si crainte de persécution correspond aux critères Convention Genève.");
                }
            }
            case ISOLEMENT_TOTAL -> messages.add("Constituer actes d'état civil, "
                    + "attestations de décès des proches, enquête sociale prouvant "
                    + "l'absence totale d'attaches familiales au pays.");
            case VICTIME_VIOLENCES -> messages.add("Constituer main courante, plaintes, "
                    + "témoignages, certificats médicaux, orientation association "
                    + "type CIDFF / FNSF / Voix de femmes.");
            case VICTIME_TRAITE -> {
                messages.add("Constituer signalement autorités, plainte, procès-verbaux, "
                        + "certificats associations spécialisées (Ac.Sé, Amicale du Nid).");
                messages.add("Alternative à privilégier : protection L.425-1 CESEDA "
                        + "(titre spécifique victime traite) si coopération avec la justice.");
            }
            case SITUATION_MEDICALE_PRECAIRE_HORS_L425_9 -> {
                messages.add("Constituer certificats médicaux détaillés, avis médical "
                        + "OFII, preuves d'impossibilité de traitement effectif au pays.");
                messages.add("Alternative à vérifier : titre étranger malade L.425-9 "
                        + "CESEDA (indisponibilité effective du traitement au pays).");
            }
            case AUTRE_HUMANITAIRE -> messages.add("Caractériser précisément le motif "
                    + "humanitaire par un dossier circonstancié — risque élevé de rejet "
                    + "si le motif reste diffus.");
        }

        if (!preuvesAdaptees) {
            messages.add("Preuves actuelles insuffisantes pour le motif retenu — "
                    + "renforcer le dossier avant dépôt.");
        }
        if (commissionRequise) {
            messages.add("Saisir la commission du titre de séjour (L.432-14) : "
                    + "délai de convocation environ 2 mois, consultation fortement "
                    + "recommandée pour ce type de dossier.");
        }
        if (commissionRequise && !commissionSaisie) {
            messages.add("Commission non saisie — à faire en priorité.");
        }
        return messages;
    }

    private static String buildFormule(int score,
                                       String verdict,
                                       MotifHumanitaire motif,
                                       boolean pasMenace,
                                       LocalDate dateExpirationInstruction) {
        String suffixDelai = dateExpirationInstruction != null
                ? " — délai d'instruction jusqu'au " + dateExpirationInstruction
                        + " (silence vaut rejet)"
                : "";
        if (!pasMenace) {
            return "AES voie humanitaire (L.435-2) : menace ordre public présumée — "
                    + "demande vouée au rejet" + suffixDelai + ".";
        }
        return String.format(
                "AES voie humanitaire (L.435-2) — motif « %s » : score %d/100, "
                        + "probabilité d'acceptation %s%s.",
                motif.name(), score, verdict, suffixDelai);
    }
}
