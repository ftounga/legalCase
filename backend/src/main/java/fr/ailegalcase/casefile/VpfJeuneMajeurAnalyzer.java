package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-220-03 : analyseur d'éligibilité d'un jeune majeur (16-21 ans, entré mineur,
 * scolarisé / pris en charge ASE) à la carte « vie privée et familiale » de
 * l'art. L.423-22 CESEDA (F-IM-49-vpf-jeune-majeur-l42322-fr). Outil single-country FR.
 *
 * <p><b>Objet</b> : la <b>transition à la majorité</b> de l'ex-mineur non accompagné
 * (MNA) confié à l'ASE — voie L.423-22 propre au jeune majeur entré mineur.
 * Distinct de F-IM-27 (VPF liens personnels L.423-23, fondement « liens personnels
 * et familiaux »), de F-IM-19 (mineurs, s'arrête à la majorité) et de F-IM-38
 * (évaluation de l'âge MNA). Couvre le trou de la sortie ASE.</p>
 *
 * <p>L'ancienneté de prise en charge requise <b>varie selon l'âge d'entrée</b> à
 * l'ASE :
 * <ul>
 *   <li>entrée <b>avant 16 ans</b> → droit de plein droit (L.423-22), aucune
 *       ancienneté minimale de prise en charge n'est exigée ;</li>
 *   <li>entrée <b>entre 16 et 18 ans</b> → la délivrance relève de l'admission
 *       exceptionnelle au séjour (L.435-3), qui suppose une prise en charge depuis
 *       au moins 6 mois et l'appréciation du caractère réel et sérieux de la
 *       formation.</li>
 * </ul>
 * Toutes ces particularités sont annotées « à vérifier par avocat » : l'outil
 * aiguille, il ne se substitue pas à la vérification du texte applicable.</p>
 *
 * <p>Source juridique (à vérifier par avocat) :
 * <ul>
 *   <li>CESEDA L.423-22 (ancien L.313-11 2°bis — VPF jeune majeur entré mineur)</li>
 *   <li>CESEDA L.435-3 (admission exceptionnelle jeune majeur confié à l'ASE)</li>
 * </ul>
 * </p>
 */
public final class VpfJeuneMajeurAnalyzer {

    // Verdicts d'éligibilité.
    public static final String ELIGIBLE_L42322 = "ELIGIBLE_L42322";
    public static final String ELIGIBLE_SOUS_RESERVE = "ELIGIBLE_SOUS_RESERVE";
    public static final String NON_ELIGIBLE = "NON_ELIGIBLE";
    public static final String ORIENTER_AES = "ORIENTER_AES";

    // Bornes d'âge de la voie jeune majeur.
    public static final int AGE_MIN = 16;
    public static final int AGE_MAX = 21;

    // Ancienneté de prise en charge requise (mois) selon l'âge d'entrée à l'ASE.
    public static final int ANCIENNETE_ENTREE_AVANT_16 = 0;   // de plein droit, L.423-22
    public static final int ANCIENNETE_ENTREE_16_18 = 6;      // AES L.435-3

    private static final String BASE_L42322 =
            "CESEDA L.423-22 (VPF jeune majeur entré mineur, ancien L.313-11 2°bis) — à vérifier par avocat";
    private static final String BASE_L4353 =
            "CESEDA L.435-3 (admission exceptionnelle jeune majeur confié à l'ASE) — à vérifier par avocat";

    private VpfJeuneMajeurAnalyzer() {}

    /**
     * Analyse l'éligibilité L.423-22 du jeune majeur ex-MNA.
     *
     * @param age                            âge du jeune (validé en amont, ∈ ]0, 30])
     * @param entreMineur                    true si entré en France avant sa majorité
     * @param ageEntreeAse                   âge d'entrée à l'ASE (nullable) — pilote l'ancienneté requise
     * @param priseEnChargeAse               true si pris en charge par l'ASE
     * @param ancienneteMoisPriseEnCharge    ancienneté de prise en charge en mois (nullable)
     * @param scolariseOuFormation           true si scolarisé ou en formation
     * @param caractereReelEtSerieuxFormation true si la formation est réelle et sérieuse
     * @param avisStructureFavorable         true si l'avis de la structure d'accueil est favorable
     * @param absenceLienFamillePays         true si absence de lien avec la famille restée au pays
     * @return résultat d'éligibilité
     */
    public static VpfJeuneMajeurResult analyze(int age,
                                               boolean entreMineur,
                                               Integer ageEntreeAse,
                                               boolean priseEnChargeAse,
                                               Integer ancienneteMoisPriseEnCharge,
                                               boolean scolariseOuFormation,
                                               boolean caractereReelEtSerieuxFormation,
                                               boolean avisStructureFavorable,
                                               boolean absenceLienFamillePays) {

        List<String> criteresManquants = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        boolean ageDansBornes = age >= AGE_MIN && age <= AGE_MAX;
        // Entrée avant 16 ans = voie L.423-22 de plein droit ; sinon AES L.435-3.
        boolean entreeAvant16 = entreMineur
                && (ageEntreeAse == null || ageEntreeAse < AGE_MIN);
        int ancienneteRequiseMois = entreeAvant16
                ? ANCIENNETE_ENTREE_AVANT_16
                : ANCIENNETE_ENTREE_16_18;

        // Inventaire des critères.
        if (!entreMineur) {
            criteresManquants.add("Entrée en France à la majorité (la voie L.423-22 suppose une entrée mineur)");
        }
        if (!ageDansBornes) {
            criteresManquants.add("Âge hors de la tranche 16-21 ans de la voie jeune majeur");
        }
        if (!priseEnChargeAse) {
            criteresManquants.add("Absence de prise en charge par l'aide sociale à l'enfance (ASE)");
        }
        if (!scolariseOuFormation) {
            criteresManquants.add("Absence de scolarisation ou de formation");
        }
        if (scolariseOuFormation && !caractereReelEtSerieuxFormation) {
            criteresManquants.add("Caractère réel et sérieux de la formation à confirmer");
        }
        boolean ancienneteSuffisante =
                ancienneteMoisPriseEnCharge == null
                        ? true // non factualisée : à confirmer par l'avocat, pas un blocage automatique
                        : ancienneteMoisPriseEnCharge >= ancienneteRequiseMois;
        if (ancienneteMoisPriseEnCharge != null && !ancienneteSuffisante) {
            criteresManquants.add("Ancienneté de prise en charge ASE insuffisante (requis : "
                    + ancienneteRequiseMois + " mois)");
        }

        bases.add(BASE_L42322);

        // Socle dur d'éligibilité : entré mineur + âge + ASE + scolarisé/formation.
        boolean socleEligible = entreMineur && ageDansBornes && priseEnChargeAse && scolariseOuFormation;

        String eligibilite;
        if (socleEligible && entreeAvant16 && caractereReelEtSerieuxFormation && ancienneteSuffisante) {
            // Voie L.423-22 de plein droit (entrée avant 16 ans).
            eligibilite = ELIGIBLE_L42322;
            messages.add("Profil correspondant à la délivrance de plein droit de la carte VPF "
                    + "« vie privée et familiale » au titre de L.423-22 (jeune majeur entré mineur "
                    + "avant 16 ans, pris en charge par l'ASE, en formation réelle et sérieuse). "
                    + "À vérifier par avocat.");
            addConditionsComplementaires(messages, avisStructureFavorable, absenceLienFamillePays);
        } else if (socleEligible && !entreeAvant16) {
            // Entrée 16-18 ans : voie admission exceptionnelle L.435-3.
            eligibilite = ORIENTER_AES;
            bases.add(BASE_L4353);
            messages.add("Entrée à l'ASE entre 16 et 18 ans : la délivrance relève non de L.423-22 "
                    + "de plein droit mais de l'admission exceptionnelle au séjour (L.435-3), sous "
                    + "réserve d'une prise en charge depuis au moins 6 mois et de l'appréciation du "
                    + "caractère réel et sérieux de la formation. À vérifier par avocat.");
            addConditionsComplementaires(messages, avisStructureFavorable, absenceLienFamillePays);
        } else if (socleEligible) {
            // Socle satisfait mais un critère de fond à confirmer (formation / ancienneté).
            eligibilite = ELIGIBLE_SOUS_RESERVE;
            messages.add("Socle de la voie L.423-22 réuni, mais au moins un critère reste à "
                    + "confirmer (caractère réel et sérieux de la formation ou ancienneté de "
                    + "prise en charge). À vérifier par avocat.");
            addConditionsComplementaires(messages, avisStructureFavorable, absenceLienFamillePays);
        } else {
            // Socle non satisfait.
            eligibilite = NON_ELIGIBLE;
            messages.add("Le socle de la voie L.423-22 n'est pas réuni en l'état (voir critères "
                    + "manquants). À vérifier par avocat.");
            // Renvoi vers l'admission exceptionnelle si l'entrée mineur + ASE existent.
            if (entreMineur && priseEnChargeAse) {
                eligibilite = ORIENTER_AES;
                bases.add(BASE_L4353);
                messages.add("Orientation vers l'admission exceptionnelle au séjour (L.435-3) : "
                        + "jeune majeur confié à l'ASE pouvant relever de cette voie alternative. "
                        + "À vérifier par avocat.");
            }
        }

        return new VpfJeuneMajeurResult(
                age,
                entreMineur,
                ageEntreeAse,
                priseEnChargeAse,
                ancienneteMoisPriseEnCharge,
                scolariseOuFormation,
                caractereReelEtSerieuxFormation,
                eligibilite,
                ancienneteRequiseMois,
                criteresManquants,
                bases,
                messages);
    }

    private static void addConditionsComplementaires(List<String> messages,
                                                     boolean avisStructureFavorable,
                                                     boolean absenceLienFamillePays) {
        if (!avisStructureFavorable) {
            messages.add("Condition complémentaire : avis de la structure d'accueil sur le parcours "
                    + "du jeune à recueillir / produire. À vérifier par avocat.");
        }
        if (!absenceLienFamillePays) {
            messages.add("Condition complémentaire : la nature des liens avec la famille restée au "
                    + "pays d'origine est appréciée par le préfet. À vérifier par avocat.");
        }
    }
}
