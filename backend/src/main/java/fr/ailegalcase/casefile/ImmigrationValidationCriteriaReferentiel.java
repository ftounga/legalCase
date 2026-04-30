package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;

/**
 * Référentiel statique des critères binaires de validité dossier immigration (F-IM-21).
 * Pattern miroir {@code RuptureConvCritereReferentiel} (F-DT-10) et {@code LICENCIEMENT_CRITERES} (F-DT-08).
 *
 * <p>Ce référentiel est un fallback Java — la source de vérité est la table {@code legal_referentials}
 * de type {@code IM21_VALIDITY_CRITERES} (seedée par migration 198).</p>
 *
 * <p>Codes binaires (VERIFIED / NON_COMPLIANT / TO_CHECK) consommés par le prompt
 * {@code CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE} (SF-IM-21-02).</p>
 */
public final class ImmigrationValidationCriteriaReferentiel {

    private static final Map<String, ImmigrationValidationCriterion> REFERENTIEL = Map.ofEntries(
            // ----- France (11 critères) -----
            Map.entry("IM21_REGULARITE_SEJOUR_FR", new ImmigrationValidationCriterion(
                    "IM21_REGULARITE_SEJOUR_FR", "FRANCE",
                    "Régularité du séjour au moment du dépôt",
                    "Art. R.431-2 CESEDA",
                    "Le demandeur est-il en situation régulière au moment du dépôt (titre en cours de validité, récépissé, ou pré-droit ouvert) ?")),
            Map.entry("IM21_DELAI_DEPOT_FR", new ImmigrationValidationCriterion(
                    "IM21_DELAI_DEPOT_FR", "FRANCE",
                    "Délai de dépôt avant expiration",
                    "Art. R.431-5 CESEDA",
                    "La demande de renouvellement a-t-elle été déposée dans les 2 mois précédant l'expiration du titre en cours ?")),
            Map.entry("IM21_PIECE_IDENTITE_FR", new ImmigrationValidationCriterion(
                    "IM21_PIECE_IDENTITE_FR", "FRANCE",
                    "Passeport ou pièce d'identité valide",
                    "Art. R.431-10 CESEDA",
                    "Le passeport ou la pièce d'identité du demandeur est-il valide pour la durée du séjour demandé ?")),
            Map.entry("IM21_JUSTIF_DOMICILE_FR", new ImmigrationValidationCriterion(
                    "IM21_JUSTIF_DOMICILE_FR", "FRANCE",
                    "Justificatif de domicile en France",
                    "Art. R.431-10 CESEDA",
                    "Le demandeur produit-il un justificatif de domicile en France de moins de 6 mois (facture, attestation hébergement, bail) ?")),
            Map.entry("IM21_ETAT_CIVIL_FR", new ImmigrationValidationCriterion(
                    "IM21_ETAT_CIVIL_FR", "FRANCE",
                    "Pièces d'état civil traduites/légalisées",
                    "Art. R.431-10 CESEDA + Conv. La Haye 1961",
                    "Les actes d'état civil étrangers sont-ils traduits par traducteur assermenté et légalisés/apostillés selon le pays d'origine ?")),
            Map.entry("IM21_PHOTO_FR", new ImmigrationValidationCriterion(
                    "IM21_PHOTO_FR", "FRANCE",
                    "Photo d'identité conforme",
                    "Art. R.431-10 CESEDA",
                    "Les photos d'identité respectent-elles la norme préfecture (récentes, fond uni, format 35x45 mm) ?")),
            Map.entry("IM21_TIMBRE_FISCAL_FR", new ImmigrationValidationCriterion(
                    "IM21_TIMBRE_FISCAL_FR", "FRANCE",
                    "Timbre fiscal acquitté",
                    "Art. L.311-13 CESEDA",
                    "Le timbre fiscal correspondant au type de titre demandé est-il acquitté et présenté ?")),
            Map.entry("IM21_PIECES_MARIAGE_FR", new ImmigrationValidationCriterion(
                    "IM21_PIECES_MARIAGE_FR", "FRANCE",
                    "Acte de mariage transcrit",
                    "Art. L.423-1 CESEDA + Art. 47 Cciv",
                    "Pour les titres VPF conjoint francais : l'acte de mariage est-il transcrit en France (ou pris en compte sans transcription si mariage en France) ?")),
            Map.entry("IM21_COMMUNAUTE_VIE_FR", new ImmigrationValidationCriterion(
                    "IM21_COMMUNAUTE_VIE_FR", "FRANCE",
                    "Communauté de vie justifiée",
                    "Art. L.423-1 CESEDA",
                    "Pour les titres VPF conjoint : la communauté de vie effective et continue est-elle justifiée (factures communes, témoignages, photos) ?")),
            Map.entry("IM21_RESSOURCES_FR", new ImmigrationValidationCriterion(
                    "IM21_RESSOURCES_FR", "FRANCE",
                    "Ressources stables et suffisantes",
                    "Art. L.423-2+ CESEDA",
                    "Pour les titres exigeant des ressources (étudiant, salarié, regroupement) : les ressources sont-elles stables et au moins égales au seuil légal du titre concerné ?")),
            Map.entry("IM21_CONVENTION_ACCUEIL_FR", new ImmigrationValidationCriterion(
                    "IM21_CONVENTION_ACCUEIL_FR", "FRANCE",
                    "Convention d'accueil organisme recherche",
                    "Art. L.421-14 CESEDA + Art. R.421-21",
                    "Pour le Passeport talent — Chercheur : la convention d'accueil signée par un organisme de recherche habilité (CNRS, INRIA, université, etc.) est-elle présente ?")),

            // ----- Belgique (7 critères) -----
            Map.entry("IM21_REGULARITE_SEJOUR_BE", new ImmigrationValidationCriterion(
                    "IM21_REGULARITE_SEJOUR_BE", "BELGIQUE",
                    "Régularité du séjour au moment du dépôt",
                    "Art. 9 + 9bis Loi 15/12/1980",
                    "Le demandeur est-il en situation régulière au moment du dépôt (annexe 19, annexe 15, carte en cours) ou justifie-t-il d'une demande pour circonstances exceptionnelles ?")),
            Map.entry("IM21_PIECE_IDENTITE_BE", new ImmigrationValidationCriterion(
                    "IM21_PIECE_IDENTITE_BE", "BELGIQUE",
                    "Passeport valide",
                    "Art. 41 Loi 15/12/1980",
                    "Le passeport du demandeur est-il valide et couvre-t-il au minimum 1 an au-delà de la date de demande ?")),
            Map.entry("IM21_PIECES_COHABITATION_BE", new ImmigrationValidationCriterion(
                    "IM21_PIECES_COHABITATION_BE", "BELGIQUE",
                    "Pièces cohabitation/mariage",
                    "Art. 40bis + 40ter + 10 + 11 Loi 15/12/1980",
                    "Pour les titres familiaux (40ter, cohabitant légal, regroupement) : l'acte de mariage ou contrat de cohabitation légale est-il enregistré et la composition de ménage commune produite ?")),
            Map.entry("IM21_RESSOURCES_BE", new ImmigrationValidationCriterion(
                    "IM21_RESSOURCES_BE", "BELGIQUE",
                    "Ressources suffisantes",
                    "Art. 10 §5 + 40ter §2 Loi 15/12/1980",
                    "Pour les titres familiaux et étudiants : les revenus mensuels nets sont-ils >= 120 % du RIS (revenu d'intégration sociale) ?")),
            Map.entry("IM21_LOGEMENT_BE", new ImmigrationValidationCriterion(
                    "IM21_LOGEMENT_BE", "BELGIQUE",
                    "Logement suffisant",
                    "Art. 10 §2 1° Loi 15/12/1980",
                    "Le demandeur (ou regroupant) dispose-t-il d'un logement suffisant pour héberger sa famille (dimensions, salubrité, attestation commune) ?")),
            Map.entry("IM21_ASSURANCE_BE", new ImmigrationValidationCriterion(
                    "IM21_ASSURANCE_BE", "BELGIQUE",
                    "Assurance maladie",
                    "Art. 9 Loi 15/12/1980 + AR 8/10/1981",
                    "Le demandeur dispose-t-il d'une assurance maladie couvrant la durée du séjour demandé (mutuelle, attestation employeur, ou européenne) ?")),
            Map.entry("IM21_EXTRAIT_CASIER_BE", new ImmigrationValidationCriterion(
                    "IM21_EXTRAIT_CASIER_BE", "BELGIQUE",
                    "Extrait de casier judiciaire",
                    "Art. 9 + 13 Loi 15/12/1980",
                    "L'extrait de casier judiciaire du pays d'origine de moins de 6 mois est-il fourni et traduit ?"))
    );

    private ImmigrationValidationCriteriaReferentiel() {}

    /** Retourne le critère correspondant au code IM21_*, ou {@code null} si inconnu. */
    public static ImmigrationValidationCriterion resolve(String code) {
        if (code == null) return null;
        return REFERENTIEL.get(code);
    }

    /** Retourne tous les critères pour un pays donné ({@code FRANCE} ou {@code BELGIQUE}). */
    public static List<ImmigrationValidationCriterion> forCountry(String country) {
        if (country == null) return List.of();
        return REFERENTIEL.values().stream()
                .filter(c -> country.equals(c.country()))
                .toList();
    }

    /** Retourne tous les critères du référentiel. */
    public static List<ImmigrationValidationCriterion> all() {
        return List.copyOf(REFERENTIEL.values());
    }
}
