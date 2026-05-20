# SF-246-20 — Lot Immigration BE

**Feature parente :** F-246 — Complétion du pré-remplissage IA des outils décisionnels  
**Branche :** `feat/SF-246-20-lot-immigration-be`  
**Date :** 2026-05-20

---

## Objectif

Brancher le pré-remplissage IA sur 4 outils Immigration BE dont les champs date/valeur
extractibles n'avaient pas de source backend réelle : `belgian-9bis`, `belgian-9ter`,
`belgian-40bis`, `belgian-40ter`. BELGIQUE UNIQUEMENT — gating `workspaceCountry === 'BELGIQUE'`
conservé dans tous les helpers.

---

## Périmètre — champs par outil (audit SF-246-14 §6)

### 1. Belgian-9bis (F-IM-14 art. 9bis Loi 15/12/1980) — 2 nouveaux champs

| Champ formulaire       | Clé JSON IA                       | Règle                                               |
|------------------------|-----------------------------------|-----------------------------------------------------|
| `dateEntreeBelgique`   | `be_9bis_date_entree_belgique`    | ISO YYYY-MM-DD, non-future, null sinon              |
| `dureePresenceMois`    | calculé backend depuis date entrée| ChronoUnit.MONTHS depuis `dateEntreeBelgique` jusqu'à aujourd'hui |

> `dateDepotDemande` ← `dateDepotProcedure` déjà branché — pas de changement.
> `dureePresenceMois` est calculé côté backend (comme `aesDureePresenceMois` SF-246-18),
> pas demandé directement au LLM.
> Champs aspirationnels supprimés : `(aiData as any).dateEntreeBelgique` dans `prefillFromAi()`.

### 2. Belgian-9ter (F-IM-14 art. 9ter Loi 15/12/1980) — 1 nouveau champ réel + suppression casts

| Champ formulaire       | Clé JSON IA                       | Règle                                               |
|------------------------|-----------------------------------|-----------------------------------------------------|
| `dateDebutSymptomes`   | `be_9ter_date_debut_symptomes`    | ISO YYYY-MM-DD, non-future, null sinon              |
| `dateDepotDemande`     | `dateDepotProcedure` (existant)   | Alias déjà typé — suppression du cast `as any`      |

> Les champs booléens (`maladieGraveCertifiee`, `soinsNecessairesDisponiblesBe`,
> `soinsInaccessiblesPaysOrigine`, `menaceOrdrePublic`) sont déjà aspirationnels
> et restent aspirationnels (catégorie "appréciation médicale/juridique" — audit §6).
> Le cast `as any` pour `dateDebutSymptomes` dans le helper est supprimé ; les casts
> sur les booléens aspirationnels restent en place car hors périmètre SF-246-20.
> Les alertes F-IA-03 `buildDateDebutSymptomesAlert()` utilisent `(aiData as any)` —
> mises à jour vers le champ typé.

### 3. Belgian-40bis (F-IM-14 art. 40bis Loi 15/12/1980) — 1 nouveau champ

| Champ formulaire       | Clé JSON IA                       | Règle                                               |
|------------------------|-----------------------------------|-----------------------------------------------------|
| `lienFamilial`         | `be_40bis_lien_familial`          | Whitelist 4 valeurs (CONJOINT / ENFANT / ASCENDANT / PARTENAIRE_ENREGISTRE), insensible casse |

> `dateDepotDemande` ← `dateDepotProcedure` et `regroupantCitoyenUe` ← `nationaliteUe`
> déjà branchés — pas de changement.
> La whitelist `lienFamilial` pour 40bis est distincte de celle du 40ter (qui a le
> même jeu de 4 valeurs mais via `belgian-40ter.model.ts` — réutiliser le même modèle).

### 4. Belgian-40ter (F-IM-14 art. 40ter Loi 15/12/1980) — 2 nouveaux champs réels + suppression casts

| Champ formulaire        | Clé JSON IA                       | Règle                                               |
|-------------------------|-----------------------------------|-----------------------------------------------------|
| `lienFamilial`          | `be_40ter_lien_familial`          | Whitelist LIENS_FAMILIAUX (CONJOINT / ENFANT / ASCENDANT / PARTENAIRE_ENREGISTRE) |
| `revenusMensuelsNets`   | `be_40ter_revenus_mensuels_nets`  | Entier > 0, ≤ 30 000, null sinon                    |
| `dateDepotDemande`      | `dateDepotProcedure` (existant)   | Alias déjà typé — suppression du cast `as any`      |

