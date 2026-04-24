# Mini-spec — F-DT-12 / SF-DT-12-02 Discrimination — dommages-intérêts (frontend)

## Identifiant
`F-DT-12 / SF-DT-12-02`

## Feature parente
`F-DT-12` — Discrimination — outil décisionnel FR + BE (critique 🔴)

## Statut `ready` · Date `2026-04-24` · Branche `feat/SF-DT-12-02-frontend-discrimination`

---

## Objectif

Livrer le composant Angular `<app-discrimination-section>` consommant l'API SF-DT-12-01 (POST/GET `/api/v1/case-files/{id}/discrimination-dommages-interets`), branché au panel décisionnel F-IA-04 via TOOL_REGISTRY pour le toolId `F-DT-12-discrimination-dommages-interets`. Le composant affiche la fourchette indicative [min, médiane, max] des dommages-intérêts discrimination, avec pré-fill IA limité au salaire et alertes de cohérence F-IA-03 (palier 1 — motif et contexte saisis à la main, l'IA travail n'extrait pas encore ces codes).

---

## Comportement attendu

### Cas nominal

1. Composant monté par le panel F-IA-04 quand le toolId est ALWAYS_ON (FR et BE).
2. `ngOnInit()` effectue `GET` → si 200, affiche la fourchette persistée ; si 404, reste en mode formulaire et tente `prefillFromAi()`.
3. Form : 3 champs — `salaireMensuelReference` (nombre, required), `motifDiscrimination` (select, 8 codes FR ou 5 BE selon `workspaceCountry`), `contexteActe` (select, 6 codes communs).
4. Bouton **Calculer** → POST → rafraîchit le dashboard (`CaseDashboardRefreshService.triggerRefresh()`) + snackbar vert + affichage de la fourchette + `baseJuridique` (en JetBrains Mono via `LegalCitationsPipe`) + `messages` (régime probatoire, prescription, cumul F-DT-11).
5. Bouton **Modifier** → repasse en mode formulaire.
6. Salaire seul est pré-remplissable depuis `aiData.salaireBrutMensuel` (badge `auto_awesome` "Pré-rempli depuis l'analyse") — motif et contexte restent à saisir manuellement par l'avocat (extension backend → future SF, cf. §Hors scope).

### Cas d'erreur

| Situation | Comportement attendu | Source |
|-----------|---------------------|--------|
| Champ manquant (salaire ≤ 0 / motif / contexte) | Bouton désactivé ; pas d'appel HTTP | `formValid()` |
| POST 400 (motif FR sur workspace BE ou inverse) | Snackbar rouge avec message serveur | Handler erreur |
| POST 400 (motif/contexte inconnu) | Snackbar rouge | Handler erreur |
| POST 404 workspace étranger | Snackbar rouge | Handler erreur |
| POST 500 inattendu | Snackbar rouge "Erreur lors du calcul" | Handler erreur |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier FR+BE dual calcul montant** : F-DT-11 harcèlement, F-DT-15 inaptitude, F-DT-19 heures sup — tous déjà pré-fill IA palier 1 via pattern `CoherenceAlertBuilder`.
- [x] **Autres pays** : `workspaceCountry` dual FR/BE — liste motifs différente (8 FR vs 5 BE). Gate via `motifsDisponibles = computed()`.
- [x] **Autres domaines** : DROIT_DU_TRAVAIL uniquement (backend gate via `resolveCaseFile`). Pas d'impact famille/immigration.
- [x] **Autres UI patterns** : formulaires réactifs signals, MatSnackBar, MatSelect, badge "auto_awesome", popover F-IA-03 (`CoherencePopoverTriggerDirective`), wrapper `field-with-alert`.
- [x] **Autres flows transversaux** : refresh dashboard F-IA-02 (obligatoire après POST succès).

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : divergence salaire (>10%) via `CoherenceAlertBuilder.forField('SALAIRE').addSource('IA', ...)` ; enrichissement PIECE_MANQUANTE (fiche de paie) si disponible. Pas d'alerte motif/contexte à ce palier (pas de champ IA correspondant — palier 2 future SF).
- [x] **Refresh dashboard (F-IA-02)** : `this.dashboardRefresh?.triggerRefresh()` dans le `next:` du POST.
- [x] **Pré-remplissage IA** : `prefillFromAi()` invoqué dans le fallback 404 du GET + re-appliqué dans `ngOnChanges(aiData)` si form vide. Provenance effacée au changement manuel (`onSalaireChange`).
- [x] **Persistance inputs** : garantie backend (DiscriminationAnalysis — colonnes dédiées + result_data JSON).
- [x] **Masquage conditionnel** : orchestration F-IA-04 (ALWAYS_ON pour DROIT_DU_TRAVAIL FR/BE).
- [x] **Alertes actives après calcul** : gate `if (!this.showForm()) return {}` dans `coherenceAlerts` — conforme SF-IA-03-12.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] Le composant et son service sont **instance d'un pattern existant** (canonique `harcelement-licenciement-nul-section` + `immigration-title-decision-section`). Aucun nouveau pattern partagé introduit. Utilise `CoherenceAlertBuilder` + `CoherenceAlert<F>` de SF-155-05. Pas de nouveau composant `shared/`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-11 (harcèlement — template canonique) | Oui | Pattern copié — structure identique (form 2 champs → 3 champs). |
| F-DT-15 (inaptitude) | Oui | Pattern vu ; alerts salaire divergence réutilisé à l'identique. |
| F-DT-19 (heures sup) | Oui | Pattern vu ; `CoherenceAlertBuilder` utilisé à l'identique. |
| F-IM-05 (immigration — canonique IA) | Oui | Pattern vu ; signals miroirs `aiDataSignal` + `ngOnChanges` réactif. |
| `decisional-tools-panel.TOOL_REGISTRY` | Oui | Ajout d'une entrée symétrique F-DT-12. |
| Extension IA extraction `motifDiscrimination` / `contexteActe` | Non (palier 2) | Backlog — à ouvrir comme SF-DT-12-03 "pré-fill motif/contexte" quand le prompt IA travail extraira ces codes. |
| Parité Famille / Immigration | Non applicable | F-DT-12 = niveau 3 hybride (calculateur fourchette). Pas de parité ≥5 requise. |

