package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-11-01 : calculateur pour l'analyse d'un changement de statut CESEDA
 * — transition d'un titre de séjour à un autre (étudiant → salarié, visiteur → salarié,
 * VPF → autre, transitions entre sous-catégories passeport talent).
 *
 * <p>Bases juridiques :
 * <ul>
 *   <li>L.421-1 et s. CESEDA — titres de séjour pour motif professionnel</li>
 *   <li>L.422-10 CESEDA — autorisation provisoire de séjour (APS) post-études</li>
 *   <li>R.5221-3 et s. Code du travail — autorisation de travail</li>
 *   <li>L.421-9 à L.421-15 CESEDA — sous-catégories passeport talent (cf. F-IM-10)</li>
 * </ul>
 *
 * <p>Outil <b>single-country FR</b>. Pour la Belgique, le changement de motif relève
 * d'une procédure distincte (modification 9bis OE) traitée par F-IM-11-BE (backlog).
 *
 * <p>Verdict :
 * <ul>
 *   <li><b>ELEVEE</b> : tous les critères réunis et transition admise par CESEDA.</li>
 *   <li><b>MOYENNE</b> : transition possible mais conditions limites (rémunération entre
 *       1,0 et 1,5 SMIC, transition autorisation préalable obligatoire, etc.).</li>
 *   <li><b>FAIBLE</b> : critère bloquant (durée restante &lt; 2 mois → renvoi vers
 *       renouvellement classique, casier non vierge, justificatif absent pour transition
 *       exigeante, rémunération sous SMIC).</li>
 * </ul>
 */
public final class ChangementStatutCalculator {

    /** SMIC mensuel brut 2026 (1801,80 €). Constante temporaire — à terme via legal_referentials. */
    public static final BigDecimal SMIC_MENSUEL_BRUT_EUR_2026 = new BigDecimal("1801.80");

    /** Multiplicateur SMIC requis pour autoriser le passage étudiant → salarié (R.5221-3). */
    public static final BigDecimal MULTIPLICATEUR_SMIC_ETUDIANT_SALARIE = new BigDecimal("1.5");

    /** Durée restante minimale (mois) sur le titre actuel pour un changement de statut. */
    public static final int DUREE_RESTANTE_MIN_MOIS = 2;

    /** Délai d'instruction par défaut (mois) en préfecture. */
    public static final int DELAI_INSTRUCTION_DEFAULT_MOIS = 3;

    /** Délai d'instruction APS (1 an renouvelable 1 fois). */
    public static final int DELAI_INSTRUCTION_APS_MOIS = 2;

    public static final String VERDICT_ELEVEE = "ELEVEE";
    public static final String VERDICT_MOYENNE = "MOYENNE";
    public static final String VERDICT_FAIBLE = "FAIBLE";

    public static final String TITRE_ETUDIANT = "ETUDIANT";
    public static final String TITRE_VISITEUR = "VISITEUR";
    public static final String TITRE_VPF = "VPF";
    public static final String TITRE_SALARIE = "SALARIE";
    public static final String TITRE_APS = "APS";
    public static final String TITRE_TALENT_SALARIE_QUALIFIE = "PASSEPORT_TALENT_SALARIE_QUALIFIE";
    public static final String TITRE_TALENT_INNOVANT = "PASSEPORT_TALENT_INNOVANT";
    public static final String TITRE_TALENT_CHERCHEUR = "PASSEPORT_TALENT_CHERCHEUR";
    public static final String TITRE_TALENT_ENTREPRENEUR = "PASSEPORT_TALENT_ENTREPRENEUR";

    private ChangementStatutCalculator() {
    }

