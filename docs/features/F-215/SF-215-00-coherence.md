# Cadrage cohérence — F-215 — P2 Immigration BE (étape 0)

**Date** : 2026-05-20
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Feature parente** : `F-215` — P2 Immigration BE — ~10 outils fréquence haute
**Sources** : `docs/features/F-191/audit-immigration-be-exhaustif.md` (Tableau B § 3.1 à § 3.13) ; PRODUCT_SPEC.md ligne F-215 ; état livraisons F-209 + F-203 + F-246-20.

---

## Contexte et déclencheur

F-209 (P1 Immigration BE ✅ Terminée) couvre 4 outils d'urgence procédurale : `F-IM-08-annexe13-be` (OQT Annexe 13), `F-IM-14-9bis-humanitaire-be`, `F-IM-14-9ter-medical-be`, `F-IM-14-40ter-familial-belge-be`. F-203 a basculé ces 4 outils + `F-IM-14-40bis-cohabitant-ue-be` en CONTEXTUAL avec 5 flags IA. F-246-20 a ensuite branché le pré-remplissage IA sur ces 4 outils (champs date/valeur). Les 4 outils transversaux restent ALWAYS_ON (F-IM-01/05/06/07).

La couverture P1 étant acquise, F-215 (P2) traite les **situations de fréquence haute** qu'un avocat belge rencontre en routine de cabinet immigration. L'audit F-191 § 5.2 (Top 10 manquants) et les sections § 3.1 à § 3.13 définissent le périmètre.

---

## Workflow métier réel — avocat belge en droit des étrangers

L'avocat immigration belge instruit des dossiers selon 6 piliers :

1. **Titres de séjour** : accompagner le demandeur dans l'obtention / renouvellement du bon titre (carte A, B, C, F, F+, K/L, single permit). Le single permit (loi 30/04/1999, AR 02/09/2018) est la procédure centrale pour les ressortissants tiers travailleurs depuis 2019.
2. **Regroupement familial** : orienter selon que le regroupant est Belge (40ter — couvert F-209), ressortissant UE (40bis — couvert F-203), ou ressortissant tiers en séjour limité (10bis) / illimité (10ter) ; calculer le seuil de ressources (≈ 1 500 €/mois).
3. **Nationalité belge** : évaluer les voies d'acquisition (déclaration art. 12bis code nationalité 1984 — voie principale ; mariage avec Belge art. 16 ; acquisition par mineur). Demande croissante.
4. **Mineurs non accompagnés (MENA)** : désignation tuteur DGDE + AESM (admission exceptionnelle séjour mineur) — procédure distincte du 9bis adulte, critique car aucun équivalent FR direct.
5. **Contentieux CCE** : recours en annulation (30 j calendaires) et en extrême urgence (5 j ouvrables BelgianBusinessDays) contre les décisions de l'OE / CGRA. Le générateur générique F-IM-06 couvre partiellement mais sans les délais spécifiques CCE ni la vérification `BelgianBusinessDaysCalculator`.
6. **Éloignement / interdiction d'entrée** : Annexe 13quinquies OQT + IE (art. 74/11) — différent de l'Annexe 13 simple déjà couverte par F-IM-08.

Séquence type : consultation → diagnostic titre/situation → dépôt dossier OE → suivi instruction → recours CCE si refus → exécution / appel CE.

---

## Cartographie features existantes ↔ workflow

| Pilier | Outil existant | Couverture BE |
|--------|----------------|---------------|
| Arbre décisionnel titre | F-IM-05-arbre-decisionnel-titre (ALWAYS_ON transversal) | ✅ partiel (single permit + 10ter manquants dans les branches) |
| Checklist pièces | F-IM-01-checklist-pieces (ALWAYS_ON transversal) | ✅ partiel (AESM/apatride non seedés) |
| OQT Annexe 13 simple | F-IM-08-annexe13-be (CONTEXTUAL BE) | ✅ livré F-209 |
| 9bis humanitaire | F-IM-14-9bis-humanitaire-be (CONTEXTUAL BE) | ✅ livré F-209 |
| 9ter médical | F-IM-14-9ter-medical-be (CONTEXTUAL BE) | ✅ livré F-209 |
| 40bis membre famille UE | F-IM-14-40bis-cohabitant-ue-be (CONTEXTUAL BE) | ✅ livré F-203 |
| 40ter regroupement avec Belge | F-IM-14-40ter-familial-belge-be (CONTEXTUAL BE) | ✅ livré F-209 |
| Générateur recours CGRA/CCE/CE | F-IM-06-recours (ALWAYS_ON transversal, 3 types BE) | ✅ partiel — CCE dédié manquant |
| Droit au travail | F-IM-07-droit-au-travail (ALWAYS_ON transversal) | ✅ livré |
| Single permit (loi 30/04/1999) | — | ❌ trou majeur |
| Regroupement 10bis/10ter ressortissant tiers | — | ❌ trou majeur |
| Seuil ressources regroupement | — | ❌ trou |
| Naturalisation (Code nationalité 1984 art. 12bis) | — | ❌ trou majeur |
| Naturalisation conjoint Belge (art. 16) | — | ❌ trou |
| MENA tutelle DGDE + AESM | — | ❌ trou majeur |
| Recours CCE annulation 30j / extrême urgence 5j | — (partiel F-IM-06) | ❌ outil dédié manquant |
| Annexe 13quinquies OQT + interdiction d'entrée | — | ❌ trou |
| Protection temporaire Ukraine | — | ❌ trou P2 actif 2026 |
| Protection subsidiaire asile | — | ❌ trou |

