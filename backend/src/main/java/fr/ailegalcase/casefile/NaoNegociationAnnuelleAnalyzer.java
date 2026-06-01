package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-29 : analyseur de la <b>conformité de la négociation annuelle obligatoire
 * (NAO)</b> côté employeur (art. L.2242-1 à L.2242-8, L.2242-11, L.2242-15,
 * L.2242-17 CT, F-DT-66). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Applicabilité</b> — l'obligation n'est déclenchée que si un délégué
 *       syndical est présent (art. L.2242-1). À défaut → {@code NON_APPLICABLE}.</li>
 *   <li><b>Checklist de conformité</b> (si applicable) — chaque item produit
 *       {@code { item, conforme, obligatoire, commentaire }} :
 *       <ol>
 *         <li>Bloc « rémunération, temps de travail, partage de la valeur »
 *             engagé (obligatoire, L.2242-15) ;</li>
 *         <li>Bloc « égalité professionnelle F/H et QVT » engagé (obligatoire,
 *             L.2242-17) ;</li>
 *         <li>Périodicité respectée (obligatoire ; ≤ 12 mois sans accord de
 *             méthode, ≤ 48 mois avec, L.2242-11) ;</li>
 *         <li>PV de désaccord établi (obligatoire si la négociation n'a pas
 *             abouti).</li>
 *       </ol></li>
 *   <li><b>Calculateur de délai</b> — si {@code dateDerniereNegociation} présente,
 *       {@code dateProchaineEcheance = dateDerniereNegociation + periodiciteMois}.
 *       {@code statutEcheance} : DEPASSEE si jours &lt; 0, ECHEANCE_PROCHE si
 *       0 ≤ jours ≤ 60, A_JOUR sinon. Le calcul est relatif à {@code today}
 *       (injecté pour la testabilité).</li>
 *   <li><b>Verdict</b> :
 *       <ul>
 *         <li>DS absent → {@code NON_APPLICABLE} ;</li>
 *         <li>applicable + tous items obligatoires conformes + échéance non
 *             dépassée → {@code CONFORME} ;</li>
 *         <li>sinon → {@code NON_CONFORME}.</li>
 *       </ul></li>
 *   <li><b>Risque</b> : ELEVE si un bloc obligatoire n'est pas engagé (délit
 *       d'entrave L.2243-2), MODERE pour les autres cas NON_CONFORME, FAIBLE si
 *       CONFORME ou NON_APPLICABLE.</li>
 * </ul>
 *
 * <p>Hors périmètre : validité d'un accord d'entreprise issu de la NAO (F-DT-67),
 * index égalité professionnelle F/H (F-DT-101), élections CSE (F-DT-65). Base
 * juridique annotée « à vérifier par avocat ».
 */
public final class NaoNegociationAnnuelleAnalyzer {

    /** Périodicité par défaut de la NAO en l'absence d'accord de méthode (mois). */
    static final int PERIODICITE_DEFAUT_MOIS = 12;

    /** Plafond de périodicité avec accord de méthode (4 ans, art. L.2242-11). */
    static final int PERIODICITE_MAX_MOIS = 48;

    /** Fenêtre (jours) en deçà de laquelle l'échéance est jugée proche. */
    static final int ECHEANCE_PROCHE_JOURS = 60;

    static final String BASE_JURIDIQUE =
            "art. L.2242-1 CT — engagement annuel d'une négociation dans les "
                    + "entreprises pourvues d'un ou plusieurs délégués syndicaux ; "
                    + "art. L.2242-15 CT — bloc « rémunération, temps de travail et "
                    + "partage de la valeur ajoutée » ; art. L.2242-17 CT — bloc "
                    + "« égalité professionnelle F/H et qualité de vie au travail » "
                    + "(incluant le droit à la déconnexion) ; art. L.2242-11 CT — accord "
                    + "de méthode pouvant porter la périodicité à 4 ans maximum ; "
                    + "art. L.2242-8 et L.2243-2 CT — défaut de négociation : délit "
                    + "d'entrave et pénalité financière en matière d'égalité "
                    + "professionnelle (à vérifier par avocat)";

    private NaoNegociationAnnuelleAnalyzer() {
    }

