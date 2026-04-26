package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-12-01 : calculateur de la recevabilité d'une demande d'asile avancé
 * (CESEDA Livre V — France) sur 5 dispositifs distincts.
 *
 * <p>Bases juridiques :
 * <ul>
 *   <li><b>DUBLIN_III</b> — Règlement UE 604/2013 : transfert vers l'État membre responsable
 *       de l'examen de la demande. Délai 6 mois (18 si demandeur en fuite). Recours
 *       suspensif 15 jours devant le TA.</li>
 *   <li><b>PROCEDURE_ACCELEREE</b> — CESEDA L.531-24 : examen accéléré (15 jours OFPRA + 5 semaines
 *       CNDA) pour pays d'origine sûrs (liste OFPRA), réexamen, fraude documentaire avérée,
 *       refus de prise d'empreintes.</li>
 *   <li><b>REEXAMEN</b> — CESEDA L.531-32 : nouvelle demande après rejet définitif, recevable
 *       seulement si éléments nouveaux personnels et postérieurs au rejet (8 jours décision
 *       OFPRA recevabilité).</li>
 *   <li><b>APATRIDIE</b> — CESEDA L.512-1 : reconnaissance du statut d'apatride par OFPRA.
 *       Conditions : aucun État ne reconnaît la nationalité, présence régulière France,
 *       absence de motifs d'exclusion.</li>
 *   <li><b>PROTECTION_SUBSIDIAIRE</b> — CESEDA L.512-1+ : protection si crainte fondée de
 *       traitements graves dans pays d'origine, sans relever de la Convention de Genève.
 *       Durée 4 ans renouvelable.</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent belge (CGRA + Loi 15/12/1980) sera couvert
 * par F-IM-12-BE (backlog).
 */
public final class AsileAvanceCalculator {

    public static final String DISP_DUBLIN_III = "DUBLIN_III";
    public static final String DISP_PROCEDURE_ACCELEREE = "PROCEDURE_ACCELEREE";
    public static final String DISP_REEXAMEN = "REEXAMEN";
    public static final String DISP_APATRIDIE = "APATRIDIE";
    public static final String DISP_PROTECTION_SUBSIDIAIRE = "PROTECTION_SUBSIDIAIRE";

    public static final String VERDICT_RECEVABLE_TRANSFERT = "RECEVABLE_TRANSFERT";
    public static final String VERDICT_FRANCE_COMPETENTE = "FRANCE_COMPETENTE";
    public static final String VERDICT_ACCELEREE_APPLICABLE = "ACCELEREE_APPLICABLE";
    public static final String VERDICT_ACCELEREE_NON_APPLICABLE = "ACCELEREE_NON_APPLICABLE";
    public static final String VERDICT_RECEVABLE_REEXAMEN = "RECEVABLE_REEXAMEN";
    public static final String VERDICT_IRRECEVABLE = "IRRECEVABLE";
    public static final String VERDICT_RECEVABLE_APATRIDIE = "RECEVABLE_APATRIDIE";
    public static final String VERDICT_RECEVABLE_PROTECTION_SUBSIDIAIRE = "RECEVABLE_PROTECTION_SUBSIDIAIRE";

    public static final double DELAI_DUBLIN_NORMAL_MOIS = 6.0;
    public static final double DELAI_DUBLIN_FUITE_MOIS = 18.0;
    public static final double DELAI_PROCEDURE_ACCELEREE_MOIS = 1.5;
    public static final double DELAI_REEXAMEN_MOIS = 0.3;
    public static final double DELAI_APATRIDIE_MOIS = 12.0;
    public static final double DELAI_PROTECTION_SUBSIDIAIRE_MOIS = 18.0;

    public static final String BASE_DUBLIN_III = "Règlement UE 604/2013 (Dublin III)";
    public static final String BASE_PROCEDURE_ACCELEREE = "CESEDA L.531-24";
    public static final String BASE_REEXAMEN = "CESEDA L.531-32";
    public static final String BASE_APATRIDIE = "CESEDA L.512-1 (Convention de New York 1954)";
    public static final String BASE_PROTECTION_SUBSIDIAIRE = "CESEDA L.512-1+";

    public static final String RECOURS_DUBLIN = "Recours suspensif 15 jours devant le TA"
            + " contre la décision de transfert (CESEDA L.572-4).";
    public static final String RECOURS_CNDA = "Recours devant la CNDA 1 mois (procédure normale)"
            + " ou 15 jours (procédure accélérée).";

