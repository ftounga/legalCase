# Mini-spec — F-217 / SF-217-05 — Frontend : section autorité parentale belge

## Identifiant
`F-217 / SF-217-05`

## Feature parente
`F-217` — P2 Famille BE — Vague 2 — Enfants

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`feat/SF-217-05-autorite-parentale-be`

---

## Objectif

Livrer la section Angular de l'outil décisionnel « Autorité parentale (Belgique) » dans le
panel décisionnel, branchée sur l'API figée par SF-217-04, avec son entrée `TOOL_REGISTRY`
et le seed `decision_tool_visibility_rules`.

---

## Comportement attendu

### Cas nominal
1. Sur un dossier de droit de la famille belge, la section `app-autorite-parentale-be-section`
   apparaît dans le panel décisionnel (mode `ALWAYS_ON`, comme les outils Famille BE de
   F-211 et de la Vague 1).
2. L'avocat saisit les éléments de la situation parentale (toggles : filiation établie aux
   deux parents, accord parental, demande d'autorité exclusive, désintérêt durable, mise en
   danger, incapacité d'un parent, décision judiciaire antérieure ; select : mode
   d'hébergement principal) et clique « Calculer ».
3. Le composant POST la requête, affiche le verdict en bandeau (4 niveaux), la voie
   procédurale recommandée, la liste des facteurs (libellé + fondement + drapeau), les
   bases juridiques et les messages.
4. Au rechargement, la section restitue le dernier résultat (GET) ; « Modifier » ré-ouvre
   le formulaire pré-rempli avec le snapshot d'inputs.

### Cas d'erreur
- Workspace `FRANCE` → bannière info « Outil Belgique uniquement », pas de formulaire,
  aucun appel réseau (gate `workspaceCountry` strict).
- Erreur HTTP backend → `MatSnackBar` rouge, le formulaire reste éditable.
- `GET` 404 (jamais calculé) → mode formulaire, pas d'erreur visible.

---

## Contrat API consommé (importé de SF-217-04 — FIGÉ)
- `POST /api/v1/case-files/{caseFileId}/autorite-parentale-be`
- `GET /api/v1/case-files/{caseFileId}/autorite-parentale-be`
- DTO `AutoriteParentaleBeRequest` / `AutoriteParentaleBeResponse`, enums `verdict`,
  `voieProcedurale`, `modeHebergementPrincipal`, `code` de facteur — cf. SF-217-04.

---

## Conformité F-IA-04
- [x] Entrée `TOOL_REGISTRY` ajoutée dans `decisional-tools-panel.component.ts` avec
  `displayLabel` humain (`Autorité parentale (Belgique)`) — garde-fou
  `DecisionToolDisplayLabelIntegrityIT`.
- [x] Entrée `THEME_BY_TOOL` (thème `VALIDITE` — outil de qualification/orientation).
- [x] `static getPrefillCount` + helper co-localisé `*-prefill-rules.ts` étiqueté
  `PREFILL_COUNT_ALWAYS_ZERO = true` (aucun flag pivot IA en V1) — garde-fous
  `prefill-count-integrity.spec.ts` (présence + parité).
- [x] Seed `decision_tool_visibility_rules` : `tool_id = autorite-parentale-be`,
  `ALWAYS_ON`, `DROIT_FAMILLE` / `BELGIQUE`, priority 70 — couplé à l'entrée TOOL_REGISTRY
  dans le même lot (garde-fou `DecisionToolVisibilityIntegrityIT`).

---

## Critères d'acceptation
- [ ] La section apparaît sur un dossier Famille BE, est absente sur un workspace FR.
- [ ] Le formulaire couvre tous les champs du contrat ; le POST envoie le bon body.
- [ ] Le verdict (4 niveaux) et la voie procédurale s'affichent ; rouge réservé à
      `AUTORITE_EXCLUSIVE_NON_FONDEE` (situation défavorable au demandeur).
- [ ] « Modifier » ré-ouvre le formulaire pré-rempli avec le snapshot.
- [ ] `getPrefillCount({}) === 0` ; le test d'intégrité prefill passe.
- [ ] `DecisionToolVisibilityIntegrityIT` + `DecisionToolDisplayLabelIntegrityIT` verts.
- [ ] Tests Jest du composant verts (statics, gate pays, rendu, calculate).

---

## Périmètre
### Hors scope
- Backend (SF-217-04).
- Pré-fill IA (aucun flag pivot V1 — `PREFILL_COUNT_ALWAYS_ZERO`).
- Validation F-IA-03 croisée (pas de champ croisable avec F-96 / questions IA pour cet
  outil en V1 — comme `divorce-ddi-3voies-be` de F-211).

---

## Technique
### Composants / fichiers
| Fichier | Opération |
|---------|-----------|
| `frontend/src/app/case-files/autorite-parentale-be-section/autorite-parentale-be-section.component.ts` | CREATE (standalone) |
| `…/autorite-parentale-be-section.component.html` / `.scss` | CREATE |
| `…/autorite-parentale-be-section.component.spec.ts` | CREATE |
| `…/autorite-parentale-be-section-prefill-rules.ts` / `.spec.ts` | CREATE |
| `frontend/src/app/core/models/autorite-parentale-be.model.ts` | CREATE |
| `frontend/src/app/core/services/autorite-parentale-be.service.ts` | CREATE |
| `decisional-tools-panel.component.ts` | EDIT — import + entrée TOOL_REGISTRY + THEME_BY_TOOL |

### Migration Liquibase
- [x] Oui — `238-seed-f217-vague2-autorite-parentale-visibility.xml`
  (seed `decision_tool_visibility_rules`).

---

## Plan de test
### Tests unitaires (Jest)
- [ ] Statics `TOOL_LABEL` / `TOOL_ICON` / `getPrefillCount` exposés.
- [ ] `getPrefillCount({})` = 0 ; helper `*PrefillRules` aligné.
- [ ] Gate `workspaceCountry=FRANCE` → bannière info, aucun GET.
- [ ] Rendu nominal BE → titre + toggles affichés.
- [ ] `calculate()` POST le body et bascule en mode résultat.

### Isolation workspace
- [x] Couvert backend (SF-217-04) ; le frontend gate l'affichage par `workspaceCountry`.

---

## Analyse d'impact
### Préoccupations transversales
- [x] **Outil décisionnel métier** — nouvelle entrée TOOL_REGISTRY + seed visibilité.
  Scan : un outil = une situation (cf. SF-217-04).
- [x] **Navigation / routing** — non modifié (section additive dans un panel existant).

### Smoke tests E2E
- [x] Aucun — feature additive (nouvelle section dans un panel existant).

---

## Dépendances
- SF-217-04 (backend) — contrat API figé ; merge backend avant frontend.

---

## Notes et décisions
- Wrapper complet (formulaire + calcul + restitution) — pattern `divorce-dc-be-section`
  (F-243) / `regime-communaute-legale-be-section` (F-217 Vague 1).
- Mode `ALWAYS_ON` : l'autorité parentale est une question systématique de tout dossier
  Famille BE comportant un enfant — situation toujours pertinente, pas à détecter
  (cf. `SF-217-00-coherence.md` ajustement n° 1).
</content>