    public static ChangementStatutResult compute(String titreActuel,
                                                 String titreEnvisage,
                                                 int dureeRestanteMois,
                                                 boolean documentJustificatifFourni,
                                                 BigDecimal remunerationContratEur,
                                                 boolean casierJudiciaireVierge) {
        validateInputs(titreActuel, titreEnvisage, dureeRestanteMois, remunerationContratEur);

        String src = titreActuel.trim().toUpperCase();
        String dst = titreEnvisage.trim().toUpperCase();

        if (src.equals(dst)) {
            throw new IllegalArgumentException(
                    "Transition non supportée : titre actuel et envisagé identiques (" + src + ")");
        }

        Transition t = resolveTransition(src, dst);

        List<String> documentsRequis = new ArrayList<>();
        List<String> risqueRefus = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // Pré-blocage : durée restante < 2 mois → préfecture renvoie en renouvellement classique
        boolean dureeBloque = dureeRestanteMois < DUREE_RESTANTE_MIN_MOIS;
        if (dureeBloque) {
            risqueRefus.add("Durée restante (" + dureeRestanteMois + " mois) inférieure à "
                    + DUREE_RESTANTE_MIN_MOIS + " mois — la préfecture renverra vers une demande de"
                    + " renouvellement classique avant changement de statut.");
        }

        // Pré-blocage : casier judiciaire non vierge
        if (!casierJudiciaireVierge) {
            risqueRefus.add("Casier judiciaire non vierge — risque de refus pour menace ordre public"
                    + " (vérification fiche police obligatoire).");
        }

        // Application de la transition
        String verdictBase = applyTransition(t, documentJustificatifFourni, remunerationContratEur,
                documentsRequis, risqueRefus, messages);

        // Verdict final : si bloque transversal, on rétrograde
        String verdictFinal = verdictBase;
        if (dureeBloque || !casierJudiciaireVierge) {
            verdictFinal = VERDICT_FAIBLE;
        }

        // Le délai APS spécifique (2 mois) s'applique tant que la transition implique
        // l'APS — que ce soit en source (post-études → VPF) ou en destination (étudiant → APS).
        int delai = (t.dst().equals(TITRE_APS) || t.src().equals(TITRE_APS))
                ? DELAI_INSTRUCTION_APS_MOIS
                : DELAI_INSTRUCTION_DEFAULT_MOIS;

        String formule = buildFormule(t, verdictFinal, dureeRestanteMois, remunerationContratEur);
        messages.add("Délai d'instruction estimatif : " + delai + " mois en préfecture.");
        messages.add("Le changement de statut doit être déposé avant l'expiration du titre actuel.");

        return new ChangementStatutResult(
                src,
                dst,
                t.dst(),
                dureeRestanteMois,
                documentJustificatifFourni,
                remunerationContratEur,
                casierJudiciaireVierge,
                verdictFinal,
                documentsRequis,
                risqueRefus,
                delai,
                t.baseJuridique(),
                formule,
                messages);
    }

    private static void validateInputs(String titreActuel,
                                       String titreEnvisage,
                                       int dureeRestanteMois,
                                       BigDecimal remunerationContratEur) {
        if (titreActuel == null || titreActuel.isBlank()) {
            throw new IllegalArgumentException("titreActuel est requis");
        }
        if (titreEnvisage == null || titreEnvisage.isBlank()) {
            throw new IllegalArgumentException("titreEnvisage est requis");
        }
        if (dureeRestanteMois < 0) {
            throw new IllegalArgumentException("dureeRestanteSurTitreActuelMois doit être ≥ 0");
        }
        if (remunerationContratEur != null
                && remunerationContratEur.signum() < 0) {
            throw new IllegalArgumentException("remunerationContratEur doit être ≥ 0");
        }
    }

