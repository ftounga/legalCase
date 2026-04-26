# Mini-spec — F-IM-20 / SF-IM-20-02 Frontend mesures d'éloignement avancées (FR)

## Identifiant

`F-IM-20 / SF-IM-20-02`

## Feature parente

`F-IM-20` — Mesures d'éloignement avancées (Expulsion préfectorale / ministérielle / sécurité État + IRTF + IAT, distinctes de l'OQTF F-IM-08)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-20-02-frontend-mesures-eloignement`

## Contrat API

Importé tel quel de `SF-IM-20-01-backend-mesures-eloignement-avancees.md` (mergé PR #645). Endpoints :

- `POST /api/v1/case-files/{caseFileId}/mesures-eloignement-analysis`
- `GET  /api/v1/case-files/{caseFileId}/mesures-eloignement-analysis`

5 dispositifs : `EXPULSION_PREFECTORALE`, `EXPULSION_MINISTERIELLE`, `EXPULSION_SECURITE_ETAT`, `IRTF`, `IAT`.
5 motifs menace : `ORDRE_PUBLIC`, `SECURITE_ETAT`, `TERRORISME`, `RECIDIVE_GRAVE`, `AUTRE`.
Verdict : `VALIDE` / `CONTESTABLE` / `NUL`. Juridiction : `TA` / `CE`. Délai recours : 15 / 30 / 60 jours.

---

## Objectif

Exposer dans le panel décisionnel F-IA-04 un composant Angular `<app-mesures-eloignement-section>` qui consomme l'API SF-IM-20-01, FR uniquement, avec pré-remplissage IA + validation de cohérence F-IA-03 sur les champs sensibles.

---

## Comportement attendu

### Cas nominal

1. Ouverture du dossier FR DROIT_IMMIGRATION : `DecisionToolsPanelComponent` rend la section via `TOOL_REGISTRY['F-IM-20-mesures-eloignement']`.
2. Chargement (collapsé par défaut, header avec titre + chip verdict si analyse persistée). À l'expand : GET 200 → résultat hydraté ; GET 404 → mode formulaire avec pré-fill IA gracieux.
3. Le formulaire propose en radio les 5 dispositifs avec sous-libellé CESEDA, puis affiche les champs conditionnels :
   - **EXPULSION_*** (3 sous-types) : `motifMenace` (radio 5 valeurs), toggle `procedureCommissionRespectee` (default true), toggle `urgenceAbsolueJustifiee` (default false).
   - **IRTF** : `motifMenace`, input `dureePresenceIrreguliereMois` (≥ 0), input `dureeCircularitePrecaire` (≥ 0), toggle `comportementAggravant`.
   - **IAT** : `motifMenace` uniquement.
   - Champ commun optionnel : `recoursDelai` (`<input type="date">`, ≤ +1 an).
4. Soumission → POST → bandeau verdict (palette navy/or/rouge stricte) :
   - VALIDE → navy/info (`verified`).
   - CONTESTABLE → or/warning (`warning`).
   - NUL → rouge alerte (`gpp_bad`).
5. Bandeau affiche : `juridictionRecours` (TA/CE), `delaiRecoursJours` formaté (15j / 30j / 60j), `formule` JetBrains Mono, `baseJuridique` JetBrains Mono.
6. `risqueAnnulation` rendus en chips (rouge si vice procédure / motif inadapté / délai expiré, or sinon).
7. `documentsRequis` rendus en chips navy.
8. `messages` rendus en liste avec `legal-citations` pipe.
9. `CaseDashboardRefreshService.triggerRefresh()` appelé après POST OK. Snackbar succès "Mesure d'éloignement analysée".
10. Bouton "Modifier" remet en mode formulaire.

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| `workspaceCountry === 'BELGIQUE'` | Bannière info "Régime FR uniquement" (CESEDA) — pas d'appel HTTP. Backlog F-IM-20-BE mentionné. |
| Form invalide (dispositif manquant ou motif manquant) | Bouton submit disabled, pas de POST. |
| `dureeCircularitePrecaire` ou `dureePresenceIrreguliereMois` < 0 | Bouton submit disabled (validation HTML5 + formValid). |
| POST 400 | Snackbar rouge avec message backend. `calculating()` redescend. |
| POST 404 (dossier inconnu) | Snackbar rouge "Dossier introuvable". |
| GET 404 | Mode formulaire silencieux + tentative `prefillFromAi()`. |
| Réseau down | Snackbar rouge générique. |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils immigration FR** : F-IM-13 Naturalisation (template choisi, palette navy/or/rouge alignée), F-IM-19 Mineurs, F-IM-11 Changement statut. Pas de redondance — F-IM-20 traite les 3 expulsions + IRTF + IAT (CESEDA L.631+/L.612+/L.222+), distincts de l'OQTF F-IM-08 (mesure droit commun) et de la naturalisation F-IM-13.
- [x] **Pays** : France uniquement (CESEDA L.631+/L.612+/L.222+). Bannière info BE explicite. Backlog F-IM-20-BE pour Loi 1980 art. 20-21 / 74/15.
- [x] **Domaine** : DROIT_IMMIGRATION (gate visibility rule priority 76 — ALWAYS_ON).
- [x] **UI patterns** : palette navy/or/rouge canonique, datepicker `<input type="date">` (pas MatDatepicker), JetBrains Mono pour `baseJuridique` + `formule`, `MatSnackBar` pour erreurs, `CaseDashboardRefreshService` après POST.
- [x] **Pré-fill IA + F-IA-03** : sur `dispositif` (mapping libre IA → enum) et `motifMenace`. Détaillé plus bas.

### Cas spécifique : pré-remplissage IA + F-IA-03

- [x] `@Input() aiData?: ImmigrationExtractedData | null` — branché via TOOL_REGISTRY (`ctx.synthesis?.immigrationExtractedData`).
- [x] `@Input() procedureChecks?` / `aiQuestions?` / `piecesManquantes?` — câblés.
- [x] `prefillFromAi()` invoquée dans `ngOnInit()` (après GET 404) ET `ngOnChanges()` quand `aiData` arrive après mount.
- [x] Signal `provenanceDispositif: signal<'IA' | null>` + `provenanceMotif: signal<'IA' | null>` + badge UI `auto_awesome` "Pré-rempli depuis l'analyse".
- [x] Handlers `onDispositifChange` / `onMotifMenaceChange` remettent la provenance à `null` au changement manuel.
- [x] `coherenceAlerts = computed<Partial<Record<MesuresEloignementAlertField, ...>>>()` gate par `showForm() && isFrance()`.
- [x] Builder partagé `CoherenceAlertBuilder.forField<F>('DISPOSITIF' | 'MOTIF_MENACE')` — pas d'interface ad-hoc.
- [x] Hiérarchie F-IA-03 : F96 > QUESTION_IA > IA > PIECE_MANQUANTE.
- [x] `<app-coherence-popover-trigger>` câblée sur les 2 fields.

### Niveaux de vérification

- [x] Modèle TS — `mesures-eloignement.model.ts`.
- [x] Service Angular — `mesures-eloignement.service.ts` (HttpClient).
- [x] Composant standalone — `mesures-eloignement-section/`.
- [x] Tests Jest — ≥ 12 attendus (mount/lifecycle, gate FR/BE, formValid, calculate par dispositif, pré-fill IA, F-IA-03, banner mapping).
- [x] Registre TOOL_REGISTRY — entrée `F-IM-20-mesures-eloignement` symétrique (passe les 4 sources IA).

### Résultat

| Cible | Applicable ? | Traitement |
|-------|--------------|------------|
| Naturalisation (F-IM-13) | Template canonique | Réutilisé (palette + structure) |
| Mineurs (F-IM-19) | Template proche | Réutilisé en partie (multi-fieldsets) |
| Belgique (F-IM-20-BE) | Backlog | Bannière info FR-only |
| Famille / Travail | Non applicable | Outil immigration administrative pure |

---

## Impact par domaine métier

- **Droit du travail** : Non applicable (mesure administrative).
- **Droit de l'immigration FR** : Cœur — les 3 expulsions + IRTF + IAT.
- **Droit de l'immigration BE** : Hors scope (gate + bannière) — backlog F-IM-20-BE.
- **Droit de la famille** : Non applicable.

Sensibilité au domaine : **OUI** — DROIT_IMMIGRATION + FRANCE.

---

## Parité des domaines métier

Niveau **5 (scoring / analyse validité)**.

| Domaine | Outil équivalent ? | Note |
|---------|--------------------|------|
| Travail FR | Non pertinent | Pas de mesure d'éloignement administrative en droit du travail |
| Famille FR | Non pertinent | Idem |
| Immigration BE | À ouvrir au backlog | Loi 15/12/1980 art. 20-21 + 74/15 → F-IM-20-BE |
| Famille BE / Travail BE | Non pertinent | Idem FR |

---

## Critères d'acceptation

- [ ] Composant `MesuresEloignementSectionComponent` standalone, palette navy/or/rouge.
- [ ] Bannière info BE quand `workspaceCountry !== 'FRANCE'`, aucun appel HTTP.
- [ ] GET au mount FR — hydrate `result()` ou bascule en mode formulaire.
- [ ] Form radio 5 dispositifs + radio 5 motifs ; champs conditionnels visibles selon dispositif.
- [ ] `<input type="date">` pour `recoursDelai` (max = +1 an).
- [ ] `formValid()` impose dispositif + motif + (durées ≥ 0 si IRTF saisies).
- [ ] POST envoie uniquement les champs pertinents pour le dispositif choisi.
- [ ] Bandeau VALIDE = navy/info, CONTESTABLE = or/warning, NUL = rouge/critical.
- [ ] Délai recours affiché formaté (15j TA / 30j TA / 60j CE).
- [ ] Pré-fill IA `dispositif` depuis `typeProcedureDetectee` (mapping libre) — badge `auto_awesome` + `provenanceDispositif`.
- [ ] Coherence alert `DISPOSITIF` quand IA / F96 / QUESTION_IA divergent du choix avocat.
- [ ] Coherence alert `MOTIF_MENACE` similaire.
- [ ] `CaseDashboardRefreshService.triggerRefresh()` appelé après POST OK.
- [ ] Snackbar succès / erreur (pas d'`alert()`).
- [ ] Entrée TOOL_REGISTRY `'F-IM-20-mesures-eloignement'` symétrique.
- [ ] Tests Jest ≥ 12 verts.

---

## Périmètre

### Hors scope

- Backend (déjà mergé via SF-IM-20-01 PR #645).
- Belgique (backlog F-IM-20-BE).
- Génération automatique de mémoire CE / requête référé.
- Pré-fill date `recoursDelai` (champ avocat libre — la donnée IA n'est pas suffisamment fiable).
- Modification du prompt d'extraction IA pour exposer `motifMenaceDetecte` / `dispositifEloignementDetecte` (backlog ; le composant est prêt à recevoir ces champs sans changement structurel).

---

## Plan de test (Jest)

1. FRANCE → `isFrance()` true, GET émis au ngOnInit.
2. BELGIQUE → bannière, aucun appel HTTP.
3. GET 200 → résultat hydraté + dispositif persisté + showForm=false.
4. GET 404 → mode formulaire.
5. `formValid()` false sans dispositif ; true dès dispositif + motif (motif requis quel que soit le dispositif).
6. Champs conditionnels : `showExpulsionFields()` / `showIrtfFields()` / `showIatFields()` selon dispositif.
7. `calculate()` POST EXPULSION_PREFECTORALE n'envoie pas `dureePresenceIrreguliereMois`.
8. `calculate()` POST IRTF envoie les durées + comportement, pas `urgenceAbsolueJustifiee`.
9. `calculate()` POST IAT minimal.
10. Snackbar succès après POST 200.
11. Erreur backend → snackbar rouge.
12. Pré-fill IA gracieux : `aiData.typeProcedureDetectee = 'EXPULSION'` mappe vers `EXPULSION_PREFECTORALE` + `provenanceDispositif='IA'`.
13. `coherenceAlerts.DISPOSITIF` présent si IA divergente.
14. `coherenceAlerts.DISPOSITIF` vide si convergence.
15. `bannerClass` mappe les 3 verdicts.
16. `editMode()` réaffiche le form.

---

## Technique

### Endpoints (consommés, pas créés)

| Méthode | URL |
|---------|-----|
| POST | `/api/v1/case-files/{caseFileId}/mesures-eloignement-analysis` |
| GET | `/api/v1/case-files/{caseFileId}/mesures-eloignement-analysis` |

### Composants impactés

| Élément | Action |
|---------|--------|
| `frontend/src/app/core/models/mesures-eloignement.model.ts` | Création |
| `frontend/src/app/core/services/mesures-eloignement.service.ts` | Création |
| `frontend/src/app/case-files/mesures-eloignement-section/*` (4 fichiers) | Création |
| `decisional-tools-panel.component.ts` | Ajout entrée TOOL_REGISTRY + import |

### Préoccupations transversales

- [ ] Auth / Principal — N/A
- [ ] Workspace context — gate via `workspaceCountry` input (déjà résolu)
- [ ] Plans / limites — N/A
- [ ] Navigation — N/A
- [x] Outil décisionnel métier — création nouvel outil F-IM-20 (FR), conforme invariant "1 outil = 1 situation"

---

## Readiness checklist

| Item | Verdict |
|------|---------|
| Mini-spec rédigée | PASS |
| Critères d'acceptation listés | PASS — 15 items |
| Plan de test ≥ 12 | PASS — 16 cas |
| Pattern de référence identifié | PASS — `naturalisation-section` |
| Backend mergé (contrat figé) | PASS — PR #645 |
| Entrée TOOL_REGISTRY libre | PASS — `F-IM-20-mesures-eloignement` |
| Gate FR + bannière BE | PASS |
| Pré-fill IA + F-IA-03 prévus | PASS |

**Verdict global** : PASS.
