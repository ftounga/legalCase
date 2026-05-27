package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-213-09 : analyseur de l'acte équipollent à rupture BE — fonction
 * <b>pure</b> (paramétrable par un {@link Clock} pour la testabilité du
 * calcul {@code delaiDepuisModification}).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 3 juillet 1978</b> art. 20 : interdit à l'employeur de
 *       modifier unilatéralement un élément essentiel du contrat.</li>
 *   <li><b>Cass. BE 23/12/1957</b> et jurisprudence postérieure : si
 *       l'employeur modifie unilatéralement un élément essentiel, le
 *       salarié peut considérer le contrat rompu aux torts de l'employeur
 *       (acte équipollent à rupture) — droit à l'indemnité compensatoire
 *       de préavis (ICP).</li>
 *   <li><b>Doctrine Ius Variandi</b> : l'employeur conserve un pouvoir
 *       de direction limité aux modifications mineures.</li>
 *   <li><b>Délai d'action</b> : pas de délai légal strict, mais la
 *       jurisprudence belge recommande de réagir dans les <b>30 jours</b>
 *       sous peine d'acceptation tacite.</li>
 * </ul>
 *
 * <h2>Matrice de décision</h2>
 * <table>
 *   <tr><th>Ampleur</th><th>Élément essentiel</th><th>Verdict</th></tr>
 *   <tr><td>MINEURE</td><td>—</td><td>PAS_ACTE_EQUIPOLLENT (Ius Variandi)</td></tr>
 *   <tr><td>SUBSTANTIELLE</td><td>true</td><td>ACTE_EQUIPOLLENT_PROBABLE</td></tr>
 *   <tr><td>SUBSTANTIELLE</td><td>false</td><td>A_ANALYSER</td></tr>
 *   <tr><td>INDETERMINEES</td><td>—</td><td>A_ANALYSER</td></tr>
 * </table>
 *
 * <h2>Avertissement risque acceptation tacite</h2>
 * <p>Levé si {@code !salarieAProteste && delaiDepuisModification > 30 j}.
 * Lorsque le verdict initial est {@code ACTE_EQUIPOLLENT_PROBABLE} ou
 * {@code A_ANALYSER}, le verdict est rétrogradé à
 * {@code RISQUE_ACCEPTATION_TACITE} pour alerter explicitement l'avocat.
 * Le drapeau {@code risqueAcceptationTacite} est posé indépendamment.</p>
 *
 * <h2>ICP indicatif</h2>
 * <p>Si {@code ACTE_EQUIPOLLENT_PROBABLE} et que la rémunération
 * hebdomadaire brute + la durée de préavis (en semaines) sont fournies :
 * {@code icpIndicatif = remunerationHebdomadaireBrute × dureePreavisCalculeeSemaines}.
 * Marqué « indicatif » — à combiner avec l'outil préavis statut unique
 * (SF-213-03) ou formule Claeys (SF-213-04).</p>
 */
public final class LicenciementBeActeEquipollentAnalyzer {

    static final String BASE_JURIDIQUE =
            "Loi 03/07/1978 art. 20 ; Cass. BE 23/12/1957 (acte équipollent à rupture)";

    static final String FONDEMENT_JURIDIQUE =
            "Loi 03/07/1978 art. 20 — modification unilatérale élément essentiel";

    static final String AVERT_RISQUE_ACCEPTATION_TACITE =
            "Délai de protestation > 30 jours sans réaction du salarié — "
                    + "risque que le silence vaille acceptation tacite "
                    + "(jurisprudence Cass. BE)";

    /** Délai recommandé pour protester formellement la modification. */
    static final int DELAI_RECOMMANDE_PROTESTATION_JOURS = 30;

    private LicenciementBeActeEquipollentAnalyzer() {
    }

    public static LicenciementBeActeEquipollentResult analyze(
            LicenciementBeActeEquipollentRequest request) {
        return analyze(request, Clock.systemDefaultZone());
    }

