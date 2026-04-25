package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SF-FA-22-01 : calculateur d'<b>éligibilité au partage judiciaire d'une
 * indivision post-communautaire</b> (art. 815 et s. Cciv + art. 1364 CPC).
 * Outil <b>single-country FR DROIT_FAMILLE</b>. La BE (loi du 13 août 2011 —
 * art. 1207 et s. C. jud.) suit une logique distincte non couverte.
 *
 * <h2>Scoring 0-100 (somme plafonnée)</h2>
 * <ul>
 *   <li>≥ 2 tentatives de partage amiable : <b>+30</b></li>
 *   <li>conflit ouvert entre indivisaires : <b>+25</b></li>
 *   <li>absence de consentement au partage global : <b>+20</b></li>
 *   <li>durée d'indivision &gt; 2 ans : <b>+15</b></li>
 *   <li>occupation du bien par un indivisaire : <b>+10</b></li>
 * </ul>
 *
 * <h2>Verdict</h2>
 * <ul>
 *   <li>≥ 70 → PARTAGE_JUDICIAIRE_RECOMMANDE</li>
 *   <li>40-69 + IMMOBILIER + ≥ 2 tentatives → LICITATION_REQUISE</li>
 *   <li>40-69 sinon → MEDIATION_PREALABLE</li>
 *   <li>&lt; 40 + consentement global → PARTAGE_AMIABLE_POSSIBLE</li>
 *   <li>&lt; 40 sinon → MEDIATION_PREALABLE</li>
 * </ul>
 *
 * <h2>Indemnité d'occupation (art. 815-9 al. 2 Cciv)</h2>
 * <pre>indemnite = valeur × 0.04 / 12 × duréeMois × quotePartLesee</pre>
 * où {@code quotePartLesee = (100 - max(quotesPart)) / 100}. Le taux 4 %/an est
 * la fourchette retenue par les notaires en l'absence d'expertise locative.
 */
public final class IndivisionCalculator {

    public static final String BASE_JURIDIQUE = "Art. 815 Cciv + art. 1364 CPC";

    public static final int SEUIL_PARTAGE_JUDICIAIRE = 70;
    public static final int SEUIL_MEDIATION = 40;
    public static final int DUREE_LONGUE_ANS = 2;
    public static final int NB_INDIVISAIRES_EXPERTISE = 3;
    public static final BigDecimal SEUIL_VALEUR_EXPERTISE_EUR = new BigDecimal("100000");
    public static final BigDecimal TAUX_OCCUPATION_ANNUEL = new BigDecimal("0.04");

    public static final String VERDICT_AMIABLE = "PARTAGE_AMIABLE_POSSIBLE";
    public static final String VERDICT_JUDICIAIRE = "PARTAGE_JUDICIAIRE_RECOMMANDE";
    public static final String VERDICT_LICITATION = "LICITATION_REQUISE";
    public static final String VERDICT_MEDIATION = "MEDIATION_PREALABLE";

    private static final Set<String> NATURES_BIENS = new LinkedHashSet<>(Arrays.asList(
            "IMMOBILIER", "MOBILIER", "COMPTES_BANCAIRES",
            "TITRES_FINANCIERS", "FONDS_COMMERCE", "AUTRE"));

    private static final Set<String> TENTATIVES = new LinkedHashSet<>(Arrays.asList(
            "PROPOSITION_NOTAIRE", "MEDIATION", "EXPERTISE_VALORISATION",
            "LICITATION_AMIABLE", "AUCUNE"));

    private IndivisionCalculator() {
    }

