package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-213-10 : calculateur pur du score CCT n° 109 du 12/02/2014 — licenciement
 * manifestement déraisonnable (art. 9). Indépendant du contexte HTTP / DB,
 * 100 % testable en unitaire.
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>CCT n° 109 du 12 février 2014</b> relative à la motivation du
 *       licenciement, <b>art. 9</b> : indemnité de 3 / 8 / 12 / 17 semaines
 *       de rémunération selon le degré de gravité du caractère
 *       manifestement déraisonnable du licenciement.</li>
 *   <li><b>CCT n° 109 art. 8</b> — qualification des motifs liés à la
 *       personne du travailleur, à sa conduite ou aux besoins de
 *       l'entreprise.</li>
 *   <li><b>Conseil d'État, arrêt n° 245.236 du 27/06/2019</b> — confirme la
 *       compatibilité de la CCT 109 avec la Constitution belge.</li>
 * </ul>
 *
 * <h2>Logique d'attribution de l'échelon (priorité descendante)</h2>
 * <ol>
 *   <li>Motif valable prouvé <b>+</b> procédures respectées → {@code
 *       NON_DERAISONNABLE} (pas d'indemnité CCT 109).</li>
 *   <li>Discrimination <b>et</b> représailles suspectées → <b>17 semaines</b>
 *       (maximum légal, faits graves cumulés).</li>
 *   <li>Discrimination <b>ou</b> représailles (1 seule) → <b>12 semaines</b>
 *       (gravité haute).</li>
 *   <li>Motif précis mais disputé <b>+</b> au moins 1 facteur aggravant
 *       mineur (procédure incomplète) → <b>8 semaines</b> (gravité ordinaire).</li>
 *   <li>Pas de motif communiqué <b>ou</b> SANS_MOTIF <b>ou</b> motif vague
 *       <b>ou</b> procédures non respectées → <b>3 semaines</b> (minimum
 *       légal CCT 109 art. 9).</li>
 *   <li>Sinon (par défaut prudent — pas de motif valable certain) →
 *       <b>3 semaines</b>.</li>
 * </ol>
 *
 * <h2>Calcul de l'indemnité</h2>
 * <p>{@code indemniteCct109 = remunerationHebdomadaireBrute × nombreSemaines}
 * (BigDecimal exact ; {@code 0} si {@code NON_DERAISONNABLE}).</p>
 *
 * <h2>Cumul avec l'ICP</h2>
 * <p>L'indemnité CCT 109 se cumule <b>toujours</b> avec l'indemnité
 * compensatoire de préavis (calculée par les outils Préavis statut unique ou
 * Formule Claeys). {@code cumulAvecIcp} = {@code true} systématiquement,
 * {@code avertissement} non-null systématiquement.</p>
 */
public final class LicenciementBeCct109DeraisonnableCalculator {

    static final String BASE_JURIDIQUE =
            "CCT n° 109 du 12/02/2014 art. 9 ; arrêt Conseil d'État "
                    + "n° 245.236 du 27/06/2019";

    static final String AVERT_CUMUL_ICP =
            "Cette indemnité se cumule avec l'ICP (indemnité compensatoire "
                    + "de préavis). Calculer l'ICP séparément via l'outil "
                    + "Préavis statut unique ou Formule Claeys.";

    static final int SEMAINES_MINIMUM = 3;
    static final int SEMAINES_GRAVITE_ORDINAIRE = 8;
    static final int SEMAINES_GRAVITE_HAUTE = 12;
    static final int SEMAINES_MAXIMUM = 17;

    private LicenciementBeCct109DeraisonnableCalculator() {
    }

    public static LicenciementBeCct109DeraisonnableResult analyze(
            LicenciementBeCct109DeraisonnableRequest request) {
        validateInputs(request);

        EchelonChoisi choisi = determineEchelon(request);

        BigDecimal indemnite = choisi.echelon == LicenciementBeCct109EchelonEnum.NON_DERAISONNABLE
                ? BigDecimal.ZERO
                : request.remunerationHebdomadaireBrute()
                        .multiply(BigDecimal.valueOf(choisi.semaines));

        return new LicenciementBeCct109DeraisonnableResult(
                request.motifCommunique(),
                request.motifLieAPersonne(),
                request.discriminationSuspectee(),
                request.represaillesSuspectees(),
                request.proceduresRespectees(),
                request.remunerationHebdomadaireBrute(),
                request.argumentsPatronal(),
                choisi.echelon,
                choisi.semaines,
                indemnite,
                choisi.justification,
                /* cumulAvecIcp = */ true,
                BASE_JURIDIQUE,
                AVERT_CUMUL_ICP);
    }

    // ── Choix de l'échelon (priorité descendante) ─────────────────────────

    private static EchelonChoisi determineEchelon(LicenciementBeCct109DeraisonnableRequest r) {
        // 1) Motif valable prouvé + procédures respectées → non déraisonnable.
        if (r.motifLieAPersonne() == LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PROUVE_VALIDE
                && r.proceduresRespectees()) {
            return new EchelonChoisi(
                    LicenciementBeCct109EchelonEnum.NON_DERAISONNABLE,
                    0,
                    "Motif valable prouvé et procédures respectées — "
                            + "licenciement non manifestement déraisonnable au "
                            + "sens de l'art. 9 CCT 109, aucune indemnité due.");
        }

        // 2) Discrimination + représailles cumulés → maximum 17 semaines.
        if (r.discriminationSuspectee() && r.represaillesSuspectees()) {
            return new EchelonChoisi(
                    LicenciementBeCct109EchelonEnum.DIX_SEPT_SEMAINES,
                    SEMAINES_MAXIMUM,
                    "Discrimination ET représailles suspectées — circonstances "
                            + "aggravantes multiples justifiant l'échelon maximum "
                            + "(17 semaines) de l'art. 9 CCT 109.");
        }

        // 3) Discrimination OU représailles (1 seul) → 12 semaines.
        if (r.discriminationSuspectee() || r.represaillesSuspectees()) {
            String motif = r.discriminationSuspectee() ? "Discrimination" : "Représailles";
            return new EchelonChoisi(
                    LicenciementBeCct109EchelonEnum.DOUZE_SEMAINES,
                    SEMAINES_GRAVITE_HAUTE,
                    motif + " suspectée — circonstance aggravante justifiant "
                            + "un échelon de gravité haute (12 semaines).");
        }

        // 4) Motif précis disputé + procédures non respectées (facteur
        //    aggravant mineur) → 8 semaines (gravité ordinaire).
        if (r.motifLieAPersonne() == LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_PRECIS_DISPUTE
                && !r.proceduresRespectees()) {
            return new EchelonChoisi(
                    LicenciementBeCct109EchelonEnum.HUIT_SEMAINES,
                    SEMAINES_GRAVITE_ORDINAIRE,
                    "Motif précis mais disputé — un facteur aggravant mineur "
                            + "détecté (procédure incomplète) justifiant "
                            + "l'échelon de gravité ordinaire (8 semaines).");
        }

        // 5) Pas de motif communiqué OU SANS_MOTIF → minimum 3 semaines.
        if (Boolean.FALSE.equals(r.motifCommunique())
                || r.motifLieAPersonne() == LicenciementBeCct109MotifLieAPersonneEnum.SANS_MOTIF) {
            return new EchelonChoisi(
                    LicenciementBeCct109EchelonEnum.TROIS_SEMAINES,
                    SEMAINES_MINIMUM,
                    "Aucun motif valable communiqué — minimum légal de "
                            + "3 semaines (art. 9 CCT 109).");
        }

        // 6) Motif vague OU procédures non respectées → 3 semaines.
        if (r.motifLieAPersonne() == LicenciementBeCct109MotifLieAPersonneEnum.MOTIF_VAGUE
                || !r.proceduresRespectees()) {
            return new EchelonChoisi(
                    LicenciementBeCct109EchelonEnum.TROIS_SEMAINES,
                    SEMAINES_MINIMUM,
                    "Motif vague ou procédures non respectées — minimum légal "
                            + "de 3 semaines (art. 9 CCT 109).");
        }

        // 7) Fallback prudent : motif précis disputé sans aggravant clair —
        //    on retient le minimum CCT 109 (3 semaines) ; à raffiner par
        //    l'avocat sur les pièces du dossier.
        return new EchelonChoisi(
                LicenciementBeCct109EchelonEnum.TROIS_SEMAINES,
                SEMAINES_MINIMUM,
                "Motif précis disputé sans facteur aggravant identifié — "
                        + "minimum légal de 3 semaines retenu par défaut ; "
                        + "à raffiner sur pièces.");
    }

    // ── Validation conditionnelle ─────────────────────────────────────────

    private static void validateInputs(LicenciementBeCct109DeraisonnableRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.motifCommunique() == null) {
            throw new IllegalArgumentException("motifCommunique est requis");
        }
        if (request.motifLieAPersonne() == null) {
            throw new IllegalArgumentException("motifLieAPersonne est requis");
        }
        if (request.remunerationHebdomadaireBrute() == null) {
            throw new IllegalArgumentException(
                    "remunerationHebdomadaireBrute est requise");
        }
        if (request.remunerationHebdomadaireBrute().signum() <= 0) {
            throw new IllegalArgumentException(
                    "remunerationHebdomadaireBrute doit être strictement positive");
        }
    }

    private record EchelonChoisi(
            LicenciementBeCct109EchelonEnum echelon,
            int semaines,
            String justification) {
    }
}
