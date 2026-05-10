package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-210-01 : analyseur de la tentative obligatoire de médiation familiale
 * avant saisine JAF (Cciv art. 373-2-10 al. 3 modifié par Loi n°2016-1547 du
 * 18/11/2016 + CPC art. 1108).
 *
 * <p>Règle : à peine d'irrecevabilité que le juge peut soulever d'office, la
 * saisine du JAF par un parent aux fins de modification d'une décision
 * relative à l'autorité parentale (ou à la contribution à l'entretien) doit
 * être précédée d'une tentative de médiation familiale, sauf si :
 * <ul>
 *   <li>la demande émane conjointement des parents et tend à homologuer un
 *       accord ;</li>
 *   <li>l'absence de recours préalable est justifiée par un motif légitime
 *       (urgence, violences conjugales, motif personnel grave) ;</li>
 *   <li>la non-saisine paraît contraire à l'intérêt de l'enfant.</li>
 * </ul>
 *
 * <p>Verdicts :
 * <ul>
 *   <li>{@code RECEVABLE} : médiation tentée OU exception applicable.</li>
 *   <li>{@code IRRECEVABLE} : motif in-scope, pas de tentative, pas d'exception.</li>
 *   <li>{@code NON_CONCERNE} : motif hors champ de la médiation obligatoire
 *       (divorce contentieux, succession, autre…).</li>
 * </ul>
 */
public final class MediationFamilialePreSaisineCalculator {

    static final String BASE_JURIDIQUE =
            "Cciv art. 373-2-10 al. 3 + Loi 18/11/2016 + CPC art. 1108";

    private MediationFamilialePreSaisineCalculator() {
    }

    public static MediationFamilialePreSaisineResult compute(
            MediationFamilialePreSaisineMotif motifSaisine,
            boolean mediationTentee,
            LocalDate dateMediation,
            MediationFamilialePreSaisineException exceptionApplicable,
            String exceptionDetail) {

        if (motifSaisine == null) {
            throw new IllegalArgumentException("motifSaisine est requis");
        }
        MediationFamilialePreSaisineException exc = exceptionApplicable != null
                ? exceptionApplicable
                : MediationFamilialePreSaisineException.AUCUNE;

        // Cas 1 : motif hors champ → NON_CONCERNE
        if (!motifSaisine.isInScope()) {
            return new MediationFamilialePreSaisineResult(
                    motifSaisine.name(),
                    mediationTentee,
                    dateMediation,
                    exc.name(),
                    exceptionDetail,
                    "NON_CONCERNE",
                    false,
                    false,
                    Collections.emptyList(),
                    BASE_JURIDIQUE,
                    "Motif hors champ de l'art. 373-2-10 al. 3 — médiation préalable non obligatoire.",
                    buildMessagesNonConcerne(motifSaisine));
        }

        // Cas 2 : exception applicable → RECEVABLE par dispense
        if (exc != MediationFamilialePreSaisineException.AUCUNE) {
            List<String> pieces = piecesPourException(exc);
            return new MediationFamilialePreSaisineResult(
                    motifSaisine.name(),
                    mediationTentee,
                    dateMediation,
                    exc.name(),
                    exceptionDetail,
                    "RECEVABLE",
                    true,
                    true,
                    pieces,
                    BASE_JURIDIQUE,
                    formuleDispense(motifSaisine, exc),
                    buildMessagesDispense(motifSaisine, exc, exceptionDetail));
        }

        // Cas 3 : motif in-scope + médiation tentée → RECEVABLE
        if (mediationTentee) {
            List<String> pieces = new ArrayList<>();
            pieces.add("Attestation de tentative de médiation familiale (médiateur agréé)");
            return new MediationFamilialePreSaisineResult(
                    motifSaisine.name(),
                    true,
                    dateMediation,
                    exc.name(),
                    exceptionDetail,
                    "RECEVABLE",
                    true,
                    false,
                    pieces,
                    BASE_JURIDIQUE,
                    formuleTentative(motifSaisine, dateMediation),
                    buildMessagesRecevable(motifSaisine, dateMediation));
        }

        // Cas 4 : motif in-scope + pas de tentative + pas d'exception → IRRECEVABLE
        List<String> pieces = new ArrayList<>();
        pieces.add("Attestation de tentative de médiation familiale (à produire avant la saisine JAF)");
        pieces.add("OU justificatif d'une exception (urgence / violences / motif légitime)");
        return new MediationFamilialePreSaisineResult(
                motifSaisine.name(),
                false,
                null,
                exc.name(),
                exceptionDetail,
                "IRRECEVABLE",
                true,
                false,
                pieces,
                BASE_JURIDIQUE,
                formuleIrrecevable(motifSaisine),
                buildMessagesIrrecevable(motifSaisine));
    }

    // -----------------------------------------------------------------------
    // Helpers de formulation
    // -----------------------------------------------------------------------

    private static String formuleDispense(MediationFamilialePreSaisineMotif motif,
                                          MediationFamilialePreSaisineException exc) {
        return "Saisine recevable par dispense — exception "
                + libelleException(exc) + " applicable au motif "
                + libelleMotif(motif) + ".";
    }

