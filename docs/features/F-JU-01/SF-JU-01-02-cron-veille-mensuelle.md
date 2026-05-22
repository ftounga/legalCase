# Mini-spec — F-JU-01 / SF-JU-01-02 Cron veille mensuelle full auto-pilot

## Identifiant
`F-JU-01 / SF-JU-01-02`

## Feature parente
`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels — full auto-pilot Claude

## Statut
`draft`

## Date de création
2026-05-22

## Branche Git
`feat/SF-JU-01-02-cron-veille-mensuelle`

---

## Objectif

Cron mensuel `JurisprudenceWatchScheduler` qui interroge JUDILIBRE pour les arrêts publiés du mois, demande à Claude Sonnet d'évaluer leur impact sur chaque mapping actif de `tool_jurisprudence_mappings`, et applique automatiquement la décision selon le trust mode configuré (Paranoia / Équilibre / Auto-pilot par défaut) — avec garde-fous (seuil confiance, top-3, alerte > 5 % impact massif, email récap).

---

## Comportement attendu

### Cas nominal (Auto-pilot par défaut)

1. `@Scheduled(cron = "0 0 3 1 * *")` — 1er du mois à 3h UTC
2. Récupère depuis JUDILIBRE les arrêts publiés au Bulletin du **mois écoulé** (filtre `date_creation` ∈ [premier jour mois N-1, premier jour mois N])
3. Pour chaque mapping `tool_jurisprudence_mappings` non archivé :
   - Filtre les arrêts JUDILIBRE potentiellement impactants (par matière / chambre / mots-clés du chapeau actuel)
   - Appelle Claude Sonnet via `AnthropicService.analyze()` avec prompt structuré (mapping actuel + arrêts entrants → JSON `{action, arret_choisi, confidence_score, raison}`)
4. Selon confiance Claude :
   - `confidence ≥ 0.85` ET `action ∈ {ADD, REPLACE, CONFIRM, ARCHIVE}` → **auto-action** (Auto-pilot)
   - `confidence ∈ [0.60, 0.85)` → **flag PENDING** dans `jurisprudence_watch_flags`
   - `confidence < 0.60` → ignorer (silence)
5. Garde-fou « alerte massive » : si plus de **5 %** des mappings actifs ont reçu une auto-action `REPLACE` ou `ARCHIVE` dans le run → **suspension** des actions restantes + email « intervention requise ». Les actions déjà appliquées restent (l'audit log permet rollback ciblé).
6. Email mensuel récap au fondateur : compteurs `arrets_traités`, `auto_confirm`, `auto_add`, `auto_replace`, `auto_archive`, `flags_pending`, `contradictions_non_resolues` + lien vers `/super-admin/jurisprudence-watch`.

### Cas d'erreur

| Situation | Comportement | Trace |
|---|---|---|
| JUDILIBRE API timeout / 5xx | retry exponentiel × 3 ; si échec final, log WARN + run abandonné (pas de partial state) | `jurisprudence_audit_log` non écrit, email d'alerte |
| Claude API timeout / 5xx | retry × 2 par mapping ; en échec final, mapping skipped (audit log row `SKIPPED`) | continue avec mapping suivant |
| Parsing JSON Claude échoue | mapping skipped + log WARN | continue |
| > 5 % mappings touchés en 1 run | **suspension** actions restantes + email « intervention requise » | `jurisprudence_audit_log` `actor=CRON action=ABORTED` |
| Trust mode = `PARANOIA` | toutes décisions deviennent flags PENDING (aucune auto-action) | comportement attendu |

### Trust modes

3 valeurs de propriété `jurisprudence.watch.trust-mode` (défaut `AUTO_PILOT`) :

| Mode | Seuil auto-action | Comportement |
|---|---|---|
| `PARANOIA` | jamais | Toutes décisions → flag PENDING (revue humaine systématique) |
| `EQUILIBRE` | confidence ≥ 0.85 | Auto-pilot sur les cas évidents ; flag PENDING entre 0.60 et 0.85 |
| `AUTO_PILOT` (défaut) | confidence ≥ 0.85 | Identique à EQUILIBRE en V1 — différenciation V2 (ex. seuil 0.90 sur REPLACE) |

---

## Analyse de cohérence transversale

- [x] **Autres outils métier** : non applicable (transversal à tous les mappings de `tool_jurisprudence_mappings`)
- [x] **Autres pays** : FR (JUDILIBRE), BE (Juridat) **différé V2** — `JudilibreApiClient` aujourd'hui ; `JuridatApiClient` à ajouter quand le cron lit aussi des mappings BE
- [x] **Service partagé** : `JudilibreApiClient` réutilisable par SF-JU-01-05 (bootstrap auto) sans modification
- [x] **Préoccupations transversales** : aucune (cron interne, pas d'auth/workspace/plans/navigation modifié)
- [x] **Décision** : intégré dans cette SF — pas de SF parallèle ; BE différé V2 par signal terrain

---

## Conformité F-IA-04
- [x] **Non applicable** — SF backend pure (scheduler + client HTTP + service Claude). Aucun composant frontend.

## Champs IA à extraire (pré-remplissage)
- [x] **Aucun pré-remplissage** — pas d'outil décisionnel à champs saisissables.

---

## Critères d'acceptation

- [ ] **CA-01** — `JurisprudenceWatchScheduler` annoté `@Scheduled(cron = "0 0 3 1 * *", zone = "UTC")`, exécution conditionnée par `${jurisprudence.watch.enabled:false}` (défaut **false** — activation explicite en staging/prod).
- [ ] **CA-02** — `JudilibreApiClient.fetchArretsForPeriod(LocalDate startInclusive, LocalDate endExclusive): List<JudilibreArret>` :
   - OAuth2 client_credentials sur PISTE (`https://oauth.piste.gouv.fr/api/oauth/token`)
   - Récupération paginée des arrêts Cour de cassation + Conseil d'État
   - Retry exponentiel × 3 (backoff 2s/8s/30s) sur 5xx et timeout
   - Token mis en cache jusqu'à expiration (1h)
