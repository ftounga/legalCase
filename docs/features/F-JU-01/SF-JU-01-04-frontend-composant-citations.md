# Mini-spec — F-JU-01 / SF-JU-01-04 Frontend composant citations + bouton signaler

## Identifiant
`F-JU-01 / SF-JU-01-04`

## Feature parente
`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels

## Statut
`draft`

## Date de création
2026-05-22

## Branche Git
`feat/SF-JU-01-04-frontend-citations`

---

## Objectif

Livrer le composant Angular standalone `<app-tool-jurisprudence-citations>` qui consomme `GET /api/v1/tools/{toolId}/jurisprudence-citations?branch={brancheId}`, affiche 0 à 3 arrêts (chapeau officiel + lien Légifrance + bouton « Signaler un problème ») sous le résultat d'un outil décisionnel ouvert en modal F-177, ainsi que l'endpoint backend `POST /signal` qui crée un `JurisprudenceWatchFlag` source `USER_SIGNAL`.

---

## Comportement attendu

### Cas nominal

**Frontend** :
1. Le composant `<app-tool-jurisprudence-citations [toolId] [branchActive]>` est inséré sous le bloc résultat d'un composant outil décisionnel (instrumentation incrémentale outil par outil — V1 livre le composant, l'intégration dans chaque outil = vague suivante).
2. Sur `ngOnInit` et sur chaque changement de `branchActive` (via `ngOnChanges`), le composant appelle le service Angular `ToolJurisprudenceClientService.findByToolAndBranch(toolId, brancheId)`.
3. Si liste vide → le bloc est **absent du DOM** (silence > placeholder).
4. Sinon → affiche 1 à 3 cartes compactes (référence + juridiction + chapeau officiel 1-2 phrases + lien Légifrance `target="_blank"` + bouton « ⚠ Signaler »).
5. Mention de prudence en bas du bloc : « Citation indicative — dernière vérification : DD/MM/YYYY. L'avocat reste seul juge de l'applicabilité. »
6. Bouton « Signaler » → prompt inline minimaliste (champ texte optionnel + envoyer) → appel `POST /api/v1/tools/{toolId}/jurisprudence-citations/{citationId}/signal {comment?: string}` → snackbar de confirmation.

**Backend** :
1. Nouvel endpoint `POST /api/v1/tools/{toolId}/jurisprudence-citations/{citationId}/signal` (auth MEMBER) qui :
   - Vérifie que le mapping existe (404 sinon)
   - Crée un `JurisprudenceWatchFlag` avec source `USER_SIGNAL`, statut `PENDING`, `arret_entrant_ref` = ref du mapping signalé, `mapping_actuel` = mapping, `comment_user` = commentaire optionnel
   - Retourne 201

### Cas d'erreur (backend)

| Situation | Code |
|---|---|
| `citationId` non UUID | 400 |
| `citationId` inexistant ou archivé | 404 |
| Utilisateur non authentifié | 401 |
| `comment` > 2000 caractères | 400 (validation) |

---

## Analyse de cohérence transversale
- [x] **Composant partagé** : `<app-tool-jurisprudence-citations>` réutilisable par les ~80 outils décisionnels éligibles
- [x] **Endpoint nouveau** : `POST .../signal` réutilisable par tout signalement futur (pattern unique)
- [x] **Préoccupations transversales** : aucune (lecture publique, écriture authentifiée, pas de modif auth/workspace/plans/navigation)

## Conformité F-IA-04
- [x] **Non applicable** — le composant est un **enrichissement** affiché DANS un composant outil décisionnel, pas un nouvel outil. Pas d'entrée `TOOL_REGISTRY`, pas de pré-fill IA propre, pas de F-IA-03 (pas de saisie utilisateur impactant la décision juridique).

## Champs IA à extraire
- [x] **Aucun pré-remplissage**.

---

## Critères d'acceptation

- [ ] **CA-01** — Composant standalone `ToolJurisprudenceCitationsComponent` sélecteur `app-tool-jurisprudence-citations`, inputs `[toolId]` (required) et `[branchActive]` (optionnel — si absent, bloc invisible).
- [ ] **CA-02** — Service Angular `ToolJurisprudenceClientService` injectable provided in root, méthode `findByToolAndBranch(toolId, brancheId): Observable<ToolJurisprudenceCitation[]>` et `signalProblem(toolId, citationId, comment?): Observable<void>`.
- [ ] **CA-03** — Modèle TS `ToolJurisprudenceCitation { id, arretRef, juridiction, dateArret, numeroPourvoi, lienLegifrance, chapeauOfficiel, lastVerifiedAt, confidenceScore }`.
- [ ] **CA-04** — Bloc citations absent du DOM si liste vide (silence > placeholder).
- [ ] **CA-05** — Lien Légifrance `target="_blank" rel="noopener noreferrer"` systématique.
- [ ] **CA-06** — Mention de prudence visible avec date de dernière vérification (la plus récente parmi les 3 arrêts).
- [ ] **CA-07** — Bouton « Signaler » avec prompt inline minimaliste (pas de MatDialog imbriqué) + snackbar de confirmation.
- [ ] **CA-08** — Endpoint backend `POST .../signal` crée un `JurisprudenceWatchFlag` source `USER_SIGNAL`.
- [ ] **CA-09** — Tests Jest sur le composant : (a) liste vide → bloc absent, (b) 3 arrêts → 3 cards, (c) clic Signaler → service appelé, (d) input branchActive change → re-fetch.
- [ ] **CA-10** — Tests UT + IT backend : POST signal 201 nominal, 404 sur mapping inexistant, 401 sans auth.

---

## Périmètre

### Hors scope
- ❌ Intégration dans les ~80 composants outils décisionnels existants — différée à SF-JU-01-05 (au moment du bootstrap mappings) ou à un effort d'instrumentation ultérieur outil-par-outil
- ❌ Bouton F-241 « Ouvrir dans Doctrine » par arrêt → V2 selon signal terrain
- ❌ Continuité F-98 (citations dans conclusions générées) → V2

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth |
|---|---|---|
| GET | `/api/v1/tools/{toolId}/jurisprudence-citations?branch={brancheId}` | MEMBER (déjà livré SF-01) |
| **POST** | **`/api/v1/tools/{toolId}/jurisprudence-citations/{citationId}/signal`** | **MEMBER (nouveau, SF-04)** |

### Tables impactées
- `jurisprudence_watch_flags` (INSERT source=USER_SIGNAL)

### Migration Liquibase
- [x] Non applicable (tables existantes).

### Composants Angular
- `ToolJurisprudenceCitationsComponent` (standalone)
- `ToolJurisprudenceClientService` (injectable root)

### Classes Java introduites
- `JurisprudenceSignalRequest` (record DTO)
- Méthode `ToolJurisprudenceService.signalProblem(toolId, citationId, comment, principal)` + nouvelle méthode contrôleur POST

---

## Plan de test

### Tests Jest (frontend)
- `tool-jurisprudence-citations.component.spec.ts`
  - liste vide → bloc absent
  - 3 arrêts → 3 cards
  - mention prudence avec date max
  - bouton Signaler appelle service
  - changement de branchActive → re-fetch

### Tests UT/IT (backend)
- `ToolJurisprudenceServiceTest.signalProblem_createsFlagWithUserSignalSource`
- `ToolJurisprudenceControllerIT.postSignal_returns201_andCreatesFlag`
- `ToolJurisprudenceControllerIT.postSignal_returns404_whenCitationNotFound`
- `ToolJurisprudenceControllerIT.postSignal_returns401_whenNotAuthenticated`

### Isolation workspace
- [x] Non applicable (tables globales).

---

## Analyse d'impact
- [x] Aucune préoccupation transversale touchée
- [x] Aucun smoke test E2E concerné

---

## Notes et décisions

1. **Pas d'intégration dans les ~80 composants outils en SF-04** — coût d'instrumentation important. Le composant est livré standalone et sera intégré incrémentalement.
2. **Bloc absent du DOM si vide** — pas de placeholder « Aucune citation » qui pollue les outils non bootstrappés.
3. **Mention prudence avec date max** — si 3 arrêts ont des `lastVerifiedAt` différents, on affiche la plus récente (rassurant pour l'avocat).
4. **Pas de MatDialog imbriqué pour le bouton Signaler** — invariant cadrage écran (modal dans modal = surcharge). Prompt inline avec champ texte optionnel.

### Coût estimé
- ~1,5 j dev (composant + service + endpoint + tests).
