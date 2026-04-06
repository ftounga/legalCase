# Mini-spec — F-113 / SF-113-03 Frontend : icône cloche + panneau notifications

---

## Identifiant

`F-113 / SF-113-03`

## Feature parente

`F-113` — Centre de notifications in-app

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-113-03-notification-center-ui`

---

## Objectif

Ajouter une icône cloche avec badge unread dans le header et un panneau dropdown listant les notifications récentes, avec marquage lu au clic et "Tout marquer lu".

---

## Comportement attendu

### Cas nominal

1. Icône `notifications` dans le header (entre le spacer et le menu utilisateur) avec `MatBadge` affichant le nombre de non-lues
2. Clic sur l'icône ouvre un panneau dropdown (mat-menu) listant les 20 dernières notifications
3. Chaque notification affiche : icône type, titre, message (tronqué), date relative
4. Clic sur une notification : marque comme lue + navigue vers le lien associé
5. Bouton "Tout marquer lu" en haut du panneau
6. Polling toutes les 60 secondes pour rafraîchir le unread count
7. Le badge disparaît quand count = 0

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| API inaccessible | Badge masqué, panneau affiche message d'erreur léger |
| Aucune notification | Panneau affiche "Aucune notification" |

---

## Critères d'acceptation

- [ ] Icône cloche visible dans le header avec MatBadge
- [ ] Badge affiche le unread count, masqué si 0
- [ ] Panneau dropdown affiche les notifications paginées
- [ ] Clic notification → markRead + navigation
- [ ] Bouton "Tout marquer lu" → markAllRead + refresh
- [ ] Polling 60s du unread count
- [ ] NotificationService Angular créé (HTTP client)
- [ ] Tests du composant et du service
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope

- Préférences de notification
- Son/vibration
- Notifications push browser

---

## Technique

### Fichiers créés

- `NotificationService` — service Angular HTTP (GET /notifications, GET /unread-count, PATCH read, PATCH read-all)
- `NotificationCenterComponent` — icône + panneau dropdown

### Fichiers modifiés

- `ShellComponent` — intégration du NotificationCenterComponent dans le header

---

## Plan de test

- [ ] NotificationService — getNotifications, getUnreadCount, markRead, markAllRead
- [ ] NotificationCenterComponent — affiche badge, ouvre panneau, marque lu
- [ ] Tests existants restent verts

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] **Navigation / routing frontend** — ajout d'un composant dans le header shell
- [ ] Aucune préoccupation transversale

### Composants impactés

| Composant | Impact | Test |
|-----------|--------|------|
| ShellComponent | Ajout NotificationCenterComponent dans le template | Tests existants shell doivent rester verts |

### Smoke tests E2E

- [ ] `e2e/smoke/navigation.spec.ts` — à valider post-staging (nouveau composant dans le header)
