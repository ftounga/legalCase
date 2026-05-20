# Mini-spec — F-217 / SF-217-15 — Frontend : section protection du majeur (Belgique)

## Identifiant
`F-217 / SF-217-15`

## Feature parente
`F-217` — P2 Famille BE — Vague 3 — Successions / protection / international

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-15-protection-majeur-be`

---

## Objectif

Livrer la section Angular de l'outil décisionnel « Protection du majeur (Belgique) »
dans le panel décisionnel, branchée sur l'API figée par SF-217-14, avec son entrée
`TOOL_REGISTRY` et le seed `decision_tool_visibility_rules`.

---

## Comportement attendu

### Cas nominal
1. Sur un dossier de droit de la famille belge, la section
   `app-protection-majeur-be-section` apparaît dans le panel décisionnel (mode
   `ALWAYS_ON`, comme les outils Famille BE de F-211 et des Vagues 1+2).
2. L'avocat saisit les éléments (select `natureAlteration`, select `graviteIncapacite`,
   toggle `mandatExtraJudiciaireSigne` + date conditionnelle, toggle
   `declarationAnticipeeExiste`, toggle `environnementFamilialProtecteur`, select
   `niveauUrgence`, select `modeSaisineEnvisage`, textarea `commentaire`) et clique
   « Calculer ».
3. Le composant POST la requête, affiche le verdict en bandeau (4 niveaux), la mesure
   recommandée, la juridiction compétente, la liste des actes protégés avec leur
   nécessité, les actions concrètes à poser, les bases juridiques et les messages.
4. Au rechargement, la section restitue le dernier résultat (GET) ; « Modifier »
   ré-ouvre le formulaire pré-rempli avec le snapshot d'inputs.

### Cas d'erreur
- Workspace `FRANCE` → bannière info « Outil Belgique uniquement », pas de formulaire,
  aucun appel réseau (gate `workspaceCountry` strict).
- Erreur HTTP backend → `MatSnackBar` rouge, le formulaire reste éditable.
- `GET` 404 (jamais calculé) → mode formulaire, pas d'erreur visible.

---

## Contrat API consommé (importé de SF-217-14 — FIGÉ)
- `POST /api/v1/case-files/{caseFileId}/protection-majeur-be`
- `GET /api/v1/case-files/{caseFileId}/protection-majeur-be`
- DTO `ProtectionMajeurBeRequest` / `ProtectionMajeurBeResponse`, enums `verdict`,
  `mesureRecommandee`, `juridictionCompetente`, `natureAlteration`,
  `graviteIncapacite`, `niveauUrgence`, `modeSaisineEnvisage`, `code` d'acte protégé,
  `necessite` — cf. SF-217-14.

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils décisionnels protection majeur BE | Oui | Aucun outil BE équivalent (F-FA-25 FR-only — régimes tutelle/curatelle FR structurellement distincts du statut unique loi 17/03/2013). Un outil = une situation. |
| Pré-fill IA | Oui | Aucun flag pivot V1 → `getPrefillCount()` = 0, `PREFILL_COUNT_ALWAYS_ZERO = true`. Pas de nouveau champ `FamilleExtractedData`. |
| F-IA-03 cohérence | Oui | Câblage `coherenceAlerts` présent mais aucun champ croisable en V1 (situations qualifiées à l'audience par l'avocat). |
| Refresh dashboard F-IA-02 | Oui | `triggerRefresh()` dans le `next:` du POST. |
| Gate `workspaceCountry` | Oui | Bannière info si workspace FR. |

### Décision
- [x] Étendu à toutes les cibles applicables.

---

## Conformité F-IA-04
- [x] Entrée `TOOL_REGISTRY` ajoutée dans `decisional-tools-panel.component.ts` avec
  `displayLabel` humain (`Protection du majeur (Belgique)`) — garde-fou
  `DecisionToolDisplayLabelIntegrityIT`.
- [x] Entrée `THEME_BY_TOOL` (thème `VALIDITE` — outil d'orientation /
  qualification, pas un outil à délais critique malgré l'urgence potentielle qui est
  qualifiée à l'audience).
- [x] `static getPrefillCount` + helper co-localisé `*-prefill-rules.ts` étiqueté
  `PREFILL_COUNT_ALWAYS_ZERO = true` (aucun flag pivot IA en V1) — garde-fous
  `prefill-count-integrity.spec.ts` (présence + parité).
- [x] Seed `decision_tool_visibility_rules` : `tool_id = protection-majeur-be`,
  `ALWAYS_ON`, `DROIT_FAMILLE` / `BELGIQUE`, priority 70 — couplé à l'entrée
  TOOL_REGISTRY dans le même lot (garde-fou `DecisionToolVisibilityIntegrityIT`).
- [x] Palette navy/or pour info, vert pour `MANDAT_EXTRA_JUDICIAIRE_VALABLE`, rouge
  réservé à `URGENCE_MESURE_PROVISOIRE` (situation critique). `QUALIFICATION_INCOMPLETE`
  = navy + icône info.
- [x] `<input type="date">` pour `mandatExtraJudiciaireDateSignature`.
- [x] OnPush + signals + `markForCheck()` dans le `next:` / `error:` (cf.
  `feedback_onpush_subscribe_markforcheck`).

### Parité des domaines (niveau ≥ 5)
- Niveau : **5** (arbre décisionnel + qualification de la mesure adéquate).
- Famille = F-217 (BE-only par construction). Droit du travail / immigration non
  pertinents.

---

## Critères d'acceptation
- [ ] La section apparaît sur un dossier Famille BE, est absente sur un workspace FR.
- [ ] Le formulaire couvre tous les champs du contrat ; le POST envoie le bon body.
- [ ] Le verdict (4 niveaux) et la mesure recommandée s'affichent ; rouge réservé à
      `URGENCE_MESURE_PROVISOIRE`.
- [ ] La liste `actesProteges` est rendue avec le code, le libellé et la nécessité
      colorée (`AUTORISATION_JP_PREALABLE` = orange, `ADMINISTRATEUR_SEUL` = navy,
      `AUTONOMIE_PRESERVEE` = vert, `INTERDICTION_ABSOLUE` = rouge).
- [ ] Les actions concrètes et bases juridiques s'affichent dans des blocs distincts.
- [ ] « Modifier » ré-ouvre le formulaire pré-rempli avec le snapshot.
- [ ] `getPrefillCount({}) === 0` ; le test d'intégrité prefill passe.
- [ ] `DecisionToolVisibilityIntegrityIT` + `DecisionToolDisplayLabelIntegrityIT` verts.
- [ ] Tests Jest du composant verts (statics, gate pays, rendu, calculate).

---

## Périmètre
### Hors scope
- Backend (SF-217-14).
- Pré-fill IA (V1 = 0 champ — `PREFILL_COUNT_ALWAYS_ZERO`).
- Validation F-IA-03 croisée effective (pas de champ croisable avec F-96 / questions IA
  en V1).
- Génération de la requête à la Justice de paix — outil dédié potentiel, reporté.

---

## Technique
### Composants / fichiers
| Fichier | Opération |
|---------|-----------|
| `frontend/src/app/case-files/protection-majeur-be-section/protection-majeur-be-section.component.ts` | CREATE (standalone) |
| `…/protection-majeur-be-section.component.html` / `.scss` | CREATE |
| `…/protection-majeur-be-section.component.spec.ts` | CREATE |
| `…/protection-majeur-be-section-prefill-rules.ts` / `.spec.ts` | CREATE |
| `frontend/src/app/core/models/protection-majeur-be.model.ts` | CREATE |
| `frontend/src/app/core/services/protection-majeur-be.service.ts` | CREATE |
| `decisional-tools-panel.component.ts` | EDIT — import + entrée TOOL_REGISTRY + THEME_BY_TOOL |

### Migration Liquibase
- [x] Oui — `275-seed-f217-vague3-protection-majeur-visibility.xml`
  (seed `decision_tool_visibility_rules`). Numéro `275` = prochain libre après `274`
  (SF-217-14). À renuméroter si conflit au merge. UUID dédié :
  `f1a04003-0000-0000-0000-eeee21703XXX`.

---

## Plan de test
### Tests unitaires (Jest)
- [ ] Statics `TOOL_LABEL` / `TOOL_ICON` / `getPrefillCount` exposés.
- [ ] `getPrefillCount({})` = 0 ; helper `*PrefillRules` aligné.
- [ ] Gate `workspaceCountry=FRANCE` → bannière info, aucun GET.
- [ ] Rendu nominal BE → titre + selects/toggles affichés.
- [ ] `calculate()` POST le body et bascule en mode résultat.
- [ ] Affichage `actesProteges` : tri par code + couleur par nécessité.
- [ ] Date de signature du mandat affichée conditionnellement
      (`mandatExtraJudiciaireSigne = true`).

### Isolation workspace
- [x] Couvert backend (SF-217-14) ; le frontend gate l'affichage par
  `workspaceCountry`.

---

## Analyse d'impact
### Préoccupations transversales
- [x] **Outil décisionnel métier** — nouvelle entrée TOOL_REGISTRY + seed visibilité.
  Scan : un outil = une situation (cf. SF-217-14).
- [x] **Navigation / routing** — non modifié (section additive dans un panel existant).

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- SF-217-14 (backend) — contrat API figé ; merge backend avant frontend.

---

## Notes et décisions
- Wrapper complet (formulaire + calcul + restitution) — pattern `divorce-dc-be-section`
  (F-211) / `autorite-parentale-be-section` (F-217 Vague 2).
- Mode `ALWAYS_ON` : la question de la protection du majeur peut se poser sur tout
  dossier famille BE comportant un client âgé / vulnérable — situation toujours
  pertinente, pas à détecter (cf. `SF-217-00-coherence.md` ajustement n° 1).
  Alternative `CATALOG` envisagée et écartée : le contexte d'un dossier famille BE
  rend la question recevable sans flag IA.
- Couleurs de la liste `actesProteges` : `AUTORISATION_JP_PREALABLE` = orange,
  `ADMINISTRATEUR_SEUL` = navy, `AUTONOMIE_PRESERVEE` = vert,
  `INTERDICTION_ABSOLUE` = rouge — décision UI documentée ici.
