package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-13-01 : calculateur de la recevabilité d'une demande de naturalisation française
 * au regard du Code civil (art. 21 et s.).
 *
 * <p>Bases juridiques :
 * <ul>
 *   <li>Art. 21-15 et s. Cciv — naturalisation par décret (voie classique)</li>
 *   <li>Art. 21-2 Cciv — déclaration de nationalité par mariage</li>
 *   <li>Art. 21-13-1 Cciv — déclaration ascendant direct d'un français</li>
 *   <li>Art. 22-1 Cciv — naturalisation du mineur dont un parent acquiert la nationalité</li>
 *   <li>Art. 24-1 et s. Cciv — réintégration dans la nationalité française</li>
 *   <li>Art. 21-4 / 27-2 Cciv — opposition gouvernementale</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent belge (CNB art. 12bis et s.) sera
 * couvert par F-IM-13-BE (backlog).
 *
 * <p>Verdict :
 * <ul>
 *   <li><b>ELEVEE</b> : tous les critères de la voie + critères communs OK.</li>
 *   <li><b>MOYENNE</b> : voie ouverte mais conditions limites (langue à valider, ressources
 *       juste suffisantes, durée tout juste atteinte).</li>
 *   <li><b>FAIBLE</b> : critère bloquant (durée résidence insuffisante, casier non vierge,
 *       opposition gouvernementale active, etc.).</li>
 * </ul>
 */
public final class NaturalisationCalculator {

    public static final String VERDICT_ELEVEE = "ELEVEE";
    public static final String VERDICT_MOYENNE = "MOYENNE";
    public static final String VERDICT_FAIBLE = "FAIBLE";

    public static final String VOIE_DECRET = "DECRET";
    public static final String VOIE_MARIAGE = "MARIAGE";
    public static final String VOIE_ASCENDANT = "ASCENDANT";
    public static final String VOIE_MINEUR = "MINEUR";
    public static final String VOIE_REINTEGRATION = "REINTEGRATION";
    public static final String VOIE_OPPOSITION = "OPPOSITION";

    /** Décret art. 21-15 : 5 ans de résidence régulière par défaut. */
    public static final int DECRET_DUREE_RESIDENCE_DEFAULT_ANNEES = 5;
    /** Décret art. 21-18 : 2 ans si études supérieures FR. */
    public static final int DECRET_DUREE_RESIDENCE_REDUITE_ANNEES = 2;
    /** Mariage art. 21-2 : 4 ans si cohabitation continue. */
    public static final int MARIAGE_DUREE_DEFAULT_ANNEES = 4;
    /** Mariage art. 21-2 : 5 ans si pas de cohabitation continue. */
    public static final int MARIAGE_DUREE_LONGUE_ANNEES = 5;
    /** Ascendant art. 21-13-1 : âge minimum 65 ans. */
    public static final int ASCENDANT_AGE_MIN = 65;
    /** Ascendant art. 21-13-1 : durée résidence régulière 25 ans. */
    public static final int ASCENDANT_DUREE_RESIDENCE_ANNEES = 25;

    public static final int DELAI_DECRET_MOIS = 18;
    public static final int DELAI_MARIAGE_MOIS = 12;
    public static final int DELAI_ASCENDANT_MOIS = 12;
    public static final int DELAI_MINEUR_MOIS = 6;
    public static final int DELAI_REINTEGRATION_MOIS = 12;
    public static final int DELAI_OPPOSITION_MOIS = 12;

    private NaturalisationCalculator() {
    }

