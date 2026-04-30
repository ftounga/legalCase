# Mini-spec — F-161 / SF-161-01 — Backend : élever les caps de synthèse + assouplir le prompt

## Identifiant

`F-161 / SF-161-01`

## Feature parente

`F-161` — Augmentation des limites de synthèse (faits, risques, points juridiques, timeline)

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-161-01-augmenter-caps-synthese`

---

## Objectif

Élever les caps de troncature `app.pipeline.limits` de ~5× et reformuler les contraintes de longueur des prompts pour que l'IA produise « jusqu'à N selon la richesse du dossier, sans rembourrer ». Aujourd'hui sur dossiers complexes (50+ pages, immigration Chen 2, prud'homal multi-CDD), les caps actuels (7-10 faits / 5-6 risques / 5 timeline) tronquent silencieusement l'analyse.

---

## Comportement attendu

### Cas nominal

1. Dossier prud'homal complexe (12 pièces, 80 pages : 3 CDD successifs, contrat CDI, 24 bulletins, lettre licenciement, échanges email, certificat médical).
2. L'IA exécute la synthèse niveau dossier (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE`).
3. Le prompt système indique désormais : *« Produis jusqu'à 80 faits, 80 points_juridiques, 40 risques, 40 questions_ouvertes, 60 entrées timeline, 40 pièces manquantes, 30 points procédure, 20 pistes stratégiques. Pas de minimum — produis exactement ce que la richesse du dossier justifie, sans rembourrer pour atteindre les limites. »*
4. L'IA produit (ex.) 47 faits, 31 risques, 22 timeline events, 18 points juridiques. **Aucune troncature** par `AnalysisJsonTruncator` puisque sous les caps.
5. Sur un dossier simple (1 contrat + 2 bulletins + 1 lettre licenciement), l'IA produit ~8 faits, ~4 risques, ~6 timeline events. Pas de rembourrage forcé.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| L'IA dépasse le nouveau cap (ex. 90 faits sur un dossier extraordinaire) | `AnalysisJsonTruncator` tronque silencieusement aux N premiers — comportement actuel inchangé |
| Tokens output dépassent `max_tokens` (16384 sur enriched, 8192 sur initial) | JSON tronqué en milieu de phrase → `truncateCaseAnalysis` retourne tel quel (catch dans le truncator) — comportement actuel inchangé |
| Dossier vide (0 doc analysé) | L'IA produit `[]` partout — comportement actuel inchangé |

---

## Contrat (modification config + prompt only)

### Avant (`app.pipeline.limits.droit-du-travail.dossier`)

```yaml
faits: 7
points-juridiques: 5
risques: 5
questions-ouvertes: 5
timeline: 5
pieces-manquantes: 5
points-procedure: 8
# pistes-strategiques: 8 (default in Java)
```

### Après — DROIT_DU_TRAVAIL (dossier)

```yaml
faits: 80
points-juridiques: 80
risques: 40
questions-ouvertes: 40
timeline: 60
pieces-manquantes: 40
points-procedure: 30
pistes-strategiques: 20
```

### Après — DROIT_IMMIGRATION (dossier)

Identique sauf `timeline: 80` (les dossiers immigration tracent souvent une chronologie longue : entrées en France, demandes successives, refus, recours, etc.).

```yaml
faits: 80
points-juridiques: 80
risques: 40
questions-ouvertes: 40
timeline: 80
pieces-manquantes: 40
points-procedure: 30
pistes-strategiques: 20
```

### Après — DROIT_FAMILLE (dossier)

Identique à Travail (les dossiers famille peuvent être longs aussi : divorce + liquidation + garde sur 5 ans).

```yaml
faits: 80
points-juridiques: 80
risques: 40
questions-ouvertes: 40
timeline: 60
pieces-manquantes: 40
points-procedure: 30
pistes-strategiques: 20
```

### Document level (per-doc)

**Inchangé.** L'analyse par document est volontairement étroite (`faits: 5-7, points: 3-4, risques: 3-4, questions: 3, timeline: 0`) — chaque doc contribue une vue ciblée, l'agrégation se fait au niveau dossier. Pas de raison d'élever.

### Prompts modifiés

Modifier la phrase de fin dans `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` ET `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` :

**Avant** :
> Contraintes de longueur : %d entrées timeline maximum, %d faits maximum, %d points_juridiques maximum, %d risques maximum, %d questions_ouvertes maximum, %d pièces manquantes maximum, %d points procédure maximum, %d pistes stratégiques maximum. Sois concis.