    /**
     * Analyse la conformité de la NAO : apprécie l'applicabilité, construit la
     * checklist, calcule l'échéance et rend le verdict global.
     *
     * @param today date de référence pour le calcul de l'échéance (injectée).
     */
    public static NaoNegociationAnnuelleResult analyze(
            Integer effectif,
            Boolean delegueSyndicalPresent,
            Boolean blocRemunerationNegocie,
            Boolean blocEgaliteQvtNegocie,
            Boolean accordMethodePeriodicite,
            LocalDate dateDerniereNegociation,
            Integer periodiciteMois,
            Boolean pvDesaccordEtabli,
            Boolean negociationAboutie,
            LocalDate today) {

        int periodicite = periodiciteMois == null ? PERIODICITE_DEFAUT_MOIS : periodiciteMois;
        validate(effectif, delegueSyndicalPresent, blocRemunerationNegocie,
                blocEgaliteQvtNegocie, periodicite);

        int eff = effectif;
        boolean ds = delegueSyndicalPresent;
        boolean accordMethode = accordMethodePeriodicite != null && accordMethodePeriodicite;
        boolean blocRemuneration = blocRemunerationNegocie;
        boolean blocEgaliteQvt = blocEgaliteQvtNegocie;
        boolean pvDesaccord = pvDesaccordEtabli != null && pvDesaccordEtabli;
        boolean aboutie = negociationAboutie != null && negociationAboutie;

        // ── Calcul de l'échéance (indépendant de l'applicabilité) ───────────
        LocalDate dateProchaineEcheance = null;
        Integer joursAvantEcheance = null;
        NaoStatutEcheance statutEcheance = null;
        if (dateDerniereNegociation != null) {
            dateProchaineEcheance = dateDerniereNegociation.plusMonths(periodicite);
            joursAvantEcheance = (int) ChronoUnit.DAYS.between(today, dateProchaineEcheance);
            statutEcheance = appreciaterEcheance(joursAvantEcheance);
        }

        // ── Applicabilité : pas de DS → pas de NAO ──────────────────────────
        if (!ds) {
            return new NaoNegociationAnnuelleResult(
                    eff,
                    false,
                    false,
                    List.of(),
                    periodicite,
                    dateProchaineEcheance,
                    joursAvantEcheance,
                    statutEcheance,
                    0,
                    NaoNegociationAnnuelleStatut.NON_APPLICABLE,
                    NaoRisqueEntrave.FAIBLE,
                    List.of("Obligation de négociation annuelle conditionnée à la présence "
                            + "d'au moins un délégué syndical (art. L.2242-1) : non applicable "
                            + "en l'absence de DS désigné."),
                    BASE_JURIDIQUE);
        }

        List<NaoChecklistItem> checklist = new ArrayList<>();
        List<String> consequences = new ArrayList<>();

        // ── Item 1 — bloc rémunération / temps de travail / valeur (L.2242-15)
        checklist.add(new NaoChecklistItem(
                "Bloc « rémunération, temps de travail et partage de la valeur » négocié",
                blocRemuneration,
                true,
                "Obligatoire chaque année (art. L.2242-15) : salaires effectifs, durée et "
                        + "organisation du temps de travail, épargne salariale, partage de la "
                        + "valeur ajoutée."));
        if (!blocRemuneration) {
            consequences.add("Engager la négociation sur le bloc « rémunération, temps de "
                    + "travail et partage de la valeur » (art. L.2242-15) : bloc obligatoire "
                    + "non engagé.");
        }

        // ── Item 2 — bloc égalité pro / QVT (L.2242-17) ─────────────────────
        checklist.add(new NaoChecklistItem(
                "Bloc « égalité professionnelle F/H et qualité de vie au travail » négocié",
                blocEgaliteQvt,
                true,
                "Obligatoire chaque année (art. L.2242-17) : égalité professionnelle entre "
                        + "les femmes et les hommes, qualité de vie au travail, droit à la "
                        + "déconnexion."));
        if (!blocEgaliteQvt) {
            consequences.add("Engager la négociation sur le bloc « égalité professionnelle "
                    + "F/H et QVT » (art. L.2242-17) : bloc obligatoire non engagé — pénalité "
                    + "égalité F/H encourue (jusqu'à 1 % de la masse salariale).");
        }

        // ── Item 3 — périodicité respectée (L.2242-11) ──────────────────────
        boolean periodiciteConforme = periodicite <= PERIODICITE_DEFAUT_MOIS || accordMethode;
        checklist.add(new NaoChecklistItem(
                "Périodicité de négociation respectée",
                periodiciteConforme,
                true,
                periodicite > PERIODICITE_DEFAUT_MOIS
                        ? "Une périodicité supérieure à 12 mois (max 48) suppose un accord de "
                                + "méthode (art. L.2242-11). À défaut, la négociation reste annuelle."
                        : "Périodicité annuelle (12 mois) par défaut (art. L.2242-1)."));
        if (!periodiciteConforme) {
            consequences.add("Régulariser la périodicité : une périodicité de " + periodicite
                    + " mois suppose un accord de méthode (art. L.2242-11). À défaut, la "
                    + "négociation doit être engagée chaque année.");
        }

        // ── Item 4 — PV de désaccord (obligatoire si négociation non aboutie)
        boolean pvObligatoire = !aboutie;
        checklist.add(new NaoChecklistItem(
                "PV de désaccord établi",
                pvDesaccord || aboutie,
                pvObligatoire,
                pvObligatoire
                        ? "Obligatoire en cas d'échec de la négociation : un PV de désaccord "
                                + "consigne les propositions respectives et les mesures que "
                                + "l'employeur entend appliquer unilatéralement (art. L.2242-5)."
                        : "Sans objet : la négociation a abouti à un accord."));
        if (pvObligatoire && !pvDesaccord) {
            consequences.add("Établir un PV de désaccord consignant les dernières propositions "
                    + "des parties et les mesures unilatérales de l'employeur (art. L.2242-5).");
        }

        // ── Comptage des items obligatoires manquants ───────────────────────
        int itemsObligatoiresManquants = (int) checklist.stream()
                .filter(NaoChecklistItem::obligatoire)
                .filter(i -> !i.conforme())
                .count();

        boolean echeanceDepassee = statutEcheance == NaoStatutEcheance.DEPASSEE;
        boolean blocManquant = !blocRemuneration || !blocEgaliteQvt;

        // ── Verdict global ──────────────────────────────────────────────────
        NaoNegociationAnnuelleStatut statut;
        if (itemsObligatoiresManquants == 0 && !echeanceDepassee) {
            statut = NaoNegociationAnnuelleStatut.CONFORME;
        } else {
            statut = NaoNegociationAnnuelleStatut.NON_CONFORME;
            if (echeanceDepassee) {
                consequences.add("Échéance de négociation dépassée (art. L.2242-1) : engager "
                        + "sans délai la négociation annuelle — le défaut de négociation peut "
                        + "caractériser un délit d'entrave (art. L.2243-2).");
            }
        }

        // ── Risque d'entrave ────────────────────────────────────────────────
        NaoRisqueEntrave risqueEntrave;
        if (statut == NaoNegociationAnnuelleStatut.CONFORME) {
            risqueEntrave = NaoRisqueEntrave.FAIBLE;
        } else if (blocManquant) {
            risqueEntrave = NaoRisqueEntrave.ELEVE;
            consequences.add("Risque de délit d'entrave (art. L.2243-2) et de pénalité "
                    + "financière en matière d'égalité professionnelle : un bloc obligatoire "
                    + "n'a pas été engagé alors qu'un délégué syndical est désigné.");
        } else {
            risqueEntrave = NaoRisqueEntrave.MODERE;
        }

        return new NaoNegociationAnnuelleResult(
                eff,
                true,
                true,
                List.copyOf(checklist),
                periodicite,
                dateProchaineEcheance,
                joursAvantEcheance,
                statutEcheance,
                itemsObligatoiresManquants,
                statut,
                risqueEntrave,
                List.copyOf(consequences),
                BASE_JURIDIQUE);
    }

