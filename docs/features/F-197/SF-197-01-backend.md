# Mini-spec — F-197 / SF-197-01 Backend — Override `type_litige_detecte` (Travail) + `type_procedure_detectee` (Immigration)

## Identifiant

`F-197 / SF-197-01`

## Statut

`draft` — 2026-05-06

## Branche Git

`feat/SF-197-01-backend-type-override`

## Pattern de référence

**SF-192-01-backend.md** mergée. F-197 est différent : **single value override** plutôt que trichotomie. Pattern adapté.

---

## Objectif

Permettre à l'avocat de **surcharger** la valeur détectée par l'IA pour `type_litige_detecte` (Travail FR : 7 enums LICENCIEMENT_SANS_CAUSE_REELLE / LICENCIEMENT_ECONOMIQUE / PRISE_ACTE_RUPTURE / HARCELEMENT_MORAL / DISCRIMINATION / HEURES_SUPPLEMENTAIRES / RAPPEL_SALAIRE) et `type_procedure_detectee` (Immigration : OQTF_AVEC_DELAI / OQTF_SANS_DELAI / etc.). L'override drive F-IA-04 visibility et le pre-fill F-DT-08/09/10 / F-IM-08/20.

Cohérence F-176 stricte : PUT override = persistance pure. Effets matérialisés au run de Synthèse enrichie.

---

## Modèle d'activation

PUT override pur. Au run synthèse enrichie : (a) F-IA-04 visibility re-eval avec override ; (b) prompt enrichi instruit l'IA `[Type litige overrider par l'avocat — re-cadrer l'analyse]` ; (c) pre-fill outils décisionnels avec le bon type ; (d) tile dashboard mise à jour.

---

## Architecture

Pas de table dédiée — colonnes nullable directement dans `case_analyses` :
- `type_litige_avocat_override VARCHAR(50) NULL` (Travail FR)
- `type_procedure_avocat_override VARCHAR(50) NULL` (Immigration)
- `type_override_raison TEXT NULL` (raison libre du override, optionnel)

Note : single value, pas trichotomie. Donc 1 colonne par dimension, pas de table.

---

## Comportement attendu

### Cas nominal