    private AsileAvanceCalculator() {
    }

    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public static AsileAvanceResult compute(String dispositifAsile,
                                            LocalDate dateDecisionAnterieure,
                                            Boolean elementsNouveaux,
                                            Boolean paysOrigineDansListeSurs,
                                            Boolean empreintesEurodacAutresEm,
                                            Boolean demandeurEnFuite,
                                            Boolean motifsExclusion,
                                            Boolean traitementsGravesEtablis,
                                            Boolean fraudeDocumentaireAvere,
                                            Boolean refusPriseEmpreintes,
                                            Boolean presenceReguliere) {
        validateInputs(dispositifAsile);

        String dispositif = dispositifAsile.trim().toUpperCase();
        switch (dispositif) {
            case DISP_DUBLIN_III:
                return applyDublin(empreintesEurodacAutresEm, demandeurEnFuite);
            case DISP_PROCEDURE_ACCELEREE:
                return applyAcceleree(paysOrigineDansListeSurs, fraudeDocumentaireAvere,
                        refusPriseEmpreintes);
            case DISP_REEXAMEN:
                return applyReexamen(dateDecisionAnterieure, elementsNouveaux);
            case DISP_APATRIDIE:
                return applyApatridie(motifsExclusion, presenceReguliere);
            case DISP_PROTECTION_SUBSIDIAIRE:
                return applyProtectionSubsidiaire(traitementsGravesEtablis, motifsExclusion);
            default:
                throw new IllegalArgumentException("Dispositif d'asile non supporté : " + dispositif);
        }
    }

    private static void validateInputs(String dispositifAsile) {
        if (dispositifAsile == null || dispositifAsile.isBlank()) {
            throw new IllegalArgumentException("dispositifAsile est requis");
        }
    }

    // -------------------------------------------------------------------------
    // DUBLIN III
    // -------------------------------------------------------------------------

    private static AsileAvanceResult applyDublin(Boolean empreintesEurodacAutresEm,
                                                 Boolean demandeurEnFuite) {
        List<String> documents = new ArrayList<>();
        List<String> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        documents.add("Récépissé de demande d'asile en France");
        documents.add("Relevé EURODAC (empreintes)");
        documents.add("Justificatifs de visa, séjour antérieur ou demande d'asile dans un autre EM");
        documents.add("Tout document attestant des liens familiaux dans un autre État membre");

        boolean enFuite = Boolean.TRUE.equals(demandeurEnFuite);
        double delai = enFuite ? DELAI_DUBLIN_FUITE_MOIS : DELAI_DUBLIN_NORMAL_MOIS;
        if (enFuite) {
            messages.add("Demandeur en fuite — délai de transfert porté à 18 mois (art. 29 §2 Règl. 604/2013).");
        }

        String verdict;
        String formule;
        if (Boolean.TRUE.equals(empreintesEurodacAutresEm)) {
            verdict = VERDICT_RECEVABLE_TRANSFERT;
            messages.add("Empreintes EURODAC trouvées dans un autre EM — saisine de l'État"
                    + " responsable, transfert probable.");
            risques.add("Refus de l'État membre saisi — la France redevient compétente après le délai.");
            risques.add("Recours suspensif 15 jours devant le TA contre la décision de transfert.");
            formule = "Asile / Dublin III — État membre responsable saisi, transfert probable"
                    + " (délai " + (int) delai + " mois).";
        } else {
            verdict = VERDICT_FRANCE_COMPETENTE;
            messages.add("Aucune empreinte EURODAC dans un autre EM — la France est compétente"
                    + " pour examiner la demande au fond.");
            formule = "Asile / Dublin III — France compétente, demande examinée au fond par l'OFPRA.";
        }

        return new AsileAvanceResult(
                DISP_DUBLIN_III,
                "Procédure Dublin III (Règl. UE 604/2013)",
                verdict,
                delai,
                RECOURS_DUBLIN,
                documents,
                risques,
                BASE_DUBLIN_III,
                formule,
                messages);
    }

    // -------------------------------------------------------------------------
    // PROCEDURE ACCELEREE
    // -------------------------------------------------------------------------

