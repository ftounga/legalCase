package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-24-13 : calculateur du <b>rapport à succession</b> (FR — art. 843-863
 * + 919 Cciv).
 *
 * <p>Mécanisme par lequel les héritiers réintègrent fictivement à la
 * succession les donations reçues du défunt de son vivant pour assurer
 * l'égalité entre cohéritiers (art. 843).</p>
 *
 * <h2>Modes de rapport (art. 858)</h2>
 * <ul>
 *   <li>{@code RAPPORT_EN_NATURE} : restitution effective du bien donné
 *       (rare, suppose que le bien existe encore).</li>
 *   <li>{@code RAPPORT_EN_VALEUR} : valeur du bien à l'époque du partage
 *       déduite de la part de l'héritier (mode par défaut, le plus courant).</li>
 *   <li>{@code RAPPORT_EN_MOINS_PRENANT} : héritier prend moins dans le
 *       partage à hauteur de la donation (mode dégradé).</li>
 * </ul>
 *
 * <h2>Évaluation (art. 860)</h2>
 * <p>Valeur du bien donné <strong>au jour du partage</strong>, en l'état où
 * il était <strong>au jour de la donation</strong>.</p>
 *
 * <h2>Exemptions</h2>
 * <ul>
 *   <li>{@code DISPENSÉ} : donation hors part successorale (dispense
 *       expresse, art. 919) — s'impute sur la quotité disponible.</li>
 *   <li>{@code EXEMPT} : frais d'éducation, d'entretien, d'installation
 *       (art. 852) ou donation rémunératoire (art. 851).</li>
 * </ul>
 *
 * <h2>Prescription</h2>
 * <p>Délai = 5 ans (art. 924-1).</p>
 *
 * <p>Outil <b>single-country FRANCE</b> — équivalent BE (CC BE art. 843+,
 * barème différent) → feature jumelle dédiée backlog (F-FA-24-BE).</p>
 */
public final class RapportSuccessionCalculator {

    /** Qualité de l'héritier vis-à-vis de l'obligation de rapport (art. 843). */
    public enum QualiteHeritier {
        /** Héritier descendant (enfant, petit-enfant venant en représentation) — obligé au rapport. */
        DESCENDANT,
        /** Conjoint survivant — obligé au rapport (en l'absence de descendants, voir art. 914-1). */
        CONJOINT_SURVIVANT
    }

    /** Mode de rapport recommandé. */
    public enum ModeRapport {
        /** Restitution effective du bien donné (rare). */
        RAPPORT_EN_NATURE,
        /** Valeur déduite de la part de l'héritier (mode par défaut, art. 860). */
        RAPPORT_EN_VALEUR,
        /** Héritier prend moins dans le partage (mode dégradé, art. 858). */
        RAPPORT_EN_MOINS_PRENANT,
        /** Pas de rapport (verdict EXEMPT/DISPENSÉ/NON_OBLIGÉ). */
        NON_APPLICABLE
    }

    /** Verdict d'obligation de rapport. */
    public enum VerdictObligation {
        /** La donation doit être rapportée à la succession. */
        RAPPORTABLE,
        /** Donation exempte (frais éducation art. 852 / rémunératoire art. 851). */
        EXEMPT,
        /** Dispensée de rapport (donation hors part successorale, art. 919). */
        DISPENSÉ,
        /** Héritier non obligé au rapport (a contrario art. 843). */
        NON_OBLIGÉ
    }

    /** Délai de prescription en années (art. 924-1). */
    public static final int PRESCRIPTION_ANS = 5;

    /** Base juridique consolidée. */
    public static final String BASE_JURIDIQUE = "Art. 843-863 + 919 Cciv";

    private RapportSuccessionCalculator() {}