    /**
     * Récupère le couple transition + base juridique pour la combinaison (src, dst).
     * Lance IllegalArgumentException si la transition n'est pas encadrée par CESEDA.
     */
    private static Transition resolveTransition(String src, String dst) {
        // ETUDIANT → SALARIE
        if (TITRE_ETUDIANT.equals(src) && TITRE_SALARIE.equals(dst)) {
            return new Transition(src, dst, TransitionKind.ETUDIANT_SALARIE,
                    "CESEDA L.421-1 + R.5221-3 Code du travail");
        }
        // ETUDIANT → APS (post-études)
        if (TITRE_ETUDIANT.equals(src) && TITRE_APS.equals(dst)) {
            return new Transition(src, dst, TransitionKind.ETUDIANT_APS,
                    "CESEDA L.422-10");
        }
        // VISITEUR → SALARIE
        if (TITRE_VISITEUR.equals(src) && TITRE_SALARIE.equals(dst)) {
            return new Transition(src, dst, TransitionKind.VISITEUR_SALARIE,
                    "CESEDA L.421-1 + R.5221-3");
        }
        // VPF → SALARIE
        if (TITRE_VPF.equals(src) && TITRE_SALARIE.equals(dst)) {
            return new Transition(src, dst, TransitionKind.VPF_SALARIE,
                    "CESEDA L.423-1 (VPF autorise déjà le travail)");
        }
        // VPF → ETUDIANT
        if (TITRE_VPF.equals(src) && TITRE_ETUDIANT.equals(dst)) {
            return new Transition(src, dst, TransitionKind.VPF_ETUDIANT,
                    "CESEDA L.422-1 (étudiant)");
        }
        // Transitions intra Passeport Talent
        if (isTalent(src) && isTalent(dst)) {
            return new Transition(src, dst, TransitionKind.TALENT_INTRA,
                    "CESEDA L.421-9 à L.421-15 (passeport talent — sous-catégories)");
        }
        // PASSEPORT_TALENT_* → SALARIE classique
        if (isTalent(src) && TITRE_SALARIE.equals(dst)) {
            return new Transition(src, dst, TransitionKind.TALENT_SALARIE,
                    "CESEDA L.421-1 + R.5221-3 (sortie passeport talent)");
        }

        // SF-IM-11-04 : transitions vers VPF (conjoint FR / parent enfant FR / PACS)
        // ETUDIANT → VPF
        if (TITRE_ETUDIANT.equals(src) && TITRE_VPF.equals(dst)) {
            return new Transition(src, dst, TransitionKind.ETUDIANT_VPF,
                    "CESEDA L.423-1 (conjoint FR) / L.423-7 (parent enfant FR) / L.423-8 (PACS)");
        }
        // VISITEUR → VPF
        if (TITRE_VISITEUR.equals(src) && TITRE_VPF.equals(dst)) {
            return new Transition(src, dst, TransitionKind.VISITEUR_VPF,
                    "CESEDA L.423-1 / L.423-7 / L.423-8");
        }
        // SALARIE → VPF
        if (TITRE_SALARIE.equals(src) && TITRE_VPF.equals(dst)) {
            return new Transition(src, dst, TransitionKind.SALARIE_VPF,
                    "CESEDA L.423-1 / L.423-7 / L.423-8");
        }
        // PASSEPORT_TALENT_* → VPF
        if (isTalent(src) && TITRE_VPF.equals(dst)) {
            return new Transition(src, dst, TransitionKind.TALENT_VPF,
                    "CESEDA L.423-1 / L.423-7 / L.423-8 (sortie passeport talent)");
        }
        // APS → VPF
        if (TITRE_APS.equals(src) && TITRE_VPF.equals(dst)) {
            return new Transition(src, dst, TransitionKind.APS_VPF,
                    "CESEDA L.423-1 / L.423-7 / L.423-8");
        }

        throw new IllegalArgumentException(
                "Transition non supportée : " + src + " → " + dst);
    }

    private static boolean isTalent(String code) {
        return code != null && code.startsWith("PASSEPORT_TALENT_");
    }

