# Mini-spec — F-JU-02 / SF-JU-02-02 Section « Jurisprudence applicable » dans le PDF synthèse

## Identifiant
`F-JU-02 / SF-02`

## Feature parente
`F-JU-02` — Citations jurisprudentielles automatiques dans les conclusions générées (pont F-JU-01 → F-98)

## Statut
`draft` → `ready` après validation user.

## Date de création
2026-05-24

## Branche Git
`feat/SF-JU-02-02-pdf-synthese-jurisprudence`

---

## Objectif

Insérer une section dédiée « 📚 Jurisprudence applicable » dans l'**export PDF synthèse** du dossier (généré côté frontend par `pdf-export.service.ts`), en réutilisant le service backend `ConclusionsJurisprudenceContext` déjà livré par SF-JU-02-01 — pour que l'avocat voie les arrêts mappés des outils utilisés sans devoir ouvrir les conclusions générées.

---

## Cycle de gouvernance — étapes 0 et 0 bis

- **Étape 0 cohérence** : ✅ couverte par le cadrage F-JU-02 dans `docs/PRODUCT_SPEC.md` (note backlog détaillée 2026-05-22). SF-02 est listée explicitement comme « SF-JU-02-02 export PDF synthèse — section dédiée par outil, miroir pattern existant ».
- **Étape 0 bis cohérence écran** : **N/A** — modification d'un export PDF backend-driven, pas d'écran utilisateur nouveau ni déplacé. Le PDF reste consulté depuis le bouton existant de la page synthèse, son rendu hors-app n'est pas un écran au sens de la skill.

---

## Comportement attendu

### Cas nominal

1. Avocat ouvre `/case-files/{id}/synthesis` puis clique « Exporter en PDF » (bouton existant).
2. **Frontend** : `pdf-export.service.ts` appelle le nouvel endpoint `GET /api/v1/case-files/{id}/jurisprudence-applicable` en parallèle de la récupération de la synthèse.
3. **Backend** : le controller vérifie l'appartenance du dossier au workspace courant, puis appelle `ConclusionsJurisprudenceContext.collectForCaseFile(caseFileId)` (SF-01 déjà livrée). Retourne la liste agrégée groupée par outil avec arrêts dédupliqués par `arret_ref`.
4. **Frontend** : si la liste est non vide, insère une nouvelle section dans le PDF entre « Risques retenus » (F-195) et « Timeline » :
   - Titre « 📚 Jurisprudence applicable »
   - Sous-titre court : « Arrêts mappés des outils décisionnels utilisés sur ce dossier »
   - Pour chaque outil utilisé : libellé outil + branche active + liste d'arrêts (référence + portée)
