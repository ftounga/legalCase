package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-37 : moteur de préparation de la phase de conciliation au Bureau de
 * Conciliation et d'Orientation (BCO) du Conseil de Prud'hommes
 * (F-DT-84-conciliation-cph-bca, FRANCE — R. 1454-7 à R. 1454-12 CT ;
 * L. 1235-1 CT al. 3 — barème transactions BCA ; formulaire D4121-1 CPC ;
 * L. 1454-1+ CT — composition et compétences du BCO).
 *
 * <p>Le BCO est la première étape obligatoire de toute procédure prud'homale
 * (sauf cas légaux dispensant de conciliation : référé, action en
 * reconnaissance d'un contrat de travail, requalification CDD en CDI...).
 * Deux issues : conciliation (accord constaté par procès-verbal, exécutoire)
 * ou orientation (renvoi devant le Bureau de Jugement ou la formation de
 * référé). En cas d'accord, le PV homologué par le BCO a force exécutoire
 * (R. 1454-12 CT).</p>
 *
 * <p><b>Barème de transactions BCA</b> (L. 1235-1 al. 3 CT) :</p>
 * <ul>
 *   <li>&lt; 2 ans &rarr; 2 mois minimum</li>
 *   <li>2 &le; ancienneté &lt; 8 ans &rarr; 3 mois minimum</li>
 *   <li>8 &le; ancienneté &lt; 15 ans &rarr; 4 mois minimum</li>
 *   <li>15 &le; ancienneté &lt; 25 ans &rarr; 8 mois minimum</li>
 *   <li>&ge; 25 ans &rarr; 14 mois minimum</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la procédure prud'homale est
 * strictement française. La BE dispose d'un régime distinct (tribunal
 * du travail, chambre de conciliation, art. 734 et s. CJ), hors périmètre.</p>
 */
public final class ConciliationCphBcaCalculator {

    /** Palier de barème BCA — L. 1235-1 al. 3 CT. */
    public enum PalierBca {
        MOINS_DE_2_ANS("< 2 ans : 2 mois", 2),
        DE_2_A_8_ANS("2 à 8 ans : 3 mois", 3),
        DE_8_A_15_ANS("8 à 15 ans : 4 mois", 4),
        DE_15_A_25_ANS("15 à 25 ans : 8 mois", 8),
        AU_MOINS_25_ANS("≥ 25 ans : 14 mois", 14);

        private final String libelle;
        private final int moisMinimum;

        PalierBca(String libelle, int moisMinimum) {
            this.libelle = libelle;
            this.moisMinimum = moisMinimum;
        }

        public String libelle() { return libelle; }
        public int moisMinimum() { return moisMinimum; }
    }

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            double montantMinimumBcaEuros,
            String palierBcaApplicable,
            String comparaisonBcaVsMacron,
            List<String> checklistBco,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private ConciliationCphBcaCalculator() {}

    /**
     * Palier BCA applicable selon l'ancienneté en mois (L. 1235-1 al. 3 CT).
     * Les bornes sont inclusives à gauche, exclusives à droite, sauf le
     * dernier palier qui couvre tout au-delà de 25 ans.
     */
    public static PalierBca palierBcaApplicable(int ancienneteMois) {
        int ans = ancienneteMois / 12;
        if (ans < 2) return PalierBca.MOINS_DE_2_ANS;
        if (ans < 8) return PalierBca.DE_2_A_8_ANS;
        if (ans < 15) return PalierBca.DE_8_A_15_ANS;
        if (ans < 25) return PalierBca.DE_15_A_25_ANS;
        return PalierBca.AU_MOINS_25_ANS;
    }

    /**
     * Point d'entrée principal.
     *
     * @param input     données saisies par l'avocat
     * @param country   pays du workspace
     * @return résultat calculé (montant minimum BCA, palier, checklist BCO)
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null.
     */
    public static Result compute(ConciliationCphBcaInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE (BCO CPH — R. 1454-7+ CT ; L. 1235-1 al. 3 CT) — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }

        int ancienneteMois = input.ancienneteMois() != null ? input.ancienneteMois() : 0;
        double salaire = input.salaireMensuelBrutEuros() != null ? input.salaireMensuelBrutEuros() : 0.0;
        double demandes = input.montantDemandesEuros() != null ? input.montantDemandesEuros() : 0.0;

        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // ── 1. Palier BCA & montant minimum ─────────────────────────────────
        PalierBca palier = palierBcaApplicable(ancienneteMois);
        double montantMinimumBca = Math.round(palier.moisMinimum() * salaire * 100.0) / 100.0;

