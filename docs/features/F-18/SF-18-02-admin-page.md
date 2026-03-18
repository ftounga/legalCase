# Mini-spec — SF-18-02 Page d'administration frontend

## Identifiant
`SF-18-02`

## Feature parente
`F-18` — Page d'administration

## Statut
`ready`

## Date de création
2026-03-19

## Branche Git
`feat/SF-18-02-admin-page`

---

## Objectif
Permettre à un OWNER ou ADMIN de visualiser la consommation LLM de son workspace (totaux globaux, détail par dossier, détail par utilisateur) via une page dédiée accessible depuis le sidenav.

---

## Comportement attendu

### Cas nominal
1. L'utilisateur clique sur « Administration » dans le sidenav
2. La route `/workspace/admin` charge `WorkspaceAdminComponent`
3. Le composant appelle `GET /api/v1/admin/usage`
4. Affichage de 3 sections :
   - **Résumé global** : 3 cartes `mat-card` — tokens input, tokens output, coût total
   - **Par dossier** : `mat-table` avec colonnes `[titre, tokensInput, tokensOutput, coût]` + `matSort` + `mat-paginator`
   - **Par utilisateur** : `mat-table` avec colonnes `[email, tokensInput, tokensOutput, coût]` + `matSort` + `mat-paginator`

### Cas d'erreur

| Situation | Comportement attendu | Code |
|-----------|---------------------|------|
| Utilisateur LAWYER ou MEMBER (403) | Affiche un message « Accès réservé aux OWNER et ADMIN » — pas de redirection | 403 |
| Erreur réseau / 500 | MatSnackBar snack-error « Erreur lors du chargement » | 5xx |
| Aucune donnée (listes vides) | Affiche les tableaux avec un message « Aucune donnée » | 200 |

---

## Critères d'acceptation

- [ ] La route `/workspace/admin` est accessible depuis le sidenav (lien visible pour tous)
- [ ] Un OWNER voit les 3 sections avec les bonnes données
- [ ] Un ADMIN voit les mêmes données qu'un OWNER
- [ ] Un LAWYER/MEMBER voit un message « Accès réservé » sans stacktrace
- [ ] Les tableaux sont triables et paginés (5/10/25 par page)
- [ ] En l'absence de données, les tableaux affichent un message vide
- [ ] Le composant est conforme au design system (palette, polices, espacements)

---

## Périmètre hors-scope

- Guard de rôle Angular sur la route (la gestion se fait dans le composant via 403)
- Export CSV
- Filtrage par période
- Gestion des membres depuis cette page (lien vers /workspace/members existant)
- Statut du plan (déjà visible dans /workspace/billing)

---

## Éléments techniques

### Route à ajouter dans `app.routes.ts`
```
{ path: 'workspace/admin', loadComponent: () => WorkspaceAdminComponent }
```

### Lien sidenav à ajouter dans `shell.component.html`
```html
<a mat-list-item routerLink="/workspace/admin" routerLinkActive="sidenav-active">
  <mat-icon matListItemIcon>admin_panel_settings</mat-icon>
  <span matListItemTitle>Administration</span>
</a>
```

### Nouveaux fichiers Angular
- `workspace/workspace-admin/workspace-admin.component.ts`
- `workspace/workspace-admin/workspace-admin.component.html`
- `workspace/workspace-admin/workspace-admin.component.scss`
- `core/services/admin-usage.service.ts`
- `core/models/workspace-usage-summary.model.ts`

### Modèles frontend
```typescript
interface WorkspaceUsageSummary {
  totalTokensInput: number;
  totalTokensOutput: number;
  totalCost: number;
  byUser: UserUsageSummary[];
  byCaseFile: CaseFileUsageSummary[];
}
interface UserUsageSummary { userId: string; userEmail: string; tokensInput: number; tokensOutput: number; totalCost: number; }
interface CaseFileUsageSummary { caseFileId: string; caseFileTitle: string; tokensInput: number; tokensOutput: number; totalCost: number; }
```

### Tables impactées
Aucune — frontend uniquement.

---

## Plan de test

### Tests unitaires
- [ ] AdminUsageService.getSummary() — appel HTTP GET /api/v1/admin/usage

### Tests de composant (Karma)
- [ ] Affiche les totaux quand l'API retourne des données
- [ ] Affiche les 2 tableaux (byUser, byCaseFile) avec les bonnes colonnes
- [ ] Affiche le message « Accès réservé » si l'API retourne 403
- [ ] Affiche un snackbar d'erreur si l'API retourne 500
- [ ] Affiche un état vide si les listes sont vides

### Isolation workspace
- Non applicable — le composant consomme uniquement l'API admin/usage qui filtre déjà par workspace côté backend

---

## Dépendances
- SF-18-01 — done

---

## Notes et décisions
- Pas de guard de rôle Angular : la page gère le 403 avec un message inline. Simple et suffisant pour V1.
- Le lien sidenav est visible pour tous les rôles — les non-admins voient le message d'accès refusé.