5. Si la liste est vide : aucune section n'est ajoutée (pas de bloc vide qui surcharge le PDF).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|---|---|---|
| Dossier inexistant | Refus | 404 |
| Dossier d'un autre workspace | Refus | 403 |
| Aucun outil utilisé sur le dossier | Liste vide → pas de section PDF | 200 `[]` |
| Backend appel KO (5xx) | Le PDF est généré quand même, **sans** la section jurisprudence (fail-open — invariant : l'export PDF ne doit jamais échouer pour cette section optionnelle) | n/a |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** — N/A (transversal, réutilise `ToolUsageAggregator` qui couvre tous les outils instrumentés par F-JU-03)
- [x] **Autres pays** — N/A (transversal FR + BE via les outils instrumentés)
- [x] **Autres domaines** — N/A (transversal 3 domaines)
- [x] **Autres flows transversaux** — Export PDF synthèse : pattern existant SF-192-03 (Stratégies retenues), SF-195-03 (Risques retenus) — section additionnelle pure, pas de modification des sections existantes

### Décision

- [x] Étendu à toutes les cibles applicables dans cette SF
- [x] Section additionnelle pure, pas de SF parallèle nécessaire

---

## Conformité F-IA-04

- [x] **Non applicable** — SF transverse export PDF, pas un composant frontend décisionnel.

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — SF d'extension PDF, pas d'outil décisionnel à champs saisissables.

---

## Critères d'acceptation

- [ ] **C1** — `GET /api/v1/case-files/{id}/jurisprudence-applicable` retourne `200` avec `JurisprudenceApplicableResponse` (liste d'objets `{ toolId, brancheCalculId, citations: [{ arretRef, portee, ... }] }`), agrégé par outil utilisé sur le dossier, arrêts dédupliqués par `arretRef`.
- [ ] **C2** — Dossier inexistant → `404`.
- [ ] **C3** — Dossier d'un autre workspace → `403` (isolation workspace strict via pattern existant `caseFileSecurityChecker` ou équivalent).
- [ ] **C4** — Dossier sans aucun outil utilisé → `200` avec liste vide `[]` (pas d'erreur).
- [ ] **C5** — Backend : tests d'intégration sur les 4 cas ci-dessus (`IT` ou `@SpringBootTest`).
- [ ] **C6** — Frontend : `pdf-export.service.ts` consomme l'endpoint en parallèle de la synthèse (`forkJoin` ou équivalent — pas d'attente séquentielle qui ralentirait l'export).
- [ ] **C7** — Frontend : nouvelle méthode `private buildJurisprudenceApplicableSection(citationsByTool: JurisprudenceApplicableResponse): object` qui retourne un bloc pdfmake structuré (titre, sous-titre, liste par outil).
- [ ] **C8** — Insertion dans l'ordre attendu du PDF : entre `buildRisquesValidesSection` (F-195) et la timeline / sections suivantes.
- [ ] **C9** — Si liste vide → la section n'est PAS ajoutée au document (pas de bloc « Aucun arrêt trouvé »).
- [ ] **C10** — Si l'appel backend échoue (status ≥ 500 ou erreur réseau) → le PDF est généré sans la section, sans bloquer l'export (`catchError` retourne `[]`, comme si aucun arrêt n'existait).
- [ ] **C11** — Tests Jest : (a) bloc PDF correctement formé avec 1 outil + 2 arrêts, (b) bloc absent si liste vide, (c) bloc absent si erreur backend (fail-open).
- [ ] **C12** — Suite backend complète reste verte. Suite Jest frontend complète reste verte.

---

## Périmètre / Hors-scope

### Hors scope SF-02
- **Modification des conclusions générées (F-98)** : déjà couvert par SF-01.
- **Persistance « par dossier » des arrêts retenus / écartés** : V2 si besoin terrain.
- **Export Word natif** : SF-98-50/51 (F-98), pas le scope F-JU-02.
- **Endpoint frontend dédié pour afficher les arrêts dans l'écran synthèse** (en plus du PDF) : V2 selon retour user.

---

## Technique

### Endpoint nouveau

| Méthode | URL | Auth | Rôle minimum | Réponse |
|---|---|---|---|---|
| `GET` | `/api/v1/case-files/{caseFileId}/jurisprudence-applicable` | Oui (OAuth2) | MEMBER du workspace du dossier | `200 JurisprudenceApplicableResponse` ; `403` ; `404` |

### Service Java

- **Réutilise existant** : `ConclusionsJurisprudenceContext.collectForCaseFile(caseFileId)` (SF-01, déjà livré, retourne `List<ToolJurisprudenceCitationByTool>`)
- **Nouveau controller** : `CaseFileJurisprudenceApplicableController` sous `fr.ailegalcase.casefile.jurisprudence` (ou ajout dans le `CaseFileController` existant — à arbitrer dans le dev selon la convention du projet)
- **Nouveau DTO de réponse** : `JurisprudenceApplicableResponse(List<JurisprudenceApplicableEntry> entries)` avec `JurisprudenceApplicableEntry(toolId, brancheCalculId, citations)` — wrap léger pour exposer le record interne au frontend.

### Service Angular

- **Nouvelle méthode** dans `frontend/src/app/core/services/case-file.service.ts` (ou un nouveau `JurisprudenceApplicableService`) :
  ```ts
  getJurisprudenceApplicable(caseFileId: string): Observable<JurisprudenceApplicableResponse>
  ```
- **Modèle TS** : `JurisprudenceApplicableResponse` + `JurisprudenceApplicableEntry` + `ToolJurisprudenceCitation` (reflet strict du DTO Java).

### Modification `pdf-export.service.ts`

- **Nouvelle méthode privée** `buildJurisprudenceApplicableSection(response: JurisprudenceApplicableResponse): object | null` (pattern miroir `buildStrategiesRetenuesSection`, `buildRisquesValidesSection`).
- **Wiring** : la méthode principale d'export (probablement `exportSynthesisPdf` ou similaire) lance `forkJoin([getSynthesis(...), getJurisprudenceApplicable(...).pipe(catchError(() => of(emptyResponse)))])` puis injecte la section dans le `docDefinition` avant la timeline.
- **Tests Jest** : 3 cas (nominal, vide, erreur).

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| Aucune | N/A | Lecture seule via `ToolUsageAggregator` et `ToolJurisprudenceService` |

### Migration Liquibase

- [x] **Non applicable** — aucun changement de schéma.

### Composants Angular

- Aucun composant écran touché. Seuls le service de récupération + le service d'export PDF.

---

## Plan de test

### Tests unitaires backend (JUnit + Mockito)

- [ ] `CaseFileJurisprudenceApplicableControllerTest` : 200 nominal, 403 workspace différent, 404 dossier inexistant, 200 liste vide
- [ ] Vérifier que `ConclusionsJurisprudenceContext.collectForCaseFile` est bien invoqué une fois par appel

### Tests intégration backend (`@SpringBootTest`)

- [ ] `CaseFileJurisprudenceApplicableIT` : seed un dossier + 2 outils utilisés + mappings → vérif réponse JSON structure

### Tests Jest frontend

- [ ] `pdf-export.service.spec.ts` étendu :
  - Bloc présent et bien formé pour 1 outil + 2 arrêts
  - Bloc absent si liste vide
  - Bloc absent si appel KO (fail-open)
- [ ] `case-file.service.spec.ts` (ou nouveau service) : test `HttpTestingController` sur le nouveau endpoint

### Isolation workspace

- [x] **Applicable** — un utilisateur du workspace A ne peut pas récupérer les arrêts du workspace B. Test IT dédié : `403 FORBIDDEN_WORKSPACE` (ou pattern existant équivalent).

---

## Analyse d'impact

### Préoccupations transversales

- [x] Auth — Non, on consomme `OidcUser` standard.
- [x] Workspace context — Non, on consomme le pattern existant.
- [x] Plans / limites — Non, lecture pure pas de gating.
- [x] Navigation / routing frontend — Non, pas de nouvelle route ni guard.

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `ConclusionsJurisprudenceContext` (existant) | Aucune modification, simple lecture | Tests SF-01 existants restent verts |
| `pdf-export.service.ts` (existant) | Ajout d'une section optionnelle, pas de modification des sections existantes | Tests existants des autres sections restent verts |
| Backend route | Nouveau endpoint, aucune route existante modifiée | N/A |

### Smoke tests E2E

- [x] **Aucun smoke E2E concerné directement** — l'export PDF n'est pas dans les smokes E2E existants (auth/workspace/navigation). Suite Jest frontend complète + suite backend complète couvrent suffisamment.

---

## Dépendances

### Bloquantes

- ✅ SF-JU-02-01 mergée (PR #1244 + #1248) — `ConclusionsJurisprudenceContext` opérationnel
- ✅ F-JU-01 Terminée (5/5 SF) — `tool_jurisprudence_mappings` + `ToolJurisprudenceService.findByToolAndBranch` opérationnels
- ✅ F-JU-03 en cours (vagues 1 Travail/Immigration/Famille FR + BE livrées) — au moins certains outils instrumentés émettent leur usage, donc la liste retournée par l'endpoint sera non vide pour des dossiers concernés

### Démarrables en parallèle
- N/A, SF-02 est terminale du périmètre F-JU-02.

### Questions ouvertes impactées
- Aucune.

---

## Notes et décisions

- **Choix « endpoint dédié »** plutôt que d'enrichir un endpoint synthèse existant : isolation des responsabilités (la jurisprudence applicable est un domaine distinct), pas de risque de casser un consommateur existant, possibilité de cache HTTP indépendant si besoin V2.
- **Fail-open frontend** : si l'endpoint répond 500 ou timeout, le PDF doit quand même se générer (sans la section). C'est l'invariant n°1 : l'export ne doit jamais échouer pour cette section optionnelle.
- **Ordre dans le PDF** : entre « Risques retenus » (F-195) et « Timeline » — choix cohérent avec la logique métier (les risques contextualisent la jurisprudence applicable, la timeline est plus narrative).
- **Wording** : « 📚 Jurisprudence applicable » avec emoji livre (cohérent avec emojis utilisés dans les autres sections du PDF — 🎯 Stratégies, ⚠️ Risques, etc.).
- **Pas de modification du contrôleur F-98** (`CaseConclusionController`) — la consommation SF-01 reste inchangée côté conclusions générées.

---

## Liens

- Étape 0 cohérence : note F-JU-02 dans `docs/PRODUCT_SPEC.md` (2026-05-22)
- SF-01 mini-spec : `docs/features/F-JU-02/SF-JU-02-01-backend-prompt-context.md`
- Service backend réutilisé : `backend/src/main/java/fr/ailegalcase/jurisprudencemapping/ConclusionsJurisprudenceContext.java`
- Service frontend cible : `frontend/src/app/core/services/pdf-export.service.ts`
- Patterns miroirs (sections PDF existantes) :
  - F-192 SF-192-03 (`buildStrategiesRetenuesSection`)
  - F-195 SF-195-03 (`buildRisquesValidesSection`)
  - F-196 SF-196-03 (`buildAiQuestionsSection`)
