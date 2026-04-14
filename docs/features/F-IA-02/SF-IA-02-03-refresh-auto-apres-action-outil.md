# Mini-spec — F-IA-02 / SF-IA-02-03 Refresh automatique après action utilisateur dans un outil

## Identifiant

`F-IA-02 / SF-IA-02-03`

## Feature parente

`F-IA-02` — Tableau de bord décisionnel transversal

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-02-03-refresh-auto-apres-action-outil`

---

## Objectif

Faire en sorte que le tableau de bord décisionnel F-IA-02 présent en tête de la fiche dossier se rafraîchisse automatiquement après chaque action utilisateur dans un outil métier (clic sur « Résoudre », « Calculer », « Enregistrer »), sans que l'avocat ait à recharger la page.

Aujourd'hui, `CaseDashboardComponent` charge ses données une seule fois dans `ngOnInit()` via `GET /case-files/{id}/dashboard`. Les outils qui alimentent les cards (F-DT-07/08/09, F-FA-05/06/07, F-IM-05/06/07) n'émettent aucun signal, si bien que le dashboard reste figé tant que l'utilisateur ne recharge pas la page.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre la fiche dossier. Le dashboard charge ses cards (comportement inchangé).
2. L'avocat clique sur « Résoudre », « Calculer » ou « Enregistrer » dans un outil (ex. F-DT-09 Comparer, F-IM-05 Résoudre, F-FA-06 Calculer calendrier).
3. Une fois la réponse backend reçue avec succès, l'outil notifie le dashboard via un signal partagé.
4. Le dashboard relance `GET /case-files/{id}/dashboard` de manière silencieuse (sans spinner plein écran, mais les cards peuvent afficher un état subtil de mise à jour) et met à jour les cards impactées.
5. En cas d'erreur HTTP du dashboard refresh, on conserve l'ancien état affiché et on log côté console (sans snackbar intrusif — l'utilisateur verra l'effet au prochain refresh).

### Déclencheurs explicites

| Outil | Action qui déclenche un refresh |
|-------|--------------------------------|
| F-DT-07 Ancienneté | Clic sur « Calculer » (réponse 200) |
| F-DT-08 Validité licenciement | Clic sur « Analyser » (réponse 200) |
| F-DT-09 Comparateur indemnités | Clic sur « Comparer » (réponse 200) |
| F-FA-05 Partage immobilier | Clic sur « Enregistrer » / « Calculer » (réponse 200) |
| F-FA-06 Calendrier garde | Clic sur « Calculer » (réponse 200) |
| F-FA-07 Checklist divorce | Basculement de statut d'une étape ou d'une pièce (réponse 200) |
| F-IM-05 Titre séjour | Clic sur « Résoudre » (réponse 200) |
| F-IM-06 Recours | Clic sur « Enregistrer » / « Générer » (réponse 200) |
| F-IM-07 Droit au travail | Clic sur « Résoudre » (réponse 200) |

Un refresh est émis **uniquement après succès backend** (réponse HTTP 2xx). Les erreurs ne déclenchent pas de refresh — l'état affiché reste cohérent avec la dernière donnée validée.

### Anti-pattern à éviter

- Pas de refresh sur modification de champ sans validation (`ngModelChange`, sélection radio, etc.). L'utilisateur a validé un feedback utilisateur explicite : « pas en live sur champ, seulement après clic ».
- Pas de polling périodique (coûteux et inutile dans un usage solo).
- Pas de rechargement global de la fiche dossier — uniquement le dashboard F-IA-02.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Outil renvoie 4xx/5xx | Outil affiche son erreur propre, aucun refresh dashboard |
| Refresh dashboard échoue | Dashboard conserve son état précédent, erreur loggée console, aucun snackbar |
| L'utilisateur quitte la fiche pendant le refresh | Unsubscribe via `takeUntilDestroyed` ou équivalent — pas de fuite |
| Plusieurs refresh rapides (clics enchaînés) | Debounce 300 ms dans le dashboard pour coalescer les requêtes |

---

## Critères d'acceptation

- [ ] Service `CaseDashboardRefreshService` exposant un `Subject<void>` (ou `signal` triggerable) accessible par tous les composants enfants de la fiche dossier.
- [ ] `CaseDashboardComponent` s'abonne au service à `ngOnInit`, debounce 300 ms, et recharge ses cards à chaque émission.
- [ ] Les 9 outils listés injectent le service et appellent `triggerRefresh()` dans le `next:` de leur observable après une action validée (pas dans `error:`).
- [ ] Aucun refresh déclenché sur simple modification de champ non validée.
- [ ] Un refresh échouant ne met pas en erreur l'outil qui l'a déclenché.
- [ ] L'abonnement est proprement nettoyé quand le dashboard ou un outil est détruit.
- [ ] Tests unitaires Jest : le service émet, le dashboard recharge, chaque outil déclenche l'émission sur succès.
- [ ] Validation manuelle sur staging via Test 1 (Dupont) : modifier F-DT-09 type de rupture → calculer → dashboard mis à jour sans reload.

---

## Périmètre

### Hors scope (explicite)

- Refresh live sur modification de champ sans validation (exclu par la demande utilisateur).
- Refresh transversal entre dossiers (chaque dashboard est lié à un `caseFileId`).
- Refresh du dashboard global `/dashboard` (hors sujet).
- Ajout d'un endpoint WebSocket / SSE (inutile pour une UX solo-avocat).
- Optimisation de performance du `GET /dashboard` (actuel suffisant ; si latence problématique, backlog séparé).
- Intégration push depuis les alertes F-IA-03 (hors sujet — F-IA-03 est un mécanisme d'alerte, pas un producteur de données du dashboard).

---

## Valeurs initiales

Aucune donnée persistée. Le service est purement en mémoire, une instance par fiche dossier ouverte (injecté au niveau du `CaseFileDetailComponent` via providers locaux, pas au niveau root — sinon tous les dashboards partageraient le signal).

---

## Contraintes de validation

Aucune (pas d'input utilisateur).

---

## Technique

### Endpoints

Aucun endpoint nouveau ou modifié. `GET /api/v1/case-files/{id}/dashboard` est rejoué à chaque refresh.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- `CaseDashboardRefreshService` (nouveau) dans `frontend/src/app/case-files/case-dashboard/case-dashboard-refresh.service.ts`.
  - Expose `triggerRefresh(): void` et `readonly refresh$: Observable<void>`.
  - Implémentation : `Subject<void>` interne, `asObservable()` exposé.
- `CaseFileDetailComponent` : ajoute `CaseDashboardRefreshService` dans ses `providers` pour scoper l'instance au dossier courant.
- `CaseDashboardComponent` : injecte le service, s'abonne dans `ngOnInit` avec `debounceTime(300)` et `takeUntilDestroyed()`, recharge `dashboardService.get(caseFileId)` à chaque émission.
- 9 composants outils : injectent le service et appellent `this.refresh.triggerRefresh()` dans le callback `next` de leurs actions métier validées.

### Impact performance

- Un clic utilisateur = un appel `GET /dashboard` supplémentaire. Négligeable pour un backend staging/prod.
- Debounce 300 ms évite les bursts.

---

## Plan de test

### Tests unitaires frontend

- [ ] `CaseDashboardRefreshServiceTest` : l'émission via `triggerRefresh()` se propage aux abonnés.
- [ ] `CaseDashboardComponentTest` : `triggerRefresh()` provoque un nouvel appel à `CaseDashboardService.get()`.
- [ ] `CaseDashboardComponentTest` : débounce — 3 appels rapides → 1 seul `GET`.
- [ ] Pour 3 outils représentatifs (F-DT-09, F-IM-05, F-FA-06) : le clic validé déclenche `triggerRefresh()`. Pour les 6 autres, un smoke test minimal suffit (vérifie juste que l'injection + appel existent, pas la logique interne).
- [ ] Erreur HTTP dans l'outil → pas d'appel à `triggerRefresh()`.

### Tests d'intégration

- [ ] Non applicable (frontend pur).

### Validation manuelle

- [ ] Staging, Test 1 Dupont : action dans F-DT-09 → dashboard mis à jour visuellement.

### Isolation workspace

- [x] Non impactée — logique frontend locale, scoped par `caseFileId`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — ajout d'un service local au composant fiche dossier et d'un abonnement déjà scopé.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `CaseDashboardComponent` | Ajout d'un abonnement + debounce ; chargement initial inchangé | Smoke test navigation |
| 9 composants outils | Ajout d'une ligne `triggerRefresh()` dans chaque `next:` de handler — aucun changement de logique métier | Jest existants restent verts |
| `CaseFileDetailComponent` | Ajoute 1 entrée `providers: []` | Smoke test navigation |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — les fiches dossier doivent rester navigables sans régression. À relancer après dev.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IA-02-02` (Done) — composant dashboard existant.
- Tous les outils F-DT-07..09, F-FA-05..07, F-IM-05..07 existent et sont déployés.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi un service injecté par `CaseFileDetailComponent` et pas root** : on veut une instance par fiche dossier ouverte (onglets, navigation). Une instance root partagée émettrait pour tous les dashboards ouverts.
- **Pourquoi pas un `EventEmitter` parent** : nécessiterait de brancher explicitement chaque outil enfant à un handler du parent, plus verbeux que Subject injecté. Moins scalable si on ajoute d'autres outils.
- **Pourquoi pas un `signal` Angular** : `Subject` reste plus naturel pour un trigger transactionnel (« un événement ponctuel »). Un signal se justifierait pour un état, pas un bus d'événements. La contrainte `takeUntilDestroyed()` couvre le lifecycle.
- **Pourquoi debounce 300 ms** : protège contre les doubles clics / batch d'enregistrements sans altérer la perception utilisateur (< seuil perceptible).
- **Pourquoi pas de WebSocket/SSE** : pas de multi-utilisateur sur un même dossier en V1, pas de calcul asynchrone de dashboard. Une invalidation pull-on-demand suffit amplement.
