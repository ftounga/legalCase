# SF-163-01 — Backend endpoint `GET /api/v1/simulators` (catalogue simulateurs)

## Objectif

Exposer un endpoint REST authentifié qui retourne, pour le workspace primary de l'utilisateur courant, le domaine, le pays et la liste dédupliquée des `tool_id` d'outils décisionnels associés — utilisée par la future page `/simulators` (F-163 frontend) pour afficher le catalogue d'outils en mode simulateur autonome (hors dossier).

## Comportement nominal

- `GET /api/v1/simulators` avec session OIDC/OAuth2 (Google / Microsoft / LOCAL) ou JWT valide.
- Résolution de l'utilisateur via `CurrentUserResolver`.
- Lecture du workspace primary via `WorkspaceMemberRepository.findByUserAndPrimaryTrue()`.
- Lecture des règles de visibilité via `DecisionToolVisibilityRuleRepository.findForDomainAndCountry(legalDomain, country)`.
- Réponse 200 :
  ```json
  {
    "legalDomain": "DROIT_IMMIGRATION",
    "country": "FRANCE",
    "toolIds": ["F-IM-05-arbre-decisionnel-titre", "F-IM-06-recours", "F-IM-08-oqtf-avec-delai-fr", "..."]
  }
  ```
- `toolIds` : liste dédupliquée (`LinkedHashSet`) issue de la projection de chaque règle sur son `tool_id`, indépendamment du `layer` (ALWAYS_ON, CONTEXTUAL, OFF) et indépendamment des triggers — l'avocat doit pouvoir lancer **n'importe quel** outil de son domaine/pays en mode simulateur, sans dépendre d'un dossier.
- Tri stable : par `priority` croissant puis `toolId` lexical.

## Cas d'erreur

| Cas | Réponse |
|-----|---------|
| Non authentifié | 401 (filtre Spring Security standard, `anyRequest().authenticated()`) |
| Workspace primary absent (rare — invariant violé) | 200 + `legalDomain=null`, `country=null`, `toolIds=[]` + log WARN |
| `legalDomain` null sur le workspace | 200 + `legalDomain=null`, `country=<country>`, `toolIds=[]` + log INFO |

Aucun cas 404 ou 500 propagé pour des données métier vides — l'absence de simulateurs est un état nominal côté UI.

## Critères d'acceptation vérifiables

1. `GET /api/v1/simulators` sur workspace `DROIT_IMMIGRATION / FRANCE` retourne `legalDomain=DROIT_IMMIGRATION`, `country=FRANCE` et `toolIds` contenant au minimum `F-IM-05-arbre-decisionnel-titre`, `F-IM-06-recours`, `F-IM-08-oqtf-avec-delai-fr`.
2. `GET /api/v1/simulators` sur workspace `DROIT_FAMILLE / BELGIQUE` retourne `legalDomain=DROIT_FAMILLE`, `country=BELGIQUE` et `toolIds` non vide (outils famille BE seedés).
3. `GET /api/v1/simulators` sans auth retourne 401.
4. `GET /api/v1/simulators` sur workspace dont `legalDomain` est null retourne 200 avec `toolIds=[]`.
5. Isolation : deux workspaces de domaines différents (Travail FR vs Immigration FR) retournent des `toolIds` disjoints — un user Travail FR ne voit pas les outils Immigration FR.

## Plan de test minimal

### Tests d'intégration (Spring Boot + MockMvc)

- **IT-01** : workspace Immigration FR → 200 + `toolIds` contient les 3 IDs immigration sample.
- **IT-02** : workspace Famille BE → 200 + `legalDomain=DROIT_FAMILLE`, `country=BELGIQUE`, `toolIds` non vide.
- **IT-03** : pas d'auth → 401.
- **IT-04** : utilisateur sans workspace primary → 200 + `legalDomain=null`, `country=null`, `toolIds=[]` (le cas "workspace sans legalDomain" est impossible à provisionner — la colonne `workspaces.legal_domain` est NOT NULL en DB ; le code reste défensif sur ce chemin).
- **IT-05** : isolation — user Travail FR n'a aucun outil exclusif Immigration FR dans sa réponse.

