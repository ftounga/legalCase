# Mini-spec — F-FA-23 / SF-FA-23-02 Frontend ordonnance sur requête (mesures urgentes familiales)

## Identifiant

`F-FA-23 / SF-FA-23-02`

## Feature parente

`F-FA-23` — Mesures urgentes familiales (ordonnance sur requête art. 493 CPC FR / art. 1025 et s. CJ BE)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-23-02-frontend-ordonnance-sur-requete`

---

## Objectif

Composant Angular `ordonnance-requete-section` consommant l'API SF-FA-23-01 (endpoints `POST/GET /api/v1/case-files/{caseFileId}/ordonnance-requete-analysis`) pour permettre à l'avocat de saisir les critères d'une requête unilatérale (motif + urgence + dérogation contradictoire + pièce justificative + enfants concernés), afficher le verdict de probabilité (ELEVEE/MOYENNE/FAIBLE), le délai typique (1-3 j IST / 5-15 j autres), le recours adverse (référé-rétractation art. 497 CPC à 15 jours) et la base juridique adaptée FR (art. 493 CPC) ou BE (art. 1025 CJ).

Outil **applicable FR + BE simultanément** — pas de gate pays. Le `baseJuridique` retourné par le backend porte la mention adaptée selon `workspaceCountry` ; le composant ne masque jamais.

---

## Contrat API

Importé de `SF-FA-23-01-backend-ordonnance-sur-requete.md`.

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/ordonnance-requete-analysis` | OIDC |
| GET | `/api/v1/case-files/{caseFileId}/ordonnance-requete-analysis` | OIDC |

**Body POST**
```json
{
  "motifRequete": "ENLEVEMENT_INTERNATIONAL",
  "urgenceJustifiee": true,
  "derogationContradictoireJustifiee": true,
  "pieceJustificativeFournie": true,
  "presenceEnfants": true,
  "commentaireUrgence": "..."
}
```

