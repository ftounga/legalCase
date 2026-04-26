package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-17-01 : calculateur de la recevabilité d'une demande de séjour ou de regroupement
 * familial pour un ressortissant algérien, au regard de l'accord franco-algérien
 * du 27/12/1968 (modifié par avenants 1985 / 1994 / 2001).
 *
 * <p>5 voies distinctes :
 * <ul>
 *   <li><b>CRA_1_AN</b> (art. 5) : 1ère demande, équivalent VPF étudiant / visiteur</li>
 *   <li><b>CRA_10_ANS_LIEN_FRANCE</b> (art. 6) : conjoint français, parent enfant français,
 *       ou 10 ans de présence régulière</li>
 *   <li><b>CRA_10_ANS_RESIDENT_ANCIEN</b> (art. 7bis) : né en France ou arrivé avant 13 ans</li>
 *   <li><b>CHANGEMENT_VERS_TRAVAILLEUR</b> (art. 7) : passage CRA 1 an → CRA Travailleur</li>
 *   <li><b>REGROUPEMENT_FAMILIAL_ACCORD_1968</b> (art. 4) : conjoint et enfants mineurs,
 *       conditions ressources réduites vs CESEDA</li>
 * </ul>
 *
 * <p>Verdict :
 * <ul>
 *   <li><b>ELEVEE</b> : tous critères remplis</li>
 *   <li><b>MOYENNE</b> : voie ouverte mais critère non bloquant à compléter</li>
 *   <li><b>FAIBLE</b> : critère bloquant (visa absent, lien non démontré, durée insuffisante…)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FRANCE</b>. Pas d'équivalent BE (accord bilatéral FR-DZ).
 */
public final class RegimeAlgerienCalculator {

    public static final String VERDICT_ELEVEE = "ELEVEE";
    public static final String VERDICT_MOYENNE = "MOYENNE";
    public static final String VERDICT_FAIBLE = "FAIBLE";

    public static final String VOIE_CRA_1_AN = "CRA_1_AN";
    public static final String VOIE_CRA_10_ANS_LIEN_FRANCE = "CRA_10_ANS_LIEN_FRANCE";
    public static final String VOIE_CRA_10_ANS_RESIDENT_ANCIEN = "CRA_10_ANS_RESIDENT_ANCIEN";
    public static final String VOIE_CHANGEMENT_VERS_TRAVAILLEUR = "CHANGEMENT_VERS_TRAVAILLEUR";
    public static final String VOIE_REGROUPEMENT = "REGROUPEMENT_FAMILIAL_ACCORD_1968";

    /** Art. 6 al. 3 : 10 ans de présence régulière (120 mois). */
    public static final int CRA10_PRESENCE_MOIS_MIN = 120;
    /** Délai standard d'instruction préfecture (mois). */
    public static final int DELAI_INSTRUCTION_MOIS_DEFAULT = 3;
    /** Délai regroupement familial (instruction OFII + préfecture). */
    public static final int DELAI_REGROUPEMENT_MOIS = 6;

    private RegimeAlgerienCalculator() {
    }

