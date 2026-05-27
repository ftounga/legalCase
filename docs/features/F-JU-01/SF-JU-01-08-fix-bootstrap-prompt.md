# Mini-spec — F-JU-01 / SF-JU-01-08 Fix prompt bootstrap stérile

## Identifiant

`F-JU-01 / SF-JU-01-08`

## Feature parente

`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels (FR + BE) — full auto-pilot Claude

## Statut

`draft`

## Date de création

2026-05-27

## Branche Git

`feat/SF-JU-01-08-fix-bootstrap-prompt`

---

## Objectif

Corriger le bug observé en staging le 2026-05-27 où le bootstrap manuel des mappings jurisprudence ne persiste **0 mapping** parce que Claude répond systématiquement `NONE` quand on lui passe un `pseudoMapping` vide (cas du bootstrap initial sans mapping pré-existant).

---

## Comportement attendu

### Cas nominal

Quand `JurisprudenceBootstrapService.runBootstrap` appelle `ClaudeJurisprudenceEvaluator.evaluate` :

- Si `mapping.arretRef` commence par la sentinelle `"(bootstrap initial"` (cf. `JurisprudenceBootstrapService.pseudoMappingFromEntry` ligne 140), l'évaluateur utilise un **SYSTEM_PROMPT distinct** dédié au mode bootstrap initial.
- Le prompt bootstrap demande à Claude de choisir parmi `ADD | NONE` uniquement (CONFIRM/REPLACE/ARCHIVE n'ont pas de sens sans mapping existant), avec la consigne explicite de **sélectionner l'arrêt le plus structurant** parmi les candidats fournis (de préférence Cour de cassation, formation plénière, publication au Bulletin), et de ne renvoyer `NONE` **que** si aucun candidat n'est pertinent pour la branche calcul.
- Le SYSTEM_PROMPT existant reste utilisé pour le mode dérive (`CONFIRM/ADD/REPLACE/ARCHIVE/NONE`), exécuté depuis les crons SF-JU-01-02/03.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Claude indisponible / timeout | Log WARN, action = NONE, entrée skipped (comportement actuel inchangé) | n/a (interne) |
| Réponse Claude non parsable | Log WARN, action = NONE, entrée skipped (comportement actuel inchangé) | n/a (interne) |
| Aucun candidat JUDILIBRE pour l'entrée | Log INFO, entrée skipped (comportement actuel inchangé) | n/a (interne) |
| Candidats présents mais Claude renvoie `NONE` en mode bootstrap | Log INFO « pas d'arrêt structurant trouvé », entrée skipped | n/a (interne) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — non applicable, le fix touche un service interne F-JU-01, pas un outil décisionnel
- [x] **Autres pays** — non applicable, le SYSTEM_PROMPT est langue-agnostique (cible JUDILIBRE = FR + BE via Cour de cassation)
- [x] **Autres domaines** — non applicable, F-JU-01 couvre les 3 domaines de manière uniforme
- [x] **Autres UI patterns** — non applicable, backend pur
- [x] **Autres flows transversaux** — non applicable, pas de modification auth/workspace/plans/navigation

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — SF backend pure (correction d'un service existant, pas de nouvel outil).

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — le SYSTEM_PROMPT est local à `ClaudeJurisprudenceEvaluator`, pas un pattern à réutiliser ailleurs.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `JurisprudenceVeilleCronService` (SF-JU-01-02) | Oui | Inchangé — utilise toujours le SYSTEM_PROMPT « dérive » historique sur des mappings existants |
| `JurisprudenceDeriveCronService` (SF-JU-01-03) | Oui | Inchangé — idem |
| `JurisprudenceBootstrapService` (SF-JU-01-05) | Oui | **Couvert dans cette SF** — bascule sur SYSTEM_PROMPT_BOOTSTRAP via détection sentinelle |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s)
- [ ] Backlog VN
- [ ] Non applicable aux autres cibles

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend pure, aucun composant Angular touché, aucun changement de `TOOL_REGISTRY` ou de visibility rule.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF backend pure, ne touche aucun outil décisionnel à champs saisissables. Le seul appel IA est un appel à Claude pour évaluer des arrêts JUDILIBRE — c'est un usage interne F-JU-01.

---

## Critères d'acceptation

- [ ] Un test unitaire `ClaudeJurisprudenceEvaluatorTest` couvre le chemin « mapping existant » et vérifie que `SYSTEM_PROMPT` historique est envoyé à `AnthropicService`.
- [ ] Un test unitaire couvre le chemin « bootstrap initial » (mapping dont `arretRef` commence par `"(bootstrap initial"`) et vérifie que `SYSTEM_PROMPT_BOOTSTRAP` est envoyé à `AnthropicService`.
- [ ] Le `SYSTEM_PROMPT_BOOTSTRAP` autorise explicitement `ADD | NONE` uniquement, et demande de sélectionner l'arrêt le plus structurant parmi les candidats.
- [ ] Le contrat `ClaudeEvaluation` reste inchangé (pas de migration de schéma ni de changement de `EvaluationAction`).
- [ ] Smoke test E2E `e2e/smoke/` non concerné — aucun parcours utilisateur visible touché (cf. section dédiée plus bas).
- [ ] Sécurité : aucun changement d'auth / d'autorisation. L'endpoint `POST /api/admin/jurisprudence/bootstrap` reste `SUPER_ADMIN` uniquement (inchangé).
- [ ] Backward compat : le mode dérive (cron veille mensuelle, cron dérive quotidienne) continue d'utiliser l'ancien prompt sans changement de comportement.

---

## Périmètre

### Hors scope (explicite)

- **Transactions par-entrée** : le `@Transactional` global de `runBootstrap` est conservé tel quel. Le découpage en transaction par-entrée + reprise sur erreur est traité par **SF-JU-01-09** (Backlog).
- **Bootstrap async + polling status** : le côté synchrone HTTP (toast 50x après timeout NGINX 120s) n'est pas corrigé ici. Traité par **SF-JU-01-10** (Backlog).
- **Évolution de `EvaluationAction`** : pas de nouvelle action enum. On reste sur les 5 valeurs existantes ; le prompt bootstrap restreint juste les choix.
- **Modification de la sentinelle `"(bootstrap initial — pas de mapping actuel)"`** : on s'appuie sur la string actuelle de `JurisprudenceBootstrapService.pseudoMappingFromEntry`.

---

## Valeurs initiales

Non applicable — aucune nouvelle entité, aucun nouvel état.

---

## Contraintes de validation

Non applicable — pas de nouveau champ saisissable.

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau. L'endpoint existant `POST /api/admin/jurisprudence/bootstrap` (déclaré dans `JurisprudenceWatchAdminController.java`) est inchangé en surface (request/response identiques).

### Tables impactées

Aucune. Pas de migration Liquibase.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Composants Angular (si applicable)

Aucun. Frontend non touché.

### Détail des modifications backend

| Fichier | Modification |
|---------|--------------|
| `backend/.../ClaudeJurisprudenceEvaluator.java` | Ajout `SYSTEM_PROMPT_BOOTSTRAP` (constante). Méthode `evaluate` détecte `mapping.arretRef.startsWith(BOOTSTRAP_MAPPING_PREFIX)` et bascule sur le prompt approprié. Extraction d'une méthode privée `pickSystemPrompt(mapping)`. |
| `backend/.../JurisprudenceBootstrapService.java` | Extraction de la constante `BOOTSTRAP_MAPPING_PREFIX = "(bootstrap initial"` (visibilité package, importée par l'évaluateur) pour éviter le couplage par littéral string. |
| `backend/.../ClaudeJurisprudenceEvaluatorTest.java` (nouveau) | Tests Mockito sur `AnthropicService` mockée — capture du `systemPrompt` passé pour valider la bascule. |

---

## Plan de test

### Tests unitaires

- [ ] `ClaudeJurisprudenceEvaluatorTest.shouldUseDeriveSystemPromptWhenMappingExists` — passe un mapping avec `arretRef` réel → vérifie que `AnthropicService.analyze` est appelée avec le `SYSTEM_PROMPT` historique (assertion sur le contenu via `ArgumentCaptor<String>`).
- [ ] `ClaudeJurisprudenceEvaluatorTest.shouldUseBootstrapSystemPromptWhenMappingIsSentinel` — passe un mapping `pseudoMappingFromEntry` (sentinelle) → vérifie que `AnthropicService.analyze` est appelée avec `SYSTEM_PROMPT_BOOTSTRAP` (assertion sur le contenu).
- [ ] `ClaudeJurisprudenceEvaluatorTest.shouldReturnAddActionWhenBootstrapModeClaudeReplyValid` — Claude renvoie `{"action":"ADD","arret_choisi_id":"X",...}` → vérifie que `EvaluationAction.ADD` est retourné avec le bon `JudilibreArret`.
- [ ] `ClaudeJurisprudenceEvaluatorTest.shouldReturnNoneWhenBootstrapClaudeReplyNone` — Claude renvoie `{"action":"NONE",...}` → vérifie `EvaluationAction.NONE` (fallback safe inchangé).

### Tests d'intégration

Pas de test d'intégration nouveau. Les ITs existants de `JurisprudenceBootstrapServiceIT` (si présents — à vérifier) restent inchangés ; sinon, pas de nouveau IT pour cette SF (la valeur du fix est sur la couche prompt, déjà couverte par les unitaires).

### Isolation workspace

- [ ] Applicable
- [x] Non applicable — raison : F-JU-01 est globale (mappings communs à tous les workspaces, gérés par SUPER_ADMIN).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à `ClaudeJurisprudenceEvaluator` et `JurisprudenceBootstrapService`.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `JurisprudenceVeilleCronService` (SF-JU-01-02) | Doit continuer d'utiliser le prompt dérive — la détection sentinelle ne doit jamais matcher un vrai mapping | Test unitaire `shouldUseDeriveSystemPromptWhenMappingExists` |
| `JurisprudenceDeriveCronService` (SF-JU-01-03) | Idem | Idem |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — justification : pas de parcours utilisateur frontend visible touché. Le seul appelant frontend (panneau super-admin) consomme la même API qu'avant, sans changement de contrat HTTP.

---

## Dépendances

### Subfeatures bloquantes

- `SF-JU-01-05` — statut : done (livraison du `JurisprudenceBootstrapService`).
- `SF-JU-01-06` — statut : done (bouton bootstrap dashboard).
- `SF-JU-01-07` — statut : done (upload CSV).

### Questions ouvertes impactées

- [x] Aucune question ouverte (`docs/OPEN_QUESTIONS.md`) impactée.

---

## Notes et décisions

- **Bug observé** 2026-05-27 ~00:56 UTC en staging : bootstrap de 200 entrées via dashboard `/super-admin/jurisprudence-watch`. NGINX timeout à 120s côté front (toast 50x trompeur) mais backend continue ; 11 entrées traitées en 5 min, **aucune transaction PG ouverte** côté `pg_stat_activity` → confirmation que tous les `mappingRepository.save()` étaient absents → tous skipped via la garde `evaluation.action() == NONE` ligne 75 de `JurisprudenceBootstrapService.java`.
- **Décision pragmatique** : conserver le `@Transactional` global pour ne pas mélanger les concerns. Le passage en tx-par-entrée (SF-09) et l'async (SF-10) sont gardés au backlog pour des SF dédiées avec leur propre cadrage.
- **Anti-régression majeure** : on extrait la sentinelle dans une constante partagée pour éviter qu'un futur refactor de `pseudoMappingFromEntry` casse silencieusement la détection côté évaluateur.
