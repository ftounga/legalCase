package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-217-11 : arbre décisionnel de la dévolution légale et de la réserve
 * héréditaire belge post-réforme du 31/07/2017 (Livre 4 CC BE réformé,
 * en vigueur 01/09/2018).
 *
 * <p><b>Point central de la réforme</b> : la réserve globale est désormais
 * <b>fixe à 1/2 de la masse</b> en présence de descendants, quelle que soit le
 * nombre d'enfants — différence majeure avec le droit français qui conserve
 * 1/2-2/3-3/4 selon le nombre d'enfants (CC art. 913 nouveau — à vérifier).
 * La réserve des ascendants est supprimée. Le cohabitant légal n'est pas
 * réservataire.</p>
 *
 * <p><b>Pays</b> : BELGIQUE uniquement. Les Calculators FR sur la réserve ne
 * sont pas réutilisables (mécaniques juridiquement distinctes).</p>
 *
 * <p><b>Validation juridique requise</b> : les articles cités (CC art. 913
 * nouveau, 745bis, 740, 1477, 920+ — tous taggés `(à vérifier)`) reflètent
 * l'état du droit connu du modèle et doivent être relus par un avocat belge
 * avant mise en production. Pour les successions ouvertes avant le 01/09/2018,
 * le droit antérieur s'applique — l'outil V1 calcule selon la réforme et émet
 * un message d'avertissement explicite (arbitrage produit V1).</p>
 */
public final class SuccessionBeDevolutionReserveCalculator {

    /** Date d'entrée en vigueur de la réforme du 31/07/2017. */
    public static final LocalDate DATE_REFORME = LocalDate.of(2018, 9, 1);

    /** Borne supérieure du nombre d'enfants. */
    private static final int NB_ENFANTS_MAX = 20;

    /** Longueur maximale du commentaire libre. */
    private static final int COMMENTAIRE_MAX = 1000;

    /** Verdict de qualification de la succession. */
    public enum Verdict {
        DEVOLUTION_ETABLIE,
        RESERVE_RESPECTEE,
        QUOTITE_DEPASSEE,
        QUALIFICATION_INCOMPLETE
    }

    /** État civil du défunt à la date du décès. */
    public enum EtatCivilDefuntBe {
        MARIE,
        COHABITANT_LEGAL,
        CELIBATAIRE,
        DIVORCE,
        VEUF
    }

    /** Régime matrimonial du défunt s'il était marié. */
    public enum RegimeMatrimonialDefuntBe {
        COMMUNAUTE_LEGALE,
        SEPARATION_BIENS,
        COMMUNAUTE_UNIVERSELLE,
        PARTICIPATION_ACQUETS,
        AUTRE
    }

    /** Code structuré d'un héritier exposé dans la réponse. */
    public enum HeritierCodeBe {
        CONJOINT_SURVIVANT,
        COHABITANT_LEGAL_SURVIVANT,
        ENFANT,
        DESCENDANT_REPRESENTATION,
        PARENT,
        FRERE_SOEUR,
        DESCENDANT_FRERE_SOEUR,
        ETAT
    }

    /** Héritier identifié dans la dévolution — structure exposée dans la réponse API. */
    public record Heritier(
            HeritierCodeBe code,
            String libelle,
            String quotePartReserveFraction,
            BigDecimal quotePartReserveEur,
            String quotePartGlobaleFraction,
            BigDecimal quotePartGlobaleEur,
            String fondement
    ) {}

    private SuccessionBeDevolutionReserveCalculator() {}

