# Mini-spec — F-JU-01 / SF-JU-01-06 Bouton bootstrap dans le dashboard admin

## Identifiant

`F-JU-01 / SF-JU-01-06`

## Feature parente

`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels (FR + BE) — full auto-pilot Claude

## Statut

`draft`

## Date de création

2026-05-24

## Branche Git

`feat/SF-JU-01-06-bouton-bootstrap-dashboard`

---

## Objectif

Ajouter un onglet « Bootstrap » dans `/super-admin/jurisprudence-watch` qui permet au super-admin de déclencher l'endpoint `POST /api/v1/super-admin/jurisprudence-watch/bootstrap` depuis l'UI au lieu d'un `curl` externe.

---

## Comportement attendu

### Cas nominal

1. Super-admin ouvre `/super-admin/jurisprudence-watch`
2. Sélectionne le nouvel onglet **« Bootstrap »** (placé en 1er — cf. cadrage 0 bis invariant lisibilité séquence)
3. L'écran présente :
   - Un `<textarea>` multi-lignes au format CSV `toolId,brancheCalculId,motCleRecherche[,juridictionFiltre[,dateMin]]` (1 entrée par ligne, 200 max)
   - Un bouton `[Exemple]` qui pré-remplit le textarea avec 3 lignes types (pour montrer le format)
   - Un compteur live `N / 200 entrées détectées` mis à jour à chaque saisie
   - Un bouton `[Lancer le bootstrap]` désactivé si 0 ou > 200 entrées, ou requête en cours
4. Au clic sur `[Lancer]` : POST vers `/bootstrap` avec le payload parsé, bouton désactivé + label « En cours… »
5. Réponse OK : snackBar succès `Bootstrap terminé : X processed, Y created, Z skipped (Wms)`, le textarea reste rempli pour permettre un re-lancement amendé, le bouton se réactive, l'onglet Audit log est rechargé en arrière-plan (`loadAudit()`)
6. Réponse KO (4xx/5xx/timeout) : snackBar erreur `Échec du bootstrap : <message HTTP>`, le bouton se réactive

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Textarea vide ou whitespace seul | Bouton désactivé (pas d'appel) | — |
| Ligne malformée (< 3 colonnes ou toolId non `^[a-zA-Z0-9_-]{1,100}$`) | SnackBar `Ligne N invalide : …` au clic, pas d'appel HTTP | — |
| > 200 lignes parsées | Bouton désactivé + helper text rouge `Max 200 entrées par batch` | — |
| Backend retourne 400 (validation Bean) | SnackBar erreur avec message API | 400 |
| Backend retourne 403 (non super-admin) | SnackBar `Accès refusé — compte non super-admin` | 403 |
| Backend retourne 500 ou timeout | SnackBar `Échec du bootstrap : <message>` | 500 |
| Workspace différent (non applicable — endpoint super-admin global) | — | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable : cette SF n'est pas un outil décisionnel avocat, c'est un écran ops super-admin. Pas de propagation à F-DT/F-FA/F-IM.
- [x] **Autres pays** — Non applicable : écran ops global, pas de gate FR/BE.
- [x] **Autres domaines** — Non applicable : même raison.
- [x] **Autres UI patterns** — Le pattern « 3ème tab dans un dashboard admin » est unique au sein de `/super-admin/*`. Aucune réutilisation prévue.
- [x] **Autres flows transversaux** — Touche uniquement `JurisprudenceWatchComponent` et son service. Pas d'impact auth (réutilise `SuperAdminGuard` existant), workspace, plans ou navigation (route déjà existante).

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** — nouveau DTO `JurisprudenceBootstrapEntry` + `JurisprudenceBootstrapRequest` + `JurisprudenceBootstrapResponse` côté frontend (miroir du backend existant SF-JU-01-05).
- [x] **Record / DTO backend** — déjà livré SF-JU-01-05, aucune modification.
- [x] **Service / logique métier** — frontend uniquement : nouvelle méthode `triggerBootstrap(entries)` dans `JurisprudenceWatchAdminClientService` ; backend inchangé.
- [x] **Entité JPA + schéma DB** — aucun changement (`tool_jurisprudence_mappings` + `jurisprudence_audit_log` peuplées par l'endpoint backend existant).
- [x] **Tests existants** — `jurisprudence-watch.component.spec.ts` existe (4 tests T-01 à T-04), à étendre avec les tests de la SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

**Non applicable** — cette SF n'est pas un outil décisionnel (pas de section `<app-XXX-section>` consommée par un avocat sur un dossier, pas de formulaire métier avec pré-fill IA). C'est un écran ops super-admin.

### Cas spécifique : nouveau pattern UI ou service partagé

**Non applicable** — pas de composant `shared/` introduit, pas de service applicatif transversal. Tout reste local au feature `/super-admin/jurisprudence-watch`. Le parseur CSV est un helper local privé (`parseCsvToEntries()`), non exporté.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils décisionnels avocat (F-DT/F-FA/F-IM) | Non | Écran ops super-admin, pas un outil métier |
| Pays FR/BE | Non | Endpoint backend déjà global |
| Domaines métier | Non | Idem |
| Pattern UI shared | Non | Tab Material standard, helper local |
| Auth/Workspace/Plans/Nav | Non | Réutilise `SuperAdminGuard` + route existants |

### Décision

- [x] Non applicable aux autres cibles (justification explicite : SF d'outillage ops sur écran super-admin existant, pas de chaîne de propagation)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : cette SF n'est pas une section décisionnelle (pas d'`app-XXX-section`, pas d'instanciation via `TOOL_REGISTRY`, pas de pré-fill IA depuis l'analyse d'un dossier). C'est un onglet ajouté à un dashboard super-admin existant.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : pas de champs IA pertinents pour un outil ops manuel. Le `motCleRecherche` est libre, à la main du super-admin.

---

## Critères d'acceptation

- [ ] **CA-01** : un 3ème onglet `mat-tab label="Bootstrap" data-test="tab-bootstrap"` est présent dans `JurisprudenceWatchComponent`, placé en 1ère position devant « Flags à arbitrer » et « Audit log »
- [ ] **CA-02** : le textarea CSV accepte une saisie multi-lignes ; un compteur `{{ parsedEntries.length }} / 200 entrées` s'affiche au-dessus
- [ ] **CA-03** : le bouton `[Lancer le bootstrap]` est désactivé tant que `parsedEntries.length === 0 || parsedEntries.length > 200 || loadingBootstrap === true`
- [ ] **CA-04** : un bouton `[Exemple]` pré-remplit le textarea avec 3 lignes types `f-dt-07,anciennete-licenciement,ancienneté préavis indemnité` (etc.)
- [ ] **CA-05** : au clic Lancer, `POST /api/v1/super-admin/jurisprudence-watch/bootstrap` est appelé avec le payload `{ entries: parsedEntries }`
- [ ] **CA-06** : succès → snackbar `Bootstrap terminé : X processed, Y created, Z skipped (Wms)` + `loadAudit()` invoqué
- [ ] **CA-07** : erreur HTTP → snackbar `Échec du bootstrap : <message>` et bouton réactivé
- [ ] **CA-08** : ligne CSV malformée (regex `^[a-zA-Z0-9_-]{1,100}$` ko sur toolId/brancheCalculId, ou < 3 colonnes) → snackbar `Ligne N invalide` + pas d'appel HTTP
- [ ] **CA-09** : isolation rôle — l'endpoint backend rejette déjà avec 403 si non super-admin (test backend SF-JU-01-05 existant). Pas de gate frontend nouveau requis (route déjà protégée par `SuperAdminGuard`).

---

## Périmètre

### Hors scope (explicite)

- Pas de génération automatique du `motCleRecherche` depuis les labels d'outils ou `TOOL_REGISTRY` (V2 si besoin émerge — V1 = saisie manuelle / paste depuis Google Sheet)
- Pas de progress bar serveur, pas de polling — endpoint synchrone, on attend la réponse
- Pas d'historique des batches précédents côté frontend — l'audit log backend est la source de vérité
- Pas de bouton « Cancel » pendant la requête (pas d'endpoint backend)
- Pas de modification du backend : tous les endpoints existent depuis SF-JU-01-05

---

## Valeurs initiales

Pas de nouvelle entité créée par cette SF. Le bootstrap appelle l'endpoint backend qui INSERT dans `tool_jurisprudence_mappings` et `jurisprudence_audit_log` (logique déjà livrée SF-JU-01-05).

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| ligne CSV | Oui | — | ≥ 3 colonnes séparées par `,` après trim() | Non | `trim()` sur chaque colonne |
| toolId (col 1) | Oui | 100 | regex `^[a-zA-Z0-9_-]{1,100}$` (miroir du backend) | Non | — |
| brancheCalculId (col 2) | Oui | 100 | regex `^[a-zA-Z0-9_-]{1,100}$` | Non | — |
| motCleRecherche (col 3) | Oui | 500 | non vide après trim() | Non | `trim()` |
| juridictionFiltre (col 4) | Non | 50 | texte libre | Non | `trim()` |
| dateMin (col 5) | Non | 10 | ISO `YYYY-MM-DD` | Non | — |

Notes :
- La validation côté frontend reproduit les contraintes Bean Validation du backend (cf. `JurisprudenceBootstrapEntry.java`) pour éviter un aller-retour HTTP 400 sur chaque erreur.
- Le backend reste source de vérité ; le frontend filtre juste les erreurs évidentes.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/super-admin/jurisprudence-watch/bootstrap` | Oui (OAuth) | SUPER_ADMIN |

**Aucun nouvel endpoint backend** — exposition d'un endpoint déjà livré SF-JU-01-05.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `tool_jurisprudence_mappings` | INSERT | Indirectement via l'endpoint backend (logique déjà livrée) |
| `jurisprudence_audit_log` | INSERT | Idem (lignes `AUTO_ADD` actor `SUPER_ADMIN`) |

### Migration Liquibase

- [x] Non applicable

### Composants Angular (si applicable)

- `JurisprudenceWatchComponent` — étendu avec :
  - propriétés mutables `csvInput`, `parseResult`, `loadingBootstrap`, `lastBootstrapResult` (cohérence avec le pattern existant du composant — propriétés + `markForCheck()` dans `subscribe()`)
  - méthodes `loadExample()`, `runBootstrap()`, `onCsvInputChange(value)`, `canLaunchBootstrap()`
  - nouveau template `mat-tab label="Bootstrap"` en 1ère position
- `JurisprudenceWatchAdminClientService` — nouvelle méthode `triggerBootstrap(entries: JurisprudenceBootstrapEntry[]): Observable<JurisprudenceBootstrapResponse>`
- Nouveaux types TS `JurisprudenceBootstrapEntry`, `JurisprudenceBootstrapResponse` dans le service
- Helper exporté `parseBootstrapCsv(input: string): BootstrapParseResult` (exporté pour faciliter les tests unitaires dédiés)

---

## Plan de test

### Tests unitaires

- [ ] `JurisprudenceWatchAdminClientService.triggerBootstrap` — POST le bon payload sur la bonne URL (mock HttpClient)
- [ ] `parseCsvToEntries` — cas nominal 3 colonnes, 5 colonnes (avec juridictionFiltre + dateMin), ligne vide ignorée, ligne malformée capturée dans `errors`

### Tests d'intégration

Tests Jest sur le composant (`jurisprudence-watch.component.spec.ts`) :

- [ ] **T-05** — onglet Bootstrap est rendu et porte `data-test="tab-bootstrap"`
- [ ] **T-06** — `loadExample()` remplit `csvInput` avec un échantillon non vide ; le compteur `parsedEntries.length` reflète immédiatement
- [ ] **T-07** — `runBootstrap()` avec entries valides appelle `client.triggerBootstrap` puis `client.listAuditLog` et affiche le snackbar succès
- [ ] **T-08** — `runBootstrap()` avec erreur HTTP (throwError) affiche un snackbar `Échec du bootstrap`
- [ ] **T-09** — bouton désactivé si `parsedEntries.length === 0` ou `> 200`
- [ ] **T-10** — ligne CSV malformée (toolId avec caractère interdit) → erreur côté parsing, snackbar invalid

### Isolation workspace

- [x] **Non applicable** — endpoint super-admin global (pas de workspace_id en jeu).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — la SF étend un composant super-admin existant. Pas de changement d'auth (réutilise `SuperAdminGuard` déjà câblé sur la route), pas de changement workspace, pas de plan, pas de routing.

### Composants / endpoints existants potentiellement impactés

Aucun. Le composant `JurisprudenceWatchComponent` est l'unique consommateur des endpoints `/super-admin/jurisprudence-watch/*`. Le nouveau tab est additif.

### Smoke tests E2E concernés

- [x] **Aucun smoke test concerné** — les smoke E2E couvrent les parcours avocat (auth, workspace, navigation principale). Aucun smoke test ne couvre `/super-admin/*` à date. Test manuel post-deploy staging suffira (voir release checklist).

---

## Dépendances

### Subfeatures bloquantes

- `SF-JU-01-05` — **done** (livré PR #1232 — fournit l'endpoint backend `POST /bootstrap`, le composant `JurisprudenceWatchComponent`, le service `JurisprudenceWatchAdminClientService` et la route `/super-admin/jurisprudence-watch`)
- Commit ops `cccc2779` — **done** (active `JURISPRUDENCE_WATCH_ENABLED=true` + secrets `JUDILIBRE_CLIENT_ID/SECRET` en staging, condition de fonctionnement réel du bootstrap)

### Questions ouvertes impactées

- [x] Aucune. Sujet pleinement tranché : V1 = saisie CSV manuelle, V2 éventuelle = génération auto depuis `TOOL_REGISTRY` (hors scope, à ré-évaluer si la friction est ressentie).

---

## Notes et décisions

- Le format CSV brut (sans génération auto) a été retenu pour V1 dans le cadrage 0 bis (invariant #2). Un super-admin qui lance 480 mappings sait paste depuis un Google Sheet où chaque colonne est calculée. Réduit le scope de la SF à ~100 lignes de code.
- L'ordre des tabs `[Bootstrap | Flags | Audit log]` reflète la séquence logique d'usage (alimenter → arbitrer → vérifier). Imposé par le cadrage 0 bis (invariant lisibilité séquence).
- Le composant existant utilise des propriétés mutables + `markForCheck()` dans `subscribe()` (pattern SF-JU-01-05). La SF garde ce pattern par cohérence interne au fichier, plutôt qu'introduire des signals partiellement — un refactor signals-only relèverait d'une SF technique distincte. Cf. mémoire `feedback_onpush_subscribe_markforcheck` (motif appliqué : `markForCheck()` dans tous les `next/error` qui mutent des propriétés affichées).