    /**
     * Variante testable injectant un {@link Clock} explicite — permet de
     * fixer "aujourd'hui" pour vérifier le calcul du délai depuis
     * modification indépendamment de l'horloge système.
     */
    public static LicenciementBeActeEquipollentResult analyze(
            LicenciementBeActeEquipollentRequest request, Clock clock) {
        validateInputs(request);

        // 1. Calcul du délai depuis la modification (utilise la valeur
        //    fournie sinon today − dateModification)
        int delaiDepuisModification = request.delaiDepuisModificationJours() != null
                ? request.delaiDepuisModificationJours()
                : (int) ChronoUnit.DAYS.between(
                        request.dateModification(), LocalDate.now(clock));

        // 2. Verdict initial à partir de la matrice ampleur × élément essentiel
        LicenciementBeActeEquipollentVerdict verdict = computeBaseVerdict(
                request.ampleurModification(), request.elementEssentielDuContrat());

        // 3. Risque d'acceptation tacite (silence > 30 j sans protestation)
        boolean risqueAcceptationTacite = !request.salarieAProteste()
                && delaiDepuisModification > DELAI_RECOMMANDE_PROTESTATION_JOURS;

        // 4. Rétrogradation du verdict si risque acceptation tacite
        //    (seul cas où le risque change le verdict final : si on était
        //    sur une trajectoire favorable au salarié)
        if (risqueAcceptationTacite
                && (verdict == LicenciementBeActeEquipollentVerdict.ACTE_EQUIPOLLENT_PROBABLE
                        || verdict == LicenciementBeActeEquipollentVerdict.A_ANALYSER)) {
            verdict = LicenciementBeActeEquipollentVerdict.RISQUE_ACCEPTATION_TACITE;
        }

        // 5. ICP indicatif — uniquement si verdict ACTE_EQUIPOLLENT_PROBABLE
        //    et données nécessaires fournies
        BigDecimal icpIndicatif = computeIcpIndicatif(verdict, request);

        // 6. Avertissement
        String avertissement = risqueAcceptationTacite
                ? AVERT_RISQUE_ACCEPTATION_TACITE : null;

        return new LicenciementBeActeEquipollentResult(
                request.typeModification(),
                request.ampleurModification(),
                request.elementEssentielDuContrat(),
                request.dateModification(),
                request.salarieAProteste(),
                delaiDepuisModification,
                request.remunerationHebdomadaireBrute(),
                request.dureePreavisCalculeeSemaines(),
                verdict,
                FONDEMENT_JURIDIQUE,
                icpIndicatif,
                risqueAcceptationTacite,
                DELAI_RECOMMANDE_PROTESTATION_JOURS,
                BASE_JURIDIQUE,
                avertissement);
    }

    // ── Verdict de base ───────────────────────────────────────────────────

    private static LicenciementBeActeEquipollentVerdict computeBaseVerdict(
            AmpleurModificationEnum ampleur, boolean elementEssentiel) {
        return switch (ampleur) {
            case MINEURE -> LicenciementBeActeEquipollentVerdict.PAS_ACTE_EQUIPOLLENT;
            case SUBSTANTIELLE -> elementEssentiel
                    ? LicenciementBeActeEquipollentVerdict.ACTE_EQUIPOLLENT_PROBABLE
                    : LicenciementBeActeEquipollentVerdict.A_ANALYSER;
            case INDETERMINEES -> LicenciementBeActeEquipollentVerdict.A_ANALYSER;
        };
    }

    // ── ICP indicatif ─────────────────────────────────────────────────────

    private static BigDecimal computeIcpIndicatif(
            LicenciementBeActeEquipollentVerdict verdict,
            LicenciementBeActeEquipollentRequest r) {
        if (verdict != LicenciementBeActeEquipollentVerdict.ACTE_EQUIPOLLENT_PROBABLE) {
            return null;
        }
        if (r.remunerationHebdomadaireBrute() == null
                || r.remunerationHebdomadaireBrute().signum() <= 0) {
            return null;
        }
        if (r.dureePreavisCalculeeSemaines() == null
                || r.dureePreavisCalculeeSemaines() <= 0) {
            return null;
        }
        return r.remunerationHebdomadaireBrute()
                .multiply(BigDecimal.valueOf(r.dureePreavisCalculeeSemaines()));
    }

    // ── Validation conditionnelle ─────────────────────────────────────────

    private static void validateInputs(LicenciementBeActeEquipollentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.typeModification() == null) {
            throw new IllegalArgumentException("typeModification est requis");
        }
        if (request.ampleurModification() == null) {
            throw new IllegalArgumentException("ampleurModification est requis");
        }
        if (request.dateModification() == null) {
            throw new IllegalArgumentException("dateModification est requise");
        }
        if (request.delaiDepuisModificationJours() != null
                && request.delaiDepuisModificationJours() < 0) {
            throw new IllegalArgumentException(
                    "delaiDepuisModificationJours ne peut pas être négatif");
        }
        if (request.remunerationHebdomadaireBrute() != null
                && request.remunerationHebdomadaireBrute().signum() < 0) {
            throw new IllegalArgumentException(
                    "remunerationHebdomadaireBrute ne peut pas être négative");
        }
        if (request.dureePreavisCalculeeSemaines() != null
                && request.dureePreavisCalculeeSemaines() < 0) {
            throw new IllegalArgumentException(
                    "dureePreavisCalculeeSemaines ne peut pas être négative");
        }
    }
}
