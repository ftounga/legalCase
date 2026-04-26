# Mini-spec — F-IM-12 / SF-IM-12-02 Frontend asile avancé (Dublin III, accélérée, réexamen, apatride, protection subsidiaire)

## Identifiant

`F-IM-12 / SF-IM-12-02`

## Feature parente

`F-IM-12` — Asile avancé (Dublin III, procédure accélérée, réexamen, apatride, protection subsidiaire)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-12-02-frontend-asile-avance`

## Référence backend (contrat figé)

Contrat importé de `SF-IM-12-01` (mergé PR #644). Voir `docs/features/F-IM-12/SF-IM-12-01-backend-asile-avance.md`.

---

## Objectif

Livrer la section frontend Angular qui permet à l'avocat (workspace `FRANCE`, dossier
`DROIT_IMMIGRATION`) de saisir le dispositif d'asile et les flags pivots adéquats, puis
d'afficher le verdict, le délai d'instruction, le recours possible, les pièces requises
et les risques de refus calculés par le backend `SF-IM-12-01`.

---

## Contrat API (figé en SF-IM-12-01)

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/asile-avance-analysis` | Oui (OAuth2) |
| GET  | `/api/v1/case-files/{caseFileId}/asile-avance-analysis` | Oui (OAuth2) |

### Body POST (`AsileAvanceRequest`)

| Champ | Type | Conditionnel |
|-------|------|--------------|
| `dispositifAsile` | enum string : `DUBLIN_III` / `PROCEDURE_ACCELEREE` / `REEXAMEN` / `APATRIDIE` / `PROTECTION_SUBSIDIAIRE` | Toujours requis |
| `dateDecisionAnterieure` | LocalDate ISO `YYYY-MM-DD` | REEXAMEN |
| `elementsNouveaux` | Boolean | REEXAMEN |
| `paysOrigineDansListeSurs` | Boolean | PROCEDURE_ACCELEREE |
| `empreintesEurodacAutresEm` | Boolean | DUBLIN_III |
| `demandeurEnFuite` | Boolean | DUBLIN_III (modifie délai 6→18 mois) |
| `motifsExclusion` | Boolean | APATRIDIE / PROTECTION_SUBSIDIAIRE (bloquant) |
| `traitementsGravesEtablis` | Boolean | PROTECTION_SUBSIDIAIRE (pivot) |
| `fraudeDocumentaireAvere` | Boolean | PROCEDURE_ACCELEREE (motif alternatif) |
| `refusPriseEmpreintes` | Boolean | PROCEDURE_ACCELEREE (motif alternatif) |
| `presenceReguliere` | Boolean | APATRIDIE |

### Body GET / POST réponse (`AsileAvanceResponse`)

| Champ | Type |
|-------|------|
| `caseFileId` | UUID |
| `country` | `"FRANCE"` |
| `dispositifAsile` | string (code normalisé) |
| `dispositifLibelle` | string humain |
| `verdictRecevabilite` | enum string (8 valeurs : `RECEVABLE_TRANSFERT` / `FRANCE_COMPETENTE` / `ACCELEREE_APPLICABLE` / `ACCELEREE_NON_APPLICABLE` / `RECEVABLE_REEXAMEN` / `RECEVABLE_APATRIDIE` / `RECEVABLE_PROTECTION_SUBSIDIAIRE` / `IRRECEVABLE`) |
| `delaiInstructionMois` | number (peut être fractionnaire) |
| `recoursPossible` | string |
| `documentsRequis` | string[] |
| `risqueRefus` | string[] |
| `baseJuridique` | string |
| `formule` | string |
| `messages` | string[] |

### Codes d'erreur

| Code HTTP | Message attendu |
|-----------|-----------------|
| 400 | `dispositifAsile` absent / inconnu |
| 400 | Workspace `BELGIQUE` (régime FR) |
| 400 | Dossier non `DROIT_IMMIGRATION` |
| 404 | Dossier inexistant ou autre workspace / GET sans POST préalable |

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier `DROIT_IMMIGRATION` (workspace FR).
2. La section "Asile avancé (FR)" apparaît dans le panel décisionnel (F-IA-04).
3. La section interroge `GET .../asile-avance-analysis` au mount.
   - 200 → résultat hydraté + `showForm=false` (mode lecture).
   - 404 → mode formulaire + pré-fill IA gracieux.