### Décision

- [x] Étendu aux cibles applicables dans cette subfeature (template canonique copié, registry étendu, builder réutilisé).
- [x] Backlog pour les cibles restantes : extension IA extraction motif/contexte discrimination (à référencer dans PRODUCT_SPEC au besoin).

---

## Critères d'acceptation

- [ ] Composant standalone `DiscriminationSectionComponent` créé avec selector `app-discrimination-section`, palette navy/or conforme au design system.
- [ ] Modèle TS `DiscriminationRequest` / `DiscriminationResponse` / enums `MotifDiscriminationFr` / `MotifDiscriminationBe` / `ContexteActe` alignés 1:1 sur `DiscriminationRequest.java` + `DiscriminationResponse.java`.
- [ ] Service `DiscriminationService` avec méthodes `calculate(caseFileId, request)` et `get(caseFileId)` ciblant `/api/v1/case-files/{id}/discrimination-dommages-interets`.
- [ ] `workspaceCountry === 'FRANCE'` → 8 motifs FR visibles ; `BELGIQUE` → 5 motifs BE.
- [ ] 6 contextes d'acte toujours visibles (communs FR/BE).
- [ ] Pré-fill IA salaire depuis `aiData.salaireBrutMensuel` (si > 0) + badge `auto_awesome`.
- [ ] Badge IA effacé dès modification manuelle (`onSalaireChange`).
- [ ] Divergence salaire > 10 % → alerte `coherenceAlerts.SALAIRE` via `CoherenceAlertBuilder`.
- [ ] GET 200 → valeurs persistées prioritaires (pas de badge IA).
- [ ] POST succès → snackbar vert + `CaseDashboardRefreshService.triggerRefresh()`.
- [ ] POST erreur → snackbar rouge avec message serveur.
- [ ] TOOL_REGISTRY enrichi avec l'entrée `F-DT-12-discrimination-dommages-interets` (symétrique F-DT-11).
- [ ] ≥ 15 tests Jest verts.

---

## Périmètre

### Hors scope (explicite)
- **Pré-fill IA de `motifDiscrimination` / `contexteActe`** : aucun champ actuellement exposé par `TravailExtractedData` — à ouvrir en SF-DT-12-03 palier 2 (backend prompt IA + mapping frontend). Le palier actuel reproduit le pattern F-155-04 palier 1 (salaire seul pré-remplissable).
- Alertes F96 / QUESTION_IA sur motif/contexte : pas de `critereCode` backend pour l'instant.
- Détection automatique d'event déclencheur "discrimination détectée" → resterait à F-DT-08 validité licenciement si besoin.
- Extraction IA de la différence salariale chiffrée.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Normalisation |
|-------|-------------|-----------------|---------------|
| `salaireMensuelReference` | Oui | Nombre > 0 | — |
| `motifDiscrimination` | Oui | 8 codes FR ou 5 codes BE selon `workspaceCountry` | — |
| `contexteActe` | Oui | `LICENCIEMENT`, `SANCTION`, `EMBAUCHE_REFUSEE`, `PROMOTION_REFUSEE`, `DIFFERENCE_SALARIALE`, `HARCELEMENT_LIE_DISCRIMINATION` | — |