    /**
     * Applique l'arbre de dévolution et de calcul de réserve.
     *
     * @param input   données saisies par l'avocat (validées non-null par les enums)
     * @param country pays du workspace ("BELGIQUE" uniquement supporté)
     * @return résultat structuré (verdict, héritiers, réserve globale, quotité disponible)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static SuccessionBeDevolutionReserveResult compute(
            SuccessionBeDevolutionReserveInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"BELGIQUE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Succession (dévolution / réserve) — outil disponible uniquement en BELGIQUE");
        }

        validateInputs(input);

        // -------- Données dérivées --------
        BigDecimal masse = input.masseSuccessoraleEur();
        BigDecimal liberalites = input.libertesConsentiesEur();
        boolean defuntMarie = input.etatCivilDefunt() == EtatCivilDefuntBe.MARIE;
        boolean defuntCohabitantLegal = input.etatCivilDefunt() == EtatCivilDefuntBe.COHABITANT_LEGAL;
        int nbEnfantsVivants = input.nombreEnfantsVivants();
        int nbEnfantsPredecedes = input.nombreEnfantsPredecedesAvecDescendants();
        boolean descendantsDirects = nbEnfantsVivants + nbEnfantsPredecedes > 0;
        boolean conjointSurvivantApplicable = defuntMarie;
        boolean cohabitantSurvivantApplicable = defuntCohabitantLegal;

        List<String> messages = new ArrayList<>();
        if (input.dateDeces() != null && input.dateDeces().isBefore(DATE_REFORME)) {
            // CC art. 913 nouveau (à vérifier) — réforme applicable au décès postérieur à
            // 01/09/2018. Arbitrage produit V1 : le calcul reste celui de la réforme, un
            // avertissement explicite est émis pour orienter l'avocat vers une analyse
            // dédiée au droit antérieur.
            messages.add("Date de décès antérieure au 01/09/2018 : le droit successoral "
                    + "antérieur (réserves 1/2-2/3-3/4 selon le nombre d'enfants ; réserve "
                    + "des ascendants) s'applique normalement. L'outil V1 calcule selon la "
                    + "réforme — interpréter les chiffres avec prudence et basculer le cas "
                    + "échéant sur une analyse manuelle.");
        }

        // -------- Cas QUALIFICATION_INCOMPLETE --------
        if (defuntMarie && input.regimeMatrimonialDefunt() == null) {
            // Sans régime matrimonial, la masse partageable ne peut être qualifiée.
            return new SuccessionBeDevolutionReserveResult(
                    Verdict.QUALIFICATION_INCOMPLETE,
                    List.of(),
                    BigDecimal.ZERO,
                    "0",
                    masse,
                    "1",
                    liberalites,
                    BigDecimal.ZERO,
                    basesJuridiques(Verdict.QUALIFICATION_INCOMPLETE),
                    List.of("Défunt marié sans régime matrimonial renseigné : la qualification "
                            + "de la masse successorale est impossible — préciser le régime "
                            + "matrimonial pour relancer le calcul."),
                    countryNormalized);
        }

        // -------- Cas DEVOLUTION_ETABLIE (sans descendants ni conjoint réservataire) --------
        if (!descendantsDirects && !conjointSurvivantApplicable) {
            return computeDevolutionSansReserveuxParticipants(
                    input, masse, liberalites, countryNormalized, messages,
                    cohabitantSurvivantApplicable);
        }

        // -------- Cas avec descendants ou conjoint survivant marié --------
        // CC art. 913 nouveau (à vérifier) : réserve globale = 1/2 quelle que soit le
        // nombre d'enfants. Si pas de descendants, le conjoint survivant marié reste
        // réservataire (CC art. 915bis — à vérifier — usufruit du logement familial).
        BigDecimal reserveGlobale = halve(masse);
        BigDecimal quotiteDisponible = masse.subtract(reserveGlobale);
        BigDecimal depassement = liberalites.subtract(quotiteDisponible);
        if (depassement.signum() < 0) {
            depassement = BigDecimal.ZERO;
        }

        List<Heritier> heritiers = new ArrayList<>();
        if (descendantsDirects) {
            heritiers.addAll(distribuerDescendants(
                    nbEnfantsVivants, nbEnfantsPredecedes, reserveGlobale, masse,
                    conjointSurvivantApplicable));
            if (conjointSurvivantApplicable) {
                // CC art. 745bis (à vérifier) : conjoint survivant = usufruit universel,
                // les descendants reçoivent la nue-propriété.
                heritiers.add(0, new Heritier(
                        HeritierCodeBe.CONJOINT_SURVIVANT,
                        "Conjoint survivant",
                        "0",
                        BigDecimal.ZERO,
                        "USUFRUIT_TOTAL",
                        null,
                        "CC art. 745bis (à vérifier) — usufruit universel du conjoint "
                                + "survivant en présence de descendants ; les descendants "
                                + "reçoivent la nue-propriété de leur quote-part."));
                messages.add("Conjoint survivant : usufruit universel sur la totalité de "
                        + "la succession (CC art. 745bis — à vérifier). Les descendants "
                        + "reçoivent la nue-propriété de leur quote-part.");
            }
            if (cohabitantSurvivantApplicable) {
                heritiers.add(0, heritierCohabitantLegalLimite());
                messages.add("Cohabitant légal survivant : droits limités à l'usufruit du "
                        + "logement familial et des meubles meublants (CC art. 1477 — à "
                        + "vérifier). Le cohabitant légal n'est pas réservataire — il ne "
                        + "peut s'opposer aux libéralités du défunt.");
            }
        } else {
            // Pas de descendants mais conjoint survivant marié : conjoint = pleine
            // propriété en concours avec collatéraux / ascendants selon les cas.
            heritiers.add(new Heritier(
                    HeritierCodeBe.CONJOINT_SURVIVANT,
                    "Conjoint survivant",
                    "1/2",
                    reserveGlobale,
                    "PLEINE_PROPRIETE",
                    null,
                    "CC art. 745bis (à vérifier) — conjoint survivant réservataire en "
                            + "l'absence de descendants ; pleine propriété sur sa part."));
            messages.add("Pas de descendants : conjoint survivant en concours avec "
                    + "ascendants / collatéraux (CC art. 745bis — à vérifier).");
        }

        // Quotité disponible message (dépassement éventuel)
        if (depassement.signum() > 0) {
            messages.add(String.format(Locale.FRANCE,
                    "Libéralités consenties (%s €) > quotité disponible (%s €) : "
                            + "dépassement de %s € sujet à réduction (CC art. 920+ — à vérifier).",
                    format(liberalites), format(quotiteDisponible), format(depassement)));
        } else {
            messages.add(String.format(Locale.FRANCE,
                    "Réserve globale = 1/2 (%s €), quotité disponible = 1/2 (%s €). "
                            + "Libéralités consenties (%s €) ≤ quotité disponible : pas de réduction.",
                    format(reserveGlobale), format(quotiteDisponible), format(liberalites)));
        }

        Verdict verdict = depassement.signum() > 0 ? Verdict.QUOTITE_DEPASSEE : Verdict.RESERVE_RESPECTEE;
        return new SuccessionBeDevolutionReserveResult(
                verdict,
                List.copyOf(heritiers),
                reserveGlobale,
                "1/2",
                quotiteDisponible,
                "1/2",
                liberalites,
                depassement,
                basesJuridiques(verdict),
                List.copyOf(messages),
                countryNormalized);
    }

    /** Dévolution sans descendants ni conjoint réservataire — pas de réserve. */
    private static SuccessionBeDevolutionReserveResult computeDevolutionSansReserveuxParticipants(
            SuccessionBeDevolutionReserveInput input,
            BigDecimal masse,
            BigDecimal liberalites,
            String country,
            List<String> messagesBase,
            boolean cohabitantSurvivantApplicable) {
        List<Heritier> heritiers = new ArrayList<>();
        List<String> messages = new ArrayList<>(messagesBase);

        if (cohabitantSurvivantApplicable) {
            heritiers.add(heritierCohabitantLegalLimite());
            messages.add("Cohabitant légal survivant : droits limités à l'usufruit du "
                    + "logement familial et des meubles meublants (CC art. 1477 — à "
                    + "vérifier). Le cohabitant légal n'est pas réservataire.");
        }

        if (Boolean.TRUE.equals(input.presenceParentsVivants())) {
            heritiers.add(new Heritier(
                    HeritierCodeBe.PARENT,
                    "Parents du défunt",
                    "0",
                    BigDecimal.ZERO,
                    null,
                    null,
                    "CC art. 746-747 (à vérifier) — dévolution aux ascendants en l'absence "
                            + "de descendants. La réserve des ascendants est supprimée par la "
                            + "réforme du 31/07/2017 (à vérifier) — droit à un entretien "
                            + "alimentaire éventuel (CC art. 205bis — à vérifier)."));
        }
        if (Boolean.TRUE.equals(input.presenceFreresSoeursOuDescendants())) {
            heritiers.add(new Heritier(
                    HeritierCodeBe.FRERE_SOEUR,
                    "Frères et sœurs du défunt (ou leurs descendants par représentation)",
                    "0",
                    BigDecimal.ZERO,
                    null,
                    null,
                    "CC art. 748+ (à vérifier) — dévolution aux collatéraux privilégiés "
                            + "en l'absence d'ascendants prioritaires."));
        }
        if (heritiers.isEmpty()) {
            heritiers.add(new Heritier(
                    HeritierCodeBe.ETAT,
                    "État (déshérence)",
                    "0",
                    BigDecimal.ZERO,
                    "1",
                    masse,
                    "CC art. 768+ (à vérifier) — déshérence : la succession est dévolue à "
                            + "l'État en l'absence de tout héritier de rang utile."));
            messages.add("Aucun héritier identifié : la succession est dévolue à l'État "
                    + "(déshérence, CC art. 768+ — à vérifier).");
        }

        messages.add(String.format(Locale.FRANCE,
                "Aucun descendant ni conjoint marié réservataire : il n'y a pas de réserve "
                        + "héréditaire. La masse successorale (%s €) est intégralement quotité "
                        + "disponible — les libéralités consenties (%s €) ne sont pas sujettes à réduction.",
                format(masse), format(liberalites)));

        return new SuccessionBeDevolutionReserveResult(
                Verdict.DEVOLUTION_ETABLIE,
                List.copyOf(heritiers),
                BigDecimal.ZERO,
                "0",
                masse,
                "1",
                liberalites,
                BigDecimal.ZERO,
                basesJuridiques(Verdict.DEVOLUTION_ETABLIE),
                List.copyOf(messages),
                country);
    }