    private static String formuleTentative(MediationFamilialePreSaisineMotif motif,
                                           LocalDate dateMediation) {
        String suffixe = (dateMediation != null)
                ? " (médiation tentée le " + dateMediation + ")"
                : " (médiation déclarée tentée — date à documenter)";
        return "Saisine recevable" + suffixe + " — motif " + libelleMotif(motif) + ".";
    }

    private static String formuleIrrecevable(MediationFamilialePreSaisineMotif motif) {
        return "Saisine IRRECEVABLE en l'état — médiation obligatoire pour le motif "
                + libelleMotif(motif)
                + ". Tenter une médiation OU justifier d'une exception (art. 373-2-10 al. 3 Cciv).";
    }

    private static List<String> buildMessagesNonConcerne(MediationFamilialePreSaisineMotif motif) {
        List<String> m = new ArrayList<>();
        m.add("Motif " + libelleMotif(motif) + " hors du champ de l'obligation de médiation pré-saisine (art. 373-2-10 al. 3 Cciv).");
        m.add("Vérifier les autres voies procédurales applicables (divorce, succession, etc.).");
        return m;
    }

    private static List<String> buildMessagesDispense(MediationFamilialePreSaisineMotif motif,
                                                      MediationFamilialePreSaisineException exc,
                                                      String detail) {
        List<String> m = new ArrayList<>();
        m.add("Exception " + libelleException(exc) + " — saisine directe possible.");
        if (detail != null && !detail.isBlank()) {
            m.add("Détail justificatif : " + detail);
        }
        m.add("À documenter au dossier : pièces justificatives de l'exception au moment de la saisine.");
        if (exc == MediationFamilialePreSaisineException.URGENCE) {
            m.add("L'urgence doit être caractérisée (atteinte imminente à l'intérêt de l'enfant).");
        }
        if (exc == MediationFamilialePreSaisineException.VIOLENCE) {
            m.add("Violences : main courante / plainte / certificat médical / ordonnance de protection en cours.");
        }
        return m;
    }

    private static List<String> buildMessagesRecevable(MediationFamilialePreSaisineMotif motif,
                                                       LocalDate dateMediation) {
        List<String> m = new ArrayList<>();
        m.add("Saisine du JAF recevable — tentative de médiation préalablement effectuée.");
        if (dateMediation == null) {
            m.add("Documenter la date de la tentative et joindre l'attestation du médiateur.");
        }
        m.add("Joindre l'attestation de tentative de médiation à la requête.");
        return m;
    }

    private static List<String> buildMessagesIrrecevable(MediationFamilialePreSaisineMotif motif) {
        List<String> m = new ArrayList<>();
        m.add("Saisine IRRECEVABLE en l'état — risque que le juge soulève d'office (art. 373-2-10 al. 3).");
        m.add("Étapes : (1) saisir un médiateur familial agréé, (2) obtenir attestation de tentative, (3) saisir le JAF.");
        m.add("Alternative : caractériser une exception (urgence, violences, motif légitime) avec pièces justificatives.");
        return m;
    }

    private static List<String> piecesPourException(MediationFamilialePreSaisineException exc) {
        List<String> pieces = new ArrayList<>();
        switch (exc) {
            case URGENCE -> pieces.add("Justificatif de l'urgence (atteinte imminente, contexte factuel daté)");
            case VIOLENCE -> {
                pieces.add("Main courante / plainte / récépissé de plainte");
                pieces.add("Certificat médical de coups et blessures (le cas échéant)");
                pieces.add("Ordonnance de protection en cours (le cas échéant — art. 515-9 Cciv)");
            }
            case ACCORD_PREALABLE -> pieces.add("Document attestant l'accord conjoint des parents");
            case MOTIF_LEGITIME -> pieces.add("Justificatif du motif légitime invoqué");
            case AUTRE -> pieces.add("Documenter précisément le motif d'exception au dossier");
            case AUCUNE -> {
                // Ne devrait pas être appelé
            }
        }
        return pieces;
    }

    private static String libelleMotif(MediationFamilialePreSaisineMotif motif) {
        return switch (motif) {
            case AUTORITE_PARENTALE -> "modalités d'autorité parentale";
            case CONTRIBUTION_ENTRETIEN -> "contribution à l'entretien et à l'éducation";
            case RESIDENCE_ENFANT -> "résidence de l'enfant";
            case DROIT_VISITE -> "droit de visite et d'hébergement";
            case DIVORCE_CONTENTIEUX -> "divorce contentieux";
            case SUCCESSION -> "succession";
            case AUTRE -> "autre";
        };
    }

    private static String libelleException(MediationFamilialePreSaisineException exc) {
        return switch (exc) {
            case URGENCE -> "urgence";
            case VIOLENCE -> "violences";
            case ACCORD_PREALABLE -> "accord préalable conjoint";
            case MOTIF_LEGITIME -> "motif légitime";
            case AUTRE -> "autre exception";
            case AUCUNE -> "aucune";
        };
    }
}
