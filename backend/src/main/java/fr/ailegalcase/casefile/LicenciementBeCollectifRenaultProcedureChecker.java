package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-07 : checklist procédurale licenciement collectif BE — Loi du
 * 13/02/1998 (loi Renault) + CCT n° 24 + CCT n° 39 — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>L'outil ne calcule pas d'indemnité : il vérifie la conformité
 * <i>procédurale</i> du licenciement collectif et identifie le verdict
 * applicable (CONFORME / non-conforme sur 4 axes / non-applicable).</p>
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 13/02/1998</b> portant des dispositions en faveur de
 *       l'emploi (chapitre VII : licenciements collectifs, dite « loi
 *       Renault » suite à la fermeture de Renault Vilvorde en 1997).</li>
 *   <li><b>CCT n° 24</b> du 02/10/1975 du Conseil National du Travail
 *       — procédure d'information et de consultation des représentants
 *       des travailleurs en cas de licenciement collectif.</li>
 *   <li><b>CCT n° 39</b> du 13/12/1983 du Conseil National du Travail
 *       — information et consultation sur les conséquences sociales de
 *       l'introduction de nouvelles technologies (mesures sociales
 *       d'accompagnement).</li>
 *   <li><b>Directive 98/59/CE</b> du Conseil du 20/07/1998 concernant le
 *       rapprochement des législations des États membres relatives aux
 *       licenciements collectifs.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li>{@code NON_APPLICABLE_SEUIL} — court-circuite tout si le seuil de
 *       déclenchement n'est pas atteint (Loi 13/02/1998 art. 2).</li>
 *   <li>{@code NON_CONFORME_PHASE_INFORMATION_INCOMPLETE} — la phase 1
 *       d'information écrite préalable est incomplète (l'employeur n'a
 *       pas notifié au CE/CPPT/DS, ou n'a pas communiqué les documents
 *       légaux : motifs, catégories, critères de sélection).</li>
 *   <li>{@code NON_CONFORME_CONSULTATION_INSUFFISANTE} — la phase 2 n'a
 *       pas permis un débat réel : pas de réunion organisée, pas de
 *       réponse motivée de l'employeur aux questions / contre-propositions.</li>
 *   <li>{@code NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE} — la
 *       notification écrite à l'autorité régionale de l'emploi (Forem
 *       / Actiris / VDAB) n'a pas été effectuée, ou la date est
 *       manquante.</li>
 *   <li>{@code NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE} — préavis
 *       individuel notifié avant l'expiration du délai d'attente de
 *       30 jours suivant la notification à l'autorité.</li>
 *   <li>Sinon : {@code CONFORME} (les 3 phases sont validées et le délai
 *       d'attente est échu — l'employeur peut notifier les préavis sans
 *       risque procédural sur la loi Renault).</li>
 * </ol>
 *
 * <h2>Sanctions du non-respect (Loi 13/02/1998 art. 67)</h2>
 * <p>Les travailleurs licenciés en violation de la procédure (notamment
 * de l'obligation d'information / consultation) bénéficient d'une
 * indemnité spéciale de protection — calculée par d'autres outils
 * (préavis BE) — et peuvent demander la nullité de leur préavis
 * (réintégration). L'outil <i>ne calcule pas</i> ces sanctions ; il
 * identifie uniquement les manquements qui les déclenchent.</p>
 */
public final class LicenciementBeCollectifRenaultProcedureChecker {

    static final String BASE_JURIDIQUE =
            "Loi du 13/02/1998 portant des dispositions en faveur de l'emploi"
                    + " (chapitre VII : licenciements collectifs, dite \"loi"
                    + " Renault\") ; CCT n° 24 du 02/10/1975 du CNT (procédure"
                    + " information / consultation) ; CCT n° 39 du 13/12/1983 du"
                    + " CNT (mesures sociales d'accompagnement) ; Directive"
                    + " 98/59/CE du 20/07/1998. Délai d'attente 30 jours après"
                    + " notification autorité régionale — sanction art. 67 :"
                    + " indemnité spéciale de protection + nullité possible du"
                    + " préavis.";

    static final String AVERT_PROCEDURAL =
            "Cet outil est une checklist procédurale, pas un calcul"
                    + " d'indemnité. Le verdict CONFORME n'écarte pas les"
                    + " contestations individuelles (motif réel et sérieux,"
                    + " critères de sélection, ordre des licenciements). En cas"
                    + " de doute factuel sur l'effectivité de la consultation,"
                    + " consulter le PV de réunion et la délégation syndicale"
                    + " avant tout préavis. Hors scope : PSE français — la"
                    + " procédure DIRECCTE / DDETS est radicalement différente.";