    /**
     * Applique la logique propre à la transition. Retourne le verdict de base (avant
     * application des bloques transversaux durée/casier).
     */
    private static String applyTransition(Transition t,
                                          boolean justificatif,
                                          BigDecimal remuneration,
                                          List<String> documentsRequis,
                                          List<String> risqueRefus,
                                          List<String> messages) {
        switch (t.kind()) {
            case ETUDIANT_SALARIE:
                documentsRequis.add("Contrat de travail signé, à temps plein, en lien avec la formation");
                documentsRequis.add("Diplôme ou attestation de réussite");
                documentsRequis.add("3 derniers bulletins de paie (si déjà en poste)");
                documentsRequis.add("Justificatif de domicile et passeport en cours de validité");
                if (!justificatif) {
                    risqueRefus.add("Contrat de travail non fourni — pièce essentielle pour passage salarié.");
                    return VERDICT_FAIBLE;
                }
                if (remuneration == null) {
                    risqueRefus.add("Rémunération du contrat non communiquée — vérifier ≥ 1,5 SMIC.");
                    return VERDICT_MOYENNE;
                }
                BigDecimal seuilHaut = SMIC_MENSUEL_BRUT_EUR_2026
                        .multiply(MULTIPLICATEUR_SMIC_ETUDIANT_SALARIE)
                        .setScale(2, RoundingMode.HALF_UP);
                if (remuneration.compareTo(SMIC_MENSUEL_BRUT_EUR_2026) < 0) {
                    risqueRefus.add("Rémunération (" + remuneration + " €) inférieure au SMIC ("
                            + SMIC_MENSUEL_BRUT_EUR_2026 + " €) — refus quasi-certain.");
                    return VERDICT_FAIBLE;
                }
                if (remuneration.compareTo(seuilHaut) < 0) {
                    risqueRefus.add("Rémunération (" + remuneration + " €) inférieure à 1,5 SMIC ("
                            + seuilHaut + " €) — exigence R.5221-3 non atteinte, autorisation"
                            + " de travail incertaine.");
                    return VERDICT_MOYENNE;
                }
                messages.add("Rémunération conforme à 1,5 SMIC : autorisation de travail favorable.");
                return VERDICT_ELEVEE;

            case ETUDIANT_APS:
                documentsRequis.add("Diplôme de niveau master au minimum (ou équivalent)");
                documentsRequis.add("Justificatif de recherche d'emploi en lien avec la formation");
                documentsRequis.add("Justificatif de moyens d'existence pour la durée de l'APS");
                if (!justificatif) {
                    risqueRefus.add("Diplôme/attestation école non fourni — pièce essentielle.");
                    return VERDICT_FAIBLE;
                }
                messages.add("APS art. L.422-10 : 1 an renouvelable 1 fois. À l'issue, transition"
                        + " automatique vers titre salarié possible si CDI obtenu.");
                return VERDICT_ELEVEE;

            case VISITEUR_SALARIE:
                documentsRequis.add("Contrat de travail signé");
                documentsRequis.add("Demande d'autorisation de travail préalable (DREETS)");
                documentsRequis.add("Métier en tension OU passeport talent applicable");
                if (!justificatif) {
                    risqueRefus.add("Contrat de travail non fourni — passage salarié impossible sans.");
                    return VERDICT_FAIBLE;
                }
                risqueRefus.add("Le statut visiteur n'autorise pas le travail : autorisation préalable"
                        + " DREETS obligatoire (R.5221-3) — délai d'instruction supplémentaire 2 mois.");
                if (remuneration != null
                        && remuneration.compareTo(SMIC_MENSUEL_BRUT_EUR_2026) < 0) {
                    risqueRefus.add("Rémunération sous SMIC — refus quasi-certain.");
                    return VERDICT_FAIBLE;
                }
                return VERDICT_MOYENNE;

            case VPF_SALARIE:
                documentsRequis.add("Contrat de travail signé (formalité)");
                documentsRequis.add("Justificatif de la situation VPF qui ouvre déjà droit au travail");
                messages.add("Le titre VPF autorise déjà l'exercice d'une activité salariée :"
                        + " la transition formelle vers un titre 'salarié' n'est pas obligatoire."
                        + " Procédure simplifiée — pas d'autorisation préalable DREETS.");
                return VERDICT_ELEVEE;

            case VPF_ETUDIANT:
                documentsRequis.add("Attestation d'inscription dans un établissement supérieur");
                documentsRequis.add("Justificatif de moyens d'existence ≥ 615 €/mois");
                if (!justificatif) {
                    risqueRefus.add("Attestation d'inscription école non fournie — pièce essentielle.");
                    return VERDICT_FAIBLE;
                }
                messages.add("Le passage VPF → étudiant est possible mais peu courant : vérifier"
                        + " l'opportunité (perte de droits associés au VPF).");
                return VERDICT_ELEVEE;

            case TALENT_INTRA:
                documentsRequis.add("Justificatifs propres à la nouvelle sous-catégorie passeport talent");
                documentsRequis.add("Lettre de mission / contrat / projet selon sous-catégorie cible");
                if (!justificatif) {
                    risqueRefus.add("Justificatifs spécifiques sous-catégorie cible non fournis.");
                    return VERDICT_FAIBLE;
                }
                messages.add("Transition intra passeport talent (L.421-9 à L.421-15) : exige justification"
                        + " des nouvelles conditions propres à la sous-catégorie ciblée (cf. F-IM-10).");
                return VERDICT_MOYENNE;

            case TALENT_SALARIE:
                documentsRequis.add("Contrat de travail signé");
                documentsRequis.add("Demande d'autorisation de travail (R.5221-3)");
                if (!justificatif) {
                    risqueRefus.add("Contrat de travail non fourni — passage salarié impossible.");
                    return VERDICT_FAIBLE;
                }
                if (remuneration != null
                        && remuneration.compareTo(SMIC_MENSUEL_BRUT_EUR_2026) < 0) {
                    risqueRefus.add("Rémunération sous SMIC — refus quasi-certain.");
                    return VERDICT_FAIBLE;
                }
                messages.add("Sortie du dispositif passeport talent vers salarié de droit commun :"
                        + " perte des avantages talent (durée 4 ans, mobilité européenne).");
                return VERDICT_MOYENNE;

            // SF-IM-11-04 : transitions vers VPF (conjoint FR / parent enfant FR / PACS)
            case ETUDIANT_VPF:
            case VISITEUR_VPF:
            case SALARIE_VPF:
            case TALENT_VPF:
            case APS_VPF:
                return applyTransitionVersVpf(justificatif, documentsRequis, risqueRefus, messages);
        }
        // unreachable
        return VERDICT_FAIBLE;
    }

