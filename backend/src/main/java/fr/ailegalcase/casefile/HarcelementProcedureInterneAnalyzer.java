package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-27 : analyseur de la <b>conformité de la procédure interne de traitement
 * d'un signalement de harcèlement</b> côté employeur (art. L.1153-5-1, L.2314-1,
 * L.1152-4 ; L.4121-1 CT, F-DT-59). Outil <b>FRANCE UNIQUEMENT</b>, distinct de
 * F-DT-11 (nullité du licenciement consécutif au harcèlement).
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Checklist de conformité</b> — chaque item produit
 *       {@code { item, conforme, obligatoire, commentaire }} :
 *       <ol>
 *         <li>Référent CSE harcèlement sexuel désigné (obligatoire si
 *             {@code effectif ≥ 11}, L.2314-1) ;</li>
 *         <li>Référent employeur désigné (obligatoire si {@code effectif ≥ 250},
 *             L.1153-5-1) ;</li>
 *         <li>Information / affichage des salariés réalisé (toujours obligatoire,
 *             L.1152-4, L.1153-5) ;</li>
 *         <li>Enquête interne déclenchée à réception d'un signalement (obligatoire
 *             si {@code signalementRecu}, L.4121-1) ;</li>
 *         <li>Enquête contradictoire et impartiale (obligatoire si une enquête est
 *             ouverte) ;</li>
 *         <li>Mesures conservatoires envisagées (recommandé si
 *             {@code signalementRecu}).</li>
 *       </ol></li>
 *   <li><b>Délai de réaction</b> — si {@code dateSignalement} et
 *       {@code dateOuvertureEnquete} sont présents, {@code delaiReactionJours} =
 *       nombre de jours. {@code delaiRaisonnable} : OUI ≤ 15 j, LIMITE 16–60 j,
 *       NON &gt; 60 j ou signalement reçu sans enquête ouverte.</li>
 *   <li><b>Verdict de conformité</b> :
 *       <ul>
 *         <li>signalement reçu et enquête non déclenchée / non contradictoire /
 *             délai NON → {@code CARENCE_GRAVE} (manquement à l'obligation de
 *             sécurité L.4121-1) ;</li>
 *         <li>sinon, au moins un item obligatoire non conforme →
 *             {@code NON_CONFORME} ;</li>
 *         <li>tous les items obligatoires conformes → {@code CONFORME}.</li>
 *       </ul></li>
 *   <li><b>Risque</b> : ELEVE si CARENCE_GRAVE, MODERE si NON_CONFORME, FAIBLE si
 *       CONFORME.</li>
 * </ul>
 *
 * <p>Hors périmètre : nullité du licenciement consécutif au harcèlement (F-DT-11),
 * chiffrage des dommages-intérêts pour préjudice de harcèlement, conformité des
 * élections / IRP (F-DT-65). Base juridique annotée « à vérifier par avocat ».
 */
public final class HarcelementProcedureInterneAnalyzer {

    /** Seuil d'effectif déclenchant le CSE et son référent harcèlement (L.2314-1). */
    static final int SEUIL_EFFECTIF_REFERENT_CSE = 11;

    /** Seuil d'effectif déclenchant le référent employeur (L.1153-5-1). */
    static final int SEUIL_EFFECTIF_REFERENT_EMPLOYEUR = 250;

    /** Délai (jours) en deçà duquel la réaction est prompte (délai raisonnable OUI). */
    static final int DELAI_RAISONNABLE_OUI_JOURS = 15;

    /** Délai (jours) au-delà duquel la réaction est tardive (délai raisonnable NON). */
    static final int DELAI_RAISONNABLE_LIMITE_JOURS = 60;

    static final String BASE_JURIDIQUE =
            "art. L.2314-1 CT — désignation d'un référent en matière de lutte contre "
                    + "le harcèlement sexuel et les agissements sexistes parmi les membres du "
                    + "CSE (toute entreprise dotée d'un CSE, dès 11 salariés) ; "
                    + "art. L.1153-5-1 CT — référent harcèlement sexuel désigné par "
                    + "l'employeur dans les entreprises d'au moins 250 salariés ; "
                    + "art. L.1152-4 et L.1153-5 CT — obligation d'information / d'affichage "
                    + "et de prévention ; art. L.4121-1 CT — obligation de sécurité : "
                    + "l'employeur doit diligenter une enquête contradictoire à réception "
                    + "d'un signalement (à vérifier par avocat)";

    private HarcelementProcedureInterneAnalyzer() {
    }

