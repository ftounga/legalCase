package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-03 : calculateur statique de la pension alimentaire enfant FR
 * (art. 371-2 Cciv + barème indicatif Cour de cassation 2010, révisé).
 *
 * <p>Règles de calcul :</p>
 * <ul>
 *   <li><b>Taux barème</b> appliqué aux revenus NETS MENSUELS du parent débiteur :
 *     <table>
 *       <tr><th>Nb enfants</th><th>Résidence classique</th><th>Résidence alternée</th></tr>
 *       <tr><td>1</td><td>18%</td><td>11%</td></tr>
 *       <tr><td>2</td><td>26%</td><td>16%</td></tr>
 *       <tr><td>3</td><td>30%</td><td>19%</td></tr>
 *       <tr><td>4</td><td>33%</td><td>21%</td></tr>
 *       <tr><td>5+</td><td>35%</td><td>22%</td></tr>
 *     </table>
 *     Taux indicatifs barème Cass. 2010 (révisé) — "classique" quand un parent
 *     héberge ; "alternée" quand la garde est partagée (50/50).
 *   </li>
 *   <li><b>Parent débiteur</b> : si {@code modeResidence == PRINCIPALE_PARENT1},
 *     parent 2 verse à parent 1 (taux × revenus parent 2). Si {@code PRINCIPALE_PARENT2},
 *     parent 1 verse à parent 2. Si {@code ALTERNEE}, le parent au revenu supérieur
 *     verse au parent au revenu inférieur (différentiel partagé).</li>
 *   <li><b>Répartition entre enfants</b> : montant total divisé à parts égales par
 *     défaut. Les ajustements par âge/charges sont signalés via {@code messages}.</li>
 *   <li><b>Charges extraordinaires</b> (frais scolaires, médicaux) : non incluses
 *     dans le calcul indicatif — alerte ajoutée pour adaptation par l'avocat
 *     (art. 371-2 Cciv al. 2).</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, le calcul SECAL (CGKR) est
 * distinct — outil BE séparé.</p>
 *
 * <p>AVERTISSEMENT JURIDIQUE : le barème est indicatif et non contraignant. Le
 * Juge aux Affaires Familiales (JAF) fixe librement la contribution en fonction
 * des ressources et besoins de chacun (art. 371-2 al. 3 Cciv).</p>
 */
public final class PensionAlimentaireEnfantFrCalculator {

    static final String BASE_JURIDIQUE =
            "art. 371-2 Cciv + barème indicatif Cour de cassation (2010, révisé) + art. 373-2-2 Cciv (révision)";

    /** Taux barème indicatif Cass. — index = min(nbEnfants, 5) - 1. */
    private static final double[][] TAUX_BAREME = {
            // {résidence classique, résidence alternée}
            { 0.18, 0.11 }, // 1 enfant
            { 0.26, 0.16 }, // 2 enfants
            { 0.30, 0.19 }, // 3 enfants
            { 0.33, 0.21 }, // 4 enfants
            { 0.35, 0.22 }, // 5+ enfants
    };

    private static final int MAX_ENFANTS = 12;

    private PensionAlimentaireEnfantFrCalculator() {}

    /**
     * Calcule la pension alimentaire enfant indicative.
     *
     * @param req     requête validée (gates pays déjà vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si les pré-requis pays ne sont pas respectés
     *                                  ou si données invalides
     */
    public static PensionAlimentaireEnfantFrResult compute(
            PensionAlimentaireEnfantFrRequest req,
            String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-02 applicable uniquement en France (art. 371-2 Cciv + barème indicatif Cass.).");
        }
        if (req == null) {
            throw new IllegalArgumentException("Corps de requête manquant.");
        }
        if (req.nombreEnfants() == null || req.nombreEnfants() < 1 || req.nombreEnfants() > MAX_ENFANTS) {
            throw new IllegalArgumentException(
                    "Le nombre d'enfants doit être compris entre 1 et " + MAX_ENFANTS + ".");
        }
        if (req.agesEnfants() == null || req.agesEnfants().size() != req.nombreEnfants()) {
            throw new IllegalArgumentException(
                    "La liste des âges doit contenir " + req.nombreEnfants() + " entrée(s).");
        }
        if (req.modeResidence() == null) {
            throw new IllegalArgumentException("Le mode de résidence est requis.");
        }