    private static AsileAvanceResult applyAcceleree(Boolean paysOrigineDansListeSurs,
                                                    Boolean fraudeDocumentaireAvere,
                                                    Boolean refusPriseEmpreintes) {
        List<String> documents = new ArrayList<>();
        List<String> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        documents.add("Formulaire OFPRA + récépissé de demande d'asile");
        documents.add("Pièce d'identité ou passeport");
        documents.add("Récit personnel circonstancié (en français)");
        documents.add("Tout justificatif de la situation dans le pays d'origine");

        boolean paysSur = Boolean.TRUE.equals(paysOrigineDansListeSurs);
        boolean fraude = Boolean.TRUE.equals(fraudeDocumentaireAvere);
        boolean refusEmpreintes = Boolean.TRUE.equals(refusPriseEmpreintes);

        boolean applicable = paysSur || fraude || refusEmpreintes;
        String verdict;
        String formule;
        List<String> motifs = new ArrayList<>();
        if (paysSur) {
            motifs.add("pays d'origine sur la liste OFPRA des pays sûrs");
        }
        if (fraude) {
            motifs.add("fraude documentaire avérée");
        }
        if (refusEmpreintes) {
            motifs.add("refus de prise d'empreintes");
        }

        if (applicable) {
            verdict = VERDICT_ACCELEREE_APPLICABLE;
            messages.add("Procédure accélérée applicable — motif(s) : " + String.join(", ", motifs) + ".");
            messages.add("Délai OFPRA réduit à 15 jours, audience CNDA 5 semaines.");
            risques.add("Le demandeur ne bénéficie pas de l'effet suspensif automatique du recours CNDA.");
            risques.add("Possibilité de référé-liberté contre l'éloignement en parallèle.");
            formule = "Asile / Procédure accélérée applicable (CESEDA L.531-24) — délai OFPRA 15 jours.";
        } else {
            verdict = VERDICT_ACCELEREE_NON_APPLICABLE;
            messages.add("Aucun motif d'application de la procédure accélérée — la demande sera"
                    + " examinée en procédure normale (délai OFPRA 6 mois).");
            formule = "Asile / Procédure accélérée non applicable — procédure normale.";
        }

        return new AsileAvanceResult(
                DISP_PROCEDURE_ACCELEREE,
                "Procédure accélérée (CESEDA L.531-24)",
                verdict,
                DELAI_PROCEDURE_ACCELEREE_MOIS,
                RECOURS_CNDA,
                documents,
                risques,
                BASE_PROCEDURE_ACCELEREE,
                formule,
                messages);
    }

    // -------------------------------------------------------------------------
    // REEXAMEN
    // -------------------------------------------------------------------------

    private static AsileAvanceResult applyReexamen(LocalDate dateDecisionAnterieure,
                                                   Boolean elementsNouveaux) {
        List<String> documents = new ArrayList<>();
        List<String> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        documents.add("Décision de rejet définitif de la précédente demande (OFPRA + CNDA)");
        documents.add("Mémoire détaillé exposant les éléments nouveaux");
        documents.add("Justificatifs des éléments nouveaux (pièces d'État-civil, médicales, politiques)");
        documents.add("Récit complémentaire au récit initial");

        boolean elementsOk = Boolean.TRUE.equals(elementsNouveaux);
        boolean dateOk = dateDecisionAnterieure != null;

        String verdict;
        String formule;
        if (!dateOk) {
            risques.add("Date de la décision antérieure non renseignée — recevabilité non vérifiable.");
        }
        if (!elementsOk) {
            risques.add("Éléments nouveaux non démontrés — la demande sera classée irrecevable par l'OFPRA"
                    + " sous 8 jours (CESEDA L.531-32).");
        } else {
            messages.add("Éléments nouveaux personnels postérieurs au rejet — réexamen recevable a priori.");
        }

        if (elementsOk && dateOk) {
            verdict = VERDICT_RECEVABLE_REEXAMEN;
            messages.add("Réexamen ouvert depuis la décision du " + dateDecisionAnterieure + ".");
            formule = "Asile / Réexamen recevable (CESEDA L.531-32) — délai décision OFPRA 8 jours.";
        } else {
            verdict = VERDICT_IRRECEVABLE;
            formule = "Asile / Réexamen irrecevable — éléments nouveaux ou date manquants.";
        }

        return new AsileAvanceResult(
                DISP_REEXAMEN,
                "Réexamen (CESEDA L.531-32)",
                verdict,
                DELAI_REEXAMEN_MOIS,
                RECOURS_CNDA,
                documents,
                risques,
                BASE_REEXAMEN,
                formule,
                messages);
    }