---

## Technique

### Endpoint (contrat importé de SF-DT-12-01 backend)

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{id}/discrimination-dommages-interets` | Oui |
| GET | idem | Oui |

### Composants Angular

- `DiscriminationSectionComponent` — standalone, signals, form 3 champs, pré-fill salaire, alertes cohérence salaire.

### Fichiers créés / modifiés

- `frontend/src/app/core/models/discrimination.model.ts` (nouveau)
- `frontend/src/app/core/services/discrimination.service.ts` (nouveau)
- `frontend/src/app/case-files/discrimination-section/` (4 fichiers — .ts, .html, .scss, .spec.ts)
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (modifié — ajout entrée TOOL_REGISTRY)

---

## Plan de test

### Tests Jest composant (≥ 15)

1. Workspace FR → 8 motifs, contient `SEXE_GROSSESSE` et `AGE`.
2. Workspace BE → 5 motifs, contient `DISCRIMINATION_GENRE_BE`.
3. 6 contextes d'acte toujours disponibles.
4. `toggleCollapse()` bascule `collapsed`.
5. `editMode()` bascule `showForm` à `true`.
6. GET 200 → valeurs persistées, `showForm=false`, pas de badge IA.
7. GET 404 + aiData absent → reste en form, rien de pré-rempli.
8. GET 404 + aiData.salaireBrutMensuel=3000 → salaire pré-rempli, badge IA, motif/contexte vides.
9. `formValid()` false si salaire=null / motif=null / contexte=null ; true si tous remplis.
10. `calculate()` ignoré si form invalide (pas d'appel HTTP).
11. POST succès → result set, `showForm=false`, snackbar vert, `triggerRefresh` appelé.
12. POST erreur 400 (motif FR sur BE) → snackbar rouge `panelClass: 'snack-error'`.
13. `onSalaireChange` efface le badge IA salaire.
14. Divergence salaire (IA 3000 vs saisie 5000) → `coherenceAlerts.SALAIRE` défini avec `source=IA`.
15. Écart salaire ≤ 10 % (3000 → 3100) → pas d'alerte.
16. `ngOnChanges(aiData)` post-mount applique le pré-fill si form vide.
17. Alertes masquées après résultat affiché (`showForm=false` → `coherenceAlerts = {}`).
18. Piece manquante salaire (`critereCode` SALAIRE_BRUT_MENSUEL) → enrichit l'alerte `contributors`.

### Tests d'intégration / isolation workspace

- Backend déjà couvert par SF-DT-12-01 (16 UT + 10 IT dont isolation workspace).
- Frontend : les tests Jest utilisent `HttpClientTestingModule` (mock HTTP) — isolation workspace garantie côté serveur.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée (nouveau composant + nouvelle entrée TOOL_REGISTRY). Pas d'auth, workspace ou plans touchés (gardés inchangés par le backend SF-DT-12-01).

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de routing ajouté, pas de modification de `case-file-detail` hors TOOL_REGISTRY (mécanisme data-driven conforme F-IA-04).

---

## Impact par domaine métier

- **Sensible au domaine ?** Oui — DROIT_DU_TRAVAIL uniquement (gate backend).
- **FR + BE** : oui — 2 sets de motifs distincts, gate frontend via `motifsDisponibles` computed.
- **Parité ≥5** : non applicable — outil de niveau 3 hybride (calculateur fourchette). Pas d'obligation d'équivalent Famille / Immigration.

---

## Dépendances

### Subfeatures bloquantes

- SF-DT-12-01 (backend) — **Done** (mergée PR #492 2026-04-24).
- SF-155-05 (`CoherenceAlertBuilder`) — **Done** (mergée avant 0a0d679).

### Questions ouvertes

- Aucune.

---

## Notes et décisions

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11, même famille calcul montant FR+BE dual). Structure HTML/SCSS copiée : header + chip résultat + body form/result + messages. Adapté 2 champs (HLN) → 3 champs (Discrimination).
- **Pattern IA** : `immigration-title-decision-section` (signals miroirs + `ngOnChanges` réactif + provenance par champ). Adopté à l'identique pour `salaireMensuelReference`.
- **Aucun MatDatepicker** (pas de champ date ici).
- **Palette** : navy/or standard — pas de rouge (outil calcul montant, pas d'urgence temporelle).
- **Polices** : Inter (labels, form), JetBrains Mono (`baseJuridique`, `formule`, messages).
- **Labels FR** : version verbeuse en langage avocat ("Origine / ethnie / nationalité", "Sexe / grossesse / maternité"…).
