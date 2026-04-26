package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-DT-33-01 : calculateur de la recevabilité d'une procédure AT/MP française
 * (Code de la sécurité sociale).
 *
 * <p>Bases juridiques :
 * <ul>
 *   <li>CSS art. L.411-1 — présomption d'imputabilité au temps et lieu du travail</li>
 *   <li>CSS art. L.441-1 — déclaration AT employeur 48h</li>
 *   <li>CSS art. L.441-6 — certificat médical initial AT</li>
 *   <li>CSS art. L.461-1 — reconnaissance MP par tableau ou système complémentaire (CRRMP)</li>
 *   <li>CSS art. L.461-5 — certificat médical initial MP</li>
 *   <li>CSS art. L.434-2 — taux d'incapacité permanente partielle</li>
 *   <li>CSS art. L.142-2 — compétence exclusive Pôle Social TJ depuis 2019</li>
 *   <li>CSS art. R.142-1 et s. — recours amiable préalable CMRA</li>
 *   <li>CSS art. R.441-13 — délai instruction CPAM AT (30j + 60j extensible)</li>
 *   <li>CSS art. R.461-9 — délai instruction CPAM MP (120j extensible)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent belge (procédure FEDRIS — loi 03/07/1967
 * sur les accidents du travail + loi 03/06/1970 sur les maladies professionnelles)
 * sera couvert par F-DT-33-BE (backlog).
 *
 * <p>Verdict :
 * <ul>
 *   <li><b>ELEVEE</b> : critères cumulatifs réunis — recevabilité élevée.</li>
 *   <li><b>MOYENNE</b> : critères partiels — recevabilité plausible mais perfectible.</li>
 *   <li><b>FAIBLE</b> : critères majeurs absents — risque de refus important.</li>
 * </ul>
 */
public final class AtMpCalculator {

    public static final String VERDICT_ELEVEE = "ELEVEE";
    public static final String VERDICT_MOYENNE = "MOYENNE";
    public static final String VERDICT_FAIBLE = "FAIBLE";

    public static final String DISP_AT = "RECONNAISSANCE_AT";
    public static final String DISP_MP = "RECONNAISSANCE_MP";
    public static final String DISP_IPP = "CONTESTATION_TAUX_IPP";

    public static final String COMP_CPAM = "CPAM";
    public static final String COMP_CRRMP = "CRRMP";
    public static final String COMP_CMRA = "CMRA";
    public static final String COMP_TJ_POLE_SOCIAL = "TJ_POLE_SOCIAL";

    public static final String HORS_TABLEAU = "HORS_TABLEAU";

    public static final int DELAI_INSTR_AT_JOURS = 90;
    public static final int DELAI_INSTR_MP_JOURS = 120;
    public static final int DELAI_RECOURS_IPP_TOTAL_JOURS = 120;

    private AtMpCalculator() {
    }

    public static AtMpResult compute(String dispositif,
                                     LocalDate dateAccident,
                                     Boolean lieuTravail,
                                     Boolean declarationEmployeurDansLes48h,
                                     Boolean certificatMedicalInitial,
                                     String numeroTableau,
                                     Boolean delaiPriseEnChargeRespecte,
                                     LocalDate dateExposition,
                                     Integer tauxFixeParCpam,
                                     Integer tauxRevendique,
                                     Boolean expertiseMedicaleProduite,
                                     LocalDate datePremierAvisCpam,
                                     LocalDate dateAnalyse) {
        validateInputs(dispositif, dateAccident, dateExposition,
                tauxFixeParCpam, tauxRevendique, datePremierAvisCpam, dateAnalyse);

        String d = dispositif.trim().toUpperCase();
        switch (d) {
            case DISP_AT:
                return computeAt(dateAccident, lieuTravail,
                        declarationEmployeurDansLes48h, certificatMedicalInitial);
            case DISP_MP:
                return computeMp(numeroTableau, delaiPriseEnChargeRespecte,
                        dateExposition, certificatMedicalInitial);
            case DISP_IPP:
                return computeIpp(tauxFixeParCpam, tauxRevendique,
                        expertiseMedicaleProduite, datePremierAvisCpam);
            default:
                throw new IllegalArgumentException("Dispositif non supporté : " + d);
        }
    }