    /**
     * Apprécie le statut de l'échéance : DEPASSEE si le délai est négatif,
     * ECHEANCE_PROCHE si 0 ≤ jours ≤ 60, A_JOUR au-delà.
     */
    static NaoStatutEcheance appreciaterEcheance(int joursAvantEcheance) {
        if (joursAvantEcheance < 0) {
            return NaoStatutEcheance.DEPASSEE;
        }
        if (joursAvantEcheance <= ECHEANCE_PROCHE_JOURS) {
            return NaoStatutEcheance.ECHEANCE_PROCHE;
        }
        return NaoStatutEcheance.A_JOUR;
    }

    private static void validate(Integer effectif,
                                 Boolean delegueSyndicalPresent,
                                 Boolean blocRemunerationNegocie,
                                 Boolean blocEgaliteQvtNegocie,
                                 int periodiciteMois) {
        if (effectif == null) {
            throw new IllegalArgumentException("effectif est requis");
        }
        if (effectif <= 0) {
            throw new IllegalArgumentException("effectif doit être strictement positif");
        }
        if (delegueSyndicalPresent == null) {
            throw new IllegalArgumentException("delegueSyndicalPresent est requis");
        }
        if (blocRemunerationNegocie == null) {
            throw new IllegalArgumentException("blocRemunerationNegocie est requis");
        }
        if (blocEgaliteQvtNegocie == null) {
            throw new IllegalArgumentException("blocEgaliteQvtNegocie est requis");
        }
        if (periodiciteMois < 1) {
            throw new IllegalArgumentException("periodiciteMois doit être au moins égal à 1");
        }
        if (periodiciteMois > PERIODICITE_MAX_MOIS) {
            throw new IllegalArgumentException(
                    "periodiciteMois ne peut pas dépasser 48 mois (art. L.2242-11)");
        }
    }
}
