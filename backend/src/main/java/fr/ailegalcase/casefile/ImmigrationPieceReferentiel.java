package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;

/**
 * Référentiel statique des pièces requises par régime juridique d'admission
 * au séjour, différencié par pays.
 *
 * <p>SF-IM-01-04 — Liste exhaustive des régimes couverts en V1 (France) :
 * <ol>
 *   <li>{@code VISA_ETUDIANT} — VLS-TS Étudiant, art. L.422-1 CESEDA</li>
 *   <li>{@code APS_POST_ETUDES} — Autorisation provisoire post-master, art. L.422-14</li>
 *   <li>{@code TITRE_SALARIE} — Carte séjour Salarié, art. L.421-1</li>
 *   <li>{@code PASSEPORT_TALENT} — Pluriannuelle talent (chercheur/salarié qualifié/entrepreneur), art. L.421-9 à L.421-14</li>
 *   <li>{@code CST_VPF_CONJOINT_FR} — Conjoint de Français, art. L.423-1</li>
 *   <li>{@code CST_VPF_PARENT_ENFANT_FR} — Parent d'enfant français, art. L.423-7</li>
 *   <li>{@code CST_VPF_LIENS_PERSONNELS} — Liens personnels et familiaux, art. L.423-23</li>
 *   <li>{@code REGROUPEMENT_FAMILIAL} — Regroupement familial, art. L.434-* (régime distinct de L.423-1)</li>
 *   <li>{@code ADMISSION_EXCEPTIONNELLE_AES} — Régularisation / AES, art. L.435-1</li>
 *   <li>{@code ASILE_OFPRA} — Demande d'asile, L.711-*</li>
 *   <li>{@code PROTECTION_SUBSIDIAIRE} — Protection subsidiaire, L.712-*</li>
 *   <li>{@code CARTE_RESIDENT_10ANS} — Carte de résident après 10 ans, art. L.426-1</li>
 *   <li>{@code NATURALISATION} — Acquisition nationalité française, art. 21-15 Code civil</li>
 * </ol>
 *
 * <p><b>Hors scope V1 explicites</b> (à ajouter si feedback terrain le demande) :
 * <ul>
 *   <li>CST Visiteur (L.426-20) — usage minoritaire</li>
 *   <li>Apatride (Conv. NY 1954) — cas rares</li>
 *   <li>Carte résident longue durée UE (L.426-8) — régime spécifique résident autre État UE</li>
 *   <li>Passeport-talent découpé en sous-types distincts (chercheur / salarié qualifié /
 *       entrepreneur) — groupé en V1 car pièces très proches</li>
 *   <li>Régularisation économique travail dissimulé — non couvert</li>
 * </ul>
 *
 * <p>Côté Belgique : les 4 régimes existants historiquement (étudiant, salarié,
 * regroupement, naturalisation) sont conservés. Les nouveaux régimes français V1
 * n'ont volontairement pas d'équivalent belge détaillé — le marché belge nécessite
 * un travail dédié avec un cabinet belge pour une couverture précise (à V2).
 */
public final class ImmigrationPieceReferentiel {