**Réponse**
```json
{
  "caseFileId": "uuid",
  "motifRequete": "ENLEVEMENT_INTERNATIONAL",
  "scoreEligibilite": 100,
  "verdictAccordeProbabilite": "ELEVEE",
  "criteresRemplis": ["URGENCE_JUSTIFIEE", "DEROGATION_CONTRADICTOIRE_JUSTIFIEE", "PIECE_JUSTIFICATIVE_FOURNIE"],
  "criteresManquants": [],
  "delaiTypiqueJoursMin": 1,
  "delaiTypiqueJoursMax": 3,
  "recoursAdverseDelaiJours": 15,
  "baseJuridique": "Art. 493 + 497 CPC + 373-2-6 Cciv (mesures concernant l'enfant)",
  "formule": "Motif ENLEVEMENT_INTERNATIONAL + 3 critères remplis ...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

**Codes d'erreur** : 400 (workspace ≠ DROIT_FAMILLE / champ absent / body absent), 404 (caseFileId inconnu, GET sans POST).

---

## Comportement attendu

### Cas nominal

1. À l'ouverture du panneau (collapsed=true), l'avocat clique pour étendre.
2. `GET /ordonnance-requete-analysis` charge un éventuel résultat persisté.
3. Si 200 → mode résultat (banner verdict + chip URGENT si motif ENLEVEMENT_INTERNATIONAL ou enfantsConcernés).
4. Si 404 → mode formulaire avec pré-fill IA gracieux (depuis `aiData`).
5. Avocat saisit motif + 3 critères + presenceEnfants + commentaire optionnel.
6. POST → snackbar succès + bascule en mode résultat + `dashboardRefresh.triggerRefresh()`.
7. Bouton "Modifier" repasse en mode formulaire.

### Cas FR + BE actifs (pas de gate pays)

- `workspaceCountry === 'FRANCE'` → fonctionne, `baseJuridique` mentionne art. 493 + 497 CPC.
- `workspaceCountry === 'BELGIQUE'` → fonctionne aussi, `baseJuridique` mentionne art. 1025 CJ.
- Aucune bannière "outil français uniquement" — l'outil tourne sur les 2 pays.

### Cas d'erreur

| Situation | UI |
|-----------|-----|
| Form invalide (motif ou 3 critères absents) | Bouton "Analyser" disabled |
| Erreur backend POST (400/404/500) | snackBar rouge avec message du backend |
| GET 404 initial | Mode formulaire (no-op, pas de toast d'erreur) |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern frontend canonique (immigration-title-decision, protection-rp, pse) | Oui | `protection-rp-section` désigné comme template (PR #634, palette navy/or/rouge, mat-radio, F-IA-03 + builder partagé) |
| Outils famille existants (F-FA-12, F-FA-14, F-FA-25, F-FA-22) | Non — outils jumelés indépendants | Pas de modification |
| Outils FR-only (protection-rp, pse, refere-prudhomal) | Non — pattern différent (gate pays). Cette SF est FR + BE actifs | Pattern emprunté pour la palette/structure mais sans gate `isFrance()` |
| Pré-remplissage IA (`FamilleExtractedData`) | Oui — mais peu de signaux utiles ici | Pré-fill `presenceEnfants` depuis `presenceEnfantsDetected` (existant SF-FA-14) si présent. Pas de pré-fill du motif (pas de signal IA fiable pour les 5 motifs très spécifiques). |
| Cohérence F-IA-03 (CoherenceAlertBuilder) | Oui | 1 alerte sur `PRESENCE_ENFANTS` (divergence IA vs saisie avocat) — utilise `presenceEnfantsDetected` du `FamilleExtractedData` |
| Refresh dashboard F-IA-02 | Oui | `dashboardRefresh.triggerRefresh()` après POST succès |
| TOOL_REGISTRY F-IA-04 | Oui | Entrée `F-FA-23-ordonnance-requete` ajoutée |
| Visibility rules `decision_tool_visibility_rules` | Non — couvert par SF-FA-23-01 (backend INSERT × 2) | Pas de modification frontend |

### Décision

- [x] Étendu — pattern emprunté `protection-rp-section` mais SANS gate pays (FR + BE actifs)
- [x] Pré-fill IA + F-IA-03 minimaux ciblés (1 champ : presenceEnfants)
- [x] TOOL_REGISTRY mis à jour
- [x] CoherenceAlertBuilder partagé utilisé (pas de définition locale ad-hoc)

---

## Impact par domaine métier

Cette feature est **sensible au domaine** : exclusivement DROIT_FAMILLE.
- **Droit du travail** : non applicable (mécanisme distinct art. 145 CPC, hors périmètre F-FA)
- **Droit de la famille (FR)** : applicable — art. 493 + 497 CPC + 373-2-6 Cciv (IST)
- **Droit de la famille (BE)** : applicable — art. 1025 et s. CJ
- **Immigration** : non applicable

Le composant n'effectue **pas** de gate workspaceCountry — la procédure est applicable FR + BE avec base juridique adaptée renvoyée par le backend.

---

## Critères d'acceptation

- [x] **C1** : composant rend `<app-ordonnance-requete-section>` avec inputs `caseFileId`, `workspaceCountry`, `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`
- [x] **C2** : ngOnInit GET 200 → mode résultat hydraté
- [x] **C3** : ngOnInit GET 404 → mode formulaire vide (pas de toast d'erreur)
- [x] **C4** : POST avec form valide → résultat + snackbar succès + `dashboardRefresh.triggerRefresh()`
- [x] **C5** : POST erreur backend → snackBar rouge avec panelClass `snack-error`
- [x] **C6** : `workspaceCountry='BELGIQUE'` → l'outil fonctionne, GET appelé, pas de gate. Test E2E sur baseJuridique BE.
- [x] **C7** : Pré-fill IA `presenceEnfantsDetected` → `presenceEnfants` rempli + provenance 'IA'
- [x] **C8** : Saisie manuelle de `presenceEnfants` après pré-fill → provenance reset à null
- [x] **C9** : F-IA-03 alerte `PRESENCE_ENFANTS` si IA détecte présence d'enfants ≠ saisie avocat
- [x] **C10** : motif ENLEVEMENT_INTERNATIONAL → chip "URGENT" affichée dans header + bandeau result
- [x] **C11** : `presenceEnfants=true` → chip URGENT affichée
- [x] **C12** : Bouton "Modifier" repasse en mode form, conserve les champs résultat précédents
- [x] **C13** : 5 motifs alignés sur enum backend (DETOURNEMENT_PATRIMOINE / ENLEVEMENT_INTERNATIONAL / ORGANISATION_INSOLVABILITE / PREUVE_ABANDON / ACCES_PIECES)
- [x] **C14** : TOOL_REGISTRY contient `'F-FA-23-ordonnance-requete'` → component résolu
- [x] **C15** : self-check grep 5/5 passants

---

## Périmètre

### Hors scope

- Génération du document de requête à imprimer (pattern F-FA-09 acte huissier — backlog)
- Calculs détaillés des saisies conservatoires patrimoniales (montants) — SF-FA-23-03 backlog
- Liaison directe avec calendrier/délais (manual pour V1)

---

## Technique

### Fichiers créés

| Fichier | Rôle |
|---------|------|
| `frontend/src/app/case-files/ordonnance-requete-section/ordonnance-requete-section.component.ts` | Logique : signals, computed, prefillFromAi, buildPresenceEnfantsAlert, calculate(), load() |
| `frontend/src/app/case-files/ordonnance-requete-section/ordonnance-requete-section.component.html` | Template : header collapsible, fieldsets motif/critères/enfants, bandeau verdict + chip URGENT |
| `frontend/src/app/case-files/ordonnance-requete-section/ordonnance-requete-section.component.scss` | Palette navy/or/rouge — rouge réservé chip URGENT (cas d'urgence) |
| `frontend/src/app/case-files/ordonnance-requete-section/ordonnance-requete-section.component.spec.ts` | Jest ≥ 10 tests |
| `frontend/src/app/core/models/ordonnance-requete.model.ts` | Types Request/Response + enums + libellés |
| `frontend/src/app/core/services/ordonnance-requete.service.ts` | HttpClient wrapper (POST, GET) |

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` | Ajout import + entrée TOOL_REGISTRY `'F-FA-23-ordonnance-requete'` |

