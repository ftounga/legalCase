package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-20-01 : calculateur de la légalité d'une mesure d'éloignement administrative française
 * autre que l'OQTF (qui est traité par F-IM-08).
 *
 * <p>Bases juridiques :
 * <ul>
 *   <li>CESEDA art. L.631-1 — expulsion préfectorale (menace grave à l'ordre public)</li>
 *   <li>CESEDA art. L.631-2 — expulsion ministérielle (urgence absolue)</li>
 *   <li>CESEDA art. L.631-3 — expulsion pour nécessité impérieuse / sécurité État</li>
 *   <li>CESEDA art. L.612-6 et s. — IRTF (interdiction de retour sur le territoire français)</li>
 *   <li>CESEDA art. L.222-1 et s. — IAT (interdiction administrative du territoire)</li>
 *   <li>CESEDA art. L.632-1 et s. — procédure commission expulsion</li>
 *   <li>CJA art. R.421-1 — délai 1 mois TA pour acte administratif général</li>
 *   <li>CJA art. R.311-1 — compétence directe CE pour acte ministériel</li>
 *   <li>CESEDA art. L.614-4 — délai 15 jours TA recours suspensif (IRTF associé OQTF)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent belge (Loi 15/12/1980 art. 20-21, 74/15)
 * sera couvert par F-IM-20-BE (backlog).
 *
 * <p>Verdict :
 * <ul>
 *   <li><b>VALIDE</b> : tous les critères du dispositif réunis, procédure respectée.</li>
 *   <li><b>CONTESTABLE</b> : motif faible, vice de procédure rattrapable, élément à nuancer.</li>
 *   <li><b>NUL</b> : motif inexistant, fondement absent, mesure manifestement infondée.</li>
 * </ul>
 */
public final class MesuresEloignementCalculator {

    public static final String VERDICT_VALIDE = "VALIDE";
    public static final String VERDICT_CONTESTABLE = "CONTESTABLE";
    public static final String VERDICT_NUL = "NUL";

    public static final String DISP_EXP_PREFECTORALE = "EXPULSION_PREFECTORALE";
    public static final String DISP_EXP_MINISTERIELLE = "EXPULSION_MINISTERIELLE";
    public static final String DISP_EXP_SECURITE_ETAT = "EXPULSION_SECURITE_ETAT";
    public static final String DISP_IRTF = "IRTF";
    public static final String DISP_IAT = "IAT";

    public static final String MOTIF_ORDRE_PUBLIC = "ORDRE_PUBLIC";
    public static final String MOTIF_SECURITE_ETAT = "SECURITE_ETAT";
    public static final String MOTIF_TERRORISME = "TERRORISME";
    public static final String MOTIF_RECIDIVE_GRAVE = "RECIDIVE_GRAVE";
    public static final String MOTIF_AUTRE = "AUTRE";

    public static final String JURIDICTION_TA = "TA";
    public static final String JURIDICTION_CE = "CE";

    public static final int DELAI_RECOURS_IRTF_JOURS = 15;
    public static final int DELAI_RECOURS_PREFECTORAL_JOURS = 30;
    public static final int DELAI_RECOURS_MINISTERIEL_JOURS = 60;

    public static final int IRTF_DUREE_PRESENCE_BASE_MOIS = 12;

    private MesuresEloignementCalculator() {
    }

    public static MesuresEloignementResult compute(String dispositif,
                                                   String motifMenace,
                                                   Boolean procedureCommissionRespectee,
                                                   Boolean urgenceAbsolueJustifiee,
                                                   Integer dureeCircularitePrecaire,
                                                   Integer dureePresenceIrreguliereMois,
                                                   Boolean comportementAggravant,
                                                   LocalDate recoursDelai,
                                                   LocalDate dateAnalyse) {
        validateInputs(dispositif, motifMenace, dureeCircularitePrecaire,
                dureePresenceIrreguliereMois, recoursDelai, dateAnalyse);

        String d = dispositif.trim().toUpperCase();
        String motif = motifMenace.trim().toUpperCase();
        DispositifDescriptor descr = resolveDispositif(d);
        validateMotif(motif);

        List<String> risques = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        boolean commission = !Boolean.FALSE.equals(procedureCommissionRespectee);
        boolean urgence = Boolean.TRUE.equals(urgenceAbsolueJustifiee);
        boolean comportement = Boolean.TRUE.equals(comportementAggravant);

        String verdict = applyDispositif(descr, motif, commission, urgence,
                dureeCircularitePrecaire, dureePresenceIrreguliereMois, comportement,
                risques, documents, messages);

        // Délai recours expiré
        if (recoursDelai != null && dateAnalyse != null) {
            long joursRestants = ChronoUnit.DAYS.between(
                    dateAnalyse.minusDays(descr.delaiRecoursJours()), recoursDelai);
            if (joursRestants < 0) {
                messages.add("Délai de recours dépassé (" + descr.delaiRecoursJours()
                        + " jours depuis notification) — risque d'irrecevabilité.");
                risques.add("Recours probablement forclos — vérifier la date de notification.");
            }
        }

        String formule = buildFormule(descr, verdict);

        return new MesuresEloignementResult(
                d,
                descr.libelle(),
                motif,
                verdict,
                risques,
                descr.delaiRecoursJours(),
                descr.juridiction(),
                documents,
                descr.baseJuridique(),
                formule,
                messages);
    }

    private static void validateInputs(String dispositif,
                                       String motifMenace,
                                       Integer dureeCircularitePrecaire,
                                       Integer dureePresenceIrreguliereMois,
                                       LocalDate recoursDelai,
                                       LocalDate dateAnalyse) {
        if (dispositif == null || dispositif.isBlank()) {
            throw new IllegalArgumentException("dispositif est requis");
        }
        if (motifMenace == null || motifMenace.isBlank()) {
            throw new IllegalArgumentException("motifMenace est requis");
        }
        if (dureeCircularitePrecaire != null && dureeCircularitePrecaire < 0) {
            throw new IllegalArgumentException("dureeCircularitePrecaire doit être ≥ 0");
        }
        if (dureePresenceIrreguliereMois != null && dureePresenceIrreguliereMois < 0) {
            throw new IllegalArgumentException("dureePresenceIrreguliereMois doit être ≥ 0");
        }
        if (recoursDelai != null && dateAnalyse != null
                && recoursDelai.isAfter(dateAnalyse.plusYears(1))) {
            throw new IllegalArgumentException("recoursDelai trop éloignée (> 1 an dans le futur)");
        }
    }

    private static DispositifDescriptor resolveDispositif(String d) {
        switch (d) {
            case DISP_EXP_PREFECTORALE:
                return new DispositifDescriptor(DISP_EXP_PREFECTORALE,
                        "Expulsion préfectorale (CESEDA art. L.631-1)",
                        "CESEDA art. L.631-1 (expulsion préfectorale)",
                        DELAI_RECOURS_PREFECTORAL_JOURS, JURIDICTION_TA);
            case DISP_EXP_MINISTERIELLE:
                return new DispositifDescriptor(DISP_EXP_MINISTERIELLE,
                        "Expulsion ministérielle urgence absolue (CESEDA art. L.631-2)",
                        "CESEDA art. L.631-2 (expulsion ministérielle / urgence absolue)",
                        DELAI_RECOURS_MINISTERIEL_JOURS, JURIDICTION_CE);
            case DISP_EXP_SECURITE_ETAT:
                return new DispositifDescriptor(DISP_EXP_SECURITE_ETAT,
                        "Expulsion nécessité impérieuse sécurité État (CESEDA art. L.631-3)",
                        "CESEDA art. L.631-3 (nécessité impérieuse sécurité État)",
                        DELAI_RECOURS_MINISTERIEL_JOURS, JURIDICTION_CE);
            case DISP_IRTF:
                return new DispositifDescriptor(DISP_IRTF,
                        "Interdiction de retour sur le territoire français (CESEDA L.612-6+)",
                        "CESEDA art. L.612-6 et s. (IRTF — sanction associée OQTF)",
                        DELAI_RECOURS_IRTF_JOURS, JURIDICTION_TA);
            case DISP_IAT:
                return new DispositifDescriptor(DISP_IAT,
                        "Interdiction administrative du territoire (CESEDA L.222-1+)",
                        "CESEDA art. L.222-1 et s. (IAT — interdiction préventive)",
                        DELAI_RECOURS_MINISTERIEL_JOURS, JURIDICTION_CE);
            default:
                throw new IllegalArgumentException("Dispositif non supporté : " + d);
        }
    }

    private static void validateMotif(String motif) {
        switch (motif) {
            case MOTIF_ORDRE_PUBLIC:
            case MOTIF_SECURITE_ETAT:
            case MOTIF_TERRORISME:
            case MOTIF_RECIDIVE_GRAVE:
            case MOTIF_AUTRE:
                return;
            default:
                throw new IllegalArgumentException("motifMenace non supporté : " + motif);
        }
    }

    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "PMD.CyclomaticComplexity"})
    private static String applyDispositif(DispositifDescriptor descr,
                                          String motif,
                                          boolean commission,
                                          boolean urgence,
                                          Integer circularite,
                                          Integer presenceIrreg,
                                          boolean comportement,
                                          List<String> risques,
                                          List<String> documents,
                                          List<String> messages) {
        switch (descr.code()) {
            case DISP_EXP_PREFECTORALE:
                return applyExpulsionPrefectorale(motif, commission, urgence,
                        risques, documents, messages);
            case DISP_EXP_MINISTERIELLE:
                return applyExpulsionMinisterielle(motif, urgence,
                        risques, documents, messages);
            case DISP_EXP_SECURITE_ETAT:
                return applyExpulsionSecuriteEtat(motif,
                        risques, documents, messages);
            case DISP_IRTF:
                return applyIrtf(motif, presenceIrreg, circularite, comportement,
                        risques, documents, messages);
            case DISP_IAT:
                return applyIat(motif, risques, documents, messages);
            default:
                return VERDICT_CONTESTABLE;
        }
    }

    private static String applyExpulsionPrefectorale(String motif,
                                                     boolean commission,
                                                     boolean urgence,
                                                     List<String> risques,
                                                     List<String> documents,
                                                     List<String> messages) {
        documents.add("Décision préfectorale notifiée (arrêté d'expulsion)");
        documents.add("Avis commission expulsion (CESEDA art. L.632-1) sauf urgence absolue");
        documents.add("Justificatifs des faits motivant la menace à l'ordre public");
        documents.add("Pièces relatives à la situation personnelle (vie privée familiale art. 8 CEDH)");

        boolean motifQualifie = MOTIF_ORDRE_PUBLIC.equals(motif)
                || MOTIF_RECIDIVE_GRAVE.equals(motif)
                || MOTIF_TERRORISME.equals(motif)
                || MOTIF_SECURITE_ETAT.equals(motif);

        if (!motifQualifie) {
            risques.add("Motif AUTRE non qualifié — l'expulsion préfectorale exige une menace"
                    + " grave à l'ordre public (CESEDA L.631-1).");
            messages.add("Sans qualification précise de la menace, la mesure est manifestement infondée.");
            return VERDICT_NUL;
        }

        if (!commission && !urgence) {
            risques.add("Procédure CESEDA L.632-1 (commission expulsion) non respectée"
                    + " — vice de procédure susceptible d'annulation.");
            messages.add("La commission d'expulsion est obligatoire sauf urgence absolue justifiée.");
            return VERDICT_CONTESTABLE;
        }

        if (urgence) {
            messages.add("Urgence absolue invoquée — vérifier que la motivation est suffisante"
                    + " (Conseil d'État exige une démonstration concrète).");
        } else {
            messages.add("Procédure commission expulsion respectée.");
        }
        return VERDICT_VALIDE;
    }

    private static String applyExpulsionMinisterielle(String motif,
                                                      boolean urgence,
                                                      List<String> risques,
                                                      List<String> documents,
                                                      List<String> messages) {
        documents.add("Arrêté ministériel d'expulsion notifié");
        documents.add("Justificatifs de l'urgence absolue invoquée");
        documents.add("Pièces relatives à la situation personnelle (art. 8 CEDH)");
        documents.add("Mémoire en référé suspension (CJA art. L.521-1) si requis");

        boolean motifGrave = MOTIF_TERRORISME.equals(motif)
                || MOTIF_SECURITE_ETAT.equals(motif)
                || MOTIF_ORDRE_PUBLIC.equals(motif);

        if (!motifGrave) {
            risques.add("Motif insuffisant pour expulsion ministérielle (urgence absolue requiert"
                    + " ordre public grave, sécurité État ou terrorisme — CESEDA L.631-2).");
            return VERDICT_CONTESTABLE;
        }

        if (!urgence) {
            risques.add("Urgence absolue non justifiée — la procédure aurait dû passer"
                    + " par la commission d'expulsion (vice de procédure).");
            messages.add("Sans urgence absolue, l'expulsion ministérielle bypasse illégalement la commission.");
            return VERDICT_CONTESTABLE;
        }

        messages.add("Recours direct devant le Conseil d'État (compétence art. R.311-1 CJA).");
        return VERDICT_VALIDE;
    }

    private static String applyExpulsionSecuriteEtat(String motif,
                                                     List<String> risques,
                                                     List<String> documents,
                                                     List<String> messages) {
        documents.add("Arrêté ministériel d'expulsion sécurité État notifié");
        documents.add("Note blanche / éléments de la DGSI motivant la mesure");
        documents.add("Pièces personnelles (situation familiale, lien avec la France)");
        documents.add("Mémoire en référé liberté (CJA art. L.521-2) recommandé");

        boolean motifQualifie = MOTIF_SECURITE_ETAT.equals(motif)
                || MOTIF_TERRORISME.equals(motif);

        if (!motifQualifie) {
            risques.add("Motif insuffisant pour expulsion L.631-3 — la nécessité impérieuse pour"
                    + " la sûreté de l'État exige une menace caractérisée (terrorisme, espionnage, etc.).");
            return VERDICT_CONTESTABLE;
        }

        messages.add("Aucune procédure commission requise (CESEDA L.631-3 — dispositif d'exception).");
        messages.add("Contrôle du juge particulièrement strict — exiger la production des pièces.");
        return VERDICT_VALIDE;
    }

    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "PMD.CyclomaticComplexity"})
    private static String applyIrtf(String motif,
                                    Integer presenceIrreg,
                                    Integer circularite,
                                    boolean comportement,
                                    List<String> risques,
                                    List<String> documents,
                                    List<String> messages) {
        documents.add("Arrêté préfectoral d'IRTF notifié (associé à OQTF)");
        documents.add("Justificatif de la durée de présence en France");
        documents.add("Justificatif du séjour précaire (titres successifs, refus, etc.)");
        documents.add("Pièces relatives à la situation personnelle (vie privée, famille)");
        documents.add("Recours OQTF suspensif (CESEDA art. L.614-4) — délai 15 jours");

        int presence = presenceIrreg != null ? presenceIrreg : 0;
        int precaire = circularite != null ? circularite : 0;

        // Motif AUTRE strict + très peu de présence = mesure manifestement infondée
        if (MOTIF_AUTRE.equals(motif) && presence == 0 && precaire < 3 && !comportement) {
            risques.add("IRTF sans fondement : motif AUTRE non qualifié, présence non documentée,"
                    + " circularité < 3 mois, pas de comportement aggravant.");
            return VERDICT_NUL;
        }

        boolean fondementSolide = presence >= IRTF_DUREE_PRESENCE_BASE_MOIS || comportement;
        if (!fondementSolide) {
            risques.add("Fondement IRTF faible : présence irrégulière courte (< 12 mois) et"
                    + " pas de comportement justifiant — risque sérieux d'annulation pour"
                    + " disproportion (CESEDA L.612-10 critères d'individualisation).");
            return VERDICT_CONTESTABLE;
        }

        messages.add("IRTF fondée sur durée présence irrégulière (" + presence + " mois)"
                + (comportement ? " + comportement aggravant" : "") + ".");
        messages.add("Durée maximale 3 ans (CESEDA L.612-7) — vérifier la proportionnalité.");
        return VERDICT_VALIDE;
    }

    private static String applyIat(String motif,
                                   List<String> risques,
                                   List<String> documents,
                                   List<String> messages) {
        documents.add("Arrêté ministériel d'IAT notifié");
        documents.add("Note blanche / pièces motivant la menace réelle, actuelle, suffisamment grave");
        documents.add("Pièces de l'intéressé (vie privée, attaches familiales)");
        documents.add("Mémoire CE (recours en excès de pouvoir + référé suspension si urgent)");

        boolean motifQualifie = MOTIF_TERRORISME.equals(motif)
                || MOTIF_SECURITE_ETAT.equals(motif)
                || MOTIF_ORDRE_PUBLIC.equals(motif);

        if (!motifQualifie) {
            risques.add("IAT (CESEDA L.222-1) requiert menace réelle, actuelle et suffisamment grave"
                    + " — motif " + motif + " probablement insuffisant.");
            return VERDICT_CONTESTABLE;
        }

        messages.add("Recours devant le Conseil d'État (compétence directe art. R.311-1 CJA).");
        messages.add("Vérifier la motivation au regard de la jurisprudence CE Gisti.");
        return VERDICT_VALIDE;
    }

    private static String buildFormule(DispositifDescriptor descr, String verdict) {
        return descr.libelle() + " — verdict " + verdict + " — recours "
                + descr.juridiction() + " dans " + descr.delaiRecoursJours() + " jours.";
    }

    private record DispositifDescriptor(String code, String libelle, String baseJuridique,
                                        int delaiRecoursJours, String juridiction) {
    }
}
