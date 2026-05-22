package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-07 : calculateur statique de la voie de recouvrement ARIPA d'une
 * pension alimentaire impayée (art. L. 581-1 à L. 581-14 CSS + art. L. 582-1
 * CSS + art. L. 523-1 à L. 523-6 CSS + Ordonnance n°2002-1358 du 20/11/2002
 * + Convention La Haye 23/11/2007 + Règlement UE n°4/2009).
 *
 * <p>Arbre décisionnel :</p>
 * <ol>
 *   <li>Si {@code !titreExecutoire} → {@link VoieRecouvrementAripaEnum#TITRE_REQUIS}
 *       (obtention préalable d'un titre indispensable, art. L. 581-2 CSS).</li>
 *   <li>Si {@code titreExecutoire} et débiteur résidant hors de France →
 *       {@link VoieRecouvrementAripaEnum#CONVENTION_INTERNATIONALE} (Convention La
 *       Haye 2007 / Règlement UE n°4/2009 — recours via le procureur ou autorité
 *       centrale).</li>
 *   <li>Si titre + débiteur en France + débiteur employeur public →
 *       {@link VoieRecouvrementAripaEnum#SATD} (saisie administrative à tiers
 *       détenteur, voie privilégiée car contrainte directe sur l'employeur public).</li>
 *   <li>Si titre + débiteur en France + débiteur salarié →
 *       {@link VoieRecouvrementAripaEnum#SAISIE_SUR_SALAIRE} (art. L. 582-1 CSS —
 *       procédure simplifiée de paiement direct).</li>
 *   <li>Sinon (titre + débiteur en France, autres situations) →
 *       {@link VoieRecouvrementAripaEnum#SDR_ARIPA} (Service de Demande de
 *       Recouvrement de la CAF/MSA, art. L. 581-1 CSS).</li>
 * </ol>
 *
 * <p>Allocation de Soutien Familial (ASF) — art. L. 523-1 CSS, montant
 * forfaitaire mensuel par enfant publié par la CAF. Le seuil et le montant
 * sont des constantes indicatives mises à jour annuellement. Si la pension
 * mensuelle par enfant est inférieure au montant ASF, le créancier peut
 * percevoir le différentiel à titre de complément CAF.</p>
 *
 * <p>Montant arriérés : pension × mois impayés. Indicatif uniquement —
 * l'ARIPA recouvre au plus 24 mois d'arriérés rétroactifs (art. L. 581-2 CSS
 * + Décret 2018-1109).</p>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, le recouvrement public des
 * pensions alimentaires passe par le SECAL (Service des Créances Alimentaires
 * du SPF Finances) — outil distinct (hors périmètre F-216).</p>
 */
public final class AripaRecouvrementFrCalculator {

    static final String BASE_JURIDIQUE =
            "art. L. 581-1 à L. 581-14 CSS + art. L. 582-1 CSS + art. L. 523-1 à L. 523-6 CSS"
                    + " + Ordonnance n°2002-1358 du 20/11/2002 (ARIPA)"
                    + " + Convention La Haye 23/11/2007"
                    + " + Règlement UE n°4/2009";

    /**
     * Montant ASF mensuel par enfant — barème CAF (indicatif, mis à jour
     * annuellement par décret). Valeur 2025 : 195 € / enfant / mois (art.
     * L. 523-1 CSS, taux plein).
     */
    static final int MONTANT_ASF_MENSUEL_PAR_ENFANT_EUR = 195;

    /**
     * Plafond ARIPA pour les arriérés rétroactifs — Décret 2018-1109 du 10/12/2018
     * (24 mois maximum). Utilisé pour informer l'avocat dans les messages.
     */
    static final int PLAFOND_ARRIERES_MOIS = 24;

    private AripaRecouvrementFrCalculator() {}

    /**
     * Calcule la voie de recouvrement recommandée et les paramètres associés.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static AripaRecouvrementFrResult compute(AripaRecouvrementFrRequest req, String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-ARIPA-RECOUVREMENT applicable uniquement en France (art. L. 581-1 CSS).");
        }

        List<String> etapes = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        int montantPension = nz(req.montantPensionMensuelleEur());
        int moisImpayes = nz(req.nombreMoisImpayes());
        int nbEnfants = Math.max(1, nz(req.nombreEnfantsACharge())); // sécurité — au moins 1 si on en arrive là
        boolean titre = Boolean.TRUE.equals(req.titreExecutoire());
        boolean debiteurFr = Boolean.TRUE.equals(req.debiteurEnFrance());
        boolean debiteurEmployeurPublic = Boolean.TRUE.equals(req.debiteurEmployeurPublic());

        // 1. Voie recommandée
        VoieRecouvrementAripaEnum voie;
        int delaiJours;

        if (!titre) {
            voie = VoieRecouvrementAripaEnum.TITRE_REQUIS;
            delaiJours = 0;
            etapes.add("Obtenir un titre exécutoire : décision JAF, convention de divorce par "
                    + "consentement mutuel notariée, ou acte notarié constatant la pension "
                    + "(art. L. 581-2 CSS).");
            etapes.add("Si urgence et créance non contestée : référé provision (art. 835 CPC) ou "
                    + "injonction de payer (art. 1405 CPC).");
            etapes.add("Une fois le titre obtenu, revenir à cet outil pour orienter la voie de "
                    + "recouvrement ARIPA.");
            alertes.add("Sans titre exécutoire, l'ARIPA ne peut être saisie. C'est le pré-requis "
                    + "bloquant n°1 (art. L. 581-2 CSS).");
        } else if (!debiteurFr) {
            voie = VoieRecouvrementAripaEnum.CONVENTION_INTERNATIONALE;
            delaiJours = 180; // procédures internationales lentes
            etapes.add("Vérifier si l'État de résidence du débiteur est lié à la France par la "
                    + "Convention de La Haye du 23/11/2007 (recouvrement international des "
                    + "aliments) ou par le Règlement UE n°4/2009 (UE).");
            etapes.add("Saisir le Bureau du Recouvrement de Créances Alimentaires (BRCA) du "
                    + "ministère des Affaires étrangères via le procureur de la République (art. 4 "
                    + "Conv. La Haye 2007) — autorité centrale française désignée.");
            etapes.add("Préparer la traduction certifiée du titre exécutoire + l'attestation de "
                    + "non-paiement (CAF / huissier).");
            etapes.add("En parallèle, vérifier la possibilité d'exequatur du jugement français "
                    + "dans l'État de résidence du débiteur si convention ad hoc.");
            messages.add("Procédure internationale : compter 6 à 18 mois pour un premier "
                    + "recouvrement effectif.");
            alertes.add("Vérifier dès maintenant que le titre exécutoire mentionne expressément "
                    + "le montant en euros (et non en monnaie locale ancienne) — sinon, demander "
                    + "un complément au juge avant transmission internationale.");
        } else if (debiteurEmployeurPublic) {
            voie = VoieRecouvrementAripaEnum.SATD;
            delaiJours = 60;
            etapes.add("Saisir la CAF (ou MSA) pour ouvrir un dossier SDR ARIPA — l'organisme "
                    + "demandera lui-même la SATD à l'employeur public.");
            etapes.add("Préparer : titre exécutoire (original + copie certifiée), justificatifs "
                    + "d'impayés (relevés bancaires, mises en demeure), pièce d'identité, RIB.");
            etapes.add("Suivi : la CAF notifie la SATD à l'employeur public dans un délai de "
                    + "15 à 30 jours. Premier prélèvement environ 30 à 45 jours après notification.");
            messages.add("SATD (saisie administrative à tiers détenteur) : voie privilégiée quand "
                    + "le débiteur est employé par une administration publique — contrainte directe.");
        } else {
            SituationAripaEnum sitDebiteur = req.situationDebiteur();
            if (sitDebiteur == SituationAripaEnum.SALARIE) {
                voie = VoieRecouvrementAripaEnum.SAISIE_SUR_SALAIRE;
                delaiJours = 45;
                etapes.add("Saisir la CAF (ou MSA) pour ouvrir un dossier SDR ARIPA (Service de "
                        + "Demande de Recouvrement) — démarche dématérialisée via "
                        + "pension-alimentaire.caf.fr.");
                etapes.add("La CAF déclenche la procédure simplifiée de paiement direct prévue à "
                        + "l'art. L. 582-1 CSS : notification à l'employeur du débiteur, qui "
                        + "verse directement la pension au créancier.");
                etapes.add("Préparer : titre exécutoire, justificatifs d'impayés, RIB, "
                        + "coordonnées employeur du débiteur si connues.");
                etapes.add("Délai indicatif : 30 à 60 jours pour les premiers versements.");
                messages.add("Saisie sur salaire (art. L. 582-1 CSS) : voie la plus rapide quand "
                        + "le débiteur est salarié et l'employeur identifié.");
            } else {
                voie = VoieRecouvrementAripaEnum.SDR_ARIPA;
                delaiJours = 90;
                etapes.add("Saisir la CAF (ou MSA) pour ouvrir un dossier SDR ARIPA via "
                        + "pension-alimentaire.caf.fr — l'ARIPA prend en charge le recouvrement "
                        + "auprès du débiteur (art. L. 581-1 CSS).");
                etapes.add("L'ARIPA dispose des prérogatives du Trésor public : avis à tiers "
                        + "détenteur sur comptes bancaires, prélèvement sur prestations sociales, "
                        + "saisie sur biens mobiliers.");
                etapes.add("Préparer : titre exécutoire (jugement / convention CM notariée / "
                        + "acte notarié), justificatifs d'impayés (au moins 1 mois), RIB, "
                        + "dernières adresses connues du débiteur.");
                etapes.add("Délai indicatif : 60 à 120 jours pour les premiers résultats — "
                        + "dépend de la solvabilité du débiteur.");
                if (sitDebiteur == SituationAripaEnum.SANS_ACTIVITE
                        || sitDebiteur == SituationAripaEnum.CHOMEUR) {
                    alertes.add("Débiteur sans revenus salariés identifiés : le recouvrement peut "
                            + "être long et partiel. Demander à l'ARIPA d'investiguer "
                            + "(comptes bancaires, prestations sociales). Étudier en parallèle la "
                            + "demande d'ASF (Allocation de Soutien Familial, art. L. 523-1 CSS) "
                            + "pour assurer un revenu minimum au créancier.");
                } else if (sitDebiteur == SituationAripaEnum.INCONNU) {
                    alertes.add("Situation du débiteur inconnue : l'ARIPA dispose du Fichier "
                            + "national des comptes bancaires (FICOBA) et du droit de "
                            + "communication. Saisir l'organisme rapidement pour bénéficier de "
                            + "ses pouvoirs d'investigation.");
                }
            }
        }

        // 2. Montant des arriérés (indicatif, plafonné mentalement à 24 mois)
        int montantArrieres = montantPension * moisImpayes;
        if (moisImpayes > PLAFOND_ARRIERES_MOIS) {
            messages.add("Arriérés > " + PLAFOND_ARRIERES_MOIS + " mois : l'ARIPA recouvre "
                    + "au plus 24 mois rétroactifs (Décret 2018-1109). Au-delà, la créance reste "
                    + "exigible directement contre le débiteur (huissier) pendant 5 ans (art. "
                    + "2224 Cciv).");
        }

        // 3. Calcul ASF — éligible si pension mensuelle par enfant < montant ASF
        int montantAsfMensuel = 0;
        if (titre && debiteurFr) {
            int pensionParEnfant = montantPension / nbEnfants;
            if (pensionParEnfant < MONTANT_ASF_MENSUEL_PAR_ENFANT_EUR) {
                int delta = MONTANT_ASF_MENSUEL_PAR_ENFANT_EUR - pensionParEnfant;
                montantAsfMensuel = delta * nbEnfants;
                messages.add("Pension par enfant (" + pensionParEnfant + " €/mois) inférieure au "
                        + "montant ASF (" + MONTANT_ASF_MENSUEL_PAR_ENFANT_EUR + " €/mois) — "
                        + "complément ASF mensuel théorique : " + montantAsfMensuel + " € (art. "
                        + "L. 523-1 CSS, taux plein 2025).");
                etapes.add("Demander l'Allocation de Soutien Familial (ASF) différentielle à la "
                        + "CAF — complément automatique quand l'ARIPA est saisie (art. L. 523-1 "
                        + "CSS).");
            } else {
                messages.add("Pension par enfant >= " + MONTANT_ASF_MENSUEL_PAR_ENFANT_EUR
                        + " €/mois : pas d'ASF complémentaire (le créancier reste éligible à "
                        + "l'ASF à titre d'avance recouvrable en cas d'impayé).");
            }

            // ASF recouvrable : en cas d'impayé total, la CAF avance l'ASF taux plein
            // et la recouvre ensuite auprès du débiteur (art. L. 581-2 CSS).
            if (montantPension == 0 || moisImpayes >= 2) {
                messages.add("En cas d'impayé total (≥ 2 mois), la CAF peut verser une avance "
                        + "ASF \"recouvrable\" (taux plein " + MONTANT_ASF_MENSUEL_PAR_ENFANT_EUR
                        + " € × " + nbEnfants + " enfant(s) = "
                        + (MONTANT_ASF_MENSUEL_PAR_ENFANT_EUR * nbEnfants) + " €/mois) qu'elle "
                        + "recouvrera ensuite contre le débiteur — art. L. 581-2 CSS.");
            }
        }

        // 4. Messages contextuels
        if (titre && debiteurFr && voie != VoieRecouvrementAripaEnum.TITRE_REQUIS) {
            messages.add("Service en ligne : pension-alimentaire.caf.fr — dépôt dématérialisé du "
                    + "SDR ARIPA, suivi du dossier et téléchargement de l'attestation d'impayé.");
        }
        if (moisImpayes >= 2) {
            messages.add("Impayé caractérisé (≥ 2 mois consécutifs ou non) : l'avocat peut, en "
                    + "parallèle du recouvrement civil ARIPA, envisager le délit d'abandon de "
                    + "famille (art. 227-3 CP — pénal, hors scope outil).");
        }

        return new AripaRecouvrementFrResult(
                voie,
                montantArrieres,
                montantAsfMensuel,
                delaiJours,
                etapes,
                BASE_JURIDIQUE,
                messages,
                alertes
        );
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
