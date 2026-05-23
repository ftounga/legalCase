package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-27 : calculateur statique pour le partage successoral notarié
 * en droit français (art. 816 et s. Cciv + art. 870 Cciv + art. 1592
 * CGI + art. 641 CGI + art. 840 Cciv).
 *
 * <p>Le calculateur :</p>
 * <ul>
 *   <li>évalue si le recours au notaire est <strong>obligatoire</strong>
 *       (présence d'immeuble dans la succession — art. 1592 CGI) ou
 *       simplement recommandé ;</li>
 *   <li>génère le <strong>calendrier des 5 étapes</strong> de la
 *       procédure amiable : désignation du notaire → bilan patrimonial
 *       (inventaire actif/passif) → attestation après décès (publication
 *       au fichier immobilier pour les immeubles) → projet de partage
 *       (répartition proposée par le notaire) → signature de l'acte de
 *       partage par tous les héritiers (présents ou représentés) ;</li>
 *   <li>calcule le <strong>délai de la déclaration fiscale</strong>
 *       (échéance = date d'ouverture + 6 mois, art. 641 CGI) et émet
 *       une alerte si la date du jour dépasse cette échéance ;</li>
 *   <li>oriente vers le <strong>partage judiciaire</strong>
 *       (F-FA-17-partage-judiciaire, art. 840 Cciv) en cas de désaccord
 *       persistant entre cohéritiers.</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, la matière
 * successorale est régie par les art. 4.62 et s. CC BE (outil
 * F-FA-BE-PARTAGE-NOTARIAL futur, hors périmètre F-216).</p>
 */
public final class PartageNotarialCalculator {

    static final String BASE_JURIDIQUE =
            "art. 816-842 Cciv (partage successoral) + art. 870 Cciv "
                    + "(déclaration de succession) + art. 1592 CGI "
                    + "(obligation notariale en présence d'immeubles) + "
                    + "art. 641 CGI (délai 6 mois déclaration fiscale) + "
                    + "art. 840 Cciv (bascule judiciaire en cas de "
                    + "désaccord)";

    /**
     * Délai légal pour la déclaration de succession en France métropolitaine
     * (art. 641 CGI). Pour les successions ouvertes hors métropole, le délai
     * passe à 12 mois (art. 641 al. 2) — non géré dans cette V1.
     */
    static final int DELAI_DECLARATION_FISCALE_MOIS = 6;

    private PartageNotarialCalculator() {}

    /**
     * Évalue le partage successoral notarié et émet le verdict + le
     * calendrier des étapes.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @param today   date de référence pour l'alerte délai (typiquement
     *                {@link LocalDate#now()}). Injectable pour tester.
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static PartageNotarialResult compute(
            PartageNotarialRequest req, String country, LocalDate today) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-PARTAGE-NOTARIAL applicable uniquement "
                            + "en France (art. 816 et s. Cciv).");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        boolean presenceImmeuble = Boolean.TRUE.equals(req.presenceImmeuble());
        boolean consentementsTous = Boolean.TRUE.equals(req.consentementsTousDetecte());
        boolean desaccord = Boolean.TRUE.equals(req.desaccordPersistant());
        boolean notaireDesigne = Boolean.TRUE.equals(req.notaireDesigne());

        // 1. Notaire obligatoire si immeuble dans la succession (art. 1592 CGI).
        boolean notaireObligatoire = presenceImmeuble;
        if (notaireObligatoire) {
            messages.add("Présence d'un immeuble dans la succession : le "
                    + "recours au notaire est OBLIGATOIRE pour rédiger "
                    + "l'attestation immobilière après décès et l'acte "
                    + "de partage (art. 1592 CGI — règles de publicité "
                    + "foncière).");
        } else {
            messages.add("Aucun immeuble identifié dans la succession : "
                    + "le recours au notaire reste fortement recommandé "
                    + "pour sécuriser le partage amiable, mais n'est "
                    + "pas légalement obligatoire (les héritiers peuvent "
                    + "régulariser le partage par acte sous seing privé).");
        }

        // 2. Désaccord persistant → bascule vers le judiciaire (art. 840 Cciv).
        boolean orientationJudiciaire = desaccord;
        if (orientationJudiciaire) {
            alertes.add("Désaccord persistant entre cohéritiers : la "
                    + "procédure amiable devant notaire n'est plus tenable. "
                    + "Bascule vers le partage judiciaire (art. 840 Cciv) "
                    + "— assignation devant le tribunal judiciaire et "
                    + "désignation d'un notaire commis par le juge. Voir "
                    + "l'outil F-FA-17-partage-judiciaire.");
        } else if (!consentementsTous) {
            alertes.add("L'accord de tous les cohéritiers n'est pas "
                    + "expressément documenté. Vérifier le consentement "
                    + "écrit de chaque héritier avant la signature de "
                    + "l'acte de partage — un seul refus rend la "
                    + "procédure amiable impossible et impose la voie "
                    + "judiciaire (art. 840 Cciv).");
        }

        // 3. Calendrier des 5 étapes.
        List<String> calendrierEtapes = new ArrayList<>();
        calendrierEtapes.add(
                notaireDesigne
                        ? "1. Désignation du notaire (DÉJÀ FAITE) — "
                                + "vérifier le mandat de chaque héritier "
                                + "et signer la convention de partage "
                                + "amiable préparatoire."
                        : "1. Désignation du notaire — choisi d'un commun "
                                + "accord par les cohéritiers, ou par le "
                                + "défunt si désigné dans le testament. "
                                + "Établissement de la convention de "
                                + "partage amiable préparatoire.");
        calendrierEtapes.add(
                "2. Bilan patrimonial — inventaire complet des biens "
                        + "(meubles, immeubles, comptes, titres) et "
                        + "des dettes successorales. Évaluation des "
                        + "biens au jour du partage (art. 829 Cciv).");
        if (presenceImmeuble) {
            calendrierEtapes.add(
                    "3. Attestation immobilière après décès — acte "
                            + "notarié obligatoire pour la publication "
                            + "au fichier immobilier (décret n° 55-22 "
                            + "du 04/01/1955), à régulariser dans les "
                            + "6 mois du décès.");
        } else {
            calendrierEtapes.add(
                    "3. (Sans objet — pas d'immeuble) Attestation après "
                            + "décès non requise. Étape passée si la "
                            + "succession ne comprend que des meubles, "
                            + "comptes ou titres.");
        }
        calendrierEtapes.add(
                "4. Projet de partage — le notaire prépare un projet de "
                        + "répartition des biens entre cohéritiers, en "
                        + "tenant compte des parts légales (art. 734 et "
                        + "s. Cciv), des éventuelles attributions "
                        + "préférentielles (art. 831 et s. Cciv) et des "
                        + "soultes nécessaires pour équilibrer les lots.");
        calendrierEtapes.add(
                "5. Signature de l'acte de partage — tous les cohéritiers "
                        + "présents ou représentés (procuration notariée). "
                        + "L'acte de partage notarié vaut titre définitif "
                        + "(art. 838 Cciv) et permet la publication "
                        + "foncière des immeubles attribués.");

        // 4. Délai déclaration fiscale (art. 641 CGI — 6 mois métropole).
        LocalDate delaiDeclarationFiscale = req.declarationSuccessionEcheance();
        if (delaiDeclarationFiscale == null && req.dateOuvertureSuccession() != null) {
            delaiDeclarationFiscale = req.dateOuvertureSuccession()
                    .plusMonths(DELAI_DECLARATION_FISCALE_MOIS);
        }

        boolean alerteDelai = false;
        if (delaiDeclarationFiscale != null && today != null) {
            if (today.isAfter(delaiDeclarationFiscale)) {
                alerteDelai = true;
                alertes.add("Délai fiscal dépassé : la déclaration de "
                        + "succession devait être déposée auprès du "
                        + "service des impôts dans les 6 mois du décès "
                        + "(art. 641 CGI). Échéance : "
                        + delaiDeclarationFiscale
                        + ". Des pénalités de retard et intérêts s'appliquent — "
                        + "régulariser sans attendre (art. 1727 CGI).");
            } else {
                long joursRestants = java.time.temporal.ChronoUnit.DAYS
                        .between(today, delaiDeclarationFiscale);
                if (joursRestants <= 30) {
                    alertes.add("Délai fiscal proche : la déclaration "
                            + "de succession doit être déposée avant le "
                            + delaiDeclarationFiscale
                            + " (art. 641 CGI — 6 mois du décès). "
                            + "Reste " + joursRestants + " jour(s).");
                } else {
                    messages.add("Déclaration de succession à déposer "
                            + "avant le " + delaiDeclarationFiscale
                            + " (art. 641 CGI — 6 mois du décès).");
                }
            }
        }

        // 5. Vigilance complémentaire — nombre élevé de cohéritiers.
        if (req.nombreCoheritiers() != null && req.nombreCoheritiers() > 5) {
            messages.add("Succession à " + req.nombreCoheritiers()
                    + " cohéritiers : prévoir un projet de partage "
                    + "particulièrement détaillé. Au-delà de 5 héritiers, "
                    + "les attributions préférentielles (art. 831 Cciv) "
                    + "et les soultes deviennent fréquentes — anticiper "
                    + "la collecte des accords individuels.");
        }

        return new PartageNotarialResult(
                notaireObligatoire,
                calendrierEtapes,
                delaiDeclarationFiscale,
                alerteDelai,
                orientationJudiciaire,
                BASE_JURIDIQUE,
                messages,
                alertes);
    }

    /** Overload pratique avec {@code today = LocalDate.now()}. */
    public static PartageNotarialResult compute(
            PartageNotarialRequest req, String country) {
        return compute(req, country, LocalDate.now());
    }
}
