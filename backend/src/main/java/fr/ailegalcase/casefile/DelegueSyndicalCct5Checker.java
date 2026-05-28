package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-219-10 : vérificateur du statut <i>délégué syndical CCT n° 5</i>
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>CCT n° 5 du 24/05/1971</b> conclue au Conseil National du
 *       Travail concernant le <b>statut des délégations syndicales du
 *       personnel des entreprises</b> (modifiée par CCT n° 5bis du
 *       30/06/1971 et CCT n° 5ter du 21/12/1978). Rendue obligatoire
 *       par AR du 26/01/1972.</li>
 *   <li>L'institution de la délégation syndicale n'est pas universelle :
 *       elle relève des <b>CCT sectorielles</b> conclues en commission
 *       paritaire, qui fixent les seuils (généralement 20 à 50 travailleurs
 *       selon le secteur) et les modalités de désignation.</li>
 *   <li>Articles centraux : art. 2 (désignation par OS représentatives),
 *       art. 3 (missions), art. 24 (missions supplétives en l'absence
 *       de CE / CPPT).</li>
 *   <li>Volet <b>protection contre le licenciement</b> non couvert par
 *       cet outil — relève de la Loi du 19/03/1991 (outil SF-213-08
 *       <i>licenciement-be-protection-deleguee</i>, distinct).</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>INELIGIBLE_ENTREPRISE_HORS_CHAMP</b> — effectif sous seuil
 *       sectoriel OU absence d'OS représentative dans l'entreprise →
 *       pas de DS au sens CCT 5 (sortie immédiate).</li>
 *   <li><b>INELIGIBLE_PAS_DESIGNE_PAR_OS</b> — pas de désignation
 *       régulière par une organisation syndicale représentative —
 *       auto-proclamation, élection interne — pas de statut CCT 5.</li>
 *   <li><b>A_ANALYSER</b> — désignation contestée (conflit
 *       inter-syndical, qualification de la représentativité) — analyse
 *       juridique pointue requise.</li>
 *   <li><b>STATUT_FRAGILE_NOTIFICATION_MANQUANTE</b> — désigné par OS
 *       mais notification écrite à l'employeur manquante — statut
 *       formellement défaillant, à régulariser.</li>
 *   <li>Sinon : <b>STATUT_RECONNU</b> — missions de l'art. 3 CCT 5
 *       (négociation CCT entreprise, traitement plaintes, information)
 *       exerçables ; missions supplétives art. 24 si CE / CPPT absents.</li>
 * </ol>
 *
 * <h2>Durée du mandat</h2>
 * <p>La CCT n° 5 ne fixe pas de durée plafond du mandat. Les CCT
 * sectorielles imposent généralement une durée de <b>4 ans</b> alignée
 * sur les élections sociales (CE / CPPT). L'outil restitue une date de
 * fin indicative à 4 ans (paramètre {@link #DUREE_MANDAT_INDICATIVE_ANNEES}).</p>
 */
public final class DelegueSyndicalCct5Checker {

    static final String BASE_JURIDIQUE =
            "CCT n° 5 du 24/05/1971 conclue au CNT (statut des délégations"
                    + " syndicales du personnel des entreprises) ; CCT n° 5bis"
                    + " du 30/06/1971 ; CCT n° 5ter du 21/12/1978 ; AR du"
                    + " 26/01/1972 (force obligatoire) ; CCT sectorielles"
                    + " applicables en commission paritaire pour les seuils,"
                    + " modalités de désignation, crédit d'heures et facilités ;"
                    + " art. 2 (désignation par OS représentatives), art. 3"
                    + " (missions : négociation, plaintes, information), art. 24"
                    + " (missions supplétives en l'absence de CE / CPPT).";

    static final String AVERT_SECTORIEL_AVOCAT =
            "L'institution effective de la délégation syndicale dépend de la"
                    + " CCT sectorielle applicable (commission paritaire) qui"
                    + " fixe le seuil exact, les modalités de désignation, le"
                    + " crédit d'heures et les facilités matérielles. Une"
                    + " analyse complète doit être confirmée par un avocat"
                    + " spécialisé en droit du travail BE. Le statut de"
                    + " protection contre le licenciement (Loi 19/03/1991)"
                    + " relève d'un outil distinct (licenciement-be-protection-"
                    + "deleguee) et obéit à un régime de procédure spécifique.";

    /** Durée indicative du mandat DS (alignée élections sociales, paramétrable par CCT sectorielle). */
    static final int DUREE_MANDAT_INDICATIVE_ANNEES = 4;

    private DelegueSyndicalCct5Checker() {
    }

    public static DelegueSyndicalCct5Result analyze(
            DelegueSyndicalCct5Request request) {
        validateInputs(request);

        // ── Hiérarchie 1 : entreprise hors champ (seuil OU OS absente) ─────
        boolean seuilAtteint = request.effectifEntreprise()
                >= request.seuilSectorielRequis();
        if (!seuilAtteint || !request.presenceOsRepresentative()) {
            String motif = !seuilAtteint
                    ? "effectif de " + request.effectifEntreprise()
                            + " travailleurs sous le seuil sectoriel de "
                            + request.seuilSectorielRequis()
                    : "absence d'organisation syndicale représentative dans"
                            + " l'entreprise";
            return ineligible(request,
                    DelegueSyndicalCct5Verdict.INELIGIBLE_ENTREPRISE_HORS_CHAMP,
                    "ENTREPRISE_HORS_CHAMP_CCT5",
                    "L'entreprise n'entre pas dans le champ d'application"
                            + " de la CCT n° 5 (" + motif + "). Aucune"
                            + " délégation syndicale au sens de la CCT 5 ne"
                            + " peut y être instituée. Vérifier toutefois si"
                            + " une CCT sectorielle plus favorable abaisse le"
                            + " seuil ou prévoit un régime distinct.");
        }

        // ── Hiérarchie 2 : pas désigné par OS représentative ───────────────
        if (request.statutDesignation()
                == DelegueSyndicalCct5StatutDesignation.PAS_DESIGNE_PAR_OS) {
            return ineligible(request,
                    DelegueSyndicalCct5Verdict.INELIGIBLE_PAS_DESIGNE_PAR_OS,
                    "PAS_DESIGNE_PAR_OS_REPRESENTATIVE",
                    "Le travailleur n'a pas été désigné par une organisation"
                            + " syndicale représentative (CSC / FGTB / CGSLB)."
                            + " La CCT n° 5 réserve le statut de délégué"
                            + " syndical aux personnes désignées par les OS"
                            + " représentatives (art. 2) — une"
                            + " auto-proclamation ou une élection interne"
                            + " sans mandat syndical ne confère pas le statut.");
        }

        // ── Hiérarchie 3 : désignation contestée ───────────────────────────
        if (request.statutDesignation()
                == DelegueSyndicalCct5StatutDesignation.DESIGNATION_CONTESTEE) {
            return ineligible(request,
                    DelegueSyndicalCct5Verdict.A_ANALYSER,
                    "DESIGNATION_CONTESTEE",
                    "La désignation fait l'objet d'une contestation pendante"
                            + " (conflit inter-syndical, contestation de la"
                            + " représentativité de l'OS, irrégularité de"
                            + " procédure). Une analyse juridique pointue est"
                            + " requise (avocat BE spécialisé droit syndical)"
                            + " avant de conclure sur le statut effectif et"
                            + " les missions exerçables.");
        }

        // ── Cas qualifiant : missions exerçables + date fin mandat ─────────
        LocalDate dateFinMandat = request.dateDesignation()
                .plusYears(DUREE_MANDAT_INDICATIVE_ANNEES);

        List<String> missions = construireMissions(
                request.ceExistant(), request.cpptExistant());

        // Notification employeur manquante → statut fragile
        if (request.statutDesignation()
                == DelegueSyndicalCct5StatutDesignation.NOTIFICATION_EMPLOYEUR_MANQUANTE) {
            String synthese = "Le travailleur a été désigné par une OS"
                    + " représentative mais la notification écrite à"
                    + " l'employeur (preuve formelle requise) est manquante"
                    + " ou non probante. Statut formellement défaillant à"
                    + " régulariser sans délai : à défaut, l'employeur peut"
                    + " légitimement contester l'exercice des missions"
                    + " (art. 3 CCT n° 5). Une fois régularisée, mandat"
                    + " indicatif jusqu'au " + dateFinMandat + " (4 ans"
                    + " référence sectorielle).";
            return new DelegueSyndicalCct5Result(
                    request.dateDesignation(),
                    request.effectifEntreprise(),
                    request.seuilSectorielRequis(),
                    request.presenceOsRepresentative(),
                    request.statutDesignation(),
                    request.ceExistant(),
                    request.cpptExistant(),
                    DelegueSyndicalCct5Verdict.STATUT_FRAGILE_NOTIFICATION_MANQUANTE,
                    false,
                    true,
                    false,
                    missions,
                    dateFinMandat,
                    "NOTIFICATION_EMPLOYEUR_MANQUANTE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_SECTORIEL_AVOCAT);
        }

        // Statut reconnu pleinement
        String synthese = "Statut de délégué syndical CCT n° 5 pleinement"
                + " reconnu : entreprise dans le champ (effectif "
                + request.effectifEntreprise() + " ≥ seuil "
                + request.seuilSectorielRequis() + " + OS représentative"
                + " présente), désignation régulière par OS, notification"
                + " formelle à l'employeur effectuée. Missions exerçables :"
                + " " + String.join(" ; ", missions) + ". Mandat indicatif"
                + " jusqu'au " + dateFinMandat + " (4 ans référence"
                + " sectorielle, durée précise à confirmer par CCT"
                + " sectorielle applicable).";

        return new DelegueSyndicalCct5Result(
                request.dateDesignation(),
                request.effectifEntreprise(),
                request.seuilSectorielRequis(),
                request.presenceOsRepresentative(),
                request.statutDesignation(),
                request.ceExistant(),
                request.cpptExistant(),
                DelegueSyndicalCct5Verdict.STATUT_RECONNU,
                true,
                true,
                true,
                missions,
                dateFinMandat,
                "STATUT_RECONNU",
                synthese,
                BASE_JURIDIQUE,
                AVERT_SECTORIEL_AVOCAT);
    }

    // ── Construction d'un résultat inéligible ──────────────────────────────

    private static DelegueSyndicalCct5Result ineligible(
            DelegueSyndicalCct5Request request,
            DelegueSyndicalCct5Verdict verdict,
            String raison, String synthese) {
        return new DelegueSyndicalCct5Result(
                request.dateDesignation(),
                request.effectifEntreprise(),
                request.seuilSectorielRequis(),
                request.presenceOsRepresentative(),
                request.statutDesignation(),
                request.ceExistant(),
                request.cpptExistant(),
                verdict,
                false,
                verdict != DelegueSyndicalCct5Verdict.INELIGIBLE_ENTREPRISE_HORS_CHAMP,
                false,
                Collections.emptyList(),
                null,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_SECTORIEL_AVOCAT);
    }

    // ── Construction des missions exerçables (art. 3 + art. 24) ────────────

    private static List<String> construireMissions(boolean ceExistant,
                                                   boolean cpptExistant) {
        List<String> missions = new ArrayList<>();
        // Art. 3 — missions de base
        missions.add("négociation des CCT d'entreprise (art. 3)");
        missions.add("traitement des réclamations individuelles et collectives"
                + " (art. 3)");
        missions.add("information du personnel sur les CCT et les questions"
                + " sociales (art. 3)");
        missions.add("intervention en cas de conflit collectif et tentative de"
                + " conciliation (art. 3)");

        // Art. 24 — missions supplétives en l'absence de CE / CPPT
        if (!ceExistant) {
            missions.add("exercice supplétif des missions du Conseil"
                    + " d'entreprise (art. 24 — économiques et financières)");
        }
        if (!cpptExistant) {
            missions.add("exercice supplétif des missions du Comité PPT"
                    + " (art. 24 — sécurité, santé, bien-être au travail)");
        }
        return Collections.unmodifiableList(missions);
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(DelegueSyndicalCct5Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateDesignation() == null) {
            throw new IllegalArgumentException("dateDesignation est requise");
        }
        if (request.effectifEntreprise() == null) {
            throw new IllegalArgumentException(
                    "effectifEntreprise est requis");
        }
        if (request.effectifEntreprise() < 0) {
            throw new IllegalArgumentException(
                    "effectifEntreprise doit être positif ou nul");
        }
        if (request.seuilSectorielRequis() == null) {
            throw new IllegalArgumentException(
                    "seuilSectorielRequis est requis");
        }
        if (request.seuilSectorielRequis() < 1) {
            throw new IllegalArgumentException(
                    "seuilSectorielRequis doit être au moins 1");
        }
        if (request.presenceOsRepresentative() == null) {
            throw new IllegalArgumentException(
                    "presenceOsRepresentative est requise");
        }
        if (request.statutDesignation() == null) {
            throw new IllegalArgumentException(
                    "statutDesignation est requis");
        }
        if (request.ceExistant() == null) {
            throw new IllegalArgumentException("ceExistant est requis");
        }
        if (request.cpptExistant() == null) {
            throw new IllegalArgumentException("cpptExistant est requis");
        }
    }
}