    /**
     * Calcule le verdict d'obligation, le montant rapportable et le mode de
     * rapport recommandé pour une donation reçue par un cohéritier.
     *
     * @throws IllegalArgumentException si paramètre invalide.
     */
    public static RapportSuccessionResult compute(BigDecimal donationsRecuesEur,
                                                  LocalDate dateDonation,
                                                  BigDecimal valeurAuJourPartage,
                                                  Boolean donationDispenseDeRapport,
                                                  Boolean naturePresumeeNonRapportable,
                                                  QualiteHeritier qualiteHeritier,
                                                  String country) {
        // ---- Validations strictes
        if (donationsRecuesEur == null || donationsRecuesEur.signum() <= 0) {
            throw new IllegalArgumentException("donationsRecuesEur doit être > 0");
        }
        if (valeurAuJourPartage == null || valeurAuJourPartage.signum() <= 0) {
            throw new IllegalArgumentException("valeurAuJourPartage doit être > 0");
        }
        if (dateDonation == null) {
            throw new IllegalArgumentException("dateDonation requise");
        }
        LocalDate today = LocalDate.now();
        if (dateDonation.isAfter(today)) {
            throw new IllegalArgumentException("dateDonation ne peut être future");
        }
        if (qualiteHeritier == null) {
            throw new IllegalArgumentException("qualiteHeritier requise");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CC BE art. 843+ avec barème différent)"
                            + " sera traité dans une feature jumelle dédiée (F-FA-24-BE).");
        }

        boolean dispense = Boolean.TRUE.equals(donationDispenseDeRapport);
        boolean nonRapportable = Boolean.TRUE.equals(naturePresumeeNonRapportable);

        List<String> messages = new ArrayList<>();

        // ============================================================
        // 1. Détermination du verdict d'obligation
        // ============================================================
        VerdictObligation verdict;
        if (dispense) {
            verdict = VerdictObligation.DISPENSÉ;
            messages.add("Donation déclarée hors part successorale (dispense expresse "
                    + "art. 919 Cciv) : DISPENSÉE de rapport. Elle s'impute sur la "
                    + "quotité disponible (cf. simulateur réserve héréditaire SF-07 — "
                    + "vérifier le risque d'atteinte à la réserve).");
        } else if (nonRapportable) {
            verdict = VerdictObligation.EXEMPT;
            messages.add("Donation présumée non rapportable (frais d'éducation, "
                    + "d'entretien, d'installation art. 852 Cciv ou donation "
                    + "rémunératoire art. 851 Cciv) : EXEMPTE de rapport.");
        } else {
            // Tous les enums actuels (DESCENDANT/CONJOINT_SURVIVANT) sont obligés au rapport
            verdict = VerdictObligation.RAPPORTABLE;
            messages.add("Héritier de qualité " + qualiteHeritier.name()
                    + " : OBLIGÉ au rapport (art. 843 Cciv). La donation est "
                    + "réintégrée fictivement à la masse à partager pour assurer "
                    + "l'égalité entre cohéritiers.");
        }

        // ============================================================
        // 2. Mode de rapport recommandé (art. 858 + 860)
        // ============================================================
        ModeRapport mode;
        BigDecimal montantRapportable;
        if (verdict == VerdictObligation.RAPPORTABLE) {
            // Mode par défaut = RAPPORT_EN_VALEUR (art. 860 — évaluation au jour du partage)
            mode = ModeRapport.RAPPORT_EN_VALEUR;
            montantRapportable = valeurAuJourPartage.setScale(2, RoundingMode.HALF_UP);
            messages.add("Mode recommandé : RAPPORT_EN_VALEUR (art. 858 + 860 Cciv) — "
                    + "valeur retenue au jour du partage (" + montantRapportable.toPlainString()
                    + " €), en l'état où le bien était au jour de la donation. "
                    + "Mode alternatif RAPPORT_EN_NATURE possible si le bien donné est "
                    + "encore détenu et identifiable. Mode dégradé RAPPORT_EN_MOINS_PRENANT "
                    + "si l'héritier ne peut pas restituer.");
            // Comparaison nominal vs jour partage pour traçabilité
            int cmp = valeurAuJourPartage.compareTo(donationsRecuesEur);
            if (cmp > 0) {
                messages.add("Plus-value depuis la donation : valeur jour partage ("
                        + valeurAuJourPartage.toPlainString() + " €) > nominal ("
                        + donationsRecuesEur.toPlainString() + " €) — la valeur "
                        + "actuelle est retenue (art. 860 al. 1).");
            } else if (cmp < 0) {
                messages.add("Moins-value depuis la donation : valeur jour partage ("
                        + valeurAuJourPartage.toPlainString() + " €) < nominal ("
                        + donationsRecuesEur.toPlainString() + " €) — la valeur "
                        + "actuelle est retenue (art. 860 al. 1).");
            }
        } else {
            mode = ModeRapport.NON_APPLICABLE;
            montantRapportable = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            messages.add("Aucun rapport dû — mode NON_APPLICABLE, montant rapportable = 0 €.");
        }

        // ============================================================
        // 3. Score d'éligibilité
        // ============================================================
        int score = computeScore(verdict);

        // ============================================================
        // 4. Formule
        // ============================================================
        String formule = String.format(Locale.ROOT,
                "donation nominale = %s € | valeur jour partage = %s € | "
                        + "dispense art. 919 = %s | nature non rapportable art. 851/852 = %s | "
                        + "qualité = %s → verdict %s | mode %s | montant rapportable = %s € | "
                        + "prescription %d ans (art. 924-1) | score %d",
                donationsRecuesEur.toPlainString(),
                valeurAuJourPartage.toPlainString(),
                dispense, nonRapportable,
                qualiteHeritier.name(),
                verdict.name(), mode.name(),
                montantRapportable.toPlainString(),
                PRESCRIPTION_ANS,
                score);

        messages.add("Délai de prescription de l'action en rapport : "
                + PRESCRIPTION_ANS + " ans (art. 924-1 Cciv).");
        messages.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return new RapportSuccessionResult(
                donationsRecuesEur.setScale(2, RoundingMode.HALF_UP),
                dateDonation,
                valeurAuJourPartage.setScale(2, RoundingMode.HALF_UP),
                dispense,
                nonRapportable,
                qualiteHeritier,
                countryNormalized,
                verdict,
                mode,
                montantRapportable,
                PRESCRIPTION_ANS,
                score,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static int computeScore(VerdictObligation verdict) {
        return switch (verdict) {
            case RAPPORTABLE -> 100;
            case EXEMPT -> 70;
            case DISPENSÉ -> 60;
            case NON_OBLIGÉ -> 40;
        };
    }
}