    private static void validateInputs(String dispositif,
                                       LocalDate dateAccident,
                                       LocalDate dateExposition,
                                       Integer tauxFixeParCpam,
                                       Integer tauxRevendique,
                                       LocalDate datePremierAvisCpam,
                                       LocalDate dateAnalyse) {
        if (dispositif == null || dispositif.isBlank()) {
            throw new IllegalArgumentException("dispositif est requis");
        }
        LocalDate today = dateAnalyse != null ? dateAnalyse : LocalDate.now();
        if (dateAccident != null && dateAccident.isAfter(today)) {
            throw new IllegalArgumentException("dateAccident ne peut pas être dans le futur");
        }
        if (dateExposition != null && dateExposition.isAfter(today)) {
            throw new IllegalArgumentException("dateExposition ne peut pas être dans le futur");
        }
        if (datePremierAvisCpam != null && datePremierAvisCpam.isAfter(today)) {
            throw new IllegalArgumentException("datePremierAvisCpam ne peut pas être dans le futur");
        }
        if (tauxFixeParCpam != null && (tauxFixeParCpam < 0 || tauxFixeParCpam > 100)) {
            throw new IllegalArgumentException("tauxFixeParCpam doit être dans [0,100]");
        }
        if (tauxRevendique != null && (tauxRevendique < 0 || tauxRevendique > 100)) {
            throw new IllegalArgumentException("tauxRevendique doit être dans [0,100]");
        }
        if (tauxFixeParCpam != null && tauxRevendique != null
                && tauxRevendique <= tauxFixeParCpam) {
            throw new IllegalArgumentException(
                    "tauxRevendique doit être strictement supérieur à tauxFixeParCpam");
        }
    }

    // ---- RECONNAISSANCE_AT --------------------------------------------------

    private static AtMpResult computeAt(LocalDate dateAccident,
                                        Boolean lieuTravail,
                                        Boolean declarationEmployeurDansLes48h,
                                        Boolean certificatMedicalInitial) {
        boolean lieu = Boolean.TRUE.equals(lieuTravail);
        boolean cmi = Boolean.TRUE.equals(certificatMedicalInitial);
        boolean declOk = Boolean.TRUE.equals(declarationEmployeurDansLes48h);

        List<String> documents = new ArrayList<>();
        documents.add("Déclaration d'accident du travail (DAT) — formulaire S6200 (employeur)");
        documents.add("Certificat médical initial (CMI) — L.441-6 CSS, à remettre à la CPAM");
        documents.add("Témoignages de collègues / matérialité de l'accident");
        documents.add("Bulletin de salaire du mois de l'accident");
        documents.add("Procès-verbal de l'inspection du travail si arrêt maladie ≥ 3 mois");

        List<String> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        String verdict;
        if (lieu && cmi) {
            verdict = VERDICT_ELEVEE;
            messages.add("Présomption d'imputabilité au travail (L.411-1 CSS) acquise — "
                    + "à charge pour la CPAM ou l'employeur de la renverser.");
        } else if (lieu || cmi) {
            verdict = VERDICT_MOYENNE;
            if (!lieu) {
                risques.add("Accident non survenu au temps et lieu du travail — la présomption "
                        + "L.411-1 ne joue pas, à compléter par d'autres preuves de matérialité.");
            }
            if (!cmi) {
                risques.add("Certificat médical initial absent — pièce maîtresse pour caractériser "
                        + "la lésion (L.441-6 CSS).");
            }
        } else {
            verdict = VERDICT_FAIBLE;
            risques.add("Ni lieu de travail ni certificat médical initial — risque sérieux de "
                    + "refus de prise en charge par la CPAM.");
        }

        if (!declOk) {
            messages.add("Attention : déclaration employeur tardive (au-delà de 48h L.441-1) — "
                    + "le salarié peut déclarer lui-même dans un délai de 2 ans (L.441-2).");
        }
        if (dateAccident != null) {
            messages.add("Date de l'accident : " + dateAccident + " — vérifier la prescription "
                    + "biennale (L.431-2 CSS).");
        }
        messages.add("Instruction CPAM : enquête 30 jours (L.441-1) + instruction médicale "
                + "+ décision motivée 60 jours = 90 jours (R.441-13).");

        boolean expertiseRequise = false; // expertise non systématique en phase reconnaissance AT

        String formule = "Reconnaissance AT — verdict " + verdict + " — instruction CPAM "
                + DELAI_INSTR_AT_JOURS + " jours.";

        return new AtMpResult(
                DISP_AT,
                "Reconnaissance accident du travail (CSS L.411-1)",
                verdict,
                DELAI_INSTR_AT_JOURS,
                COMP_CPAM,
                expertiseRequise,
                documents,
                risques,
                "CSS art. L.411-1 + L.441-1 + L.441-6 (présomption AT)",
                formule,
                messages);
    }

