package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-218-07 : barème de la saisie sur rémunération (quotité saisissable) —
 * art. R. 3252-2, R. 3252-3 et L. 3252-3 du Code du travail. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>La part saisissable d'une rémunération est déterminée par un barème
 * progressif par tranches (art. R. 3252-2 CT) : à chaque tranche de
 * rémunération annuelle correspond une fraction saisissable croissante
 * (1/20, 1/10, 1/5, 1/4, 1/3, 2/3, puis totalité au-delà de la dernière
 * borne). Les bornes du barème, fixées en valeur annuelle, sont ramenées au
 * mois pour le calcul mensuel.
 *
 * <p>Chaque personne à charge majore chaque borne de tranche d'un montant
 * annuel fixe (art. R. 3252-3 CT), ce qui déplace le barème vers le haut et
 * réduit la part saisissable.
 *
 * <p>Une fraction de la rémunération demeure <b>absolument insaisissable</b> :
 * la part égale au montant forfaitaire du RSA pour un allocataire seul ne peut
 * jamais être saisie (art. L. 3252-3 CT), y compris pour une créance
 * alimentaire.
 *
 * <p><b>CONSTANTES À ACTUALISER ANNUELLEMENT</b> : les bornes du barème
 * (révisées chaque année par décret, généralement en décembre), le montant de
 * majoration par personne à charge et le montant forfaitaire RSA sont
 * revalorisés chaque année. Valeurs 2026 documentées ci-dessous. Vérifier et
 * mettre à jour {@link #BAREME_SAISIE_REMUNERATION_2026},
 * {@link #MAJORATION_PAR_PERSONNE_A_CHARGE_2026} et
 * {@link #FRACTION_INSAISISSABLE_RSA_2026} à chaque décret annuel.
 */
public final class BaremeSaisieRemuneration {

    /**
     * Une tranche du barème de la quotité saisissable.
     *
     * @param borneSuperieureAnnuelle borne supérieure de la tranche en valeur
     *        annuelle (€). {@link Double#MAX_VALUE} pour la dernière tranche
     *        (au-delà → totalité saisissable).
     * @param numerateur numérateur de la fraction saisissable de la tranche.
     * @param denominateur dénominateur de la fraction saisissable de la tranche.
     */
    public record TrancheSaisie(double borneSuperieureAnnuelle, int numerateur, int denominateur) {

        /** Fraction saisissable de la tranche (numérateur / dénominateur). */
        public double fraction() {
            return (double) numerateur / denominateur;
        }
    }

    /**
     * Barème annuel de la quotité saisissable — <b>valeurs 2026</b>.
     * <b>À ACTUALISER ANNUELLEMENT</b> (décret annuel — art. R. 3252-2 CT,
     * révision au 1er janvier). 7 tranches de fractions croissantes 1/20, 1/10,
     * 1/5, 1/4, 1/3, 2/3, totalité (la 7e tranche, au-delà de la dernière borne,
     * est entièrement saisissable).
     */
    public static final List<TrancheSaisie> BAREME_SAISIE_REMUNERATION_2026 = List.of(
            new TrancheSaisie(4_440.0, 1, 20),
            new TrancheSaisie(8_660.0, 1, 10),
            new TrancheSaisie(12_890.0, 1, 5),
            new TrancheSaisie(17_100.0, 1, 4),
            new TrancheSaisie(21_330.0, 1, 3),
            new TrancheSaisie(25_620.0, 2, 3),
            new TrancheSaisie(Double.MAX_VALUE, 1, 1));

    /**
     * Montant annuel de majoration de chaque borne de tranche par personne à
     * charge — <b>valeur 2026</b>. <b>À ACTUALISER ANNUELLEMENT</b> (décret
     * annuel — art. R. 3252-3 CT).
     */
    public static final double MAJORATION_PAR_PERSONNE_A_CHARGE_2026 = 1_690.0;

    /**
     * Fraction absolument insaisissable = montant forfaitaire mensuel du RSA
     * pour un allocataire seul — <b>valeur 2026</b>. <b>À ACTUALISER
     * ANNUELLEMENT</b> (revalorisation du RSA — art. L. 3252-3 CT).
     */
    public static final double FRACTION_INSAISISSABLE_RSA_2026 = 646.52;

    private BaremeSaisieRemuneration() {
    }
}
