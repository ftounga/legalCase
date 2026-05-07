# Audit juridique exhaustif — Outils décisionnels Droit de la famille (Famille FR)

**Feature** : F-191 — Audit Famille FR
**Date** : 2026-05-06
**Auteur** : delivery-orchestrator (audit autonome)
**Méthode** : croisement (a) migrations Liquibase `decision_tool_visibility_rules` (105/106/127→145, 151→155, 168/169, 177→190, 191/192) — (b) `TOOL_REGISTRY` frontend `decisional-tools-panel.component.ts` — (c) interface `FamilleExtractedData` (frontend `divorce-accepte.model.ts`, 138 champs détectés) — (d) Code civil français + Code de procédure civile + lois récentes.

**Objectif** : produire l'inventaire exhaustif, l'audit juridique exhaustif (en couvrant les 11 grandes branches du droit FR de la famille), la synthèse des manquants, et l'extension F-166 (ALWAYS_ON → CONTEXTUAL + flags IA Famille FR).

> Limitation explicite : tout article du Code civil ou du Code de procédure civile cité ci-dessous a été référencé dans la mesure du possible à partir de sources stables (Code civil annoté Dalloz / Légifrance, jurisprudence Cassation citée dans les briefs SF Famille déjà mergées). Quand un point spécifique n'a pas pu être vérifié à 100 %, il est marqué `(à vérifier)` afin de ne pas inventer.

---

## Sommaire