4. L'avocat choisit le dispositif (`mat-radio-group` 5 options) et les champs
   conditionnels apparaissent en fonction.
5. Pour chaque dispositif :
   - DUBLIN_III : toggle `empreintesEurodacAutresEm` + toggle `demandeurEnFuite` + date décision antérieure (optionnelle).
   - PROCEDURE_ACCELEREE : toggle `paysOrigineDansListeSurs` + toggle `fraudeDocumentaireAvere` + toggle `refusPriseEmpreintes`.
   - REEXAMEN : date décision antérieure + toggle `elementsNouveaux`.
   - APATRIDIE : toggle `motifsExclusion` + toggle `presenceReguliere`.
   - PROTECTION_SUBSIDIAIRE : toggle `traitementsGravesEtablis` + toggle `motifsExclusion`.
6. Soumission → POST → bandeau verdict 3 niveaux (info/warning/critical) + délai
   d'instruction + recours + chips documents + chips risques + messages.
7. `CaseDashboardRefreshService.triggerRefresh()` après succès.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Form incomplet (dispositif non choisi) | bouton désactivé |
| Workspace BELGIQUE | bannière info "Régime FR" + form masqué (pas d'appel HTTP) |
| Backend 400/4xx | snackbar rouge avec message backend |
| Backend 404 GET | mode formulaire + pré-fill IA gracieux |

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Template canonique** : `naturalisation-section` (PR #646 — pattern récent immigration FR
  avec radio voie + champs conditionnels par voie + bandeau 3 niveaux + helper
  `CoherenceAlertBuilder` partagé).
- **Autres pays** : Belgique exclue (régime CGRA + Loi 15/12/1980 distinct — backlog
  F-IM-12-BE si confirmé). Bannière info gracieuse (pas masquage silencieux).
- **Autres domaines** : non applicable (asile = pure immigration).
- **Autres UI patterns concurrents** : `naturalisation-section` (FR-only multi-voie),
  `mineurs-immigration-section` (FR-only multi-régime), `changement-statut-section`
  (FR-only multi-fieldset). Pattern unifié — réutilisé tel quel.

### Niveaux de vérification couverts

- [x] Modèle TypeScript (`asile-avance.model.ts`) : enum + DTOs + helpers de mapping IA.
- [x] Service Angular (`asile-avance.service.ts`) : wrapper `HttpClient` POST/GET.
- [x] Composant standalone (`asile-avance-section`) : signal-based + `mat-radio` + champs
      conditionnels par dispositif + bandeau verdict.
- [x] Tests Jest ≥ 12 couvrant lifecycle / form valid / champs conditionnels par dispositif
      / calculate / F-IA-03 / divers.
- [x] Intégration `decisional-tools-panel` : entrée `'F-IM-12-asile-avance'` dans
      `TOOL_REGISTRY` symétrique aux autres outils immigration FR.

### Cas spécifique : nouvelle feature d'outil décisionnel (RÈGLE FONDAMENTALE)

- [x] **Pré-remplissage IA** : `prefillFromAi()` invoqué dans `ngOnInit()` ET `ngOnChanges()`.
      Mapping gracieux depuis `aiData.typeProcedureDetectee` → `dispositifAsile` (ASILE_DUBLIN
      / ASILE_ACCELEREE / ASILE_REEXAMEN / ASILE_APATRIDIE / ASILE_PROTECTION_SUBSIDIAIRE).
      Signal `provenanceDispositif: 'IA' | null`. Badge UI `auto_awesome`. Handler
      `onDispositifChange()` qui efface la provenance.
- [x] **Validation IA au changement F-IA-03** : `coherenceAlerts` computed sur le field
      `DISPOSITIF_ASILE` consolidant 4 sources (F-96 > Question IA > IA > pièce manquante)
      via `CoherenceAlertBuilder.forField()`. Directive `<app-coherence-popover-trigger>`
      câblée sur le radio.
- [x] **Refresh dashboard (F-IA-02)** : `CaseDashboardRefreshService.triggerRefresh()` après POST.
- [x] **Snackbar erreurs** : `MatSnackBar` (pas `alert/confirm`).
- [x] **Polices** : Inter pour le reste, JetBrains Mono pour `baseJuridique`, `formule`,
      `delaiInstructionMois`.
- [x] **Palette statut** : navy/or/rouge — rouge réservé verdict `IRRECEVABLE` ou risques
      bloquants (motifs d'exclusion, traitement imminent).
- [x] **Datepicker** : `<input type="date">` natif (pas `MatDatepicker`).
- [x] **Gate `workspaceCountry`** : bannière info BE (pas masquage silencieux).
- [x] **Entrée TOOL_REGISTRY** : `inputs(ctx) => ({ caseFileId, workspaceCountry, aiData,
      procedureChecks, aiQuestions, piecesManquantes })` symétrique aux autres outils FR.

### Cas spécifique : nouveau pattern UI ou service partagé

Aucun — réutilise strictement `CoherenceAlertBuilder` (helper partagé F-IA-03), pattern
canonique `naturalisation-section`. Pas de directive ni service nouveau introduit.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern radio + champs conditionnels | Oui | Réutilisé tel quel (`naturalisation-section`) |
| Belgique (CGRA / Loi 1980) | Non (cette SF) | Backlog F-IM-12-BE si besoin métier |
| Autres domaines (travail / famille) | Non | Asile = pure immigration |
| `CoherenceAlertBuilder` partagé | Oui | Utilisé (1 field `DISPOSITIF_ASILE`) |
| `CaseDashboardRefreshService` | Oui | Appelé après succès POST |
| Pré-fill IA / F-IA-03 | Oui — RÈGLE FONDAMENTALE | Implémenté (5 niveaux checklist) |

### Décision

- [x] Étendu à toutes les cibles applicables côté frontend dans cette subfeature.
- [x] Backlog F-IM-12-BE noté pour besoin futur.

---

## Impact par domaine métier

Cette feature est **spécifique au domaine droit de l'immigration** (asile FR exclusivement).
Elle ne s'applique ni au droit du travail ni au droit de la famille. Elle est
**single-country FRANCE** — le régime belge (CGRA + Loi 15/12/1980) est en backlog
(F-IM-12-BE). Pas d'impact transversal sur les autres outils.

## Parité des domaines métier

Outil de niveau **5 (scoring / analyse de validité)** — verdict `RECEVABLE_*` /
`IRRECEVABLE` selon flags pivots. Asile n'a **pas d'équivalent** en droit du travail ou
en droit de la famille (concept juridique strictement immigration). Pas de feature
jumelle dans les autres domaines.

---

## Critères d'acceptation

- [x] Mount dossier FR `DROIT_IMMIGRATION` → GET émis ; 200 = lecture, 404 = formulaire.
- [x] Mount dossier BE → bannière info BE, aucun appel HTTP.
- [x] 5 dispositifs disponibles via `mat-radio-group`.
- [x] Champs conditionnels affichés selon le dispositif sélectionné.
- [x] Form valid uniquement quand dispositif choisi (et date décision antérieure pour REEXAMEN).
- [x] Submit DUBLIN_III avec empreintes → bandeau "Recevable transfert" navy + délai 6 mois.
- [x] Submit DUBLIN_III avec demandeur en fuite → délai 18 mois.
- [x] Submit PROCEDURE_ACCELEREE avec pays sûr → bandeau "Accélérée applicable" or + délai 1.5 mois.
- [x] Submit REEXAMEN sans éléments nouveaux → bandeau rouge "Irrecevable" + délai 0.3 mois.
- [x] Submit APATRIDIE avec motifs d'exclusion → bandeau rouge "Irrecevable" + délai 12 mois.
- [x] Submit PROTECTION_SUBSIDIAIRE avec traitements établis → bandeau navy + délai 18 mois.
- [x] Pré-fill IA fonctionnel : `aiData.typeProcedureDetectee = "ASILE_DUBLIN_III"` →
      `dispositifAsile = "DUBLIN_III"` + provenance `IA`.
- [x] Alerte F-IA-03 : IA détecte `ASILE_DUBLIN_III` mais avocat saisit `APATRIDIE` →
      badge orange "Incohérence détectée".
- [x] Multi-sources F96 + IA → source `MULTI` + 2 contributors.
- [x] Champs spécifiques d'un autre dispositif **ne sont pas envoyés** au backend.
- [x] Backend 4xx → snackbar rouge.
- [x] Refresh dashboard appelé après POST succès.
- [x] Tests Jest ≥ 12 verts.
- [x] Self-check 5/5.

---

## Périmètre

### Hors scope (explicite)

- Backend (déjà livré en SF-IM-12-01).
- Régime BE (backlog F-IM-12-BE).
- Génération automatique de mémoires de recours (autre feature).
- Mise à jour dynamique de la liste OFPRA pays sûrs (référentiel statique côté backend).

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs |
|-------|-------------|-----------------|
| `dispositifAsile` | Oui | enum 5 valeurs |
| `dateDecisionAnterieure` | Conditionnel REEXAMEN | `<input type="date">` natif |
| Tous les booleans | Optionnels | `mat-slide-toggle` (3-state via signal `null` initial possible mais UI bool) |

---

## Technique

### Fichiers créés

- `frontend/src/app/core/models/asile-avance.model.ts`
- `frontend/src/app/core/services/asile-avance.service.ts`
- `frontend/src/app/case-files/asile-avance-section/asile-avance-section.component.ts`
- `frontend/src/app/case-files/asile-avance-section/asile-avance-section.component.html`
- `frontend/src/app/case-files/asile-avance-section/asile-avance-section.component.scss`
- `frontend/src/app/case-files/asile-avance-section/asile-avance-section.component.spec.ts`

### Fichiers modifiés

- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`
  → ajout entrée `'F-IM-12-asile-avance'`.

### Pas de migration / pas de backend touché

---

## Plan de test (Jest)

≥ 12 tests :

1. `FRANCE → isFrance() true, GET émis au ngOnInit`
2. `BELGIQUE → isFrance() false, aucun appel HTTP`
3. `GET 200 → résultat hydraté + dispositif persisté + showForm=false`
4. `GET 404 → mode formulaire (showForm=true)`
5. `formValid false initialement` (aucun dispositif)
6. `formValid true dès dispositif choisi (DUBLIN_III)`
7. `champs conditionnels selon dispositif` (5 cas)
8. `pré-fill IA : ASILE_DUBLIN_III → DUBLIN_III + provenance IA`
9. `pré-fill IA : aiData absent → no-op`
10. `onDispositifChange efface provenance`
11. `calculate() POST DUBLIN_III avec pivots ciblés`
12. `calculate() POST REEXAMEN avec date décision`
13. `calculate() ne send pas champs d'autres dispositifs`
14. `calculate() ignoré si form invalide (no HTTP)`
15. `calculate() backend erreur → snackbar`
16. `coherenceAlerts vide quand convergent`
17. `coherenceAlerts présent si IA divergente → source IA`
18. `coherenceAlerts multi-sources F96 + IA → MULTI`
19. `bannerClass mappe verdict → classe CSS`
20. `bannerLabel humain par verdict`

### Isolation workspace

- [x] Couverte par les gates `workspaceCountry` côté frontend + l'isolation backend
      (404 sur dossier d'un autre workspace garanti par SF-IM-12-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Outil décisionnel métier** — Nouvelle entrée `TOOL_REGISTRY` ajoutée au panel
      F-IA-04. Aucun outil existant modifié, scan effectué : asile = situation distincte
      des autres outils immigration (titre séjour / OQTF / recours / mineurs / changement
      statut / naturalisation / régularisation / aide). Pattern unique, pas de twin.

### Smoke tests E2E concernés

- [x] Aucun (composant nouveau non câblé sur un flow critique d'auth/workspace).

---

## Dépendances

### Subfeatures bloquantes

- F-IA-04 (mergée) — visibility rule dans la migration backend SF-IM-12-01.
- SF-IM-12-01 (mergée PR #644) — endpoint backend.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- Pattern de référence : `naturalisation-section` (PR #646).
- Helper partagé `CoherenceAlertBuilder.forField('DISPOSITIF_ASILE')`.
- Single-country FR cohérent avec le backend.
- Date décision antérieure : `<input type="date">` natif (convention Calendrier FR-only
  conformément CLAUDE.md).
- Mat-slide-toggle pour les booleans (3-state cassé en bool — défaut `false` si non précisé).
- Le bandeau verdict utilise 3 niveaux :
  - `RECEVABLE_TRANSFERT` / `FRANCE_COMPETENTE` / `RECEVABLE_APATRIDIE` /
    `RECEVABLE_PROTECTION_SUBSIDIAIRE` / `RECEVABLE_REEXAMEN` → navy/info.
  - `ACCELEREE_APPLICABLE` / `ACCELEREE_NON_APPLICABLE` → or/warning.
  - `IRRECEVABLE` → rouge/critical.