    private static final Map<String, Map<String, List<String>>> REFERENTIEL = Map.ofEntries(
            // ================================================================
            // ÉTUDES & FORMATION
            // ================================================================

            Map.entry("VISA_ETUDIANT", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Justificatif d'inscription dans un établissement d'enseignement",
                            "Justificatif de ressources suffisantes (615 €/mois minimum)",
                            "Justificatif d'hébergement",
                            "Photo d'identité",
                            "Formulaire de demande de visa complété"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité",
                            "Lettre d'admission d'un établissement d'enseignement belge",
                            "Preuve de paiement des frais d'inscription",
                            "Justificatif de ressources suffisantes (620 EUR/mois)",
                            "Assurance maladie couvrant la Belgique",
                            "Justificatif d'hébergement",
                            "Photo d'identité récente"
                    )
            )),

            Map.entry("APS_POST_ETUDES", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Titre de séjour étudiant en cours de validité",
                            "Diplôme au minimum de niveau master (ou équivalent)",
                            "Justificatif de recherche d'emploi OU projet de création d'entreprise",
                            "Justificatif de ressources (épargne, APL, aide parentale, etc.)",
                            "Justificatif de domicile < 3 mois",
                            "Photo d'identité (norme ANTS)",
                            "Formulaire CERFA 15186 (APS post-master)"
                    )
            )),

            // ================================================================
            // TRAVAIL & ÉCONOMIQUE
            // ================================================================

            Map.entry("TITRE_SALARIE", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Contrat de travail signé par l'employeur",
                            "Autorisation de travail délivrée par la DREETS",
                            "Justificatif de qualification professionnelle",
                            "Justificatif d'hébergement",
                            "Photo d'identité",
                            "Formulaire de demande CERFA"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité",
                            "Contrat de travail ou offre d'emploi signée",
                            "Permis de travail délivré par la région compétente",
                            "Diplômes ou qualifications professionnelles",
                            "Justificatif d'hébergement",
                            "Casier judiciaire du pays d'origine",
                            "Photo d'identité récente"
                    )
            )),

            Map.entry("PASSEPORT_TALENT", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Justificatif du régime visé (convention d'accueil chercheur OU contrat salarié qualifié ≥ 2× SMIC OU projet entrepreneurial)",
                            "Diplôme de niveau master minimum (ou équivalent expérience)",
                            "Justificatif de ressources stables (≥ salaire minimum au titre concerné)",
                            "Justificatif de domicile < 3 mois",
                            "Justificatif d'assurance maladie",
                            "Photo d'identité (norme ANTS)",
                            "Formulaire CERFA adapté (passeport talent)"
                    )
            )),

            // ================================================================
            // FAMILLE — régimes distincts à ne pas confondre
            // ================================================================

            Map.entry("CST_VPF_CONJOINT_FR", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité du demandeur",
                            "Justificatif du séjour régulier au moment de la demande (visa, titre en cours)",
                            "Acte de mariage intégral (copie < 3 mois)",
                            "Justificatif de nationalité française du conjoint (CNI, passeport, certificat de nationalité française)",
                            "Justificatif de communauté de vie effective (domicile commun, attestations, documents communs)",
                            "Justificatif de domicile du couple < 3 mois",
                            "Photo d'identité récente (norme ANTS)",
                            "Formulaire CERFA 15187 (demande carte VPF conjoint)"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité",
                            "Acte de mariage légalisé ou apostillé",
                            "Preuve de nationalité belge du conjoint",
                            "Preuve de cohabitation (composition de ménage récente)",
                            "Justificatif de séjour régulier en Belgique",
                            "Assurance maladie",
                            "Photo d'identité récente"
                    )
            )),

            Map.entry("CST_VPF_PARENT_ENFANT_FR", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité du demandeur",
                            "Justificatif du séjour régulier au moment de la demande",
                            "Acte de naissance intégral de l'enfant français (copie < 3 mois)",
                            "Justificatif de nationalité française de l'enfant (certificat de nationalité ou CNI)",
                            "Justificatif de la contribution effective à l'entretien et à l'éducation de l'enfant (attestations, versements, factures, relevés bancaires)",
                            "Justificatif de domicile < 3 mois",
                            "Photo d'identité récente (norme ANTS)",
                            "Formulaire CERFA adapté (parent d'enfant français)"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité",
                            "Acte de naissance de l'enfant belge légalisé",
                            "Preuve de nationalité belge de l'enfant",
                            "Preuve de contribution à l'entretien (cohabitation ou pension alimentaire)",
                            "Justificatif de séjour régulier",
                            "Photo d'identité récente"
                    )
            )),

            Map.entry("CST_VPF_LIENS_PERSONNELS", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Justificatif de séjour en France (durée, dates, preuves)",
                            "Justificatifs de liens personnels et familiaux en France (attestations de proches, preuves d'intégration)",
                            "Justificatif de l'intensité des attaches (scolarité enfants, activité associative, relations durables)",
                            "Justificatifs d'absence de liens avec le pays d'origine (peu de famille, pas de résidence récente)",
                            "Justificatif de domicile < 3 mois",
                            "Photo d'identité (norme ANTS)",
                            "Formulaire CERFA adapté (VPF liens personnels)"
                    )
            )),

            Map.entry("REGROUPEMENT_FAMILIAL", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité du demandeur (famille à l'étranger)",
                            "Titre de séjour du regroupant en France (validité ≥ 1 an)",
                            "Justificatif de ressources stables du regroupant (≥ SMIC net moyen sur 12 mois)",
                            "Justificatif de logement aux normes (surface habitable loi SRU par personne)",
                            "Acte de mariage OU actes de naissance des enfants (apostille + traduction)",
                            "Avis OFII favorable sur logement et ressources",
                            "Photo d'identité",
                            "Formulaire de demande de regroupement familial"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité du demandeur",
                            "Titre de séjour du regroupant en Belgique",
                            "Preuve du lien familial (acte de mariage ou naissance légalisé)",
                            "Justificatif de ressources stables et suffisantes",
                            "Justificatif de logement adéquat",
                            "Assurance maladie",
                            "Photo d'identité récente"
                    )
            )),

            // ================================================================
            // RÉGULARISATION & PROTECTION
            // ================================================================

            Map.entry("ADMISSION_EXCEPTIONNELLE_AES", Map.of(
                    "FRANCE", List.of(
                            "Passeport OU pièce d'identité d'origine",
                            "Justificatifs de présence continue en France (≥ 3 ans selon circulaire Valls)",
                            "Justificatifs d'intégration (bulletins scolaires enfants, engagement associatif, niveau français)",
                            "Justificatifs professionnels (fiches de paie, contrats, attestations employeur) si AES travail",
                            "Justificatif de domicile < 3 mois",
                            "Casier judiciaire vierge / attestations bonne moralité",
                            "Photo d'identité",
                            "Lettre motivée de demande d'admission exceptionnelle au séjour"
                    )
            )),

            Map.entry("ASILE_OFPRA", Map.of(
                    "FRANCE", List.of(
                            "Passeport OU pièce d'identité d'origine (si disponible)",
                            "Attestation de demandeur d'asile (ADA) délivrée par la préfecture",
                            "Récit des persécutions craintes (rédigé en français)",
                            "Pièces justifiant les persécutions (presse, attestations témoins, documents officiels)",
                            "Fiche familiale d'état civil",
                            "Photo d'identité (norme OFPRA)",
                            "Formulaire OFPRA dûment complété et signé"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport OU pièce d'identité d'origine (si disponible)",
                            "Annexe 26 remise à l'Office des étrangers",
                            "Récit de protection internationale",
                            "Pièces justifiant les persécutions",
                            "Photo d'identité",
                            "Formulaire de demande CGRA"
                    )
            )),

            Map.entry("PROTECTION_SUBSIDIAIRE", Map.of(
                    "FRANCE", List.of(
                            "Pièces identiques à ASILE_OFPRA",
                            "Éléments démontrant le risque de peine de mort OU de traitements inhumains OU de violence aveugle (conflit armé)",
                            "Rapports pays d'origine (Amnesty International, ONU, HCR)",
                            "Attestations de proches restés au pays",
                            "Toute autre pièce pertinente au regard de L.712-*"
                    )
            )),

            // ================================================================
            // LONGUE DURÉE & NATIONALITÉ
            // ================================================================

            Map.entry("CARTE_RESIDENT_10ANS", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Titres de séjour couvrant les 10 dernières années (pluriannuelle, salarié, VPF, etc.)",
                            "Justificatif de résidence habituelle et continue en France sur 10 ans (quittances, impôts, factures)",
                            "Justificatif d'intégration républicaine (niveau A2 minimum, valeurs République)",
                            "Justificatif de ressources stables et suffisantes",
                            "Casier judiciaire",
                            "Justificatif de domicile < 3 mois",
                            "Photo d'identité",
                            "Formulaire CERFA carte résident 10 ans"
                    )
            )),

            Map.entry("NATURALISATION", Map.of(
                    "FRANCE", List.of(
                            "Passeport en cours de validité",
                            "Titre de séjour valide depuis au moins 5 ans",
                            "Justificatif de résidence habituelle en France",
                            "Justificatif d'intégration (niveau de langue B1 minimum)",
                            "Justificatif de ressources stables",
                            "Casier judiciaire vierge",
                            "Formulaire de demande de naturalisation",
                            "Photo d'identité"
                    ),
                    "BELGIQUE", List.of(
                            "Passeport en cours de validité",
                            "Preuve de résidence légale en Belgique depuis 5 ans",
                            "Déclaration de nationalité ou demande de naturalisation",
                            "Preuve d'intégration sociale (participation civique, connaissance de la langue)",
                            "Justificatif de ressources suffisantes",
                            "Casier judiciaire belge et du pays d'origine",
                            "Photo d'identité récente"
                    )
            ))
    );

    private ImmigrationPieceReferentiel() {}

    public static List<String> getPieces(String titreType, String country) {
        Map<String, List<String>> byCountry = REFERENTIEL.get(titreType);
        if (byCountry == null) return List.of();
        return byCountry.getOrDefault(country, List.of());
    }

    public static boolean isTitreTypeValid(String titreType) {
        return REFERENTIEL.containsKey(titreType);
    }

    public static boolean isCountryValid(String country) {
        return "FRANCE".equals(country) || "BELGIQUE".equals(country);
    }

    /**
     * SF-IM-01-04 : infère le type de checklist le plus pertinent à partir
     * des données extraites par l'IA, en respectant la distinction juridique
     * entre régimes (L.423-1 vs L.423-7 vs L.434-* etc.).
     *
     * <p>Ordre de priorité :
     * <ol>
     *   <li><b>Événement déclencheur F-150</b> (signal le plus précis) : mariage →
     *       conjoint FR ; naissance enfant FR → parent d'enfant ; CDI → salarié ; etc.</li>
     *   <li><b>Titre cible suggéré</b> : CST_SALARIE → TITRE_SALARIE, CARTE_RESIDENT →
     *       NATURALISATION. CST_VPF seul n'est PAS mappé (ambigu entre conjoint / parent /
     *       liens personnels — ne jamais "deviner").</li>
     *   <li><b>Titre actuel</b> (fallback) : VLS_TS_ETUDIANT / CARTE_PLURIANNUELLE →
     *       VISA_ETUDIANT, CST_SALARIE → TITRE_SALARIE, CARTE_RESIDENT → NATURALISATION.</li>
     * </ol>
     *
     * @return type de checklist inféré, ou {@code null} si aucun mapping sûr
     *         (l'avocat choisit manuellement dans les 13 types disponibles)
     */
    public static String inferChecklistType(String currentTitleCode, String targetTitleCode,
                                             String triggerEventCode) {
        // 1. Événement déclencheur (signal le plus précis sur le régime juridique)
        if (triggerEventCode != null) {
            switch (triggerEventCode) {
                case "MARIAGE_RESSORTISSANT_FR":
                case "PACS_RESSORTISSANT_FR":
                case "VIOLENCES_CONJUGALES_CONSTATEES":
                    return "CST_VPF_CONJOINT_FR"; // L.423-1 CESEDA
                case "NAISSANCE_ENFANT_FR":
                    return "CST_VPF_PARENT_ENFANT_FR"; // L.423-7 CESEDA
                case "REGROUPEMENT_FAMILIAL_AUTORISE":
                    return "REGROUPEMENT_FAMILIAL"; // L.434-* CESEDA (régime distinct)
                case "CDI_OBTENU_SALARIE":
                    return "TITRE_SALARIE";
                case "DOCTORAT_OBTENU":
                    return "PASSEPORT_TALENT"; // L.421-14 chercheur
                case "ENTREE_LEGALE_10ANS":
                    return "CARTE_RESIDENT_10ANS";
                case "DEMANDE_ASILE_ACCORDEE_OFPRA":
                    return "ASILE_OFPRA";
                case "ENFANT_NE_FR_13ANS_PRESENCE":
                    return "NATURALISATION";
                default:
                    // fall through
            }
        }

        // 2. Titre cible explicite (F-IM-05 arbre décisionnel)
        if ("CST_SALARIE".equals(targetTitleCode)) return "TITRE_SALARIE";
        if ("CARTE_RESIDENT".equals(targetTitleCode)) return "CARTE_RESIDENT_10ANS";
        if ("APS".equals(targetTitleCode)) return "APS_POST_ETUDES";

        // 3. Titre actuel (cas renouvellement sans événement déclencheur)
        if ("CST_SALARIE".equals(currentTitleCode) || "VLS_TS_SALARIE".equals(currentTitleCode)) {
            return "TITRE_SALARIE";
        }
        if ("VLS_TS_ETUDIANT".equals(currentTitleCode) || "CARTE_PLURIANNUELLE".equals(currentTitleCode)) {
            return "VISA_ETUDIANT";
        }
        if ("APS".equals(currentTitleCode)) return "APS_POST_ETUDES";
        if ("CARTE_RESIDENT".equals(currentTitleCode)) return "CARTE_RESIDENT_10ANS";
        if ("RECEPISSE_ASILE".equals(currentTitleCode)) return "ASILE_OFPRA";

        // CST_VPF seul est ambigu — renvoie null pour forcer choix manuel
        return null;
    }

    /** Rétrocompat 2-args pré-SF-IM-01-04 (sans événement déclencheur). */
    public static String inferChecklistType(String currentTitleCode, String targetTitleCode) {
        return inferChecklistType(currentTitleCode, targetTitleCode, null);
    }
}