1. [Étape 1 — Inventaire Famille FR existant (Tableau A)](#etape-1)
2. [Étape 2 — Audit juridique exhaustif Famille FR (Tableau B)](#etape-2)
3. [Étape 3 — Audit F-166 Famille FR (ALWAYS_ON → CONTEXTUAL + flags IA, Tableau C)](#etape-3)
4. [Étape 4 — Synthèse, Top 10 manquants, découpages, hors périmètre](#etape-4)

---

## Étape 1 — Inventaire Famille FR existant — Tableau A <a id="etape-1"></a>

### A.1 — Méthodologie d'inventaire

**Sources croisées** :

- **DB** : agrégation des seeds `INSERT INTO decision_tool_visibility_rules` avec `legal_domain='DROIT_FAMILLE'` ET `country IN (NULL, 'FRANCE')` à partir des migrations 105 / 106 / 127–145 / 151–155 / 168 / 169 / 177–190 / 192.
- **Frontend** : entrées `TOOL_REGISTRY` dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`.
- **Backend** : présence d'un calculateur / analyzer / controller qui matérialise l'outil (implicite : table `*_analyses` créée par migration).

### A.2 — Tableau A : outils Famille FR existants au 2026-05-06

| # | tool_id (DB) | layer (DB) | trigger_field / value | TOOL_REGISTRY frontend | Backend (table `_analyses` / service) | Situation juridique couverte | Source CC/CPC |
|---|---|---|---|---|---|---|---|
| A1 | `F-FA-05-partage-immobilier` | ALWAYS_ON + CONTEXTUAL (`regime_matrimonial=COMMUNAUTE_LEGALE` ou `PARTICIPATION_ACQUETS`) | oui — partage indivis | oui (`partage-immobilier-section`) | calculateur partage soulte / récompenses | Liquidation immobilière entre époux post-divorce — partage en nature ou en valeur, soulte | art. 1467+ Cciv (liquidation), 815+ Cciv (indivision) |
| A2 | `F-FA-06-calendrier-garde` | ALWAYS_ON + CONTEXTUAL (`mode_garde_detaille` × 6 valeurs) | oui — calendrier garde | oui | Calendrier garde alternée / DVH classique / DVH élargi (FR), Alternée / Secondaire / Secondaire élargi (BE) | Modalités de résidence et droit de visite et hébergement | art. 373-2-9 Cciv |
| A3 | `F-FA-07-checklist-divorce` | ALWAYS_ON + CONTEXTUAL (`type_procedure_detectee=DIVORCE_CONSENTEMENT_MUTUEL`) | oui | oui | Checklist pièces requises convention divorce (notarié 2017+) | art. 229-1 Cciv (divorce sans juge) |
| A4 | `F-FA-08-divorce-alteration` | ALWAYS_ON | oui (`divorce-alteration-section`) | `divorce_alteration_analyses` (mig 127) | Divorce pour altération définitive du lien conjugal — délai séparation 1 an | art. 237-238 Cciv |
| A5 | `F-FA-09-divorce-faute` | ALWAYS_ON | oui (`divorce-faute-section`) | `divorce_faute_analyses` (mig 128) | Divorce pour faute — adultère, violation devoirs, violences | art. 242 Cciv + jurisprudence |
| A6 | `F-FA-10-divorce-accepte` | ALWAYS_ON | oui (`divorce-accepte-section`) | `divorce_accepte_analyses` (mig 129) | Divorce accepté (acceptation du principe de la rupture) | art. 233-234 Cciv |
| A7 | `F-FA-11-desunion-irremediable-be` | ALWAYS_ON `country='BELGIQUE'` | oui | mig 131 | Désunion irrémédiable BE — **BE seulement** (hors périmètre Famille FR mais référencé pour clarté) | art. 229 CC belge |
| A8 | `F-FA-12-mesures-provisoires` | ALWAYS_ON | oui | `mesures_provisoires_analyses` (mig 136) | Mesures provisoires JAF (résidence enfant, pension provisoire, jouissance logement) | art. 254 Cciv + 1118 CPC |
| A9 | `F-FA-13-revisions-post-divorce` | ALWAYS_ON | oui | `revisions_post_divorce_analyses` (mig 135) | Révision pension alimentaire / prestation compensatoire après divorce | art. 276-3 Cciv (PC) + 373-2-2 Cciv (PA) |
| A10 | `F-FA-14-ordonnance-protection` | ALWAYS_ON | oui (`ordonnance-protection-section`) | `ordonnance_protection_analyses` (mig 137) | Ordonnance de protection — violences conjugales/familiales, BAR (loi 2019) | art. 515-9 à 515-13 Cciv + loi 9/7/2010, 28/12/2019 |
| A11 | `F-FA-15-recompenses` | ALWAYS_ON | oui (`recompenses-section`) | `recompenses_analyses` (mig 138) | Récompenses entre époux et communauté (régime communauté légale) | art. 1433+ Cciv |
| A12 | `F-FA-16-communaute-universelle` | ALWAYS_ON | oui | `communaute_universelle_analyses` (mig 177) | Communauté universelle, clause attribution intégrale | art. 1526 + 1527 al. 2 Cciv |
| A13 | `F-FA-17-partage-judiciaire` | ALWAYS_ON | oui | `partage_judiciaire_analyses` (mig 169) | Partage judiciaire (art. 840+ Cciv) — désaccord coindivisaires, PV difficultés | art. 840+ Cciv + 1364+ CPC |
| A14 | `F-FA-18-adoption` | ALWAYS_ON | oui | `adoption_analyses` (mig 187) | Adoption plénière vs simple — pupille de l'État, conditions d'âge | art. 343-370-2 Cciv (réforme 21/2/2022) |
| A15 | `F-FA-18-reconnaissance-paternelle` | ALWAYS_ON | oui | `reconnaissance_paternelle_analyses` (mig 178) | Reconnaissance volontaire d'enfant — anté/postnatale | art. 316 + 332-335 + 372 Cciv |
| A16 | `F-FA-18-contestation-paternite` | ALWAYS_ON | oui | `contestation_paternite_analyses` (mig 181) | Contestation de paternité — qualité, délais, possession d'état | art. 332-335 + 311-1 + 321 + 372 Cciv |
| A17 | `F-FA-18-recherche-paternite` | ALWAYS_ON | oui | `recherche_paternite_analyses` (mig 183) | Action en recherche de paternité (judiciaire) — expertise ADN | art. 327+ Cciv |
| A18 | `F-FA-18-possession-etat` | ALWAYS_ON | oui | `possession_etat_analyses` (mig 185) | Filiation par possession d'état — délai 5 ans, présomptions | art. 311-1 + 311-2 + 317 Cciv |
| A19 | `F-FA-19-autorite-parentale` | ALWAYS_ON | oui | `autorite_parentale_analyses` (mig 139) | Exercice autorité parentale — conjoint/exclusif, délégation | art. 371+ Cciv |
| A20 | `F-FA-19-changement-residence` | ALWAYS_ON | oui | `changement_residence_analyses` (mig 144) | Changement de résidence d'enfant — information préalable autre parent | art. 373-2 Cciv |
| A21 | `F-FA-19-desaccords-parentaux` | ALWAYS_ON | oui | `desaccords_parentaux_analyses` (mig 145) | Désaccords parentaux (art. 373-2-10) — JAF saisi par requête, médiation préalable | art. 373-2-10 Cciv + loi 18/11/2016 art. 7 |
| A22 | `F-FA-20-pacs-dissolution` | ALWAYS_ON | oui | `pacs_dissolution_analyses` (mig 152) | Dissolution du PACS — déclaration commune, unilatérale, mariage | art. 515-7 Cciv |
| A23 | `F-FA-21-separation-corps` | ALWAYS_ON | oui | `separation_corps_analyses` (mig 154) | Séparation de corps + conversion divorce | art. 296-308 Cciv |
| A24 | `F-FA-22-indivision` | ALWAYS_ON | oui | `indivision_analyses` (mig 151) | Indivision conventionnelle / légale | art. 815+ Cciv |
| A25 | `F-FA-23-ordonnance-requete` | ALWAYS_ON FRANCE + BELGIQUE | oui | `ordonnance_requete_analyses` (mig 168) | Ordonnance sur requête — mesures urgentes familiales | art. 493+ CPC |
| A26 | `F-FA-24-devolution-legale` | ALWAYS_ON | oui | `devolution_legale_analyses` (mig 179) | Dévolution légale successorale — ordres / degrés | art. 731-755 Cciv |
| A27 | `F-FA-24-testament-validite` | ALWAYS_ON | oui | `testament_validite_analyses` (mig 182) | Validité testament (forme, sain d'esprit, quotité disponible) | art. 893-1100 Cciv |
| A28 | `F-FA-24-donation` | ALWAYS_ON | oui | `donation_analyses` (mig 184) | Donation entre vifs — forme, dispense rapport, quotité | art. 893+ + 894+ + 919+ Cciv |
| A29 | `F-FA-24-reserve-heriditaire` | ALWAYS_ON | oui | `reserve_heriditaire_analyses` (mig 186) | Réserve héréditaire — calcul, action en réduction | art. 912-928 Cciv |
| A30 | `F-FA-24-partage-successoral` | ALWAYS_ON | oui | `partage_successoral_analyses` (mig 188) | Partage successoral (notarié / judiciaire) — modes | art. 816+ Cciv |
| A31 | `F-FA-24-indivision-successorale` | ALWAYS_ON | oui | `indivision_successorale_analyses` (mig 189) | Indivision successorale — gestion, sortie | art. 815+ Cciv |
| A32 | `F-FA-24-rapport-succession` | ALWAYS_ON | oui | `rapport_succession_analyses` (mig 190) | Rapport des libéralités — donations rapportables | art. 843+ Cciv |
| A33 | `F-FA-25-majeurs-proteges` | ALWAYS_ON | oui (`majeurs-proteges-section`) | `majeurs_proteges_analyses` (mig 153) + référentiels (159/160/161) | Régimes de protection : sauvegarde, curatelle simple/renforcée, tutelle, mandat protection future, habilitation familiale | art. 425-494-1 Cciv |
| A34 | `F-FA-26-changement-etat-civil` | ALWAYS_ON | oui | `changement_etat_civil_analyses` (mig 155) | Changement nom / prénom / sexe à l'état civil | art. 60 + 61-1 + 61-5 Cciv |
| A35 | `F-FA-27-pma-gpa` | ALWAYS_ON | oui | `pma_gpa_bioethique_analyses` (mig 180) | PMA pour toutes (loi bioéthique 2/8/2021) — couples femmes / GPA reconnaissance étranger | loi 2/8/2021 + art. 342-9+ Cciv + Cass. 5/7/2017 |

### A.3 — Synthèse Tableau A

- **35 outils Famille FR uniques** présents à la fois (a) en seed `decision_tool_visibility_rules` `country=NULL ou FRANCE` et (b) en `TOOL_REGISTRY` frontend (entrée `['F-FA-XX', {...}]`).
- **+1 outil BE** (`F-FA-11-desunion-irremediable-be`) — hors périmètre Famille FR.
- **Layers** : 33 outils en ALWAYS_ON, 2 outils en CONTEXTUAL + ALWAYS_ON cumulés (F-FA-05, F-FA-06, F-FA-07).
- **Outils SUPPRIMÉS de la table de visibilité par migration 191 sans frontend** (cf. `191-realign-decision-tool-ids.xml`) puis **restaurés par migration 192** (race condition fix-up) : `F-FA-18-adoption`, `F-FA-24-partage-successoral`, `F-FA-24-indivision-successorale`, `F-FA-24-rapport-succession` — tous présents au 2026-05-06.
- **Outils mentionnés dans seeds initiaux mais sans frontend complet (DELETE par migration 191 sans restauration)** :
  - `F-FA-01-prestation-compensatoire` (DELETE) — calculateur backend disponible probable, composant frontend manquant ou présentationnel pur.
  - `F-FA-02-pension-alimentaire` (DELETE) — même remarque.
  - `F-FA-04-liquidation-communaute` (DELETE) — même remarque.
  - `F-152-divorce-consentement-scoring` (DELETE) — composant scoring intégré dans synthesis.component, pas auto-suffisant pour panel F-IA-04.
  - `F-153-fourchettes-jaf` (DELETE) — comparateur fourchettes JAF, présentationnel pur.

> **Remarque importante** : 5 outils (F-FA-01, F-FA-02, F-FA-04, F-152, F-153) sont **fonctionnellement attendus par les avocats** (prestation comp., pension alim., liquidation, scoring divorce CM, fourchettes JAF) mais **absents du panel F-IA-04** au 2026-05-06 faute de wrapper frontend auto-suffisant. Ce sont des **dettes de couverture** déjà identifiées par F-191 et candidats explicites à un rattrapage F-191/F-192.

### A.4 — Champs IA Famille FR (vue d'ensemble)

L'interface `FamilleExtractedData` (138 champs détectés agrégeant les 4 domaines de pré-fill SF-FA-XX-02) couvre principalement :

- **Régime matrimonial** : `regimeMatrimonialDetecte`, `patrimoineCommun`, `valeurImmeuble`, `capitalRestantDu`, `clauseAttributionIntegraleDetected`, `valeurCommunauteEurDetectee`, `enfantsNonCommunsDetected`, `contratNotarieDetected`.
- **Divorce / séparation / PACS** : `dureeMariageAnnees`, `revenusAnnuelsEpoux1Eur`, `revenusAnnuelsEpoux2Eur`, `dateAcceptationPV`, `dateSeparation`, `separationConsentue`, `dateConclusionPacs`, `modeDissolutionPacsDetecte`.
- **Filiation** : `consentementLibreDuPereDetected`, `paterniteVraisemblableDetected`, `enfantNonReconnuParAutrePereDetected`, `procedureRespecteeReconnaissanceDetected`, `dateNaissanceEnfantDetectee`, `expertiseAdnDemandeeDetected`, `motifsSerieuxDetected`, `possessionEtatConforme5AnsDetected`, `dispositifBioethiqueDetecte`.
- **Autorité parentale / désaccords / résidence** : `regimeExerciceActuel`, `dangerCaracterise`, `consentementAutreParent`, `interferenceVieEnfant`, `ageEnfants`, `domaineDesaccordDetecte`, `intensiteDesaccordDetecte`, `tentativesMediationDetectees`, `urgenceDetectee`, `raisonChangementDetectee`, `informePrealablement`, `modeResidenceActuel`.
- **Ordonnance protection** : `dateRequeteOP`, `violencesAllegueesDetectees`, `preuvesViolencesDetectees`, `dangerImmediatDetected`, `presenceEnfantsDetected`, `logementCommunDetected`, `victimeFinanciairementDependanteDetected`, `demandeurDejaProtegeDetected`.
- **Majeurs protégés** : `certificatMedicalCirconstancieDetected`, `dateCertificatMedicalDetected`, `consentementPersonneAProtegerDetected`, `demandeurFamilialDetected`, `actesEnvisagesDetected`, `incapaciteGestionQuotidienneDetected`, `altertationGraveDetected`, `mandatPrealableSigneDetected`, `formeMandatProtectionDetected`.
- **Changement état civil** : `typeChangementDetecte`, `motifChangementDetecte`, `dateNaissanceDemandeurDetectee`, `majeurDemandeurDetected`, `consentementParentalDetected`.
- **Indivision / partage** : `pvDifficultesEtablisDetected`, `tentativeAmiableEpuiseueeDetected`, `nombreCoindivisairesDetecte`, `valeurBiensIndivisionEur`, `modePartageDemandeDetecte`.
- **Succession** : `conjointSurvivantDetected`, `nbDescendantsDetecte`, `tousDescendantsCommunsAvecConjointDetected`, `nbFreresSoeursDetecte`, `nombreEnfantsSuccessionDetecte`, `montantSuccessionEurDetecte`, `montantLibsTotalEurDetecte`, `dateOuvertureSuccessionDetectee`, `nombreCoheritiersDetecte`, `dateDecesDetectee`, `typeIndivisionSuccessoraleDetecte`, `montantDonationsRecuesEurDetecte`, `valeurDonationAuJourPartageEurDetectee`, `qualiteHeritierRapportDetectee`, `donationDispenseDeRapportDetected`, `naturePresumeeNonRapportableDetected`.
- **Testament / donation** : `formeTestamentDetectee`, `dateRedactionTestamentDetectee`, `saineDEspritTestateurDetected`, `legsExcedeQuotiteDisponibleDetected`, `formeDonationDetectee`, `dateDonationDetectee`, `saineDEspritDonateurDetected`, `respectQuotiteDisponibleDetected`.
- **Adoption** : `formeAdoptionDemandeeDetected` (`PLENIERE` | `SIMPLE`), `pupilleEtatDetected`, `adoptantMarieDetected`, `ageAdoptantDetecte`, `ageAdopteDetecte`.

**Constat important** : la qualité du pré-fill est très inégale. La couverture est dense pour les outils de **filiation** et de **succession** (35+ champs détectés) ; elle est très limitée sur **divorce faute** (pas de flag `motif_faute_detecte`), **ordonnance de protection** (pas de flag pivot `ordonnance_protection_envisagee`), **divorce CM scoring** (pas de flag pivot `divorce_consentement_mutuel_envisage`), **mesures provisoires**, **séparation de corps**, **régime matrimonial liquidation**.

---

## Étape 2 — Audit juridique exhaustif Famille FR — Tableau B <a id="etape-2"></a>

### B.0 — Méthodologie

L'audit est structuré par **branche du droit** plutôt que par outil — afin de partir des situations juridiques exhaustives et de mesurer la couverture, pas l'inverse. Sources principales : Code civil français (art. 144-1100 + art. 720-1100 succession + art. 425-494-1 protection majeurs), Code de procédure civile (livre III JAF + livre IV protection), lois récentes (2010 violences, 2013 mariage pour tous, 2017 divorce notarié, 2019 réforme divorce, 2021 bioéthique, 2022 réforme adoption).

### B.1 — Branche : MARIAGE (formation, conditions, effets)

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? | Niveau attendu |
|---|---|---|---|---|---|
| B1.1 | Vérification empêchements à mariage (art. 144 âge, 161-164 parenté, 147 bigamie) | art. 144-148 + 161-164 Cciv | F-FA-MARIAGE-EMPECHEMENTS | **MANQUE** | 4 (arbre décisionnel) |
| B1.2 | Choix régime matrimonial (commun / séparation / participation / universel) avant mariage | art. 1387-1391 Cciv | F-FA-MARIAGE-CHOIX-REGIME | **MANQUE** | 6 (comparateur) |
| B1.3 | Validité contrat de mariage notarié | art. 1394 Cciv | F-FA-MARIAGE-CONTRAT | **MANQUE** | 5 (validité) |
| B1.4 | Annulation mariage (vice consentement, mariage forcé) | art. 180 Cciv + loi 4/4/2006 | F-FA-MARIAGE-ANNULATION | **MANQUE** | 5 |
| B1.5 | Logement familial — protection (cogestion 215 al. 3) | art. 215 al. 3 Cciv | F-FA-LOGEMENT-FAMILIAL | **MANQUE** | 4 |
| B1.6 | Devoirs des époux (fidélité, secours, assistance, contribution charges) | art. 212-214 Cciv | F-FA-DEVOIRS-EPOUX | non applicable (théorique, pas d'outil) | — |

**Couverture mariage** : 0 / 5 outils (B1.6 hors périmètre car déclaratif). **5 outils manquants**.

### B.2 — Branche : PACS / CONCUBINAGE

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B2.1 | Conclusion PACS — formalités, régime des biens (séparation par défaut, indivision sur option) | art. 515-1 à 515-7 Cciv | F-FA-PACS-CONCLUSION | **MANQUE** |
| B2.2 | Dissolution PACS (déclaration commune, unilatérale, mariage, décès) | art. 515-7 Cciv | F-FA-20-pacs-dissolution | **EXISTE** A22 |
| B2.3 | Liquidation PACS — partage indivision, créances entre partenaires | art. 515-5+ Cciv | F-FA-PACS-LIQUIDATION | **MANQUE** (ou couvert partiellement par F-FA-20) |
| B2.4 | Concubinage — preuve, effets (très limités), rupture | art. 515-8 Cciv + jurisprudence | F-FA-CONCUBINAGE-RUPTURE | **MANQUE** (cas fréquent en pratique : indivision logement, créances) |

**Couverture PACS / concubinage** : 1 / 4 outils. **3 outils manquants** (dont B2.4 cas fréquent).

### B.3 — Branche : DIVORCE (4 cas + procédure)

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B3.1 | Divorce consentement mutuel notarié (depuis 2017) | art. 229-1 + 229-3 Cciv | F-FA-07-checklist-divorce + F-152-divorce-consentement-scoring | **EXISTE PARTIEL** (checklist OK, scoring DELETE 191) |
| B3.2 | Divorce consentement mutuel judiciaire (mineur demandant audition) | art. 229-2 Cciv | F-FA-DIVORCE-CM-JUDICIAIRE | **MANQUE** (cas peu fréquent mais réel) |
| B3.3 | Divorce accepté (acceptation principe rupture) | art. 233-234 Cciv | F-FA-10-divorce-accepte | **EXISTE** A6 |
| B3.4 | Divorce altération définitive lien conjugal (1 an séparation) | art. 237-238 Cciv | F-FA-08-divorce-alteration | **EXISTE** A4 |
| B3.5 | Divorce pour faute | art. 242 Cciv + jurisprudence | F-FA-09-divorce-faute | **EXISTE** A5 |
| B3.6 | Procédure conjointe / requête conjointe (réforme 2019) | loi 23/3/2019 + art. 1107 CPC | F-FA-DIVORCE-PROCEDURE-CONJOINTE | **MANQUE** (vue procédure transverse) |
| B3.7 | Mesures provisoires JAF (résidence, pension provisoire, logement) | art. 254 Cciv + 1118 CPC | F-FA-12-mesures-provisoires | **EXISTE** A8 |
| B3.8 | Convention parentale annexée à convention divorce CM | art. 229-3 6° Cciv | F-FA-CONVENTION-PARENTALE | **MANQUE** (cas couvert partiellement par F-FA-07) |
| B3.9 | Audience JAF mesures provisoires (avant divorce 1118 CPC) | art. 1118 CPC | F-FA-AUDIENCE-MP | **MANQUE** (préparation audience) |
| B3.10 | Liquidation régime matrimonial post-divorce | art. 1467+ Cciv | F-FA-04-liquidation-communaute | **DELETE 191** (à restaurer) |
| B3.11 | Prestation compensatoire — calcul, forme (capital / rente) | art. 270-281 Cciv | F-FA-01-prestation-compensatoire | **DELETE 191** (à restaurer) |
| B3.12 | Révision pension alimentaire / prestation compensatoire post-divorce | art. 276-3 Cciv + 373-2-2 Cciv | F-FA-13-revisions-post-divorce | **EXISTE** A9 |
| B3.13 | Procédure participative entre avocats (alternative judiciaire) | art. 2062-2068 Cciv + 1542+ CPC | F-FA-DIVORCE-PROCEDURE-PARTICIPATIVE | **MANQUE** |

**Couverture divorce** : 6 outils existants + 2 DELETE à restaurer (F-FA-01, F-FA-04) + 5 manquants.

### B.4 — Branche : SÉPARATION DE CORPS

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B4.1 | Séparation de corps prononcée (4 cas comme divorce) | art. 296-308 Cciv | F-FA-21-separation-corps | **EXISTE** A23 |
| B4.2 | Conversion séparation de corps en divorce (2 ans) | art. 306 Cciv | (intégré dans F-FA-21) | EXISTE PARTIEL |

**Couverture séparation corps** : OK.

### B.5 — Branche : RÉGIMES MATRIMONIAUX (gestion, liquidation)

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B5.1 | Communauté légale — partage, soulte, bien immobilier | art. 1467+ Cciv | F-FA-05-partage-immobilier | **EXISTE** A1 |
| B5.2 | Récompenses époux ↔ communauté | art. 1433+ Cciv | F-FA-15-recompenses | **EXISTE** A11 |
| B5.3 | Communauté universelle + clause attribution intégrale | art. 1526 + 1527 al. 2 Cciv | F-FA-16-communaute-universelle | **EXISTE** A12 |
| B5.4 | Séparation de biens — créances entre époux | art. 1536+ Cciv | F-FA-SEPARATION-BIENS-CREANCES | **MANQUE** (cas fréquent : créances 214 Cciv) |
| B5.5 | Participation aux acquêts — calcul créance participation | art. 1569+ Cciv | F-FA-PARTICIPATION-ACQUETS | **MANQUE** |
| B5.6 | Changement de régime matrimonial (homologation 1397) | art. 1397 Cciv | F-FA-CHANGEMENT-REGIME | **MANQUE** |
| B5.7 | Donation entre époux pendant mariage | art. 1096 Cciv | F-FA-DONATION-ENTRE-EPOUX | **MANQUE** (distinct de B6.7 donation hors mariage) |

**Couverture régimes matrimoniaux** : 3 outils existants + 4 manquants.

### B.6 — Branche : SUCCESSIONS / LIBÉRALITÉS

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B6.1 | Dévolution légale (ordres / degrés) | art. 731-755 Cciv | F-FA-24-devolution-legale | **EXISTE** A26 |
| B6.2 | Réserve héréditaire / quotité disponible / action en réduction | art. 912-928 Cciv | F-FA-24-reserve-heriditaire | **EXISTE** A29 |
| B6.3 | Validité testament (forme, capacité, contenu) | art. 893-1100 Cciv (notamment 967-1001) | F-FA-24-testament-validite | **EXISTE** A27 |
| B6.4 | Donation entre vifs (forme, validité, dispense rapport) | art. 893+ + 932+ Cciv | F-FA-24-donation | **EXISTE** A28 |
| B6.5 | Indivision successorale (gestion, sortie) | art. 815+ Cciv | F-FA-24-indivision-successorale | **EXISTE** A31 |
| B6.6 | Partage successoral (notarié / judiciaire) | art. 816+ + 840+ Cciv | F-FA-24-partage-successoral | **EXISTE** A30 |
| B6.7 | Rapport des libéralités | art. 843+ Cciv | F-FA-24-rapport-succession | **EXISTE** A32 |
| B6.8 | Recel succession (art. 778) — peines | art. 778 Cciv | F-FA-RECEL-SUCCESSION | **MANQUE** |
| B6.9 | Indignité successorale (art. 726-727) | art. 726-727 Cciv | F-FA-INDIGNITE-SUCCESSORALE | **MANQUE** |
| B6.10 | Acceptation succession (pure et simple, à concurrence actif net, renonciation) | art. 768-781 Cciv | F-FA-ACCEPTATION-SUCCESSION | **MANQUE** (outil pivot — tous les avocats successoral le rencontrent) |
| B6.11 | Liquidation et partage notarial / déclaration succession | art. 870+ Cciv | F-FA-LIQUIDATION-NOTARIALE | **MANQUE** (cf. découpage succession judiciaire vs notariale) |
| B6.12 | Donation-partage (art. 1075+) | art. 1075-1080 Cciv | F-FA-DONATION-PARTAGE | **MANQUE** |
| B6.13 | Substitution fidéicommissaire (libéralités graduelles / résiduelles) | art. 1048-1061 Cciv | F-FA-LIBERALITES-GRADUELLES | **MANQUE** (cas patrimonial complexe) |
| B6.14 | Exhérédation / insertion clause pénale testamentaire | art. 900+ Cciv | F-FA-EXHEREDATION | **MANQUE** |
| B6.15 | Action en retranchement (art. 1527 al. 2) — enfant non commun + communauté universelle | art. 1527 al. 2 Cciv | (intégré dans F-FA-16) | EXISTE PARTIEL |
| B6.16 | Successions internationales (Règlement UE 650/2012) | Règlement 650/2012 | F-FA-SUCCESSION-INTERNATIONALE | **MANQUE** (de + en + fréquent) |
| B6.17 | Mandat à effet posthume | art. 812-812-7 Cciv | F-FA-MANDAT-POSTHUME | **MANQUE** |

**Couverture succession** : 7 outils existants + 10 manquants — c'est la branche la mieux couverte mais où les cas fréquents (acceptation, recel, indignité) manquent encore.

### B.7 — Branche : FILIATION

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B7.1 | Présomption paternité du mari | art. 312+ Cciv | F-FA-PRESOMPTION-PATERNITE | **MANQUE** (cas désaveu, désaveu pour cause d'adultère) |
| B7.2 | Reconnaissance volontaire d'enfant — anté/postnatale | art. 316 + 332-335 + 372 Cciv | F-FA-18-reconnaissance-paternelle | **EXISTE** A15 |
| B7.3 | Contestation paternité — qualité, délais (5 ans / 10 ans), possession d'état | art. 332-335 + 311-1 + 321 + 372 Cciv | F-FA-18-contestation-paternite | **EXISTE** A16 |
| B7.4 | Action en recherche paternité (judiciaire) — expertise ADN | art. 327+ Cciv | F-FA-18-recherche-paternite | **EXISTE** A17 |
| B7.5 | Filiation par possession d'état (constatation par acte de notoriété) | art. 311-1 + 311-2 + 317 Cciv | F-FA-18-possession-etat | **EXISTE** A18 |
| B7.6 | Filiation par PMA — couples femmes (loi bioéthique 2/8/2021) | loi 2/8/2021 + art. 342-9+ Cciv | F-FA-27-pma-gpa | **EXISTE PARTIEL** A35 |
| B7.7 | GPA étranger — reconnaissance enfants nés à l'étranger (Cass. 5/7/2017) | jurisprudence Cass. 5/7/2017 + art. 47 Cciv | (intégré dans F-FA-27-pma-gpa) | EXISTE PARTIEL |
| B7.8 | Action en rétablissement filiation (art. 322) | art. 322+ Cciv | F-FA-RETABLISSEMENT-FILIATION | **MANQUE** |
| B7.9 | Recherche origines / accès AAD (CNAOP / loi bioéthique 2021) | loi 2/8/2021 + Cciv adoption | F-FA-RECHERCHE-ORIGINES | **MANQUE** |
| B7.10 | Reconnaissance maternité (rare en FR — accouchement sous X) | art. 326 Cciv (accouchement sous X) | F-FA-ACCOUCHEMENT-X | **MANQUE** |

**Couverture filiation** : 5 outils existants + 4 manquants.

### B.8 — Branche : ADOPTION

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B8.1 | Adoption plénière (rupture filiation antérieure) | art. 343-359 Cciv | F-FA-18-adoption (form `PLENIERE`) | **EXISTE** A14 |
| B8.2 | Adoption simple (cumul filiations) | art. 360-370-2 Cciv | F-FA-18-adoption (form `SIMPLE`) | **EXISTE** A14 |
| B8.3 | Adoption couple homosexuel (loi mariage pour tous 17/5/2013) | loi 17/5/2013 + art. 343 Cciv | (intégré F-FA-18-adoption) | EXISTE PARTIEL |
| B8.4 | Adoption internationale — Convention La Haye 1993 | Convention La Haye 29/5/1993 + art. 370-3+ Cciv | F-FA-ADOPTION-INTERNATIONALE | **MANQUE** (cas fréquent — agrément, exequatur) |
| B8.5 | Adoption de l'enfant du conjoint (intra-familiale) | art. 343-1 al. 2 + 345-1 Cciv | F-FA-ADOPTION-INTRA | **MANQUE** (très fréquent : couple recomposé) |
| B8.6 | Réforme adoption 2022 — abaissement âge, conditions | loi 21/2/2022 | (intégré F-FA-18-adoption) | EXISTE PARTIEL |
| B8.7 | Pupille de l'État (placement préalable adoption) | art. L. 224 CASF | F-FA-PUPILLE-ETAT | **MANQUE** (article CASF, pas Cciv — frontière intervention) |

**Couverture adoption** : 1 outil principal + variantes — 3 manquants pour cas fréquents (intra-familiale, internationale).

### B.9 — Branche : AUTORITÉ PARENTALE / RÉSIDENCE / GARDE

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B9.1 | Exercice autorité parentale (conjoint / exclusif) | art. 371-372-2 Cciv | F-FA-19-autorite-parentale | **EXISTE** A19 |
| B9.2 | Délégation autorité parentale | art. 376-1 Cciv | F-FA-DELEGATION-AP | **MANQUE** (situé entre F-FA-19 et tiers — cas fréquent grand-parent) |
| B9.3 | Retrait autorité parentale (peines / violence) | art. 378+ Cciv + loi 2/3/2022 | F-FA-RETRAIT-AP | **MANQUE** (cas grave — 2/3/2022 violences) |
| B9.4 | Désaccord parental — saisine JAF (art. 373-2-10) | art. 373-2-10 Cciv + loi 18/11/2016 médiation | F-FA-19-desaccords-parentaux | **EXISTE** A21 |
| B9.5 | Changement de résidence enfant — info préalable autre parent | art. 373-2 Cciv | F-FA-19-changement-residence | **EXISTE** A20 |
| B9.6 | Calendrier garde (alternée / DVH classique / élargi) | art. 373-2-9 Cciv | F-FA-06-calendrier-garde | **EXISTE** A2 |
| B9.7 | Audition de l'enfant mineur par JAF (art. 388-1) | art. 388-1 Cciv | F-FA-AUDITION-ENFANT | **MANQUE** (cas fréquent : checklist conditions, demande, refus motivé) |
| B9.8 | Pension alimentaire enfant — calcul, barème, indexation INSEE | art. 371-2 Cciv + barème indicatif Cass. + art. L. 581-1 CSS | F-FA-02-pension-alimentaire | **DELETE 191** (à restaurer — cas pivot) |
| B9.9 | Recouvrement pension impayée — ARIPA | art. L. 581+ CSS + ARIPA | F-FA-ARIPA-RECOUVREMENT | **MANQUE** (FR-only — pas BE) |
| B9.10 | Saisie-attribution rémunération pension impayée | CPC exé. + L. 581 CSS | F-FA-SAISIE-PENSION | **MANQUE** |
| B9.11 | Allocation de soutien familial (CAF — pension impayée) | art. L. 523-1 CSS | F-FA-ASF-CAF | **MANQUE** |
| B9.12 | Prestation compensatoire — capital ou rente | art. 270-281 Cciv | F-FA-01-prestation-compensatoire | **DELETE 191** (à restaurer) |
| B9.13 | Conflit de résidence transfrontière — Règlement Bruxelles II ter (UE 2019/1111) | Règlement Bruxelles II ter | F-FA-TRANSFRONTIERE-ENFANT | **MANQUE** (cas fréquent : enlèvement international) |
| B9.14 | Convention de La Haye 1980 enlèvement enfant | Convention La Haye 25/10/1980 | (couvert avec B9.13) | **MANQUE** |
| B9.15 | Tribunal pour enfants — assistance éducative (art. 375+ Cciv + L. 375+) | art. 375+ Cciv + L. 375+ CASF | F-FA-ASSISTANCE-EDUCATIVE | **MANQUE** (FR-only — frontière entre civil et tribunal pour enfants) |
| B9.16 | Aide éducative à domicile (AED) / Action éducative en milieu ouvert (AEMO) | L. 222+ + L. 313+ CASF | F-FA-AED-AEMO | **MANQUE** (FR-only) |
| B9.17 | Ordonnance placement provisoire (OPP) — juge des enfants | art. 375-5 Cciv | F-FA-OPP | **MANQUE** (FR-only — juge des enfants) |

**Couverture autorité parentale** : 5 outils existants + 9 manquants — la pension alimentaire (B9.8) reste un trou critique (DELETE 191 sans wrapper frontend ; outil le plus utilisé en pratique).

### B.10 — Branche : VIOLENCES / ORDONNANCE PROTECTION

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B10.1 | Ordonnance de protection (violences conjugales / familiales) | art. 515-9 à 515-13 Cciv + loi 9/7/2010 | F-FA-14-ordonnance-protection | **EXISTE** A10 |
| B10.2 | Bracelet anti-rapprochement (BAR — loi 28/12/2019) | loi 28/12/2019 + art. 132-45-1 CP | (intégré dans F-FA-14) | EXISTE PARTIEL |
| B10.3 | Téléphone grave danger (TGD — loi 4/8/2014) | art. 41-3-1 CPP | F-FA-TGD | **MANQUE** (cas fréquent — outil pénal mais conseillé en civil) |
| B10.4 | Mesures éloignement conjoint violent (logement, contact, géolocalisation) | art. 515-11 Cciv | (intégré dans F-FA-14) | EXISTE PARTIEL |
| B10.5 | Délai 6 jours décision JAF ordonnance protection | art. 515-11 Cciv (à vérifier — délai 6 jours fixé par loi 30/7/2020) | (intégré F-FA-14) | EXISTE PARTIEL |
| B10.6 | Loi 30/7/2020 — protection victimes violences | loi 30/7/2020 | (intégré F-FA-14) | EXISTE PARTIEL |

**Couverture violences** : 1 outil + composantes — TGD à ajouter.

### B.11 — Branche : PROTECTION DES MAJEURS

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B11.1 | Sauvegarde de justice (art. 433+ Cciv) | art. 433-439 Cciv | (intégré F-FA-25) | EXISTE PARTIEL |
| B11.2 | Curatelle simple (art. 440 al. 1) | art. 440 al. 1 + 467 + 469 Cciv | (intégré F-FA-25, ref `CURATELLE_SIMPLE`) | **EXISTE** A33 |
| B11.3 | Curatelle renforcée (art. 472) | art. 472 Cciv | (intégré F-FA-25, ref `CURATELLE_RENFORCEE`) | **EXISTE** A33 |
| B11.4 | Tutelle (art. 440 al. 3) | art. 440 al. 3 + 425 + 473 + 510 Cciv | (intégré F-FA-25, ref `TUTELLE`) | **EXISTE** A33 |
| B11.5 | Mandat de protection future (art. 477-494) | art. 477-494 Cciv | (intégré F-FA-25, ref `MANDAT_PROTECTION_FUTURE`) | **EXISTE** A33 |
| B11.6 | Habilitation familiale (art. 494-1+) | art. 494-1 à 494-12 Cciv | (à intégrer F-FA-25) | **MANQUE** (alternative tutelle — cas fréquent simple) |
| B11.7 | Procuration testamentaire / mandat conventionnel pour gestion (alternative à tutelle) | art. 1984+ Cciv | F-FA-PROCURATION-MANDAT | **MANQUE** (utile lors de l'étape pré-tutelle) |
| B11.8 | Renouvellement mesure protection (5 ans → 10 ans si grave et durable) | art. 441 Cciv | (intégré F-FA-25) | EXISTE PARTIEL |
| B11.9 | Mainlevée mesure protection | art. 442 Cciv | (intégré F-FA-25) | EXISTE PARTIEL |
| B11.10 | Compte de gestion annuel curateur / tuteur | art. 510-515 Cciv | F-FA-COMPTE-GESTION | **MANQUE** |

**Couverture majeurs protégés** : 4 régimes intégrés F-FA-25 (curatelle simple/renforcée, tutelle, mandat protection future) + 4 sous-cas manquants (habilitation familiale = pivot).

### B.12 — Branche : ÉTAT CIVIL

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B12.1 | Changement nom / prénom / sexe à l'état civil (art. 60, 61-1, 61-5) | art. 60 + 61-1 + 61-5 Cciv | F-FA-26-changement-etat-civil | **EXISTE** A34 |
| B12.2 | Rectification erreur acte état civil | art. 99 Cciv | F-FA-RECTIFICATION-AC | **MANQUE** |
| B12.3 | Reconnaissance jugement étranger (exequatur acte d'état civil étranger) | art. 47 Cciv + Règlement Bruxelles II ter | F-FA-EXEQUATUR-AC | **MANQUE** |

**Couverture état civil** : 1 outil + 2 manquants.

### B.13 — Branche : AUTRES SITUATIONS RÉFÉRENCÉES

| ID | Situation juridique | Source CC/CPC | Outil correspondant | EXISTE ? |
|---|---|---|---|---|
| B13.1 | Indivision conventionnelle / légale (hors succession) | art. 815+ Cciv | F-FA-22-indivision | **EXISTE** A24 |
| B13.2 | Partage judiciaire (art. 840+) — désaccord coindivisaires | art. 840+ Cciv + 1364+ CPC | F-FA-17-partage-judiciaire | **EXISTE** A13 |
| B13.3 | Ordonnance sur requête (mesures urgentes familiales) | art. 493+ CPC | F-FA-23-ordonnance-requete | **EXISTE** A25 |
| B13.4 | Médiation familiale obligatoire pré-saisine JAF (art. 7 loi 18/11/2016) | art. 7 loi 18/11/2016 + art. 1108 CPC | F-FA-MEDIATION-OBLIGATOIRE | **MANQUE** (cas pivot — vérifier obligation) |
| B13.5 | Procédure participative (art. 2062 Cciv + 1542+ CPC) | art. 2062 Cciv + 1542+ CPC | F-FA-PROCEDURE-PARTICIPATIVE | **MANQUE** |
| B13.6 | Convention parentale (art. 373-2-7 Cciv — approbation JAF) | art. 373-2-7 Cciv | F-FA-CONVENTION-PARENTALE | **MANQUE** |
| B13.7 | Audience procédure conjointe (réforme 2019) | art. 1107+ CPC | (intégré dans procédures divorce) | **MANQUE** |
| B13.8 | Liquidation succession partage notaire — calendrier, étapes | art. 870+ Cciv | F-FA-LIQUIDATION-NOTARIAL | **MANQUE** (vu en B6.11) |

**Couverture autres** : 3 outils + 5 manquants.

### B.14 — Couverture par branche (résumé)

| Branche | Outils existants | Outils manquants | Couverture % | Notes |
|---|---|---|---|---|
| B.1 Mariage (formation) | 0 | 5 | 0% | Toute la branche manque (sauf déclaratif B1.6) |
| B.2 PACS / concubinage | 1 | 3 | 25% | F-FA-20 OK, manque conclusion + concubinage |
| B.3 Divorce (4 cas + procédure) | 6 | 5 (+2 DELETE) | 55% | F-FA-04 / F-FA-01 DELETE 191 critiques |
| B.4 Séparation de corps | 1 | 0 | 100% | OK |
| B.5 Régimes matrimoniaux | 3 | 4 | 43% | Séparation biens / changement régime / participation acquêts |
| B.6 Successions | 7 | 10 | 41% | Recel / indignité / acceptation = pivots manquants |
| B.7 Filiation | 5 | 4 | 56% | Présomption paternité / origines / accouchement X |
| B.8 Adoption | 1 | 3 | 25% | Intra-familiale + internationale = très fréquents |
| B.9 Autorité parentale / garde | 5 | 9 (+2 DELETE) | 33% | Pension alim. (B9.8) DELETE 191 = pivot critique |
| B.10 Violences / ordonnance protection | 1 | 1 | 50% | TGD à ajouter |
| B.11 Majeurs protégés | 1 (avec 4 ref) | 4 | 50% | Habilitation familiale + compte gestion |
| B.12 État civil | 1 | 2 | 33% | Rectification + exequatur |
| B.13 Autres | 3 | 5 | 38% | Médiation obligatoire + procédure participative |
| **TOTAL** | **35** | **55** (+ 4 DELETE 191 sans frontend) | **~39%** | |

---

## Étape 3 — Audit F-166 Famille FR — Tableau C <a id="etape-3"></a>

### C.0 — Méthode

F-166 a posé la règle suivante (Travail FR) : un outil ALWAYS_ON sans `trigger_field/value` apparaît systématiquement dans le panel F-IA-04 d'un dossier travail FR — y compris quand il est cliniquement non pertinent (surcharge cognitive avocat). La solution est de basculer ces outils en CONTEXTUAL avec un flag IA pivot (`<situation>_envisagee=true`) émis par le pipeline Sonnet niveau 3.

L'objectif Étape 3 est de **transposer F-166 sur Famille FR** : pour chaque outil ALWAYS_ON Famille FR, identifier (a) si le maintien ALWAYS_ON est cliniquement justifié — par exemple parce qu'il s'applique à 80%+ des dossiers familiaux —, ou (b) si le passage CONTEXTUAL est requis avec proposition de flag IA pivot.

### C.1 — Critère de classification

- **Conserver ALWAYS_ON** : outil pertinent dans la quasi-totalité des dossiers familiaux (calendrier garde, mesures provisoires, régime matrimonial liquidation, autorité parentale, partage immobilier, devoirs époux structurels). Probabilité d'utilité > 80%.
- **Basculer CONTEXTUAL** : outil très spécifique à une situation (PMA/GPA, ordonnance protection, désaccords parentaux, recel succession, divorce CM scoring, contestation paternité). Probabilité d'utilité < 30% si le dossier ne mentionne pas la situation.
- **Mixte ALWAYS_ON + CONTEXTUAL** (déjà existant pour F-FA-05 / F-FA-06 / F-FA-07) : conserver ALWAYS_ON pour le tronc commun, ajouter triggers CONTEXTUAL pour la qualification fine.

### C.2 — Tableau C : extension F-166 Famille FR

| # | tool_id | Layer actuel | Décision F-166 | Flag IA pivot proposé | Source détection | Priorité backlog | Note clinique |
|---|---|---|---|---|---|---|---|
| C1 | `F-FA-05-partage-immobilier` | ALWAYS_ON + CONTEXTUAL | **CONSERVER MIXTE** | (déjà existant) | (a) `regime_matrimonial=COMMUNAUTE_LEGALE\|PARTICIPATION_ACQUETS` ; (b) ALWAYS_ON tronc commun | Maintenu | Tronc commun divorce |
| C2 | `F-FA-06-calendrier-garde` | ALWAYS_ON + CONTEXTUAL | **CONSERVER MIXTE** | (déjà existant) | `mode_garde_detaille` × 6 valeurs FR + BE | Maintenu | Tronc commun mineurs |
| C3 | `F-FA-07-checklist-divorce` | ALWAYS_ON + CONTEXTUAL | **BASCULER CONTEXTUAL pur** | `divorce_consentement_mutuel_envisage` | Sonnet niveau 3 — détection mention "convention divorce", "CM", "229-1" | P1 | Pas de raison d'apparaître hors divorce CM |
| C4 | `F-FA-08-divorce-alteration` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `divorce_alteration_lien_envisage` | Sonnet niveau 3 — détection "altération", "1 an séparation", "237" | P1 | Hors séparation 1 an : non pertinent |
| C5 | `F-FA-09-divorce-faute` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `divorce_faute_envisage` | Sonnet niveau 3 — détection "violences", "adultère", "242", "abandon foyer" | P1 | Hors faute : 0% d'utilité |
| C6 | `F-FA-10-divorce-accepte` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `divorce_accepte_envisage` | Sonnet niveau 3 — détection "PV acceptation", "233-234", "principe de la rupture" | P1 | Hors divorce accepté : non pertinent |
| C7 | `F-FA-12-mesures-provisoires` | ALWAYS_ON | **CONSERVER ALWAYS_ON** | — | — | Maintenu | Tronc commun divorce judiciaire — utile dès qu'il y a divorce non amiable |
| C8 | `F-FA-13-revisions-post-divorce` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `revision_post_divorce_envisagee` | Sonnet — "révision pension", "276-3", "changement situation" | P2 | Apparaît post-divorce uniquement |
| C9 | `F-FA-14-ordonnance-protection` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `ordonnance_protection_envisagee` ou `violences_conjugales_detectees` | Sonnet — détection "violences", "515-9", "BAR", "main courante", "plainte violences" | P0 (critique) | Cas grave — pas dans le bruit du panel |
| C10 | `F-FA-15-recompenses` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `recompenses_envisagees` ou `regime_communaute_detecte` | Sonnet — `regimeMatrimonialDetecte=COMMUNAUTE_LEGALE` ou détection "récompenses", "1433" | P1 | Non pertinent en séparation biens / participation acquêts |
| C11 | `F-FA-16-communaute-universelle` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `regime_communaute_universelle_detecte` | Sonnet — `regimeMatrimonialDetecte=COMMUNAUTE_UNIVERSELLE` | P1 | Régime rare — non pertinent par défaut |
| C12 | `F-FA-17-partage-judiciaire` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `partage_judiciaire_envisage` | Sonnet — détection "PV difficultés", "tentative amiable épuisée", "840 Cciv" | P2 | Apparaît si désaccord coindivisaires |
| C13 | `F-FA-18-adoption` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `adoption_envisagee` | Sonnet — détection "adoption", "pupille", "agrément" | P0 | 0% d'utilité hors adoption |
| C14 | `F-FA-18-reconnaissance-paternelle` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `reconnaissance_paternelle_envisagee` | Sonnet — détection "reconnaissance enfant", "316" | P1 | Non pertinent si filiation déjà établie |
| C15 | `F-FA-18-contestation-paternite` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `contestation_paternite_envisagee` | Sonnet — détection "contestation paternité", "désaveu", "332-335" | P1 | Cas spécifique |
| C16 | `F-FA-18-recherche-paternite` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `recherche_paternite_envisagee` | Sonnet — détection "recherche paternité", "expertise ADN", "327" | P1 | Cas spécifique |
| C17 | `F-FA-18-possession-etat` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `possession_etat_envisagee` | Sonnet — détection "possession d'état", "311-1", "317" | P2 | Cas spécifique |
| C18 | `F-FA-19-autorite-parentale` | ALWAYS_ON | **CONSERVER ALWAYS_ON** | — | — | Maintenu | Tronc commun mineurs (utile dès que des enfants existent) |
| C19 | `F-FA-19-changement-residence` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `changement_residence_envisage` ou `desaccord_changement_residence_detecte` | Sonnet — détection "déménagement", "mutation", "373-2", `informePrealablement=false` | P1 | Apparaît si conflit changement résidence |
| C20 | `F-FA-19-desaccords-parentaux` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `desaccord_parental_detecte` | Sonnet — détection "désaccord", "373-2-10", `domaineDesaccordDetecte != null` | P0 | Si pas de désaccord détecté : pas pertinent |
| C21 | `F-FA-20-pacs-dissolution` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `pacs_dissolution_envisagee` | Sonnet — détection "PACS", "dissolution PACS", `dateConclusionPacs != null` | P1 | Cas spécifique |
| C22 | `F-FA-21-separation-corps` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `separation_corps_envisagee` | Sonnet — détection "séparation de corps", "296" | P2 | Cas peu fréquent — bon candidat CONTEXTUAL |
| C23 | `F-FA-22-indivision` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `indivision_envisagee` | Sonnet — détection "indivision", "815", "coindivisaire" | P2 | Cas patrimoine partagé hors mariage |
| C24 | `F-FA-23-ordonnance-requete` | ALWAYS_ON FR + BE | **BASCULER CONTEXTUAL** | `ordonnance_requete_envisagee` | Sonnet — détection "mesure urgente", "493 CPC" | P2 | Très spécifique procédure urgente |
| C25 | `F-FA-24-devolution-legale` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `succession_envisagee` | Sonnet — détection "succession", "décès", `dateOuvertureSuccessionDetectee != null` | P0 | Pivot succession — sans décès : 0% pertinence |
| C26 | `F-FA-24-testament-validite` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `testament_envisage` ou `succession_envisagee` | Sonnet — détection "testament", `formeTestamentDetectee != null` | P1 | Apparaît si testament mentionné |
| C27 | `F-FA-24-donation` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `donation_envisagee` | Sonnet — détection "donation", `formeDonationDetectee != null` | P1 | |
| C28 | `F-FA-24-reserve-heriditaire` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `reserve_hereditaire_envisagee` ou `succession_envisagee` | Sonnet — détection "réserve héréditaire", "quotité disponible", `legsExcedeQuotiteDisponibleDetected=true` | P1 | |
| C29 | `F-FA-24-partage-successoral` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `succession_envisagee` (pivot) ou `partage_successoral_envisage` | Sonnet — détection "partage succession", `modePartageDemandeDetecte != null` | P0 | |
| C30 | `F-FA-24-indivision-successorale` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `succession_envisagee` (pivot) ou `indivision_successorale_envisagee` | Sonnet — détection "indivision succession", `typeIndivisionSuccessoraleDetecte != null` | P0 | |
| C31 | `F-FA-24-rapport-succession` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `rapport_succession_envisage` | Sonnet — détection "rapport succession", `qualiteHeritierRapportDetectee != null`, `montantDonationsRecuesEurDetecte > 0` | P1 | |
| C32 | `F-FA-25-majeurs-proteges` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `protection_majeur_envisagee` | Sonnet — détection "tutelle", "curatelle", "425", `certificatMedicalCirconstancieDetected=true` | P1 | Cas spécifique |
| C33 | `F-FA-26-changement-etat-civil` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `changement_etat_civil_envisage` | Sonnet — détection "changement nom", "changement prénom", "61-1", "61-5", `typeChangementDetecte != null` | P2 | Cas spécifique |
| C34 | `F-FA-27-pma-gpa` | ALWAYS_ON | **BASCULER CONTEXTUAL** | `pma_gpa_envisagee` ou `dispositif_bioethique_detecte` | Sonnet — détection "PMA", "GPA", "couple femmes", `dispositifBioethiqueDetecte != null` | P0 | 0% d'utilité hors PMA/GPA — bruit majeur si maintenu ALWAYS_ON |

### C.3 — Synthèse Tableau C

- **34 outils ALWAYS_ON Famille FR** analysés (hors F-FA-11-desunion-irremediable-be qui est BE).
- **3 outils maintenus ALWAYS_ON** (cliniquement justifié) : `F-FA-12-mesures-provisoires`, `F-FA-19-autorite-parentale`, et le tronc commun de `F-FA-05`/`F-FA-06`/`F-FA-07` (mixte CONTEXTUAL + ALWAYS_ON).
- **31 outils à basculer CONTEXTUAL** avec flag IA pivot proposé (P0 = 7, P1 = 16, P2 = 8).
- **Flags IA pivot proposés** (à produire par pipeline Sonnet niveau 3) : `divorce_consentement_mutuel_envisage`, `divorce_faute_envisage`, `divorce_alteration_lien_envisage`, `divorce_accepte_envisage`, `mediation_familiale_obligatoire_detectee`, `violences_conjugales_detectees`, `ordonnance_protection_envisagee`, `pma_gpa_envisagee` / `dispositif_bioethique_detecte`, `gpa_etranger_detectee`, `succession_recel_detecte`, `succession_indignite_detectee`, `succession_envisagee` (pivot), `mineur_audition_demande_detectee`, `desaccord_parental_detecte`, `pension_alimentaire_impayee_detectee`, `regime_communaute_universelle_detecte`, `bracelet_anti_rapprochement_envisage`, `adoption_envisagee`, `protection_majeur_envisagee`, `partage_judiciaire_envisage`, `pacs_dissolution_envisagee`, `separation_corps_envisagee`, `indivision_envisagee`, `ordonnance_requete_envisagee`, `testament_envisage`, `donation_envisagee`, `reserve_hereditaire_envisagee`, `partage_successoral_envisage`, `indivision_successorale_envisagee`, `rapport_succession_envisage`, `changement_etat_civil_envisage`, `recompenses_envisagees`, `regime_communaute_detecte`, `revision_post_divorce_envisagee`, `reconnaissance_paternelle_envisagee`, `contestation_paternite_envisagee`, `recherche_paternite_envisagee`, `possession_etat_envisagee`, `changement_residence_envisage`, `desaccord_changement_residence_detecte`.

### C.4 — Conséquence pratique de F-166 Famille FR

Sur un dossier Famille FR vide aujourd'hui : 33 outils ALWAYS_ON apparaissent. Après F-166 Famille FR : 3 outils ALWAYS_ON visibles (mesures provisoires + autorité parentale + tronc commun), 31 outils en CONTEXTUAL qui n'apparaissent qu'au déclenchement du flag IA correspondant. Réduction du bruit panel F-IA-04 : ~91%.

---

## Étape 4 — Synthèse, Top 10 manquants, découpages, hors périmètre <a id="etape-4"></a>

### D.1 — Synthèse chiffrée Famille FR

| Indicateur | Valeur |
|---|---|
| Outils Famille FR existants (DB + frontend OK) | **35** |
| Outils Famille FR DELETE 191 sans frontend (gap connu) | **5** (F-FA-01, F-FA-02, F-FA-04, F-152, F-153) |
| Outils Famille FR potentiels couvrant exhaustivement le droit FR | **~90** (estimation prudente) |
| Couverture actuelle (outils existants / total potentiel) | **~39%** |
| Manquants critiques (pivot avocat) | **15** (cf. Top 10 ci-dessous) |
| Outils ALWAYS_ON à basculer CONTEXTUAL (F-166 Famille FR) | **31 / 34** |
| Flags IA pivot Famille FR à produire | **~30** |

### D.2 — Top 10 outils Famille FR manquants (pivots avocat)

Les 10 outils suivants sont les manquants les plus impactants — soit parce qu'ils sont au cœur du quotidien du droit de la famille (pension alimentaire, prestation comp., liquidation), soit parce qu'ils relèvent d'un cas fréquent absolument non couvert (adoption intra-familiale, médiation obligatoire, acceptation succession).

| Rang | tool_id proposé | Branche | Justification | Niveau | Priorité backlog |
|---|---|---|---|---|---|
| 1 | `F-FA-02-pension-alimentaire` (à restaurer) | B.9 Autorité parentale | Calcul + barème + indexation INSEE — outil le plus utilisé en pratique. DELETE 191 critique. | 3 (calculateur) | **P0** |
| 2 | `F-FA-01-prestation-compensatoire` (à restaurer) | B.3 Divorce | Capital ou rente + critères (durée mariage, revenus, âge). DELETE 191 critique. | 3 | **P0** |
| 3 | `F-FA-04-liquidation-communaute` (à restaurer) | B.5 Régimes matrimoniaux | Calcul masse commune / soulte / récompenses. DELETE 191 critique. | 3 | **P0** |
| 4 | `F-FA-MEDIATION-OBLIGATOIRE` | B.13 Procédure | Vérifier obligation médiation pré-saisine JAF (art. 7 loi 18/11/2016) | 1 (checklist) | **P0** |
| 5 | `F-FA-ACCEPTATION-SUCCESSION` | B.6 Succession | Pure et simple / à concurrence actif net / renonciation — pivot succession | 4 (arbre) | **P0** |
| 6 | `F-FA-ARIPA-RECOUVREMENT` | B.9 Autorité parentale | Recouvrement pension impayée (FR-only) | 2 (générateur) | **P1** |
| 7 | `F-FA-ADOPTION-INTRA` | B.8 Adoption | Adoption enfant du conjoint — couple recomposé fréquent | 4 | **P1** |
| 8 | `F-FA-DELEGATION-AP` | B.9 Autorité parentale | Délégation autorité parentale (grand-parent fréquent) | 4 | **P1** |
| 9 | `F-FA-AUDITION-ENFANT` | B.9 Autorité parentale | Conditions audition mineur 388-1, demande, refus motivé | 4 | **P1** |
| 10 | `F-FA-152-divorce-consentement-scoring` (à restaurer en wrapper auto-suffisant) | B.3 Divorce | Scoring viabilité divorce CM (déjà partiellement implémenté présentationnel) | 5 (scoring) | **P1** |

### D.3 — Outils FR-only sans équivalent BE

Le droit de la famille FR comporte **plusieurs institutions sans équivalent direct BE** qui imposent un outil FR-only — pas un outil mutualisé FR+BE :

| FR-only | Justification |
|---|---|
| `F-FA-01-prestation-compensatoire` | La prestation compensatoire FR (art. 270-281 Cciv) n'a pas d'équivalent direct BE — le droit belge raisonne en pension après divorce (art. 301 CC belge) avec des critères et barèmes différents (à vérifier). |
| `F-FA-ARIPA-RECOUVREMENT` | ARIPA (Agence recouvrement impayés pensions alimentaires) est un dispositif FR (CSS L. 581+) sans équivalent BE — la Belgique a SECAL (Service des créances alimentaires) qui fonctionne différemment. |
| `F-FA-CONVENTION-PARENTALE` (notarié 2017) | Convention parentale annexée à convention divorce CM est une création FR loi 2/2017. BE n'a pas de divorce notarié. |
| `F-FA-ASSISTANCE-EDUCATIVE` (juge des enfants) | Tribunal pour enfants FR (art. 375+ Cciv + L. 375+ CASF) — BE a juge de la jeunesse mais structure différente. |
| `F-FA-AED-AEMO` | AED / AEMO sont des mesures FR de protection enfance (CASF) sans équivalent terminologique BE. |
| `F-FA-OPP` | Ordonnance placement provisoire FR (art. 375-5 Cciv) — l'équivalent BE serait à analyser séparément (à vérifier — placement urgent juge de la jeunesse BE). |
| Loi mariage pour tous 2013 | Loi FR — BE ouverte au mariage homosexuel depuis 2003. Le timing et les conséquences sur la filiation sont distincts. |
| Réforme adoption 2022 | Loi 21/2/2022 FR — BE a sa propre réforme. |
| Loi bioéthique 2/8/2021 (PMA toutes) | FR — BE l'avait depuis 2007. Régime PMA distinct. |
| BAR (bracelet anti-rapprochement) | Loi 28/12/2019 FR — BE évalue son propre dispositif (à vérifier). |
| TGD (téléphone grave danger) | Loi 4/8/2014 FR — BE a un dispositif différent (à vérifier). |
| ASF (allocation soutien familial CAF) | L. 523-1 CSS — BE a SECAL (équivalent fonctionnel mais structure et conditions distinctes). |

**Conclusion** : il existe au moins **12 outils FR-only** justifiés — qui ne doivent pas être mutualisés en outil FR+BE. La parité Famille BE devra créer ses propres outils jumeaux quand ils existent (SECAL, etc.) ou ouvrir des features dédiées.

### D.4 — Découpages à éclater

Plusieurs outils Famille FR aujourd'hui mono-fichier mélangent en réalité plusieurs situations distinctes. Ils doivent être éclatés conformément à la règle CLAUDE.md « un outil décisionnel = une situation métier ».

| Outil actuel | Situations mélangées | Découpage proposé |
|---|---|---|
| `F-FA-07-checklist-divorce` | Couvre uniquement divorce CM notarié, mais nom suggère « divorce » générique | Renommer `F-FA-07-checklist-divorce-cm` + créer outils dédiés pour autres procédures (assignation contentieux, requête conjointe). |
| `F-FA-18-adoption` | Adoption plénière + simple + intra-familiale + internationale + couple homosexuel | À éclater : `F-FA-18-adoption-pleniere`, `F-FA-18-adoption-simple`, `F-FA-18-adoption-intra`, `F-FA-18-adoption-internationale`. |
| `F-FA-25-majeurs-proteges` | 5 régimes (sauvegarde, curatelle simple/renforcée, tutelle, mandat protection future) + habilitation familiale absente | Maintenir 1 outil arbre décisionnel sélecteur de régime, mais ajouter `F-FA-25-habilitation-familiale` séparé (pivot) + `F-FA-25-compte-gestion`. |
| `F-FA-27-pma-gpa` | PMA + GPA + bioéthique 2021 | À éclater : `F-FA-27-pma` (bioéthique 2021 PMA toutes) + `F-FA-27-gpa-etranger-reconnaissance` (Cass. 5/7/2017). |
| `F-FA-24-partage-successoral` | Partage notarié + partage judiciaire | À éclater : `F-FA-24-partage-notarial` + `F-FA-24-partage-successoral-judiciaire`. La voie procédurale est radicalement différente. |
| `F-FA-13-revisions-post-divorce` | Révision PA enfant (art. 373-2-2) + révision PC (art. 276-3) | À éclater si le contenu diverge : `F-FA-13-revision-pa` + `F-FA-13-revision-pc`. À examiner. |
| `F-FA-14-ordonnance-protection` | OP + BAR + TGD + mesures éloignement | OP cœur central — TGD à séparer (procédure pénale différente, art. 41-3-1 CPP) en `F-FA-TGD`. |
| `F-FA-19-autorite-parentale` | Exercice + délégation + retrait | Conserver exercice = tronc commun, créer `F-FA-DELEGATION-AP` + `F-FA-RETRAIT-AP`. |

**8 découpages à instruire.**

### D.5 — Hors périmètre / honnêteté

Les éléments suivants n'ont **pas** été couverts par cet audit, à dessein :

- **Outils Famille BE** : explicitement hors périmètre (cf. mission §contraintes). L'audit BE devra être conduit séparément, avec ses sources propres (CC belge, CIR, art. 229+ CC belge, SECAL, etc.).
- **Outils niveau ≥ 5 nouveaux** (scoring, comparateurs, détection événements) : les manquants identifiés Top 10 sont en majorité de niveau 3-4 (calculateurs, arbres décisionnels). Un audit niveau ≥ 5 séparé est utile pour sortir des outils de différenciation produit (ex. comparateur stratégies divorce, scoring viabilité PMA, détection événement violences).
- **Vérification fine articles** : tous les articles Code civil ci-dessus ont été référencés à partir de sources stables, mais quelques articles de procédure (CPC) et lois récentes (notamment délais ordonnance protection, art. CASF placement) sont marqués `(à vérifier)`. Une revue avec un référent métier juridique est recommandée avant de transformer cet audit en mini-specs SF.
- **Articulation droit pénal** : violences conjugales / TGD / BAR ont des composantes pénales (CPP) que cet audit traite uniquement par leur volet civil (art. 515-9+ Cciv). Un audit pénal frontalier serait utile pour les avocats Famille qui plaident aussi en pénal.
- **Articulation droit fiscal** : la donation, le partage, la liquidation succession ont des conséquences fiscales (CGI, droits de mutation, plus-values immobilières). Hors périmètre LegalCase V1 (cf. CLAUDE.md V1 = droit du travail). À ouvrir si le scope LegalCase est étendu au fiscal.
- **Conventions internationales** : Bruxelles II ter, La Haye 1980, La Haye 1993 sont mentionnées mais pas développées (5 outils manquants : B6.16, B7.9, B8.4, B9.13, B9.14). Un audit international Famille FR serait utile.
- **Procédures collectives spécifiques** : surendettement / faillite civile (procédure civile droit local Alsace-Moselle, art. L. 670 CCH) impactent la famille (logement) — hors scope.
- **Articulation droit social** : RSA, allocation logement, AAH, AVPF (assurance vieillesse parents au foyer) impactent indirectement le droit de la famille — hors scope ou périmètre frontalier à arbitrer.

### D.6 — Recommandations d'instruction (suite F-191)

1. **Ouvrir 5 SF de rattrapage immédiat** pour les outils DELETE 191 sans frontend qui sont des pivots avocat critiques :
   - SF-191-01 → wrapper frontend `F-FA-01-prestation-compensatoire`.
   - SF-191-02 → wrapper frontend `F-FA-02-pension-alimentaire`.
   - SF-191-03 → wrapper frontend `F-FA-04-liquidation-communaute`.
   - SF-191-04 → wrapper frontend `F-152-divorce-consentement-scoring`.
   - SF-191-05 → wrapper frontend `F-153-fourchettes-jaf`.
2. **Ouvrir une feature F-192 (ou jumelle de F-166) Famille FR** pour basculer 31 outils ALWAYS_ON en CONTEXTUAL avec ~30 nouveaux flags IA Sonnet niveau 3. Plan à itérer par lots de 5 outils.
3. **Ouvrir 10 SF backlog Top 10 manquants** (cf. D.2) — pension alimentaire, prestation compensatoire, liquidation, médiation obligatoire, acceptation succession, ARIPA, adoption intra, délégation AP, audition enfant, scoring divorce CM.
4. **Ouvrir 8 SF de découpage** (cf. D.4) — adoption, PMA/GPA, partage successoral, ordonnance protection, autorité parentale.
5. **Ouvrir audit Famille BE** — mêmes 11 branches, sources CC belge, SECAL, art. 229+ CC belge, juge de la paix vs juge de la jeunesse, etc. Symétrique à F-191.
6. **Réviser règle CLAUDE.md « parité des domaines métier »** — si Famille FR seule reçoit F-166 (passage CONTEXTUAL), Travail BE doit recevoir le pendant + Famille BE et Immigration FR/BE doivent passer leur audit équivalent dans les 10 features qui suivent.

---

## Annexe — Articles Code civil français cités (synthèse)

- **Mariage** : art. 144-228 (formation, conditions, devoirs, contrats matrimoniaux), art. 215 al. 3 (logement familial cogestion).
- **PACS** : art. 515-1 à 515-7-1 (conclusion, dissolution, régime).
- **Divorce** : art. 229 (4 cas), 229-1 à 229-3 (CM notarié), 233-234 (accepté), 237-238 (altération), 242 (faute), 254 (mesures provisoires), 270-281 (prestation compensatoire), 296-308 (séparation de corps + conversion).
- **Régimes matrimoniaux** : art. 1387-1581 (général), 1397 (changement régime), 1433+ (récompenses), 1467+ (liquidation), 1526-1527 al. 2 (communauté universelle + retranchement), 1536+ (séparation biens), 1569+ (participation acquêts).
- **Filiation** : art. 311-1 à 322 (présomption, possession d'état), 316 (reconnaissance), 326 (accouchement sous X), 327+ (recherche paternité), 332-335 (contestation), 342-9+ (PMA bioéthique 2021).
- **Adoption** : art. 343-370-2 (plénière + simple + variantes — réforme 21/2/2022), 343-1 al. 2 + 345-1 (intra-familiale), 370-3+ (internationale).
- **Autorité parentale** : art. 371 (principe), 371-2 (contribution éducation), 372-2 (exercice), 373-2 (résidence + info préalable), 373-2-2 (révision PA), 373-2-7 (convention parentale), 373-2-9 (modes garde), 373-2-10 (saisine JAF), 376-1 (délégation), 378+ (retrait), 388-1 (audition mineur).
- **Violences** : art. 515-9 à 515-13 (ordonnance protection), loi 9/7/2010, 28/12/2019 (BAR), 30/7/2020.
- **Successions** : art. 720-1100, notamment 726-727 (indignité), 731-755 (dévolution légale), 768-781 (acceptation), 778 (recel), 815+ (indivision), 816+ (partage), 840+ (partage judiciaire), 843+ (rapport), 870+ (liquidation notariale), 893-1100 (libéralités), 894+ (donation), 900+ (clauses), 912-928 (réserve héréditaire / quotité), 919+ (rapport libéralités), 932+ (forme), 967-1001 (testaments), 1048-1061 (libéralités graduelles), 1075-1080 (donation-partage), 1096 (donation entre époux), Règlement UE 650/2012 (successions internationales).
- **Protection majeurs** : art. 425-494-1 (sauvegarde, curatelle simple 440 al. 1, curatelle renforcée 472, tutelle 440 al. 3, mandat protection future 477-494, habilitation familiale 494-1+), 510-515 (compte gestion), 510 (compte annuel), 1984+ (mandat conventionnel).
- **État civil** : art. 47 (acte étranger), 60 (changement prénom), 61-1 (changement nom), 61-5 (changement sexe), 99 (rectification).
- **Procédure civile (CPC)** : art. 493+ (ordonnance sur requête), 1070+ (compétence JAF), 1107+ (procédure conjointe), 1118 (mesures provisoires), 1364+ (partage judiciaire), 1542+ (procédure participative).
- **Lois récentes** : 17/5/2013 mariage pour tous, 18/11/2016 médiation familiale obligatoire (art. 7), 4/4/2006 mariage forcé, 2017 divorce notarié, 23/3/2019 réforme divorce, 28/12/2019 BAR, 30/7/2020 protection victimes, 2/8/2021 bioéthique, 21/2/2022 réforme adoption, 2/3/2022 retrait AP violences, 4/8/2014 TGD.
- **Conventions internationales** : Convention La Haye 25/10/1980 (enlèvement enfant), Convention La Haye 29/5/1993 (adoption internationale), Règlement Bruxelles II ter UE 2019/1111 (responsabilité parentale + matrimoniale).
- **Code action sociale et familles (CASF)** : L. 222+ (AED), L. 224 (pupille de l'État), L. 313+ (AEMO), L. 375+ (assistance éducative).
- **Code sécurité sociale (CSS)** : L. 523-1 (ASF allocation soutien familial), L. 581-1+ (recouvrement pensions impayées ARIPA).
- **Code procédure pénale (CPP)** : art. 41-3-1 (TGD téléphone grave danger).

> Cet audit a été structuré pour servir de **référence à mini-specs F-191/F-192** et à l'arbitrage de la couverture Famille FR. Il **ne se substitue pas** à un audit métier juridique conduit par un avocat famille spécialisé, qui reste recommandé avant tout chantier de découpage à grande échelle.
