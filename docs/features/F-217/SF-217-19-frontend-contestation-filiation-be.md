# Mini-spec — F-217 / SF-217-19 — Frontend : section contestation de filiation (Belgique)

## Identifiant
`F-217 / SF-217-19`

## Feature parente
`F-217` — P2 Famille BE — Vague 3 — Successions / protection / international

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-19-contestation-filiation-be`

---

## Objectif

Livrer la section Angular de l'outil décisionnel « Contestation de filiation
(Belgique) » dans le panel décisionnel, branchée sur l'API figée par SF-217-18, avec
son entrée `TOOL_REGISTRY` et le seed `decision_tool_visibility_rules`.

---

## Comportement attendu

### Cas nominal
1. Sur un dossier de droit de la famille belge, la section
   `app-contestation-filiation-be-section` apparaît dans le panel décisionnel en mode
   **`CATALOG`** (visibilité gérée par F-IA-04 — outil contextuel, pas systématique
   sur tout dossier famille). L'avocat peut l'ajouter via le catalogue F-238.
2. L'avocat saisit les éléments (select `natureActionFiliation`, select
   `qualiteDemandeur`, `<input type="date">` `dateNaissanceEnfant`, `<input type="date">`
   `dateConnaissanceFaitContestation`, toggle `possessionEtatConforme` + input nombre
   `dureePossessionEtatAnnees` conditionnel, toggles `expertiseAdnDisponible` /
   `demandeExpertiseAdnEnvisagee`, textarea `commentaire`) et clique « Calculer ».
3. Le composant POST la requête, affiche le verdict en bandeau (6 niveaux), la date
   limite + jours restants + statut coloré, la voie procédurale, la liste des motifs
   d'irrecevabilité (rouge `HIGH`), les actions concrètes, les bases juridiques et
   les messages.
4. Au rechargement, la section restitue le dernier résultat (GET) ; « Modifier »
   ré-ouvre le formulaire pré-rempli avec le snapshot d'inputs.

### Cas d'erreur
- Workspace `FRANCE` → bannière info « Outil Belgique uniquement », pas de formulaire,
  aucun appel réseau (gate `workspaceCountry` strict).
- Erreur HTTP backend → `MatSnackBar` rouge, le formulaire reste éditable.
- `GET` 404 (jamais calculé) → mode formulaire, pas d'erreur visible.
- Soumission de `MATERNITE` → backend retourne `400` → message clair « Contestation
  de maternité hors scope V1 ».

---

## Contrat API consommé (importé de SF-217-18 — FIGÉ)
- `POST /api/v1/case-files/{caseFileId}/contestation-filiation-be`
- `GET /api/v1/case-files/{caseFileId}/contestation-filiation-be`
- DTO `ContestationFiliationBeRequest` / `ContestationFiliationBeResponse`, enums
  `verdict`, `natureActionFiliation`, `qualiteDemandeur`, `voieProcedurale`,
  `delaiStatut`, `code` de motif — cf. SF-217-18.

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils décisionnels filiation BE | Oui | Aucun outil BE équivalent (F-FA-18-* sont FR-only). Distinct de `reconnaissance-paternelle-be` (reporté F-223) et `recherche-paternite-be` (reporté F-223) — un outil = une situation. |
| Pré-fill IA | Oui | Aucun flag pivot V1 → `getPrefillCount()` = 0, `PREFILL_COUNT_ALWAYS_ZERO = true`. Pas de nouveau champ `FamilleExtractedData`. |
| F-IA-03 cohérence | Oui | Câblage présent ; `dateNaissanceEnfant` croisable potentiellement avec la liste d'enfants du dossier (V1 = pas de source IA stable). |
| Refresh dashboard F-IA-02 | Oui | `triggerRefresh()` dans le `next:` du POST. |
| Gate `workspaceCountry` | Oui | Bannière info si workspace FR. |

### Décision
- [x] Étendu à toutes les cibles applicables.

---

## Conformité F-IA-04
- [x] Entrée `TOOL_REGISTRY` ajoutée dans `decisional-tools-panel.component.ts` avec
  `displayLabel` humain (`Contestation de filiation (Belgique)`) — garde-fou
  `DecisionToolDisplayLabelIntegrityIT`.
- [x] Entrée `THEME_BY_TOOL` (thème `DELAIS` — outil à délais critique, 1 an de
  forclusion + 5 ans de blocage par possession d'état).
- [x] `static getPrefillCount` + helper co-localisé `*-prefill-rules.ts` étiqueté
  `PREFILL_COUNT_ALWAYS_ZERO = true`.
- [x] Seed `decision_tool_visibility_rules` : `tool_id = contestation-filiation-be`,
  **`CATALOG`**, `DROIT_FAMILLE` / `BELGIQUE`, priority 50 — outil contextuel (cf.
  `SF-217-00-coherence.md` ajustement n° 1 : « `CATALOG` pour les situations
  contextuelles sans flag IA extractible en V1 »).
- [x] Palette : vert pour `ACTION_RECEVABLE`, orange pour
  `ACTION_RECEVABLE_DELAI_CRITIQUE`, rouge réservé aux verdicts d'irrecevabilité
  (`IRRECEVABLE_*`). `QUALIFICATION_INCOMPLETE` = navy info. `delaiStatut` coloré
  (vert / orange / rouge).
- [x] `<input type="date">` pour `dateNaissanceEnfant` et
  `dateConnaissanceFaitContestation`.
- [x] OnPush + signals + `markForCheck()` dans `next:` / `error:` (cf.
  `feedback_onpush_subscribe_markforcheck`).

### Parité des domaines (niveau ≥ 5)
- Niveau : **5** (analyse de recevabilité + délais critiques + motifs
  d'irrecevabilité).
- Famille = F-217 (BE-only par construction). Droit du travail / immigration non
  pertinents.

---

## Critères d'acceptation
- [ ] La section apparaît dans le catalogue F-238 sur un dossier Famille BE, est
      absente sur un workspace FR.
- [ ] Une fois ajoutée au dossier (via F-238), la section est visible et utilisable.
- [ ] Le formulaire couvre tous les champs du contrat ; le POST envoie le bon body.
- [ ] Le verdict (6 niveaux) s'affiche ; rouge réservé aux verdicts
      d'irrecevabilité.
- [ ] Date limite + jours restants + statut affichés ; couleur du `delaiStatut`
      cohérente.
- [ ] Liste `motifsIrrecevabilite` rendue avec code + libellé (rouge si présente).
- [ ] Liste `actionsConcretes` rendue en bullet points.
- [ ] Soumission `natureActionFiliation = MATERNITE` → message clair retourné par
      le backend, affiché en `MatSnackBar`.
- [ ] `dureePossessionEtatAnnees` affiché uniquement si `possessionEtatConforme = true`.
- [ ] « Modifier » ré-ouvre le formulaire pré-rempli avec le snapshot.
- [ ] `getPrefillCount({}) === 0` ; le test d'intégrité prefill passe.
- [ ] `DecisionToolVisibilityIntegrityIT` + `DecisionToolDisplayLabelIntegrityIT` verts.
- [ ] Tests Jest du composant verts.

---

## Périmètre
### Hors scope
- Backend (SF-217-18).
- Pré-fill IA (V1 = 0 champ).
- Contestation de **maternité** (hors scope V1 backend également).
- Action en recherche de paternité / reconnaissance — outils distincts (reportés
  F-223).
- Génération de la requête au TF — outil dédié potentiel, reporté.
- Visibilité `CONTEXTUAL` automatique sur détection d'un litige de filiation —
  reporté (flag `presomption_paternite_litige_be` pipeline V2 mentionné audit F-191).

---

## Technique
### Composants / fichiers
| Fichier | Opération |
|---------|-----------|
| `frontend/src/app/case-files/contestation-filiation-be-section/contestation-filiation-be-section.component.ts` | CREATE (standalone) |
| `…/contestation-filiation-be-section.component.html` / `.scss` | CREATE |
| `…/contestation-filiation-be-section.component.spec.ts` | CREATE |
| `…/contestation-filiation-be-section-prefill-rules.ts` / `.spec.ts` | CREATE |
| `frontend/src/app/core/models/contestation-filiation-be.model.ts` | CREATE |
| `frontend/src/app/core/services/contestation-filiation-be.service.ts` | CREATE |
| `decisional-tools-panel.component.ts` | EDIT — import + entrée TOOL_REGISTRY + THEME_BY_TOOL |

### Migration Liquibase
- [x] Oui — `279-seed-f217-vague3-contestation-filiation-visibility.xml`
  (seed `decision_tool_visibility_rules`). Numéro `279` = prochain libre après `278`
  (SF-217-18). UUID dédié : `f1a04003-0000-0000-0000-eeee21703XXX`.

---

## Plan de test
### Tests unitaires (Jest)
- [ ] Statics `TOOL_LABEL` / `TOOL_ICON` / `getPrefillCount` exposés.
- [ ] `getPrefillCount({})` = 0.
- [ ] Gate `workspaceCountry=FRANCE` → bannière info, aucun GET.
- [ ] Rendu nominal BE → titre + sélecteurs + toggles affichés.
- [ ] `calculate()` POST le body et bascule en mode résultat.
- [ ] Affichage du `delaiStatut` avec couleur (`OK` vert, `CRITIQUE` orange,
      `DEPASSE` rouge).
- [ ] Bloc `dureePossessionEtatAnnees` affiché uniquement si
      `possessionEtatConforme = true`.
- [ ] Soumission `MATERNITE` → `MatSnackBar` avec message backend.
- [ ] Affichage de la liste `motifsIrrecevabilite` quand verdict d'irrecevabilité.

### Isolation workspace
- [x] Couvert backend (SF-217-18) ; le frontend gate l'affichage par
  `workspaceCountry`.

---

## Analyse d'impact
### Préoccupations transversales
- [x] **Outil décisionnel métier** — nouvelle entrée TOOL_REGISTRY + seed visibilité.
  Scan : un outil = une situation (cf. SF-217-18). Mode `CATALOG` choisi pour outil
  contextuel.
- [x] **Navigation / routing** — non modifié (section additive dans un panel existant,
  ajout via catalogue F-238).

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- SF-217-18 (backend) — contrat API figé ; merge backend avant frontend.

---

## Notes et décisions
- Wrapper complet (formulaire + calcul + restitution) — pattern `divorce-dc-be-section`
  / `autorite-parentale-be-section`.
- Mode **`CATALOG`** (et non `ALWAYS_ON`) : la contestation de filiation n'est pas
  systématique sur tout dossier famille BE — elle est contextuelle (typiquement
  présente sur 5-10 % des dossiers, cas spécialisés). L'avocat l'ajoute via F-238. Pas
  de flag IA extractible en V1 → pas de mode `CONTEXTUAL`. Cohérent avec
  `SF-217-00-coherence.md` ajustement n° 1.
- Thème `DELAIS` choisi (et non `VALIDITE`) : la dimension délai (1 an de
  forclusion + 5 ans de blocage par possession d'état) est centrale dans l'analyse —
  c'est ce qui rend l'outil utile à l'avocat (ne pas perdre l'action). Cohérent avec
  `liquidation-partage-be` (`DELAIS`) et `succession-be-acceptation-renonciation`
  (`DELAIS`).
- Le champ `dureePossessionEtatAnnees` est affiché conditionnellement via un signal
  computed sur `possessionEtatConforme()` — évite de polluer le formulaire dans le
  cas standard.
