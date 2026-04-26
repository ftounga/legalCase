package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-FA-27-01 : calculateur de la recevabilité d'une demande relevant de la bioéthique
 * familiale (loi 2/8/2021 + Cass. 18/12/2022).
 *
 * <p>Bases juridiques :
 * <ul>
 *   <li>Art. 342-9 et s. Cciv (loi bioéthique 2021) — reconnaissance conjointe anticipée
 *       PMA pour couples de femmes / femmes seules. La reconnaissance doit intervenir
 *       AVANT la PMA, par acte notarié.</li>
 *   <li>Cass. ass. plén. 4 arrêts 18/12/2022 (n° 21-12.394 et al.) — clarification du
 *       régime de transcription / adoption pour les enfants nés par GPA légale à l'étranger.</li>
 *   <li>Art. 16-8-1 Cciv (loi bioéthique 2021, en vigueur depuis 1/9/2022) — accès
 *       à l'identité du donneur de gamètes pour l'enfant majeur (≥ 18 ans).</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. L'équivalent BE (loi 6/7/2007 PMA, GPA non encadrée)
 * sera couvert par F-FA-27-BE (backlog).
 *
 * <p>Verdict :
 * <ul>
 *   <li><b>ELEVEE</b> : tous les critères du dispositif sont remplis.</li>
 *   <li><b>MOYENNE</b> : critères principaux remplis mais éléments accessoires manquants.</li>
 *   <li><b>FAIBLE</b> : critère bloquant (pays non légal, antériorité non respectée,
 *       âge insuffisant, etc.).</li>
 * </ul>
 */
public final class PmaGpaBioethiqueCalculator {

    public static final String VERDICT_ELEVEE = "ELEVEE";
    public static final String VERDICT_MOYENNE = "MOYENNE";
    public static final String VERDICT_FAIBLE = "FAIBLE";

    public static final String RISQUE_FAIBLE = "FAIBLE";
    public static final String RISQUE_MOYEN = "MOYEN";
    public static final String RISQUE_ELEVE = "ELEVE";

    public static final String DISPOSITIF_PMA = "PMA_RECONNAISSANCE_ANTICIPEE";
    public static final String DISPOSITIF_GPA = "GPA_TRANSCRIPTION_ETAT_CIVIL";
    public static final String DISPOSITIF_DON = "DON_GAMETES_ACCES_ORIGINES";

    public static final int DELAI_PMA_MOIS = 1;
    public static final int DELAI_GPA_MOIS = 9;
    public static final int DELAI_DON_MOIS = 6;

    public static final int AGE_MIN_ACCES_ORIGINES = 18;
    /** Loi bioéthique 2021 : article 16-8-1 entré en vigueur le 1/9/2022. */
    public static final LocalDate DATE_ENTREE_VIGUEUR_ACCES_ORIGINES =
            LocalDate.of(2022, 9, 1);

    private PmaGpaBioethiqueCalculator() {
    }

    public static PmaGpaBioethiqueResult compute(String dispositif,
                                                 Boolean consentementsConjointsNotaire,
                                                 LocalDate dateReconnaissanceAnterieurePMA,
                                                 LocalDate datePMA,
                                                 Boolean conditionsAccesPMA,
                                                 Boolean paysGPALegal,
                                                 Boolean parentBiologiqueAvere,
                                                 Boolean decisionEtrangereProduite,
                                                 Boolean adoptionDemande,
                                                 LocalDate dateDon,
                                                 Boolean demandeAcces,
                                                 Integer ageDemandeur) {
        validateInputs(dispositif, ageDemandeur);

        String d = dispositif.trim().toUpperCase();
        switch (d) {
            case DISPOSITIF_PMA:
                return computePMA(consentementsConjointsNotaire,
                        dateReconnaissanceAnterieurePMA, datePMA, conditionsAccesPMA);
            case DISPOSITIF_GPA:
                return computeGPA(paysGPALegal, parentBiologiqueAvere,
                        decisionEtrangereProduite, adoptionDemande);
            case DISPOSITIF_DON:
                return computeDon(dateDon, demandeAcces, ageDemandeur);
            default:
                throw new IllegalArgumentException("Dispositif non supporté : " + d);
        }
    }

