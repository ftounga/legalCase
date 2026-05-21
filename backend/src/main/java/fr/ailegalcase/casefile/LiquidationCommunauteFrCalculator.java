package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-05 : calculateur statique de la liquidation de communauté légale FR
 * (art. 1467-1517 Cciv).
 *
 * <p>Règles de calcul :</p>
 * <ul>
 *   <li><b>Masse commune</b> = somme des actifs communs (2 immeubles + mobilier + autres)
 *       − passif commun (capital restant dû du crédit immobilier commun).</li>
 *   <li><b>Quote-part théorique</b> = masse commune / 2 (art. 1475 Cciv — partage égalitaire).</li>
 *   <li><b>Récompenses</b> (art. 1433-1435 Cciv) : ajoutées à la quote-part de l'époux
 *       créancier vis-à-vis de la communauté, retranchées de celle de l'époux débiteur.</li>
 *   <li><b>Soulte</b> : différence positive à compenser quand un époux reprend en nature
 *       un bien indivisible (ex. logement). Calculée = max(quote-part - bienAttribue, 0)
 *       suivant l'option choisie via {@code occupationLogementEpoux}.</li>
 *   <li><b>Alerte indivision</b> (art. 815-832 Cciv) : déclenchée si le partage n'aboutit
 *       pas à une attribution claire — ex. logement non attribué, désaccord apparent.</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, la liquidation est régie par
 * le livre 2, titre 3 du Code civil belge (outils F-217 distincts).</p>
 *
 * <p>Gate régime : uniquement COMMUNAUTE_LEGALE (régime supplétif). Les régimes
 * COMMUNAUTE_UNIVERSELLE, SEPARATION_BIENS, PARTICIPATION_ACQUETS sont rejetés
 * avec un message explicite.</p>
 */
public final class LiquidationCommunauteFrCalculator {

    static final String BASE_JURIDIQUE =
            "art. 1467-1517 Cciv + art. 1433-1435 Cciv (récompenses) + art. 815-832 Cciv (indivision)";
    static final String REGIME_REQUIS = "COMMUNAUTE_LEGALE";

    private LiquidationCommunauteFrCalculator() {}