    /**
     * Répartit la masse entre descendants (par tête entre enfants vivants, par souche
     * pour les enfants prédécédés laissant des descendants — CC art. 740 — à vérifier).
     * La quote-part de chaque souche = part par tête (= 1 / nbSouches) de la masse
     * (resp. de la nue-propriété si conjoint survivant).
     */
    private static List<Heritier> distribuerDescendants(int nbEnfantsVivants,
                                                        int nbEnfantsPredecedesAvecDescendants,
                                                        BigDecimal reserveGlobale,
                                                        BigDecimal masse,
                                                        boolean conjointSurvivant) {
        int nbSouches = nbEnfantsVivants + nbEnfantsPredecedesAvecDescendants;
        if (nbSouches <= 0) {
            return List.of();
        }
        BigDecimal partReserveParSouche = divideExact(reserveGlobale, nbSouches);
        BigDecimal partGlobaleParSouche = divideExact(masse, nbSouches);
        String fractionReserveParSouche = "1/" + (nbSouches * 2);
        String fractionGlobaleParSouche = "1/" + nbSouches;

        List<Heritier> heritiers = new ArrayList<>();
        for (int i = 0; i < nbEnfantsVivants; i++) {
            String libelle = conjointSurvivant
                    ? "Enfant n°" + (i + 1) + " (nue-propriété)"
                    : "Enfant n°" + (i + 1);
            heritiers.add(new Heritier(
                    HeritierCodeBe.ENFANT,
                    libelle,
                    fractionReserveParSouche,
                    partReserveParSouche,
                    fractionGlobaleParSouche,
                    conjointSurvivant ? partGlobaleParSouche : partGlobaleParSouche,
                    "CC art. 913 nouveau (à vérifier) — réserve globale 1/2 partagée par "
                            + "tête entre descendants (réforme loi 31/07/2017)."));
        }
        for (int i = 0; i < nbEnfantsPredecedesAvecDescendants; i++) {
            String libelle = conjointSurvivant
                    ? "Souche enfant prédécédé n°" + (i + 1) + " (descendants par représentation, nue-propriété)"
                    : "Souche enfant prédécédé n°" + (i + 1) + " (descendants par représentation)";
            heritiers.add(new Heritier(
                    HeritierCodeBe.DESCENDANT_REPRESENTATION,
                    libelle,
                    fractionReserveParSouche,
                    partReserveParSouche,
                    fractionGlobaleParSouche,
                    partGlobaleParSouche,
                    "CC art. 740 (à vérifier) — représentation : les descendants d'un "
                            + "enfant prédécédé viennent en ses lieu et place, partagent la "
                            + "part par souche."));
        }
        return heritiers;
    }