    private static void validateInputs(String dispositif, Integer ageDemandeur) {
        if (dispositif == null || dispositif.isBlank()) {
            throw new IllegalArgumentException("dispositif est requis");
        }
        if (ageDemandeur != null && ageDemandeur < 0) {
            throw new IllegalArgumentException("ageDemandeur doit être ≥ 0");
        }
    }

    // ---- PMA — Reconnaissance anticipée (art. 342-9 et s. Cciv loi 2021) ---------

    private static PmaGpaBioethiqueResult computePMA(Boolean consentementNotaire,
                                                     LocalDate dateRecon,
                                                     LocalDate datePMA,
                                                     Boolean conditionsAcces) {
        List<String> procedures = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        documents.add("Acte de consentement à la PMA reçu par notaire (art. 342-10 Cciv)");
        documents.add("Acte de reconnaissance conjointe anticipée notarié");
        documents.add("Justificatif d'accès à la PMA (couple de femmes ou femme seule, ≥ 18 ans)");
        documents.add("Pièces d'identité des deux parents d'intention");

        procedures.add("Établir l'acte de consentement à la PMA devant notaire");
        procedures.add("Recevoir la reconnaissance conjointe anticipée par acte notarié AVANT la PMA");
        procedures.add("Procéder à la PMA dans un centre agréé en France");
        procedures.add("Présenter l'acte de reconnaissance à l'officier d'état civil à la naissance");

        boolean notaireOk = Boolean.TRUE.equals(consentementNotaire);
        boolean conditionsOk = Boolean.TRUE.equals(conditionsAcces);
        boolean anteriorite = checkAnteriorite(dateRecon, datePMA);

        String verdict;
        String risque;
        if (!notaireOk) {
            verdict = VERDICT_FAIBLE;
            risque = RISQUE_ELEVE;
            messages.add("Le consentement et la reconnaissance doivent être recueillis par "
                    + "acte notarié (art. 342-10 Cciv) — sous peine de nullité du dispositif.");
        } else if (!conditionsOk) {
            verdict = VERDICT_FAIBLE;
            risque = RISQUE_ELEVE;
            messages.add("Les conditions d'accès à la PMA (couple de femmes ou femme seule, "
                    + "≥ 18 ans, pas de séparation, capacité) ne sont pas remplies.");
        } else if (!anteriorite) {
            verdict = VERDICT_FAIBLE;
            risque = RISQUE_ELEVE;
            messages.add("La reconnaissance conjointe anticipée doit impérativement précéder "
                    + "la PMA (art. 342-11 Cciv) — postérieure, elle est sans effet.");
        } else {
            verdict = VERDICT_ELEVEE;
            risque = RISQUE_FAIBLE;
            messages.add("Reconnaissance notariée antérieure à la PMA + conditions d'accès remplies "
                    + "= filiation établie de plein droit dès la naissance (art. 342-11 Cciv).");
        }

        messages.add("Délai d'instruction estimatif (établissement de l'état civil) : "
                + DELAI_PMA_MOIS + " mois.");

        String formule = "PMA reconnaissance anticipée — verdict " + verdict + ".";
        String baseJuridique = "Code civil art. 342-9 à 342-13 (loi bioéthique 2/8/2021)";

        return new PmaGpaBioethiqueResult(DISPOSITIF_PMA, verdict, procedures, risque,
                documents, DELAI_PMA_MOIS, baseJuridique, formule, messages);
    }

    private static boolean checkAnteriorite(LocalDate dateRecon, LocalDate datePMA) {
        if (dateRecon == null || datePMA == null) {
            // Si l'avocat n'a pas renseigné les dates, on présume que la chronologie est OK
            return true;
        }
        return dateRecon.isBefore(datePMA);
    }