    /**
     * Analyse la conformité de la procédure interne : construit la checklist,
     * apprécie le délai de réaction et rend le verdict global.
     */
    public static HarcelementProcedureInterneResult analyze(
            Integer effectif,
            Boolean referentCseDesigne,
            Boolean referentEmployeurDesigne,
            Boolean informationAffichageRealisee,
            Boolean signalementRecu,
            LocalDate dateSignalement,
            LocalDate dateOuvertureEnquete,
            Boolean enqueteContradictoire,
            Boolean mesuresConservatoiresPrises) {

        validate(effectif, referentCseDesigne, informationAffichageRealisee, signalementRecu,
                dateSignalement, dateOuvertureEnquete);

        int eff = effectif;
        boolean referentCse = referentCseDesigne;
        boolean referentEmployeur = referentEmployeurDesigne != null && referentEmployeurDesigne;
        boolean affichage = informationAffichageRealisee;
        boolean signalement = signalementRecu;
        boolean contradictoire = enqueteContradictoire != null && enqueteContradictoire;
        boolean mesuresConservatoires = mesuresConservatoiresPrises != null && mesuresConservatoiresPrises;
        boolean enqueteOuverte = dateOuvertureEnquete != null;

        List<HarcelementChecklistItem> checklist = new ArrayList<>();
        List<String> consequences = new ArrayList<>();

        // ── Item 1 — référent CSE harcèlement sexuel (L.2314-1) ─────────────
        boolean referentCseObligatoire = eff >= SEUIL_EFFECTIF_REFERENT_CSE;
        checklist.add(new HarcelementChecklistItem(
                "Référent CSE harcèlement sexuel désigné",
                referentCse,
                referentCseObligatoire,
                referentCseObligatoire
                        ? "Obligatoire à partir de 11 salariés (CSE) : un référent harcèlement "
                                + "sexuel doit être désigné parmi les membres du CSE (art. L.2314-1)."
                        : "Recommandé : en deçà de 11 salariés, le CSE et son référent ne sont "
                                + "pas obligatoires, mais l'obligation de prévention demeure."));
        if (referentCseObligatoire && !referentCse) {
            consequences.add("Désigner un référent harcèlement sexuel parmi les membres du CSE "
                    + "(art. L.2314-1) : obligation non satisfaite.");
        }

        // ── Item 2 — référent employeur (L.1153-5-1) ────────────────────────
        boolean referentEmployeurObligatoire = eff >= SEUIL_EFFECTIF_REFERENT_EMPLOYEUR;
        checklist.add(new HarcelementChecklistItem(
                "Référent employeur harcèlement sexuel désigné",
                referentEmployeur,
                referentEmployeurObligatoire,
                referentEmployeurObligatoire
                        ? "Obligatoire à partir de 250 salariés : l'employeur doit désigner un "
                                + "référent chargé d'orienter, d'informer et d'accompagner les "
                                + "salariés (art. L.1153-5-1)."
                        : "Recommandé : en deçà de 250 salariés, le référent employeur n'est pas "
                                + "obligatoire."));
        if (referentEmployeurObligatoire && !referentEmployeur) {
            consequences.add("Désigner un référent employeur harcèlement sexuel "
                    + "(art. L.1153-5-1) : obligation non satisfaite (effectif ≥ 250).");
        }

        // ── Item 3 — information / affichage (L.1152-4, L.1153-5) ────────────
        checklist.add(new HarcelementChecklistItem(
                "Information / affichage des salariés réalisé",
                affichage,
                true,
                "Toujours obligatoire : information des salariés sur les sanctions et voies de "
                        + "recours en matière de harcèlement (art. L.1152-4, L.1153-5)."));
        if (!affichage) {
            consequences.add("Réaliser l'information / l'affichage des salariés sur le harcèlement "
                    + "et les voies de recours (art. L.1152-4, L.1153-5) : obligation non satisfaite.");
        }

        // ── Délai de réaction ───────────────────────────────────────────────
        Integer delaiReactionJours = null;
        if (signalement && dateSignalement != null && dateOuvertureEnquete != null) {
            delaiReactionJours = (int) ChronoUnit.DAYS.between(dateSignalement, dateOuvertureEnquete);
        }
        HarcelementProcedureInterneDelaiRaisonnable delaiRaisonnable =
                appreciaterDelai(signalement, enqueteOuverte, delaiReactionJours);

        // ── Item 4 — enquête déclenchée à réception d'un signalement (L.4121-1)
        boolean enqueteCarence = false;
        if (signalement) {
            boolean enqueteDeclenchee = enqueteOuverte;
            checklist.add(new HarcelementChecklistItem(
                    "Enquête interne déclenchée à réception du signalement",
                    enqueteDeclenchee,
                    true,
                    "Obligatoire : à réception d'un signalement, l'employeur tenu à une "
                            + "obligation de sécurité doit diligenter une enquête interne "
                            + "(art. L.4121-1, L.1152-4)."));

            // ── Item 5 — enquête contradictoire et impartiale ───────────────
            checklist.add(new HarcelementChecklistItem(
                    "Enquête contradictoire et impartiale",
                    contradictoire,
                    enqueteDeclenchee,
                    enqueteDeclenchee
                            ? "Obligatoire dès lors qu'une enquête est ouverte : audition "
                                    + "contradictoire du signalant et de la personne mise en cause, "
                                    + "impartialité de l'enquête."
                            : "Une enquête contradictoire suppose qu'une enquête ait été ouverte."));

            // ── Item 6 — mesures conservatoires (recommandé) ────────────────
            checklist.add(new HarcelementChecklistItem(
                    "Mesures conservatoires envisagées durant l'enquête",
                    mesuresConservatoires,
                    false,
                    "Recommandé : envisager des mesures conservatoires (éloignement, "
                            + "suspension) pour protéger le salarié signalant durant l'enquête."));

            if (!enqueteDeclenchee || !contradictoire
                    || delaiRaisonnable == HarcelementProcedureInterneDelaiRaisonnable.NON) {
                enqueteCarence = true;
            }
            if (!enqueteDeclenchee) {
                consequences.add("Diligenter sans délai une enquête interne contradictoire : "
                        + "un signalement a été reçu sans enquête ouverte — manquement à "
                        + "l'obligation de sécurité (art. L.4121-1).");
            } else if (!contradictoire) {
                consequences.add("Sécuriser le caractère contradictoire et impartial de "
                        + "l'enquête (audition du signalant et de la personne mise en cause).");
            }
            if (!mesuresConservatoires) {
                consequences.add("Envisager des mesures conservatoires (éloignement, suspension) "
                        + "pour protéger le salarié signalant durant l'enquête.");
            }
        }

        // ── Comptage des items obligatoires manquants ───────────────────────
        int itemsObligatoiresManquants = (int) checklist.stream()
                .filter(HarcelementChecklistItem::obligatoire)
                .filter(i -> !i.conforme())
                .count();

        // ── Verdict global ──────────────────────────────────────────────────
        HarcelementProcedureInterneVerdict statut;
        if (enqueteCarence) {
            statut = HarcelementProcedureInterneVerdict.CARENCE_GRAVE;
            consequences.add("Manquement à l'obligation de sécurité (art. L.4121-1) susceptible "
                    + "d'engager la responsabilité de l'employeur : carence dans le traitement "
                    + "du signalement de harcèlement.");
        } else if (itemsObligatoiresManquants > 0) {
            statut = HarcelementProcedureInterneVerdict.NON_CONFORME;
        } else {
            statut = HarcelementProcedureInterneVerdict.CONFORME;
        }

        HarcelementProcedureInterneRisque risque = switch (statut) {
            case CARENCE_GRAVE -> HarcelementProcedureInterneRisque.ELEVE;
            case NON_CONFORME -> HarcelementProcedureInterneRisque.MODERE;
            case CONFORME -> HarcelementProcedureInterneRisque.FAIBLE;
        };

        return new HarcelementProcedureInterneResult(
                eff,
                signalement,
                List.copyOf(checklist),
                delaiReactionJours,
                delaiRaisonnable,
                itemsObligatoiresManquants,
                statut,
                risque,
                List.copyOf(consequences),
                BASE_JURIDIQUE);
    }