    /** Délai d'attente minimum après notification autorité (Loi 13/02/1998 art. 66). */
    static final int DELAI_ATTENTE_JOURS = 30;

    /** Effectif minimum pour qu'une entreprise soit soumise à la loi Renault. */
    static final int EFFECTIF_MINIMAL_LOI_RENAULT = 20;

    /** Seuil PME (20-99 travailleurs) — 10 licenciements sur 60 jours. */
    static final int SEUIL_PME = 10;

    /** Seuil très grande entreprise (≥ 300 travailleurs) — 30 licenciements / 60 j. */
    static final int SEUIL_TRES_GRANDE = 30;

    private LicenciementBeCollectifRenaultProcedureChecker() {
    }

    public static LicenciementBeCollectifRenaultResult analyze(
            LicenciementBeCollectifRenaultRequest request) {
        validateInputs(request);

        int seuilLegal = seuilLegalPour(request.tailleEntreprise(),
                request.effectifMoyen());

        // 1. Vérification du seuil de déclenchement (court-circuite tout).
        if (request.tailleEntreprise()
                == LicenciementBeCollectifRenaultTailleEntreprise.PETITE_MOINS_20
                || request.effectifMoyen() < EFFECTIF_MINIMAL_LOI_RENAULT
                || request.nombreLicenciementsEnvisages() < seuilLegal) {
            return nonApplicableSeuil(request, seuilLegal);
        }

        // 2. Phase 1 — information écrite préalable.
        boolean infoComplete = Boolean.TRUE.equals(request.informationEcriteCePrealable())
                && Boolean.TRUE.equals(request.documentsLegauxCommuniques());
        if (!infoComplete) {
            return nonConforme(request, seuilLegal,
                    LicenciementBeCollectifRenaultVerdict.NON_CONFORME_PHASE_INFORMATION_INCOMPLETE,
                    "PHASE_INFORMATION_INCOMPLETE",
                    "Phase 1 (information préalable du CE / CPPT / délégation"
                            + " syndicale) incomplète : "
                            + (!Boolean.TRUE.equals(request.informationEcriteCePrealable())
                                    ? "notification écrite préalable manquante"
                                    : "documents légaux non communiqués (motifs"
                                    + " économiques, catégories, critères de sélection)")
                            + ". Sanction Loi 13/02/1998 art. 67 : indemnité"
                            + " spéciale de protection + nullité possible des"
                            + " préavis.");
        }

        // 3. Phase 2 — consultation effective + réponses motivées.
        boolean consultationComplete = Boolean.TRUE.equals(request.consultationEffectiveTenue())
                && Boolean.TRUE.equals(request.reponsesMotiveesEmployeur());
        if (!consultationComplete) {
            return nonConforme(request, seuilLegal,
                    LicenciementBeCollectifRenaultVerdict.NON_CONFORME_CONSULTATION_INSUFFISANTE,
                    "CONSULTATION_INSUFFISANTE",
                    "Phase 2 (consultation effective) insuffisante : "
                            + (!Boolean.TRUE.equals(request.consultationEffectiveTenue())
                                    ? "aucune réunion de consultation tenue"
                                    : "aucune réponse motivée écrite de"
                                    + " l'employeur aux questions /"
                                    + " contre-propositions des représentants")
                            + ". La consultation doit permettre un débat réel"
                            + " (CCT n° 24 art. 7) — sanction art. 67.");
        }

        // 4. Phase 3 — notification à l'autorité régionale.
        if (!Boolean.TRUE.equals(request.notificationAutoriteRegionaleFaite())
                || request.dateNotificationAutorite() == null) {
            return nonConforme(request, seuilLegal,
                    LicenciementBeCollectifRenaultVerdict.NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE,
                    "NOTIFICATION_AUTORITE_MANQUANTE",
                    "Phase 3 (notification écrite à l'autorité régionale de"
                            + " l'emploi — Forem / Actiris / VDAB selon la"
                            + " région) manquante ou date non renseignée. Le"
                            + " délai d'attente de 30 jours ne commence pas à"
                            + " courir tant que la notification n'est pas faite"
                            + " — tout préavis notifié est nul (Loi 13/02/1998"
                            + " art. 66).");
        }

        LocalDate dateFinDelai = request.dateNotificationAutorite()
                .plusDays(DELAI_ATTENTE_JOURS);

        // 5. Délai d'attente 30 jours — vérifié si un préavis est déjà notifié.
        boolean delaiRespecte = true;
        if (request.datePremierPreavisNotifie() != null) {
            // Un préavis notifié strictement avant la fin du délai = violation.
            delaiRespecte = !request.datePremierPreavisNotifie().isBefore(dateFinDelai);
            if (!delaiRespecte) {
                return new LicenciementBeCollectifRenaultResult(
                        request.dateProjet(),
                        request.tailleEntreprise(),
                        request.effectifMoyen(),
                        request.nombreLicenciementsEnvisages(),
                        request.phaseAtteinte(),
                        request.informationEcriteCePrealable(),
                        request.documentsLegauxCommuniques(),
                        request.consultationEffectiveTenue(),
                        request.reponsesMotiveesEmployeur(),
                        request.notificationAutoriteRegionaleFaite(),
                        request.dateNotificationAutorite(),
                        request.datePremierPreavisNotifie(),
                        LicenciementBeCollectifRenaultVerdict.NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE,
                        false,
                        true,
                        seuilLegal,
                        dateFinDelai,
                        false,
                        "DELAI_ATTENTE_NON_RESPECTE",
                        "Préavis individuel notifié le "
                                + request.datePremierPreavisNotifie()
                                + " avant l'expiration du délai d'attente"
                                + " (fin : " + dateFinDelai + "). Sanction Loi"
                                + " 13/02/1998 art. 66 : préavis nul +"
                                + " indemnité spéciale de protection art. 67"
                                + " (équivalente à la rémunération restant"
                                + " due jusqu'à expiration du délai +"
                                + " indemnité forfaitaire complémentaire).",
                        BASE_JURIDIQUE,
                        AVERT_PROCEDURAL);
            }
        }

        // 6. Toutes les phases sont conformes.
        return new LicenciementBeCollectifRenaultResult(
                request.dateProjet(),
                request.tailleEntreprise(),
                request.effectifMoyen(),
                request.nombreLicenciementsEnvisages(),
                request.phaseAtteinte(),
                request.informationEcriteCePrealable(),
                request.documentsLegauxCommuniques(),
                request.consultationEffectiveTenue(),
                request.reponsesMotiveesEmployeur(),
                request.notificationAutoriteRegionaleFaite(),
                request.dateNotificationAutorite(),
                request.datePremierPreavisNotifie(),
                LicenciementBeCollectifRenaultVerdict.CONFORME,
                true,
                true,
                seuilLegal,
                dateFinDelai,
                true,
                "PROCEDURE_CONFORME",
                "Procédure Renault conforme : 3 phases (information,"
                        + " consultation, décision + notification autorité)"
                        + " validées + délai d'attente 30 jours échu le "
                        + dateFinDelai + ". Les préavis individuels peuvent"
                        + " être notifiés sans risque de nullité procédurale"
                        + " (contestations individuelles motif réel et"
                        + " sérieux / critères de sélection restent"
                        + " possibles).",
                BASE_JURIDIQUE,
                AVERT_PROCEDURAL);
    }

