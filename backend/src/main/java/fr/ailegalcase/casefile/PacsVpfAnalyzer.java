package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-220-04 : analyseur de l'éligibilité à la carte « vie privée et familiale »
 * (CESEDA L.423-23) <b>au titre d'un PACS</b> conclu en France
 * (F-IM-50-pacs-vpf-fr). Outil single-country FR.
 *
 * <p><b>Objet</b> : apprécier le PACS comme <b>faisceau d'indices</b> de vie
 * privée et familiale. Contrairement au <b>conjoint marié</b> de Français
 * (F-IM-21), le PACS <b>n'ouvre pas automatiquement</b> de droit au séjour : il
 * est un <b>élément</b> d'appréciation, dont la valeur probante dépend de
 * l'ancienneté (~1 an) et de l'intensité / stabilité de la communauté de vie.
 * Distinct aussi de la voie L.423-23 « liens personnels » générale (F-IM-27) :
 * l'angle est ici spécifiquement le PACS.</p>
 *
 * <p>Toutes ces appréciations sont annotées « à vérifier par avocat » : l'outil
 * aiguille sur la solidité du faisceau, il ne se substitue pas à l'appréciation
 * souveraine du préfet ni du juge.</p>
 *
 * <p>Source juridique (à vérifier par avocat) :
 * <ul>
 *   <li>CESEDA L.423-23 (carte VPF — liens privés et familiaux)</li>
 *   <li>Jurisprudence sur la valeur probante du PACS (ancienneté, intensité de
 *       la communauté de vie)</li>
 * </ul>
 * </p>
 */
public final class PacsVpfAnalyzer {

    // Verdicts d'éligibilité.
    public static final String FAISCEAU_FAVORABLE = "FAISCEAU_FAVORABLE";
    public static final String FAISCEAU_INSUFFISANT = "FAISCEAU_INSUFFISANT";
    public static final String A_CONSOLIDER = "A_CONSOLIDER";
    public static final String NON_ELIGIBLE = "NON_ELIGIBLE";

    // Statut du partenaire pacsé.
    public static final String PARTENAIRE_FRANCAIS = "FRANCAIS";
    public static final String PARTENAIRE_ETRANGER_REGULIER = "ETRANGER_REGULIER";
    public static final String PARTENAIRE_AUTRE = "AUTRE";
    public static final Set<String> PARTENAIRE_STATUTS =
            Set.of(PARTENAIRE_FRANCAIS, PARTENAIRE_ETRANGER_REGULIER, PARTENAIRE_AUTRE);

    // Intensité de la communauté de vie.
    public static final String INTENSITE_FORTE = "FORTE";
    public static final String INTENSITE_MOYENNE = "MOYENNE";
    public static final String INTENSITE_FAIBLE = "FAIBLE";
    public static final String INTENSITE_NON_ETABLIE = "NON_ETABLIE";
    public static final Set<String> INTENSITE_VALEURS =
            Set.of(INTENSITE_FORTE, INTENSITE_MOYENNE, INTENSITE_FAIBLE, INTENSITE_NON_ETABLIE);

    // Ancienneté indicative de référence (mois) — seuil jurisprudentiel ~1 an.
    public static final int ANCIENNETE_REFERENCE_MOIS = 12;

    private static final String BASE_L42323 =
            "CESEDA L.423-23 (carte VPF — liens privés et familiaux) — à vérifier par avocat";
    private static final String BASE_JURISPRUDENCE_PACS =
            "Jurisprudence sur la valeur probante du PACS (ancienneté ~1 an + intensité de la "
                    + "communauté de vie) — à vérifier par avocat";

    private PacsVpfAnalyzer() {}

