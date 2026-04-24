# Mini-spec — F-IM-18 / SF-IM-18-01 Situation familiale branchée dans l'arbre décisionnel

## Identifiant
`F-IM-18 / SF-IM-18-01`

## Feature parente
`F-IM-18` — Situation familiale branchée dans l'arbre décisionnel

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-18-01-situation-familiale-branchee`

---

## Objectif

Brancher effectivement le paramètre `situationFamiliale` (déjà accepté par `ImmigrationTitleDecisionEngine` mais ignoré — lignes 50-101) dans la logique de résolution `motif=FAMILLE` pour produire des recommandations différenciées selon MARIE / PACS_COHABITATION / CELIBATAIRE, France et Belgique, avec bases légales explicites.

---

## Comportement attendu

### Cas nominal

**France + motif=FAMILLE :**
| situationFamiliale | Titres recommandés | Base légale |
|---|---|---|
| `MARIE` | `CST_VPF` (prioritaire) + `CARTE_RESIDENT` si LONG_SEJOUR | L.423-1 (conjoint de Français) + L.423-2 (accès carte résident après 3 ans mariage) |
| `PACS_COHABITATION` | `CST_VPF` uniquement | L.423-1 (PACS ≥ 1 an) — pas d'accès direct à la CARTE_RESIDENT |
| `CELIBATAIRE` | `CST_VPF` uniquement | L.423-7 (parent d'enfant français) ou L.423-9 (liens personnels et familiaux) — cas fallback, pas d'accès direct à la CARTE_RESIDENT |
| `null` (non renseigné) | comportement actuel : `CST_VPF` + `CARTE_RESIDENT` si LONG_SEJOUR | rétrocompatibilité totale |

**Belgique + motif=FAMILLE :**
| situationFamiliale | Titres recommandés | Base légale |
|---|---|---|
| `MARIE` | `CARTE_A_FAMILLE` (prioritaire) + `CARTE_B` si LONG_SEJOUR | art. 40ter Loi 15/12/1980 (membre famille Belge) + carte B après 5 ans |
| `PACS_COHABITATION` | `CARTE_A_FAMILLE` (prioritaire) + `CARTE_B` si LONG_SEJOUR | art. 40ter (cohabitation légale — équivalent belge du PACS) |
| `CELIBATAIRE` | `CARTE_A_FAMILLE` uniquement | parent d'enfant belge ou autre lien familial — pas d'accès direct à la CARTE_B |
| `null` | comportement actuel : `CARTE_A_FAMILLE` + `CARTE_B` si LONG_SEJOUR | rétrocompatibilité totale |

Pour les autres motifs (TRAVAIL, ETUDES, ASILE, AUTRE) le paramètre `situationFamiliale` n'a aucun effet (comportement inchangé).

### Cas d'erreur
| Situation | Comportement | Code HTTP |
|---|---|---|
| `situationFamiliale` invalide (hors enum) | Erreur existante | 400 |
| `situationFamiliale = null` | Comportement legacy conservé (rétrocompatibilité) | 200 |
| `motif != FAMILLE` + situationFamiliale fourni | Paramètre ignoré (comportement inchangé) | 200 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-IM-05 Titre séjour (cible directe), F-IM-06 Recours (pas de situation familiale), F-IM-07 Droit au travail (pas de situation familiale), F-DT-* / F-FA-* (pas impactés)
- [x] **Autres pays** : France + Belgique tous les deux traités
- [x] **Autres domaines** : non applicable — spécifique DROIT_IMMIGRATION
- [x] **Autres UI patterns** : non applicable — pas de nouveau pattern UI
- [x] **Autres flows transversaux** : aucun

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `ImmigrationTitleDecisionEngine` France | Oui | Intégré dans cette SF |
| `ImmigrationTitleDecisionEngine` Belgique | Oui | Intégré dans cette SF (parité FR/BE) |
| F-IM-06 Recours | Non | Recours = décision administrative, pas lié à situation familiale |
| F-IM-07 Droit au travail | Non | Droit travail = lié au titre, pas à situation familiale |
| Frontend `immigration-title-decision-section.component` | Non | Le frontend propose déjà le sélecteur `situations` (MARIE/PACS/CELIBATAIRE) — aucun changement d'UI requis, seul le résultat renvoyé change. |
| Enum `VALID_SITUATIONS` (backend) | Non | Les 3 valeurs existantes suffisent pour cette SF. Ajout éventuel de `PARENT_ENFANT_NATIONAL` → backlog F-IM-18 V8 (pas bloquant). |
| Base de données (colonne `situation_familiale`) | Non | Colonne déjà persistée depuis F-IM-05 (non modifiée). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (FR + BE, parité)
- [ ] Subfeature(s) parallèle(s) créée(s) — aucune
- [ ] Backlog V8 — possibilité d'étendre avec `PARENT_ENFANT_NATIONAL` pour trigger `NAISSANCE_ENFANT_FR`, non bloquant pour cette SF
- [x] Non applicable aux autres outils (justifié ci-dessus)

---

## Impact par domaine métier

Cette feature est **sensible au domaine** : spécifique à **DROIT_IMMIGRATION**.
- **Droit du travail** : non applicable — la situation familiale n'impacte pas la résolution de titres travail.
- **Droit de la famille** : non applicable — `ImmigrationTitleDecisionEngine` est hors domaine famille.
- **Droit immigration** : adressé sur les 2 pays (France + Belgique).

La SF couvre FR + BE systématiquement (parité géographique garantie).

---

## Parité des domaines métier

Outil décisionnel **niveau 4** (arbre décisionnel) — la règle "parité niveaux ≥5" ne s'applique pas formellement. Cependant :
- Les domaines `DROIT_DU_TRAVAIL` et `DROIT_FAMILLE` n'ont pas d'arbre décisionnel équivalent pour "situation familiale" (concept propre à l'immigration).
- Pas d'asymétrie créée.

---

## Critères d'acceptation

- [ ] **C1** : `ImmigrationTitleDecisionEngine.resolve(FRANCE, false, FAMILLE, LONG_SEJOUR, "MARIE")` retourne `[CST_VPF, CARTE_RESIDENT]` dans cet ordre
- [ ] **C2** : `ImmigrationTitleDecisionEngine.resolve(FRANCE, false, FAMILLE, LONG_SEJOUR, "PACS_COHABITATION")` retourne `[CST_VPF]` seulement (pas de CARTE_RESIDENT)
- [ ] **C3** : `ImmigrationTitleDecisionEngine.resolve(FRANCE, false, FAMILLE, LONG_SEJOUR, "CELIBATAIRE")` retourne `[CST_VPF]` seulement
- [ ] **C4** : `ImmigrationTitleDecisionEngine.resolve(FRANCE, false, FAMILLE, LONG_SEJOUR, null)` retourne `[CST_VPF, CARTE_RESIDENT]` (rétrocompat)
- [ ] **C5** : `ImmigrationTitleDecisionEngine.resolve(BELGIQUE, false, FAMILLE, LONG_SEJOUR, "MARIE")` retourne `[CARTE_A_FAMILLE, CARTE_B]`
- [ ] **C6** : `ImmigrationTitleDecisionEngine.resolve(BELGIQUE, false, FAMILLE, LONG_SEJOUR, "CELIBATAIRE")` retourne `[CARTE_A_FAMILLE]` seulement
- [ ] **C7** : `ImmigrationTitleDecisionEngine.resolve(BELGIQUE, false, FAMILLE, LONG_SEJOUR, null)` retourne `[CARTE_A_FAMILLE, CARTE_B]` (rétrocompat)
- [ ] **C8** : `motif=TRAVAIL` + `situationFamiliale` non-null → résultat inchangé (paramètre ignoré hors FAMILLE)
- [ ] **C9** : IT `POST /api/v1/case-files/{id}/immigration-title-decision` avec `situationFamiliale=CELIBATAIRE` + `motif=FAMILLE` → 200 + recommandations correctement restreintes
- [ ] **C10** : tous les tests existants `ImmigrationTitleDecisionEngineTest` restent verts (pas de régression)

---

## Périmètre

### Hors scope (explicite)
- Ajout d'un nouveau code `PARENT_ENFANT_NATIONAL` à l'enum — backlog (F-IM-18 extension).
- Ajout d'un champ `legalBasis` au record `TitleRecommendation` — backlog.
- Différenciation de l'affichage CONDITIONS côté frontend selon situation familiale — hors SF.
- Prompt IA modifié pour détecter situation familiale — hors SF (la détection vient déjà des triggers F-150 `MARIAGE_RESSORTISSANT_FR` / `PACS_RESSORTISSANT_FR`).

---

## Technique

### Endpoints
Inchangés. `POST /api/v1/case-files/{id}/immigration-title-decision` + `GET /api/v1/case-files/{id}/immigration-title-decision`.

### Tables impactées
Aucune. Colonne `situation_familiale` déjà persistée dans `immigration_title_decisions`.

### Migration Liquibase
- [ ] Oui
- [x] Non applicable

### Composants modifiés
- Backend : `ImmigrationTitleDecisionEngine.java` (méthodes `resolveFrance` + `resolveBelgique`, branche `FAMILLE`)
- Backend tests : `ImmigrationTitleDecisionEngineTest.java` (~8 nouveaux tests)
- Backend IT : `ImmigrationTitleDecisionControllerIT.java` (1 nouveau test)

---

## Plan de test

### Tests unitaires (ImmigrationTitleDecisionEngineTest)

- [ ] `horsUE_famille_france_marie_longSejour_returnsCstVpfAndCarteResident`
- [ ] `horsUE_famille_france_pacs_longSejour_returnsOnlyCstVpf`
- [ ] `horsUE_famille_france_celibataire_longSejour_returnsOnlyCstVpf`
- [ ] `horsUE_famille_france_nullSituation_longSejour_returnsCstVpfAndCarteResident` (rétrocompat)
- [ ] `horsUE_famille_belgique_marie_longSejour_returnsCarteAFamilleAndCarteB`
- [ ] `horsUE_famille_belgique_celibataire_longSejour_returnsOnlyCarteAFamille`
- [ ] `horsUE_famille_belgique_nullSituation_longSejour_returnsCarteAFamilleAndCarteB` (rétrocompat)
- [ ] `horsUE_travail_situationIgnored` (MARIE + TRAVAIL → résultat identique à null + TRAVAIL)

### Tests d'intégration (ImmigrationTitleDecisionControllerIT)

- [ ] `POST /api/v1/case-files/{id}/immigration-title-decision` — situationFamiliale=CELIBATAIRE + motif=FAMILLE + LONG_SEJOUR + FRANCE → 200 + `recommendations.size==1` + `recommendations[0].code=="CST_VPF"`

### Isolation workspace

- [x] Non applicable — la SF ne change pas le modèle d'accès. L'IT existant `validates_workspace_isolation` continue de couvrir la règle.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing
- [x] **Aucune préoccupation transversale** — subfeature isolée à `ImmigrationTitleDecisionEngine`.

### Composants / endpoints impactés potentiels

Aucun — la signature publique de `resolve(...)` est inchangée, seule la logique interne change. Le frontend continue d'envoyer les mêmes paramètres et d'afficher la réponse (liste `recommendations`) identiquement. Les tests existants restent tous verts.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — la SF ne touche ni auth, ni workspace, ni navigation, ni plans.

---

## Dépendances

### Subfeatures bloquantes
- SF-IM-05-01 (engine existant) — done
- SF-IM-05-04 (mapping trigger → situation) — done

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- **Conservation de l'enum** : on garde `[CELIBATAIRE, MARIE, PACS_COHABITATION]` inchangé. Ajout d'un 4e code (ex: `PARENT_ENFANT_NATIONAL`) jugé hors scope — reporté si un cas d'usage réel le demande.
- **Rétrocompat** : `null` déclenche explicitement le comportement legacy (CST_VPF + CARTE_RESIDENT si LONG), pour ne pas casser les dossiers déjà résolus avant cette SF.
- **Base juridique FR** : L.423-2 CESEDA — carte de résident pour conjoint de Français délivrée après 3 ans de mariage + vie commune. Pour PACS, pas d'équivalent direct (le PACS ne conduit pas automatiquement à la CARTE_RESIDENT).
- **Base juridique BE** : art. 40ter Loi du 15 décembre 1980 — membre famille de Belge (conjoint, cohabitant légal, parent d'enfant mineur belge). Accès CARTE_B après 5 ans carte A.