    private static Heritier heritierCohabitantLegalLimite() {
        return new Heritier(
                HeritierCodeBe.COHABITANT_LEGAL_SURVIVANT,
                "Cohabitant légal survivant",
                "0",
                BigDecimal.ZERO,
                "USUFRUIT_LOGEMENT_FAMILIAL",
                null,
                "CC art. 1477 (à vérifier) — droits limités à l'usufruit du logement "
                        + "familial occupé en commun et des meubles meublants. Le cohabitant "
                        + "légal n'est pas réservataire (pas de réserve opposable aux libéralités).");
    }

    private static List<String> basesJuridiques(Verdict verdict) {
        Set<String> bases = new LinkedHashSet<>();
        bases.add("CC art. 913 nouveau (à vérifier) — réserve globale 1/2 quelle que soit "
                + "le nombre d'enfants (réforme loi 31/07/2017, en vigueur 01/09/2018).");
        bases.add("CC art. 745bis (à vérifier) — droits du conjoint survivant (usufruit "
                + "universel en présence de descendants).");
        bases.add("CC art. 740 (à vérifier) — représentation des descendants.");
        if (verdict == Verdict.QUOTITE_DEPASSEE) {
            bases.add("CC art. 920+ (à vérifier) — réduction des libéralités excessives.");
        }
        if (verdict == Verdict.DEVOLUTION_ETABLIE) {
            bases.add("CC art. 746-747 (à vérifier) — dévolution aux ascendants ; "
                    + "réserve des ascendants supprimée par la réforme 2017 (à vérifier).");
            bases.add("CC art. 748+ (à vérifier) — dévolution aux collatéraux privilégiés.");
        }
        bases.add("CC art. 1477 (à vérifier) — droits limités du cohabitant légal survivant "
                + "(non réservataire).");
        return new ArrayList<>(bases);
    }

