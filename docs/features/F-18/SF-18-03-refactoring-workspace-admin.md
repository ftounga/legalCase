# Mini-spec — F-18 / SF-18-03 Refactoring page admin workspace

## Identifiant

`F-18 / SF-18-03`

## Feature parente

`F-18` — Page d'administration

## Statut

`ready`

## Date de création

2026-03-20

## Branche Git

`feat/SF-18-03-refactoring-workspace-admin`

---

## Objectif

Remplacer le contenu de la page `/workspace/admin` — actuellement orientée consommation tokens/coûts — par une vue consolidée plan + membres, utile au client sans le surcharger d'informations infra.

---

## Comportement attendu

### Cas nominal

La page `/workspace/admin` affiche :

1. **Section Plan** — carte résumant :
   - Nom du plan (FREE / STARTER / PRO)
   - Statut (ACTIVE / EXPIRED)
   - Date d'expiration trial si plan FREE
   - Nombre de dossiers utilisés vs quota du plan (FREE: 0, STARTER: 3, PRO: 20)

2. **Section Membres** — tableau listant les membres du workspace :
   - Email, rôle (OWNER / ADMIN / LAWYER / MEMBER), statut (ACTIVE)
   - Lien vers `/workspace/members` pour la gestion complète

Accès : OWNER et ADMIN uniquement (403 sinon → message d'accès refusé).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| 403 (non OWNER/ADMIN) | Message d'accès refusé |
| Erreur réseau | Snackbar erreur |

---

## Critères d'acceptation

- [ ] La page ne contient plus aucune donnée de tokens ou coûts LLM
- [ ] La section Plan affiche le plan actuel, son statut et la date d'expiration trial si applicable
- [ ] La section Membres liste tous les membres du workspace avec email et rôle
- [ ] Un utilisateur MEMBER ou LAWYER voit un message d'accès refusé (403)
- [ ] Un lien vers `/workspace/members` permet la gestion complète des membres

---

## Périmètre

### Hors scope

- Suppression de membres depuis cette page (géré dans `/workspace/members`)
- Affichage des tokens/coûts (déplacé vers F-25 super-admin)
- Modification du backend — pas de nouvel endpoint
- Suppression de l'endpoint `GET /api/v1/admin/usage` (conservé pour F-25)

---

## Technique

### Endpoints utilisés (existants)

| Méthode | URL | Notes |
|---------|-----|-------|
| GET | `/api/v1/workspaces/current` | Plan + expiry |
| GET | `/api/v1/workspaces/members` | Liste membres |

### Composants Angular modifiés

- `WorkspaceAdminComponent` — refactoring complet du template et du TS
- `AdminUsageService` — plus utilisé par ce composant (conservé pour F-25)

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests Angular (Karma)

- [ ] Chargement nominal — plan + membres affichés
- [ ] 403 sur `/workspaces/members` → `accessDenied = true`, message affiché
- [ ] Erreur réseau → snackbar erreur
- [ ] Plan FREE avec date d'expiration → date affichée

---

## Dépendances

### Subfeatures bloquantes

- SF-17-02 (API membres) — statut : done
- SF-17-06 (workspace switcher) — statut : done

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Le quota de dossiers par plan est hardcodé côté frontend (FREE=0, STARTER=3, PRO=20) — cohérent avec `PlanLimitService` backend
- `AdminUsageService` et `GET /api/v1/admin/usage` sont conservés — ils seront réutilisés par F-25
- Le composant TS est simplifié : suppression des `MatTableDataSource` tokens, ajout des appels workspace/current + members
