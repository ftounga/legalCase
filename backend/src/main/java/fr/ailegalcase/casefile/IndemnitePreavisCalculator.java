package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-DT-25-01 : calculateur de l'indemnité compensatrice de préavis FR
 * (art. L.1234-1 Code du travail + CCN).
 *
 * <p>Règles légales (art. L.1234-1) en l'absence de stipulation conventionnelle plus favorable :</p>
 * <ul>
 *   <li>Ancienneté &lt; 6 mois → durée selon CCN ou usage (laissée à l'appréciation locale).</li>
 *   <li>6 mois ≤ ancienneté &lt; 24 mois → 1 mois de préavis.</li>
 *   <li>Ancienneté ≥ 24 mois → 2 mois de préavis.</li>
 * </ul>
 *
 * <p>Lorsque la convention collective est connue (paramètre {@code ccnDureeMois} non null),
 * la durée retenue est <strong>{@code max(durée légale, durée CCN)}</strong> en application
 * du principe de faveur.</p>
 *
 * <p>Si l'employeur a effectivement dispensé le salarié de préavis (paramètre {@code exemptionEmployeur=true}),
 * le montant de l'indemnité compensatrice est :
 * {@code montant = dureePreavisMois × salaireMensuelBrutEur}, arrondi au centime (HALF_UP).
 * Sinon le salarié exécute son préavis et reçoit son salaire normal — l'indemnité compensatrice
 * vaut 0 même si une durée de préavis est calculée à titre informatif.</p>
 *
 * <p>Le calculator est <strong>stateless</strong> et ne consulte pas la base : la résolution CCN
 * est confiée au service appelant qui passe ici la durée déjà résolue (ou null).</p>
 */
public final class IndemnitePreavisCalculator {

    /** Résultat consolidé de la résolution de la durée — passé au calculator par le service. */
    public record DureePreavisResolution(int dureeMois, IndemnitePreavisSourceDuree source, String articleCcn) {}

    /** Seuil légal court (mois) en deçà duquel la durée légale n'est pas définie. */
    private static final int SEUIL_LEGAL_COURT_MOIS = 6;

    /** Seuil légal long (mois) à partir duquel la durée légale passe à 2 mois. */
    private static final int SEUIL_LEGAL_LONG_MOIS = 24;

    private IndemnitePreavisCalculator() {}

    /**
     * Calcule l'indemnité compensatrice de préavis.
     *
     * @param ancienneteMois        ancienneté du salarié exprimée en mois (≥ 0)
     * @param fonction              catégorie professionnelle (obligatoire)
     * @param ccnCode               code CCN normalisé (nullable — si null, durée légale uniquement)
     * @param ccnDureeMois          durée prévue par la CCN pour la combinaison (fonction, ancienneté) — nullable
     * @param ccnArticle            référence de l'article CCN (utilisée dans la base juridique) — nullable
     * @param salaireMensuelBrutEur salaire mensuel brut (&gt; 0)
     * @param exemptionEmployeur    true si l'employeur a dispensé du préavis
     * @param dateRupture           date de rupture (utilisée dans la formule explicative)
     * @return résultat structuré (durée + montant + formule + base juridique + messages)
     * @throws IllegalArgumentException si un paramètre est invalide
     */
    public static IndemnitePreavisResult compute(int ancienneteMois,
                                                 IndemnitePreavisFonction fonction,
                                                 String ccnCode,
                                                 Integer ccnDureeMois,
                                                 String ccnArticle,
                                                 BigDecimal salaireMensuelBrutEur,
                                                 boolean exemptionEmployeur,
                                                 LocalDate dateRupture) {
        if (ancienneteMois < 0) {
            throw new IllegalArgumentException("Ancienneté en mois requise et positive");
        }
        if (fonction == null) {
            throw new IllegalArgumentException("Fonction du salarié requise");
        }
        if (salaireMensuelBrutEur == null || salaireMensuelBrutEur.signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel brut requis et strictement positif");
        }
        if (dateRupture == null) {
            throw new IllegalArgumentException("Date de rupture requise");
        }
        if (ccnDureeMois != null && ccnDureeMois < 0) {
            throw new IllegalArgumentException("Durée CCN invalide (négative)");
        }

        DureePreavisResolution resolution = resolveDuree(ancienneteMois, ccnCode, ccnDureeMois, ccnArticle);

        BigDecimal salaireRounded = salaireMensuelBrutEur.setScale(2, RoundingMode.HALF_UP);

        boolean exemptionRetenue = exemptionEmployeur && resolution.dureeMois() > 0;
        BigDecimal montant = exemptionRetenue
                ? salaireRounded.multiply(new BigDecimal(resolution.dureeMois())).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        String baseJuridique = buildBaseJuridique(resolution, ccnCode);
        String formule = buildFormule(resolution, salaireRounded, montant, exemptionEmployeur);
        List<String> messages = buildMessages(resolution, exemptionEmployeur, ancienneteMois, fonction, ccnCode, ccnDureeMois);

        return new IndemnitePreavisResult(
                ancienneteMois,
                fonction,
                ccnCode,
                salaireRounded,
                exemptionEmployeur,
                exemptionRetenue,
                dateRupture,
                resolution.dureeMois(),
                resolution.source(),
                montant,
                baseJuridique,
                formule,
                messages
        );
    }

    /**
     * Détermine la durée de préavis applicable selon la règle de faveur.
     *
     * <ol>
     *   <li>Calcule la durée légale (L.1234-1).</li>
     *   <li>Si une durée CCN est fournie : retient {@code max(légale, CCN)}.</li>
     *   <li>Si CCN strictement &gt; légale : source = CCN.</li>
     *   <li>Sinon source = LEGALE — sauf si ancienneté &lt; 6 mois <em>et</em> aucune CCN connue → source USAGE.</li>
     * </ol>
     */
    private static DureePreavisResolution resolveDuree(int ancienneteMois, String ccnCode, Integer ccnDureeMois, String ccnArticle) {
        int dureeLegaleMois;
        if (ancienneteMois >= SEUIL_LEGAL_LONG_MOIS) {
            dureeLegaleMois = 2;
        } else if (ancienneteMois >= SEUIL_LEGAL_COURT_MOIS) {
            dureeLegaleMois = 1;
        } else {
            dureeLegaleMois = 0;
        }

        if (ccnDureeMois != null && ccnDureeMois > dureeLegaleMois) {
            return new DureePreavisResolution(ccnDureeMois, IndemnitePreavisSourceDuree.CCN, ccnArticle);
        }
        if (dureeLegaleMois > 0) {
            return new DureePreavisResolution(dureeLegaleMois, IndemnitePreavisSourceDuree.LEGALE, null);
        }
        // Ancienneté < 6 mois et CCN absente ou ne prévoit rien de plus favorable.
        if (ccnDureeMois != null && ccnDureeMois > 0) {
            // CCN prévoit une durée pour les anciennetés courtes (ex. cadres).
            return new DureePreavisResolution(ccnDureeMois, IndemnitePreavisSourceDuree.CCN, ccnArticle);
        }
        return new DureePreavisResolution(0, IndemnitePreavisSourceDuree.USAGE, null);
    }

    private static String buildBaseJuridique(DureePreavisResolution resolution, String ccnCode) {
        return switch (resolution.source()) {
            case LEGALE -> "Art. L.1234-1 Code du travail";
            case CCN -> {
                String article = (resolution.articleCcn() != null && !resolution.articleCcn().isBlank())
                        ? " " + resolution.articleCcn()
                        : "";
                String ccn = ccnCode != null ? ccnCode : "CCN";
                yield "Art. L.1234-1 Code du travail + " + ccn + article;
            }
            case USAGE -> "Art. L.1234-1 Code du travail (durée selon usage local — ancienneté < 6 mois)";
        };
    }

    private static String buildFormule(DureePreavisResolution resolution,
                                       BigDecimal salaire,
                                       BigDecimal montant,
                                       boolean exemptionEmployeur) {
        if (resolution.dureeMois() == 0) {
            return "0 mois × " + formatMontant(salaire) + " € = 0,00 €";
        }
        if (!exemptionEmployeur) {
            return resolution.dureeMois() + " mois (préavis exécuté — pas d'indemnité due)";
        }
        return resolution.dureeMois() + " mois × " + formatMontant(salaire) + " € = " + formatMontant(montant) + " €";
    }

    private static List<String> buildMessages(DureePreavisResolution resolution,
                                              boolean exemptionEmployeur,
                                              int ancienneteMois,
                                              IndemnitePreavisFonction fonction,
                                              String ccnCode,
                                              Integer ccnDureeMois) {
        List<String> msgs = new ArrayList<>();

        switch (resolution.source()) {
            case LEGALE -> {
                msgs.add("Durée retenue : durée légale (art. L.1234-1) — "
                        + (ancienneteMois >= SEUIL_LEGAL_LONG_MOIS
                                ? "2 mois car ancienneté ≥ 2 ans."
                                : "1 mois car ancienneté ∈ [6 mois ; 2 ans[."));
                if (ccnDureeMois != null && ccnDureeMois <= resolution.dureeMois()) {
                    msgs.add("La CCN " + ccnCode + " prévoit " + ccnDureeMois
                            + " mois pour la fonction " + fonction
                            + " — la durée légale est plus favorable, c'est elle qui s'applique (principe de faveur, art. L.2253-3).");
                }
            }
            case CCN -> {
                msgs.add("Durée retenue : convention collective " + ccnCode
                        + " (" + resolution.dureeMois() + " mois) — plus favorable que la durée légale.");
                msgs.add("Vérifier que la CCN " + ccnCode + " est bien applicable au contrat (mention au contrat ou à défaut activité principale de l'employeur).");
            }
            case USAGE -> {
                msgs.add("Ancienneté inférieure à 6 mois : la durée légale n'est pas définie (art. L.1234-1).");
                msgs.add("Vérifier la durée de préavis prévue par la CCN applicable ou, à défaut, l'usage local du secteur.");
            }
        }

        if (exemptionEmployeur && resolution.dureeMois() > 0) {
            msgs.add("L'employeur a dispensé du préavis : indemnité compensatrice due (art. L.1234-5) — équivaut au salaire que le salarié aurait perçu pendant le préavis.");
            msgs.add("L'indemnité reste soumise à cotisations sociales et à l'impôt sur le revenu.");
        } else if (!exemptionEmployeur) {
            msgs.add("L'employeur n'a pas dispensé du préavis : aucune indemnité compensatrice n'est due. Le salarié exécute son préavis et perçoit son salaire normal.");
        } else {
            // exemption=true mais durée=0 (USAGE) → message dédié
            msgs.add("L'employeur a dispensé du préavis mais la durée n'est pas déterminée (usage) : préciser la durée applicable avant de chiffrer l'indemnité.");
        }

        return msgs;
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