    public static RegimeAlgerienResult compute(String voieDemande,
                                               Boolean documentEtatCivilOriginal,
                                               Integer presenceReguliereFranceMois,
                                               boolean casierJudiciaireVierge,
                                               Boolean visaLongSejourValide,
                                               Boolean conjointFrancais,
                                               Boolean parentEnfantFrancais,
                                               Boolean neEnFrance,
                                               Boolean arriveeAvant13Ans,
                                               Boolean contratTravailValide,
                                               Boolean ressourcesSuffisantes,
                                               Boolean logementDecent,
                                               Integer nombrePersonnesFoyer) {
        validateInputs(voieDemande, presenceReguliereFranceMois, nombrePersonnesFoyer);

        String voie = voieDemande.trim().toUpperCase();
        VoieDescriptor d = resolveVoie(voie);

        List<String> criteresNonRemplis = new ArrayList<>();
        List<String> documentsRequis = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // Critères communs transversaux
        boolean blockCasier = !casierJudiciaireVierge;
        if (blockCasier) {
            criteresNonRemplis.add("Casier judiciaire non vierge — moralité non établie"
                    + " (motif d'ordre public, accord 1968 art. 5).");
        }
        boolean blockEtatCivil = Boolean.FALSE.equals(documentEtatCivilOriginal);
        if (blockEtatCivil) {
            criteresNonRemplis.add("Acte d'état civil algérien original manquant"
                    + " — la préfecture exige des registres consulaires algériens authentifiés.");
        }

        String verdictBase = applyVoie(d, presenceReguliereFranceMois,
                visaLongSejourValide, conjointFrancais, parentEnfantFrancais,
                neEnFrance, arriveeAvant13Ans, contratTravailValide,
                ressourcesSuffisantes, logementDecent, nombrePersonnesFoyer,
                criteresNonRemplis, documentsRequis, messages);

        String verdictFinal = verdictBase;
        if (blockCasier || blockEtatCivil) {
            verdictFinal = VERDICT_FAIBLE;
        }

        String formule = buildFormule(d, verdictFinal);
        messages.add("Délai d'instruction estimatif : " + d.delaiMois() + " mois.");
        messages.add("Régime parallèle au CESEDA — applicable uniquement aux ressortissants algériens"
                + " (accord franco-algérien 27/12/1968 modifié).");

        return new RegimeAlgerienResult(
                voie,
                d.libelle(),
                verdictFinal,
                d.titreApplicable(),
                d.dureeTitreAnnees(),
                criteresNonRemplis,
                documentsRequis,
                d.delaiMois(),
                d.baseJuridique(),
                formule,
                messages);
    }

    private static void validateInputs(String voieDemande,
                                       Integer presenceReguliereFranceMois,
                                       Integer nombrePersonnesFoyer) {
        if (voieDemande == null || voieDemande.isBlank()) {
            throw new IllegalArgumentException("voieDemande est requise");
        }
        if (presenceReguliereFranceMois != null && presenceReguliereFranceMois < 0) {
            throw new IllegalArgumentException("presenceReguliereFranceMois doit être ≥ 0");
        }
        if (nombrePersonnesFoyer != null && nombrePersonnesFoyer < 0) {
            throw new IllegalArgumentException("nombrePersonnesFoyer doit être ≥ 0");
        }
    }

