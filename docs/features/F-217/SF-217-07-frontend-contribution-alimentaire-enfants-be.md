# Mini-spec — F-217 / SF-217-07 — Frontend : section contribution alimentaire des enfants (méthode Renard) BE

## Identifiant
`F-217 / SF-217-07`

## Feature parente
`F-217` — P2 Famille BE — Vague 2 — Enfants

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
`feat/SF-217-07-contribution-alimentaire-enfants-be`

---

## Objectif

Livrer la section Angular de l'outil décisionnel « Contribution alimentaire des enfants
(Belgique) » dans le panel décisionnel, branchée sur l'API figée par SF-217-06, avec son
entrée `TOOL_REGISTRY` et le seed `decision_tool_visibility_rules`.

---

## Comportement attendu

### Cas nominal
1. Sur un dossier de droit de la famille belge, la section
   `app-contribution-alimentaire-enfants-be-section` apparaît dans le panel décisionnel
   (mode `ALWAYS_ON`).
2. L'avocat saisit : nombre d'enfants, tranche d'âge, revenu mensuel de chaque parent,
   coût mensuel global (optionnel — forfait sinon), nuits d'hébergement par parent,
   allocations familiales, frais extraordinaires, et clique « Calculer ».
3. Le composant POST la requête, affiche le verdict (3 niveaux), le montant mensuel net de
   contribution, le détail du calcul Renard (coût retenu, quotes-parts, parts hébergement),
   les frais extraordinaires répartis, les bases juridiques et les messages.
4. Au rechargement, restitution du dernier résultat (GET) ; « Modifier » ré-ouvre le
   formulaire pré-rempli.

### Cas d'erreur
- Workspace `FRANCE` → bannière info « Outil Belgique uniquement », pas de formulaire,
  aucun appel réseau.
- Erreur HTTP backend → `MatSnackBar` rouge, formulaire éditable.
- `GET` 404 → mode formulaire, pas d'erreur.

---

## Contrat API consommé (importé de SF-217-06 — FIGÉ)
- `POST /api/v1/case-files/{caseFileId}/contribution-alimentaire-enfants-be`
- `GET /api/v1/case-files/{caseFileId}/contribution-alimentaire-enfants-be`
- DTO `ContributionAlimentaireEnfantsBeRequest` / `…Response`, enums `verdict`,
  `trancheAgeEnfants`, `parentDebiteur` — cf. SF-217-06.

---

## Conformité F-IA-04
- [x] Entrée `TOOL_REGISTRY` avec `displayLabel` humain
  (`Contribution alimentaire des enfants (Belgique)`).
- [x] Entrée `THEME_BY_TOOL` (thème `INDEMNITES` — l'outil chiffre une créance alimentaire).
- [x] `static getPrefillCount` + helper `*-prefill-rules.ts` étiqueté
  `PREFILL_COUNT_ALWAYS_ZERO = true`.
- [x] Seed `decision_tool_visibility_rules` : `tool_id = contribution-alimentaire-enfants-be`,
  `ALWAYS_ON`, `DROIT_FAMILLE` / `BELGIQUE`, priority 70 — couplé à l'entrée TOOL_REGISTRY.

---

## Critères d'acceptation
- [ ] La section apparaît sur un dossier Famille BE, est absente sur un workspace FR.
- [ ] Le formulaire couvre tous les champs du contrat ; le POST envoie le bon body.
- [ ] Le verdict (3 niveaux) et le montant mensuel net s'affichent ; le détail du calcul
      Renard est lisible.
- [ ] « Modifier » ré-ouvre le formulaire pré-rempli avec le snapshot.
- [ ] `getPrefillCount({}) === 0` ; test d'intégrité prefill passe.
- [ ] `DecisionToolVisibilityIntegrityIT` + `DecisionToolDisplayLabelIntegrityIT` verts.
- [ ] Tests Jest du composant verts.

---

## Périmètre
### Hors scope
- Backend (SF-217-06).
- Pré-fill IA (`PREFILL_COUNT_ALWAYS_ZERO`).
- Validation F-IA-03 croisée (pas de champ croisable V1).

---

## Technique
### Composants / fichiers
| Fichier | Opération |
|---------|-----------|
| `frontend/src/app/case-files/contribution-alimentaire-enfants-be-section/…component.ts` | CREATE (standalone) |
| `…component.html` / `.scss` / `.spec.ts` | CREATE |
| `…-prefill-rules.ts` / `.spec.ts` | CREATE |
| `frontend/src/app/core/models/contribution-alimentaire-enfants-be.model.ts` | CREATE |
| `frontend/src/app/core/services/contribution-alimentaire-enfants-be.service.ts` | CREATE |
| `decisional-tools-panel.component.ts` | EDIT — import + TOOL_REGISTRY + THEME_BY_TOOL |

### Migration Liquibase
- [x] Oui — `240-seed-f217-vague2-contribution-alimentaire-visibility.xml`.

---

## Plan de test
### Tests unitaires (Jest)
- [ ] Statics exposés ; `getPrefillCount({})` = 0 ; helper aligné.
- [ ] Gate `workspaceCountry=FRANCE` → bannière info, aucun GET.
- [ ] Rendu nominal BE → titre + champs affichés.
- [ ] `calculate()` POST le body et bascule en mode résultat.

### Isolation workspace
- [x] Couvert backend ; le frontend gate l'affichage par `workspaceCountry`.

---

## Analyse d'impact
### Préoccupations transversales
- [x] **Outil décisionnel métier** — nouvelle entrée TOOL_REGISTRY + seed visibilité.
- [x] **Navigation / routing** — non modifié (section additive).

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- SF-217-06 (backend) — merge backend avant frontend.

---

## Notes et décisions
- Wrapper complet — pattern `regime-communaute-legale-be-section` (F-217 Vague 1).
- Mode `ALWAYS_ON` : la contribution alimentaire est une question systématique d'un
  dossier Famille BE avec enfant.
- Le résultat est présenté comme une « estimation indicative selon la méthode Renard » —
  méthode jurisprudentielle non codifiée (cf. SF-217-06).
</content>
