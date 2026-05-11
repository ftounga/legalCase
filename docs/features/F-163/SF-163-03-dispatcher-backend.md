# Mini-spec — F-163 / SF-163-03 Dispatcher backend `/api/v1/simulators/{toolId}/calculate`

## Identifiant

`F-163 / SF-163-03`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-03-dispatcher-backend`

---

## Objectif

> Livrer le **dispatcher backend** unique `POST /api/v1/simulators/{toolId}/calculate` qui route vers le bon `Calculator` / `Analyzer` stateless **sans persister** le résultat. Couvre tous les calculators identifiables comme **purs** dans l'audit transversal 2026-05-11 (~60 calculators stateless). Les calculators avec dépendances complexes (lecture d'autres analyses, parsing PDF, etc.) sont **explicitement exclus V1** et listés dans la mini-spec.

---

## Comportement attendu

### Cas nominal

1. Le frontend (composant en mode standalone) POST `/api/v1/simulators/F-DT-08-licenciement-validity/calculate` avec un body identique à celui qu'il aurait envoyé sur `/api/v1/case-files/{id}/licenciement`.
2. `SimulatorCalculateController` reçoit la requête, vérifie l'auth (MEMBER min via filtre Spring Security standard).
3. Le contrôleur résout le `toolId` dans `SimulatorCalculatorRegistry` qui retourne un descripteur `{ requestType: Class<?>, handler: Function<Object, Object> }`.
4. Le contrôleur désérialise le body JSON vers `requestType` (via Jackson), appelle le handler, sérialise le résultat en JSON.
5. **Aucune persistance** — pas de save sur `*AnalysisRepository`.
6. Réponse 200 avec le payload identique à ce qu'aurait retourné l'endpoint case-file scoped (compatibilité ascendante stricte avec les composants frontend).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `toolId` inconnu du dispatcher | `{ error: "Tool not supported in simulator mode", toolId }` | 404 |
| Body JSON invalide pour le requestType de l'outil | Message d'erreur Jackson + détail | 400 |
| Champs obligatoires manquants (validation Bean Validation) | Message structuré champ → erreur | 422 |
| Erreur métier du calculator (ex. country non supporté) | Message du calculator | 422 |
| Erreur interne inattendue | Snack + log SLF4J | 500 |
| Non authentifié | Filtre Spring Security | 401 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Tous les calculators backend stateless** : audit 2026-05-11 identifie ~60 calculators purs (`LicenciementAnalyzer`, `DiscriminationCalculator`, `RuptureConvAnalyzer`, etc.).
- [x] **Tous les services avec persistance** : ~30 services `*Service.java` qui appellent `repository.save()`. Le dispatcher contourne le service en V1 — il appelle directement le calculator pur.
- [x] **Calculators avec dépendances complexes** : ~5-10 calculators qui lisent d'autres analyses du dossier (ex. `PrestationCompensatoireCalculator` qui dépend de `liquidationCommunaute`). **Exclus V1** — listés dans le hors-scope.
- [x] **Calculators PDF / OCR** : Fiche prud'homale, tribunal travail. **Exclus V1** — leur output principal est un PDF, pas un calcul. Mode simulateur non pertinent.
- [x] **Composants frontend** : la signature `SimulatorCalculateRequest/Response` doit être **identique** au request/response existant de chaque outil pour permettre une simple bascule d'URL côté frontend (pas de refactor de payload).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Calculator stateless (`LicenciementAnalyzer`, etc.) | Oui (~60) | Inclus V1 dans `SimulatorCalculatorRegistry` |
| Calculator avec dépendances cross-analysis | Oui (~5-10) | Exclus V1 — 404 si appelé en standalone, listé en hors-scope |
| Calculator PDF | Oui (~3) | Exclus V1 — 404 si appelé |
| Service avec persistance | Oui (~30) | Bypassé — le dispatcher appelle directement le calculator pur sans passer par le service |
| Endpoint `/api/v1/case-files/{id}/{tool}-analysis` existants | Oui (~80) | Inchangés — mode case-file continue de fonctionner normalement |

### Décision

- [x] Étendu à toutes les cibles applicables (60 calculators stateless couverts) dans cette SF
- [x] Backlog ouvert : SF-163-03b ultérieure pour les calculators à dépendances complexes (5-10 outils restants) si la demande émerge.

---

## Conformité F-IA-04

- [x] **Non applicable** — SF backend pure (endpoint + registry, pas de composant frontend décisionnel).

---

## Critères d'acceptation

- [ ] **CA-01** : nouvelle classe `SimulatorCalculateController` exposant `POST /api/v1/simulators/{toolId}/calculate` protégé par filtre Spring Security standard.
- [ ] **CA-02** : nouvelle classe `SimulatorCalculatorRegistry` (`@Component`) avec une `Map<String, CalculatorDescriptor>` immutable initialisée au `@PostConstruct`.
- [ ] **CA-03** : le registry référence **au minimum 50 calculators stateless** (couverture 80%+ des outils Travail FR + Famille FR + Immigration FR/BE existants).
- [ ] **CA-04** : pour `F-DT-08-licenciement-validity`, le dispatcher invoque `LicenciementAnalyzer.analyze(...)` avec les mêmes paramètres que `LicenciementService` aujourd'hui, **sans appeler `analysisRepository.save()`**.
- [ ] **CA-05** : le payload de réponse est **strictement identique** au `LicenciementResponse` retourné par l'endpoint case-file scoped (mêmes champs, mêmes types, mêmes valeurs).
- [ ] **CA-06** : `toolId` inconnu du registry → 404 avec body `{ error, toolId }`.
- [ ] **CA-07** : body JSON invalide → 400 avec message Jackson.
- [ ] **CA-08** : champs obligatoires manquants → 422 (validation `@Valid` activée).
- [ ] **CA-09** : 401 si non authentifié (filtre Spring Security).
- [ ] **CA-10** : **aucune ligne** insérée dans `*_analyses` ou tables `case_files` lors d'un appel au dispatcher (vérifié par test IT — compter avant/après).
- [ ] **CA-11** : tests IT couvrent : (a) F-DT-08 nominal, (b) au moins 3 autres outils représentatifs des 3 domaines (1 Famille FR, 1 Immigration FR, 1 Travail BE), (c) toolId inconnu 404, (d) payload invalide 400/422, (e) 401, (f) non-persistance vérifiée par compteur DB.
- [ ] **CA-12** : la liste exacte des `toolId` supportés est exposée via `GET /api/v1/simulators/supported-tools` (optionnel V1) OU **documentée dans la mini-spec** + dans le test IT.
- [ ] **CA-13** : isolation workspace — le dispatcher **n'utilise pas** le workspace de l'utilisateur dans la logique métier (le calcul est universel), mais l'auth est requise pour éviter les abus.

---

## Périmètre

### Hors scope V1 (explicite)

Les `toolId` suivants ne sont **pas couverts** par le dispatcher V1 — appel → 404 :
- **Calculators PDF** (3) : `F-DT-04-fiche-prudhomale`, `F-DT-06-requete-tribunal-travail`, et tout outil dont l'output principal est un document Word/PDF.
- **Calculators avec dépendances cross-analysis** (5-10) : tout calculator qui aujourd'hui lit dans son service une autre `*AnalysisRepository` du même dossier. Liste à figer pendant l'implémentation par grep dans les `*Service.java` ; documenter dans le code source du registry avec un commentaire `// Excluded V1 — depends on cross-analysis: ...`.
- **F-FA-01-prestation-compensatoire** (wrapper info-only) et autres wrappers `PREFILL_COUNT_ALWAYS_ZERO=true` — pas de logique de calcul à invoquer.