    // ── Calcul du seuil légal applicable selon la taille ──────────────────

    static int seuilLegalPour(LicenciementBeCollectifRenaultTailleEntreprise taille,
                              int effectifMoyen) {
        return switch (taille) {
            case PETITE_MOINS_20 -> 0; // pas applicable
            case PME_20_99 -> SEUIL_PME;
            // 10 % de l'effectif, arrondi au supérieur ; min 10 (pour 100 : 10 % = 10)
            case GRANDE_100_299 -> Math.max(SEUIL_PME,
                    (int) Math.ceil(effectifMoyen * 0.10));
            case TRES_GRANDE_300_PLUS -> SEUIL_TRES_GRANDE;
        };
    }

    // ── Construction du résultat NON_APPLICABLE_SEUIL ─────────────────────

    private static LicenciementBeCollectifRenaultResult nonApplicableSeuil(
            LicenciementBeCollectifRenaultRequest request, int seuilLegal) {
        return new LicenciementBeCollectifRenaultResult(
                request.dateProjet(),
                request.tailleEntreprise(),
                request.effectifMoyen(),
                request.nombreLicenciementsEnvisages(),
                request.phaseAtteinte(),
                request.informationEcriteCePrealable(),
                request.documentsLegauxCommuniques(),
                request.consultationEffectiveTenue(),
                request.reponsesMotiveesEmployeur(),
                request.notificationAutoriteRegionaleFaite(),
                request.dateNotificationAutorite(),
                request.datePremierPreavisNotifie(),
                LicenciementBeCollectifRenaultVerdict.NON_APPLICABLE_SEUIL,
                false,
                false,
                seuilLegal,
                null,
                false,
                "SEUIL_NON_ATTEINT",
                "La Loi Renault ne s'applique pas : seuil de déclenchement"
                        + " non atteint (entreprise " + request.effectifMoyen()
                        + " ETP, " + request.nombreLicenciementsEnvisages()
                        + " licenciements envisagés vs seuil légal " + seuilLegal
                        + " sur 60 jours). La procédure d'information générale"
                        + " (CCT n° 9 du CNT) peut subsister selon la situation"
                        + " — non couverte par cet outil.",
                BASE_JURIDIQUE,
                AVERT_PROCEDURAL);
    }