    // ---- RECONNAISSANCE_MP --------------------------------------------------

    private static AtMpResult computeMp(String numeroTableau,
                                        Boolean delaiPriseEnChargeRespecte,
                                        LocalDate dateExposition,
                                        Boolean certificatMedicalInitial) {
        String tableau = numeroTableau != null ? numeroTableau.trim().toUpperCase() : null;
        boolean horsTableau = tableau == null || tableau.isEmpty() || HORS_TABLEAU.equals(tableau);
        boolean delaiOk = !Boolean.FALSE.equals(delaiPriseEnChargeRespecte);
        boolean cmi = Boolean.TRUE.equals(certificatMedicalInitial);

        List<String> documents = new ArrayList<>();
        documents.add("Déclaration de maladie professionnelle (formulaire S6100) — salarié");
        documents.add("Certificat médical initial (L.461-5 CSS) mentionnant le lien avec le travail");
        documents.add("Justificatifs d'exposition à l'agent pathogène (postes occupés, durée)");
        documents.add("Attestations employeurs successifs / fiches de poste");
        documents.add("Bilan médical complet (radiologies, audiogrammes, biopsies selon tableau)");

        List<String> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        String verdict;
        String competence;
        boolean expertiseRequise = false;

        if (horsTableau) {
            // Système complémentaire L.461-1 al.4 → CRRMP
            verdict = VERDICT_MOYENNE;
            competence = COMP_CRRMP;
            expertiseRequise = true;
            messages.add("Maladie hors tableau — saisine du CRRMP (Comité Régional de "
                    + "Reconnaissance des Maladies Professionnelles, L.461-1 al.4 CSS).");
            messages.add("Le CRRMP doit constater un lien direct entre la maladie et le "
                    + "travail habituel (et IPP ≥ 25 % pour la voie complémentaire stricto sensu).");
            risques.add("Reconnaissance hors tableau plus exigeante : preuve directe du lien "
                    + "exposition / pathologie obligatoire.");
            if (!cmi) {
                risques.add("Certificat médical initial absent — pièce indispensable même en "
                        + "système complémentaire.");
            }
        } else if (!delaiOk) {
            // Tableau visé mais délai de prise en charge non respecté
            verdict = VERDICT_FAIBLE;
            competence = COMP_CRRMP;
            expertiseRequise = true;
            risques.add("Délai de prise en charge du tableau " + tableau + " non respecté — "
                    + "saisine CRRMP nécessaire (article R.461-8 CSS).");
            messages.add("Le CRRMP peut reconnaître la MP si le lien direct avec le travail "
                    + "est établi malgré le dépassement du délai du tableau.");
        } else if (cmi) {
            // Tableau + délai respecté + CMI = présomption MP joue
            verdict = VERDICT_ELEVEE;
            competence = COMP_CPAM;
            messages.add("Tableau " + tableau + " visé + délai de prise en charge respecté + "
                    + "CMI produit → présomption de MP (L.461-1 al.2 CSS).");
        } else {
            verdict = VERDICT_MOYENNE;
            competence = COMP_CPAM;
            risques.add("Certificat médical initial manquant — pièce centrale de la déclaration MP.");
        }

        if (dateExposition != null) {
            messages.add("Date d'exposition : " + dateExposition + " — vérifier la cohérence "
                    + "avec le délai de prise en charge du tableau visé.");
        }
        messages.add("Instruction CPAM : 120 jours (R.461-9 CSS), extensible de 60 jours sur "
                + "enquête complémentaire ou saisine CRRMP.");

        String formule = "Reconnaissance MP — verdict " + verdict + " — instruction "
                + DELAI_INSTR_MP_JOURS + " jours — compétence " + competence + ".";

        return new AtMpResult(
                DISP_MP,
                "Reconnaissance maladie professionnelle (CSS L.461-1)",
                verdict,
                DELAI_INSTR_MP_JOURS,
                competence,
                expertiseRequise,
                documents,
                risques,
                "CSS art. L.461-1 + L.461-5 + R.461-8 + R.461-9 (tableaux MP / CRRMP)",
                formule,
                messages);
    }