    public static IndivisionResult compute(LocalDate dateOrigineIndivision,
                                           List<String> natureBiens,
                                           BigDecimal valeurEstimeeTotaleEur,
                                           int nbIndivisaires,
                                           List<BigDecimal> quotesPart,
                                           List<String> tentativesPartageAmiable,
                                           boolean consentementPartageGlobal,
                                           boolean occupationBienParUnIndivisaire,
                                           int indivisionDureeAnnees,
                                           boolean demandeMesuresConservatoires,
                                           boolean conflitOuvertEntreIndivisaires) {
        validate(dateOrigineIndivision, natureBiens, valeurEstimeeTotaleEur,
                nbIndivisaires, quotesPart, tentativesPartageAmiable,
                indivisionDureeAnnees);

        List<String> natures = List.copyOf(natureBiens);
        List<String> tentatives = List.copyOf(tentativesPartageAmiable);
        List<BigDecimal> quotes = List.copyOf(quotesPart);

        // 1. Score
        int score = 0;
        long nbTentativesEffectives = tentatives.stream()
                .filter(t -> !"AUCUNE".equals(t)).count();
        if (nbTentativesEffectives >= 2) score += 30;
        if (conflitOuvertEntreIndivisaires) score += 25;
        if (!consentementPartageGlobal) score += 20;
        if (indivisionDureeAnnees > DUREE_LONGUE_ANS) score += 15;
        if (occupationBienParUnIndivisaire) score += 10;
        score = Math.max(0, Math.min(100, score));

        boolean immobilier = natures.contains("IMMOBILIER");
        boolean amiableImpossible = nbTentativesEffectives >= 2;

        // 2. Verdict
        String verdict;
        if (score >= SEUIL_PARTAGE_JUDICIAIRE) {
            verdict = VERDICT_JUDICIAIRE;
        } else if (score >= SEUIL_MEDIATION) {
            verdict = (immobilier && amiableImpossible)
                    ? VERDICT_LICITATION : VERDICT_MEDIATION;
        } else {
            verdict = consentementPartageGlobal ? VERDICT_AMIABLE : VERDICT_MEDIATION;
        }

        // 3. Indemnité d'occupation
        BigDecimal indemnite = computeIndemniteOccupation(
                occupationBienParUnIndivisaire,
                valeurEstimeeTotaleEur,
                indivisionDureeAnnees,
                quotes);

        // 4. Recommandations dérivées
        boolean expertiseRecommandee = nbIndivisaires >= NB_INDIVISAIRES_EXPERTISE
                || valeurEstimeeTotaleEur.compareTo(SEUIL_VALEUR_EXPERTISE_EUR) > 0;
        boolean licitationRecommandee = immobilier
                && (VERDICT_JUDICIAIRE.equals(verdict) || VERDICT_LICITATION.equals(verdict));
        int delaiMois = computeDelaiMois(nbIndivisaires, immobilier, conflitOuvertEntreIndivisaires);

        // 5. Messages
        List<String> messages = buildMessages(verdict, immobilier, occupationBienParUnIndivisaire,
                expertiseRecommandee, licitationRecommandee, demandeMesuresConservatoires,
                consentementPartageGlobal, nbTentativesEffectives);

        // 6. Formule
        String formule = buildFormule(score, verdict, nbTentativesEffectives,
                conflitOuvertEntreIndivisaires, consentementPartageGlobal,
                indivisionDureeAnnees, occupationBienParUnIndivisaire, indemnite);

        return new IndivisionResult(
                dateOrigineIndivision,
                natures,
                money(valeurEstimeeTotaleEur),
                nbIndivisaires,
                quotes.stream().map(IndivisionCalculator::moneyQuote).toList(),
                tentatives,
                consentementPartageGlobal,
                occupationBienParUnIndivisaire,
                indivisionDureeAnnees,
                demandeMesuresConservatoires,
                conflitOuvertEntreIndivisaires,
                score,
                verdict,
                indemnite,
                expertiseRecommandee,
                licitationRecommandee,
                delaiMois,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    static BigDecimal computeIndemniteOccupation(boolean occupation,
                                                 BigDecimal valeur,
                                                 int dureeAnnees,
                                                 List<BigDecimal> quotesPart) {
        if (!occupation) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (dureeAnnees <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxQuote = quotesPart.stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(100));
        BigDecimal quotePartLesee = BigDecimal.valueOf(100).subtract(maxQuote)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal dureeMois = BigDecimal.valueOf(dureeAnnees * 12L);
        // formule consolidée : valeur × taux/12 × mois × quotePartLesee
        // évite les arrondis intermédiaires en effectuant les multiplications
        // d'abord, puis la division finale par 12.
        BigDecimal indemnite = valeur
                .multiply(TAUX_OCCUPATION_ANNUEL)
                .multiply(dureeMois)
                .multiply(quotePartLesee)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        return indemnite.setScale(2, RoundingMode.HALF_UP);
    }

    private static int computeDelaiMois(int nbIndivisaires,
                                        boolean immobilier,
                                        boolean conflit) {
        if (nbIndivisaires >= 4 || (conflit && immobilier)) return 24;
        if (immobilier || nbIndivisaires >= 3) return 18;
        return 12;
    }

    private static List<String> buildMessages(String verdict,
                                              boolean immobilier,
                                              boolean occupation,
                                              boolean expertiseRecommandee,
                                              boolean licitationRecommandee,
                                              boolean demandeMesuresConservatoires,
                                              boolean consentementPartageGlobal,
                                              long nbTentativesEffectives) {
        List<String> msg = new ArrayList<>();
        msg.add("Indivision post-communautaire : analyse fondée sur les art. 815 "
                + "et s. Cciv (régime de l'indivision) et l'art. 1364 CPC "
                + "(procédure de partage judiciaire devant le tribunal judiciaire).");
        msg.add("Principe : « Nul ne peut être contraint à demeurer dans "
                + "l'indivision » (art. 815 al. 1 Cciv). Tout indivisaire peut "
                + "à tout moment provoquer le partage.");
        switch (verdict) {
            case VERDICT_JUDICIAIRE -> msg.add("Voie amiable manifestement épuisée — "
                    + "saisir le TJ par assignation en partage (art. 1364 CPC). "
                    + "Le juge désignera un notaire commis pour les opérations "
                    + "de partage si la complexité le justifie.");
            case VERDICT_LICITATION -> msg.add("Bien immobilier non partageable en "
                    + "nature — licitation (vente aux enchères judiciaire ou "
                    + "amiable autorisée par le juge) art. 1377 CPC. Mise à "
                    + "prix à fixer après expertise.");
            case VERDICT_MEDIATION -> msg.add("Tenter une médiation préalable ou un "
                    + "rendez-vous notarial commun avant assignation. Une "
                    + "tentative de règlement amiable est obligatoirement "
                    + "mentionnée dans l'assignation au TJ (art. 1108 CPC).");
            case VERDICT_AMIABLE -> msg.add("Partage amiable possible (art. 835 "
                    + "Cciv) : acte notarié obligatoire si bien immobilier "
                    + "indivis. Frais d'acte 1,1 % de la valeur (frais réduits).");
            default -> { /* no-op */ }
        }
        if (occupation) {
            msg.add("Indemnité d'occupation due par l'indivisaire occupant "
                    + "(art. 815-9 al. 2 Cciv) — taux conventionnel 4 % "
                    + "annuel sur la valeur, à proportion de la quote-part "
                    + "des autres indivisaires. Le notaire ou le juge peut "
                    + "retenir un taux différent (3 à 5 %) selon l'expertise "
                    + "locative produite.");
        }
        if (expertiseRecommandee) {
            msg.add("Expertise notariale recommandée (patrimoine complexe ou "
                    + "valeur > 100 000 €) : permet d'asseoir la mise à prix, "
                    + "d'identifier les biens partageables en nature et de "
                    + "réduire le risque de contestation post-partage.");
        }
        if (licitationRecommandee) {
            msg.add("Licitation recommandée : le bien immobilier ne peut être "
                    + "commodément partagé en nature ; il sera vendu et le "
                    + "prix réparti entre indivisaires (art. 1378 CPC).");
        }
        if (demandeMesuresConservatoires) {
            msg.add("Mesures conservatoires demandées : examiner si saisie "
                    + "conservatoire (art. 67 al. 1 procédures civiles "
                    + "d'exécution) ou désignation d'un séquestre judiciaire "
                    + "(art. 1961 Cciv) sont opportunes — voir F-FA-23.");
        }
        if (!immobilier && verdict.equals(VERDICT_LICITATION)) {
            msg.add("Pas de bien immobilier identifié : la licitation ne "
                    + "s'applique pas, l'orientation est révisée.");
        }
        if (consentementPartageGlobal && nbTentativesEffectives == 0) {
            msg.add("Consentement global affirmé mais aucune tentative formalisée — "
                    + "documenter l'accord par écrit (acte sous seing privé ou "
                    + "acte notarié) pour sécuriser le partage.");
        }
        msg.add("Outil indicatif : la qualification définitive (partage en nature, "
                + "licitation, attribution préférentielle art. 831-832 Cciv) "
                + "relève du juge ou du notaire commis.");
        return msg;
    }

    private static String buildFormule(int score,
                                       String verdict,
                                       long nbTentativesEffectives,
                                       boolean conflitOuvert,
                                       boolean consentementPartageGlobal,
                                       int indivisionDureeAnnees,
                                       boolean occupation,
                                       BigDecimal indemnite) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score ").append(score).append("/100 = ");
        List<String> contribs = new ArrayList<>();
        if (nbTentativesEffectives >= 2) contribs.add("≥2 tentatives amiables (+30)");
        if (conflitOuvert) contribs.add("conflit ouvert (+25)");
        if (!consentementPartageGlobal) contribs.add("absence de consentement (+20)");
        if (indivisionDureeAnnees > DUREE_LONGUE_ANS) contribs.add(
                "durée " + indivisionDureeAnnees + " ans > 2 (+15)");
        if (occupation) contribs.add("occupation (+10)");
        sb.append(contribs.isEmpty() ? "aucune contribution" : String.join(" + ", contribs));
        sb.append(". Verdict ").append(verdict).append(".");
        if (occupation) {
            sb.append(" Indemnité d'occupation (art. 815-9 al. 2) = ")
                    .append(indemnite.toPlainString()).append(" €.");
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    private static void validate(LocalDate dateOrigineIndivision,
                                 List<String> natureBiens,
                                 BigDecimal valeurEstimeeTotaleEur,
                                 int nbIndivisaires,
                                 List<BigDecimal> quotesPart,
                                 List<String> tentatives,
                                 int indivisionDureeAnnees) {
        if (dateOrigineIndivision == null) {
            throw new IllegalArgumentException("dateOrigineIndivision est requise");
        }
        if (dateOrigineIndivision.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dateOrigineIndivision ne peut être dans le futur");
        }
        if (natureBiens == null || natureBiens.isEmpty()) {
            throw new IllegalArgumentException("natureBiens : au moins une nature requise");
        }
        for (String n : natureBiens) {
            if (n == null || !NATURES_BIENS.contains(n)) {
                throw new IllegalArgumentException("natureBiens : valeur invalide : "
                        + n + ". Valeurs : " + NATURES_BIENS);
            }
        }
        if (valeurEstimeeTotaleEur == null) {
            throw new IllegalArgumentException("valeurEstimeeTotaleEur est requise");
        }
        if (valeurEstimeeTotaleEur.signum() < 0) {
            throw new IllegalArgumentException("valeurEstimeeTotaleEur ne peut être négative");
        }
        if (nbIndivisaires < 2 || nbIndivisaires > 20) {
            throw new IllegalArgumentException("nbIndivisaires doit être entre 2 et 20");
        }
        if (quotesPart == null || quotesPart.size() != nbIndivisaires) {
            throw new IllegalArgumentException(
                    "quotesPart : taille (" + (quotesPart == null ? 0 : quotesPart.size())
                            + ") doit égaler nbIndivisaires (" + nbIndivisaires + ")");
        }
        BigDecimal somme = BigDecimal.ZERO;
        for (BigDecimal q : quotesPart) {
            if (q == null || q.signum() < 0
                    || q.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(
                        "quotesPart : chaque quote-part doit être entre 0 et 100 (vu : " + q + ")");
            }
            somme = somme.add(q);
        }
        BigDecimal diff = somme.subtract(BigDecimal.valueOf(100)).abs();
        if (diff.compareTo(new BigDecimal("0.5")) > 0) {
            throw new IllegalArgumentException(
                    "quotesPart : somme doit être 100 (vu : " + somme + ")");
        }
        if (tentatives == null) {
            throw new IllegalArgumentException("tentativesPartageAmiable est requis");
        }
        for (String t : tentatives) {
            if (t == null || !TENTATIVES.contains(t)) {
                throw new IllegalArgumentException(
                        "tentativesPartageAmiable : valeur invalide : " + t
                                + ". Valeurs : " + TENTATIVES);
            }
        }
        if (indivisionDureeAnnees < 0) {
            throw new IllegalArgumentException("indivisionDureeAnnees ne peut être négative");
        }
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal moneyQuote(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }
}