    // -------- Validation des inputs --------
    private static void validateInputs(SuccessionBeDevolutionReserveInput in) {
        if (in.dateDeces() == null) {
            throw new IllegalArgumentException("dateDeces est requise");
        }
        if (in.dateDeces().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dateDeces ne peut être dans le futur");
        }
        if (in.etatCivilDefunt() == null) {
            throw new IllegalArgumentException("etatCivilDefunt est requis");
        }
        // regimeMatrimonialDefunt : géré en QUALIFICATION_INCOMPLETE plus haut si MARIE+null.
        requireIntInBounds(in.nombreEnfantsVivants(), "nombreEnfantsVivants");
        requireIntInBounds(in.nombreEnfantsPredecedesAvecDescendants(),
                "nombreEnfantsPredecedesAvecDescendants");
        if (in.presenceParentsVivants() == null) {
            throw new IllegalArgumentException("presenceParentsVivants est requis");
        }
        if (in.presenceFreresSoeursOuDescendants() == null) {
            throw new IllegalArgumentException("presenceFreresSoeursOuDescendants est requis");
        }
        requireMontant(in.masseSuccessoraleEur(), "masseSuccessoraleEur");
        requireMontant(in.libertesConsentiesEur(), "libertesConsentiesEur");
        if (in.commentaire() != null && in.commentaire().length() > COMMENTAIRE_MAX) {
            throw new IllegalArgumentException("Le commentaire ne peut dépasser "
                    + COMMENTAIRE_MAX + " caractères");
        }
    }

    private static void requireIntInBounds(Integer value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " est requis");
        }
        if (value < 0) {
            throw new IllegalArgumentException(name + " doit être ≥ 0");
        }
        if (value > NB_ENFANTS_MAX) {
            throw new IllegalArgumentException(name + " doit être ≤ " + NB_ENFANTS_MAX);
        }
    }

    private static void requireMontant(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " est requis");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " doit être ≥ 0");
        }
    }

    // -------- Utilitaires arithmétiques --------
    /** Divise une masse en parts égales (arrondi HALF_UP à l'euro entier). */
    private static BigDecimal divideExact(BigDecimal value, int parts) {
        if (parts <= 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(BigDecimal.valueOf(parts), 0, RoundingMode.HALF_UP);
    }

    /** Calcule la moitié d'un montant (arrondi HALF_UP à l'euro entier). */
    private static BigDecimal halve(BigDecimal value) {
        return value.divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
    }

    private static String format(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }
}
