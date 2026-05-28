package fr.ailegalcase.casefile;

/**
 * SF-219-08 : vérificateur de conformité du <i>transfert d'entreprise
 * CCT n° 32bis</i> — fonction <b>pure</b> (sans dépendance HTTP /
 * persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>CCT n° 32bis du 07/06/1985</b> conclue au Conseil National du
 *       Travail — transposition de la Directive 2001/23/CE concernant le
 *       maintien des droits des travailleurs en cas de transfert
 *       d'entreprise par convention.</li>
 *   <li><b>Loi du 17/03/1965</b> portant approbation de la convention
 *       collective de travail conclue le 24/05/1960 — sanctions pénales
 *       en cas de non-respect de l'information-consultation préalable.</li>
 *   <li><b>Directive 2001/23/CE</b> du 12/03/2001 (consolidée) —
 *       maintien des droits des travailleurs en cas de transfert
 *       d'entreprises, d'établissements ou de parties d'entreprises ou
 *       d'établissements.</li>
 *   <li><b>CCT n° 9</b> et <b>CCT n° 9bis</b> CNT — information /
 *       consultation des travailleurs sur les décisions économiques.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>INELIGIBLE_TYPE_OPERATION</b> — opération non qualifiante
 *       (faillite, liquidation judiciaire, cession d'actions, intra-groupe
 *       sans cession). Sortie immédiate.</li>
 *   <li><b>INELIGIBLE_PAS_ENTITE_ECONOMIQUE</b> — pas d'entité économique
 *       préservée (cession isolée d'actifs). La CCT 32bis ne s'applique
 *       pas (jp Süzen, Abler).</li>
 *   <li><b>A_ANALYSER</b> — identité économique ambiguë (état
 *       {@link TransfertEntrepriseCct32bisIdentiteEconomique#A_ANALYSER}) :
 *       qualification juridique pointue requise (avocat BE).</li>
 *   <li>Sinon : qualifiant. Vérifie la procédure (info-consult préalable)
 *       et les droits maintenus :
 *       <ul>
 *         <li>Procédure non respectée → {@link
 *             TransfertEntrepriseCct32bisVerdict#TRANSFERT_NON_CONFORME_INFO_CONSULT}.</li>
 *         <li>Procédure OK → {@link
 *             TransfertEntrepriseCct32bisVerdict#TRANSFERT_CONFORME}.</li>
 *       </ul>
 *       Dans les 2 cas, le maintien des contrats est inchangé (relève de
 *       l'ordre public — art. 7 CCT 32bis).</li>
 * </ol>
 *
 * <h2>Responsabilité solidaire cédant / cessionnaire</h2>
 * <p>L'art. 3 de la Directive 2001/23/CE prévoit une responsabilité
 * solidaire du cédant et du cessionnaire pour les dettes salariales
 * antérieures au transfert, pendant un délai d'<b>1 an</b> à compter
 * de la date de transfert. Le calcul de cette échéance est restitué
 * dans le verdict (champ {@code dateFinResponsabiliteSolidaire}).</p>
 */
public final class TransfertEntrepriseCct32bisChecker {

    static final String BASE_JURIDIQUE =
            "CCT n° 32bis du 07/06/1985 conclue au CNT (transfert conventionnel"
                    + " d'entreprise) ; Loi du 17/03/1965 (sanctions pénales"
                    + " information-consultation) ; Directive 2001/23/CE du"
                    + " 12/03/2001 (maintien des droits des travailleurs) ;"
                    + " CCT n° 9 et n° 9bis (information-consultation CE / DS"
                    + " / CPPT) ; jp CJUE Süzen 11/03/1997, Abler 20/11/2003 ;"
                    + " Cass. BE 17/05/1999, 14/02/2011.";