        int revenus1 = nz(req.revenusNetsParent1Eur());
        int revenus2 = nz(req.revenusNetsParent2Eur());
        if (revenus1 < 0 || revenus2 < 0) {
            throw new IllegalArgumentException("Les revenus nets ne peuvent être négatifs.");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        boolean alternee = req.modeResidence() == ModeResidenceEnfantEnum.ALTERNEE;
        int idx = Math.min(req.nombreEnfants(), 5) - 1;
        double taux = alternee ? TAUX_BAREME[idx][1] : TAUX_BAREME[idx][0];
        double coefficient = alternee ? 0.5 : 1.0;

        // Détermination du parent débiteur selon le mode de résidence et les revenus.
        int revenusDebiteur;
        String parentDebiteur;
        switch (req.modeResidence()) {
            case PRINCIPALE_PARENT1 -> {
                // Parent 1 héberge → parent 2 est débiteur de la pension.
                revenusDebiteur = revenus2;
                parentDebiteur = "PARENT2";
            }
            case PRINCIPALE_PARENT2 -> {
                // Parent 2 héberge → parent 1 est débiteur.
                revenusDebiteur = revenus1;
                parentDebiteur = "PARENT1";
            }
            case ALTERNEE -> {
                // En garde alternée, le parent au revenu le plus élevé verse au parent au
                // revenu le plus faible un différentiel basé sur le taux alterné.
                if (revenus1 == revenus2) {
                    revenusDebiteur = 0;
                    parentDebiteur = "AUCUN";
                    messages.add("Garde alternée avec revenus comparables — pas de pension indicative due "
                            + "(art. 373-2-2 Cciv : la contribution peut prendre la forme du partage direct des frais).");
                } else if (revenus1 > revenus2) {
                    revenusDebiteur = revenus1 - revenus2;
                    parentDebiteur = "PARENT1";
                } else {
                    revenusDebiteur = revenus2 - revenus1;
                    parentDebiteur = "PARENT2";
                }
            }
            default -> throw new IllegalArgumentException("Mode de résidence non supporté : " + req.modeResidence());
        }

        // Montant total mensuel = taux × revenus du débiteur.
        int totalMensuel = (int) Math.round(revenusDebiteur * taux);
        if (totalMensuel < 0) totalMensuel = 0;

        // Répartition à parts égales entre enfants (le delta de centimes va au dernier).
        int n = req.nombreEnfants();
        List<Integer> montantsParEnfant = new ArrayList<>(n);
        int montantUnitaire = totalMensuel / n;
        int reste = totalMensuel - montantUnitaire * n;
        for (int i = 0; i < n; i++) {
            int m = montantUnitaire + (i == n - 1 ? reste : 0);
            montantsParEnfant.add(m);
        }

        // Messages contextuels.
        if (revenusDebiteur == 0 && !"AUCUN".equals(parentDebiteur)) {
            alertes.add("Revenus du parent débiteur = 0 — montant indicatif nul, "
                    + "à confirmer par la déclaration de ressources (art. 272 CPC).");
        }
        if (req.nombreEnfants() >= 5) {
            messages.add("Au-delà de 5 enfants, le taux barème plafonne — la pension peut nécessiter "
                    + "un ajustement spécifique par le JAF (art. 371-2 al. 3 Cciv).");
        }
        if (req.agesEnfants().stream().anyMatch(age -> age != null && age >= 18)) {
            messages.add("Un ou plusieurs enfants majeurs détectés — la pension peut se poursuivre tant que "
                    + "l'enfant n'est pas autonome (art. 371-2 al. 2 Cciv) ; vérifier la poursuite d'études.");
        }
        if (alternee) {
            messages.add("Mode résidence alternée — montant réduit (coefficient 0,5) ; le partage direct "
                    + "des frais (cantine, scolarité, santé) peut compléter ou remplacer la pension.");
        }
        alertes.add("Charges extraordinaires non comprises (frais scolaires, médicaux, activités) — "
                + "à prévoir distinctement (art. 371-2 al. 2 Cciv).");
        messages.add("Montant indicatif — le JAF fixe librement la contribution en fonction des ressources "
                + "et besoins (art. 371-2 al. 3 Cciv + Cass. 1ère civ. 22/03/2017 n°16-13.946).");

        return new PensionAlimentaireEnfantFrResult(
                montantsParEnfant,
                totalMensuel,
                taux,
                coefficient,
                parentDebiteur,
                BASE_JURIDIQUE,
                messages,
                alertes
        );
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