---

## Challenge de cohérence amont — les pré-requis existent-ils ?

| Pré-requis | État | Verdict |
|------------|------|---------|
| Pattern outil décisionnel (Calculator + endpoint + composant Angular + F-IA-04 + F-IA-03) | Éprouvé sur 9 outils Immigration BE (F-209 / F-203) | ✅ disponible |
| `ImmigrationExtractedData` record + 20 flags BE (F-203 + F-246-20) | Livré. 5 flags actifs + 15 flags pour outils futurs dont `single_permit_envisage`, `naturalisation_be_envisagee`, `mineur_non_accompagne_be_detecte`, etc. | ✅ disponible — flags à activer |
| BelgianBusinessDaysCalculator | Livré (F-IM-08-annexe13-be) | ✅ disponible — à réutiliser pour CCE |
| Référentiels procéduraux BE | `RECOURS_CCE`, `RECOURS_CE_BELGIQUE` seedés (F-IM-06) | ✅ disponible |
| `TOOL_REGISTRY` + `KNOWN_FRONTEND_TOOL_IDS` + garde-fous CI | Pattern établi, garde-fous actifs | ✅ disponible |
| Gate `workspaceCountry === 'BELGIQUE'` | Implémenté sur 5 outils existants | ✅ disponible |
| Flags IA futurs F-215 dans prompt `IMMIGRATION_INSTRUCTION` | F-203 a semé 15 flags futurs (`single_permit_envisage`, `naturalisation_be_envisagee`, etc.) — non encore actifs dans `decision_tool_visibility_rules` | ⚠️ activer à chaque SF backend |

---

## Challenge de cohérence aval — la sortie est-elle exploitable ?

Les verdicts alimentent `app-case-dashboard` (synthèse décisionnelle) et les pistes stratégiques F-176 — chaînage établi et opérationnel sur les outils existants. F-215 s'y branche à l'identique. ✅

---

## Sélection des 10 outils P2 — justification

Le PRODUCT_SPEC.md ligne F-215 cite : « AES BE, single permit (loi 30/04/1999), naturalisation BE (Code nationalité 1984), MENA AESM tutelle, regroupement 10ter ressortissant tiers, recours CCE ». L'audit F-191 § 5.2 (Top 10 manquants) sert de référence. Le critère retenu : **fréquence haute** (situation rencontrée plusieurs fois par mois dans un cabinet immigration belge).

Nota : « AES BE » désigne en fait le régime **9bis humanitaire** (art. 9bis Loi 15/12/1980), équivalent fonctionnel de l'AES FR — **déjà livré** par F-209. L'outil P2 à livrer est la voie naturalisation art. 12bis, distincte.