    static final String AVERT_QUALIFICATION_AVOCAT =
            "La qualification d'un transfert au sens de la CCT 32bis est"
                    + " hautement contextuelle (préservation d'identité"
                    + " économique, intentions des parties, modalités de"
                    + " l'opération). Toute conclusion doit être confirmée"
                    + " par un avocat spécialisé en droit du travail BE."
                    + " La responsabilité solidaire 1 an (Directive 2001/23/CE"
                    + " art. 3) couvre les dettes salariales antérieures au"
                    + " transfert — son périmètre exact dépend de la"
                    + " jurisprudence applicable.";

    /** Durée légale de la responsabilité solidaire cédant/cessionnaire (Directive 2001/23/CE art. 3). */
    static final int DUREE_RESPONSABILITE_SOLIDAIRE_ANNEES = 1;

    private TransfertEntrepriseCct32bisChecker() {
    }

    public static TransfertEntrepriseCct32bisResult analyze(
            TransfertEntrepriseCct32bisRequest request) {
        validateInputs(request);

        // ── Hiérarchie 1 : type d'opération non qualifiant ─────────────────
        if (!estTypeQualifiant(request.typeOperation())) {
            return ineligible(request,
                    TransfertEntrepriseCct32bisVerdict.INELIGIBLE_TYPE_OPERATION,
                    "TYPE_OPERATION_NON_QUALIFIANT",
                    "L'opération déclarée (" + libelleType(request.typeOperation())
                            + ") n'entre pas dans le champ de la CCT n° 32bis"
                            + " — pas de maintien automatique des contrats au"
                            + " titre de ce régime. " + suggestionRegimeAlternatif(
                            request.typeOperation()) + " Toute conclusion doit"
                            + " être validée par un avocat BE.");
        }

        // ── Hiérarchie 2 : identité économique non préservée ────────────────
        if (request.identiteEconomique()
                == TransfertEntrepriseCct32bisIdentiteEconomique.NON_PRESERVEE) {
            return ineligible(request,
                    TransfertEntrepriseCct32bisVerdict.INELIGIBLE_PAS_ENTITE_ECONOMIQUE,
                    "PAS_ENTITE_ECONOMIQUE_PRESERVEE",
                    "L'identité économique de l'entité transférée n'est pas"
                            + " préservée après l'opération (simple cession"
                            + " d'actifs, externalisation sans transfert de"
                            + " moyens essentiels). La CCT n° 32bis ne s'applique"
                            + " pas (jp CJUE Süzen, Abler ; Cass. BE 17/05/1999).");
        }

        // ── Hiérarchie 3 : ambiguïté → A_ANALYSER ─────────────────────────
        if (request.identiteEconomique()
                == TransfertEntrepriseCct32bisIdentiteEconomique.A_ANALYSER) {
            return ineligible(request,
                    TransfertEntrepriseCct32bisVerdict.A_ANALYSER,
                    "IDENTITE_ECONOMIQUE_A_ANALYSER",
                    "La qualification de la préservation de l'identité"
                            + " économique est ambiguë (continuité partielle"
                            + " d'activité, reprise hétérogène des moyens)."
                            + " Une analyse juridique pointue est requise"
                            + " (avocat spécialisé BE) — la CCT 32bis"
                            + " pourrait s'appliquer ou non selon le faisceau"
                            + " d'indices retenu (jp CJUE Süzen, Abler).");
        }

        // ── Cas qualifiant : analyse procédurale ──────────────────────────
        java.time.LocalDate finSolidarite = request.dateTransfert()
                .plusYears(DUREE_RESPONSABILITE_SOLIDAIRE_ANNEES);

        // Procédure info-consult : obligation cumulative
        boolean procedureConforme = request.infoConsultPrealableRespectee()
                && request.conseilEntrepriseConsulte();

        // Droits maintenus : obligation d'ordre public (vérification factuelle)
        boolean droitsMaintenus = request.maintienContratsAutomatique()
                && request.maintienAncienneteRespecte()
                && request.maintienRemunerationRespecte()
                && request.conventionsCollectivesMaintenues();

        TransfertEntrepriseCct32bisVerdict verdict;
        String raison;
        String synthese;

        if (procedureConforme && droitsMaintenus) {
            verdict = TransfertEntrepriseCct32bisVerdict.TRANSFERT_CONFORME;
            raison = "TRANSFERT_CONFORME";
            synthese = "Transfert conventionnel qualifiant CCT n° 32bis (opération "
                    + libelleType(request.typeOperation()) + " avec préservation"
                    + " d'identité économique). Procédure d'information-consultation"
                    + " préalable respectée. Maintien automatique des contrats"
                    + " (ancienneté, rémunération, avantages) et des conventions"
                    + " collectives. Responsabilité solidaire cédant/cessionnaire"
                    + " jusqu'au " + finSolidarite + " (1 an art. 3 Directive"
                    + " 2001/23/CE).";
        } else if (!procedureConforme) {
            verdict = TransfertEntrepriseCct32bisVerdict.TRANSFERT_NON_CONFORME_INFO_CONSULT;
            raison = "INFO_CONSULT_PREALABLE_NON_RESPECTEE";
            synthese = "Transfert conventionnel qualifiant CCT n° 32bis mais"
                    + " procédure d'information-consultation préalable des"
                    + " représentants des travailleurs (CE / DS / CPPT) non"
                    + " respectée. Risque de sanction pénale (Loi 17/03/1965)"
                    + " + dommages-intérêts individuels. Le maintien des"
                    + " contrats demeure d'ordre public (inchangé)."
                    + droitsMaintenusSuffix(droitsMaintenus, finSolidarite);
        } else {
            // Procédure OK mais droits non maintenus dans les faits déclarés
            verdict = TransfertEntrepriseCct32bisVerdict.TRANSFERT_CONFORME;
            raison = "TRANSFERT_CONFORME_AVEC_RESERVES_MAINTIEN";
            synthese = "Transfert conventionnel qualifiant CCT n° 32bis,"
                    + " procédure d'information-consultation respectée. Au moins"
                    + " un droit (ancienneté / rémunération / contrat / CCT)"
                    + " déclaré non maintenu par le cessionnaire — situation"
                    + " contraire à l'ordre public (art. 7 CCT 32bis) ouvrant"
                    + " une action en réintégration des droits + dommages-intérêts."
                    + " Responsabilité solidaire jusqu'au " + finSolidarite + ".";
        }

        return new TransfertEntrepriseCct32bisResult(
                request.dateTransfert(),
                request.typeOperation(),
                request.identiteEconomique(),
                request.infoConsultPrealableRespectee(),
                request.conseilEntrepriseConsulte(),
                request.maintienContratsAutomatique(),
                request.maintienAncienneteRespecte(),
                request.maintienRemunerationRespecte(),
                request.conventionsCollectivesMaintenues(),
                verdict,
                true,
                procedureConforme,
                droitsMaintenus,
                finSolidarite,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_QUALIFICATION_AVOCAT);
    }