    // ---- GPA — Transcription état civil (Cass. 4 arrêts 18/12/2022) -------------

    private static PmaGpaBioethiqueResult computeGPA(Boolean paysLegal,
                                                     Boolean biologique,
                                                     Boolean decisionEtrangere,
                                                     Boolean adoption) {
        List<String> procedures = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        documents.add("Acte de naissance étranger de l'enfant (apostillé / légalisé)");
        documents.add("Décision étrangère établissant la filiation (jugement de GPA)");
        documents.add("Convention de gestation avec la mère porteuse (pays légal)");
        documents.add("Justificatif de la nationalité française du parent biologique");
        documents.add("Test de filiation génétique (rapport biologique)");
        documents.add("Requête en transcription auprès du procureur de la République de Nantes");

        procedures.add("Faire transcrire l'acte de naissance étranger sur les registres "
                + "français (parent biologique uniquement) — procureur de Nantes");
        procedures.add("Pour l'autre parent d'intention : engager une adoption simple "
                + "(art. 360 et s. Cciv) en parallèle / postérieurement");
        procedures.add("En cas de refus de transcription : exercer un recours devant le TJ Nantes "
                + "puis le cas échéant devant la cour d'appel");

        boolean paysOk = Boolean.TRUE.equals(paysLegal);
        boolean bioOk = Boolean.TRUE.equals(biologique);
        boolean decisionOk = Boolean.TRUE.equals(decisionEtrangere);
        boolean adoptionOk = Boolean.TRUE.equals(adoption);

        String verdict;
        String risque;
        if (!paysOk) {
            verdict = VERDICT_FAIBLE;
            risque = RISQUE_ELEVE;
            messages.add("La GPA doit avoir été pratiquée dans un pays où elle est légale "
                    + "(USA, Canada, Royaume-Uni, etc.) — à défaut, le risque de refus de "
                    + "transcription est très élevé.");
        } else if (!bioOk) {
            verdict = VERDICT_FAIBLE;
            risque = RISQUE_ELEVE;
            messages.add("Cass. ass. plén. 4 arrêts 18/12/2022 : la transcription automatique "
                    + "exige que le parent transcrit soit génétiquement parent. Sans lien biologique "
                    + "avéré, la voie ouverte est l'adoption (art. 360 Cciv).");
        } else if (!decisionOk) {
            verdict = VERDICT_FAIBLE;
            risque = RISQUE_ELEVE;
            messages.add("La décision étrangère établissant la filiation est nécessaire à la "
                    + "transcription (jurisprudence constante).");
        } else if (!adoptionOk) {
            verdict = VERDICT_MOYENNE;
            risque = RISQUE_MOYEN;
            messages.add("Le parent biologique sera transcrit automatiquement, mais l'autre "
                    + "parent d'intention restera dépourvu de filiation tant qu'une procédure "
                    + "d'adoption simple (art. 360+ Cciv) n'aura pas été engagée.");
        } else {
            verdict = VERDICT_ELEVEE;
            risque = RISQUE_FAIBLE;
            messages.add("Pays légal + lien biologique + décision étrangère + adoption simple "
                    + "demandée = filiation des 2 parents établie (transcription + adoption).");
        }

        messages.add("Cass. ass. plén. 18/12/2022 (Ménesson et al.) : régime stabilisé pour la "
                + "GPA légale à l'étranger — transcription automatique parent biologique + "
                + "adoption simple parent d'intention.");
        messages.add("Délai d'instruction estimatif (transcription + adoption simple) : "
                + DELAI_GPA_MOIS + " mois.");

        String formule = "GPA transcription état civil — verdict " + verdict + ".";
        String baseJuridique = "Code civil art. 47 + 360 (adoption simple) — Cass. ass. plén. "
                + "4 arrêts 18/12/2022 (n° 21-12.394, 20-15.733, 21-21.330, 21-23.119)";

        return new PmaGpaBioethiqueResult(DISPOSITIF_GPA, verdict, procedures, risque,
                documents, DELAI_GPA_MOIS, baseJuridique, formule, messages);
    }