    public static NaturalisationResult compute(String voieNaturalisation,
                                               Integer dureeResidenceReguliereAnnees,
                                               Integer dureeMariageAnnees,
                                               Boolean cohabitationContinue,
                                               Integer ageDemandeur,
                                               Boolean ascendantDirectFrancais,
                                               Boolean parentAcquiertNationalite,
                                               Boolean vitAvecParentAcquereur,
                                               Boolean ancienFrancais,
                                               boolean casierJudiciaireVierge,
                                               Boolean assimilationLangueB1,
                                               Boolean ressourcesStables,
                                               boolean oppositionGouvernementaleActive,
                                               Boolean etudesSuperieuresFrance) {
        validateInputs(voieNaturalisation, dureeResidenceReguliereAnnees, dureeMariageAnnees,
                ageDemandeur);

        String voie = voieNaturalisation.trim().toUpperCase();
        VoieDescriptor d = resolveVoie(voie);

        List<String> criteresNonRemplis = new ArrayList<>();
        List<String> documentsAFournir = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // Pré-blocages transversaux
        boolean blockCasier = !casierJudiciaireVierge;
        if (blockCasier) {
            criteresNonRemplis.add("Casier judiciaire non vierge — moralité non établie (Cciv art. 21-23).");
        }
        boolean blockOpposition = oppositionGouvernementaleActive;
        if (blockOpposition) {
            criteresNonRemplis.add("Opposition gouvernementale active — la décision de naturalisation"
                    + " ne peut être enregistrée tant que l'opposition n'est pas levée (Cciv art. 21-4 / 27-2).");
        }

        String verdictBase = applyVoie(d, dureeResidenceReguliereAnnees, dureeMariageAnnees,
                cohabitationContinue, ageDemandeur, ascendantDirectFrancais,
                parentAcquiertNationalite, vitAvecParentAcquereur, ancienFrancais,
                assimilationLangueB1, ressourcesStables, etudesSuperieuresFrance,
                criteresNonRemplis, documentsAFournir, messages);

        String verdictFinal = verdictBase;
        if (blockCasier || blockOpposition) {
            verdictFinal = VERDICT_FAIBLE;
        }

        String formule = buildFormule(d, verdictFinal);
        if (VOIE_DECRET.equals(voie)) {
            messages.add("La naturalisation par décret est une décision discrétionnaire du gouvernement"
                    + " — l'outil évalue la recevabilité technique, pas la décision finale.");
        }
        messages.add("Délai d'instruction estimatif : " + d.delaiMois() + " mois.");

        return new NaturalisationResult(
                voie,
                d.libelle(),
                verdictFinal,
                criteresNonRemplis,
                documentsAFournir,
                d.delaiMois(),
                d.baseJuridique(),
                formule,
                messages);
    }

    private static void validateInputs(String voieNaturalisation,
                                       Integer dureeResidenceReguliereAnnees,
                                       Integer dureeMariageAnnees,
                                       Integer ageDemandeur) {
        if (voieNaturalisation == null || voieNaturalisation.isBlank()) {
            throw new IllegalArgumentException("voieNaturalisation est requise");
        }
        if (dureeResidenceReguliereAnnees != null && dureeResidenceReguliereAnnees < 0) {
            throw new IllegalArgumentException("dureeResidenceReguliereAnnees doit être ≥ 0");
        }
        if (dureeMariageAnnees != null && dureeMariageAnnees < 0) {
            throw new IllegalArgumentException("dureeMariageAnnees doit être ≥ 0");
        }
        if (ageDemandeur != null && ageDemandeur < 0) {
            throw new IllegalArgumentException("ageDemandeur doit être ≥ 0");
        }
    }