        bases.add("L. 1235-1 al. 3 CT — barème de transactions BCA (seuils minimaux selon ancienneté)");
        bases.add("R. 1454-7 à R. 1454-12 CT — composition et procédure du Bureau de Conciliation et d'Orientation");
        bases.add("L. 1411-1 CT — compétence du Conseil de Prud'hommes");
        messages.add(String.format(
                "Palier BCA applicable : %s. Montant minimum d'indemnité BCA : %.2f € (= %d mois × %.2f €).",
                palier.libelle(), montantMinimumBca, palier.moisMinimum(), salaire));

        // ── 2. Comparaison BCA vs barème Macron ─────────────────────────────
        String comparaison;
        if (demandes <= 0) {
            comparaison = "Aucune comparaison effectuée : montant des demandes non renseigné. Le BCA minimum reste un plancher légal négociable à la hausse.";
        } else if (montantMinimumBca >= demandes) {
            comparaison = String.format(
                    "Le BCA minimum (%.2f €) est supérieur ou égal au montant des demandes (%.2f €) — la conciliation au BCO sécurise au moins l'équivalent du contentieux et évite l'aléa du Bureau de Jugement.",
                    montantMinimumBca, demandes);
        } else {
            double ecart = demandes - montantMinimumBca;
            double tauxEcart = (ecart / demandes) * 100.0;
            comparaison = String.format(
                    "Le BCA minimum (%.2f €) est inférieur au montant des demandes (%.2f €) — écart de %.2f € (%.0f %%). Arbitrer entre la sécurité du PV homologué exécutoire et l'espérance d'obtenir le barème Macron complet devant le BJ (aléa : motif réel et sérieux, prescription, force des preuves).",
                    montantMinimumBca, demandes, ecart, tauxEcart);
        }
        bases.add("L. 1235-3 CT — barème Macron des indemnités pour licenciement sans cause réelle et sérieuse (référence de comparaison)");

        // ── 3. Opportunité avocat ───────────────────────────────────────────
        ConciliationCphBcaInput.ConciliationCphBcaOpportunite opp = input.opportuniteConciliation();
        if (opp == null) {
            opp = ConciliationCphBcaInput.ConciliationCphBcaOpportunite.INCERTAIN;
        }
        switch (opp) {
            case FAVORABLE -> messages.add(
                    "Opportunité jugée FAVORABLE par l'avocat — orienter le BCO vers un PV de conciliation (homologation R. 1454-12 CT, exécutoire de plein droit).");
            case DEFAVORABLE -> messages.add(
                    "Opportunité jugée DÉFAVORABLE par l'avocat — préparer l'orientation vers le Bureau de Jugement (renvoi sur le fond) ou la formation de référé en cas d'urgence.");
            case INCERTAIN -> messages.add(
                    "Opportunité jugée INCERTAINE — étudier les pièces produites au BCO (R. 1454-10 CT mise en état) et le bordereau de pièces du défendeur avant arbitrage.");
        }

        // ── 4. Checklist BCO ────────────────────────────────────────────────
        List<String> checklist = new ArrayList<>();
        checklist.add("Requête introductive (formulaire CERFA n°15586*10 ou équivalent — R. 1452-1 CT) déposée au greffe du CPH compétent.");
        checklist.add("Bordereau de pièces communiquées à la partie adverse (R. 1453-3 CT) — pièces numérotées, chronologie.");
        checklist.add("Contrat de travail + avenants éventuels + bulletins de paie 12 derniers mois + lettre de licenciement (ou rupture).");
        checklist.add("Justificatifs des préjudices invoqués : tableaux indemnitaires chiffrés (heures sup, congés payés, rappel salaire, dommages-intérêts).");
        checklist.add("Convention collective applicable + extraits pertinents (ancienneté, indemnités conventionnelles, préavis).");
        checklist.add("Convocation BCO réceptionnée par les deux parties (R. 1452-4 CT — au moins 15 jours avant l'audience).");
        checklist.add("Mandat de représentation à jour si l'avocat représente sans déplacement personnel du salarié (R. 1453-2 CT).");
        checklist.add("Projet de protocole transactionnel chiffré (montant indemnitaire global + clause de renonciation à toute action future), pour le cas où la conciliation aboutirait.");
        checklist.add("Synthèse argumentaire écrite remise au BCO (facultative mais recommandée — pratique professionnelle).");
        bases.add("R. 1452-1 à R. 1454-12 CT — procédure devant le Conseil de Prud'hommes (saisine, mise en état, conciliation)");
        bases.add("R. 1454-12 CT — homologation du PV de conciliation par le BCO, force exécutoire");

        return new Result(
                montantMinimumBca,
                palier.libelle(),
                comparaison,
                List.copyOf(checklist),
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