    // ── Construction d'un résultat NON_CONFORME (phases 1, 2 ou 3) ────────

    private static LicenciementBeCollectifRenaultResult nonConforme(
            LicenciementBeCollectifRenaultRequest request,
            int seuilLegal,
            LicenciementBeCollectifRenaultVerdict verdict,
            String raison, String synthese) {
        LocalDate dateFinDelai = (request.dateNotificationAutorite() != null)
                ? request.dateNotificationAutorite().plusDays(DELAI_ATTENTE_JOURS)
                : null;
        return new LicenciementBeCollectifRenaultResult(
                request.dateProjet(),
                request.tailleEntreprise(),
                request.effectifMoyen(),
                request.nombreLicenciementsEnvisages(),
                request.phaseAtteinte(),
                request.informationEcriteCePrealable(),
                request.documentsLegauxCommuniques(),
                request.consultationEffectiveTenue(),
                request.reponsesMotiveesEmployeur(),
                request.notificationAutoriteRegionaleFaite(),
                request.dateNotificationAutorite(),
                request.datePremierPreavisNotifie(),
                verdict,
                false,
                true,
                seuilLegal,
                dateFinDelai,
                false,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_PROCEDURAL);
    }

    // ── Validation conditionnelle ─────────────────────────────────────────

    private static void validateInputs(LicenciementBeCollectifRenaultRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateProjet() == null) {
            throw new IllegalArgumentException("dateProjet est requise");
        }
        if (request.tailleEntreprise() == null) {
            throw new IllegalArgumentException("tailleEntreprise est requise");
        }
        if (request.effectifMoyen() == null || request.effectifMoyen() < 0) {
            throw new IllegalArgumentException("effectifMoyen requis et ≥ 0");
        }
        if (request.nombreLicenciementsEnvisages() == null
                || request.nombreLicenciementsEnvisages() < 0) {
            throw new IllegalArgumentException(
                    "nombreLicenciementsEnvisages requis et ≥ 0");
        }
        if (request.phaseAtteinte() == null) {
            throw new IllegalArgumentException("phaseAtteinte est requise");
        }
        if (request.informationEcriteCePrealable() == null) {
            throw new IllegalArgumentException(
                    "informationEcriteCePrealable est requis");
        }
        if (request.documentsLegauxCommuniques() == null) {
            throw new IllegalArgumentException(
                    "documentsLegauxCommuniques est requis");
        }
        if (request.consultationEffectiveTenue() == null) {
            throw new IllegalArgumentException(
                    "consultationEffectiveTenue est requis");
        }
        if (request.reponsesMotiveesEmployeur() == null) {
            throw new IllegalArgumentException(
                    "reponsesMotiveesEmployeur est requis");
        }
        if (request.notificationAutoriteRegionaleFaite() == null) {
            throw new IllegalArgumentException(
                    "notificationAutoriteRegionaleFaite est requis");
        }
        if (Boolean.TRUE.equals(request.notificationAutoriteRegionaleFaite())
                && request.dateNotificationAutorite() == null) {
            throw new IllegalArgumentException(
                    "dateNotificationAutorite requise si notification faite");
        }
        if (request.dateNotificationAutorite() != null
                && request.dateNotificationAutorite().isBefore(request.dateProjet())) {
            throw new IllegalArgumentException(
                    "dateNotificationAutorite ne peut pas précéder dateProjet");
        }
        if (request.datePremierPreavisNotifie() != null
                && request.dateNotificationAutorite() != null
                && request.datePremierPreavisNotifie()
                        .isBefore(request.dateNotificationAutorite())) {
            throw new IllegalArgumentException(
                    "datePremierPreavisNotifie ne peut pas précéder"
                            + " dateNotificationAutorite");
        }
    }
}
