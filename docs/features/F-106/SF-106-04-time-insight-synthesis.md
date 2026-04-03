# Mini-spec — F-106 / SF-106-04 — Suivi du temps facturable — insight temps × risque dans la synthèse

## Identifiant
`F-106 / SF-106-04`

## Feature parente
`F-106` — Suivi du temps facturable par dossier

## Statut
`draft`

## Date de création
2026-04-03

## Branche Git
`feat/SF-106-04-time-insight-synthesis`

---

## Objectif

Afficher dans la page synthèse (`/case-files/:id/synthesis`) un encart insight combinant le temps total enregistré sur le dossier et le score de risque IA, pour aider l'avocat à calibrer son honoraire.

---

## Comportement attendu

### Cas nominal

**Encart insight (dans SynthesisComponent)**
- Affiché sous le badge de risque, uniquement si : synthèse chargée ET `riskLevel != null` ET au moins une session terminée (`durationSeconds != null`).
- Texte généré côté frontend à partir des données déjà disponibles :
  - Temps total : somme des `durationSeconds` des entrées terminées (`stoppedAt != null`) du dossier.
  - Format durée : "Xh Ymin" (ex. "2h 15min"). Si < 1min → "< 1min".
  - Niveau de risque : label français (`FAIBLE` → "Faible", `MOYEN` → "Moyen", `ELEVE` → "Élevé").
- Message affiché : **"Ce dossier représente [durée] de travail enregistré — risque [niveau]. Pensez à vérifier votre honoraire."**
- Style : callout doré (`#C9973A`) avec icône `lightbulb`, fond `rgba(201, 151, 58, 0.08)`, border-left `3px solid #C9973A`.

**Chargement**
- Les entrées de temps sont chargées via `TimeService.loadEntries(caseFileId)` (endpoint `GET /api/v1/case-files/{id}/time-entries` — déjà disponible).
- Le chargement est parallèle au chargement de la synthèse (`forkJoin` ou séquence existante dans `ngOnInit`).
- Si le chargement des entrées échoue silencieusement → l'encart n'est simplement pas affiché (pas de message d'erreur bloquant).

### Cas de non-affichage (pas d'erreur, encart absent)

| Condition | Comportement |
|-----------|-------------|
| Synthèse sans `riskLevel` | Encart non affiché |
| Aucune session terminée sur le dossier | Encart non affiché |
| Chargement des entrées en erreur | Encart non affiché (fail silencieux) |
| Timer actif uniquement (pas encore stoppé) | Non compté — seules les entrées avec `durationSeconds != null` |

---

## Critères d'acceptation

- [ ] Encart visible dans la synthèse si riskLevel non null ET au moins une entrée terminée
- [ ] Durée totale calculée uniquement sur les entrées avec `durationSeconds != null`
- [ ] Format durée "Xh Ymin" (ou "< 1min" si < 60s)
- [ ] Niveau de risque traduit en français
- [ ] Encart absent si pas de session ou pas de riskLevel
- [ ] Échec silencieux si l'API time-entries renvoie une erreur
- [ ] Style : callout doré conforme au design system
- [ ] Aucun nouveau endpoint backend — données issues des APIs existantes

---

## Périmètre

### Hors scope
- Rapport mensuel (SF-106-03 — déjà fait)
- Modification des sessions depuis la synthèse
- Calcul du montant dans la synthèse (le montant est dans le rapport mensuel)
- Appel LLM pour générer le texte (le message est un template statique)

---

## Technique

### Composants Angular

- `SynthesisComponent` — injection de `TimeService`, chargement des entrées dans `ngOnInit`, `computed()` pour le total et l'insight text.
- Pas de nouveau composant — le callout est inline dans le template de synthèse.

### Endpoints consommés (SF-106-01, déjà mergé)
- `GET /api/v1/case-files/{id}/time-entries` (déjà utilisé par `TimeService.loadEntries()`)

### Migration Liquibase
- [x] Non applicable (frontend only)

---

## Plan de test

### Tests unitaires

- [ ] `SynthesisComponent` — `totalBilledSeconds()` : somme correcte des entrées terminées
- [ ] `SynthesisComponent` — `totalBilledSeconds()` : exclut les entrées actives (durationSeconds null)
- [ ] `SynthesisComponent` — `showInsight()` : true si riskLevel non null ET totalBilledSeconds > 0
- [ ] `SynthesisComponent` — `showInsight()` : false si aucune entrée
- [ ] `SynthesisComponent` — `showInsight()` : false si riskLevel null
- [ ] `SynthesisComponent` — `insightText()` : format "2h 15min … Élevé"
- [ ] `SynthesisComponent` — `insightText()` : format "< 1min" si < 60s

### Isolation workspace
- [ ] Non applicable — isolation vérifiée côté backend

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Aucune préoccupation transversale** — modification isolée à `SynthesisComponent`, pas de nouvelle route, pas de changement auth ni workspace context

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| `SynthesisComponent` | Ajout de `TimeService` en dépendance + chargement parallèle des entrées | Tests existants doivent rester verts |

### Smoke tests E2E concernés
- [ ] Aucun smoke test concerné — pas de nouvelle route, pas de guard modifié

---

## Dépendances

### Subfeatures bloquantes
- SF-106-01 — statut : done ✅
- SF-106-02 — statut : done ✅
- SF-106-03 — statut : done ✅

---

## Notes et décisions

- **Pas d'appel LLM** : le texte est un template statique combinant durée + niveau de risque. L'IA dans le nom SF-106-04 désigne le score de risque calculé par l'IA lors de l'analyse — pas un nouveau call LLM.
- **Fail silencieux** : si l'API time-entries échoue (ex. timeout), l'encart est simplement absent. La synthèse reste entièrement fonctionnelle.
- **`TimeService` déjà injecté ?** Non — c'est un nouvel import dans `SynthesisComponent`. Le service est `providedIn: 'root'`, pas d'impact sur les autres composants.
- **Timer actif en cours** : non comptabilisé (pas de `durationSeconds`). L'avocat verra le total des sessions fermées uniquement.
