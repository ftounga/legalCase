# Mini-spec — F-DT-21 / SF-DT-21-02 Frontend indemnité travail dissimulé

## Identifiant
`F-DT-21 / SF-DT-21-02`

## Feature parente
`F-DT-21` — Travail dissimulé (art. L.8223-1 Code du travail)

## Statut `ready` · Date `2026-04-24` · Branche `feat/SF-DT-21-02-frontend-travail-dissimule`

---

## Objectif

Exposer dans le panel F-IA-04 un composant Angular décisionnel qui calcule
l'indemnité forfaitaire de 6 mois (L.8223-1) via l'API SF-DT-21-01, avec
pré-fill IA du salaire de référence depuis `travailExtractedData` et note
d'info si le salaire a été déduit d'un net (× 1,30). FR uniquement.

---

## Comportement attendu

### Cas nominal

- Panel F-IA-04 expose `F-DT-21-travail-dissimule` (ALWAYS_ON DROIT_DU_TRAVAIL/FRANCE).
- Le composant s'affiche collapsé (chip `— €` avant calcul). À l'ouverture :
  - GET 200 → affiche le résultat persisté (form masqué, bouton Modifier).
  - GET 404 → affiche le form ; si `aiData.salaireBrutMensuel > 0`, pré-remplit
    le champ `salaireMensuelReference` (provenance IA = badge "Pré-rempli
    depuis l'analyse" + icône `auto_awesome`).
  - Si `aiData.salaireEstDeduit === true`, une note info discrète est affichée
    sous le field ("Salaire déduit d'un montant net × 1,30").
- Soumission du form → POST /travail-dissimule → `indemniteForfaitaire`
  affiché, formule en JetBrains Mono, base juridique L.8223-1, messages
  jurisprudentiels (cumul Cass. soc. ch. mixte 26/03/2010, non-cumul
  visite médicale L.4624-1, condition rupture + infraction caractérisée)
  rendus via `LegalCitationsPipe` (JetBrains Mono sur les citations).
- `CaseDashboardRefreshService.triggerRefresh()` appelé après succès POST.
- `MatSnackBar` succès/erreur (pas d'alert/confirm).

### Cas d'erreur

| Situation | Comportement | Affichage |
|---|---|---|
| salaire ≤ 0 ou null | Bouton Calculer désactivé | `formValid()` false |
| 400 backend | Snackbar erreur | `panelClass: 'snack-error'` |
| 403/404 workspace | Snackbar erreur | même chemin |
| `workspaceCountry === 'BELGIQUE'` | Bannière info "Outil applicable aux dossiers droit du travail France" (masquage silencieux interdit — règle CLAUDE.md) | pas de form |

### Pré-fill IA — scope

| Champ UI | Source `aiData` | Comportement |
|---|---|---|
| `salaireMensuelReference` | `salaireBrutMensuel` si > 0 | pré-rempli + badge IA |
| Note info | `salaireEstDeduit === true` | hint "Salaire déduit d'un net × 1,30" |
| (durée travail dissimulé) | aucun champ disponible | **skip gracefully** — pas pertinent pour la formule (6 × salaire) |

**Décision documentée** : la durée de travail dissimulé n'influence PAS la
formule L.8223-1 (indemnité forfaitaire = 6 mois, indépendamment de la durée
de la dissimulation). Pas de pré-fill sur ce point. Les conditions
(rupture + infraction) sont rappelées dans les messages.

### Alerte de cohérence F-IA-03

- `SALAIRE` : si `aiData.salaireBrutMensuel` > 0 et écart relatif > 10 %
  vs saisie avocat → badge warning (pattern SALAIRE_DIVERGENCE_RATIO = 0,10).
- Multi-source enrichie (SF-155-06) : `PIECE_MANQUANTE` contributrice si
  `critereCode` ∈ { `SALAIRE_BRUT_MENSUEL`, `TRAVAIL_DISSIMULE_SALAIRE` }.

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11)
  — réutilisé intégralement (signaux, computed, prefillFromAi, CoherenceAlertBuilder).
- [x] **Pattern IA** : `immigration-title-decision-section` — même logique
  signaux + `@Input` IA.
- [x] **Autres outils décisionnels FR travail** : F-DT-07 Ancienneté, F-DT-09
  Comparateur, F-DT-11 Harcèlement, F-DT-15 Inaptitude, F-DT-17 CDD, F-DT-19
  HS — tous consomment `travailExtractedData` pour le salaire. Pattern aligné.
- [x] **Design system** : Inter + JetBrains Mono (citations/formule), navy/or,
  pas de rouge (réservé critique).
- [x] **Refresh dashboard** : F-IA-02 — `CaseDashboardRefreshService.triggerRefresh()`.
- [x] **Pré-fill IA** : branchement via `synthesis.travailExtractedData`.
- [x] **Persistance** : déjà gérée côté backend SF-DT-21-01 (entity + GET).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Frontend F-DT-21 FR | Oui | Intégré dans cette SF |
| Frontend F-DT-21 BE | Non | Pas d'équivalent L.8223-1 en droit belge (cf. SF-DT-21-01) |
| CoherenceAlertBuilder partagé | Oui | Utilisé |
| `LegalCitationsPipe` | Oui | Utilisé sur `baseJuridique` et chaque `message` |
| `CaseDashboardRefreshService` | Oui | Injecté `@Optional()` |
| Pré-fill IA salaire | Oui | Pattern canonique |
| Pré-fill IA durée | Non applicable | formule 6 × salaire indépendante de la durée |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette SF
- [x] Belgique non applicable (justifié SF-DT-21-01)

### Nouveau pattern UI ou service partagé

Aucun — le composant consomme le template canonique F-DT-11 sans introduire
de nouveau pattern partagé.

---

## Impact par domaine métier

**Sensible au domaine** : DROIT_DU_TRAVAIL FRANCE uniquement.

- Travail FR : cœur de la SF.
- Travail BE : non applicable — la sanction belge du travail non déclaré
  est pénale/administrative (loi-programme 27/12/2006, art. 53), sans
  indemnité forfaitaire civile équivalente. Bannière info côté UI si le
  workspace est `BELGIQUE`.
- Immigration / Famille : non applicable.

---

## Parité des domaines métier

Niveau 3 (calculateur). Règle parité niveau ≥ 5 non applicable.

---

## Critères d'acceptation

- [x] Le composant s'affiche dans le panel F-IA-04 pour un dossier
      DROIT_DU_TRAVAIL / FRANCE via `TOOL_REGISTRY`.
- [x] GET 200 → form masqué, résultat affiché (montant, formule JetBrains
      Mono, base juridique L.8223-1 rendue via `LegalCitationsPipe`, 3 messages).
- [x] GET 404 → form affiché. `aiData.salaireBrutMensuel` > 0 pré-remplit
      le field avec badge IA (provenance = 'IA').
- [x] `aiData.salaireEstDeduit === true` affiche une note info sous le field.
- [x] Modifier le salaire manuellement efface le badge IA (provenance null).
- [x] POST réussi → snackbar succès + `CaseDashboardRefreshService.triggerRefresh()`.
- [x] POST 400 → snackbar erreur (panelClass `snack-error`).
- [x] `formValid()` : salaire > 0 requis.
- [x] `coherenceAlerts.SALAIRE` présente si divergence > 10 %, absente si ≤ 10 %.
- [x] `coherenceAlerts` vide quand `showForm() === false` (anti-bug SF-IA-03-12).
- [x] Workspace BE → bannière info, form masqué.
- [x] Entrée `TOOL_REGISTRY` : `F-DT-21-travail-dissimule` avec bindings
      `caseFileId`, `workspaceCountry`, `aiData`, `procedureChecks`,
      `aiQuestions`, `piecesManquantes`.

---

## Périmètre

### Hors scope

- Pré-fill IA sur une durée de dissimulation (non pertinent formule L.8223-1).
- Gating pays — géré côté backend ET UI (bannière info, pas de masquage silencieux).
- Génération de document / mise en demeure (pas le scope de cet outil).
- Belgique — pas d'équivalent législatif.

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `salaireMensuelReference` | Oui | number > 0, affichage € |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Notes |
|---------|-----|------|-------|
| POST | `/api/v1/case-files/{caseFileId}/travail-dissimule` | Oui | SF-DT-21-01 |
| GET | `/api/v1/case-files/{caseFileId}/travail-dissimule` | Oui | SF-DT-21-01 |

### Composants créés

- `frontend/src/app/core/models/travail-dissimule.model.ts` — types Request/Response.
- `frontend/src/app/core/services/travail-dissimule.service.ts` — wrapper HttpClient.
- `frontend/src/app/case-files/travail-dissimule-section/travail-dissimule-section.component.{ts,html,scss,spec.ts}`.

### Composants modifiés

- `decisional-tools-panel.component.ts` — entrée `TOOL_REGISTRY`
  `F-DT-21-travail-dissimule`.

---

## Plan de test

### Tests unitaires (≥ 15)

- [x] FR → field salaire seul, message cumul Cass. soc. 26/03/2010 rendu.
- [x] BE → bannière info, form absent.
- [x] GET 200 → affichage résultat (montant, formule, messages).
- [x] GET 404 → reste en mode formulaire.
- [x] `formValid` false si salaire null/≤0.
- [x] `calculate()` POST + snackbar succès + refresh dashboard.
- [x] `calculate()` erreur backend → snackbar rouge.
- [x] `calculate()` ignoré si form invalide.
- [x] Pré-fill IA complet (salaire) + GET 404 → valeur + badge IA.
- [x] Pré-fill sans aiData → aucun pré-remplissage.
- [x] `salaireBrutMensuel ≤ 0` → pas de pré-fill.
- [x] `onSalaireChange` efface le badge IA.
- [x] `loadExisting` (GET 200) → jamais de badge IA (persistance prioritaire).
- [x] `coherenceAlerts.SALAIRE` présent si divergence > 10 %.
- [x] `coherenceAlerts.SALAIRE` absent si écart ≤ 10 %.
- [x] `coherenceAlerts` vide si `showForm=false` (anti-bug SF-IA-03-12).
- [x] `salaireEstDeduit=true` → note déduction exposée.
- [x] Messages F-DT-21 spécifiques : cumul Cass. soc. ch. mixte 26/03/2010 +
      non-cumul visite médicale L.4624-1 rendus.
- [x] TOOL_REGISTRY `F-DT-21-travail-dissimule` résout le bon composant +
      inputs attendus (`caseFileId`, `workspaceCountry`, `aiData`,
      `procedureChecks`, `aiQuestions`, `piecesManquantes`).

### Isolation workspace

- Couverte par le backend (SF-DT-21-01 IT) — côté frontend le test vérifie
  la bonne transmission du `caseFileId` dans l'URL.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Aucune préoccupation transversale critique** — subfeature frontend
      isolée consommant une API déjà mergée. Pas d'Auth, pas de plan, pas
      de workspace-switch, pas de navigation.
- [x] **Outil décisionnel métier** — template canonique F-DT-11 réutilisé ;
      registry augmenté d'une entrée ; pas de divergence de palette/pickers.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `decisional-tools-panel` | Nouvelle entrée TOOL_REGISTRY | Jest ciblé vérifie la nouvelle clé |

### Smoke tests E2E

- Aucun smoke test E2E concerné — la feature est un ajout d'outil dans
  un panel déjà testé. Pas de modification de route/guard.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-21-01` — backend mergé (PR #489).
- `SF-155-05` — CoherenceAlert partagée mergée (PR #527).

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- Gating BE : bannière info au lieu de masquage silencieux (règle CLAUDE.md
  nouveau composant décisionnel).
- Le backend filtre déjà par `legalDomain === DROIT_DU_TRAVAIL` (400 sinon).
  On ne dédouble pas ce gate côté frontend — le panel F-IA-04 ne montrera
  l'outil que pour DROIT_DU_TRAVAIL/FRANCE (ALWAYS_ON migration 110).
- Durée de travail dissimulé : non pertinente pour L.8223-1 (indemnité
  forfaitaire 6 mois indépendamment de la durée). Pas de pré-fill.