### Hors scope autres

- **GET `/supported-tools`** : optionnel V1 (peut être livré dans cette SF si effort faible — sinon report V2).
- **Persistance optionnelle "save to my simulations"** : différée à SF-163-04 si la demande émerge.

---

## Contrat API exposé

`POST /api/v1/simulators/{toolId}/calculate`

**Auth** : Bearer JWT / cookie session (filtre Spring Security standard).

**Path parameter** : `toolId` (string, exact match des IDs présents dans `TOOL_REGISTRY` frontend et `decision_tool_visibility_rules.tool_id` backend).

**Request body** : JSON spécifique au `toolId` — **structure identique** au body de l'endpoint case-file scoped correspondant. Documenté dans les tests IT (un exemple par calculator).

**Response 200** : JSON spécifique au `toolId` — **structure identique** à la réponse de l'endpoint case-file scoped correspondant.

**Response 404** :
```json
{ "error": "Tool not supported in simulator mode", "toolId": "..." }
```

**Response 401 / 400 / 422 / 500** : conventions Spring Boot standard.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/simulators/{toolId}/calculate` | Oui | MEMBER |
| GET | `/api/v1/simulators/supported-tools` | Oui | MEMBER | *(optionnel V1)* |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| Aucune | — | Dispatcher pur, **pas de persistance** |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Composants Spring Boot (à créer)

- `backend/src/main/java/fr/ailegalcase/casefile/SimulatorCalculateController.java`
- `backend/src/main/java/fr/ailegalcase/casefile/SimulatorCalculatorRegistry.java`
- `backend/src/main/java/fr/ailegalcase/casefile/SimulatorCalculatorDescriptor.java` (record `record SimulatorCalculatorDescriptor<Req>(String toolId, Class<Req> requestType, Function<Req, Object> handler)`)
- `backend/src/test/java/fr/ailegalcase/casefile/SimulatorCalculateControllerIT.java`

### Pattern d'enregistrement (exemple)

```java
@Component
public class SimulatorCalculatorRegistry {
    private final Map<String, SimulatorCalculatorDescriptor<?>> descriptors = new HashMap<>();

