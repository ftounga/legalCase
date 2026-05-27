package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/**
 * SF-219-05 : analyseur de conformité de l'outplacement BE général au titre
 * du régime "préavis ≥ 30 semaines" — fonction <b>pure</b> (sans dépendance
 * HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 05/09/2001 art. 11</b> visant à améliorer le taux d'emploi
 *       des travailleurs — instaure le droit à l'outplacement pour les
 *       travailleurs licenciés.</li>
 *   <li><b>AR du 21/10/2007</b> portant exécution de l'article 13 de la Loi
 *       du 05/09/2001 — fixe les conditions de l'offre d'outplacement
 *       (régime général) : 60h, 12 mois, contenu minimum.</li>
 * </ul>
 *
 * <h2>Conditions cumulatives (toutes requises pour {@code CONFORME})</h2>
 * <ol>
 *   <li><b>Licenciement effectif</b> à l'initiative de l'employeur.</li>
 *   <li>Motif <b>non grave</b> (art. 11/3 § 1er Loi 05/09/2001).</li>
 *   <li>Préavis (ou indemnité de rupture équivalente) ≥ <b>30 semaines</b>.</li>
 *   <li>Offre <b>dans les 15 jours civils</b> suivant la fin du contrat.</li>
 *   <li>Programme ≥ <b>60 heures</b> sur ≤ <b>12 mois</b>.</li>
 *   <li>Forme : écrite, individuelle, contenu minimum présent.</li>
 * </ol>
 *
 * <h2>Hiérarchie des raisons de non-conformité</h2>
 * <p>Lorsque plusieurs conditions échouent, l'ordre de priorité retenu est :
 * <ol>
 *   <li>{@code NON_DU_DEMISSION} (showstopper — pas de droit) ;</li>
 *   <li>{@code NON_DU_MOTIF_GRAVE} (showstopper — exclu du régime) ;</li>
 *   <li>{@code NON_DU_PREAVIS_INSUFFISANT} (régime 30 semaines non applicable) ;</li>
 *   <li>{@code NON_CONFORME_OFFRE_TARDIVE} (employeur fautif) ;</li>
 *   <li>{@code NON_CONFORME_DUREE_INSUFFISANTE} (heures / mois) ;</li>
 *   <li>{@code NON_CONFORME_FORME} (écrit, individuel, contenu).</li>
 * </ol>
 * Le détail des conditions reste toujours retourné via les flags
 * {@code conditionXxxRemplie} pour permettre à l'UI de présenter le bilan
 * complet à l'avocat.</p>
 *
 * <h2>Sanction indicative travailleur</h2>
 * <p>Lorsque l'employeur a manqué à son obligation d'offre conforme
 * (verdicts {@code NON_CONFORME_*}), une indemnité forfaitaire indicative
 * de <b>1 800 €</b> est calculée (CCT n° 82 / 82bis, montant de base à
 * confirmer par la jurisprudence et l'évolution de la CCT). Aucune sanction
 * n'est calculée pour les verdicts {@code NON_DU_*} (pas de droit ouvert,
 * donc pas de manquement) ni pour le verdict {@code CONFORME}.</p>
 */
public final class OutplacementBeGeneral30semChecker {

    static final String BASE_JURIDIQUE =
            "Loi du 05/09/2001 art. 11 (politique de l'emploi) ; "
                    + "AR du 21/10/2007 (procédure outplacement régime "
                    + "général — 60h sur 12 mois, contenu minimum) ; "
                    + "CCT n° 82 / 82bis (cadre conventionnel).";

    static final String AVERT_SANCTION_INDICATIVE =
            "L'indemnité forfaitaire est indicative — montant définitif à "
                    + "confirmer par CCT sectorielle / barème ONEM / "
                    + "jurisprudence en vigueur. Le travailleur peut également "
                    + "encourir une sanction ONEM d'exclusion des allocations "
                    + "chômage (4 à 52 semaines, art. 51 AR 25/11/1991) en cas "
                    + "de refus injustifié d'un outplacement régulièrement offert.";

    /** Seuil minimum de préavis (semaines) pour ouverture du régime général. */
    static final int SEUIL_PREAVIS_SEMAINES = 30;

    /** Délai maximum d'offre (jours civils après fin du contrat). */
    static final int DELAI_OFFRE_MAX_JOURS = 15;

    /** Heures minimum du programme (AR 21/10/2007 — art. 1er § 1er). */
    static final int HEURES_MIN_PROGRAMME = 60;

    /** Durée maximum du programme en mois. */
    static final int DUREE_MAX_PROGRAMME_MOIS = 12;

    /** Indemnité forfaitaire indicative travailleur (CCT n° 82 / 82bis). */
    static final BigDecimal INDEMNITE_FORFAITAIRE_TRAVAILLEUR =
            new BigDecimal("1800.00");

    private OutplacementBeGeneral30semChecker() {
    }