    /**
     * Analyse la solidité du faisceau L.423-23 au titre d'un PACS.
     *
     * @param pacsConclu                true si un PACS a été conclu
     * @param partenaireStatut          statut du partenaire (FRANCAIS / ETRANGER_REGULIER / AUTRE)
     * @param dureeVieCommuneMois       durée de vie commune en mois (nullable)
     * @param intensiteCommunauteVie    intensité de la communauté de vie (FORTE / MOYENNE / FAIBLE / NON_ETABLIE)
     * @param autresLiensPrivesFamiliaux true si d'autres liens privés / familiaux renforcent le faisceau
     * @return résultat d'éligibilité
     */
    public static PacsVpfResult analyze(boolean pacsConclu,
                                        String partenaireStatut,
                                        Integer dureeVieCommuneMois,
                                        String intensiteCommunauteVie,
                                        boolean autresLiensPrivesFamiliaux) {

        List<String> favorables = new ArrayList<>();
        List<String> manquants = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        bases.add(BASE_L42323);
        bases.add(BASE_JURISPRUDENCE_PACS);

        // Garde-fou anti-doublon : le PACS n'est jamais un droit automatique (≠ mariage F-IM-21).
        messages.add("Rappel : le PACS n'ouvre pas, à lui seul, de droit automatique au séjour "
                + "(différence avec le conjoint marié de Français). Il constitue un élément du "
                + "faisceau d'indices de vie privée et familiale apprécié au titre de L.423-23. "
                + "À vérifier par avocat.");

        // Socle : sans PACS conclu, l'outil n'a pas d'objet — non éligible par cette voie.
        if (!pacsConclu) {
            manquants.add("Aucun PACS conclu (l'outil apprécie le séjour au titre d'un PACS)");
            messages.add("En l'absence de PACS conclu, la voie L.423-23 « au titre d'un PACS » est "
                    + "sans objet ; une autre voie de vie privée et familiale (F-IM-27) peut rester "
                    + "ouverte. À vérifier par avocat.");
            return new PacsVpfResult(false, partenaireStatut, dureeVieCommuneMois,
                    intensiteCommunauteVie, autresLiensPrivesFamiliaux,
                    NON_ELIGIBLE, favorables, manquants, bases, messages);
        }

        favorables.add("PACS conclu");

        // Ancienneté de la vie commune.
        boolean ancienneteSuffisante = dureeVieCommuneMois != null
                && dureeVieCommuneMois >= ANCIENNETE_REFERENCE_MOIS;
        boolean ancienneteConnue = dureeVieCommuneMois != null;
        if (ancienneteSuffisante) {
            favorables.add("Vie commune d'au moins " + ANCIENNETE_REFERENCE_MOIS + " mois ("
                    + dureeVieCommuneMois + " mois)");
        } else if (ancienneteConnue) {
            manquants.add("Ancienneté de vie commune inférieure au seuil indicatif d'environ 1 an ("
                    + dureeVieCommuneMois + " mois) — faisceau à consolider");
        } else {
            manquants.add("Ancienneté de vie commune non établie (~1 an attendu) — à factualiser");
        }

        // Intensité de la communauté de vie.
        boolean intensiteForte = INTENSITE_FORTE.equals(intensiteCommunauteVie);
        boolean intensiteMoyenne = INTENSITE_MOYENNE.equals(intensiteCommunauteVie);
        boolean intensiteFaible = INTENSITE_FAIBLE.equals(intensiteCommunauteVie)
                || INTENSITE_NON_ETABLIE.equals(intensiteCommunauteVie);
        if (intensiteForte) {
            favorables.add("Communauté de vie d'intensité forte (stabilité établie)");
        } else if (intensiteMoyenne) {
            manquants.add("Communauté de vie d'intensité moyenne — preuves de vie commune à renforcer");
        } else {
            manquants.add("Communauté de vie faible ou non établie — preuves de vie commune à constituer");
        }

        // Statut du partenaire.
        boolean partenaireSolide = PARTENAIRE_FRANCAIS.equals(partenaireStatut)
                || PARTENAIRE_ETRANGER_REGULIER.equals(partenaireStatut);
        if (PARTENAIRE_FRANCAIS.equals(partenaireStatut)) {
            favorables.add("Partenaire de nationalité française");
        } else if (PARTENAIRE_ETRANGER_REGULIER.equals(partenaireStatut)) {
            favorables.add("Partenaire étranger en séjour régulier");
        } else {
            manquants.add("Statut du partenaire non favorable (ni français ni étranger en séjour régulier)");
        }

        // Autres liens privés et familiaux : élément renforçant.
        if (autresLiensPrivesFamiliaux) {
            favorables.add("Autres liens privés et familiaux renforçant le faisceau");
        }

        String eligibilite;
        if (ancienneteSuffisante && intensiteForte && partenaireSolide) {
            eligibilite = FAISCEAU_FAVORABLE;
            messages.add("Faisceau d'indices favorable au titre de L.423-23 : PACS ancien (≥ ~1 an), "
                    + "communauté de vie d'intensité forte et partenaire français ou en séjour régulier. "
                    + "La délivrance reste soumise à l'appréciation de l'administration. À vérifier par avocat.");
        } else if (intensiteFaible || (ancienneteConnue && !ancienneteSuffisante)) {
            // PACS récent (< ~1 an) ou intensité faible → faisceau insuffisant en l'état.
            eligibilite = FAISCEAU_INSUFFISANT;
            messages.add("Faisceau insuffisant en l'état : PACS récent (< ~1 an) et/ou communauté de "
                    + "vie peu intense. Le PACS seul ne suffit pas ; consolider les preuves d'ancienneté "
                    + "et d'intensité de la vie commune. À vérifier par avocat.");
        } else {
            // Socle réuni mais un ou plusieurs éléments à confirmer.
            eligibilite = A_CONSOLIDER;
            messages.add("Faisceau à consolider : certains éléments (ancienneté, intensité ou statut du "
                    + "partenaire) restent à factualiser ou à renforcer avant un dépôt. À vérifier par avocat.");
        }

        return new PacsVpfResult(true, partenaireStatut, dureeVieCommuneMois,
                intensiteCommunauteVie, autresLiensPrivesFamiliaux,
                eligibilite, favorables, manquants, bases, messages);
    }
}