> Le cast `as any` pour `lienFamilialBe`/`lienFamilial` et `revenusMensuelsNets` est
> supprimé dans le helper. `regroupantBelge` reste aspirationnel (booléen non extractible
> des pièces avec fiabilité — audit §6, catégorie "constat/paramètre").

---

## Nouveaux champs backend (ImmigrationExtractedData)

```
// SF-246-20 : pré-fill lot Immigration BE
// BELGIQUE UNIQUEMENT — dossiers FR : null sur tous ces champs.
String be9bisDateEntreeBelgique,      // ISO YYYY-MM-DD, non-future
Integer be9bisDureePresenceMois,      // ChronoUnit.MONTHS depuis be9bisDateEntreeBelgique
String be9terDateDebutSymptomes,      // ISO YYYY-MM-DD, non-future (art. 9ter)
String be40bisLienFamilial,           // whitelist LienFamilialBe 4 valeurs (40bis)
String be40terLienFamilial,           // whitelist LienFamilialBe 4 valeurs (40ter)
Integer be40terRevenusMensuelsNets,   // entier > 0, ≤ 30 000
```

---

## Comportement nominal

1. Backend extrait les 6 nouveaux champs depuis le nœud JSON de la réponse IA.
2. Prompt enrichi : sous-objet `immigration_be_detection_v2` avec les 6 clés BE.
3. Frontend DTO reçoit les 6 nouveaux champs dans `ImmigrationExtractedData`.
4. Helpers mis à jour — suppression des casts `as any` pour les champs maintenant typés.
5. `prefillFromAi()` des 4 composants branche les nouveaux champs.
6. `computePrefillCount()` reflète les nouveaux champs.

## Cas d'erreur

- Date future → `null` (fail-open).
- Format non ISO → `null`.
- Valeur hors-whitelist → `null` (normalizeEnumCode insensible casse).
- Montant ≤ 0 ou > 30 000 → `null`.
- Champ absent du JSON → `null`.
- Workspace FRANCE → tous les nouveaux champs null (BE-only).

---

## Critères d'acceptation

1. `computePrefillCount()` pour `belgian-9bis` retourne 2 max (dateEntreeBelgique + dateDepot
   déjà existant) quand les deux sont non-null.
2. `computePrefillCount()` pour `belgian-9ter` retourne 2 max (dateDebutSymptomes + dateDepot)
   quand les deux sont non-null (les booléens aspirationnels ne comptent pas).
3. `computePrefillCount()` pour `belgian-40bis` retourne 3 max (lienFamilial + dateDepot +
   regroupantCitoyenUe déjà existants).
4. `computePrefillCount()` pour `belgian-40ter` retourne 3 max (lienFamilial + revenusMensuel +
   dateDepot — `regroupantBelge` aspirationnel reste non compté).
5. Aucun cast `as any` pour `dateDebutSymptomes`, `dateEntreeBelgique`, `lienFamilialBe`,
   `lienFamilial`, `revenusMensuelsNets` dans les helpers (champs maintenant typés).
6. Tests Jest PASS pour tous les helpers.
7. Tests JUnit PASS pour tous les extracteurs backend.
8. Aucune régression sur les outils existants (smoke E2E).

---

## Plan de test

### Backend (JUnit — CaseAnalysisResponseTest)

- `extractImmigrationData_sf24620_9bis_dateEntree_nominal` — date ISO valide
- `extractImmigrationData_sf24620_9bis_dateEntree_future_rejected` — date future → null
- `extractImmigrationData_sf24620_9bis_dateEntree_nonIso_rejected` — format non-ISO → null
- `extractImmigrationData_sf24620_9bis_dureePresence_calcule_depuis_date` — duree calculée
- `extractImmigrationData_sf24620_9ter_dateDebutSymptomes_nominal` — date ISO valide
- `extractImmigrationData_sf24620_9ter_dateDebutSymptomes_future_rejected` — date future → null
- `extractImmigrationData_sf24620_40bis_lienFamilial_nominal` — CONJOINT mappé
- `extractImmigrationData_sf24620_40bis_lienFamilial_invalid_null` — valeur hors-whitelist
- `extractImmigrationData_sf24620_40ter_lienFamilial_nominal` — ENFANT mappé
- `extractImmigrationData_sf24620_40ter_revenus_nominal` — 2500 → ok
- `extractImmigrationData_sf24620_40ter_revenus_out_of_range_null` — 0 ou >30000 → null
- `extractImmigrationData_sf24620_all_new_be_fields_null_graceful` — tous null ok

