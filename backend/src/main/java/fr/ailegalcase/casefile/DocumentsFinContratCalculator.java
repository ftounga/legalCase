package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-DT-32-01 : moteur de contrôle de conformité des trois documents de fin de contrat
 * obligatoires en droit du travail français.
 *
 * <ul>
 *   <li>Certificat de travail — art. L.1234-19 (remise « lors de l'expiration du contrat » ;
 *       jurisprudence pratique : 7 jours max).</li>
 *   <li>Attestation France Travail (ex-Pôle emploi) — art. R.1234-9 (remise immédiate,
 *       7 jours max en pratique).</li>
 *   <li>Reçu pour solde de tout compte — art. L.1234-20 (signature dans les 30 jours,
 *       contestable 6 mois après signature, al. 2).</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement. La BE a ses propres documents (C4 ONEM, certificat
 * Loi 03/07/1978 art. 22) — feature jumelle backlog.</p>
 *
 * <p>Calcule un score 0-100 et un verdict de risque contentieux pour l'employeur.</p>
 */
public final class DocumentsFinContratCalculator {

    /** Verdict de risque contentieux dérivé du score. */
    public enum VerdictRisqueContentieux {
        FAIBLE,    // ≥ 80
        MOYEN,     // 50-79
        ELEVE      // < 50
    }

    /** Délai jurisprudentiel pratique pour certificat / attestation (jours). */
    private static final int DELAI_REMISE_PRATIQUE_JOURS = 7;
    /** Délai légal de signature du solde de tout compte (jours). */
    private static final int DELAI_SIGNATURE_STC_JOURS = 30;
    /** Fenêtre de contestation du STC après signature (mois). */
    private static final int CONTESTATION_STC_MOIS = 6;

    private static final int SCORE_CERTIFICAT = 35;
    private static final int SCORE_ATTESTATION = 35;
    private static final int SCORE_STC = 30;
    private static final int SCORE_MAX = 100;

    private static final int SEUIL_FAIBLE = 80;
    private static final int SEUIL_MOYEN = 50;

    private static final BigDecimal TRENTE = new BigDecimal("30");

    private static final String BASE_JURIDIQUE =
            "Art. L.1234-19 (certificat de travail) + R.1234-9 (attestation France Travail) "
                    + "+ L.1234-20 (reçu pour solde de tout compte)";

    private DocumentsFinContratCalculator() {}