1. **PUT override pur** : `PUT /api/v1/case-files/{id}/type-litige-override` body `{ type: 'LICENCIEMENT_ECONOMIQUE', raison?: 'Analyse documents : ...' }` → upsert sur la dernière `CaseAnalysis` DONE (overwrite si existant). **Aucun side-effect**.
2. **Endpoint GET** : `GET /api/v1/case-files/{id}/type-litige-override` → retourne `{ typeLitigeAvocat?, typeProcedureAvocat?, raison? }` lus sur la dernière analyse DONE.
3. **Matérialisation au run synthèse enrichie** :
   - `EnrichedAnalysisService.run` lit l'override sur l'analyse précédente
   - **Avant l'appel IA** : si override présent, injecter dans prompt enrichi section `[Type litige fixé par l'avocat]` qui instruit l'IA de cadrer son analyse sur ce type (pas de tentative de re-détection)
   - **Après l'appel IA** : nouvelle analyse créée. Si override existait sur la précédente, le **propager automatiquement** sur la nouvelle (cloner les 3 colonnes vers la nouvelle `CaseAnalysis`).
   - **F-IA-04 visibility** : `DecisionToolVisibilityService` (existant F-IA-04) lit le `type_litige_detecte` ou `type_litige_avocat_override` (priorité avocat) lors de l'évaluation des règles de visibilité.
   - **F-DT-08/09/10 + F-IM-08/20 pre-fill** : l'override est passé via `aiData` côté frontend (cf. SF-197-02). Backend n'a rien à modifier.
4. **Tile dashboard** : extension de la tile existante `riskScore` (F-IA-02) ou nouvelle tile `F-197-type-litige-summary` ?
   - Décision : pas de nouvelle tile dédiée (le type litige est déjà visible dans la grille de badges F-162 sur la synthèse). Juste un **badge** côté frontend sur la grille pour signaler override avocat.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|--------------|-----------|
| Type non valide (hors enum) | 400 | 400 |
| Type Travail sur dossier Immigration (incohérent domaine) | 400 "Type non applicable au domaine" | 400 |
| Aucune analyse DONE | 404 "Aucune analyse à overrider" | 404 |
| Autre workspace | 404 camouflage | 404 |

---

## Critères d'acceptation

- [ ] **CA-01 PUT pur** : override persisté sur `case_analyses`, aucun autre side-effect
- [ ] **CA-02 GET retourne override** : `{ typeLitigeAvocat: 'LICENCIEMENT_ECONOMIQUE' }` après PUT
- [ ] **CA-03 propagation au run** : nouvelle analyse créée hérite de l'override de la précédente (clone des 3 colonnes)
- [ ] **CA-04 prompt enrichi** : section `[Type litige fixé par l'avocat]` injectée si override présent
- [ ] **CA-05 F-IA-04 visibility avec override** : `DecisionToolVisibilityService` retourne F-DT-13 (économique) si `type_litige_avocat_override = LICENCIEMENT_ECONOMIQUE`, même si IA a détecté sans cause réelle
- [ ] **CA-06 validation domaine** : type Travail sur dossier Immigration → 400
- [ ] **CA-07 isolation workspace** : autre workspace → 404
- [ ] **CA-08 fail-open propagation** : exception clone override → run réussit + log warn
- [ ] **CA-09 cohérence Stripe-like** : 2ᵉ PUT remplace le 1er (pas d'historique multi-overrides V1)

---

## Hors scope V1

- (a) Historique des overrides (V1 = 1 valeur courante, pas de versioning)
- (b) Override de Famille (`regime_matrimonial`) — V2
- (c) Override `type_litige` BE équivalents — V2
- (d) Override sur sous-types (ex. licenciement économique → motif structurel/conjoncturel) — V2
- (e) UI dans grille de badges F-162 pour signaler override (V1 = juste backend, frontend SF-197-02 fait l'UI)

---

## Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analyses` | ALTER | 3 nouvelles colonnes nullable |

### Migration : `XXX-f197-type-litige-override.xml`

### Composants Spring Boot

- `TypeLitigeOverrideService` (nouveau) — upsert + getForLatestAnalysis + clone-on-new-analysis
- `TypeLitigeOverrideController` (nouveau) — endpoint PUT + GET
- `EnrichedAnalysisService` extension :
  - `buildEnrichedPrompt` : section `[Type litige fixé par l'avocat]` si override présent
  - `run` : clone override de l'analyse précédente vers la nouvelle (avant ou après création — à investiguer dev)
- `DecisionToolVisibilityService` (F-IA-04) extension : lit override en priorité sur `type_litige_detecte`

---

## Plan de test

### UT (~6)

- `TypeLitigeOverrideServiceTest` — upsert + clone + validation domaine
- `EnrichedAnalysisServiceTest` — section prompt présente si override

### IT (~5)

- `TypeLitigeOverrideControllerIT` — PUT/GET + 404 isolation + 401 + 400 invalide
- `EnrichedAnalysisServiceIT` — run avec override → propagation correcte + visibility F-IA-04 utilise override

---

## Dépendances

- F-IA-04 ✅
- F-DT-08/09/10 ✅, F-IM-08/20 ✅
- F-192 SF-192-01 ✅ (pattern partagé)

---

## Notes 2026-05-06

- **Pattern différent de F-192/F-193/F-194/F-195** : single value, pas trichotomie. Pas de table de statut. Juste 3 colonnes nullable.
- Validation domaine côté backend obligatoire — ne pas accepter un type Travail sur dossier Immigration
- Clone automatique de l'override entre analyses : éviter à l'avocat de re-saisir à chaque run
- F-IA-04 visibility lit override en priorité — c'est l'effet le plus impactant produit