**Après** :
> Contraintes de longueur : produis jusqu'à %d entrées timeline, %d faits, %d points_juridiques, %d risques, %d questions_ouvertes, %d pièces manquantes, %d points procédure, %d pistes stratégiques. Pas de minimum — produis exactement ce que la richesse du dossier justifie, sans rembourrer pour atteindre les limites.

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Document level** (DocumentAnalysisService) — pas concerné, caps volontairement étroits par-doc.
- **AnalysisJsonTruncator** — déjà paramétré par `LevelLimits`, aucun changement de code requis (c'est juste qu'il tronquera moins souvent).
- **`max_tokens` Anthropic** : 8192 sur initial (`CaseAnalysisService`), 16384 sur enriched (`EnrichedAnalysisService`). Avec 80 faits + 80 points + 40 risques + 60 timeline + 40 questions + 40 pièces + 30 procedure + 20 pistes ≈ ~250 entrées texte, marge de ~30-50 tokens/entrée + score_risque + delais_detectes + source_explanations → pic estimé 6-10 K output tokens. **Reste sous 8192 dans la majorité des cas, sous 16384 systématiquement**. Risque de troncature niveau initial sur dossiers extraordinaires : marginal mais possible — surveiller.
- **Coût IA** : Claude Sonnet 4.6 = 3 $/M input + 15 $/M output. +5 K output tokens ≈ +0,075 $/dossier. Sur ARR cible (191 €/mois × 12 = 2 300 € / SOLO) = négligeable.
- **F-15 budget tokens mensuel** : enveloppes SOLO/TEAM/PRO absorbent largement (cf. F-171 pour visibilité erreur quota déjà livrée).
- **Frontend SynthesisComponent** : aucune limite hardcodée côté front — il itère sur les arrays JSON. Pas de changement requis. Les pages risquent juste d'être plus longues — c'est précisément l'objet de F-162 (refonte synthèse en pages dédiées) qui est dépendante de F-160.
- **Tests** : `AnalysisJsonTruncatorTest` doit être adapté si les fixtures s'appuyaient sur les vieilles caps.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `application.yml` (caps `app.pipeline.limits`) | Oui | Modifié dans cette SF |
| `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` | Oui | Phrase finale reformulée dans cette SF |
| `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` | Oui | Phrase finale reformulée dans cette SF |
| `AnalysisJsonTruncator` | Non | Code inchangé, paramétré par `LevelLimits` |
| `AnalysisJsonTruncatorTest` | Oui | Mise à jour si fixtures s'appuyaient sur 5/7 caps |
| `DocumentAnalysisService` (doc-level) | Non | Caps volontairement étroits, hors scope |
| Frontend `SynthesisComponent` | Non | Itère sur arrays, aucune limite hardcodée |
| `AnalysisLimitsProperties` (record Java) | Non | Pas de changement structurel, juste valeurs |

### Décision

- [x] Étendu aux 2 prompts (initial + enriched) — symétrique
- [x] Étendu aux 3 domaines (Travail / Immigration / Famille)
- [x] Document level volontairement non touché — caps étroits per-doc

---

## Impact par domaine métier

**Transversal aux 3 domaines** (Travail / Immigration / Famille) avec un seul ajustement : `timeline: 80` pour Immigration vs `60` pour Travail/Famille (chronologies souvent plus longues en immigration : demandes successives, refus, recours, etc.). Pas de variation par pays (FR/BE) — la richesse est intrinsèque au dossier, pas au pays. Document level inchangé sur les 3 domaines.

---

## Critères d'acceptation

- [ ] **C1** — `application.yml` `app.pipeline.limits.droit-du-travail.dossier` mis à jour : faits 80, points-juridiques 80, risques 40, questions-ouvertes 40, timeline 60, pieces-manquantes 40, points-procedure 30, pistes-strategiques 20
- [ ] **C2** — `application.yml` `app.pipeline.limits.droit-immigration.dossier` idem mais `timeline: 80`
- [ ] **C3** — `application.yml` `app.pipeline.limits.droit-famille.dossier` idem que Travail
- [ ] **C4** — `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` : phrase finale remplacée par la nouvelle formulation « jusqu'à N… pas de minimum »
- [ ] **C5** — `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` : idem C4 (formulation symétrique)
- [ ] **C6** — `AnalysisJsonTruncatorTest` : ajout d'un test couvrant les nouveaux caps (ex. 80 faits → pas tronqué, 81 faits → tronqué à 80)
- [ ] **C7** — Tests unitaires existants `CaseAnalysisServiceTest`, `EnrichedAnalysisServiceTest` adaptés si une fixture s'appuyait sur les anciennes caps (vérification `prompt.contains("7 faits maximum")` ou similaire) — recherche grep + adaptation
- [ ] **C8** — Build backend vert (`./mvnw test`)
- [ ] **C9** — Aucun changement frontend requis (vérifier qu'aucun test Jest ne s'appuie sur le wording « maximum » dans la synthèse)
- [ ] **C10** — Aucune régression sur les tests d'intégration `CaseAnalysisFullIT` / `EnrichedAnalysisFullIT` (s'ils existent)