    /**
     * Calcule la conformité des documents de fin de contrat.
     *
     * @param input  données saisies par l'avocat
     * @param country pays du workspace (FRANCE uniquement)
     * @return résultat structuré (booléens conformité, indemnités retard, score, verdict)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static DocumentsFinContratResult compute(DocumentsFinContratInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (input.dateFinContrat() == null) {
            throw new IllegalArgumentException("Date de fin de contrat requise");
        }
        if (input.salaireMensuelBrutEur() == null || input.salaireMensuelBrutEur().signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel brut requis et strictement positif");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — pendant BE en cours de scoping");
        }

        LocalDate fin = input.dateFinContrat();
        LocalDate today = input.dateReference() != null ? input.dateReference() : LocalDate.now();
        BigDecimal salaire = input.salaireMensuelBrutEur().setScale(2, RoundingMode.HALF_UP);
        BigDecimal indemniteJour = salaire.divide(TRENTE, 2, RoundingMode.HALF_UP);

        // --- Certificat de travail (L.1234-19) ---
        boolean certifRemis = Boolean.TRUE.equals(input.certificatTravailRemis())
                && input.dateCertificatTravail() != null;
        boolean certifDansDelai = certifRemis
                && !input.dateCertificatTravail().isAfter(fin.plusDays(DELAI_REMISE_PRATIQUE_JOURS));
        BigDecimal indemniteCertif = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (certifRemis && !certifDansDelai) {
            long retard = ChronoUnit.DAYS.between(
                    fin.plusDays(DELAI_REMISE_PRATIQUE_JOURS),
                    input.dateCertificatTravail());
            if (retard > 0) {
                indemniteCertif = indemniteJour.multiply(new BigDecimal(retard))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        } else if (!certifRemis) {
            // Non remis du tout = retard depuis fin contrat + 7j jusqu'à today
            long retard = ChronoUnit.DAYS.between(fin.plusDays(DELAI_REMISE_PRATIQUE_JOURS), today);
            if (retard > 0) {
                indemniteCertif = indemniteJour.multiply(new BigDecimal(retard))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        // --- Attestation France Travail (R.1234-9) ---
        boolean attestRemise = Boolean.TRUE.equals(input.attestationFranceTravailRemise())
                && input.dateAttestationFranceTravail() != null;
        boolean attestDansDelai = attestRemise
                && !input.dateAttestationFranceTravail().isAfter(fin.plusDays(DELAI_REMISE_PRATIQUE_JOURS));
        BigDecimal indemniteAttest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (attestRemise && !attestDansDelai) {
            long retard = ChronoUnit.DAYS.between(
                    fin.plusDays(DELAI_REMISE_PRATIQUE_JOURS),
                    input.dateAttestationFranceTravail());
            if (retard > 0) {
                indemniteAttest = indemniteJour.multiply(new BigDecimal(retard))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        } else if (!attestRemise) {
            long retard = ChronoUnit.DAYS.between(fin.plusDays(DELAI_REMISE_PRATIQUE_JOURS), today);
            if (retard > 0) {
                indemniteAttest = indemniteJour.multiply(new BigDecimal(retard))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        // --- Reçu pour solde de tout compte (L.1234-20) ---
        boolean stcSigne = Boolean.TRUE.equals(input.souldeToutCompteSigne())
                && input.dateSouldeToutCompte() != null;
        boolean stcValide;
        boolean stcContestable;
        if (stcSigne) {
            long joursAvantSig = ChronoUnit.DAYS.between(fin, input.dateSouldeToutCompte());
            stcValide = joursAvantSig >= 0 && joursAvantSig <= DELAI_SIGNATURE_STC_JOURS;
            // Contestable = (date signature + 6 mois) > today
            LocalDate finFenetre = input.dateSouldeToutCompte().plusMonths(CONTESTATION_STC_MOIS);
            stcContestable = !today.isAfter(finFenetre);
        } else {
            stcValide = false;
            stcContestable = false; // pas signé donc pas de fenêtre de contestation ouverte
        }

        // --- Score conformité employeur ---
        int score = 0;
        int sanctions = 0;
        List<String> messagesContentieux = new ArrayList<>();
        if (certifDansDelai) {
            score += SCORE_CERTIFICAT;
        } else {
            sanctions++;
            if (!certifRemis) {
                messagesContentieux.add("Certificat de travail non remis — sanction art. R.1238-3 (jusqu'à 750 € amende) + indemnité de retard.");
            } else {
                messagesContentieux.add("Certificat de travail remis hors délai (>" + DELAI_REMISE_PRATIQUE_JOURS + "j) — indemnité de retard due.");
            }
        }
        if (attestDansDelai) {
            score += SCORE_ATTESTATION;
        } else {
            sanctions++;
            if (!attestRemise) {
                messagesContentieux.add("Attestation France Travail non remise — sanction art. R.1238-7 (jusqu'à 1500 € amende) + indemnité (privation droits chômage).");
            } else {
                messagesContentieux.add("Attestation France Travail remise hors délai (>" + DELAI_REMISE_PRATIQUE_JOURS + "j) — indemnité de retard due.");
            }
        }
        // STC : signé valide OU non signé (employeur non en faute) → +30
        if (stcValide || !stcSigne) {
            score += SCORE_STC;
        } else {
            sanctions++;
            messagesContentieux.add("STC signé hors délai 30 jours (L.1234-20) — risque de contestation prolongée.");
        }

        if (score > SCORE_MAX) score = SCORE_MAX;
        VerdictRisqueContentieux verdict = verdictPour(score);

        String formule = construireFormule(certifDansDelai, attestDansDelai, stcValide, stcSigne, score, verdict);
        List<String> messages = construireMessages(certifDansDelai, attestDansDelai,
                stcValide, stcSigne, stcContestable, today);

        return new DocumentsFinContratResult(
                certifDansDelai,
                attestDansDelai,
                stcValide,
                stcContestable,
                sanctions,
                indemniteCertif,
                indemniteAttest,
                score,
                verdict,
                BASE_JURIDIQUE,
                formule,
                messages,
                List.copyOf(messagesContentieux),
                countryNormalized
        );
    }

    private static VerdictRisqueContentieux verdictPour(int score) {
        if (score >= SEUIL_FAIBLE) return VerdictRisqueContentieux.FAIBLE;
        if (score >= SEUIL_MOYEN) return VerdictRisqueContentieux.MOYEN;
        return VerdictRisqueContentieux.ELEVE;
    }

    private static String construireFormule(boolean certif, boolean attest,
                                            boolean stcValide, boolean stcSigne,
                                            int score, VerdictRisqueContentieux verdict) {
        StringBuilder sb = new StringBuilder("3 documents requis : ");
        sb.append("certificat ").append(certif ? "OK" : "KO");
        sb.append(", attestation ").append(attest ? "OK" : "KO");
        sb.append(", STC ");
        if (!stcSigne) {
            sb.append("absent");
        } else {
            sb.append(stcValide ? "OK" : "KO");
        }
        sb.append(" → score ").append(score).append(" = risque contentieux ").append(verdict.name());
        return sb.toString();
    }

    private static List<String> construireMessages(boolean certif, boolean attest,
                                                   boolean stcValide, boolean stcSigne,
                                                   boolean stcContestable, LocalDate today) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Délai légal certificat : remise immédiate à fin de contrat (L.1234-19) — tolérance pratique 7 jours.");
        msgs.add("Attestation France Travail : remise immédiate (R.1234-9) — indispensable pour l'ouverture des droits chômage.");
        msgs.add("STC peut être contesté 6 mois après signature (L.1234-20 al. 2) — au-delà, l'effet libératoire devient irrévocable.");

        if (!certif) {
            msgs.add("Conséquence certificat manquant : indemnité de retard (jurisprudence) + amende contraventionnelle 750 €.");
        }
        if (!attest) {
            msgs.add("Conséquence attestation manquante : amende 1500 € + dommages et intérêts pour privation des droits chômage.");
        }
        if (stcSigne && !stcValide) {
            msgs.add("STC signé hors délai 30 jours : effet libératoire fragilisé, le salarié peut contester sur l'ensemble des sommes.");
        }
        if (stcSigne && stcContestable) {
            msgs.add("STC encore contestable — fenêtre de 6 mois pas écoulée à la date de référence (" + today + ").");
        }
        if (stcSigne && !stcContestable) {
            msgs.add("STC : fenêtre de contestation 6 mois expirée — effet libératoire définitivement acquis.");
        }
        return msgs;
    }
}