    /**
     * Logique commune aux 5 transitions vers VPF (CESEDA L.423-1 / L.423-7 / L.423-8).
     *
     * <p>Spécificité : la rémunération n'est <b>pas</b> exigée (différent des transitions
     * vers SALARIE qui imposent SMIC × 1,5). Le verdict de base dépend uniquement de la
     * production du justificatif (acte de mariage / acte de naissance / convention PACS).
     */
    private static String applyTransitionVersVpf(boolean justificatif,
                                                 List<String> documentsRequis,
                                                 List<String> risqueRefus,
                                                 List<String> messages) {
        documentsRequis.add("Acte civil de référence (acte de mariage / acte de naissance d'enfant FR / convention PACS)");
        documentsRequis.add("Justificatif de domicile commun");
        documentsRequis.add("Attestation de communauté de vie effective (mariage > 6 mois — L.423-1)");
        documentsRequis.add("Passeport en cours de validité");

        messages.add("Communauté de vie effective requise : 6 mois minimum (L.423-1) "
                + "ou 1 an pour délivrance directe carte de résident (L.423-2).");
        messages.add("Rupture de la vie commune dans les 3 ans entraîne retrait du titre (L.432-4).");

        if (!justificatif) {
            risqueRefus.add("Acte civil de référence (mariage / naissance enfant FR / PACS) "
                    + "non produit — pièce essentielle pour passage VPF.");
            return VERDICT_FAIBLE;
        }
        return VERDICT_ELEVEE;
    }

    private static String buildFormule(Transition t,
                                       String verdict,
                                       int dureeRestanteMois,
                                       BigDecimal remuneration) {
        StringBuilder sb = new StringBuilder("Changement de statut ");
        sb.append(t.src()).append(" → ").append(t.dst());
        sb.append(" : verdict ").append(verdict);
        sb.append(" (durée restante ").append(dureeRestanteMois).append(" mois");
        if (remuneration != null) {
            sb.append(", rémunération ").append(remuneration).append(" €");
        }
        sb.append(").");
        return sb.toString();
    }

    /** Couple transition source → destination + métadonnées. */
    private record Transition(String src, String dst, TransitionKind kind, String baseJuridique) {
    }

    private enum TransitionKind {
        ETUDIANT_SALARIE,
        ETUDIANT_APS,
        VISITEUR_SALARIE,
        VPF_SALARIE,
        VPF_ETUDIANT,
        TALENT_INTRA,
        TALENT_SALARIE,
        // SF-IM-11-04 : transitions vers VPF (conjoint FR / parent enfant FR / PACS)
        ETUDIANT_VPF,
        VISITEUR_VPF,
        SALARIE_VPF,
        TALENT_VPF,
        APS_VPF
    }
}