---

## Périmètre

### Hors scope

- Refonte structurelle de `AnalysisLimitsProperties` (déjà paramétré par F-29, on bump juste les valeurs)
- Refonte de `AnalysisJsonTruncator` (déjà paramétré, code inchangé)
- Document level caps (volontairement étroits, hors scope)
- Frontend pagination des arrays plus longs (couvert par F-160 + F-162 séparément)
- Augmentation de `max_tokens` Anthropic sur `CaseAnalysisService` (8192 reste suffisant — passer à 16384 systématique sera une optim séparée si on observe de la troncature niveau initial)
- Variation par pays (FR/BE) — la richesse est intrinsèque au dossier, pas au pays

---

## Technique

### Fichiers modifiés

- `backend/src/main/resources/application.yml` (3 blocs `dossier:` Travail/Immigration/Famille)
- `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisService.java` (template prompt ligne ~75)
- `backend/src/main/java/fr/ailegalcase/analysis/EnrichedAnalysisService.java` (template prompt ligne ~103)
- `backend/src/test/java/fr/ailegalcase/analysis/AnalysisJsonTruncatorTest.java` (ajout test cap 80)
- éventuellement `backend/src/test/java/fr/ailegalcase/analysis/CaseAnalysisServiceTest.java` (si fixture wording affectée)
- éventuellement `backend/src/test/java/fr/ailegalcase/analysis/EnrichedAnalysisServiceTest.java` (idem)

### Pattern de référence

`F-29` — externalisation des limites pipeline IA via `AnalysisLimitsProperties`. Cette SF pure data + prompt wording. Aucune classe Java structurelle modifiée.

### Endpoints / tables

Aucun changement.

### Migration Liquibase

Non applicable.

---

## Plan de test

### Tests unitaires (Jest / JUnit)

- [ ] `AnalysisJsonTruncatorTest` — nouveau test : `truncateCaseAnalysis` avec 80 faits + cap 80 → array intact ; 81 faits + cap 80 → tronqué à 80
- [ ] `CaseAnalysisServiceTest` (si présent) — vérifier que `buildSystemPrompt` produit bien la nouvelle formulation et inclut les nouvelles valeurs (assert `contains("jusqu'à 80 faits")` ou similaire)
- [ ] `EnrichedAnalysisServiceTest` (si présent) — symétrique

### Tests d'intégration

- [ ] Run complet `./mvnw test` (3000+ tests existants) — aucune régression
- [ ] Tests d'intégration pipeline IA si présents — non bloquants pour cette SF (la modif n'introduit pas de nouveau code logique)

### Isolation workspace

Non applicable (pas de touche au workspace_id).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non (F-15 budget tokens absorbe largement, +0,075 $/dossier)
- [ ] Navigation / routing — non
- [ ] Outil décisionnel métier — non
- [x] Aucune préoccupation transversale critique

### Smoke tests E2E

Aucun smoke test ne dépend des caps de synthèse. Pas de régression attendue.

### Coût IA

Estimation : +5 K output tokens en pic (dossiers extraordinaires) × 15 $/M = +0,075 $/dossier. Négligeable vs ARR cible (191 €/mois SOLO).

### Risques `max_tokens` (8192 initial)

Surveiller en staging sur 1-2 dossiers complexes après déploiement. Si troncature niveau initial observée, passer à 16384 dans une SF séparée (modification trivial dans `CaseAnalysisService.consumeAnalysis`).

---

## Dépendances

### Subfeatures bloquantes

Aucune. F-29 (externalisation des limites) est Terminée. F-160 (historique paginé) et F-162 (refonte synthèse) sont **indépendantes** — elles traiteront la pagination UI, pas la production backend.

---

## Notes et décisions

- **Décision** : caps identiques sur 3 domaines avec une seule variation `timeline: 80` pour Immigration. Évite la complexité config par domaine.
- **Décision** : document level inchangé. Les caps étroits par-doc sont voulus — l'agrégation richesse se fait au niveau dossier.
- **Décision** : on garde `max_tokens: 8192` initial. Le pipeline enriched (16384) absorbe déjà les dossiers complexes. Si on observe troncature, on passera initial à 16384 en SF séparée.
- **Décision** : reformulation prompt « jusqu'à N… pas de minimum — produis exactement ce que la richesse justifie » plutôt que « N maximum, sois concis ». Évite que l'IA bourre artificiellement pour atteindre la limite ou se censure sur dossiers riches.
- **Note** : F-161 ne crée pas de pagination front. Les pages synthèse seront plus longues. Fixé volontairement par F-162 (refonte) qui dépend de F-160 (historique paginé). Trade-off accepté — la valeur métier d'analyser un dossier complet > la longueur de la page.