---

## Plan de test

### Tests Jest `ordonnance-requete-section.component.spec.ts` (≥ 10)

1. FRANCE — ngOnInit GET appelé
2. BELGIQUE — ngOnInit GET appelé aussi (pas de gate pays)
3. GET 200 → mode résultat hydraté
4. GET 404 → mode formulaire
5. formValid : false initial, true quand 4 champs requis présents
6. Pré-fill IA `presenceEnfantsDetected=true` → `presenceEnfants=true` + provenance 'IA'
7. onPresenceEnfantsChange après pré-fill → provenance reset null
8. F-IA-03 alerte PRESENCE_ENFANTS si IA dit true et avocat saisit false
9. coherenceAlerts vides quand showForm=false
10. POST + résultat + snackbar succès + dashboardRefresh
11. POST erreur 400 → snackBar rouge
12. POST omet `commentaireUrgence` si null
13. Chip URGENT sur motif ENLEVEMENT_INTERNATIONAL
14. Chip URGENT sur presenceEnfants=true
15. toggleCollapse / editMode

### Self-check grep 5/5

À exécuter pré-commit. Vérifie :
1. `prefillFromAi(` présent dans component.ts
2. `provenance` (signal IA) présent dans component.ts
3. `coherenceAlerts = computed` présent dans component.ts
4. `CoherenceAlertBuilder` import présent
5. `triggerRefresh` présent

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Outil décisionnel métier** — création d'un nouveau composant frontend de scoring (niveau 5).
- [ ] Auth/Principal — non
- [ ] Workspace context — non
- [ ] Plans/limites — non
- [ ] Navigation — non

### Composants existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `decisional-tools-panel.component.ts` | Ajout entrée TOOL_REGISTRY | Aucun — addition non-breaking |
| `protection-rp-section`, `pse-section`, `refere-prudhomal-section` | Aucun (pattern emprunté, pas modifié) | Aucun |

### Smoke tests E2E

- [x] Aucun smoke test impacté (composant isolé sous le panel F-IA-04)

---

## Dépendances

- **SF-FA-23-01** : backend mergé PR #637 (CONFIRMÉ master 9506925c)
- **SF-IA-04-02** : panel F-IA-04 disponible (master)
- **SF-155-05** : `CoherenceAlertBuilder` partagé (master)

---

## Notes et décisions

- **FR + BE actifs** : décision confirmée — la procédure existe à l'identique dans les 2 pays. Le composant délègue le `baseJuridique` adapté au backend (qui le construit selon `country`).
- **Chip URGENT** : critère IST = `motifRequete === 'ENLEVEMENT_INTERNATIONAL' || presenceEnfants === true`. Affiché dans header (à côté du verdict) + bandeau résultat.
- **Pré-fill IA minimal** : seul `presenceEnfants` est pré-fillable depuis `aiData.presenceEnfantsDetected` (signal existant SF-FA-14). Le motif n'est PAS pré-fill (pas de signal IA fiable pour les 5 motifs très spécifiques — l'avocat doit choisir explicitement).
- **F-IA-03** : 1 alerte sur `PRESENCE_ENFANTS` via CoherenceAlertBuilder (pattern canonique). Pas de double-validation sur les 3 critères booléens (pas de signal IA fiable correspondant).
- **Pas de gate pays** : intentionnel — divergence du pattern protection-rp/pse/refere-prudhomal qui sont FR-only. Cette SF est FR + BE actifs (équivalent direct dans les 2 pays).
- **Test BE** : un test Jest dédié vérifie que `workspaceCountry='BELGIQUE'` ne masque pas l'outil et appelle bien l'API.
