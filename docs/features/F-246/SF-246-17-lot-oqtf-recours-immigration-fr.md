# Mini-spec — [F-246 / SF-246-17] Pré-remplissage IA — Lot OQTF/recours Immigration FR : dublin-recours, crrv-refus-visa, jld-retention

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Pattern de référence : `docs/features/F-246/SF-246-16-immigration-recours-identites.md` (SF-246-16, commit `1d9f093c`).

---

## Identifiant

`F-246 / SF-246-17`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-17-lot-oqtf-recours-immigration-fr`

---

## Objectif

Compléter le pré-remplissage IA des 3 outils décisionnels Immigration FR
`dublin-recours` (F-IM-22), `crrv-refus-visa` (F-IM-23) et `jld-retention`
(F-IM-21) en branchant les champs saisissables extractibles des pièces qui
n'avaient pas encore de signal IA :

- `dublin-recours` : `etatMembreResponsable` (texte libre) + `motifTransfert` (enum `MotifTransfertDublin`)
- `crrv-refus-visa` : `typeVisa` (enum `TypeVisaCrrv`) + `motifRefus` (texte libre)
- `jld-retention` : `recoursForme` (booléen, depuis `recoursFormeDetected` existant)

---

## Comportement attendu

### Cas nominal

1. Le pipeline IA (prompt `IMMIGRATION_INSTRUCTION`) est enrichi de 4 nouvelles clés
   extraites des pièces Dublin / refus de visa.
2. L'extracteur `extractImmigrationData()` parse ces 4 clés en champs typés du record
   `ImmigrationExtractedData` : `dublinEtatMembreResponsable` (texte libre),
   `dublinMotifTransfert` (code enum normalisé), `crrvTypeVisa` (code enum normalisé),
   `crrvMotifRefus` (texte libre, tronqué à 500 car.).
3. Le champ `recoursFormeDetected` existant dans le record et déjà extrait est désormais
   consommé par le helper `JldRetentionPrefillRules` pour pré-remplir `recoursForme`.
4. Le DTO frontend `ImmigrationExtractedData` expose les 4 nouveaux champs backend.
5. Les helpers prefill-rules des 3 outils sont mis à jour :
   - `DublinRecoursPrefillRules.computeEtatMembreResponsable()` + `computeMotifTransfert()` → `computePrefillCount()` passe de 1 à 3
   - `CrrvRefusVisaPrefillRules.computeTypeVisa()` + `computeMotifRefus()` → `computePrefillCount()` passe de 1 à 3
   - `JldRetentionPrefillRules.computeRecoursForme()` → `computePrefillCount()` passe de 2 à 3
6. Les composants `prefillFromAi()` appellent les nouvelles fonctions et appliquent les
   signaux de provenance + badges `auto_awesome` pour chaque champ pré-rempli.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Dossier non-Dublin (ex. OQTF classique) | `dublinEtatMembreResponsable` + `dublinMotifTransfert` = `null` ; helper no-op |
| Décision Dublin sans état membre lisible | `dublinEtatMembreResponsable` = `null` |
| Motif transfert hors enum (whitelist 5 codes) | `dublinMotifTransfert` = `null` |
| Dossier sans refus de visa CRRV | `crrvTypeVisa` + `crrvMotifRefus` = `null` |
| Type visa hors whitelist (5 codes) | `crrvTypeVisa` = `null` |
| `recoursFormeDetected` = `INCONNU` | `recoursForme` = `null` (no-op gracieux — ne jamais deviner) |
| `recoursFormeDetected` = `NON` | `recoursForme` = `false` (pré-remplit avec `false`) |
| Dossier BE | Tous les 4 nouveaux champs backend = `null` ; prompt impose null hors FR |
| `aiData` arrive après le premier rendu | `prefillFromAi()` ré-invoqué dans `ngOnChanges()` |

---

## Analyse de cohérence transversale

- [x] **Autres outils Immigration FR** : les 3 outils concernés sont distincts et non recoupés.
- [x] **Autres pays** : France uniquement. Prompt impose `null` hors FR pour les 4 nouveaux champs.
- [x] **Autres domaines** : non applicable.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique SF-246-02/16), badges `auto_awesome`. Pas d'alerte F-IA-03 ajoutée (champs sans notion d'écart relatif).
- [x] **Outil décisionnel** : scan réalisé — aucun doublon de concept entre `dublin-recours`, `crrv-refus-visa`, `jld-retention` et les autres outils immigration.
- [x] **Composants potentiellement impactés** : les 3 helpers + composants, le record backend, le prompt, l'extracteur, le DTO frontend.

**Décision** : étendu à toutes les cibles applicables dans cette SF.

---

## Champs IA à extraire (pré-remplissage)

| Champ formulaire | Outil | Type | Champ record `ImmigrationExtractedData` | Extension requise |
|---|---|---|---|---|
| `etatMembreResponsable` | `dublin-recours` | texte libre | `dublinEtatMembreResponsable` (String, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `motifTransfert` | `dublin-recours` | enum `MotifTransfertDublin` | `dublinMotifTransfert` (String, nullable, code enum normalisé) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `typeVisa` | `crrv-refus-visa` | enum `TypeVisaCrrv` | `crrvTypeVisa` (String, nullable, code enum normalisé) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `motifRefus` | `crrv-refus-visa` | texte libre | `crrvMotifRefus` (String, nullable, ≤ 500 car.) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `recoursForme` | `jld-retention` | booléen | `recoursFormeDetected` (existant, type `DetectedAnswer`) | [ ] record (existant) ; [x] helper frontend |

---

## Critères d'acceptation

- [ ] `ImmigrationExtractedData` contient les 4 nouveaux champs (`dublinEtatMembreResponsable`, `dublinMotifTransfert`, `crrvTypeVisa`, `crrvMotifRefus`), tous `String` nullable, propagés par le builder F-234.
- [ ] Le prompt `IMMIGRATION_INSTRUCTION` décrit les 4 nouvelles clés avec définitions juridiques sans ambiguïté + règle `null` hors FR.
- [ ] `extractImmigrationData()` parse les 4 clés : textes via `truncatedTextOrNull()`, enum via `normalizeEnumCode()` whitelist respectifs.
- [ ] Le DTO frontend expose les 4 nouveaux champs.
- [ ] `DublinRecoursPrefillRules` : `computeEtatMembreResponsable()` + `computeMotifTransfert()` + `computePrefillCount()` = 3.
- [ ] `CrrvRefusVisaPrefillRules` : `computeTypeVisa()` + `computeMotifRefus()` + `computePrefillCount()` = 3.
- [ ] `JldRetentionPrefillRules` : `computeRecoursForme()` depuis `recoursFormeDetected` (OUI → `true`, NON → `false`, INCONNU/null → `null`) + `computePrefillCount()` = 3.
- [ ] Chaque composant `prefillFromAi()` applique les nouvelles valeurs avec signaux provenance + badges `auto_awesome`, remise à `null` au changement manuel.
- [ ] Tests Jest : cas 0 / partiel / nominal pour chaque outil.
- [ ] Tests backend : extracteur + parsing des 4 nouveaux champs (nominal + hors enum + texte vide + dossier BE → null).
- [ ] Isolation workspace : non applicable côté pré-fill (données portées par la synthèse du dossier, déjà isolée).

---

## Périmètre

### Hors scope (explicite)

- `recoursForme` de `dublin-recours` et `crrv-refus-visa` — le recours n'est pas encore formé au moment de l'analyse ; champ saisi par l'avocat.
- `dateRecours` des 3 outils — acte à venir, non extractible.
- `etatMembreResponsable` de `dublin-recours` : texte libre seulement, pas de whitelist d'États membres (trop variable).
- Alertes F-IA-03 sur ces champs — pas de notion d'écart relatif (texte libre ou enum exact).
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format/Valeurs | Normalisation |
|---|---|---|---|---|
| `dublinEtatMembreResponsable` | Non | 200 car. | texte libre | `truncatedTextOrNull()` |
| `dublinMotifTransfert` | Non | — | 5 codes : `DEMANDE_ASILE_AUTRE_ETAT`, `VISA_DELIVRE_AUTRE_ETAT`, `ENTREE_IRREGULIERE_AUTRE_ETAT`, `MEMBRE_FAMILLE_AUTRE_ETAT`, `AUTRE` | `normalizeEnumCode()` upper-case + whitelist |
| `crrvTypeVisa` | Non | — | 5 codes : `COURT_SEJOUR`, `LONG_SEJOUR`, `REGROUPEMENT_FAMILIAL`, `ETUDIANT`, `AUTRE` | `normalizeEnumCode()` |
| `crrvMotifRefus` | Non | 500 car. | texte libre | `truncatedTextOrNull()` |

---

## Technique

### Endpoint(s)

Inchangés — les champs IA transitent par la synthèse d'analyse (`immigrationExtractedData`).

### Contrat API figé

**Clés JSON nouvelles dans `analysis_result` (racine) :**

```json
"dublin_etat_membre_responsable": "Allemagne",
"dublin_motif_transfert": "DEMANDE_ASILE_AUTRE_ETAT",
"crrv_type_visa": "LONG_SEJOUR",
"crrv_motif_refus": "Ressources insuffisantes"
```

**Record backend `ImmigrationExtractedData`** — 4 champs ajoutés :

```java
// SF-246-17 : pré-fill dublin-recours + crrv-refus-visa (FR uniquement, nullables)
String dublinEtatMembreResponsable,
String dublinMotifTransfert,
String crrvTypeVisa,
String crrvMotifRefus
```

**DTO frontend `ImmigrationExtractedData`** — 4 champs ajoutés :

```ts
dublinEtatMembreResponsable?: string | null;
dublinMotifTransfert?: string | null;
crrvTypeVisa?: string | null;
crrvMotifRefus?: string | null;
```

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- `dublin-recours-section-prefill-rules.ts` : `computeEtatMembreResponsable()` + `computeMotifTransfert()` + `computePrefillCount()` 1→3
- `crrv-refus-visa-section-prefill-rules.ts` : `computeTypeVisa()` + `computeMotifRefus()` + `computePrefillCount()` 1→3
- `jld-retention-section-prefill-rules.ts` : `computeRecoursForme()` + `computePrefillCount()` 2→3
- Les 3 composants section : `prefillFromAi()` étendu, signaux provenance, badges `auto_awesome`

---

## Plan de test

### Tests unitaires backend

- [ ] `extractImmigrationData()` — `dublin_etat_membre_responsable` présent → `dublinEtatMembreResponsable` renseigné
- [ ] `extractImmigrationData()` — `dublin_motif_transfert` valide → code normalisé ; hors whitelist → `null`
- [ ] `extractImmigrationData()` — `crrv_type_visa` valide → code normalisé ; hors whitelist → `null`
- [ ] `extractImmigrationData()` — `crrv_motif_refus` texte → tronqué à 500 ; absent → `null`
- [ ] `extractImmigrationData()` — dossier sans ces clés → 4 champs `null`

### Tests unitaires frontend (Jest)

- [ ] `DublinRecoursPrefillRules.computePrefillCount()` : 0 (vide/BE) / partiel / 3 (nominal)
- [ ] `computeEtatMembreResponsable()` : texte non vide OK, null/vide → null, hors FR → null
- [ ] `computeMotifTransfert()` : codes valides, insensible casse, hors whitelist → null
- [ ] `CrrvRefusVisaPrefillRules.computePrefillCount()` : 0 / partiel / 3
- [ ] `computeTypeVisa()` : codes valides, hors whitelist → null
- [ ] `computeMotifRefus()` : texte non vide OK, null/vide → null
- [ ] `JldRetentionPrefillRules.computePrefillCount()` : 0 / partiel / 3
- [ ] `computeRecoursForme()` : OUI → `true`, NON → `false`, INCONNU → `null`, null → `null`

### Tests d'intégration

- [ ] Non-régression endpoints `dublin-recours`, `crrv-refus-visa`, `jld-retention` (403 workspace différent — tests existants conservés).

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Outil décisionnel métier** — 3 outils impactés, composants listés ci-dessus.
- [ ] Auth / Principal — non applicable
- [ ] Workspace context — non applicable
- [ ] Plans / limites — non applicable
- [ ] Navigation / routing — non applicable

### Smoke tests E2E

La SF étend uniquement un record IA, un prompt, un extracteur et 3 helpers de pré-fill. Aucune route, aucun guard, aucun endpoint modifié. Les ~27 échecs E2E préexistants sont tolérés.

---

## Dépendances

- **SF-246-16** — `done`, mergée sur master. Record `ImmigrationExtractedData` à jour via builder F-234.
- **SF-246-13** — `done`, mergée sur master (début de cette session).
- Aucune autre SF F-246 ne modifie `ImmigrationExtractedData` en parallèle.

---

## Notes

### `recoursFormeDetected` — réutilisation pour JLD

Le champ `recoursFormeDetected` (type `DetectedAnswer` = `{reponse: "OUI"|"NON"|"INCONNU", justification: ...}`) existe déjà dans `ImmigrationExtractedData` et est extrait par l'extracteur (clé `recours_forme_detected`). Il sert déjà pour F-IM-08. Le helper `JldRetentionPrefillRules` ne l'utilisait pas encore — cette SF le branche : `OUI` → `true`, `NON` → `false`, `INCONNU` ou `null` → `null` (no-op gracieux).

### Pas de `recoursFormeDetected` spécifique Dublin/CRRV

La clé `recours_forme_detected` est générique OQTF. Pour Dublin et CRRV, `recoursForme` n'est pas pré-rempli (décision de l'audit : « acte à venir »). La valeur JLD est un signal indirect acceptable car le placement en CRA suit toujours une OQTF.

### `dublinEtatMembreResponsable` — texte libre, pas de whitelist

Les États membres UE sont trop nombreux (27) et nommés de façon variable dans les décisions de transfert. Le champ est transmis tel quel en texte libre (tronqué à 200 car.) sans normalisation. L'avocat peut corriger.