    private static VoieDescriptor resolveVoie(String voie) {
        switch (voie) {
            case VOIE_DECRET:
                return new VoieDescriptor(VOIE_DECRET, "Naturalisation par décret (art. 21-15+)",
                        "Code civil art. 21-15 à 21-25-1", DELAI_DECRET_MOIS);
            case VOIE_MARIAGE:
                return new VoieDescriptor(VOIE_MARIAGE,
                        "Déclaration de nationalité par mariage (art. 21-2)",
                        "Code civil art. 21-2", DELAI_MARIAGE_MOIS);
            case VOIE_ASCENDANT:
                return new VoieDescriptor(VOIE_ASCENDANT,
                        "Déclaration ascendant direct d'un français (art. 21-13-1)",
                        "Code civil art. 21-13-1", DELAI_ASCENDANT_MOIS);
            case VOIE_MINEUR:
                return new VoieDescriptor(VOIE_MINEUR,
                        "Effet collectif sur mineur (art. 22-1)",
                        "Code civil art. 22-1", DELAI_MINEUR_MOIS);
            case VOIE_REINTEGRATION:
                return new VoieDescriptor(VOIE_REINTEGRATION,
                        "Réintégration dans la nationalité française (art. 24-1+)",
                        "Code civil art. 24-1 à 24-3", DELAI_REINTEGRATION_MOIS);
            case VOIE_OPPOSITION:
                return new VoieDescriptor(VOIE_OPPOSITION,
                        "Opposition gouvernementale (art. 21-4 / 27-2)",
                        "Code civil art. 21-4 et 27-2", DELAI_OPPOSITION_MOIS);
            default:
                throw new IllegalArgumentException("Voie non supportée : " + voie);
        }
    }

    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "PMD.CyclomaticComplexity"})
    private static String applyVoie(VoieDescriptor d,
                                    Integer dureeResidence,
                                    Integer dureeMariage,
                                    Boolean cohabitationContinue,
                                    Integer ageDemandeur,
                                    Boolean ascendantDirectFrancais,
                                    Boolean parentAcquiert,
                                    Boolean vitAvecParent,
                                    Boolean ancienFrancais,
                                    Boolean langueB1,
                                    Boolean ressourcesStables,
                                    Boolean etudesSupFr,
                                    List<String> criteresNonRemplis,
                                    List<String> documentsAFournir,
                                    List<String> messages) {
        switch (d.code()) {
            case VOIE_DECRET:
                return applyDecret(dureeResidence, langueB1, ressourcesStables, etudesSupFr,
                        criteresNonRemplis, documentsAFournir, messages);
            case VOIE_MARIAGE:
                return applyMariage(dureeMariage, cohabitationContinue, langueB1,
                        criteresNonRemplis, documentsAFournir, messages);
            case VOIE_ASCENDANT:
                return applyAscendant(dureeResidence, ageDemandeur, ascendantDirectFrancais,
                        criteresNonRemplis, documentsAFournir, messages);
            case VOIE_MINEUR:
                return applyMineur(parentAcquiert, vitAvecParent,
                        criteresNonRemplis, documentsAFournir, messages);
            case VOIE_REINTEGRATION:
                return applyReintegration(ancienFrancais,
                        criteresNonRemplis, documentsAFournir, messages);
            case VOIE_OPPOSITION:
                return applyOpposition(criteresNonRemplis, documentsAFournir, messages);
            default:
                return VERDICT_FAIBLE;
        }
    }

    private static String applyDecret(Integer dureeResidence,
                                      Boolean langueB1,
                                      Boolean ressourcesStables,
                                      Boolean etudesSupFr,
                                      List<String> criteres,
                                      List<String> docs,
                                      List<String> messages) {
        docs.add("Justificatifs de résidence régulière en France (titres de séjour successifs)");
        docs.add("Acte de naissance traduit/légalisé");
        docs.add("Certificat médical si demandé");
        docs.add("Justificatifs de ressources (3 derniers avis d'imposition, bulletins de paie)");
        docs.add("Diplôme attestant assimilation linguistique (DELF B1 ou équivalent)");
        docs.add("Bulletin n°3 du casier judiciaire (et étranger pour les pays de résidence)");

        boolean etudesFr = Boolean.TRUE.equals(etudesSupFr);
        int seuilDuree = etudesFr
                ? DECRET_DUREE_RESIDENCE_REDUITE_ANNEES
                : DECRET_DUREE_RESIDENCE_DEFAULT_ANNEES;
        if (etudesFr) {
            messages.add("Études supérieures en France : durée de résidence requise réduite à "
                    + DECRET_DUREE_RESIDENCE_REDUITE_ANNEES + " ans (Cciv art. 21-18).");
        }
        if (dureeResidence == null || dureeResidence < seuilDuree) {
            criteres.add("Durée de résidence régulière insuffisante (" + (dureeResidence == null
                    ? "non renseignée" : dureeResidence + " ans") + " < " + seuilDuree + " ans).");
            return VERDICT_FAIBLE;
        }

        boolean langueOk = Boolean.TRUE.equals(langueB1);
        boolean ressourcesOk = Boolean.TRUE.equals(ressourcesStables);

        if (!langueOk && !ressourcesOk) {
            criteres.add("Assimilation linguistique B1 non établie (Cciv art. 21-24).");
            criteres.add("Ressources stables et suffisantes non démontrées.");
            return VERDICT_FAIBLE;
        }
        if (!langueOk) {
            criteres.add("Assimilation linguistique B1 non établie (Cciv art. 21-24).");
            return VERDICT_MOYENNE;
        }
        if (!ressourcesOk) {
            criteres.add("Ressources stables et suffisantes non démontrées.");
            return VERDICT_MOYENNE;
        }
        return VERDICT_ELEVEE;
    }

    private static String applyMariage(Integer dureeMariage,
                                       Boolean cohabitationContinue,
                                       Boolean langueB1,
                                       List<String> criteres,
                                       List<String> docs,
                                       List<String> messages) {
        docs.add("Acte de mariage transcrit (registre français)");
        docs.add("Justificatif de la nationalité française du conjoint (CNF, carte d'identité)");
        docs.add("Justificatifs de communauté de vie matérielle et affective (factures communes,"
                + " imposition commune, attestation domicile commun)");
        docs.add("Diplôme attestant assimilation linguistique (B1)");
        docs.add("Bulletin n°3 du casier judiciaire");

        boolean cohabOk = Boolean.TRUE.equals(cohabitationContinue);
        int seuil = cohabOk ? MARIAGE_DUREE_DEFAULT_ANNEES : MARIAGE_DUREE_LONGUE_ANNEES;
        messages.add("Durée de mariage requise : " + seuil + " ans"
                + (cohabOk ? " (cohabitation continue)" : " (sans cohabitation continue ou résidence FR < 3 ans)")
                + ".");

        if (dureeMariage == null || dureeMariage < seuil) {
            criteres.add("Durée de mariage insuffisante (" + (dureeMariage == null
                    ? "non renseignée" : dureeMariage + " ans") + " < " + seuil + " ans).");
            return VERDICT_FAIBLE;
        }

        if (!Boolean.TRUE.equals(langueB1)) {
            criteres.add("Assimilation linguistique B1 non établie (Cciv art. 21-2 al. 3).");
            return VERDICT_MOYENNE;
        }
        return VERDICT_ELEVEE;
    }

    private static String applyAscendant(Integer dureeResidence,
                                         Integer ageDemandeur,
                                         Boolean ascendantDirect,
                                         List<String> criteres,
                                         List<String> docs,
                                         List<String> messages) {
        docs.add("Justificatifs de résidence régulière en France ≥ 25 ans");
        docs.add("Justificatif du lien de filiation directe avec un français (acte naissance enfant)");
        docs.add("Justificatif de la nationalité française de l'enfant adulte");
        docs.add("Pièce d'identité justifiant l'âge ≥ 65 ans");

        if (ageDemandeur == null || ageDemandeur < ASCENDANT_AGE_MIN) {
            criteres.add("Âge insuffisant (" + (ageDemandeur == null
                    ? "non renseigné" : ageDemandeur + " ans") + " < " + ASCENDANT_AGE_MIN + " ans).");
            return VERDICT_FAIBLE;
        }
        if (dureeResidence == null || dureeResidence < ASCENDANT_DUREE_RESIDENCE_ANNEES) {
            criteres.add("Durée de résidence régulière insuffisante (" + (dureeResidence == null
                    ? "non renseignée" : dureeResidence + " ans")
                    + " < " + ASCENDANT_DUREE_RESIDENCE_ANNEES + " ans).");
            return VERDICT_FAIBLE;
        }
        if (!Boolean.TRUE.equals(ascendantDirect)) {
            criteres.add("Lien d'ascendant direct d'un français non établi (Cciv art. 21-13-1).");
            return VERDICT_FAIBLE;
        }
        messages.add("Voie réservée aux ascendants directs (≥ 65 ans, ≥ 25 ans de résidence régulière).");
        return VERDICT_ELEVEE;
    }

    private static String applyMineur(Boolean parentAcquiert,
                                      Boolean vitAvecParent,
                                      List<String> criteres,
                                      List<String> docs,
                                      List<String> messages) {
        docs.add("Acte de naissance du mineur");
        docs.add("Décret de naturalisation ou déclaration du parent acquéreur");
        docs.add("Justificatif de résidence habituelle commune avec le parent acquéreur");
        docs.add("Pièce d'identité du parent acquéreur");

        if (!Boolean.TRUE.equals(parentAcquiert)) {
            criteres.add("Aucun parent n'acquiert la nationalité française — voie 22-1 inapplicable.");
            return VERDICT_FAIBLE;
        }
        if (!Boolean.TRUE.equals(vitAvecParent)) {
            criteres.add("Le mineur ne vit pas habituellement avec le parent acquéreur"
                    + " (Cciv art. 22-1 — résidence habituelle requise).");
            return VERDICT_FAIBLE;
        }
        messages.add("Effet collectif : le mineur acquiert la nationalité avec le parent. Procédure"
                + " accessoire à la demande du parent (pas de demande autonome).");
        return VERDICT_ELEVEE;
    }

    private static String applyReintegration(Boolean ancienFrancais,
                                             List<String> criteres,
                                             List<String> docs,
                                             List<String> messages) {
        docs.add("Acte de naissance et justificatif de l'ancienne nationalité française");
        docs.add("Justificatif de la perte de la nationalité française (acte, jugement)");
        docs.add("Bulletin n°3 du casier judiciaire");
        docs.add("Justificatif de résidence régulière en France");

        if (!Boolean.TRUE.equals(ancienFrancais)) {
            criteres.add("Le demandeur n'a jamais été français — réintégration impossible (Cciv art. 24-1).");
            return VERDICT_FAIBLE;
        }
        messages.add("Réintégration : voie ouverte aux personnes ayant perdu la nationalité française"
                + " (par mariage avant 1973, par déclaration ou décret).");
        return VERDICT_ELEVEE;
    }

    private static String applyOpposition(List<String> criteres,
                                          List<String> docs,
                                          List<String> messages) {
        docs.add("Notification de l'intention d'opposition (gouvernement)");
        docs.add("Mémoire en réponse à l'opposition (recours en Conseil d'État possible)");
        messages.add("L'opposition gouvernementale est une voie de rejet, pas d'acquisition.");
        messages.add("Le gouvernement dispose d'un délai d'1 an pour s'opposer au mariage (Cciv art. 21-4)"
                + " ou au décret (art. 27-2). Recours possible devant le Conseil d'État.");
        return VERDICT_MOYENNE;
    }

    private static String buildFormule(VoieDescriptor d, String verdict) {
        return "Naturalisation — " + d.libelle() + " : verdict " + verdict + ".";
    }

    private record VoieDescriptor(String code, String libelle, String baseJuridique, int delaiMois) {
    }
}
