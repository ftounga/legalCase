package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * SF-219-23 : vérificateur de la situation <i>refus d'aménagements
 * raisonnables handicap BE</i> (Loi du 10/05/2007 tendant à lutter
 * contre certaines formes de discrimination M.B. 30/05/2007 + CCT
 * n° 95 du 10/10/2008 du CNT NAR + Directive 2000/78/CE art. 5 +
 * Convention ONU 13/12/2006 art. 27 + Convention OIT n° 159) —
 * fonction <b>pure</b> (sans dépendance HTTP / persistance /
 * horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 10/05/2007</b> tendant à lutter contre certaines
 *       formes de discrimination (M.B. 30/05/2007) — art. 4 4°
 *       définition du handicap (limitation durable et substantielle),
 *       art. 4 9° définition de l'aménagement raisonnable, art. 14
 *       obligation d'aménagement raisonnable au bénéfice des personnes
 *       en situation de handicap, le refus d'aménagement raisonnable
 *       constituant une discrimination indirecte fondée sur le
 *       handicap sauf charge disproportionnée démontrée par
 *       l'employeur. Art. 17 protection contre les représailles —
 *       renversement de la charge de la preuve sur l'employeur. Art.
 *       28 § 2 1° indemnité forfaitaire 6 mois de rémunération brute
 *       ou indemnisation préjudice réel art. 28 § 2 2°. Art. 28 § 3
 *       renversement de la charge en présomption de discrimination.</li>
 *   <li><b>CCT n° 95 du 10/10/2008</b> du CNT NAR concernant l'égalité
 *       de traitement durant toutes les phases de la relation de
 *       travail, rendue obligatoire par AR du 11/01/2009 (M.B.
 *       22/01/2009) — socle conventionnel transposant Directive
 *       2000/78/CE.</li>
 *   <li><b>Loi du 04/08/1996</b> relative au bien-être des travailleurs
 *       lors de l'exécution de leur travail, art. 32bis à 32sexies
 *       — articulation avec la procédure interne harcèlement /
 *       comportement abusif en cas de représailles après demande
 *       d'aménagement.</li>
 *   <li><b>Convention OIT n° 159</b> du 20/06/1983 concernant la
 *       réadaptation professionnelle et l'emploi des personnes
 *       handicapées (ratifiée par la Belgique, M.B. 28/04/1989).</li>
 *   <li><b>Convention ONU du 13/12/2006</b> relative aux droits des
 *       personnes handicapées, ratifiée par la Belgique (M.B.
 *       22/07/2009, entrée en vigueur 02/08/2009) — art. 5 égalité
 *       et non-discrimination + art. 27 travail et emploi.</li>
 *   <li><b>Directive 2000/78/CE</b> du Conseil du 27/11/2000 portant
 *       création d'un cadre général en faveur de l'égalité de
 *       traitement en matière d'emploi et de travail — art. 5
 *       aménagements raisonnables (obligation de moyens, sauf charge
 *       disproportionnée).</li>
 *   <li><b>C. pén. social du 06/06/2010</b>, art. 121 — sanction de
 *       niveau 2 amende administrative 80 à 800 € ou pénale 200 à
 *       2 000 € par travailleur (plafonnée) pour les infractions à
 *       la Loi 10/05/2007.</li>
 *   <li><b>Cass. (BE) 09/01/2017 S.15.0035.N</b> — caractère indirect
 *       de la discrimination par refus d'aménagement raisonnable.</li>
 *   <li><b>Cass. (BE) 18/12/2017 S.16.0014.F</b> — la charge
 *       disproportionnée s'apprécie strictement, l'employeur doit
 *       avoir épuisé les pistes de subsides AVIQ / VDAB / PHARE /
 *       Actiris avant d'invoquer ce moyen.</li>
 *   <li><b>CJUE 11/04/2013 C-335/11 et C-337/11 HK Danmark / Ring et
 *       Werge</b> — notion fonctionnelle de handicap couvrant la
 *       maladie de longue durée entraînant une limitation durable et
 *       substantielle de la participation à la vie professionnelle
 *       sur un pied d'égalité.</li>
 *   <li><b>CJUE 11/09/2019 C-397/18 Nobel Plastiques</b> —
 *       articulation invalidité / aménagements et obligation pour
 *       l'employeur de rechercher activement une solution.</li>
 *   <li><b>CJUE 11/07/2006 C-13/05 Chacón Navas</b> — distinction
 *       handicap / maladie ordinaire ; <b>CJUE 17/07/2008 C-303/06
 *       Coleman</b> — protection par ricochet (aidant).</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts (court-circuits successifs)</h2>
 * <ol>
 *   <li><b>A_ANALYSER</b> — statut handicap
 *       {@link DiscriminationBeHandicapAmenagementStatutHandicap#INDETERMINE}
 *       OU réponse employeur
 *       {@link DiscriminationBeHandicapAmenagementReponseEmployeur#INDETERMINE}
 *       / {@link DiscriminationBeHandicapAmenagementReponseEmployeur#EN_ATTENTE}.</li>
 *   <li><b>FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE</b> — statut
 *       {@link DiscriminationBeHandicapAmenagementStatutHandicap#CONTESTE}
 *       ou {@link DiscriminationBeHandicapAmenagementStatutHandicap#DURABLE_FACTUEL}
 *       sans avis SEPP favorable — la qualification est fragile, à
 *       trancher avant l'analyse discrimination.</li>
 *   <li><b>REPRESAILLES_PRESUMEES_LICENCIEMENT</b> — sanction subie
 *       autre que AUCUNE après la demande — la présomption art. 17
 *       joue.</li>
 *   <li><b>CONFORME_AMENAGEMENT_ACCORDE</b> — réponse employeur
 *       ACCORDE ou CONTRE_PROPOSITION avec mesures alternatives
 *       raisonnables.</li>
 *   <li><b>DISCRIMINATION_PRESUMEE_NON_MOTIVATION</b> — refus
 *       (REFUSE_NON_MOTIVE ou NON_REPONDU_RAISONNABLE) — l'absence
 *       de motivation détaillée écrite déclenche la présomption
 *       art. 28 § 3.</li>
 *   <li><b>CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE</b> — refus
 *       motivé, charge disproportionnée invoquée et démontrée
 *       (subsides demandés OU effectif très réduit, devis externe
 *       fourni, coût élevé en proportion du CA, avis SEPP non
 *       contredisant).</li>
 *   <li>Sinon (refus motivé mais charge non démontrée) →
 *       <b>DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE</b>.</li>
 * </ol>
 */
public final class DiscriminationBeHandicapAmenagementChecker {

    /** Effectif en deçà duquel la taille de l'entreprise milite pour la charge disproportionnée. */
    static final int SEUIL_EFFECTIF_TPE = 10;

    /** Seuil indicatif d'un coût élevé en proportion du CA (5 %). */
    static final BigDecimal SEUIL_RATIO_COUT_CA = new BigDecimal("0.05");

    static final String BASE_JURIDIQUE =
            "Loi du 10/05/2007 tendant à lutter contre certaines formes"
                    + " de discrimination (M.B. 30/05/2007), art. 4 4°"
                    + " (définition du handicap — limitation durable et"
                    + " substantielle), art. 4 9° (définition de"
                    + " l'aménagement raisonnable), art. 14 (obligation"
                    + " d'aménagement raisonnable au bénéfice des personnes"
                    + " en situation de handicap — le refus d'aménagement"
                    + " constitue une discrimination indirecte sauf charge"
                    + " disproportionnée démontrée par l'employeur), art. 17"
                    + " (protection contre les représailles — renversement"
                    + " de la charge de la preuve), art. 28 § 2 1°"
                    + " (indemnité forfaitaire 6 mois de rémunération brute)"
                    + " ou art. 28 § 2 2° (indemnisation du préjudice"
                    + " réellement subi), art. 28 § 3 (présomption de"
                    + " discrimination — renversement de la charge sur"
                    + " l'employeur dès lors que la victime apporte des"
                    + " faits permettant de présumer l'existence d'une"
                    + " discrimination) ; CCT n° 95 du 10/10/2008 du CNT"
                    + " NAR concernant l'égalité de traitement durant"
                    + " toutes les phases de la relation de travail,"
                    + " rendue obligatoire par AR du 11/01/2009 (M.B."
                    + " 22/01/2009) — socle conventionnel transposant"
                    + " Directive 2000/78/CE ; Loi du 04/08/1996 relative"
                    + " au bien-être des travailleurs, art. 32bis-32sexies"
                    + " (articulation avec la procédure interne harcèlement"
                    + " / comportement abusif si représailles après demande)"
                    + " ; Convention OIT n° 159 du 20/06/1983 concernant la"
                    + " réadaptation professionnelle et l'emploi des"
                    + " personnes handicapées (ratifiée par la Belgique"
                    + " M.B. 28/04/1989) ; Convention ONU du 13/12/2006"
                    + " relative aux droits des personnes handicapées,"
                    + " ratifiée par la Belgique (M.B. 22/07/2009 ;"
                    + " entrée en vigueur 02/08/2009), art. 5 égalité et"
                    + " non-discrimination + art. 27 travail et emploi ;"
                    + " Directive 2000/78/CE du Conseil du 27/11/2000"
                    + " portant création d'un cadre général en faveur de"
                    + " l'égalité de traitement en matière d'emploi et"
                    + " de travail, art. 5 aménagements raisonnables ;"
                    + " C. pén. social du 06/06/2010, art. 121 (sanction"
                    + " de niveau 2 amende administrative 80 à 800 € ou"
                    + " pénale 200 à 2 000 € par travailleur, plafonnée) ;"
                    + " Cass. (BE) 09/01/2017 S.15.0035.N (caractère"
                    + " indirect de la discrimination par refus"
                    + " d'aménagement) ; Cass. (BE) 18/12/2017"
                    + " S.16.0014.F (charge disproportionnée stricte —"
                    + " l'employeur doit avoir épuisé les pistes de"
                    + " subsides AVIQ / VDAB / PHARE / Actiris) ; CJUE"
                    + " 11/04/2013 C-335/11 et C-337/11 HK Danmark / Ring"
                    + " et Werge (notion fonctionnelle de handicap"
                    + " incluant maladie de longue durée) ; CJUE"
                    + " 11/09/2019 C-397/18 Nobel Plastiques (obligation"
                    + " de rechercher activement une solution) ; CJUE"
                    + " 11/07/2006 C-13/05 Chacón Navas (distinction"
                    + " handicap / maladie ordinaire) ; CJUE 17/07/2008"
                    + " C-303/06 Coleman (protection par ricochet).";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'analyse du refus d'aménagements raisonnables"
                    + " handicap BE produit un verdict indicatif fondé"
                    + " sur les déclarations de l'avocat. L'avocat"
                    + " spécialisé en droit social BE doit confirmer"
                    + " pour la situation considérée : (i) la"
                    + " qualification handicap au sens de l'art. 4 4°"
                    + " Loi 10/05/2007 et de la jurisprudence CJUE"
                    + " Ring/Werge — une simple maladie sans limitation"
                    + " durable ne suffit pas (Chacón Navas), tandis"
                    + " qu'une maladie de longue durée entraînant une"
                    + " limitation durable et substantielle de la"
                    + " participation à la vie professionnelle est"
                    + " couverte ; (ii) la formalisation de la demande"
                    + " d'aménagement par écrit (lettre recommandée"
                    + " conseillée pour la datation et la conservation"
                    + " de la preuve) ou via le rapport du médecin du"
                    + " travail SEPP — l'employeur informé d'une"
                    + " situation de handicap doit envisager"
                    + " spontanément l'aménagement, ne pas attendre une"
                    + " demande formelle ; (iii) la mobilisation des"
                    + " dispositifs de subsides AVIQ (Wallonie), VDAB"
                    + " (Flandre), PHARE / equal.brussels (Bruxelles"
                    + " francophone), Actiris (Bruxelles) avant"
                    + " d'invoquer la charge disproportionnée — la"
                    + " jurisprudence Cass. 18/12/2017 exige que"
                    + " l'employeur ait épuisé ces pistes ; (iv) la"
                    + " collecte de devis externes objectifs et chiffrés"
                    + " si la charge disproportionnée est invoquée, et"
                    + " leur communication contradictoire au"
                    + " travailleur ; (v) la chronologie post-demande —"
                    + " toute sanction (licenciement, régression"
                    + " substantielle des conditions, harcèlement"
                    + " représaille) intervenant après la demande"
                    + " d'aménagement déclenche la présomption art. 17"
                    + " Loi 10/05/2007 et renverse la charge de la"
                    + " preuve sur l'employeur ; (vi) le cumul des"
                    + " sanctions financières disponibles — indemnité"
                    + " forfaitaire 6 mois de rémunération brute (art."
                    + " 28 § 2 1°) cumulable avec l'indemnité de"
                    + " rupture de droit commun (Loi 03/07/1978 art."
                    + " 37 et s. + Loi 26/12/2013 statut unique) et"
                    + " avec les éventuels dommages et intérêts pour"
                    + " préjudice moral ; (vii) la stratégie de saisine"
                    + " — UNIA Centre interfédéral pour l'égalité des"
                    + " chances (action en cessation, intervention"
                    + " volontaire) ; Inspection sociale SPF Emploi"
                    + " (constat infraction C. pén. social art. 121) ;"
                    + " Tribunal du travail (action principale)"
                    + " parfois assortie d'une procédure UNIA"
                    + " parallèle ; (viii) l'articulation avec une"
                    + " éventuelle procédure interne harcèlement /"
                    + " comportement abusif (Loi 04/08/1996 art."
                    + " 32bis-32sexies) si les représailles prennent"
                    + " la forme d'un comportement persistant ; (ix)"
                    + " la prescription — action principale 5 ans à"
                    + " compter de la connaissance du dommage et de"
                    + " l'auteur (art. 2262bis C. civ. BE) /"
                    + " l'application du délai de 1 an / 5 ans Loi"
                    + " 03/07/1978 art. 15 pour les actions"
                    + " contractuelles connexes.";

    private DiscriminationBeHandicapAmenagementChecker() {
    }

    public static DiscriminationBeHandicapAmenagementResult analyze(
            DiscriminationBeHandicapAmenagementRequest request) {
        validateInputs(request);

        // ── Calculs dérivés ────────────────────────────────────────────────────
        DiscriminationBeHandicapAmenagementStatutHandicap statut =
                request.statutHandicap();
        DiscriminationBeHandicapAmenagementReponseEmployeur reponse =
                request.reponseEmployeur();
        DiscriminationBeHandicapAmenagementSanctionSubie sanction =
                request.sanctionSubie();

        boolean handicapQualifie = isHandicapQualifie(statut,
                request.avisSeppFavorable());
        boolean demandeFormalisee = request.dateDemandeAmenagement() != null;
        boolean refusCaracterise = isRefusCaracterise(reponse);
        boolean chargeDisproportionneeDemontree =
                isChargeDisproportionneeDemontree(request);
        boolean representaillesPresumees =
                sanction != DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE;
        BigDecimal indemnite6Mois =
                computeIndemniteForfaitaire6Mois(request.salaireMensuelBrut());

        // ── Hiérarchie 1 : configuration ambigüe → A_ANALYSER ──────────────────
        if (statut == DiscriminationBeHandicapAmenagementStatutHandicap.INDETERMINE
                || reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.INDETERMINE
                || reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.EN_ATTENTE) {
            String synthese = "La situation n'est pas encore mûre pour"
                    + " une qualification juridique. "
                    + (statut == DiscriminationBeHandicapAmenagementStatutHandicap.INDETERMINE
                            ? "Le statut handicap reste à instruire (avis SEPP, attestation AVIQ/VAPH/PHARE/DGPH ou éléments médicaux établissant la limitation durable et substantielle au sens de l'art. 4 4° Loi 10/05/2007 et de la jurisprudence CJUE Ring/Werge). "
                            : "")
                    + (reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.INDETERMINE
                            ? "La réponse de l'employeur n'est pas connue. "
                            : "")
                    + (reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.EN_ATTENTE
                            ? "Le délai raisonnable de réponse de l'employeur (typiquement 1 à 2 mois selon complexité de l'aménagement) n'est pas encore écoulé. "
                            : "")
                    + "Une analyse au cas par cas est requise pour qualifier"
                    + " la situation au regard de l'art. 14 Loi 10/05/2007.";
            return build(request,
                    DiscriminationBeHandicapAmenagementVerdict.A_ANALYSER,
                    handicapQualifie, demandeFormalisee, refusCaracterise,
                    chargeDisproportionneeDemontree, representaillesPresumees,
                    indemnite6Mois,
                    "CONFIGURATION_AMBIGUE",
                    synthese);
        }

        // ── Hiérarchie 2 : représailles présumées (priorité absolue) ───────────
        if (representaillesPresumees) {
            String sanctionLib = libelleSanction(sanction);
            String chronologie = (request.dateSanction() != null
                    && request.dateDemandeAmenagement() != null)
                    ? " La sanction est intervenue le "
                            + request.dateSanction() + " après une demande"
                            + " d'aménagement formalisée le "
                            + request.dateDemandeAmenagement() + "."
                    : "";
            String synthese = sanctionLib
                    + chronologie
                    + " La présomption de représailles de l'art. 17 Loi"
                    + " 10/05/2007 joue : l'employeur devra démontrer que"
                    + " la mesure a un motif étranger à la demande"
                    + " d'aménagement raisonnable. À défaut, le travailleur"
                    + " peut prétendre à l'indemnité forfaitaire de 6"
                    + " mois de rémunération brute (art. 28 § 2 1° Loi"
                    + " 10/05/2007)"
                    + (indemnite6Mois != null
                            ? ", indicativement " + indemnite6Mois.toPlainString() + " €,"
                            : "")
                    + " cumulable avec l'indemnité de rupture de droit"
                    + " commun (Loi 03/07/1978 art. 37 et s. + Loi"
                    + " 26/12/2013 statut unique) en cas de licenciement,"
                    + " et avec les éventuels dommages et intérêts pour"
                    + " préjudice moral. L'avocat veillera à saisir UNIA"
                    + " et/ou à introduire une action devant le Tribunal"
                    + " du travail dans le délai de prescription de 5"
                    + " ans (art. 2262bis C. civ. BE).";
            return build(request,
                    DiscriminationBeHandicapAmenagementVerdict.REPRESAILLES_PRESUMEES_LICENCIEMENT,
                    handicapQualifie, demandeFormalisee, refusCaracterise,
                    chargeDisproportionneeDemontree, true,
                    indemnite6Mois,
                    "REPRESAILLES_PRESUMEES_ART_17",
                    synthese);
        }

        // ── Hiérarchie 3 : qualification handicap fragile ──────────────────────
        if (!handicapQualifie) {
            String motif;
            if (statut == DiscriminationBeHandicapAmenagementStatutHandicap.CONTESTE) {
                motif = "L'employeur conteste la qualification handicap."
                        + " La preuve doit être apportée prioritairement"
                        + " par une attestation officielle AVIQ / VAPH /"
                        + " PHARE / DGPH ou un avis du médecin du travail"
                        + " SEPP constatant une limitation durable et"
                        + " substantielle au sens de l'art. 4 4° Loi"
                        + " 10/05/2007 et de la jurisprudence CJUE"
                        + " Ring/Werge.";
            } else {
                motif = "Le statut handicap repose sur une situation"
                        + " factuelle durable sans reconnaissance officielle"
                        + " ni avis SEPP favorable. La jurisprudence CJUE"
                        + " Ring/Werge admet ce critère fonctionnel mais"
                        + " la preuve est plus lourde pour le travailleur"
                        + " — il faut établir une limitation durable et"
                        + " substantielle de la participation à la vie"
                        + " professionnelle sur un pied d'égalité,"
                        + " distincte de la maladie ordinaire (CJUE"
                        + " Chacón Navas).";
            }
            String synthese = "La qualification handicap au sens de la"
                    + " Loi 10/05/2007 n'est pas suffisamment établie"
                    + " pour fonder une analyse discrimination solide. "
                    + motif + " L'avocat invitera le travailleur à"
                    + " consolider la preuve médicale (rapport du"
                    + " médecin traitant + sollicitation d'un avis SEPP +"
                    + " demande d'évaluation AVIQ/VAPH/PHARE) avant toute"
                    + " action contentieuse.";
            return build(request,
                    DiscriminationBeHandicapAmenagementVerdict.FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE,
                    false, demandeFormalisee, refusCaracterise,
                    chargeDisproportionneeDemontree, false,
                    indemnite6Mois,
                    "QUALIFICATION_HANDICAP_INCERTAINE",
                    synthese);
        }

        // ── Hiérarchie 4 : aménagement accordé ─────────────────────────────────
        if (reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE) {
            String synthese = "L'employeur a accordé l'aménagement"
                    + " raisonnable demandé"
                    + (request.typeAmenagementDemande() != null
                            && !request.typeAmenagementDemande().isBlank()
                            ? " (" + request.typeAmenagementDemande() + ")" : "")
                    + ". L'obligation art. 14 Loi 10/05/2007 est remplie."
                    + " L'avocat recommande de formaliser l'aménagement"
                    + " par avenant écrit au contrat de travail précisant"
                    + " sa durée, ses modalités concrètes, ses conditions"
                    + " de révision périodique, et le cas échéant la"
                    + " répartition des coûts entre employeur et subsides"
                    + " externes — afin de prévenir tout litige ultérieur"
                    + " sur l'effectivité de l'aménagement.";
            return build(request,
                    DiscriminationBeHandicapAmenagementVerdict.CONFORME_AMENAGEMENT_ACCORDE,
                    true, demandeFormalisee, false,
                    false, false,
                    indemnite6Mois,
                    "AMENAGEMENT_ACCORDE",
                    synthese);
        }

        if (reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.CONTRE_PROPOSITION
                && request.mesuresAlternativesProposees()) {
            String synthese = "L'employeur a formulé une contre-proposition"
                    + " comportant des mesures alternatives. L'art. 14"
                    + " Loi 10/05/2007 n'impose pas l'aménagement"
                    + " spécifiquement demandé, mais bien un aménagement"
                    + " raisonnable propre à atteindre l'objectif"
                    + " d'égalité — la contre-proposition est admissible"
                    + " si elle est effectivement raisonnable et"
                    + " proportionnée. L'avocat doit apprécier"
                    + " contradictoirement le caractère adapté des"
                    + " mesures proposées, leur faisabilité concrète et"
                    + " leur acceptabilité pour le travailleur. Si"
                    + " l'accord est trouvé, formalisation par avenant"
                    + " écrit conseillée. Si la contre-proposition est"
                    + " manifestement insuffisante au regard du handicap,"
                    + " l'analyse bascule sur la qualification refus"
                    + " déguisé (DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE).";
            return build(request,
                    DiscriminationBeHandicapAmenagementVerdict.CONFORME_AMENAGEMENT_ACCORDE,
                    true, demandeFormalisee, false,
                    false, false,
                    indemnite6Mois,
                    "CONTRE_PROPOSITION_ACCEPTABLE",
                    synthese);
        }

        // ── Hiérarchie 5 : refus non motivé → présomption art. 28 § 3 ──────────
        if (reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_NON_MOTIVE
                || reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.NON_REPONDU_RAISONNABLE
                || !request.motivationDetailleeFournie()) {
            String contexte = (reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.NON_REPONDU_RAISONNABLE)
                    ? "L'absence de réponse de l'employeur au-delà du"
                            + " délai raisonnable équivaut à un refus de"
                            + " fait."
                    : "L'employeur a refusé sans fournir de motivation"
                            + " détaillée écrite invoquant explicitement"
                            + " la charge disproportionnée.";
            String synthese = contexte
                    + " Le travailleur apporte ainsi un fait permettant"
                    + " de présumer l'existence d'une discrimination"
                    + " indirecte fondée sur le handicap. La présomption"
                    + " de l'art. 28 § 3 Loi 10/05/2007 joue : la charge"
                    + " de la preuve est renversée sur l'employeur, qui"
                    + " devra démontrer devant le juge l'existence d'une"
                    + " charge disproportionnée objective (épuisement des"
                    + " pistes de subsides AVIQ / VDAB / PHARE / Actiris"
                    + " — Cass. 18/12/2017, devis externes, ratio coût /"
                    + " CA, taille effective de l'entreprise). À défaut,"
                    + " le travailleur peut prétendre à l'indemnité"
                    + " forfaitaire de 6 mois de rémunération brute"
                    + " (art. 28 § 2 1°)"
                    + (indemnite6Mois != null
                            ? ", indicativement " + indemnite6Mois.toPlainString() + " €"
                            : "")
                    + ". L'avocat conseille la saisine d'UNIA en parallèle"
                    + " du Tribunal du travail.";
            return build(request,
                    DiscriminationBeHandicapAmenagementVerdict.DISCRIMINATION_PRESUMEE_NON_MOTIVATION,
                    true, demandeFormalisee, true,
                    false, false,
                    indemnite6Mois,
                    "MOTIVATION_DETAILLEE_MANQUANTE",
                    synthese);
        }

        // ── Hiérarchie 6 : charge disproportionnée démontrée ────────────────────
        if (chargeDisproportionneeDemontree) {
            String synthese = "L'employeur a fourni une motivation écrite"
                    + " détaillée invoquant la charge disproportionnée et"
                    + " produit des éléments objectifs à l'appui"
                    + " (notamment "
                    + (request.devisExterneFourni() ? "devis externe(s) chiffré(s)" : "")
                    + (request.devisExterneFourni() && request.subsidesDemandes() ? ", " : "")
                    + (request.subsidesDemandes()
                            ? "instruction des dispositifs de subsides AVIQ / VDAB / PHARE / Actiris"
                            : "")
                    + (request.effectifEntreprise() <= SEUIL_EFFECTIF_TPE
                            ? (request.devisExterneFourni() || request.subsidesDemandes() ? ", " : "")
                                    + "taille très réduite de l'entreprise (" + request.effectifEntreprise() + " ETP)"
                            : "")
                    + "). La charge disproportionnée au sens de l'art."
                    + " 14 Loi 10/05/2007 paraît démontrée"
                    + " conformément aux exigences de la jurisprudence"
                    + " Cass. 18/12/2017 S.16.0014.F. L'avocat veillera"
                    + " néanmoins à examiner contradictoirement si"
                    + " des mesures alternatives moins coûteuses"
                    + " (réaffectation, télétravail, ajustement horaire)"
                    + " auraient pu satisfaire l'obligation —"
                    + " l'employeur reste tenu de l'obligation de"
                    + " rechercher activement une solution (CJUE"
                    + " 11/09/2019 C-397/18 Nobel Plastiques).";
            return build(request,
                    DiscriminationBeHandicapAmenagementVerdict.CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE,
                    true, demandeFormalisee, true,
                    true, false,
                    indemnite6Mois,
                    "CHARGE_DISPROPORTIONNEE_DEMONTREE",
                    synthese);
        }

        // ── Cas qualifiant : refus motivé mais charge non démontrée ────────────
        StringBuilder faiblesses = new StringBuilder();
        if (!request.subsidesDemandes()) {
            faiblesses.append(" l'employeur n'a pas instruit les"
                    + " dispositifs de subsides AVIQ / VDAB / PHARE /"
                    + " Actiris pourtant susceptibles de réduire"
                    + " substantiellement le coût net (Cass. BE"
                    + " 18/12/2017 S.16.0014.F exige l'épuisement de"
                    + " ces pistes) ;");
        }
        if (!request.devisExterneFourni()) {
            faiblesses.append(" l'employeur n'a pas produit de devis"
                    + " externe chiffré et objectif à l'appui du coût"
                    + " allégué ;");
        }
        if (!request.mesuresAlternativesProposees()) {
            faiblesses.append(" l'employeur n'a pas exploré ni proposé"
                    + " de mesures alternatives moins coûteuses (CJUE"
                    + " 11/09/2019 Nobel Plastiques) ;");
        }
        if (request.avisSeppFavorable()) {
            faiblesses.append(" l'avis du médecin du travail SEPP est"
                    + " favorable à l'aménagement, contredisant la"
                    + " thèse de l'inadéquation invoquée par"
                    + " l'employeur ;");
        }
        String faiblessesTxt = faiblesses.length() > 0
                ? faiblesses.substring(0, faiblesses.length() - 1)
                : "";
        String synthese = "L'employeur a refusé l'aménagement raisonnable"
                + " en fournissant une motivation, mais la charge"
                + " disproportionnée n'est pas suffisamment démontrée au"
                + " sens de l'art. 14 Loi 10/05/2007 et de la"
                + " jurisprudence Cass. 18/12/2017. En particulier :"
                + faiblessesTxt
                + ". Le refus constitue dès lors une discrimination"
                + " indirecte fondée sur le handicap (Cass. BE 09/01/2017"
                + " S.15.0035.N). Le travailleur peut prétendre à"
                + " l'indemnité forfaitaire de 6 mois de rémunération"
                + " brute (art. 28 § 2 1° Loi 10/05/2007)"
                + (indemnite6Mois != null
                        ? ", indicativement " + indemnite6Mois.toPlainString() + " €,"
                        : "")
                + " ou à l'indemnisation du préjudice réellement subi"
                + " (art. 28 § 2 2°), cumulable le cas échéant avec"
                + " l'indemnité de rupture si licenciement consécutif."
                + " L'avocat conseille la saisine combinée d'UNIA et du"
                + " Tribunal du travail dans le délai de prescription de"
                + " 5 ans (art. 2262bis C. civ. BE).";
        return build(request,
                DiscriminationBeHandicapAmenagementVerdict.DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE,
                true, demandeFormalisee, true,
                false, false,
                indemnite6Mois,
                "CHARGE_DISPROPORTIONNEE_NON_DEMONTREE",
                synthese);
    }

    /**
     * Le handicap est <i>qualifié</i> si reconnu officiellement, sur
     * avis SEPP, par invalidité mutuelle, ou (cas faible) durable
     * factuel avec avis SEPP favorable. Sinon, qualification fragile.
     */
    private static boolean isHandicapQualifie(
            DiscriminationBeHandicapAmenagementStatutHandicap statut,
            boolean avisSeppFavorable) {
        return statut == DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL
                || statut == DiscriminationBeHandicapAmenagementStatutHandicap.MEDECIN_TRAVAIL_AVIS
                || statut == DiscriminationBeHandicapAmenagementStatutHandicap.INVALIDITE_MUTUELLE
                || (statut == DiscriminationBeHandicapAmenagementStatutHandicap.DURABLE_FACTUEL
                        && avisSeppFavorable);
    }

    /**
     * Un refus est <i>caractérisé</i> dès lors que la réponse n'est
     * pas un accord, une contre-proposition exploitable ou un
     * statut d'attente / indéterminé.
     */
    private static boolean isRefusCaracterise(
            DiscriminationBeHandicapAmenagementReponseEmployeur reponse) {
        return reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE
                || reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_NON_MOTIVE
                || reponse == DiscriminationBeHandicapAmenagementReponseEmployeur.NON_REPONDU_RAISONNABLE;
    }

    /**
     * Charge disproportionnée raisonnablement démontrée si l'employeur
     * a (i) invoqué explicitement le moyen, (ii) fourni une motivation
     * détaillée, ET (iii) au moins deux critères objectifs parmi :
     * subsides instruits, devis externe, ratio coût/CA significatif,
     * effectif TPE. L'avis SEPP non favorable est neutre.
     */
    private static boolean isChargeDisproportionneeDemontree(
            DiscriminationBeHandicapAmenagementRequest request) {
        if (!request.chargeDisproportionneeInvoquee()
                || !request.motivationDetailleeFournie()) {
            return false;
        }
        int score = 0;
        if (request.subsidesDemandes()) {
            score++;
        }
        if (request.devisExterneFourni()) {
            score++;
        }
        if (request.effectifEntreprise() <= SEUIL_EFFECTIF_TPE) {
            score++;
        }
        if (request.coutEstimeAmenagement() != null
                && request.chiffreAffairesAnnuel() != null
                && request.chiffreAffairesAnnuel().signum() > 0) {
            BigDecimal ratio = request.coutEstimeAmenagement()
                    .divide(request.chiffreAffairesAnnuel(), 6,
                            RoundingMode.HALF_UP);
            if (ratio.compareTo(SEUIL_RATIO_COUT_CA) >= 0) {
                score++;
            }
        }
        if (!request.avisSeppFavorable()) {
            score++;
        }
        return score >= 2;
    }

    /** Indemnité forfaitaire 6 mois indicative (art. 28 § 2 1° Loi 10/05/2007). */
    private static BigDecimal computeIndemniteForfaitaire6Mois(
            BigDecimal salaireMensuelBrut) {
        if (salaireMensuelBrut == null
                || salaireMensuelBrut.signum() <= 0) {
            return null;
        }
        return salaireMensuelBrut.multiply(BigDecimal.valueOf(6))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static String libelleSanction(
            DiscriminationBeHandicapAmenagementSanctionSubie sanction) {
        return switch (sanction) {
            case LICENCIEMENT -> "Le travailleur a été licencié après"
                    + " avoir formulé une demande d'aménagement"
                    + " raisonnable.";
            case REGRESSION_CONDITIONS_TRAVAIL -> "Le travailleur a"
                    + " subi une régression substantielle de ses"
                    + " conditions de travail (déclassement, modification"
                    + " substantielle unilatérale, mutation défavorable)"
                    + " après avoir formulé une demande d'aménagement"
                    + " raisonnable.";
            case HARCELEMENT_REPRESAILLE -> "Le travailleur a fait"
                    + " l'objet d'un comportement abusif caractérisé"
                    + " après avoir formulé une demande d'aménagement"
                    + " raisonnable — articulation possible avec la"
                    + " procédure interne harcèlement / comportement"
                    + " abusif (Loi 04/08/1996 art. 32bis-32sexies).";
            case AUCUNE -> "";
        };
    }

    private static DiscriminationBeHandicapAmenagementResult build(
            DiscriminationBeHandicapAmenagementRequest request,
            DiscriminationBeHandicapAmenagementVerdict verdict,
            boolean handicapQualifie,
            boolean demandeFormalisee,
            boolean refusCaracterise,
            boolean chargeDisproportionneeDemontree,
            boolean representaillesPresumees,
            BigDecimal indemnite6Mois,
            String raison,
            String synthese) {
        return new DiscriminationBeHandicapAmenagementResult(
                request.statutHandicap(),
                request.dateDemandeAmenagement(),
                request.typeAmenagementDemande(),
                request.coutEstimeAmenagement(),
                request.subsidesDemandes(),
                request.effectifEntreprise(),
                request.chiffreAffairesAnnuel(),
                request.reponseEmployeur(),
                request.motivationDetailleeFournie(),
                request.avisSeppFavorable(),
                request.chargeDisproportionneeInvoquee(),
                request.devisExterneFourni(),
                request.mesuresAlternativesProposees(),
                request.sanctionSubie(),
                request.dateSanction(),
                request.salaireMensuelBrut(),
                request.procedureUniaSaisie(),
                verdict,
                handicapQualifie,
                demandeFormalisee,
                refusCaracterise,
                chargeDisproportionneeDemontree,
                representaillesPresumees,
                indemnite6Mois,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(
            DiscriminationBeHandicapAmenagementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.statutHandicap() == null) {
            throw new IllegalArgumentException(
                    "statutHandicap est requis");
        }
        if (request.subsidesDemandes() == null) {
            throw new IllegalArgumentException(
                    "subsidesDemandes est requis");
        }
        if (request.effectifEntreprise() == null) {
            throw new IllegalArgumentException(
                    "effectifEntreprise est requis");
        }
        if (request.effectifEntreprise() < 0) {
            throw new IllegalArgumentException(
                    "effectifEntreprise doit être positif ou nul");
        }
        if (request.reponseEmployeur() == null) {
            throw new IllegalArgumentException(
                    "reponseEmployeur est requis");
        }
        if (request.motivationDetailleeFournie() == null) {
            throw new IllegalArgumentException(
                    "motivationDetailleeFournie est requis");
        }
        if (request.avisSeppFavorable() == null) {
            throw new IllegalArgumentException(
                    "avisSeppFavorable est requis");
        }
        if (request.chargeDisproportionneeInvoquee() == null) {
            throw new IllegalArgumentException(
                    "chargeDisproportionneeInvoquee est requis");
        }
        if (request.devisExterneFourni() == null) {
            throw new IllegalArgumentException(
                    "devisExterneFourni est requis");
        }
        if (request.mesuresAlternativesProposees() == null) {
            throw new IllegalArgumentException(
                    "mesuresAlternativesProposees est requis");
        }
        if (request.sanctionSubie() == null) {
            throw new IllegalArgumentException(
                    "sanctionSubie est requis");
        }
        if (request.procedureUniaSaisie() == null) {
            throw new IllegalArgumentException(
                    "procedureUniaSaisie est requis");
        }
        if (request.coutEstimeAmenagement() != null
                && request.coutEstimeAmenagement().signum() < 0) {
            throw new IllegalArgumentException(
                    "coutEstimeAmenagement doit être positif ou nul");
        }
        if (request.chiffreAffairesAnnuel() != null
                && request.chiffreAffairesAnnuel().signum() < 0) {
            throw new IllegalArgumentException(
                    "chiffreAffairesAnnuel doit être positif ou nul");
        }
        if (request.salaireMensuelBrut() != null
                && request.salaireMensuelBrut().signum() < 0) {
            throw new IllegalArgumentException(
                    "salaireMensuelBrut doit être positif ou nul");
        }
    }
}