    // ---- CONTESTATION_TAUX_IPP ----------------------------------------------

    private static AtMpResult computeIpp(Integer tauxFixeParCpam,
                                         Integer tauxRevendique,
                                         Boolean expertiseMedicaleProduite,
                                         LocalDate datePremierAvisCpam) {
        int tauxFixe = tauxFixeParCpam != null ? tauxFixeParCpam : 0;
        int tauxRev = tauxRevendique != null ? tauxRevendique : tauxFixe;
        int ecart = tauxRev - tauxFixe;
        boolean expertise = Boolean.TRUE.equals(expertiseMedicaleProduite);

        List<String> documents = new ArrayList<>();
        documents.add("Notification du taux IPP par la CPAM (point de départ du délai)");
        documents.add("Rapport d'évaluation médicale du médecin conseil CPAM");
        documents.add("Rapport médical contradictoire produit par le salarié");
        documents.add("Barème indicatif d'invalidité AT-MP applicable (annexe I CSS)");
        documents.add("Saisine de la Commission Médicale de Recours Amiable (CMRA) — formulaire");
        documents.add("Si rejet CMRA : assignation devant le Pôle Social du TJ");

        List<String> risques = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        String verdict;
        if (ecart >= 10 && expertise) {
            verdict = VERDICT_ELEVEE;
            messages.add("Écart " + ecart + " points + expertise médicale produite : "
                    + "contestation solide (jurisprudence Cass. 2e civ.).");
        } else if (ecart >= 5 && expertise) {
            verdict = VERDICT_MOYENNE;
            messages.add("Écart " + ecart + " points + expertise — contestation plausible mais "
                    + "exigera un débat médical contradictoire poussé.");
        } else if (!expertise) {
            verdict = VERDICT_FAIBLE;
            risques.add("Sans rapport médical contradictoire, la contestation a peu de chance "
                    + "d'aboutir (la CMRA et le TJ s'appuient sur des éléments médicaux objectifs).");
        } else {
            verdict = VERDICT_FAIBLE;
            risques.add("Écart " + ecart + " points trop faible — la juridiction n'infirmera pas "
                    + "le taux CPAM pour quelques points sans motivation médicale forte.");
        }

        messages.add("Phase amiable obligatoire : recours devant la CMRA dans 60 jours à compter "
                + "de la notification CPAM (R.142-1 CSS).");
        messages.add("Phase judiciaire : si rejet CMRA, saisine du Pôle Social du TJ dans "
                + "60 jours (compétence exclusive depuis 2019 — L.142-2 CSS).");
        if (datePremierAvisCpam != null) {
            messages.add("Premier avis CPAM : " + datePremierAvisCpam
                    + " — vérifier impérativement les délais de recours.");
        }

        String formule = "Contestation taux IPP — verdict " + verdict + " — écart " + ecart
                + " points — recours CMRA puis TJ Pôle Social.";

        return new AtMpResult(
                DISP_IPP,
                "Contestation taux IPP (CSS L.434-2)",
                verdict,
                DELAI_RECOURS_IPP_TOTAL_JOURS,
                COMP_CMRA,
                true,
                documents,
                risques,
                "CSS art. L.434-2 + L.142-2 + R.142-1 (taux IPP / CMRA / TJ Pôle Social)",
                formule,
                messages);
    }
}
