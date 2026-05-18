package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-217-08 : analyseur de la pension alimentaire entre ex-époux belge
 * (CC art. 301).
 *
 * <p>Règles appliquées :</p>
 * <ul>
 *   <li>en divorce par consentement mutuel (DC), la pension relève de la
 *       convention préalable des parties — l'outil ne fixe pas de barème ;</li>
 *   <li>en divorce pour désunion irrémédiable (DDI), une pension peut être
 *       accordée par le Tribunal de la famille à l'époux dans le besoin, sauf
 *       faute grave de celui-ci (CC art. 301 §§1-2) ;</li>
 *   <li>la durée de la pension ne peut excéder la durée du mariage
 *       (CC art. 301 §4) ;</li>
 *   <li>le montant couvre l'état de besoin et reste plafonné au tiers des
 *       revenus du débiteur (règle légale — CC art. 301 §3).</li>
 * </ul>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. La pension belge (révisable, plafonnée
 * en durée) est juridiquement distincte de la prestation compensatoire française
 * (capital forfaitaire — F-FA-01) — pas de réutilisation possible.</p>
 *
 * <p>Le plafond du tiers des revenus du débiteur est une <b>règle légale</b>
 * codifiée à l'art. 301 §3 du Code civil belge (la pension ne peut excéder le
 * tiers des revenus du conjoint débiteur) — il ne s'agit pas d'une simple
 * orientation jurisprudentielle.</p>
 *
 * <p><b>Validation juridique requise</b> : CC art. 301 (§§1-4) reflète l'état du
 * droit connu du modèle et doit être relu par un avocat belge avant mise en
 * production. Le contenu juridique est centralisé ici (source unique de vérité).</p>
 */
public final class ContributionConjointBeCalculator {

    /** Verdict de l'analyse de la pension alimentaire entre ex-époux. */
    public enum Verdict {
        PENSION_DUE,
        PENSION_NON_DUE,
        PENSION_CONVENTIONNELLE,
        DONNEES_INSUFFISANTES
    }

    /** Type de divorce prononcé. */
    public enum TypeDivorce {
        DC,
        DDI
    }

    /** Code structuré d'un motif d'exclusion de la pension. */
    public enum MotifExclusion {
        FAUTE_GRAVE_CREANCIER,
        CREANCIER_HORS_BESOIN,
        RENONCIATION_CONVENTIONNELLE
    }

    /** Durée maximale plausible d'un mariage acceptée en saisie (années). */
    private static final int DUREE_MARIAGE_MAX = 80;
    private static final int MOIS_PAR_AN = 12;

    private ContributionConjointBeCalculator() {}