### Tests unitaires

Non requis : la logique métier est entièrement déléguée à `DecisionToolVisibilityRuleRepository` (déjà couvert) et `WorkspaceMemberRepository` (déjà couvert). Le service `SimulatorsCatalogService` est un orchestrateur de 3 appels — couverture IT suffisante.

## Tables / endpoints / composants impactés

- **Endpoint nouveau** : `GET /api/v1/simulators`
- **Tables lues (aucune écriture)** : `workspace_members`, `workspaces`, `decision_tool_visibility_rules`, `users`, `auth_accounts`
- **Composants backend nouveaux** :
  - `SimulatorsCatalogController` (`fr.ailegalcase.casefile`)
  - `SimulatorsCatalogService` (`fr.ailegalcase.casefile`)
  - `SimulatorsCatalogResponse` (record DTO)
- **Composants frontend** : aucun dans cette SF (frontend = SF jumelle parallèle SF-163-02).

## Contrat API (figé — consommé par SF-163-02 frontend)

- **Méthode** : `GET`
- **URL** : `/api/v1/simulators`
- **Auth** : session OIDC/OAuth2 ou JWT (MEMBER min)
- **Body de requête** : aucun
- **Réponse 200** :
  ```json
  {
    "legalDomain": "DROIT_DU_TRAVAIL | DROIT_IMMIGRATION | DROIT_FAMILLE | null",
    "country": "FRANCE | BELGIQUE | null",
    "toolIds": ["string", "..."]
  }
  ```
- **Réponse 401** : non authentifié (filtre Spring Security standard).
- **Aucune réponse 404 / 500** propagée pour données vides.

## Hors périmètre

- Pas de filtrage par sous-domaine ou matière (l'avocat peut tester tous les outils de son domaine).
- Pas de personnalisation par préférence utilisateur (V1 = visibilité globale du domaine).
- Pas de pagination (le volume max attendu V1 est ~50 outils par domaine, négligeable).
- Pas de cache HTTP (`Cache-Control: no-cache` par défaut — la liste change rarement, recharge à chaque mount du composant).
- Pas de support multi-workspace : la SF lit uniquement le workspace **primary**. Si l'utilisateur veut tester un outil d'un autre domaine, il devra changer son workspace primary (cohérent avec le pattern F-IA-04).
- Pas de gestion de la persistance des simulations (hors scope F-163, simulateur stateless).

## Analyse de cohérence transversale

- **Pattern auth** : strictement aligné sur `DecisionToolVisibilityController` (paramètres `@AuthenticationPrincipal OidcUser` + `Principal`, résolution via `CurrentUserResolver` + `OAuthProviderResolver.resolve(principal)`).
- **Pattern lecture workspace** : `WorkspaceMemberRepository.findByUserAndPrimaryTrue()` — déjà utilisé par `CurrentWorkspaceController` et `DashboardController`.
- **Pattern lecture règles visibilité** : `DecisionToolVisibilityRuleRepository.findForDomainAndCountry()` — déjà utilisé par `DecisionToolVisibilityService`.
- **Pas de pattern partagé nouveau** : la SF n'introduit pas de service réutilisable au-delà de ces 3 patterns existants.

## Impact par domaine métier

Transversale : aucune adaptation par domaine. Le filtrage par `legalDomain × country` est porté par la table `decision_tool_visibility_rules` (déjà seedée pour les 3 domaines × 2 pays). L'endpoint retourne ce que la table contient pour le couple courant.

## Préoccupations transversales

- **Auth / Principal** : N/A — pattern miroir d'un endpoint déjà éprouvé.
- **Workspace context** : N/A — utilise `findByUserAndPrimaryTrue()` standard.
- **Plans / limites** : aucune limite plan (la lecture du catalogue est gratuite pour tous les plans).
- **Navigation / routing** : nouvelle route backend, pas de modification du routing frontend dans cette SF.
- **Outil décisionnel métier** : exposition lecture du catalogue, aucun outil n'est modifié.

## Migrations

Aucune migration Liquibase. Lecture pure.