    // -------------------------------------------------------------------------
    // APATRIDIE
    // -------------------------------------------------------------------------

    private static AsileAvanceResult applyApatridie(Boolean motifsExclusion,
                                                    Boolean presenceReguliere) {
        List<String> documents = new ArrayList<>();
        List<String> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        documents.add("Demande de reconnaissance du statut d'apatride à l'OFPRA");
        documents.add("Tout justificatif établissant qu'aucun État ne reconnaît la nationalité"
                + " (refus consulats, décisions étrangères)");
        documents.add("Justificatifs de présence régulière en France");
        documents.add("Bulletin n°3 du casier judiciaire (vérification motifs d'exclusion)");

        boolean exclusion = Boolean.TRUE.equals(motifsExclusion);
        boolean reguliere = !Boolean.FALSE.equals(presenceReguliere); // défaut true si non précisé

        String verdict;
        String formule;
        if (exclusion) {
            verdict = VERDICT_IRRECEVABLE;
            risques.add("Motifs d'exclusion (crime de guerre, sécurité publique) — statut refusé.");
            formule = "Asile / Apatridie irrecevable — motifs d'exclusion.";
        } else if (!reguliere) {
            verdict = VERDICT_IRRECEVABLE;
            risques.add("Présence non régulière en France — condition d'admission au statut non remplie.");
            formule = "Asile / Apatridie irrecevable — présence irrégulière.";
        } else {
            verdict = VERDICT_RECEVABLE_APATRIDIE;
            messages.add("Aucun motif d'exclusion identifié, présence régulière — instruction OFPRA"
                    + " sur la non-reconnaissance par tout État.");
            formule = "Asile / Apatridie recevable (CESEDA L.512-1) — instruction OFPRA 12 mois.";
        }

        return new AsileAvanceResult(
                DISP_APATRIDIE,
                "Statut d'apatride (CESEDA L.512-1, Convention NY 1954)",
                verdict,
                DELAI_APATRIDIE_MOIS,
                RECOURS_CNDA,
                documents,
                risques,
                BASE_APATRIDIE,
                formule,
                messages);
    }

    // -------------------------------------------------------------------------
    // PROTECTION SUBSIDIAIRE
    // -------------------------------------------------------------------------

    private static AsileAvanceResult applyProtectionSubsidiaire(Boolean traitementsGravesEtablis,
                                                                Boolean motifsExclusion) {
        List<String> documents = new ArrayList<>();
        List<String> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        documents.add("Récit personnel circonstancié des traitements graves redoutés");
        documents.add("Justificatifs des conditions dans le pays d'origine (rapports HCR, ONG, presse)");
        documents.add("Certificats médicaux ou psychologiques le cas échéant");
        documents.add("Tout justificatif d'identité et de parcours migratoire");

        boolean exclusion = Boolean.TRUE.equals(motifsExclusion);
        boolean traitementsOk = Boolean.TRUE.equals(traitementsGravesEtablis);

        String verdict;
        String formule;
        if (exclusion) {
            verdict = VERDICT_IRRECEVABLE;
            risques.add("Motifs d'exclusion — protection subsidiaire refusée (CESEDA L.512-2).");
            formule = "Asile / Protection subsidiaire irrecevable — motifs d'exclusion.";
        } else if (!traitementsOk) {
            verdict = VERDICT_IRRECEVABLE;
            risques.add("Crainte fondée de traitements graves non établie — protection refusée.");
            formule = "Asile / Protection subsidiaire irrecevable — traitements graves non établis.";
        } else {
            verdict = VERDICT_RECEVABLE_PROTECTION_SUBSIDIAIRE;
            messages.add("Crainte fondée de traitements graves établie, pas de motif d'exclusion —"
                    + " protection subsidiaire ouverte (durée 4 ans renouvelable).");
            formule = "Asile / Protection subsidiaire recevable (CESEDA L.512-1+) — durée 4 ans renouvelable.";
        }

        return new AsileAvanceResult(
                DISP_PROTECTION_SUBSIDIAIRE,
                "Protection subsidiaire (CESEDA L.512-1+)",
                verdict,
                DELAI_PROTECTION_SUBSIDIAIRE_MOIS,
                RECOURS_CNDA,
                documents,
                risques,
                BASE_PROTECTION_SUBSIDIAIRE,
                formule,
                messages);
    }
}
