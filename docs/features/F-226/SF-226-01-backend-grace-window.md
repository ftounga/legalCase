# Mini-spec — F-226 / SF-226-01 Backend — Élargir grace window PipelineRecoveryRunner 30s → 600s

## Identifiant

`F-226 / SF-226-01`

## Statut

`draft` — 2026-05-07

## Branche Git

`feat/SF-226-01-backend-recovery-grace-window`

## Pattern de référence

Aucun pattern à dupliquer — fix ciblé sur `PipelineRecoveryRunner` (SF-185-06).

---

## Objectif

Passer le défaut `pipeline.recovery.grace-seconds` de **30 à 600 secondes** (10 minutes) dans `PipelineRecoveryRunner` pour empêcher le HPA scale-up de marquer FAILED les analyses lourdes (3-5 min) actuellement en cours sur un autre pod.

---

## Comportement attendu

### Cas nominal (après fix)

1. HPA détecte CPU > 70% → ajoute un pod
2. Nouveau pod démarre → `PipelineRecoveryRunner.onStartup()` → `runRecovery()`
3. SQL UPDATE filtre `created_at < NOW() - 600 seconds`
4. Une analyse en cours créée il y a 3 min → `created_at = NOW() - 180s` → **NON marquée FAILED** (grace window respectée)
5. L'analyse continue sur le pod d'origine et finit DONE normalement

### Cas couvert : pod réellement crashé / OOM

1. Pod kill brutal (OOM, kubectl delete) — l'analyse n'a pas le temps de marquer FAILED
2. Nouveau pod démarre 90s plus tard
3. Analyse zombie en DB : `created_at = NOW() - 4min` → toujours dans grace window (10 min) → **NON marquée FAILED**
4. ⚠️ L'analyse zombie reste PROCESSING jusqu'à 10 min après création (au lieu de 30s actuellement) — accepté car :
   - Les analyses Anthropic timeout à 5 min côté streaming (`Streaming SUMMARY` log)
   - Au pire, l'utilisateur attend 10 min avant de pouvoir relancer (vs aujourd'hui où il attendait 0 min mais voyait sa vraie analyse killée à tort)
   - Bénéfice net : 99% des analyses réussissent au lieu d'être bombardées

### Cas restant non couvert (V2 — différé)

Une analyse REÉLLEMENT zombie qui ne timeout pas (ex. Anthropic répond très lentement après 10 min) ne sera pas killée par recovery — elle restera PROCESSING jusqu'au prochain restart. **Acceptable V1**, fix V2 = pattern heartbeat (analyse écrit `last_active` toutes les 30s).

---

## Critères d'acceptation

- [ ] **CA-01** : `@Value("${pipeline.recovery.grace-seconds:30}")` → `@Value("${pipeline.recovery.grace-seconds:600}")`
- [ ] **CA-02** : la JavaDoc de `PipelineRecoveryRunner` reflète le nouveau seuil et explique la motivation (HPA scale-up + bug F-226 staging Immigration Chen 16 2026-05-07)
- [ ] **CA-03** : test `PipelineRecoveryRunnerTest` existant (grace window respectée pour analyse récente) reste vert
- [ ] **CA-04** : nouveau test : analyse créée 5 min avant → préservée (passait FAILED avec grace=30s, doit rester préservée avec grace=600s)
- [ ] **CA-05** : nouveau test : analyse créée 11 min avant → marquée FAILED (au-delà de grace=600s)
- [ ] **CA-06** : log INFO ajouté au démarrage du runner : `Pipeline recovery starting — grace window {graceSeconds}s` (pour faciliter le debug futur si grace re-modifiée)
- [ ] **CA-07** : aucune migration DB nécessaire — fix purement applicatif

---

## Périmètre

### Hors scope V1

- (a) Pattern heartbeat — V2 si signal terrain montre des analyses zombies > 10 min
- (b) Détection des pods morts via Kubernetes API — overkill V1
- (c) Modification du timeout côté streaming Anthropic — couvert ailleurs

---

## Technique

### Fichiers à modifier

1. `backend/src/main/java/fr/ailegalcase/analysis/PipelineRecoveryRunner.java` :
   - Ligne 61 : `@Value("${pipeline.recovery.grace-seconds:30}")` → `:600}"`
   - Lignes 28-31 : mettre à jour la JavaDoc « Grace window » avec nouvelle valeur + motivation
   - Ligne ~75 : ajouter `log.info("Pipeline recovery starting — grace window {}s", graceSeconds);` au début de `runRecovery()`

2. `backend/src/test/java/fr/ailegalcase/analysis/PipelineRecoveryRunnerTest.java` :
   - Adapter les tests qui dépendent de la grace window (les tests existants utilisent probablement des valeurs explicites passées au constructeur — vérifier)
   - Ajouter test CA-04 et CA-05

### Aucune migration

Pas de schema change. Variable application.yml.

### Configuration optionnelle staging/prod

Aucune action requise — le défaut 600s sera appliqué automatiquement via le `@Value` lors du prochain rebuild. Si on veut re-customiser plus tard, ajouter `pipeline.recovery.grace-seconds: 1200` dans `application-prod.yml`.

---

## Plan de test

### Tests unitaires (~3-4)

- `PipelineRecoveryRunnerTest` :
  - T-existant : analyse récente (< grace) préservée → reste vert
  - T-CA-04 : analyse 5 min ancienne avec grace=600s → préservée
  - T-CA-05 : analyse 11 min ancienne avec grace=600s → marquée FAILED
  - T-CA-06 : log INFO `Pipeline recovery starting — grace window 600s` émis

### Test manuel post-deploy staging

1. Lancer une analyse longue (5 min) sur un dossier
2. Forcer un scale-up HPA en générant du CPU (autre dossier ou simuler)
3. Vérifier dans logs : nouveau pod démarre, `Pipeline recovery on startup — 0 case_analyses + 0 jobs` (grace window 600s respectée)
4. L'analyse termine DONE normalement sans interruption

---

## Dépendances

- F-185 SF-185-06 ✅ (PipelineRecoveryRunner existant)

---

## Impact par domaine métier

Transversal — infra pipeline IA, aucune adaptation par domaine ni par pays.

---

## Analyse de cohérence transversale

- **Auth/Principal** : non concerné — tâche système.
- **Workspace context** : non concerné — opération transversale tous workspaces.
- **Plans/limites** : non concerné.
- **Navigation/routing** : non concerné — backend.
- **Outil décisionnel métier** : non concerné — infra.
- **Pattern partagé** : aucun nouveau pattern. Modification d'un défaut existant.

---

## Risques

- **Analyse zombie réelle non killée pendant 10 min** : utilisateur bloqué 10 min au pire. Aujourd'hui il était bloqué FAUSSEMENT (vraie analyse killée). Bénéfice net positif.
- **Régression tests** : 30s hardcodé dans certains tests probablement — adapter.

---

## Notes

- **Décision 2026-05-07** : V1 = simple bump 30→600s. V2 heartbeat seulement si signal terrain montre besoin. Garder l'implémentation aussi simple que possible.
- **Décision 2026-05-07** : pas de configuration différente staging/prod — même défaut partout pour cohérence et reproductibilité des bugs.