    /**
     * Analyse le droit à une pension alimentaire entre ex-époux.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static ContributionConjointBeResult compute(ContributionConjointBeInput input,
                                                       String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Pension alimentaire entre ex-époux — outil disponible uniquement en BELGIQUE");
        }
        validate(input);

        BigDecimal revenuCreancier = input.revenuMensuelCreancier();
        BigDecimal revenuDebiteur = input.revenuMensuelDebiteur();
        BigDecimal plafondTiers = revenuDebiteur.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

        List<String> detail = new ArrayList<>();
        List<MotifExclusion> motifs = new ArrayList<>();

        // Cas 1 — divorce par consentement mutuel : pension conventionnelle.
        if (input.typeDivorce() == TypeDivorce.DC) {
            detail.add("Divorce par consentement mutuel : la pension relève de la convention "
                    + "préalable des parties (CJ art. 1287) — l'outil ne fixe pas de barème.");
            return new ContributionConjointBeResult(
                    Verdict.PENSION_CONVENTIONNELLE,
                    0, BigDecimal.ZERO, plafondTiers, List.of(),
                    detail, basesJuridiques(Verdict.PENSION_CONVENTIONNELLE),
                    List.of("La pension entre ex-époux a été réglée dans la convention préalable "
                            + "de divorce — se reporter à cette convention."),
                    countryNormalized);
        }

        // Cas 2 — divorce pour désunion irrémédiable : motifs d'exclusion.
        if (Boolean.TRUE.equals(input.renonciationPensionConvention())) {
            motifs.add(MotifExclusion.RENONCIATION_CONVENTIONNELLE);
        }
        if (Boolean.TRUE.equals(input.fauteGraveCreancier())) {
            motifs.add(MotifExclusion.FAUTE_GRAVE_CREANCIER);
        }
        if (!Boolean.TRUE.equals(input.creancierEnEtatDeBesoin())) {
            motifs.add(MotifExclusion.CREANCIER_HORS_BESOIN);
        }

        if (!motifs.isEmpty()) {
            detail.add("Divorce pour désunion irrémédiable : un ou plusieurs motifs excluent "
                    + "le droit à la pension alimentaire (CC art. 301 §2).");
            return new ContributionConjointBeResult(
                    Verdict.PENSION_NON_DUE,
                    0, BigDecimal.ZERO, plafondTiers, List.copyOf(motifs),
                    detail, basesJuridiques(Verdict.PENSION_NON_DUE),
                    construireMessages(Verdict.PENSION_NON_DUE, motifs),
                    countryNormalized);
        }

        // Cas 3 — DDI, créancier dans le besoin, pas de faute : revenus nuls ?
        if (revenuCreancier.signum() == 0 && revenuDebiteur.signum() == 0) {
            detail.add("Les revenus des deux ex-époux sont nuls : le besoin du créancier et le "
                    + "plafond du débiteur ne peuvent être estimés.");
            return new ContributionConjointBeResult(
                    Verdict.DONNEES_INSUFFISANTES,
                    0, BigDecimal.ZERO, BigDecimal.ZERO, List.of(),
                    detail, basesJuridiques(Verdict.DONNEES_INSUFFISANTES),
                    List.of("Compléter les revenus des deux ex-époux pour estimer la pension."),
                    countryNormalized);
        }

        // Cas 4 — PENSION_DUE : durée et montant.
        int dureeMaxMois = input.dureeMariageAnnees() * MOIS_PAR_AN;
        BigDecimal besoin = revenuDebiteur.subtract(revenuCreancier).max(BigDecimal.ZERO);
        BigDecimal montant = besoin.min(plafondTiers).setScale(2, RoundingMode.HALF_UP);

        detail.add("Divorce pour désunion irrémédiable : une pension peut être accordée par le "
                + "Tribunal de la famille (CC art. 301 §1).");
        detail.add("Durée maximale légale de la pension = durée du mariage : "
                + input.dureeMariageAnnees() + " an(s) = " + dureeMaxMois + " mois (CC art. 301 §4).");
        detail.add("Besoin du créancier estimé : écart de revenus "
                + money(revenuDebiteur) + " € − " + money(revenuCreancier) + " € = "
                + money(besoin) + " €.");
        detail.add("Montant indicatif retenu = min(besoin, plafond légal du tiers des revenus "
                + "du débiteur " + plafondTiers + " € — CC art. 301 §3) : " + montant + " €.");

        List<String> messages = construireMessages(Verdict.PENSION_DUE, motifs);
        if (Boolean.TRUE.equals(input.degradationEconomiqueLieeAuMariage())) {
            messages.add("Une dégradation économique liée aux choix du mariage est invoquée : "
                    + "elle renforce l'argumentaire mais n'est pas un critère de calcul autonome.");
        }

        return new ContributionConjointBeResult(
                Verdict.PENSION_DUE,
                dureeMaxMois, montant, plafondTiers, List.of(),
                detail, basesJuridiques(Verdict.PENSION_DUE), messages,
                countryNormalized);
    }

    private static void validate(ContributionConjointBeInput in) {
        if (in.typeDivorce() == null) {
            throw new IllegalArgumentException("typeDivorce est requis");
        }
        requireFlag(in.renonciationPensionConvention(), "renonciationPensionConvention");
        requireFlag(in.creancierEnEtatDeBesoin(), "creancierEnEtatDeBesoin");
        requireFlag(in.fauteGraveCreancier(), "fauteGraveCreancier");
        requireFlag(in.degradationEconomiqueLieeAuMariage(), "degradationEconomiqueLieeAuMariage");
        if (in.dureeMariageAnnees() < 0 || in.dureeMariageAnnees() > DUREE_MARIAGE_MAX) {
            throw new IllegalArgumentException(
                    "La durée du mariage doit être comprise entre 0 et " + DUREE_MARIAGE_MAX + " ans");
        }
        if (in.revenuMensuelCreancier() == null || in.revenuMensuelDebiteur() == null) {
            throw new IllegalArgumentException("Les revenus des deux ex-époux sont requis");
        }
        if (in.revenuMensuelCreancier().signum() < 0 || in.revenuMensuelDebiteur().signum() < 0) {
            throw new IllegalArgumentException("Les revenus ne peuvent être négatifs");
        }
        if (in.commentaire() != null && in.commentaire().length() > 1000) {
            throw new IllegalArgumentException("Le commentaire ne peut dépasser 1000 caractères");
        }
    }

    private static void requireFlag(Boolean value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " est requis");
        }
    }

    private static List<String> basesJuridiques(Verdict verdict) {
        List<String> bases = new ArrayList<>();
        if (verdict == Verdict.PENSION_CONVENTIONNELLE) {
            bases.add("CJ art. 1287 (convention préalable de divorce par consentement mutuel)");
            return bases;
        }
        bases.add("CC art. 301 §1 (pension alimentaire après divorce pour désunion irrémédiable)");
        bases.add("CC art. 301 §2 (refus de pension en cas de faute grave du créancier)");
        if (verdict == Verdict.PENSION_DUE) {
            bases.add("CC art. 301 §3 (montant — couverture de l'état de besoin)");
            bases.add("CC art. 301 §4 (durée — ne peut excéder la durée du mariage)");
        }
        return bases;
    }

    private static List<String> construireMessages(Verdict verdict, List<MotifExclusion> motifs) {
        List<String> msgs = new ArrayList<>();
        switch (verdict) {
            case PENSION_DUE -> msgs.add(
                    "Estimation indicative — le Tribunal de la famille apprécie souverainement le "
                            + "montant et la durée selon les besoins et les facultés des parties.");
            case PENSION_NON_DUE -> {
                for (MotifExclusion m : motifs) {
                    switch (m) {
                        case RENONCIATION_CONVENTIONNELLE -> msgs.add(
                                "Une renonciation à la pension a été convenue : aucune pension "
                                        + "n'est due sauf remise en cause de cette renonciation.");
                        case FAUTE_GRAVE_CREANCIER -> msgs.add(
                                "Une faute grave du créancier est invoquée : le Tribunal de la "
                                        + "famille peut refuser la pension (CC art. 301 §2).");
                        case CREANCIER_HORS_BESOIN -> msgs.add(
                                "Le créancier n'est pas dans le besoin : la condition de l'état de "
                                        + "besoin n'est pas remplie.");
                    }
                }
            }
            default -> { /* PENSION_CONVENTIONNELLE / DONNEES_INSUFFISANTES gérés ailleurs */ }
        }
        return msgs;
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