### Frontend (Jest — helpers purs)

- `Belgian9bisPrefillRules` : computeDateEntreeBelgique / computeDureePresenceMois / computePrefillCount
- `Belgian9terPrefillRules` : computeDateDebutSymptomes (typed) / computePrefillCount (2 champs réels)
- `Belgian40bisPrefillRules` : computeLienFamilial / computePrefillCount (3 champs)
- `Belgian40terPrefillRules` : computeLienFamilial (typed) / computeRevenusMensuelsNets (typed) / computePrefillCount

---

## Fichiers impactés

### Backend
- `CaseAnalysisResponse.java` — 6 champs dans `ImmigrationExtractedData` + builder + setters + toBuilder + build + extracteur + null guard
- `LegalDomainPromptBuilder.java` — `IMMIGRATION_INSTRUCTION` + sous-objet `immigration_be_detection_v2`

### Frontend
- `case-analysis.model.ts` — 6 champs dans `ImmigrationExtractedData`
- `belgian-9bis-section-prefill-rules.ts` — `computeDateEntreeBelgique` + `computeDureePresenceMois` (nouveau) + `computePrefillCount`
- `belgian-9bis-section-prefill-rules.spec.ts` — nouveaux cas
- `belgian-9bis-section.component.ts` — suppression cast `as any`, provenance dateEntreeBelgique branchée
- `belgian-9ter-section-prefill-rules.ts` — cast `as any` pour `dateDebutSymptomes` supprimé → typé
- `belgian-9ter-section-prefill-rules.spec.ts` — mise à jour
- `belgian-9ter-section.component.ts` — alertes F-IA-03 `buildDateDebutSymptomesAlert` → typé
- `belgian-40bis-section-prefill-rules.ts` — `computeLienFamilial` + `computePrefillCount`
- `belgian-40bis-section-prefill-rules.spec.ts` — nouveaux cas
- `belgian-40bis-section.component.ts` — provenance lienFamilial branchée
- `belgian-40ter-section-prefill-rules.ts` — cast `as any` pour lienFamilial/revenus supprimé → typé
- `belgian-40ter-section-prefill-rules.spec.ts` — mise à jour
- `belgian-40ter-section.component.ts` — alertes F-IA-03 → typé

---

## Champs IA à extraire (pré-remplissage) — garde-fou F-246

| Outil | Champ formulaire | Clé JSON IA | Record `ImmigrationExtractedData` | Prompt |
|---|---|---|---|---|
| belgian-9bis | dateEntreeBelgique | `be_9bis_date_entree_belgique` | `be9bisDateEntreeBelgique` | ✅ ajouté |
| belgian-9bis | dureePresenceMois | calculé backend | `be9bisDureePresenceMois` | n/a |
| belgian-9ter | dateDebutSymptomes | `be_9ter_date_debut_symptomes` | `be9terDateDebutSymptomes` | ✅ ajouté |
| belgian-40bis | lienFamilial | `be_40bis_lien_familial` | `be40bisLienFamilial` | ✅ ajouté |
| belgian-40ter | lienFamilial | `be_40ter_lien_familial` | `be40terLienFamilial` | ✅ ajouté |
| belgian-40ter | revenusMensuelsNets | `be_40ter_revenus_mensuels_nets` | `be40terRevenusMensuelsNets` | ✅ ajouté |

---

## Hors périmètre

- Champs booléens aspirationnels de `belgian-9ter` : `maladieGraveCertifiee`,
  `soinsNecessairesDisponiblesBe`, `soinsInaccessiblesPaysOrigine`, `menaceOrdrePublic`
  → catégorie "appréciation médicale/juridique" (audit §6), pas extractibles des pièces
  avec fiabilité — maintenus aspirationnels avec casts `as any`.
- `regroupantBelge` de `belgian-40ter` : booléen non extractible — maintenu aspirationnel.
- `regroupantActiviteCategorie`, `ressourcesSuffisantes`, `assuranceMaladieUe`,
  `logementSuffisant`, `menaceOrdrePublic` de `belgian-40bis` : appréciations/constats
  non extractibles des pièces — hors périmètre.
