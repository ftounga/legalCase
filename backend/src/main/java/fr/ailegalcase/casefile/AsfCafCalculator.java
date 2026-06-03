package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-222-01 : calculateur statique de l'allocation de soutien familial (ASF),
 * art. L. 523-1 CSS, Famille FRANCE uniquement.
 *
 * <p>Logique (4 verdicts) :</p>
 * <ul>
 *   <li><b>PAS_DE_DROIT</b> si {@code parentIsole=false} ET pas de cas décès / inconnu —
 *       l'ASF est réservée au parent isolé (sauf décès / parent inconnu).</li>
 *   <li><b>DROIT_ASF_PLEIN</b> (montant plein par enfant) si {@code autreParentDecedeOuInconnu}
 *       OU pension non fixée (pas de titre exécutoire mobilisable).</li>
 *   <li><b>DROIT_ASF_DIFFERENTIELLE</b> si pension fixée payée partiellement et inférieure
 *       au montant ASF — la CAF complète jusqu'au montant plancher.</li>
 *   <li><b>DROIT_AVEC_RECOUVREMENT</b> si pension fixée mais non payée — la CAF verse l'ASF
 *       et se subroge (recouvrement ARIPA). {@code recouvrementApplicable=true} +
 *       renvoi vers {@code F-FA-ARIPA-RECOUVREMENT}.</li>
 * </ul>
 *
 * <p>Anti-doublon : ce calculateur ne chiffre pas le recouvrement (arriérés, voie
 * de recouvrement) — il se borne au droit + montant de la prestation CAF.</p>
 *
 * <p>Montants 2026 indicatifs (à paramétrer en référentiel) :</p>
 * <ul>
 *   <li>ASF taux plein : {@value #ASF_TAUX_PLEIN_PAR_ENFANT_EUR} €/enfant/mois.</li>
 * </ul>
 */
public final class AsfCafCalculator {

    /** Montant ASF taux plein indicatif 2026 par enfant et par mois (art. L. 523-1 CSS). */
    static final int ASF_TAUX_PLEIN_PAR_ENFANT_EUR = 187;

    static final String BASE_L523 = "art. L. 523-1 CSS (allocation de soutien familial)";
    static final String BASE_PARENT_ISOLE = "art. L. 523-1 1° CSS (parent isolé assumant seul la charge de l'enfant)";
    static final String BASE_SUBROGATION = "art. L. 581-2 CSS (subrogation de la CAF / ARIPA pour pension impayée)";
    static final String BASE_DECES_INCONNU = "art. L. 523-1 2° / 3° CSS (autre parent décédé, hors d'état ou non identifié)";

    private AsfCafCalculator() {}

    /**
     * Calcule le droit et le montant indicatif de l'ASF.
     *
     * @param req     requête validée (gate pays vérifié par le service ; nbEnfants ≥ 1)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si les pré-requis pays ne sont pas respectés
     */
    public static AsfCafResult compute(AsfCafRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-ASF-CAF applicable uniquement en France (art. L. 523-1 CSS).");
        }

        boolean parentIsole = Boolean.TRUE.equals(req.parentIsole());
        boolean pensionFixee = Boolean.TRUE.equals(req.pensionFixee());
        boolean decesOuInconnu = Boolean.TRUE.equals(req.autreParentDecedeOuInconnu());
        PensionPayeeEnum pensionPayee = req.pensionPayee();
        int nbEnfants = req.nbEnfantsACharge() == null ? 0 : req.nbEnfantsACharge();

        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        bases.add(BASE_L523);

        int montantPlein = ASF_TAUX_PLEIN_PAR_ENFANT_EUR * Math.max(nbEnfants, 0);

        // 1. Cas décès / parent inconnu — ASF taux plein, sans condition de parent isolé.
        if (decesOuInconnu) {
            bases.add(BASE_DECES_INCONNU);
            messages.add("Autre parent décédé, hors d'état ou non identifié : ASF à taux plein due, "
                    + "sans recouvrement possible (pas de débiteur identifié).");
            messages.add(montantMessage(nbEnfants, montantPlein));
            return new AsfCafResult(VerdictAsfEnum.DROIT_ASF_PLEIN, montantPlein, false, bases, messages);
        }

        // 2. ASF réservée au parent isolé (hors cas décès / inconnu déjà traité).
        if (!parentIsole) {
            messages.add("L'ASF est réservée au parent isolé assumant seul la charge effective de l'enfant "
                    + "(art. L. 523-1 1° CSS), sauf décès ou parent non identifié. Aucun droit en l'état.");
            return new AsfCafResult(VerdictAsfEnum.PAS_DE_DROIT, 0, false, bases, messages);
        }
        bases.add(BASE_PARENT_ISOLE);

        // 3. Pension non fixée (aucun titre mobilisable) — ASF taux plein (non recouvrable).
        if (!pensionFixee) {
            messages.add("Aucune pension alimentaire fixée par titre exécutoire : l'ASF est due à taux plein. "
                    + "Une action en fixation de la contribution à l'entretien (art. 371-2 Cciv) reste opportune.");
            messages.add(montantMessage(nbEnfants, montantPlein));
            return new AsfCafResult(VerdictAsfEnum.DROIT_ASF_PLEIN, montantPlein, false, bases, messages);
        }

        // 4. Pension fixée — l'état de paiement détermine le verdict.
        PensionPayeeEnum etat = pensionPayee == null ? PensionPayeeEnum.NON_PAYEE : pensionPayee;
        switch (etat) {
            case PAYEE -> {
                messages.add("Pension alimentaire fixée et payée intégralement : pas de droit à l'ASF de ce chef. "
                        + "Une ASF différentielle reste possible si le montant payé est inférieur au montant ASF.");
                return new AsfCafResult(VerdictAsfEnum.PAS_DE_DROIT, 0, false, bases, messages);
            }
            case PARTIELLE -> {
                int pension = req.montantPensionMensuel() == null ? 0 : Math.max(req.montantPensionMensuel(), 0);
                int complement = Math.max(montantPlein - pension, 0);
                if (complement <= 0) {
                    messages.add("Pension partielle (" + pension + " €/mois) supérieure ou égale au montant ASF ("
                            + montantPlein + " €/mois) : pas de complément différentiel dû.");
                    return new AsfCafResult(VerdictAsfEnum.PAS_DE_DROIT, 0, false, bases, messages);
                }
                messages.add("Pension fixée payée partiellement (" + pension + " €/mois) et inférieure au montant ASF ("
                        + montantPlein + " €/mois) : la CAF verse une ASF différentielle complétant jusqu'au plancher.");
                messages.add("Complément ASF différentiel estimé : " + complement + " €/mois.");
                return new AsfCafResult(VerdictAsfEnum.DROIT_ASF_DIFFERENTIELLE, complement, false, bases, messages);
            }
            case NON_PAYEE -> {
                bases.add(BASE_SUBROGATION);
                messages.add("Pension fixée mais non payée : la CAF verse l'ASF à titre d'avance et se subroge "
                        + "dans le recouvrement de la pension impayée.");
                messages.add(montantMessage(nbEnfants, montantPlein));
                messages.add("Recouvrement forcé de la pension : voir l'outil ARIPA recouvrement "
                        + "(F-FA-ARIPA-RECOUVREMENT) — cet outil ne calcule pas le recouvrement.");
                return new AsfCafResult(VerdictAsfEnum.DROIT_AVEC_RECOUVREMENT, montantPlein, true, bases, messages);
            }
            default -> {
                // Inatteignable (enum exhaustif), garde-fou défensif.
                return new AsfCafResult(VerdictAsfEnum.PAS_DE_DROIT, 0, false, bases, messages);
            }
        }
    }

    private static String montantMessage(int nbEnfants, int montant) {
        return "Montant ASF indicatif : " + ASF_TAUX_PLEIN_PAR_ENFANT_EUR + " €/enfant/mois × "
                + nbEnfants + " enfant(s) = " + montant + " €/mois (barème 2026 indicatif — montant définitif fixé par la CAF).";
    }
}