| # | tool_id proposé | Situation | Section audit | Flags IA existants | Priorité |
|---|-----------------|-----------|---------------|--------------------|---------|
| 1 | `F-IM-25-single-permit-be` | Single permit — checklist + délais renouvellement (loi 30/04/1999 + AR 02/09/2018) | § 3.1 / § 3.9 | `single_permit_envisage` (F-203 seedé) | P2 + P3 BE-only |
| 2 | `F-IM-26-regroupement-10ter-be` | Regroupement art. 10ter — ressortissant tiers séjour illimité (carte B/C) + seuil ressources 1 500 € | § 3.2 | `regroupement_10ter_detecte` (à créer) | P2 + P3 BE-only |
| 3 | `F-IM-27-regroupement-10bis-be` | Regroupement art. 10bis — ressortissant tiers séjour limité | § 3.2 | `regroupement_10bis_detecte` (à créer) | P2 + P3 BE-only |
| 4 | `F-IM-28-naturalisation-12bis-be` | Déclaration nationalité art. 12bis — 5 ans ou 10 ans, A2 langue | § 3.10 | `naturalisation_be_envisagee` (F-203 seedé) | P2 + P3 BE-only |
| 5 | `F-IM-29-naturalisation-conjoint-belge-be` | Acquisition par mariage avec Belge art. 16 CNB — 5 ans cohabitation + langue | § 3.10 | (à créer, lié à `naturalisation_be_envisagee`) | P2 + P3 BE-only |
| 6 | `F-IM-30-aesm-mena-be` | AESM + tutelle DGDE — mineur non accompagné, projet de vie | § 3.6 | `mineur_non_accompagne_be_detecte` (F-203 seedé) | P1 + P3 BE-only |
| 7 | `F-IM-31-cce-annulation-30j-be` | Recours CCE annulation — 30 j calendaires depuis notification | § 3.11 | `recours_cce_envisage` (F-203 seedé) | P1 + P3 BE-only |
| 8 | `F-IM-32-cce-extreme-urgence-5j-be` | Recours CCE extrême urgence — 5 j ouvrables (BelgianBusinessDays) | § 3.11 | `recours_cce_extreme_urgence` (F-203 seedé) | P1 + P3 BE-only |
| 9 | `F-IM-33-annexe13quinquies-ie-be` | Annexe 13quinquies — OQT + interdiction d'entrée (art. 74/11), durée 3/5/8 ans | § 3.5 | `interdiction_entree_be_detectee` (F-203 seedé) | P1 + P3 BE-only |
| 10 | `F-IM-34-protection-temporaire-ukraine-be` | Protection temporaire Ukraine — régime actif 2022-2026+, droit travail immédiat | § 3.4 | `protection_temporaire_ukraine_detectee` (F-203 seedé) | P2 + P3 BE-only |

---

## Justification des exclusions

| Outil audit F-191 | Exclusion F-215 | Motif |
|-------------------|-----------------|-------|
| `9bis-humanitaire-be` | ✅ LIVRÉ | F-209 SF-IM-14-01 |
| `9ter-medical-be` | ✅ LIVRÉ | F-209 SF-IM-14-02 |
| `40bis-cohabitant-ue-be` | ✅ LIVRÉ | F-203 SF-IM-14-03 |
| `40ter-familial-belge-be` | ✅ LIVRÉ | F-209 SF-IM-14-04 |
| `annexe-13-oqt-be` | ✅ LIVRÉ | F-209 SF-IM-08-05 |
| `carte-a-prorogation-be` | Reporté F-221 (P3) | Spécificité BE longue traîne — procédure commune différée |
| `carte-b-illimite-conditions` | Reporté F-221 (P3) | Spécificité P3 BE-only |
| `carte-c-installation-conditions` | Reporté F-221 (P3) | Spécificité P3, volume modéré |
| `carte-k-l-ue-residence-longue-duree` | Reporté F-221 (P3) | P3 BE-only — directive 2003/109 |
| `carte-h-brexit-conditions` | Reporté F-221 (P3) | Cas résiduel — inscriptions clôturées 31/12/2021 |
| `protection-subsidiaire-be` | Reporté F-221 (P3) | Asile BE complet = F-221, dépasserait périmètre P2 |
| `dublin-iii-be-determination` | Reporté F-221 (P3) | Asile BE = F-221 |
| `asile-procedure-acceleree-be` | Reporté F-221 (P3) | Asile BE = F-221 |
| `detention-centre-ferme-be` | Reporté F-221 (P3) | P1 BE-only mais très spécialisé, hors fréquence haute généraliste |
| `annexe-14-be` | Reporté F-221 (P3) | Procédure amont OE — P3 |
| `expulsion-art-20-22-be` | Reporté F-221 (P3) | Cas rares, procédure très spécifique |

---

## Tableau récapitulatif — outils F-215