    // ---- DON GAMÈTES — Accès aux origines (art. 16-8-1 Cciv loi 2021) -----------

    private static PmaGpaBioethiqueResult computeDon(LocalDate dateDon,
                                                     Boolean demandeAcces,
                                                     Integer ageDemandeur) {
        List<String> procedures = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        documents.add("Pièce d'identité du demandeur (justifiant ≥ 18 ans)");
        documents.add("Acte de naissance du demandeur");
        documents.add("Demande écrite à la Commission d'accès aux données non identifiantes "
                + "et à l'identité du tiers donneur (CAPADD)");
        documents.add("Le cas échéant : justificatif de la PMA dont est issu le demandeur");

        procedures.add("Saisir la CAPADD (Commission d'accès) par formulaire dédié");
        procedures.add("La CAPADD interroge l'Agence de la biomédecine qui détient les données");
        procedures.add("La CAPADD informe le donneur de la demande (consentement préalable "
                + "présumé pour les dons postérieurs au 1/9/2022)");
        procedures.add("Réponse à la demande (identité ou données non identifiantes)");

        String verdict;
        String risque;
        boolean demandeOk = Boolean.TRUE.equals(demandeAcces);
        boolean ageOk = ageDemandeur != null && ageDemandeur >= AGE_MIN_ACCES_ORIGINES;
        boolean donPosterieurEntreeVigueur = dateDon == null
                || !dateDon.isBefore(DATE_ENTREE_VIGUEUR_ACCES_ORIGINES);

        if (!ageOk) {
            verdict = VERDICT_FAIBLE;
            risque = RISQUE_ELEVE;
            messages.add("L'accès aux origines est ouvert au seul enfant majeur "
                    + "(≥ " + AGE_MIN_ACCES_ORIGINES + " ans, art. 16-8-1 Cciv).");
        } else if (!demandeOk) {
            verdict = VERDICT_FAIBLE;
            risque = RISQUE_ELEVE;
            messages.add("Aucune demande formelle n'a été déposée auprès de la CAPADD — "
                    + "la procédure ne peut s'engager qu'à la demande expresse de l'enfant majeur.");
        } else if (!donPosterieurEntreeVigueur) {
            verdict = VERDICT_MOYENNE;
            risque = RISQUE_MOYEN;
            messages.add("Don antérieur au 1/9/2022 : l'accès à l'identité requiert le "
                    + "consentement du donneur (régime transitoire art. 6 loi 2/8/2021). "
                    + "Données non identifiantes accessibles, identité conditionnée à accord donneur.");
        } else {
            verdict = VERDICT_ELEVEE;
            risque = RISQUE_FAIBLE;
            messages.add("Don postérieur au 1/9/2022 + demandeur majeur + demande formelle "
                    + "= accès à l'identité de plein droit (art. 16-8-1 Cciv loi 2021).");
        }

        messages.add("La CAPADD est l'autorité compétente. L'Agence de la biomédecine conserve "
                + "les données. Le donneur peut, depuis le 1/9/2022, être informé de la demande.");
        messages.add("Délai d'instruction estimatif (CAPADD) : " + DELAI_DON_MOIS + " mois.");

        String formule = "Don de gamètes accès aux origines — verdict " + verdict + ".";
        String baseJuridique = "Code civil art. 16-8-1 (loi bioéthique 2/8/2021, en vigueur 1/9/2022)";

        return new PmaGpaBioethiqueResult(DISPOSITIF_DON, verdict, procedures, risque,
                documents, DELAI_DON_MOIS, baseJuridique, formule, messages);
    }
}