- [ ] **CA-03** — `ClaudeJurisprudenceEvaluator.evaluate(ToolJurisprudenceMapping mapping, List<JudilibreArret> candidates): ClaudeEvaluation` produit un record `{action, arret_choisi, confidence_score, raison}` ; prompt structuré avec system rule « réponds uniquement en JSON ».
- [ ] **CA-04** — `JurisprudenceWatchService.runMonthlyWatch()` orchestre : pull JUDILIBRE → boucle mappings → Claude eval → action selon trust mode → INSERT `jurisprudence_audit_log` + INSERT/UPDATE/ARCHIVE `tool_jurisprudence_mappings` + INSERT `jurisprudence_watch_flags` si PENDING.
- [ ] **CA-05** — Garde-fou « alerte massive » : si compteur `AUTO_REPLACE + AUTO_ARCHIVE > 5 %` des mappings actifs → suspension actions restantes + log audit `ABORTED` + email d'alerte.
- [ ] **CA-06** — Email mensuel récap envoyé au fondateur via `EmailService` existant : sujet `[LegalCase] Veille jurisprudentielle — <mois> <année>` + compteurs + lien dashboard `/super-admin/jurisprudence-watch`.
- [ ] **CA-07** — 3 trust modes paramétrables via `jurisprudence.watch.trust-mode={PARANOIA|EQUILIBRE|AUTO_PILOT}` (défaut `AUTO_PILOT`).
- [ ] **CA-08** — Tests UT sur `ClaudeJurisprudenceEvaluator` (parse JSON, fallback parsing échec, confiance bornée [0,1]), `JurisprudenceWatchService` (trust modes, alerte massive, retry échec, dispatching action), `JudilibreApiClient` (token caching, retry exponentiel, parsing pagination).
- [ ] **CA-09** — Pas de test IT (la vraie API PISTE nécessite un compte OAuth2 non disponible en CI ; les mocks HTTP couvrent le contrat).
- [ ] **CA-10** — Configuration `application.properties` : `jurisprudence.watch.enabled=false`, `jurisprudence.watch.trust-mode=AUTO_PILOT`, `jurisprudence.watch.alert-threshold-percent=5`, `judilibre.client-id=${JUDILIBRE_CLIENT_ID:}`, `judilibre.client-secret=${JUDILIBRE_CLIENT_SECRET:}`.

---

## Périmètre

### Hors scope (explicite)

- ❌ Belgique (Juridat) — V2 selon adoption FR
- ❌ Dashboard admin frontend `/super-admin/jurisprudence-watch` — SF-JU-01-05
- ❌ Bouton « Signaler » côté avocat utilisateur — SF-JU-01-04
- ❌ Bootstrap initial des mappings — SF-JU-01-05
- ❌ Création du compte OAuth2 PISTE côté ailegalcase — geste opérationnel hors code (à faire en parallèle, à documenter dans la PR)
- ❌ Cron dérive quotidienne (calculators vs mappings) — SF-JU-01-03

---

## Technique

### Endpoint(s) externes consommés

