package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-31 : analyseur de la <b>validité d'un accord d'entreprise</b> au regard
 * des conditions de majorité issues de la loi Travail 2016 et de l'ordonnance
 * 2017 (art. L.2232-12 CT ; révision / dénonciation : art. L.2261-7 et s. CT,
 * F-DT-67). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Condition de majorité</b> (toutes opérations) :
 *       <ul>
 *         <li>signataires ≥ 50 % → {@link AccordConditionMajorite#MAJORITE_50}
 *             (accord valide sans référendum) ;</li>
 *         <li>30 % ≤ signataires &lt; 50 % avec référendum organisé ET approuvé →
 *             {@link AccordConditionMajorite#REFERENDUM_30} (valide par
 *             référendum) ;</li>
 *         <li>30 % ≤ signataires &lt; 50 % sans référendum approuvé, ou
 *             signataires &lt; 30 % → {@link AccordConditionMajorite#INSUFFISANTE}
 *             (non valide en l'état).</li>
 *       </ul></li>
 *   <li><b>Révision</b> : ajoute la vérification {@code signePartiesHabilitees}
 *       (parties habilitées à engager la procédure, art. L.2261-7).</li>
 *   <li><b>Dénonciation</b> : vérifie {@code preavisDenonciationRespecte} et, si
 *       {@code dateDenonciation} est fournie, calcule
 *       {@code dateFinSurvie = dateDenonciation + 3 mois (préavis) + 12 mois
 *       (survie)} = +15 mois.</li>
 *   <li><b>Verdict</b> :
 *       <ul>
 *         <li>majorité remplie (et, selon l'opération, parties habilitées /
 *             préavis respecté) → {@link AccordEntrepriseValiditeStatut#VALIDE} ;
 *             {@link AccordEntrepriseValiditeStatut#VALIDE_SOUS_RESERVE} si la
 *             majorité est atteinte par référendum (REFERENDUM_30) ;</li>
 *         <li>condition de majorité non remplie, ou item d'opération non satisfait
 *             → {@link AccordEntrepriseValiditeStatut#NON_VALIDE}.</li>
 *       </ul></li>
 * </ul>
 *
 * <p>Hors périmètre : contenu de fond / opposabilité d'une clause, NAO (F-DT-66),
 * accord de branche / extension. Base juridique annotée « à vérifier par avocat ».
 */
public final class AccordEntrepriseValiditeAnalyzer {

    /** Seuil de majorité directe — signature &gt; 50 % des suffrages exprimés. */
    static final BigDecimal SEUIL_MAJORITE = new BigDecimal("50");

    /** Seuil d'accès au référendum de validation — signataires ≥ 30 %. */
    static final BigDecimal SEUIL_REFERENDUM = new BigDecimal("30");

    static final BigDecimal CENT = new BigDecimal("100");
    static final BigDecimal ZERO = BigDecimal.ZERO;

    /** Préavis légal de dénonciation (mois, art. L.2261-9). */
    static final int PREAVIS_DENONCIATION_MOIS = 3;

    /** Durée de survie de l'accord dénoncé (mois, art. L.2261-10/11). */
    static final int SURVIE_MOIS = 12;

    static final String BASE_JURIDIQUE =
            "art. L.2232-12 CT — validité d'un accord d'entreprise : signature par "
                    + "des syndicats représentatifs ayant recueilli plus de 50 % des "
                    + "suffrages exprimés au 1er tour des dernières élections ; à défaut "
                    + "(signataires ≥ 30 %), validation par référendum des salariés à la "
                    + "majorité des suffrages exprimés ; art. L.2261-7 et suivants CT — "
                    + "révision (parties habilitées à engager la procédure) et dénonciation "
                    + "(préavis de 3 mois, maintien des effets pendant 12 mois — survie de "
                    + "l'accord) (à vérifier par avocat)";

    private AccordEntrepriseValiditeAnalyzer() {
    }

    /**
     * Analyse la validité de l'accord : qualifie la condition de majorité,
     * construit la checklist (avec les items propres à l'opération), calcule la
     * fin de survie en dénonciation et rend le verdict global.
     */
    public static AccordEntrepriseValiditeResult analyze(
            BigDecimal pourcentageSuffragesSignataires,
            Boolean referendumOrganise,
            Boolean referendumApprouve,
            AccordTypeOperation typeOperation,
            Boolean signePartiesHabilitees,
            Boolean preavisDenonciationRespecte,
            LocalDate dateDenonciation) {

        validate(pourcentageSuffragesSignataires, typeOperation, signePartiesHabilitees);

        BigDecimal pourcentage = pourcentageSuffragesSignataires;
        boolean refOrganise = referendumOrganise != null && referendumOrganise;
        boolean refApprouve = referendumApprouve != null && referendumApprouve;
        boolean refValide = refOrganise && refApprouve;

        List<AccordValiditeChecklistItem> checklist = new ArrayList<>();
        List<String> consequences = new ArrayList<>();

        // ── Condition de majorité (art. L.2232-12) ──────────────────────────
        AccordConditionMajorite conditionMajorite;
        boolean majoriteRemplie;
        if (pourcentage.compareTo(SEUIL_MAJORITE) >= 0) {
            conditionMajorite = AccordConditionMajorite.MAJORITE_50;
            majoriteRemplie = true;
            checklist.add(new AccordValiditeChecklistItem(
                    "Majorité de signature (> 50 % des suffrages exprimés au 1er tour)",
                    true,
                    "Les syndicats signataires ont recueilli au moins 50 % des suffrages "
                            + "exprimés au 1er tour des dernières élections : accord valide "
                            + "sans référendum (art. L.2232-12)."));
        } else if (pourcentage.compareTo(SEUIL_REFERENDUM) >= 0 && refValide) {
            conditionMajorite = AccordConditionMajorite.REFERENDUM_30;
            majoriteRemplie = true;
            checklist.add(new AccordValiditeChecklistItem(
                    "Majorité atteinte par référendum (signataires ≥ 30 % + approbation)",
                    true,
                    "Les signataires atteignent au moins 30 % des suffrages exprimés et "
                            + "l'accord a été approuvé par référendum des salariés à la "
                            + "majorité des suffrages exprimés (art. L.2232-12) : validité "
                            + "subordonnée à la régularité du référendum."));
        } else {
            conditionMajorite = AccordConditionMajorite.INSUFFISANTE;
            majoriteRemplie = false;
            if (pourcentage.compareTo(SEUIL_REFERENDUM) >= 0) {
                checklist.add(new AccordValiditeChecklistItem(
                        "Majorité de signature (> 50 % des suffrages exprimés au 1er tour)",
                        false,
                        "Signataires entre 30 % et 50 % : un référendum de validation des "
                                + "salariés est nécessaire et doit être approuvé (art. L.2232-12). "
                                + "À défaut, l'accord n'est pas valide en l'état."));
                consequences.add("Organiser un référendum de validation des salariés (signataires "
                        + "≥ 30 % et < 50 %) et le faire approuver à la majorité des suffrages "
                        + "exprimés (art. L.2232-12) : à défaut, l'accord n'est pas valide.");
            } else {
                checklist.add(new AccordValiditeChecklistItem(
                        "Majorité de signature (> 50 % des suffrages exprimés au 1er tour)",
                        false,
                        "Signataires en deçà de 30 % des suffrages exprimés : le référendum "
                                + "de validation est impossible (art. L.2232-12). L'accord ne "
                                + "peut être valide en l'état."));
                consequences.add("Atteindre le seuil de 30 % de signataires pour pouvoir recourir "
                        + "au référendum de validation, ou le seuil de 50 % pour une validité "
                        + "directe (art. L.2232-12) : seuil non atteint, accord non valide.");
            }
        }

        // ── Item référendum (si organisé) ───────────────────────────────────
        if (refOrganise) {
            checklist.add(new AccordValiditeChecklistItem(
                    "Référendum de validation approuvé",
                    refApprouve,
                    refApprouve
                            ? "Le référendum de validation a approuvé l'accord à la majorité des "
                                    + "suffrages exprimés (art. L.2232-12)."
                            : "Le référendum de validation a été organisé mais n'a pas approuvé "
                                    + "l'accord : l'accord n'est pas valide (art. L.2232-12)."));
            if (!refApprouve) {
                consequences.add("Le référendum de validation organisé n'a pas approuvé l'accord "
                        + "(art. L.2232-12) : l'accord est réputé non écrit.");
            }
        }

        // ── Item propre à l'opération ───────────────────────────────────────
        boolean itemOperationConforme = true;
        if (typeOperation == AccordTypeOperation.REVISION) {
            boolean partiesHabilitees = signePartiesHabilitees != null && signePartiesHabilitees;
            itemOperationConforme = partiesHabilitees;
            checklist.add(new AccordValiditeChecklistItem(
                    "Avenant signé par les parties habilitées à réviser",
                    partiesHabilitees,
                    "La révision doit être engagée et signée par les parties habilitées "
                            + "(organisations signataires ou adhérentes selon la période, "
                            + "art. L.2261-7 et s.)."));
            if (!partiesHabilitees) {
                consequences.add("Faire signer l'avenant de révision par les parties habilitées "
                        + "(art. L.2261-7 et s.) : l'avenant n'est pas valablement conclu.");
            }
        } else if (typeOperation == AccordTypeOperation.DENONCIATION) {
            boolean preavisRespecte = preavisDenonciationRespecte == null || preavisDenonciationRespecte;
            itemOperationConforme = preavisRespecte;
            checklist.add(new AccordValiditeChecklistItem(
                    "Préavis de dénonciation de 3 mois respecté",
                    preavisRespecte,
                    "La dénonciation suppose le respect d'un préavis de 3 mois ; l'accord "
                            + "survit ensuite pendant 12 mois (art. L.2261-9 à L.2261-11)."));
            if (!preavisRespecte) {
                consequences.add("Respecter le préavis de dénonciation de 3 mois (art. L.2261-9) : "
                        + "préavis non respecté.");
            }
        }

        // ── Calcul de la fin de survie (dénonciation) ───────────────────────
        LocalDate dateFinSurvie = null;
        if (typeOperation == AccordTypeOperation.DENONCIATION && dateDenonciation != null) {
            dateFinSurvie = dateDenonciation
                    .plusMonths(PREAVIS_DENONCIATION_MOIS)
                    .plusMonths(SURVIE_MOIS);
            consequences.add("Fin de survie de l'accord dénoncé estimée au "
                    + dateFinSurvie + " (dénonciation + 3 mois de préavis + 12 mois de survie, "
                    + "art. L.2261-9 à L.2261-11) : à défaut d'accord de substitution, les "
                    + "salariés conservent la rémunération acquise.");
        }

        int itemsNonConformes = (int) checklist.stream()
                .filter(i -> !i.conforme())
                .count();

        // ── Verdict global ──────────────────────────────────────────────────
        AccordEntrepriseValiditeStatut statut;
        if (!majoriteRemplie || !itemOperationConforme) {
            statut = AccordEntrepriseValiditeStatut.NON_VALIDE;
        } else if (conditionMajorite == AccordConditionMajorite.REFERENDUM_30) {
            statut = AccordEntrepriseValiditeStatut.VALIDE_SOUS_RESERVE;
            consequences.add("Validité subordonnée à la régularité du référendum de validation "
                    + "(art. L.2232-12) : vérifier les conditions d'organisation et de "
                    + "participation.");
        } else {
            statut = AccordEntrepriseValiditeStatut.VALIDE;
        }

        return new AccordEntrepriseValiditeResult(
                pourcentage,
                typeOperation,
                refOrganise,
                refApprouve,
                conditionMajorite,
                typeOperation == AccordTypeOperation.DENONCIATION ? dateDenonciation : null,
                dateFinSurvie,
                List.copyOf(checklist),
                itemsNonConformes,
                statut,
                List.copyOf(consequences),
                BASE_JURIDIQUE);
    }

    private static void validate(BigDecimal pourcentageSuffragesSignataires,
                                 AccordTypeOperation typeOperation,
                                 Boolean signePartiesHabilitees) {
        if (pourcentageSuffragesSignataires == null) {
            throw new IllegalArgumentException("pourcentageSuffragesSignataires est requis");
        }
        if (pourcentageSuffragesSignataires.compareTo(ZERO) < 0
                || pourcentageSuffragesSignataires.compareTo(CENT) > 0) {
            throw new IllegalArgumentException(
                    "pourcentageSuffragesSignataires doit être compris entre 0 et 100");
        }
        if (typeOperation == null) {
            throw new IllegalArgumentException("typeOperation est requis");
        }
        if (typeOperation == AccordTypeOperation.REVISION && signePartiesHabilitees == null) {
            throw new IllegalArgumentException(
                    "signePartiesHabilitees est requis pour une révision (art. L.2261-7)");
        }
    }
}