    /**
     * Apprécie le délai de réaction. NON si un signalement a été reçu sans enquête
     * ouverte, ou si le délai dépasse 60 jours. OUI ≤ 15 jours, LIMITE 16–60 jours.
     * Si aucun signalement n'est en cause, le délai est OUI (sans objet).
     */
    static HarcelementProcedureInterneDelaiRaisonnable appreciaterDelai(
            boolean signalementRecu, boolean enqueteOuverte, Integer delaiReactionJours) {
        if (!signalementRecu) {
            return HarcelementProcedureInterneDelaiRaisonnable.OUI;
        }
        if (!enqueteOuverte) {
            return HarcelementProcedureInterneDelaiRaisonnable.NON;
        }
        if (delaiReactionJours == null) {
            // Enquête ouverte mais délai non mesurable (date de signalement absente).
            return HarcelementProcedureInterneDelaiRaisonnable.LIMITE;
        }
        if (delaiReactionJours <= DELAI_RAISONNABLE_OUI_JOURS) {
            return HarcelementProcedureInterneDelaiRaisonnable.OUI;
        }
        if (delaiReactionJours <= DELAI_RAISONNABLE_LIMITE_JOURS) {
            return HarcelementProcedureInterneDelaiRaisonnable.LIMITE;
        }
        return HarcelementProcedureInterneDelaiRaisonnable.NON;
    }

    private static void validate(Integer effectif,
                                 Boolean referentCseDesigne,
                                 Boolean informationAffichageRealisee,
                                 Boolean signalementRecu,
                                 LocalDate dateSignalement,
                                 LocalDate dateOuvertureEnquete) {
        if (effectif == null) {
            throw new IllegalArgumentException("effectif est requis");
        }
        if (effectif <= 0) {
            throw new IllegalArgumentException("effectif doit être strictement positif");
        }
        if (referentCseDesigne == null) {
            throw new IllegalArgumentException("referentCseDesigne est requis");
        }
        if (informationAffichageRealisee == null) {
            throw new IllegalArgumentException("informationAffichageRealisee est requis");
        }
        if (signalementRecu == null) {
            throw new IllegalArgumentException("signalementRecu est requis");
        }
        if (dateSignalement != null && dateOuvertureEnquete != null
                && dateOuvertureEnquete.isBefore(dateSignalement)) {
            throw new IllegalArgumentException(
                    "dateOuvertureEnquete ne peut pas précéder dateSignalement");
        }
    }
}
