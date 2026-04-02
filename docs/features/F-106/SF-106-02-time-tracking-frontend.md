# Mini-spec — F-106 / SF-106-02 — Suivi du temps facturable — frontend timer

## Identifiant
`F-106 / SF-106-02`

## Feature parente
`F-106` — Suivi du temps facturable par dossier

## Statut
`draft`

## Date de création
2026-04-02

## Branche Git
`feat/SF-106-02-time-tracking-frontend`

---

## Objectif

Intégrer un widget timer start/stop dans la page dossier (`case-file-detail`) permettant à l'avocat de mesurer le temps passé sur un dossier, et afficher la liste des sessions du jour.

---

## Comportement attendu

### Cas nominal

**Widget timer (dans case-file-detail)**
- Un bouton "Démarrer le chrono" est visible en haut de la page dossier.
- Clic "Démarrer" → appel `POST .../time-entries/start` → le bouton bascule en "Arrêter" avec un chronomètre live (HH:MM:SS) qui s'incrémente chaque seconde.
- Clic "Arrêter" → appel `POST .../time-entries/{id}/stop` → le chrono se réinitialise, la session apparaît dans la liste en dessous.
- Si un timer est déjà actif sur CE dossier au chargement de la page → le widget reprend le chrono en cours (récupération via `GET .../time-entries`, filtre `stoppedAt = null`).
- Si un timer est actif sur UN AUTRE dossier → bouton "Démarrer" désactivé avec tooltip "Un timer est déjà actif sur un autre dossier".

**Liste des sessions**
- Sous le widget, une liste des sessions du dossier (toutes, pas seulement le jour) triées par `startedAt DESC`.
- Chaque ligne affiche : date, durée formatée (ex. "1h 23min"), statut (En cours / Terminé).
- Maximum 10 entrées affichées, sans pagination (suffisant pour V4).

**Configuration taux horaire**
- Dans `/workspace/settings` (écran existant), une section "Facturation" avec un champ "Taux horaire (€/h)".
- Affiche le taux actif. Bouton "Enregistrer" → appel `PUT /api/v1/workspace/billing-rate`.
- Feedback succès via MatSnackBar.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Timer déjà actif sur un autre dossier (409) | Toast erreur : "Un timer est déjà actif sur un autre dossier. Arrêtez-le d'abord." |
| Perte de connexion pendant le chrono | Le chrono continue côté client. À la reconnexion, stop normal. |
| Taux horaire invalide (≤ 0) | Erreur inline sous le champ |

---

## Critères d'acceptation

- [ ] Bouton start/stop visible dans case-file-detail
- [ ] Chrono live (HH:MM:SS) pendant que le timer est actif
- [ ] Reprise automatique du timer actif au rechargement de la page
- [ ] Bouton désactivé si timer actif sur un autre dossier (tooltip explicatif)
- [ ] Liste des sessions sous le widget (max 10, triées par date desc)
- [ ] Section "Facturation" dans /workspace/settings avec champ taux horaire
- [ ] Feedback MatSnackBar au save du taux
- [ ] Couleurs et composants conformes au design system

---

## Périmètre

### Hors scope
- Écran rapport mensuel (SF-106-03)
- Insight IA dans la synthèse (SF-106-04)
- Modification / suppression manuelle d'une session
- Notifications "timer oublié ouvert"

---

## Technique

### Composants Angular

- `TimerWidgetComponent` (standalone) — bouton start/stop + chrono live + liste sessions. Intégré dans `CaseFileDetailComponent`.
- `TimeService` — appels API start/stop/list, gestion de l'état timer actif (signal), interval chrono.
- Section "Facturation" dans le composant `WorkspaceSettingsComponent` existant (ou équivalent).
- `BillingRateService` (frontend) — GET/PUT `/api/v1/workspace/billing-rate`.

### Endpoints consommés (SF-106-01)
- `POST /api/v1/case-files/{id}/time-entries/start`
- `POST /api/v1/time-entries/{id}/stop`
- `GET /api/v1/case-files/{id}/time-entries`
- `GET /api/v1/workspace/billing-rate`
- `PUT /api/v1/workspace/billing-rate`

### Migration Liquibase
- [x] Non applicable (backend SF-106-01 déjà mergé)

---

## Plan de test

### Tests unitaires

- [ ] `TimeService` — startTimer() met à jour le signal activeEntry
- [ ] `TimeService` — stopTimer() efface le signal activeEntry
- [ ] `TimeService` — formatDuration() : 3661s → "1h 01min 01s"
- [ ] `TimerWidgetComponent` — bouton "Démarrer" visible par défaut
- [ ] `TimerWidgetComponent` — bouton bascule en "Arrêter" après start
- [ ] `TimerWidgetComponent` — liste des sessions affichée après stop

### Isolation workspace
- [ ] Non applicable — les appels API vérifient l'isolation côté backend (SF-106-01)

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Navigation / routing frontend** — ajout d'une section dans /workspace/settings

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| `CaseFileDetailComponent` | Ajout du `TimerWidgetComponent` — pas de modification des sections existantes | Tests existants doivent rester verts |
| `WorkspaceSettingsComponent` | Ajout section "Facturation" | Tests existants doivent rester verts |

### Smoke tests E2E concernés
- [ ] `e2e/smoke/navigation.spec.ts` — routes protégées toujours fonctionnelles
- [ ] `e2e/smoke/auth.spec.ts` — login non impacté

---

## Dépendances

### Subfeatures bloquantes
- SF-106-01 — statut : done ✅

---

## Notes et décisions

- **Signal Angular** pour l'état du timer actif : `activeEntry = signal<TimeEntryResponse | null>(null)`. Le chrono est un `setInterval` géré dans `ngOnInit` / `ngOnDestroy`.
- **Reprise du timer** : au chargement, `GET .../time-entries` filtre la première entrée sans `stoppedAt`. Si trouvée → activeEntry initialisé avec `startedAt` de l'entrée existante.
- **Détection "autre dossier"** : si le 409 est reçu, le message du backend indique qu'un timer est actif → désactiver le bouton sur ce dossier et afficher le tooltip.