| Méthode | URL | Auth |
|---|---|---|
| POST | `https://oauth.piste.gouv.fr/api/oauth/token` | client_credentials |
| GET | `https://api.piste.gouv.fr/cassation/judilibre/v1.0/export` | Bearer |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `tool_jurisprudence_mappings` | SELECT + UPDATE + INSERT | Mise à jour `last_verified_at`, ajout / remplacement / archivage |
| `jurisprudence_watch_flags` | INSERT | Création flags PENDING quand confidence ambiguë |
| `jurisprudence_audit_log` | INSERT | Trace de toutes les actions auto + cas `SKIPPED` / `ABORTED` |

### Migration Liquibase
- [x] **Non applicable** — tables déjà créées en SF-JU-01-01.

### Classes Java introduites

Package `fr.ailegalcase.jurisprudencemapping` (existant) :
- `JurisprudenceWatchScheduler` — `@Scheduled` mensuel + activation conditionnée
- `JurisprudenceWatchService` — orchestrateur
- `JudilibreApiClient` — client HTTP avec OAuth2 + retry
- `JudilibreArret` (record) — DTO retour JUDILIBRE (`id`, `ref`, `juridiction`, `date`, `numero`, `chapeau`, `lien`)
- `JudilibreOAuthTokenProvider` — cache token OAuth2 ; expose `String currentToken()`
- `ClaudeJurisprudenceEvaluator` — wrapper `AnthropicService.analyze()` + prompt template + parsing JSON
- `ClaudeEvaluation` (record) — `{action: EvaluationAction, arretChoisi: JudilibreArret?, confidenceScore: BigDecimal, raison: String}`
- `EvaluationAction` (enum) — `CONFIRM`, `ADD`, `REPLACE`, `ARCHIVE`, `NONE`
- `TrustMode` (enum) — `PARANOIA`, `EQUILIBRE`, `AUTO_PILOT`
- `JurisprudenceWatchRunSummary` (record) — compteurs pour email récap
- `JurisprudenceWatchEmailService` — composition + envoi email mensuel

---

## Plan de test

### Tests unitaires

- `JudilibreApiClientTest` — token cache, parsing pagination, retry exponentiel sur 5xx, échec définitif
- `ClaudeJurisprudenceEvaluatorTest` — prompt construit, parsing JSON valide, parsing JSON corrompu (fallback NONE), confidence bornée [0,1]
- `JurisprudenceWatchServiceTest` — dispatch action selon trust mode (AUTO_PILOT, EQUILIBRE, PARANOIA), alerte massive > 5 %, mapping skipped sur échec Claude, audit log écrit, email récap appelé
- `JurisprudenceWatchSchedulerTest` — invocation `runMonthlyWatch()` conditionnée par `enabled=true`

### Tests d'intégration

- [x] **Aucun IT** sur cette SF — la vraie API PISTE nécessite un compte OAuth2 non disponible en CI. Les mocks HTTP des UT couvrent le contrat.

### Isolation workspace

- [x] **Non applicable** — cron transversal sur table globale.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune** — cron interne, pas de modif auth/workspace/plans/navigation.

### Smoke tests E2E concernés
- [x] Aucun — SF backend pure.

---

## Dépendances

### Subfeatures bloquantes
- SF-JU-01-01 (Done — PR #1218 mergée 2026-05-21).

### Subfeatures débloquées
- SF-JU-01-05 (réutilise `JudilibreApiClient` + `ClaudeJurisprudenceEvaluator` pour le bootstrap initial).

---

## Notes et décisions

1. **Email récap via `EmailService` existant** — pas de nouveau provider, pas de templates `.html` séparés (corps texte simple sur cette V1, HTML possible en V2 si signal).
2. **Trust modes EQUILIBRE ≈ AUTO_PILOT en V1** — différenciation seulement en V2 si besoin d'un seuil plus strict pour les REPLACE (qui sont les actions à risque).
3. **`jurisprudence.watch.enabled=false` par défaut** — activation explicite en staging puis en prod après vérification du compte OAuth2 PISTE configuré. Évite tout effet de bord si le cron part en boucle sur une mauvaise configuration.
4. **Pas de IT** — décision pragmatique : la vraie API PISTE n'est pas mockable en CI sans compte. Les mocks HTTP des UT garantissent le contrat client. Validation réelle en staging post-déploiement.
5. **Coût IA estimé** — ~200 arrêts/mois × 80 mappings filtrés = ~16 K évaluations Claude/mois. Au tarif Sonnet (~3 $/M tokens input, ~15 $/M tokens output) avec ~2 K tokens input + 100 tokens output par eval → ~32 M input + 1,6 M output → ~96 $ input + 24 $ output ≈ **120 $/mois LLM**. À optimiser en V2 (pré-filtre par embedding ou matière avant Claude). En V1, acceptable vs ARR.

### Coût estimé
- ~2 j dev backend.