    // ── Construction d'un résultat inéligible ──────────────────────────────

    private static TransfertEntrepriseCct32bisResult ineligible(
            TransfertEntrepriseCct32bisRequest request,
            TransfertEntrepriseCct32bisVerdict verdict,
            String raison, String synthese) {
        return new TransfertEntrepriseCct32bisResult(
                request.dateTransfert(),
                request.typeOperation(),
                request.identiteEconomique(),
                request.infoConsultPrealableRespectee(),
                request.conseilEntrepriseConsulte(),
                request.maintienContratsAutomatique(),
                request.maintienAncienneteRespecte(),
                request.maintienRemunerationRespecte(),
                request.conventionsCollectivesMaintenues(),
                verdict,
                false,
                false,
                false,
                null,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_QUALIFICATION_AVOCAT);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static boolean estTypeQualifiant(
            TransfertEntrepriseCct32bisTypeOperation type) {
        return switch (type) {
            case VENTE_FONDS_COMMERCE, FUSION_ABSORPTION, SCISSION,
                 APPORT_UNIVERSALITE, DEMEMBREMENT -> true;
            case FAILLITE, LIQUIDATION_JUDICIAIRE, CESSION_ACTIONS,
                 TRANSFERT_INTRA_GROUPE_SANS_CESSION -> false;
        };
    }

    private static String libelleType(
            TransfertEntrepriseCct32bisTypeOperation type) {
        return switch (type) {
            case VENTE_FONDS_COMMERCE -> "vente de fonds de commerce";
            case FUSION_ABSORPTION -> "fusion par absorption";
            case SCISSION -> "scission";
            case APPORT_UNIVERSALITE -> "apport d'universalité ou de branche d'activité";
            case DEMEMBREMENT -> "démembrement / réorganisation conventionnelle";
            case FAILLITE -> "faillite judiciaire";
            case LIQUIDATION_JUDICIAIRE -> "liquidation judiciaire";
            case CESSION_ACTIONS -> "cession d'actions / parts";
            case TRANSFERT_INTRA_GROUPE_SANS_CESSION -> "transfert intra-groupe sans cession";
        };
    }

    private static String suggestionRegimeAlternatif(
            TransfertEntrepriseCct32bisTypeOperation type) {
        return switch (type) {
            case FAILLITE, LIQUIDATION_JUDICIAIRE ->
                    "Vérifier l'applicabilité de la CCT n° 102 (transfert dans le"
                            + " cadre d'une procédure de réorganisation judiciaire)"
                            + " ou du régime FFE (Loi 26/06/2002).";
            case CESSION_ACTIONS ->
                    "La personne morale employeur ne change pas — pas de transfert"
                            + " au sens CCT 32bis. Les contrats individuels sont"
                            + " maintenus de plein droit par l'absence de"
                            + " substitution d'employeur.";
            case TRANSFERT_INTRA_GROUPE_SANS_CESSION ->
                    "Vérifier si la réorganisation constitue une véritable cession"
                            + " (changement d'employeur réel) — sinon pas de CCT"
                            + " 32bis.";
            default -> "Vérifier l'applicabilité d'un autre régime BE pertinent.";
        };
    }

    private static String droitsMaintenusSuffix(
            boolean droitsMaintenus, java.time.LocalDate fin) {
        return droitsMaintenus
                ? " Droits individuels effectivement maintenus selon les"
                + " déclarations. Responsabilité solidaire cédant/cessionnaire"
                + " jusqu'au " + fin + "."
                : " De surcroît, au moins un droit déclaré non maintenu —"
                + " action complémentaire en réintégration possible.";
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(TransfertEntrepriseCct32bisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateTransfert() == null) {
            throw new IllegalArgumentException("dateTransfert est requise");
        }
        if (request.typeOperation() == null) {
            throw new IllegalArgumentException("typeOperation est requise");
        }
        if (request.identiteEconomique() == null) {
            throw new IllegalArgumentException("identiteEconomique est requise");
        }
        if (request.infoConsultPrealableRespectee() == null) {
            throw new IllegalArgumentException(
                    "infoConsultPrealableRespectee est requise");
        }
        if (request.conseilEntrepriseConsulte() == null) {
            throw new IllegalArgumentException(
                    "conseilEntrepriseConsulte est requise");
        }
        if (request.maintienContratsAutomatique() == null) {
            throw new IllegalArgumentException(
                    "maintienContratsAutomatique est requise");
        }
        if (request.maintienAncienneteRespecte() == null) {
            throw new IllegalArgumentException(
                    "maintienAncienneteRespecte est requise");
        }
        if (request.maintienRemunerationRespecte() == null) {
            throw new IllegalArgumentException(
                    "maintienRemunerationRespecte est requise");
        }
        if (request.conventionsCollectivesMaintenues() == null) {
            throw new IllegalArgumentException(
                    "conventionsCollectivesMaintenues est requise");
        }
    }
}