    private static VoieDescriptor resolveVoie(String voie) {
        switch (voie) {
            case VOIE_CRA_1_AN:
                return new VoieDescriptor(VOIE_CRA_1_AN,
                        "Certificat de résidence algérien 1 an (art. 5)",
                        VOIE_CRA_1_AN, 1,
                        "Accord franco-algérien 27/12/1968 modifié — art. 5",
                        DELAI_INSTRUCTION_MOIS_DEFAULT);
            case VOIE_CRA_10_ANS_LIEN_FRANCE:
                return new VoieDescriptor(VOIE_CRA_10_ANS_LIEN_FRANCE,
                        "Certificat de résidence algérien 10 ans — lien avec la France (art. 6)",
                        VOIE_CRA_10_ANS_LIEN_FRANCE, 10,
                        "Accord franco-algérien 27/12/1968 modifié — art. 6",
                        DELAI_INSTRUCTION_MOIS_DEFAULT);
            case VOIE_CRA_10_ANS_RESIDENT_ANCIEN:
                return new VoieDescriptor(VOIE_CRA_10_ANS_RESIDENT_ANCIEN,
                        "Certificat de résidence algérien 10 ans — résident ancien (art. 7bis)",
                        VOIE_CRA_10_ANS_RESIDENT_ANCIEN, 10,
                        "Accord franco-algérien 27/12/1968 modifié — art. 7bis",
                        DELAI_INSTRUCTION_MOIS_DEFAULT);
            case VOIE_CHANGEMENT_VERS_TRAVAILLEUR:
                return new VoieDescriptor(VOIE_CHANGEMENT_VERS_TRAVAILLEUR,
                        "Changement de statut vers travailleur (art. 7)",
                        VOIE_CHANGEMENT_VERS_TRAVAILLEUR, 1,
                        "Accord franco-algérien 27/12/1968 modifié — art. 7",
                        DELAI_INSTRUCTION_MOIS_DEFAULT);
            case VOIE_REGROUPEMENT:
                return new VoieDescriptor(VOIE_REGROUPEMENT,
                        "Regroupement familial — accord 1968 (art. 4)",
                        VOIE_REGROUPEMENT, 0,
                        "Accord franco-algérien 27/12/1968 modifié — art. 4",
                        DELAI_REGROUPEMENT_MOIS);
            default:
                throw new IllegalArgumentException("Voie non supportée : " + voie);
        }
    }

    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "PMD.CyclomaticComplexity"})
    private static String applyVoie(VoieDescriptor d,
                                    Integer presenceMois,
                                    Boolean visaLongSejour,
                                    Boolean conjointFr,
                                    Boolean parentEnfantFr,
                                    Boolean neEnFrance,
                                    Boolean arriveeAvant13,
                                    Boolean contratTravail,
                                    Boolean ressourcesSuffisantes,
                                    Boolean logementDecent,
                                    Integer nbPersonnesFoyer,
                                    List<String> criteres,
                                    List<String> documents,
                                    List<String> messages) {
        switch (d.code()) {
            case VOIE_CRA_1_AN:
                return applyCra1An(visaLongSejour, criteres, documents, messages);
            case VOIE_CRA_10_ANS_LIEN_FRANCE:
                return applyCra10AnsLienFrance(conjointFr, parentEnfantFr, presenceMois,
                        criteres, documents, messages);
            case VOIE_CRA_10_ANS_RESIDENT_ANCIEN:
                return applyCra10AnsResidentAncien(neEnFrance, arriveeAvant13,
                        criteres, documents, messages);
            case VOIE_CHANGEMENT_VERS_TRAVAILLEUR:
                return applyChangementVersTravailleur(contratTravail, criteres, documents, messages);
            case VOIE_REGROUPEMENT:
                return applyRegroupement(ressourcesSuffisantes, logementDecent, nbPersonnesFoyer,
                        criteres, documents, messages);
            default:
                return VERDICT_FAIBLE;
        }
    }

    private static String applyCra1An(Boolean visaLongSejour,
                                      List<String> criteres,
                                      List<String> documents,
                                      List<String> messages) {
        documents.add("Passeport algérien en cours de validité");
        documents.add("Visa de long séjour valide (VLS-TS)");
        documents.add("Acte de naissance algérien original");
        documents.add("Justificatif de domicile en France (< 3 mois)");
        documents.add("Justificatif d'objet de séjour (inscription, contrat, attestation famille…)");
        documents.add("Bulletin n°3 du casier judiciaire");

        if (!Boolean.TRUE.equals(visaLongSejour)) {
            criteres.add("Visa de long séjour valide manquant"
                    + " — la 1ère demande de CRA suppose une entrée régulière en France"
                    + " (accord 1968 art. 5).");
            return VERDICT_FAIBLE;
        }
        messages.add("CRA 1 an : titre initial, renouvelable. Possible passage CRA 10 ans après"
                + " 3 ans de séjour régulier.");
        return VERDICT_ELEVEE;
    }

    private static String applyCra10AnsLienFrance(Boolean conjointFr,
                                                  Boolean parentEnfantFr,
                                                  Integer presenceMois,
                                                  List<String> criteres,
                                                  List<String> documents,
                                                  List<String> messages) {
        documents.add("Passeport algérien en cours de validité");
        documents.add("Acte de mariage transcrit OU acte de naissance de l'enfant français OU"
                + " 10 ans de titres de séjour successifs");
        documents.add("Justificatif de communauté de vie (si conjoint français)");
        documents.add("Bulletin n°3 du casier judiciaire");

        boolean conjoint = Boolean.TRUE.equals(conjointFr);
        boolean parent = Boolean.TRUE.equals(parentEnfantFr);
        boolean dixAns = presenceMois != null && presenceMois >= CRA10_PRESENCE_MOIS_MIN;

        if (!conjoint && !parent && !dixAns) {
            criteres.add("Aucun lien avec la France établi : ni conjoint français, ni parent"
                    + " d'enfant français, ni 10 ans de présence régulière (≥ "
                    + CRA10_PRESENCE_MOIS_MIN + " mois).");
            return VERDICT_FAIBLE;
        }

        if (conjoint) {
            messages.add("Voie conjoint français (art. 6 al. 1) : communauté de vie effective requise.");
        } else if (parent) {
            messages.add("Voie parent d'enfant français (art. 6 al. 2) : contribution à"
                    + " l'entretien et à l'éducation requise.");
        } else {
            messages.add("Voie 10 ans de présence régulière (art. 6 al. 3) : présence ≥ 10 ans"
                    + " avec titres réguliers consécutifs ou non.");
        }
        return VERDICT_ELEVEE;
    }

    private static String applyCra10AnsResidentAncien(Boolean neEnFrance,
                                                      Boolean arriveeAvant13,
                                                      List<String> criteres,
                                                      List<String> documents,
                                                      List<String> messages) {
        documents.add("Acte de naissance (algérien ou français)");
        documents.add("Justificatifs de présence en France depuis l'enfance"
                + " (scolarité, certificats médicaux, attestations)");
        documents.add("Bulletin n°3 du casier judiciaire");

        boolean neFr = Boolean.TRUE.equals(neEnFrance);
        boolean avant13 = Boolean.TRUE.equals(arriveeAvant13);

        if (!neFr && !avant13) {
            criteres.add("L'art. 7bis exige une naissance en France OU une arrivée avant"
                    + " l'âge de 13 ans — aucune des deux conditions n'est remplie.");
            return VERDICT_FAIBLE;
        }
        messages.add("Voie art. 7bis : titre 10 ans de plein droit pour les ressortissants"
                + " algériens nés en France ou arrivés avant 13 ans.");
        return VERDICT_ELEVEE;
    }

    private static String applyChangementVersTravailleur(Boolean contratTravailValide,
                                                         List<String> criteres,
                                                         List<String> documents,
                                                         List<String> messages) {
        documents.add("CRA 1 an en cours de validité");
        documents.add("Contrat de travail (CDI ou CDD ≥ 12 mois) signé par l'employeur");
        documents.add("Autorisation de travail délivrée par la DREETS / DDETS");
        documents.add("Justificatif de salaire ≥ SMIC");

        if (!Boolean.TRUE.equals(contratTravailValide)) {
            criteres.add("Contrat de travail valide manquant — le passage au CRA Travailleur"
                    + " (art. 7) suppose un contrat signé et l'autorisation de travail.");
            return VERDICT_FAIBLE;
        }
        messages.add("Le changement de statut vers travailleur reste discrétionnaire."
                + " La situation de l'emploi peut être opposée hors métiers en tension.");
        return VERDICT_MOYENNE;
    }

    private static String applyRegroupement(Boolean ressourcesSuffisantes,
                                            Boolean logementDecent,
                                            Integer nbPersonnesFoyer,
                                            List<String> criteres,
                                            List<String> documents,
                                            List<String> messages) {
        documents.add("CRA 10 ans (ou 1 an renouvelé) du demandeur");
        documents.add("Acte de mariage transcrit (registre français)");
        documents.add("Actes de naissance des enfants à rejoindre");
        documents.add("Justificatifs de ressources sur les 12 derniers mois");
        documents.add("Justificatif de logement (bail, attestation surface, salubrité)");

        boolean ressourcesOk = Boolean.TRUE.equals(ressourcesSuffisantes);
        boolean logementOk = Boolean.TRUE.equals(logementDecent);

        if (!ressourcesOk) {
            criteres.add("Ressources stables et suffisantes non démontrées"
                    + " — l'accord 1968 art. 4 impose un seuil voisin du SMIC, modulable selon"
                    + " la composition du foyer.");
            return VERDICT_FAIBLE;
        }
        if (!logementOk) {
            criteres.add("Logement décent et adapté à la composition du foyer"
                    + " non démontré (art. 4 accord 1968).");
            return VERDICT_MOYENNE;
        }

        if (nbPersonnesFoyer != null && nbPersonnesFoyer > 0) {
            messages.add("Composition du foyer (" + nbPersonnesFoyer + " personnes) : la condition"
                    + " ressources est appréciée au prorata.");
        }
        messages.add("Conditions ressources et logement plus souples que le CESEDA"
                + " — la jurisprudence applique l'accord 1968 strictement.");
        return VERDICT_ELEVEE;
    }

    private static String buildFormule(VoieDescriptor d, String verdict) {
        return "Régime algérien — " + d.libelle() + " : verdict " + verdict + ".";
    }

    private record VoieDescriptor(String code,
                                  String libelle,
                                  String titreApplicable,
                                  int dureeTitreAnnees,
                                  String baseJuridique,
                                  int delaiMois) {
    }
}
