# Mini-spec — F-IM-05 / SF-IM-05-04 Normalisation IA du type de titre + extraction nationalité UE

## Identifiant

`F-IM-05 / SF-IM-05-04`

## Feature parente

`F-IM-05` — Arbre décisionnel type de titre

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IM-05-04-normalisation-ia-titre`

---

## Objectif

Enrichir l'extraction IA pour l'immigration : normaliser `type_titre_sejour` aux 16 codes enum exacts de F-IM-05 (`VLS_TS_ETUDIANT`, `CARTE_A_TRAVAIL`, etc.) et ajouter un champ `nationalite_ue` (boolean). Ces données alimentent le pré-remplissage du questionnaire F-IM-05 et débloquent SF-IA-03-09.

---

## Comportement attendu

### Cas nominal

1. Le prompt IMMIGRATION demande à Claude deux nouveaux champs :
   - `type_titre_sejour_code` : l'un des 16 codes exacts (8 FR + 8 BE) ou `null` si non déterminable avec certitude
   - `nationalite_ue` : `true` / `false` / `null` selon que le ressortissant est de l'UE/EEE/Suisse
2. Le champ `type_titre_sejour` en texte libre existant est **conservé** (rétrocompat pour l'affichage et les usages existants).
3. `ImmigrationExtractedData` expose `typeTitreSejourCode` et `nationaliteUe`.
4. `ImmigrationTitleDecisionSectionComponent` utilise ces valeurs pour le pré-remplissage :
   - `nationaliteUe` IA → pré-sélectionné
   - `typeTitreSejourCode` IA → pré-remplit `motif` via une table de correspondance (voir ci-dessous) lorsque le code est exploitable

### Table de correspondance code → motif

| Codes titre | Motif pré-rempli |
|---|---|
| `VLS_TS_ETUDIANT`, `CARTE_A_ETUDES` | `ETUDES` |
| `VLS_TS_SALARIE`, `CST_SALARIE`, `CARTE_PLURIANNUELLE`, `APS`, `CARTE_A_TRAVAIL`, `PERMIS_UNIQUE` | `TRAVAIL` |
| `CST_VPF`, `CARTE_A_FAMILLE` | `FAMILLE` |
| `RECEPISSE_ASILE`, `ATTESTATION_IMMATRICULATION`, `ANNEXE_15` | `ASILE` |
| `CARTE_RESIDENT`, `CARTE_B`, `CARTE_C` | *pas de pré-remplissage* (titres génériques stables, motif d'origine non déductible) |

Le mapping heuristique textuel déjà présent dans le composant (SF-IM-05-03, lignes 110-117) est **remplacé** par cette table basée sur le code normalisé. Plus robuste.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| IA renvoie un code hors enum | `typeTitreSejourCode` = null (fail-open) |
| IA renvoie une casse variable (`vls_ts_etudiant`) | normalisé upper-case |
| IA renvoie `nationalite_ue` non booléen | normalisé (string `"true"`/`"false"`) ou null |
| Code IA d'un pays différent du workspace | exposé tel quel, le composant filtre lors du pré-remplissage |
| Ni code ni boolean présents | pré-remplissage actuel préservé (fallback heuristique sur le texte libre) |

---

## Critères d'acceptation

- [ ] Prompt `IMMIGRATION_INSTRUCTION` dans `LegalDomainPromptBuilder` ajoute `type_titre_sejour_code` (16 valeurs ou null) et `nationalite_ue` (bool ou null).
- [ ] `ImmigrationExtractedData` record : deux nouveaux champs `typeTitreSejourCode`, `nationaliteUe`.
- [ ] Set `IMMIGRATION_TITLE_CODES` dans `CaseAnalysisResponse` pour validation fail-open.
- [ ] `extractImmigrationData` parse upper-case + filtre enum + gère booléen string/bool.
- [ ] Rétrocompat : `typeTitreSejour` libre inchangé, les composants existants qui le lisent continuent de fonctionner.
- [ ] Frontend : `ImmigrationExtractedData` model ajoute les deux champs optionnels.
- [ ] `ImmigrationTitleDecisionSectionComponent` applique le pré-remplissage via la table de correspondance quand `typeTitreSejourCode` présent, sinon tombe sur l'heuristique existante.
- [ ] `nationaliteUe` IA → `nationaliteUe` form pré-sélectionné.
- [ ] Tests backend (parsing code upper-case, code hors enum, nationalite bool/string/null).
- [ ] Tests frontend (mapping code → motif, pré-remplissage nationaliteUe, fallback heuristique).

---

## Périmètre

### Hors scope (explicite)

- Extraction IA de `duree` (court/long séjour) et `situation_familiale` — signaux trop incertains dans les documents types.
- Cohérence IA (alerte sur divergence) → SF-IA-03-09 suivante.
- Modification du moteur de décision (`ImmigrationTitleDecisionEngine`) : inchangé.
- Backfill des anciennes analyses : pas de migration nécessaire (donnée dans JSON).

---

## Valeurs initiales

Aucune entité créée. Les nouveaux champs sont extraits à la volée du raw JSON.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Normalisation |
|-------|-------------|------------------|---------------|
| `type_titre_sejour_code` | Non | un parmi 16 (8 FR + 8 BE) | upper-case, filtré contre l'enum |
| `nationalite_ue` | Non | true / false / "true" / "false" / null | converti en Boolean |

---

## Technique

### Endpoint(s)

| Méthode | URL | Changement |
|---|---|---|
| GET | `/api/v1/case-files/{id}/case-analysis` | `immigrationExtractedData.typeTitreSejourCode` et `.nationaliteUe` ajoutés |

### Tables impactées

Aucune — tout dans le JSON `analysis_result`.

### Migration Liquibase

- [x] **Non applicable**.

### Composants Angular

- `ImmigrationExtractedData` model (frontend) : 2 champs optionnels
- `ImmigrationTitleDecisionSectionComponent` :
  - Mapping `CODE_TO_MOTIF` (Record)
  - Fonction `prefillFromAi()` refactor pour utiliser le code + fallback heuristique
  - Pré-remplissage de `nationaliteUe` si IA fournit

---

## Plan de test

### Tests unitaires backend

- [ ] Code IA `vls_ts_etudiant` → upper-case `VLS_TS_ETUDIANT` persisté.
- [ ] Code IA `UNKNOWN_CODE` → null.
- [ ] Code absent → null.
- [ ] `nationalite_ue: true` (bool) → true.
- [ ] `nationalite_ue: "true"` (string) → true.
- [ ] `nationalite_ue: null` → null.
- [ ] Prompt IMMIGRATION_INSTRUCTION contient les 16 codes et les champs.

### Tests unitaires frontend

- [ ] `typeTitreSejourCode = VLS_TS_ETUDIANT` → motif `ETUDES` pré-rempli.
- [ ] `typeTitreSejourCode = PERMIS_UNIQUE` → motif `TRAVAIL`.
- [ ] `typeTitreSejourCode = CARTE_RESIDENT` → pas de pré-remplissage motif (générique).
- [ ] `nationaliteUe = true` → toggle pré-sélectionné.
- [ ] `typeTitreSejourCode` null + `typeTitreSejour` = "étudiant" → fallback heuristique → `ETUDES`.
- [ ] `typeTitreSejourCode` et `typeTitreSejour` null → aucun pré-remplissage.

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants impactés

| Composant | Impact |
|---|---|
| `LegalDomainPromptBuilder` | prompt IMMIGRATION rallongé |
| `CaseAnalysisResponse.ImmigrationExtractedData` | 2 champs, record constructeur rallongé |
| `ImmigrationTitleDecisionSectionComponent` | refactor prefill, fallback heuristique préservé |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `F-IA-01` (Done) — pipeline d'extraction.
- `F-IM-05` SF-01/02/03 (Done) — composant existant avec prefill heuristique.

### Questions ouvertes

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi 2 champs en parallèle du texte libre** : la rétrocompat est primordiale. Plusieurs parties de l'app lisent `typeTitreSejour` en texte libre (affichage synthèse, etc.). On ajoute le code normalisé à côté sans remplacer.
- **Pourquoi pas d'extraction `duree` et `situation_familiale`** : ces infos sont rarement explicites dans les documents. Les demander à l'IA produirait surtout des `null`. Signal faible pour un coût de prompt.
- **Pourquoi remplacer l'heuristique textuelle par le mapping code** : le code normalisé est déterministe ; la heuristique textuelle de SF-IM-05-03 est approximative. On garde l'heuristique en fallback quand le code est absent.
- **Pourquoi ne pas mapper CARTE_RESIDENT vers un motif** : c'est un titre d'établissement stable, il couvre plusieurs motifs d'origine (travail devenu long séjour, famille, etc.). Pré-remplir arbitrairement biaiserait le questionnaire.