    public static OutplacementBeGeneral30semResult analyze(
            OutplacementBeGeneral30semRequest request) {
        validateInputs(request);

        boolean licenciementOk = Boolean.TRUE.equals(request.licenciementEffectif());
        boolean motifNonGraveOk = !Boolean.TRUE.equals(request.motifGrave());
        boolean preavisOk = request.semainesPreavisOuIndemnite() != null
                && request.semainesPreavisOuIndemnite() >= SEUIL_PREAVIS_SEMAINES;
        boolean offreDansLes15joursOk = computeOffreDansLes15joursOk(request);
        boolean dureeOk = request.heuresProgramme() != null
                && request.dureeProgrammeMois() != null
                && request.heuresProgramme() >= HEURES_MIN_PROGRAMME
                && request.dureeProgrammeMois() <= DUREE_MAX_PROGRAMME_MOIS
                && request.dureeProgrammeMois() > 0;
        boolean formeOk = Boolean.TRUE.equals(request.offreEcrite())
                && Boolean.TRUE.equals(request.offreIndividuelle())
                && Boolean.TRUE.equals(request.contenuMinimumRespecte());

        // Verdict avec priorité :
        //   démission > motif grave > préavis > offre tardive > durée > forme > conforme
        OutplacementBeGeneral30semVerdict verdict;
        if (!licenciementOk) {
            verdict = OutplacementBeGeneral30semVerdict.NON_DU_DEMISSION;
        } else if (!motifNonGraveOk) {
            verdict = OutplacementBeGeneral30semVerdict.NON_DU_MOTIF_GRAVE;
        } else if (!preavisOk) {
            verdict = OutplacementBeGeneral30semVerdict.NON_DU_PREAVIS_INSUFFISANT;
        } else if (!offreDansLes15joursOk) {
            verdict = OutplacementBeGeneral30semVerdict.NON_CONFORME_OFFRE_TARDIVE;
        } else if (!dureeOk) {
            verdict = OutplacementBeGeneral30semVerdict.NON_CONFORME_DUREE_INSUFFISANTE;
        } else if (!formeOk) {
            verdict = OutplacementBeGeneral30semVerdict.NON_CONFORME_FORME;
        } else {
            verdict = OutplacementBeGeneral30semVerdict.CONFORME;
        }

        boolean conforme = verdict == OutplacementBeGeneral30semVerdict.CONFORME;

        // Sanction : uniquement pour les verdicts NON_CONFORME_* (l'employeur
        // a manqué à son obligation d'offre conforme). Les verdicts NON_DU_*
        // n'ouvrent pas de droit, donc pas de manquement.
        BigDecimal indemniteForfaitaire = null;
        String avertissement = null;
        if (verdict == OutplacementBeGeneral30semVerdict.NON_CONFORME_OFFRE_TARDIVE
                || verdict == OutplacementBeGeneral30semVerdict.NON_CONFORME_DUREE_INSUFFISANTE
                || verdict == OutplacementBeGeneral30semVerdict.NON_CONFORME_FORME) {
            indemniteForfaitaire = INDEMNITE_FORFAITAIRE_TRAVAILLEUR;
            avertissement = AVERT_SANCTION_INDICATIVE;
        }

        String synthese = buildSynthese(verdict, request,
                preavisOk, dureeOk, formeOk);

        return new OutplacementBeGeneral30semResult(
                licenciementOk,
                Boolean.TRUE.equals(request.motifGrave()),
                request.semainesPreavisOuIndemnite(),
                request.dateFinContrat(),
                Boolean.TRUE.equals(request.offreNotifiee()),
                request.dateNotificationOffre(),
                request.heuresProgramme(),
                request.dureeProgrammeMois(),
                Boolean.TRUE.equals(request.offreEcrite()),
                Boolean.TRUE.equals(request.offreIndividuelle()),
                Boolean.TRUE.equals(request.contenuMinimumRespecte()),
                verdict,
                conforme,
                licenciementOk,
                motifNonGraveOk,
                preavisOk,
                offreDansLes15joursOk,
                dureeOk,
                formeOk,
                indemniteForfaitaire,
                synthese,
                BASE_JURIDIQUE,
                avertissement);
    }

    /**
     * Évalue la condition "offre dans les 15 jours civils suivant la fin
     * du contrat". Si l'employeur n'a pas du tout notifié l'offre, la
     * condition est non remplie. Si la date est avant la fin du contrat
     * (cas exceptionnel — offre anticipée), elle est tout de même valide
     * (anticipation favorable au travailleur).
     */
    private static boolean computeOffreDansLes15joursOk(
            OutplacementBeGeneral30semRequest request) {
        if (!Boolean.TRUE.equals(request.offreNotifiee())) {
            return false;
        }
        if (request.dateNotificationOffre() == null) {
            return false;
        }
        long deltaJours = ChronoUnit.DAYS.between(
                request.dateFinContrat(), request.dateNotificationOffre());
        return deltaJours <= DELAI_OFFRE_MAX_JOURS;
    }

