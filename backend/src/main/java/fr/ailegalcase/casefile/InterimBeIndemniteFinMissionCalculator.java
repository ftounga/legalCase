package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SF-219-15 : calculateur des indemnités de fin de mission intérim BE
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 24/07/1987</b> relative au travail temporaire (M.B.
 *       20/08/1987), art. 8 + art. 10 (parité salariale stricte avec
 *       le permanent de référence chez l'utilisateur).</li>
 *   <li><b>AR du 30/03/1967</b> sur les vacances annuelles, art. 19
 *       (taux pécule de vacances = 15,38 % du brut de l'exercice de
 *       référence, dont 7,67 % pécule simple + 7,71 % pécule double).</li>
 *   <li><b>CCT n° 322bis du 16/12/2010</b> conclue au CNT —
 *       responsabilité ETI pour le versement du pécule de vacances
 *       intérim via le <i>Fonds Social pour les Intérimaires</i>
 *       (FSI).</li>
 *   <li><b>CCT sectorielles propres à la CP de l'utilisateur</b> —
 *       prime de fin d'année (« 13e mois ») due par application de
 *       l'art. 10 Loi 24/07/1987 + CCT n° 322 du 14/06/2010.</li>
 *   <li><b>Loi du 03/07/1978</b> sur les contrats de travail, art. 39
 *       et s. — indemnité compensatoire de préavis applicable par
 *       renvoi.</li>
 *   <li><b>Loi du 16/03/1971</b> sur le travail, art. 29 — sursalaires
 *       50 % en semaine, 100 % le dimanche / jour férié.</li>
 *   <li><b>Cass. (BE) 04/02/1991</b>, S.90.0024.F + <b>Cass. (BE)
 *       16/12/2002</b>, S.01.0124.F — rupture anticipée d'une mission
 *       intérim à durée déterminée par l'ETI sans motif grave ouvre
 *       droit aux rémunérations restant à courir jusqu'au terme.</li>
 *   <li><b>Cass. (BE) 03/05/2010</b>, S.09.0086.F + <b>Cass. (BE)
 *       23/06/2003</b>, S.02.0103.F — REFUS d'une prime de précarité
 *       forfaitaire de 10 % style FR (C. trav. fr. art. L. 1251-32).
 *       Pas d'analogue belge.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR</b> — l'ETI a
 *       rompu sans motif grave avant le terme prévu : indemnité
 *       égale au salaire brut sur les jours restants jusqu'au terme,
 *       prime sur le calcul ordinaire. Cass. 04/02/1991.</li>
 *   <li><b>A_ANALYSER_SECTEUR_NON_RECONNU</b> — CP utilisateur =
 *       AUTRE_NON_RECENSE → prime fin d'année non calculable en
 *       automatique. Outil renvoie pécule + heures sup mais marque
 *       la prime fin d'année à analyser.</li>
 *   <li><b>INDEMNITES_DUES</b> — au moins une composante non nulle.</li>
 *   <li><b>AUCUNE_INDEMNITE_DUE</b> — fin au terme, pécule FSI déjà
 *       versé, ancienneté sectorielle insuffisante, pas d'heures sup.</li>
 * </ol>
 *
 * <h2>Constantes paramétriques</h2>
 * <ul>
 *   <li>{@link #TAUX_PECULE_VACANCES_INTERIM} = 15,38 % (AR 30/03/1967
 *       art. 19 — total pécule simple 7,67 % + double 7,71 %).</li>
 *   <li>{@link #ANCIENNETE_SECTORIELLE_MINIMALE_JOURS} = 65 jours
 *       (seuil indicatif majoritaire CCT sectorielles pour la prime
 *       fin d'année, à confirmer cas par cas).</li>
 *   <li>{@link #TAUX_SURSALAIRE_SEMAINE} = 50 % (Loi 16/03/1971 art. 29).</li>
 *   <li>{@link #TAUX_SURSALAIRE_DIMANCHE_FERIE} = 100 % (id.).</li>
 * </ul>
 */
public final class InterimBeIndemniteFinMissionCalculator {

    /** Taux pécule de vacances intérim — AR 30/03/1967 art. 19 (7,67 % + 7,71 %). */
    public static final BigDecimal TAUX_PECULE_VACANCES_INTERIM =
            new BigDecimal("0.1538");

    /** Ancienneté sectorielle indicative pour ouvrir droit à la prime fin d'année. */
    public static final int ANCIENNETE_SECTORIELLE_MINIMALE_JOURS = 65;

    /** Sursalaire 50 % heures supplémentaires en semaine — Loi 16/03/1971 art. 29. */
    public static final BigDecimal TAUX_SURSALAIRE_SEMAINE = new BigDecimal("0.50");

    /** Sursalaire 100 % dimanche / jour férié — Loi 16/03/1971 art. 29. */
    public static final BigDecimal TAUX_SURSALAIRE_DIMANCHE_FERIE = new BigDecimal("1.00");

    static final String BASE_JURIDIQUE =
            "Loi du 24/07/1987 sur le travail temporaire (M.B. 20/08/1987),"
                    + " art. 8 et art. 10 (parité salariale) ; AR du 30/03/1967"
                    + " sur les vacances annuelles, art. 19 (pécule = 15,38 %"
                    + " du brut, soit 7,67 % simple + 7,71 % double) ; CCT"
                    + " n° 322 du 14/06/2010 conclue au CNT (responsabilité"
                    + " solidaire ETI/utilisateur, parité salariale) ; CCT"
                    + " n° 322bis du 16/12/2010 (responsabilité ETI pour"
                    + " versement pécule de vacances via le Fonds Social pour"
                    + " les Intérimaires – FSI) ; CCT sectorielles propres à"
                    + " la commission paritaire de l'utilisateur pour la"
                    + " prime de fin d'année (CP 124 construction CCT"
                    + " 12/06/2014 AR 11/02/2015, CP 200 employés CCT"
                    + " 09/06/2016 AR 13/06/2017, CP 140 transport CCT"
                    + " 27/01/2011 AR 30/03/2011, etc.) ; Loi du 03/07/1978"
                    + " sur les contrats de travail, art. 39 et s. (indemnité"
                    + " de préavis applicable par renvoi) ; Loi du 16/03/1971"
                    + " sur le travail, art. 29 (sursalaires 50 % semaine,"
                    + " 100 % dimanche/jour férié) ; Cass. (BE) 04/02/1991,"
                    + " S.90.0024.F et 16/12/2002, S.01.0124.F (rupture"
                    + " anticipée injustifiée d'une mission intérim ouvre"
                    + " droit au salaire restant à courir jusqu'au terme) ;"
                    + " Cass. (BE) 03/05/2010, S.09.0086.F et 23/06/2003,"
                    + " S.02.0103.F (PAS de prime de précarité forfaitaire"
                    + " 10 % analogue à l'IFM française art. L. 1251-32 en"
                    + " droit belge).";

    static final String AVERT_AVOCAT_BE =
            "Aucune prime de précarité forfaitaire 10 % de type français"
                    + " (C. trav. fr. art. L. 1251-32) n'est due en droit"
                    + " belge à la fin d'une mission intérimaire (Cass."
                    + " 03/05/2010 et 23/06/2003). Toute transposition"
                    + " mécanique du droit FR est à proscrire — la confusion"
                    + " est récurrente dans la jurisprudence du fond. Les"
                    + " taux exacts du pécule de vacances (15,38 % par"
                    + " défaut) et de la prime de fin d'année (variable"
                    + " selon la CCT sectorielle de la CP de l'utilisateur"
                    + " en vigueur à la date de la mission) doivent être"
                    + " confirmés par un avocat spécialisé en droit social"
                    + " BE. Le seuil indicatif d'ancienneté sectorielle"
                    + " (65 jours) doit également être vérifié pour la CCT"
                    + " sectorielle applicable. En cas de rupture anticipée"
                    + " contestée, l'avocat doit vérifier l'absence de motif"
                    + " grave invocable par l'ETI (Loi 03/07/1978 art. 35)"
                    + " avant de revendiquer l'indemnité de restant à courir.";

    private InterimBeIndemniteFinMissionCalculator() {
    }

    public static InterimBeIndemniteFinMissionResult analyze(
            InterimBeIndemniteFinMissionRequest request) {
        validateInputs(request);

        // ── Calculs communs ────────────────────────────────────────────────────
        // Salaire brut total des prestations (heures normales × taux horaire)
        BigDecimal salaireBrutTotalPrestations = request.salaireHoraireBrut()
                .multiply(request.heuresPrestees())
                .setScale(2, RoundingMode.HALF_UP);

        // Pécule de vacances intérim : 0 si FSI a déjà versé, sinon 15,38 %
        BigDecimal peculeVacancesInterim = request.peculeDejaVerseParFsi()
                ? BigDecimal.ZERO.setScale(2)
                : salaireBrutTotalPrestations
                        .multiply(TAUX_PECULE_VACANCES_INTERIM)
                        .setScale(2, RoundingMode.HALF_UP);

        // Ancienneté sectorielle suffisante ?
        boolean ancienneteSuffisante = request.ancienneteSectorielleJours()
                >= ANCIENNETE_SECTORIELLE_MINIMALE_JOURS;

        // Prime de fin d'année sectorielle : taux et applicabilité selon CP
        BigDecimal tauxPrimeFinAnnee = tauxPrimeFinAnneePourCp(
                request.commissionParitaireUtilisateur());
        BigDecimal primeFinAnneeSectorielle;
        if (request.commissionParitaireUtilisateur()
                == InterimBeCommissionParitaire.AUTRE_NON_RECENSE) {
            primeFinAnneeSectorielle = BigDecimal.ZERO.setScale(2);
        } else if (!ancienneteSuffisante) {
            primeFinAnneeSectorielle = BigDecimal.ZERO.setScale(2);
        } else {
            primeFinAnneeSectorielle = salaireBrutTotalPrestations
                    .multiply(tauxPrimeFinAnnee)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Sursalaire heures sup : 50 % semaine + 100 % dimanche/férié
        BigDecimal sursalaireSemaine = request.heuresSupplementairesSemaine()
                .multiply(request.salaireHoraireBrut())
                .multiply(BigDecimal.ONE.add(TAUX_SURSALAIRE_SEMAINE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal sursalaireDimanche = request.heuresSupplementairesDimancheFerie()
                .multiply(request.salaireHoraireBrut())
                .multiply(BigDecimal.ONE.add(TAUX_SURSALAIRE_DIMANCHE_FERIE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal sursalaireTotal = sursalaireSemaine.add(sursalaireDimanche)
                .setScale(2, RoundingMode.HALF_UP);

        // Jours restants à courir jusqu'au terme prévu (calcul indemnité Cass.)
        int joursRestants = Math.max(0,
                request.dureePrevueJours() - request.dureeReellePrestationJours());

        // Indemnité de rupture anticipée : salaire horaire × heures par jour
        // estimées (7,6 h/j par défaut, ratio standard 38 h/sem) × jours restants
        // — uniquement si rupture anticipée par ETI sans motif grave
        BigDecimal indemniteRupture;
        BigDecimal heuresParJourEstimees = new BigDecimal("7.6");
        if (request.ruptureAnticipeeParEtiSansMotifGrave() && joursRestants > 0) {
            indemniteRupture = request.salaireHoraireBrut()
                    .multiply(heuresParJourEstimees)
                    .multiply(new BigDecimal(joursRestants))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            indemniteRupture = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal total = peculeVacancesInterim
                .add(primeFinAnneeSectorielle)
                .add(indemniteRupture)
                .add(sursalaireTotal)
                .setScale(2, RoundingMode.HALF_UP);

        // ── Hiérarchie 1 : rupture anticipée injustifiée (Cass. BE) ────────────
        if (request.ruptureAnticipeeParEtiSansMotifGrave() && joursRestants > 0) {
            String synthese = "Rupture anticipée de la mission par l'ETI"
                    + " sans motif grave invocable : application de"
                    + " Cass. (BE) 04/02/1991 et 16/12/2002, l'intérimaire"
                    + " a droit aux rémunérations restant à courir jusqu'au"
                    + " terme prévu (" + joursRestants + " jours × "
                    + heuresParJourEstimees + " h/j × "
                    + request.salaireHoraireBrut() + " €/h = "
                    + indemniteRupture + " €). S'y ajoutent le pécule de"
                    + " vacances intérim (" + peculeVacancesInterim
                    + " €), la prime de fin d'année sectorielle ("
                    + primeFinAnneeSectorielle + " €) et les sursalaires"
                    + " heures supplémentaires (" + sursalaireTotal
                    + " €). Total brut = " + total + " €. AUCUNE prime"
                    + " de précarité forfaitaire 10 % style FR n'est due"
                    + " en BE.";
            return new InterimBeIndemniteFinMissionResult(
                    request.dateDebutMission(),
                    request.dateFinPrevue(),
                    request.dateFinReelle(),
                    request.dureeReellePrestationJours(),
                    request.dureePrevueJours(),
                    request.salaireHoraireBrut(),
                    request.heuresPrestees(),
                    request.heuresSupplementairesSemaine(),
                    request.heuresSupplementairesDimancheFerie(),
                    request.peculeDejaVerseParFsi(),
                    request.ruptureAnticipeeParEtiSansMotifGrave(),
                    request.commissionParitaireUtilisateur(),
                    request.ancienneteSectorielleJours(),
                    InterimBeIndemniteFinMissionVerdict.RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR,
                    salaireBrutTotalPrestations,
                    peculeVacancesInterim,
                    primeFinAnneeSectorielle,
                    indemniteRupture,
                    sursalaireTotal,
                    total,
                    BigDecimal.ZERO.setScale(2),
                    tauxPrimeFinAnnee,
                    joursRestants,
                    ancienneteSuffisante,
                    "RUPTURE_ANTICIPEE_RESTE_A_COURIR",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 2 : CP utilisateur non recensée ─────────────────────────
        if (request.commissionParitaireUtilisateur()
                == InterimBeCommissionParitaire.AUTRE_NON_RECENSE) {
            String synthese = "Commission paritaire de l'utilisateur non"
                    + " recensée par l'outil V1 : la prime de fin d'année"
                    + " sectorielle ne peut pas être calculée automatiquement"
                    + " (renvoi avocat BE pour identifier la CCT sectorielle"
                    + " applicable). Les autres composantes restent calculées :"
                    + " pécule de vacances intérim = " + peculeVacancesInterim
                    + " €, sursalaire heures sup = " + sursalaireTotal
                    + " €. Total brut hors prime fin d'année = " + total
                    + " €. AUCUNE prime de précarité forfaitaire 10 % style"
                    + " FR n'est due en BE.";
            return new InterimBeIndemniteFinMissionResult(
                    request.dateDebutMission(),
                    request.dateFinPrevue(),
                    request.dateFinReelle(),
                    request.dureeReellePrestationJours(),
                    request.dureePrevueJours(),
                    request.salaireHoraireBrut(),
                    request.heuresPrestees(),
                    request.heuresSupplementairesSemaine(),
                    request.heuresSupplementairesDimancheFerie(),
                    request.peculeDejaVerseParFsi(),
                    request.ruptureAnticipeeParEtiSansMotifGrave(),
                    request.commissionParitaireUtilisateur(),
                    request.ancienneteSectorielleJours(),
                    InterimBeIndemniteFinMissionVerdict.A_ANALYSER_SECTEUR_NON_RECONNU,
                    salaireBrutTotalPrestations,
                    peculeVacancesInterim,
                    primeFinAnneeSectorielle,
                    indemniteRupture,
                    sursalaireTotal,
                    total,
                    BigDecimal.ZERO.setScale(2),
                    BigDecimal.ZERO,
                    joursRestants,
                    ancienneteSuffisante,
                    "SECTEUR_CP_NON_RECENSE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 3 : aucune indemnité due ────────────────────────────────
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            String synthese = "Aucune indemnité de fin de mission n'est due :"
                    + " fin de mission au terme prévu, pécule de vacances"
                    + " intérim déjà versé par le Fonds Social Intérimaires"
                    + " (FSI), ancienneté sectorielle insuffisante pour"
                    + " ouvrir la prime de fin d'année sectorielle ("
                    + request.ancienneteSectorielleJours() + " j < "
                    + ANCIENNETE_SECTORIELLE_MINIMALE_JOURS + " j seuil"
                    + " indicatif), aucune heure supplémentaire non payée."
                    + " Rappel : AUCUNE prime de précarité forfaitaire 10 %"
                    + " analogue à l'IFM française n'existe en droit belge"
                    + " (Cass. BE 03/05/2010 et 23/06/2003).";
            return new InterimBeIndemniteFinMissionResult(
                    request.dateDebutMission(),
                    request.dateFinPrevue(),
                    request.dateFinReelle(),
                    request.dureeReellePrestationJours(),
                    request.dureePrevueJours(),
                    request.salaireHoraireBrut(),
                    request.heuresPrestees(),
                    request.heuresSupplementairesSemaine(),
                    request.heuresSupplementairesDimancheFerie(),
                    request.peculeDejaVerseParFsi(),
                    request.ruptureAnticipeeParEtiSansMotifGrave(),
                    request.commissionParitaireUtilisateur(),
                    request.ancienneteSectorielleJours(),
                    InterimBeIndemniteFinMissionVerdict.AUCUNE_INDEMNITE_DUE,
                    salaireBrutTotalPrestations,
                    peculeVacancesInterim,
                    primeFinAnneeSectorielle,
                    indemniteRupture,
                    sursalaireTotal,
                    total,
                    BigDecimal.ZERO.setScale(2),
                    tauxPrimeFinAnnee,
                    joursRestants,
                    ancienneteSuffisante,
                    "AUCUNE_INDEMNITE_DUE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Cas standard : INDEMNITES_DUES ─────────────────────────────────────
        String synthese = "Indemnités de fin de mission intérim BE exigibles :"
                + " pécule de vacances intérim = " + peculeVacancesInterim
                + " € (taux 15,38 % AR 30/03/1967 art. 19, sauf si FSI a"
                + " déjà versé), prime de fin d'année sectorielle = "
                + primeFinAnneeSectorielle + " € (taux "
                + tauxPrimeFinAnnee.multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP)
                + " % CCT sectorielle CP "
                + libelleCp(request.commissionParitaireUtilisateur())
                + ", ancienneté "
                + (ancienneteSuffisante ? "suffisante" : "insuffisante")
                + "), sursalaire heures supplémentaires = " + sursalaireTotal
                + " € (Loi 16/03/1971 art. 29). Total brut = " + total
                + " €. Rappel : AUCUNE prime de précarité forfaitaire 10 %"
                + " analogue à l'IFM française art. L. 1251-32 n'est due en"
                + " droit belge (Cass. BE 03/05/2010 et 23/06/2003).";
        return new InterimBeIndemniteFinMissionResult(
                request.dateDebutMission(),
                request.dateFinPrevue(),
                request.dateFinReelle(),
                request.dureeReellePrestationJours(),
                request.dureePrevueJours(),
                request.salaireHoraireBrut(),
                request.heuresPrestees(),
                request.heuresSupplementairesSemaine(),
                request.heuresSupplementairesDimancheFerie(),
                request.peculeDejaVerseParFsi(),
                request.ruptureAnticipeeParEtiSansMotifGrave(),
                request.commissionParitaireUtilisateur(),
                request.ancienneteSectorielleJours(),
                InterimBeIndemniteFinMissionVerdict.INDEMNITES_DUES,
                salaireBrutTotalPrestations,
                peculeVacancesInterim,
                primeFinAnneeSectorielle,
                indemniteRupture,
                sursalaireTotal,
                total,
                BigDecimal.ZERO.setScale(2),
                tauxPrimeFinAnnee,
                joursRestants,
                ancienneteSuffisante,
                "INDEMNITES_DUES",
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    /**
     * Taux prime fin d'année conventionnel par défaut selon la CP de
     * l'utilisateur. Les taux sont indicatifs et doivent être confirmés
     * cas par cas via la CCT sectorielle en vigueur à la date de la
     * mission.
     */
    static BigDecimal tauxPrimeFinAnneePourCp(InterimBeCommissionParitaire cp) {
        return switch (cp) {
            // CP 124 construction — 9,12 % CCT 12/06/2014 AR 11/02/2015
            case CP_124_CONSTRUCTION -> new BigDecimal("0.0912");
            // CP 200 employés — 1/12 = 8,33 % CCT 09/06/2016 AR 13/06/2017
            case CP_200_AUXILIAIRE_EMPLOYES -> new BigDecimal("0.0833");
            // CP 218 non-marchand — taux moyen 8,33 % (variable sous-CP)
            case CP_218_NON_MARCHAND -> new BigDecimal("0.0833");
            // CP 322 intérim — pas de prime propre (renvoi CP utilisateur)
            case CP_322_INTERIM -> BigDecimal.ZERO;
            // CP 140 transport — 8,33 % CCT 27/01/2011 AR 30/03/2011
            case CP_140_TRANSPORT_LOGISTIQUE -> new BigDecimal("0.0833");
            // CP 209 métaux — taux conventionnel 8,33 % par défaut
            case CP_209_METAUX_FABRICATIONS_METALLIQUES -> new BigDecimal("0.0833");
            // Autre — pas de calcul automatique
            case AUTRE_NON_RECENSE -> BigDecimal.ZERO;
        };
    }

    private static String libelleCp(InterimBeCommissionParitaire cp) {
        return switch (cp) {
            case CP_124_CONSTRUCTION -> "124 construction";
            case CP_200_AUXILIAIRE_EMPLOYES -> "200 employés";
            case CP_218_NON_MARCHAND -> "218 non-marchand";
            case CP_322_INTERIM -> "322 intérim";
            case CP_140_TRANSPORT_LOGISTIQUE -> "140 transport/logistique";
            case CP_209_METAUX_FABRICATIONS_METALLIQUES -> "209 métaux";
            case AUTRE_NON_RECENSE -> "non recensée";
        };
    }

    private static void validateInputs(InterimBeIndemniteFinMissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateDebutMission() == null) {
            throw new IllegalArgumentException(
                    "dateDebutMission est requise");
        }
        if (request.dateFinPrevue() == null) {
            throw new IllegalArgumentException(
                    "dateFinPrevue est requise");
        }
        if (request.dateFinReelle() == null) {
            throw new IllegalArgumentException(
                    "dateFinReelle est requise");
        }
        if (request.dureeReellePrestationJours() == null) {
            throw new IllegalArgumentException(
                    "dureeReellePrestationJours est requise");
        }
        if (request.dureeReellePrestationJours() < 0) {
            throw new IllegalArgumentException(
                    "dureeReellePrestationJours doit être positive ou nulle");
        }
        if (request.dureePrevueJours() == null) {
            throw new IllegalArgumentException(
                    "dureePrevueJours est requise");
        }
        if (request.dureePrevueJours() <= 0) {
            throw new IllegalArgumentException(
                    "dureePrevueJours doit être strictement positive");
        }
        if (request.salaireHoraireBrut() == null) {
            throw new IllegalArgumentException(
                    "salaireHoraireBrut est requis");
        }
        if (request.salaireHoraireBrut().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "salaireHoraireBrut doit être strictement positif");
        }
        if (request.heuresPrestees() == null) {
            throw new IllegalArgumentException("heuresPrestees est requis");
        }
        if (request.heuresPrestees().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "heuresPrestees doit être positif ou nul");
        }
        if (request.heuresSupplementairesSemaine() == null) {
            throw new IllegalArgumentException(
                    "heuresSupplementairesSemaine est requis");
        }
        if (request.heuresSupplementairesSemaine()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "heuresSupplementairesSemaine doit être positif ou nul");
        }
        if (request.heuresSupplementairesDimancheFerie() == null) {
            throw new IllegalArgumentException(
                    "heuresSupplementairesDimancheFerie est requis");
        }
        if (request.heuresSupplementairesDimancheFerie()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "heuresSupplementairesDimancheFerie doit être positif ou nul");
        }
        if (request.peculeDejaVerseParFsi() == null) {
            throw new IllegalArgumentException(
                    "peculeDejaVerseParFsi est requis");
        }
        if (request.ruptureAnticipeeParEtiSansMotifGrave() == null) {
            throw new IllegalArgumentException(
                    "ruptureAnticipeeParEtiSansMotifGrave est requis");
        }
        if (request.commissionParitaireUtilisateur() == null) {
            throw new IllegalArgumentException(
                    "commissionParitaireUtilisateur est requise");
        }
        if (request.ancienneteSectorielleJours() == null) {
            throw new IllegalArgumentException(
                    "ancienneteSectorielleJours est requise");
        }
        if (request.ancienneteSectorielleJours() < 0) {
            throw new IllegalArgumentException(
                    "ancienneteSectorielleJours doit être positif ou nul");
        }
    }
}