    /**
     * Calcule la liquidation indicative.
     *
     * @param req     requête validée (gates pays/régime déjà vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si les pré-requis pays/régime ne sont pas respectés
     */
    public static LiquidationCommunauteFrResult compute(LiquidationCommunauteFrRequest req, String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-04 applicable uniquement en France (art. 1467 Cciv).");
        }
        // Régime : si renseigné, doit être COMMUNAUTE_LEGALE. Si null, on l'autorise
        // (l'avocat peut ne pas avoir documenté le régime — la liquidation reste indicative).
        if (req.regimeMatrimonialDetecte() != null
                && !REGIME_REQUIS.equals(req.regimeMatrimonialDetecte())) {
            throw new IllegalArgumentException(
                    "Outil F-FA-04 applicable uniquement en régime de communauté légale (art. 1467 Cciv). "
                            + "Régime détecté : " + req.regimeMatrimonialDetecte() + ".");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        // 1. Conversion null → 0 pour le calcul (le service rejette les négatifs).
        int immeuble1 = nz(req.valeurImmeubleCommun1Eur());
        int immeuble2 = nz(req.valeurImmeubleCommun2Eur());
        int credit = nz(req.capitalRestantDuEur());
        int mobilier = nz(req.valeurMobilierCommunEur());
        int autres = nz(req.autresActifsCommunsEur());
        int recompenses1 = nz(req.recompensesEpoux1Eur());
        int recompenses2 = nz(req.recompensesEpoux2Eur());

        // 2. Actif commun brut
        int actifCommunBrut = immeuble1 + immeuble2 + mobilier + autres;

        // 3. Masse commune nette (≥ 0 ; si crédit > actif, on retombe à 0 + alerte)
        int masseCommune = actifCommunBrut - credit;
        if (masseCommune < 0) {
            alertes.add("Le passif commun (crédit restant dû : " + credit + " €) dépasse l'actif commun ("
                    + actifCommunBrut + " €) — communauté en déficit, à arbitrer (art. 1483 Cciv).");
            masseCommune = 0;
        }

        // 4. Quote-part théorique (art. 1475 Cciv — partage égalitaire)
        int quotePartTheorique = masseCommune / 2;

        // 5. Application des récompenses (art. 1433-1435 Cciv)
        // Les récompenses dues par l'époux X à la communauté **diminuent** sa quote-part
        // et sont reportées dans le solde de l'autre époux (effet de transfert).
        int quotaPartEpoux1 = quotePartTheorique - recompenses1 + recompenses2;
        int quotaPartEpoux2 = quotePartTheorique - recompenses2 + recompenses1;
        // Bornage à 0 pour ne pas afficher de valeurs négatives — alerte si bornage.
        if (quotaPartEpoux1 < 0) {
            alertes.add("Les récompenses dues par l'époux 1 à la communauté excèdent sa quote-part — "
                    + "compensation à organiser hors masse (art. 1469 Cciv).");
            quotaPartEpoux1 = 0;
        }
        if (quotaPartEpoux2 < 0) {
            alertes.add("Les récompenses dues par l'époux 2 à la communauté excèdent sa quote-part — "
                    + "compensation à organiser hors masse (art. 1469 Cciv).");
            quotaPartEpoux2 = 0;
        }

        // 6. Soulte indicative — un époux conserve l'immeuble principal (immeuble1)
        // et doit compenser l'autre à hauteur de sa quote-part théorique sur ce bien.
        int soulte = 0;
        boolean alerteIndivision = false;
        if (req.occupationLogementEpoux() != null && immeuble1 > 0) {
            switch (req.occupationLogementEpoux()) {
                case EPOUX1 -> {
                    // L'époux 1 conserve l'immeuble 1 → soulte = part théorique sur immeuble1
                    // Soulte = valeur immeuble - (quote-part epoux1 - autres actifs déjà attribués)
                    // Modèle indicatif : moitié de la valeur nette du logement
                    int valeurNetteLogement = Math.max(0, immeuble1 - credit);
                    soulte = valeurNetteLogement / 2;
                    messages.add("L'époux 1 conserve le logement (valeur nette " + valeurNetteLogement
                            + " €) — soulte indicative " + soulte + " € due à l'époux 2 (art. 832-2 Cciv).");
                }
                case EPOUX2 -> {
                    int valeurNetteLogement = Math.max(0, immeuble1 - credit);
                    soulte = valeurNetteLogement / 2;
                    messages.add("L'époux 2 conserve le logement (valeur nette " + valeurNetteLogement
                            + " €) — soulte indicative " + soulte + " € due à l'époux 1 (art. 832-2 Cciv).");
                }
                case AUCUN -> {
                    alerteIndivision = true;
                    alertes.add("Aucun époux ne reprend le logement en nature — indivision possible "
                            + "(art. 815 Cciv), envisager la vente amiable ou la licitation (art. 1377 CPC).");
                }
            }
        } else if (immeuble1 > 0 || immeuble2 > 0) {
            messages.add("Aucune attribution du logement saisie — à arbitrer entre les époux ou par le juge "
                    + "(art. 832 et s. Cciv).");
        }

        // 7. Messages contextuels
        if (masseCommune == 0 && actifCommunBrut == 0) {
            messages.add("Aucun actif commun renseigné — saisir les valeurs immobilières, mobilières et le passif "
                    + "pour un chiffrage indicatif.");
        }
        if (recompenses1 > 0 || recompenses2 > 0) {
            messages.add("Récompenses appliquées (art. 1433-1435 Cciv) — le calcul rééquilibre les quote-parts "
                    + "selon les créances réciproques entre époux et communauté.");
        }
        messages.add("Montant indicatif — la liquidation définitive relève du notaire désigné "
                + "(art. 1364 CPC) ou du juge (art. 1377 CPC) en cas de désaccord.");

        return new LiquidationCommunauteFrResult(
                masseCommune,
                quotaPartEpoux1,
                quotaPartEpoux2,
                soulte,
                recompenses2,   // récompenses nettes pour epoux1 = ce qu'il reçoit de l'autre
                recompenses1,   // récompenses nettes pour epoux2 = ce qu'il reçoit de l'autre
                alerteIndivision,
                BASE_JURIDIQUE,
                messages,
                alertes
        );
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