    // ── Synthèse en langage avocat ─────────────────────────────────────────

    private static String buildSynthese(
            OutplacementBeGeneral30semVerdict verdict,
            OutplacementBeGeneral30semRequest request,
            boolean preavisOk,
            boolean dureeOk,
            boolean formeOk) {
        return switch (verdict) {
            case CONFORME -> "Offre d'outplacement régulière : licenciement "
                    + "effectif, motif non grave, préavis ≥ 30 semaines ("
                    + request.semainesPreavisOuIndemnite() + " sem.), offre "
                    + "écrite individuelle dans les 15 jours, programme de "
                    + request.heuresProgramme() + "h sur "
                    + request.dureeProgrammeMois() + " mois avec contenu "
                    + "minimum respecté.";
            case NON_DU_DEMISSION -> "Démission : pas de droit à l'outplacement "
                    + "(régime fermé en l'absence de licenciement par "
                    + "l'employeur).";
            case NON_DU_MOTIF_GRAVE -> "Licenciement pour motif grave : exclu "
                    + "du régime d'outplacement (art. 11/3 § 1er Loi du "
                    + "05/09/2001).";
            case NON_DU_PREAVIS_INSUFFISANT -> "Préavis ("
                    + request.semainesPreavisOuIndemnite() + " semaines) "
                    + "< 30 semaines : régime général \"30 semaines\" non "
                    + "applicable. Vérifier l'éligibilité au régime "
                    + "outplacement 45+ ans (F-207 SF-207-08 — autre "
                    + "régime).";
            case NON_CONFORME_OFFRE_TARDIVE -> "Offre absente ou notifiée "
                    + "au-delà du délai de 15 jours civils après la fin du "
                    + "contrat — manquement de l'employeur. Indemnité "
                    + "forfaitaire travailleur due. "
                    + (dureeOk && formeOk
                            ? "La durée et la forme du programme sont conformes."
                            : "D'autres défauts de conformité sont également relevés.");
            case NON_CONFORME_DUREE_INSUFFISANTE -> "Programme < 60h ou "
                    + "durée > 12 mois (heures = " + request.heuresProgramme()
                    + ", durée = " + request.dureeProgrammeMois() + " mois) : "
                    + "manquement de l'employeur (AR 21/10/2007 — art. 1er § 1er). "
                    + (formeOk
                            ? "La forme de l'offre est conforme."
                            : "La forme de l'offre présente également des défauts.");
            case NON_CONFORME_FORME -> "Forme de l'offre non conforme : "
                    + "vérifier le caractère écrit, individuel et le contenu "
                    + "minimum (bilan personnel, projet professionnel, "
                    + "coaching, formation, soutien psychologique). "
                    + "Manquement de l'employeur.";
        };
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(OutplacementBeGeneral30semRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.licenciementEffectif() == null) {
            throw new IllegalArgumentException("licenciementEffectif est requis");
        }
        if (request.motifGrave() == null) {
            throw new IllegalArgumentException("motifGrave est requis");
        }
        if (request.semainesPreavisOuIndemnite() == null) {
            throw new IllegalArgumentException(
                    "semainesPreavisOuIndemnite est requis");
        }
        if (request.semainesPreavisOuIndemnite() < 0) {
            throw new IllegalArgumentException(
                    "semainesPreavisOuIndemnite ne peut pas être négatif");
        }
        if (request.dateFinContrat() == null) {
            throw new IllegalArgumentException("dateFinContrat est requise");
        }
        if (request.offreNotifiee() == null) {
            throw new IllegalArgumentException("offreNotifiee est requis");
        }
        if (request.heuresProgramme() == null) {
            throw new IllegalArgumentException("heuresProgramme est requis");
        }
        if (request.heuresProgramme() < 0) {
            throw new IllegalArgumentException(
                    "heuresProgramme ne peut pas être négatif");
        }
        if (request.dureeProgrammeMois() == null) {
            throw new IllegalArgumentException("dureeProgrammeMois est requise");
        }
        if (request.dureeProgrammeMois() < 0) {
            throw new IllegalArgumentException(
                    "dureeProgrammeMois ne peut pas être négative");
        }
        if (request.offreEcrite() == null) {
            throw new IllegalArgumentException("offreEcrite est requis");
        }
        if (request.offreIndividuelle() == null) {
            throw new IllegalArgumentException("offreIndividuelle est requis");
        }
        if (request.contenuMinimumRespecte() == null) {
            throw new IllegalArgumentException("contenuMinimumRespecte est requis");
        }
        // Cohérence : si offre déclarée notifiée, date d'offre obligatoire
        if (Boolean.TRUE.equals(request.offreNotifiee())
                && request.dateNotificationOffre() == null) {
            throw new IllegalArgumentException(
                    "dateNotificationOffre est requise lorsque "
                            + "offreNotifiee = true");
        }
    }
}