    public SimulatorCalculatorRegistry(/* injections : LicenciementAnalyzer, DiscriminationCalculator, ... */) {
        register("F-DT-08-licenciement-validity", LicenciementRequest.class,
                req -> LicenciementAnalyzer.analyze(req.country(), req.responses(), ...));
        register("F-DT-12-discrimination-dommages-interets", DiscriminationRequest.class,
                req -> discriminationCalculator.calculate(req));
        // ... 50+ entries
    }

    public Optional<SimulatorCalculatorDescriptor<?>> find(String toolId) { ... }
}
```

---

## Plan de test

### Tests d'intégration (Spring Boot)

- [ ] `SimulatorCalculateControllerIT.licenciementValidity_nominal_200` — POST avec un payload valide F-DT-08 → 200 + verdict attendu.
- [ ] `SimulatorCalculateControllerIT.unknownToolId_404` — POST `/api/v1/simulators/UNKNOWN/calculate` → 404.
- [ ] `SimulatorCalculateControllerIT.invalidJson_400` — body malformé → 400.
- [ ] `SimulatorCalculateControllerIT.missingRequiredField_422` — champ obligatoire absent → 422.
- [ ] `SimulatorCalculateControllerIT.unauthenticated_401` — sans token → 401.
- [ ] `SimulatorCalculateControllerIT.noPersistence` — POST + compter les rows dans 5 tables `*_analyses` avant/après → identique.
- [ ] `SimulatorCalculateControllerIT.familleFR_nominal_200` — 1 outil Famille FR représentatif.
- [ ] `SimulatorCalculateControllerIT.immigrationFR_nominal_200` — 1 outil Immigration FR.
- [ ] `SimulatorCalculateControllerIT.travailBE_nominal_200` — 1 outil Travail BE.

### Isolation workspace

- [x] Applicable au sens auth requise — mais le résultat du calcul ne dépend pas du workspace (calcul universel).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal** — réutilise filtre Spring Security standard.
- [ ] **Workspace context** — pas utilisé dans la logique métier.
- [ ] **Plans / limites** — pas de gate plan V1.
- [ ] **Navigation / routing** — backend uniquement.
- [ ] **Outil décisionnel métier** — coché, mais pas de nouvel outil créé : on expose les calculators existants via un nouveau canal.
- [x] **Aucune autre préoccupation transversale**

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `LicenciementController.analyze` et autres endpoints case-file scoped | Aucun — non touchés | Test IT existant `LicenciementControllerIT` doit rester vert |
| `*Analyzer.java` / `*Calculator.java` purs | Aucun — invoqués via le registry sans modification de leur API publique | Tests UT calculators existants verts |

### Smoke tests E2E concernés

- [ ] Aucun smoke E2E nouveau requis — l'endpoint est consommé par le frontend SF-163-02a (testé séparément).

---

## Dépendances

### Subfeatures bloquantes

- Aucune (SF-163-03 peut démarrer en parallèle de SF-163-02a — c'est même le pattern recommandé `parallel-frontback-delivery`).

### Notes et décisions

- **Décision** : le dispatcher invoque directement le `*Analyzer` / `*Calculator` pur, **pas** le `*Service` (qui ajouterait la persistance). C'est l'architecture clean — séparation calcul / persistance déjà en place dans le codebase.
- **Décision** : pas de table `simulator_calculations_log` V1 — le dispatcher est totalement stateless. Un audit/log SLF4J INFO par requête suffit pour debug.
- **Décision** : les `toolId` supportés sont enregistrés explicitement dans le registry (pas de scan auto par réflexion) — meilleure traçabilité, évite les surprises.
- **Décision** : pas de cache de réponses V1 — chaque appel recalcule. À envisager V2 si charge élevée.
- **Décision** : versioning de l'endpoint (`/api/v1/...`) — pas de versioning multi-tenant ni de versioning par toolId. Les payloads suivent les types existants ; toute évolution future sera coordonnée avec les calculators.
