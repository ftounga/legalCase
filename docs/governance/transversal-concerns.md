# Préoccupations transversales — règle anti-régression

Certaines modifications impactent silencieusement des composants existants qui n'ont pas été touchés.
Ces **préoccupations transversales** doivent être traitées explicitement à chaque subfeature.

La règle de blocage automatique correspondante est définie dans `CLAUDE.md` section "Préoccupations transversales". Ce fichier détaille les déclencheurs et les actions attendues.

## Déclencheurs obligatoires

| Préoccupation | Exemples concrets | Action requise |
|--------------|------------------|----------------|
| **Auth / Principal** | Nouveau type d'auth, modification du Principal, changement de session | Lister tous les `@AuthenticationPrincipal` existants. Vérifier que chacun supporte le nouveau type. Ajouter test de non-régression. |
| **Workspace context** | Nouveau moyen de résoudre le workspace, changement de `workspace_id` | Lister tous les composants qui résolvent le workspace. Vérifier leur comportement. |
| **Plans / limites** | Nouveau plan, changement de quota, nouveau gate | Lister tous les appels à `PlanLimitService`. Vérifier les gates. |
| **Navigation / routing** | Nouvelle route, guard modifié, redirection ajoutée | Vérifier tous les chemins de navigation existants. Lancer les smoke tests. |
| **Outil décisionnel métier** | Création, modification ou observation concernant un outil décisionnel (calculator / analyzer / generator / decision engine côté backend ; section composant côté frontend). Inclut tout ajout backlog, toute SF qui touche un outil existant, toute observation de bug qui en mentionne un. | **Lister tous les outils décisionnels** (F-DT-07/08/09/10, F-IM-05/06/07, F-FA-05/06/07, etc.). **Scanner chacun** pour vérifier s'il contient un switch conditionnel sur un type métier, un pays ou un mode qui mélange plusieurs situations distinctes. **Classer** chaque outil : déjà séparé / multi-situations à scinder / paramétrage simple. **Appliquer l'invariant** : un outil décisionnel = une situation métier (pattern F-DT-08/F-DT-10). Si un autre outil présente le même pattern que celui à l'origine de la demande, l'inclure dans le périmètre ou ouvrir une feature jumelle au backlog. |

## Suite de smoke tests E2E

Les tests de non-régression automatiques sont dans `e2e/smoke/`.
Lancer avant tout push touchant une préoccupation transversale :

```bash
cd e2e && npm test
```

Les smoke tests couvrent les chemins critiques d'intégration :
- `auth.spec.ts` — login local, login OAuth, logout, redirect non-authentifié
- `workspace.spec.ts` — switch workspace → rechargement des dossiers
- `navigation.spec.ts` — invitation → /login, guards, redirections