| # | tool_id | SF backend | SF frontend | Flag F-203 | Source juridique BE | Découpage |
|---|---------|-----------|------------|------------|---------------------|-----------|
| 1 | `F-IM-25-single-permit-be` | SF-215-01 | SF-215-02 | `single_permit_envisage` (existant F-203) | Loi 30/04/1999 ; AR 02/09/2018 | 2 SF (champ IA `dateDepotSinglePermit` + renouvellement) |
| 2 | `F-IM-26-regroupement-10ter-be` | SF-215-03 | SF-215-04 | `regroupement_10ter_detecte` (nouveau flag) | Loi 15/12/1980 art. 10 + 10ter ; AR 17/05/2007 | 2 SF |
| 3 | `F-IM-27-regroupement-10bis-be` | SF-215-05 | SF-215-06 | `regroupement_10bis_detecte` (nouveau flag) | Loi 15/12/1980 art. 10bis ; AR 17/05/2007 | 2 SF |
| 4 | `F-IM-28-naturalisation-12bis-be` | SF-215-07 | SF-215-08 | `naturalisation_be_envisagee` (existant F-203) | Code nationalité belge (loi 28/06/1984) art. 12bis ; AR 14/01/2013 | 2 SF |
| 5 | `F-IM-29-naturalisation-conjoint-belge-be` | SF-215-09 | SF-215-10 | `naturalisation_be_envisagee` (partagé) | Code nationalité belge art. 16 ; AR 14/01/2013 | 2 SF |
| 6 | `F-IM-30-aesm-mena-be` | SF-215-11 | SF-215-12 | `mineur_non_accompagne_be_detecte` (existant F-203) | Loi 04/05/2007 (tutelle MENA) ; loi 15/12/1980 art. 9bis ; circulaire 15/09/2005 | 2 SF |
| 7 | `F-IM-31-cce-annulation-30j-be` | SF-215-13 | SF-215-14 | `recours_cce_envisage` (existant F-203) | Loi 15/12/1980 art. 39/82 §4 al. 1 ; loi 15/09/2006 | 2 SF |
| 8 | `F-IM-32-cce-extreme-urgence-5j-be` | SF-215-15 | SF-215-16 | `recours_cce_extreme_urgence` (existant F-203) | Loi 15/12/1980 art. 39/82 §4 al. 2-3 ; loi 15/09/2006 | 2 SF |
| 9 | `F-IM-33-annexe13quinquies-ie-be` | SF-215-17 | SF-215-18 | `interdiction_entree_be_detectee` (existant F-203) | Loi 15/12/1980 art. 74/11 + 74/12 ; AR 08/10/1981 | 2 SF |
| 10 | `F-IM-34-protection-temporaire-ukraine-be` | SF-215-19 | SF-215-20 | `protection_temporaire_ukraine_detectee` (existant F-203) | Décision UE 2022/382 ; loi 15/12/1980 art. 57/29+ | 2 SF |

**Total : 10 outils, 20 SF** (1 SF backend + 1 SF frontend par outil).

---

## Ajustements à porter par les mini-specs

1. **Mode de visibilité** : tous les outils F-215 sont `CONTEXTUAL` (flag IA comme trigger) — conforme au modèle F-203. Aucun outil ne sera ALWAYS_ON car les situations sont spécifiques.
2. **Flags IA nouveaux** : SF-215-03/05 créent 2 nouveaux flags (`regroupement_10ter_detecte`, `regroupement_10bis_detecte`) dans `ImmigrationExtractedData` + prompt. Les 8 autres flags sont déjà seedés par F-203.
3. **BelgianBusinessDaysCalculator** : réutilisé tel quel pour SF-215-13 (CCE annulation) et SF-215-15 (extrême urgence). Pas de duplication.
4. **AESM** : outil composite — tutelle DGDE (volet protection mineur, loi 04/05/2007) + AESM (volet séjour, art. 9bis adapté MENA). Un outil = une situation conformément à l'invariant. La distinction avec 9bis adulte (F-IM-14-9bis-humanitaire-be) est documentée dans la mini-spec SF-215-11.
5. **Sources juridiques à vérifier** : toutes les sources art. citées sont issues des connaissances générales du modèle — un avocat belge doit confirmer avant mise en prod (annotation dans chaque Calculator).
6. **Pré-remplissage IA** : les champs date/valeur de chaque outil sont listés dans les mini-specs par outil. Les 8 flags F-203 déjà seedés activent le CONTEXTUAL sans nouveau flag prompt.

---

## Verdict : **GO**

F-215 est cohérent : il comble les trous prioritaires du workflow de l'avocat belge en immigration, tous les pré-requis techniques existent (patttern éprouvé sur 9 outils BE, BelgianBusinessDaysCalculator, flags IA F-203), et la sortie est exploitable via le chaînage dashboard/synthèse. Seuls 2 nouveaux flags nécessitent une extension du prompt (SF-215-03/05).
