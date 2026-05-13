# Mini-spec — F-240 / SF-240-01 Backend consent — table + endpoint

## Identifiant

`F-240 / SF-240-01`

## Feature parente

`F-240` — Conformité contractuelle — click-wrap CGU/CGV/DPA + traçabilité

## Statut

`draft`

## Date de création

2026-05-11

## Branche Git

`feat/SF-240-01-consent-backend`

---

## Objectif

Persister chaque acceptation explicite par un utilisateur d'un document légal (CGU, politique de confidentialité, CGV de paiement, téléchargement DPA) avec timestamp + IP + user-agent, et exposer un endpoint REST pour que le frontend (SF-240-02 sign-up, SF-240-03 paiement, SF-240-04 DPA) puisse enregistrer ces acceptations.

---

## Comportement attendu

### Cas nominal

L'utilisateur authentifié déclenche depuis le frontend une acceptation (case à cocher au sign-up, modale paiement, ou téléchargement DPA). Le frontend appelle `POST /api/v1/consent/accept` avec un body indiquant le(s) type(s) de consentement et la version du document. Le backend :

1. Vérifie l'authentification (Spring Security OAuth2).
2. Valide les valeurs de `consentTypes` (chaque entrée doit appartenir à l'enum applicatif).
3. Valide le format de `version` (chaîne non vide ≤ 64 caractères).
4. Récupère l'IP cliente (en respectant le header `X-Forwarded-For` configuré pour l'Ingress K8s) et le user-agent.
5. INSERT une ligne par `consentType` dans `user_consent_acceptance` — pas de déduplication, chaque acceptation est conservée pour audit (un même utilisateur peut accepter plusieurs fois — par exemple à chaque souscription d'un plan).
6. Si l'utilisateur a déjà un workspace primary (via `WorkspaceMember.is_primary = true`), le `workspace_id` est renseigné automatiquement. Sinon (cas d'un sign-up tout neuf qui accepte AVANT la création workspace), `workspace_id` reste NULL.
7. Renvoie 201 avec la liste des entrées créées.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `consentTypes` absent ou vide | Erreur de validation | 400 |
| `consentTypes` contient une valeur inconnue (ni `SIGNUP_TERMS`, `PRIVACY_POLICY`, `PAYMENT_TERMS`, `DPA_DOWNLOAD`) | Erreur de validation explicite | 400 |
| `version` absent, vide, ou > 64 caractères | Erreur de validation | 400 |
| Utilisateur non authentifié | Réponse standard Spring Security | 401 |
| Erreur DB (contrainte FK utilisateur supprimé entre-temps) | 500 avec log, message générique côté client | 500 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable — c'est une infrastructure de consentement, aucun lien avec les outils décisionnels.
- [x] **Autres pays** : transversal FR + BE — le RGPD s'applique aux deux pays, le DPA est commun en V1. Pas d'adaptation par pays.
- [x] **Autres domaines** : transversal DROIT_DU_TRAVAIL / DROIT_FAMILLE / DROIT_IMMIGRATION — aucune adaptation par domaine.
- [x] **Autres UI patterns** : aucun (SF backend pure).
- [x] **Autres flows transversaux** :
  - **Auth / Principal** — le consent est attaché à `user_id`, vérifier que tous les `@AuthenticationPrincipal` existants restent compatibles (lecture seule du Principal pour récupérer l'ID utilisateur, pas de modification du contrat).
  - **Workspace context** — `workspace_id` nullable car `SIGNUP_TERMS` est accepté AVANT création workspace ; les `WHERE workspace_id = :ws` standards ne s'appliquent pas pour cette table.
  - **Plans / limites** — sans impact (le consent est gratuit, pas de gate).
  - **Navigation / routing** — sans impact backend (le routing modale paiement est traité en SF-240-03).

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : le contrat API ci-dessous est figé pour SF-240-02/03/04.
- [x] **Record / DTO backend** : `ConsentAcceptanceRequest`, `ConsentAcceptanceResponse`.
- [x] **Service / logique métier** : `ConsentAcceptanceService` (validation, persistance, propagation workspace_id).
- [x] **Entité JPA + schéma DB** : entité `UserConsentAcceptance` + migration 229.
- [x] **Tests existants** : pas de table préexistante à étendre — nouvelle infrastructure.

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF introduit un **endpoint transversal** consommé par 3 SF parallèles (SF-240-02 sign-up, SF-240-03 paiement, SF-240-04 DPA téléchargement). Le contrat est figé ci-dessous (section "Technique → Endpoint").

- [x] **Où le nouveau endpoint pourrait-il être réutilisé ?** Scanné : tout nouveau document légal opposable à l'avenir (ex. acceptation d'un avenant CGU, acceptation d'un module add-on facturable, opt-in marketing distinct du consentement légal). Le design `consent_type` enum extensible couvre ces cas sans schema migration future.
- [x] **Y a-t-il des patterns concurrents ?** Audit : aucun. Le projet n'avait jusqu'ici **aucune** infrastructure de consentement applicatif. F-77 (GA4 + bannière cookies) gère le consent cookies au niveau navigateur (localStorage côté frontend), distinct du consent contractuel.
- [x] **Le nouveau endpoint peut-il servir à d'autres features ?** Oui — F-240 lui-même utilise le même endpoint pour 4 types distincts, et F-134 (V9+) pourra l'étendre avec un `consent_type = 'MSA_SIGNED'` pour les grands comptes sans changement de schéma.
- [x] **Équivalent design à remplacer ?** Aucun.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| SF-240-02 (frontend sign-up) | Oui | SF parallèle — consomme `POST /api/v1/consent/accept` figé ici |
| SF-240-03 (frontend paiement) | Oui | SF parallèle — consomme `POST /api/v1/consent/accept` figé ici |
| SF-240-04 (DPA téléchargement) | Oui | SF parallèle — consomme `POST /api/v1/consent/accept` côté serveur lors du téléchargement |
| F-134 (DPA signé V9+) | Non en V1 | Backlog — extension future via nouveau `consent_type` |
| F-178 (super-admin backlog DB) | Non en V1 | Backlog V2 — UI de consultation des acceptations |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature : **non** — SF-240-01 livre le backend seul, les 3 SF frontend consomment l'endpoint en parallèle.
- [x] Subfeature(s) parallèle(s) créée(s) : SF-240-02, SF-240-03, SF-240-04 (à rédiger après merge SF-240-01).
- [x] Backlog pour cibles non prioritaires : F-134 V9+ (DPA signé), F-178 V2 (UI super-admin).
- [x] Non applicable aux autres outils métier (justification : infrastructure transversale).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend pure. Aucun composant frontend décisionnel, aucune intégration au panel F-IA-04, aucune entrée `TOOL_REGISTRY`. La conformité F-IA-04 sera traitée si pertinente dans SF-240-02/03 (en pratique : non, les composants sign-up et paiement ne sont pas des outils décisionnels métier).

---

## Impact par domaine métier

- [x] **Sensible au domaine ?** Non — infrastructure transversale.
- [x] **Sensible au pays (FR / BE) ?** Non — le RGPD est commun FR + BE. Le DPA V1 est commun aux deux pays. L'IP collectée est neutre. Aucune branche `workspaceCountry` dans cette SF.
- [x] **Justification transversale** : « infrastructure de consentement applicatif, aucune adaptation par domaine ni par pays ».

---

## Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool décisionnel livré : **non applicable** — SF backend infrastructure, pas un outil décisionnel.

---

## Critères d'acceptation

- [ ] **CA-01** : `POST /api/v1/consent/accept` avec un payload valide (`consentTypes: ["SIGNUP_TERMS", "PRIVACY_POLICY"]`, `version: "2026-05-11"`) retourne 201 avec un tableau d'entrées créées (1 entrée par `consentType` du body).
- [ ] **CA-02** : chaque entrée créée contient `id` (UUID), `userId` (UUID de l'utilisateur authentifié), `consentType`, `version`, `acceptedAt` (ISO-8601 UTC), `acceptanceIp` (extraite du header `X-Forwarded-For` ou du `RemoteAddr` à défaut), `acceptanceUserAgent`, `workspaceId` (UUID du workspace primary de l'utilisateur ou NULL si pas encore de workspace).
- [ ] **CA-03** : `POST /api/v1/consent/accept` avec `consentTypes: ["UNKNOWN_TYPE"]` retourne 400 avec message d'erreur indiquant les valeurs autorisées.
- [ ] **CA-04** : `POST /api/v1/consent/accept` avec `consentTypes: []` ou champ absent retourne 400.
- [ ] **CA-05** : `POST /api/v1/consent/accept` avec `version` vide, absent, ou > 64 caractères retourne 400.
- [ ] **CA-06** : appel sans authentification retourne 401.
- [ ] **CA-07** : un même utilisateur peut accepter le même `consentType` plusieurs fois — chaque acceptation est conservée comme ligne distincte (audit trail).
- [ ] **CA-08** : l'IP est résolue depuis `X-Forwarded-For` (premier élément si liste séparée par virgule) avec fallback sur `request.getRemoteAddr()`.
- [ ] **CA-09** : isolation workspace — bien que la table n'utilise pas un filtre `workspace_id` standard (le consent précède parfois le workspace), aucune fuite d'information transverse n'est possible car le endpoint ne fait que de l'écriture pour l'utilisateur authentifié courant.
- [ ] **CA-10** : la migration Liquibase 229 s'exécute proprement sur H2 (profil `dev`) et PostgreSQL (profil `local` + prod) sans collision de numéro.

---

## Périmètre

### Hors scope (explicite)

- Endpoint de lecture (`GET /api/v1/consent/...`) — non requis en V1, le frontend n'a pas besoin de consulter les acceptations passées pour l'instant. À ajouter en V2 si super-admin demande une UI de consultation (F-178 v2).
- Endpoint de révocation — la révocation d'un consent (« je retire mon consentement à la politique de confidentialité ») nécessite par construction la suppression du compte (Art. 17 RGPD droit à l'effacement). Pas de retrait partiel.
- Versioning historique des CGU avec migration des acceptations passées — V2 si signal terrain.
- UI super-admin pour consulter les acceptations — V2 (rattachable à F-178).
- Signature électronique cryptographique (HMAC, certificat) — V9+ via F-134.
- Notifications email à l'utilisateur après acceptation (confirmation de souscription au plan) — c'est déjà couvert par les emails Stripe + les emails transactionnels existants.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `id` | UUID v4 généré côté Java (`UUID.randomUUID()`) | Toujours auto-généré, jamais fourni par le client |
| `accepted_at` | `Instant.now()` côté Java | Pas de trust du timestamp client (sécurité) |
| `acceptance_ip` | extraite du request (X-Forwarded-For prioritaire) | Pas de trust du client |
| `acceptance_user_agent` | header `User-Agent` du request | Tronquée à 500 chars |
| `workspace_id` | lookup `WorkspaceMember.is_primary=true` pour le user, sinon NULL | Renseigné automatiquement si possible |

Comportements à la création :

- `created_at` n'existe pas sur cette table — `accepted_at` joue le rôle de timestamp de création (un consent est immuable).
- Pas de soft-delete — un consent ne se supprime pas (RGPD : preuve d'acceptation à conserver pendant la durée de la relation contractuelle + 5 ans, art. 2224 Code civil pour la prescription).

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `consentTypes` (body) | Oui | tableau 1-10 entrées | `SIGNUP_TERMS` \| `PRIVACY_POLICY` \| `PAYMENT_TERMS` \| `DPA_DOWNLOAD` | Pas d'unicité applicative | trim() puis toUpperCase |
| `version` (body) | Oui | 64 chars | non vide, non blanc | Non | trim() |
| `acceptance_ip` (auto) | Oui | 45 chars (IPv6 max) | format IP valide | Non | extraction X-Forwarded-For \|\| RemoteAddr |
| `acceptance_user_agent` (auto) | Oui | 500 chars | texte libre | Non | truncate(500) |

Notes :

- Les valeurs `consentTypes` sont des strings côté wire mais persistées telles quelles dans une colonne `VARCHAR(32)` avec contrainte CHECK applicative — pas d'enum JPA (pour rester extensible sans migration). Validation côté `ConsentAcceptanceService`.
- `version` est une chaîne libre côté backend (le frontend décide du format — date `"2026-05-11"` ou semver `"1.0"`). Pour V1 on accepte n'importe quelle chaîne ≤ 64 chars. Si un audit ultérieur exige un format strict, ajouter un regex.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/consent/accept` | Oui (OAuth2 standard) | Tout utilisateur authentifié — pas de gate workspace car SIGNUP_TERMS précède la création workspace |

**Contrat API figé pour parallélisation SF-240-02 / SF-240-03 / SF-240-04** :

```
POST /api/v1/consent/accept
Authorization: (cookie session OAuth2 standard)
Content-Type: application/json

{
  "consentTypes": ["SIGNUP_TERMS", "PRIVACY_POLICY"],
  "version": "2026-05-11"
}

→ 201 Created
{
  "acceptances": [
    {
      "id": "f3a7b2c1-...",
      "userId": "a1b2c3d4-...",
      "consentType": "SIGNUP_TERMS",
      "version": "2026-05-11",
      "acceptedAt": "2026-05-11T14:23:05.123Z",
      "acceptanceIp": "82.65.123.45",
      "acceptanceUserAgent": "Mozilla/5.0 (Macintosh; ...)",
      "workspaceId": "w1234567-..." | null
    },
    {
      "id": "...",
      "consentType": "PRIVACY_POLICY",
      ...
    }
  ]
}

→ 400 Bad Request (validation)
{
  "error": "INVALID_CONSENT_TYPE",
  "message": "Unknown consent type 'XXX'. Allowed: SIGNUP_TERMS, PRIVACY_POLICY, PAYMENT_TERMS, DPA_DOWNLOAD"
}

→ 401 Unauthorized (réponse standard Spring Security)
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `user_consent_acceptance` | CREATE + INSERT | Nouvelle table |
| `users` | SELECT (FK uniquement) | Pas de modif |
| `workspace_members` | SELECT (lookup primary workspace) | Pas de modif |

### Migration Liquibase

- [x] Oui — `229-create-user-consent-acceptance.xml`

```sql
CREATE TABLE user_consent_acceptance (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  consent_type VARCHAR(32) NOT NULL,
  version VARCHAR(64) NOT NULL,
  accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  acceptance_ip VARCHAR(45) NOT NULL,
  acceptance_user_agent VARCHAR(500) NOT NULL,
  workspace_id UUID NULL REFERENCES workspaces(id),
  CONSTRAINT chk_consent_type CHECK (consent_type IN ('SIGNUP_TERMS', 'PRIVACY_POLICY', 'PAYMENT_TERMS', 'DPA_DOWNLOAD'))
);

CREATE INDEX idx_consent_user_type ON user_consent_acceptance(user_id, consent_type);
CREATE INDEX idx_consent_workspace ON user_consent_acceptance(workspace_id) WHERE workspace_id IS NOT NULL;
```

### Composants Angular (si applicable)

Aucun (SF backend pure).

### Classes Java livrées

- `fr.ailegalcase.consent.UserConsentAcceptance` (entité JPA)
- `fr.ailegalcase.consent.UserConsentAcceptanceRepository` (Spring Data JPA)
- `fr.ailegalcase.consent.ConsentAcceptanceService` (validation + persistance + lookup workspace primary)
- `fr.ailegalcase.consent.ConsentAcceptanceController` (endpoint POST)
- `fr.ailegalcase.consent.dto.ConsentAcceptanceRequest` (record)
- `fr.ailegalcase.consent.dto.ConsentAcceptanceResponse` (record + nested `Acceptance` record)

---

## Plan de test

### Tests unitaires

- [ ] **CASE-01** `ConsentAcceptanceServiceTest` — cas nominal : 2 `consentTypes` → 2 entrées insérées avec timestamps cohérents et IP/UA renseignés.
- [ ] **CASE-02** `ConsentAcceptanceServiceTest` — `consentTypes` contient une valeur inconnue → `IllegalArgumentException` avec message explicite.
- [ ] **CASE-03** `ConsentAcceptanceServiceTest` — `consentTypes` vide ou null → `IllegalArgumentException`.
- [ ] **CASE-04** `ConsentAcceptanceServiceTest` — `version` null/blank/> 64 chars → `IllegalArgumentException`.
- [ ] **CASE-05** `ConsentAcceptanceServiceTest` — utilisateur avec workspace primary → `workspace_id` rempli automatiquement.
- [ ] **CASE-06** `ConsentAcceptanceServiceTest` — utilisateur sans workspace primary → `workspace_id` NULL.
- [ ] **CASE-07** `ConsentAcceptanceServiceTest` — extraction IP depuis `X-Forwarded-For` avec liste `"82.65.1.1, 10.0.0.1"` → retient `82.65.1.1`.
- [ ] **CASE-08** `ConsentAcceptanceServiceTest` — pas de header `X-Forwarded-For` → fallback sur `request.getRemoteAddr()`.

### Tests d'intégration

- [ ] **IT-01** `POST /api/v1/consent/accept` avec body valide (2 types) → 201 + 2 entrées en DB.
- [ ] **IT-02** `POST /api/v1/consent/accept` sans auth → 401.
- [ ] **IT-03** `POST /api/v1/consent/accept` avec type inconnu → 400 + message explicite.
- [ ] **IT-04** `POST /api/v1/consent/accept` avec `consentTypes: []` → 400.
- [ ] **IT-05** `POST /api/v1/consent/accept` avec `version` absent → 400.
- [ ] **IT-06** Double appel du même endpoint avec mêmes types → 2 lignes distinctes en DB (pas de déduplication).
- [ ] **IT-07** User avec workspace primary → la réponse contient `workspaceId` non null.
- [ ] **IT-08** User sans workspace (just signed up) → la réponse contient `workspaceId: null`.

### Isolation workspace

- [x] **Non applicable** — raison : la table `user_consent_acceptance` n'est pas filtrée par `workspace_id` (le consent précède parfois le workspace). L'endpoint POST n'écrit que pour l'utilisateur authentifié courant (`@AuthenticationPrincipal`), pas de risque de fuite cross-user. Aucun endpoint GET n'est exposé en V1. La règle CLAUDE.md « Accès données sans filtre `workspace_id` → REFUS » est explicitement levée pour cette table avec justification dans la mini-spec — exception documentée.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — utilise `@AuthenticationPrincipal` pour récupérer l'utilisateur courant ; ne modifie pas le contrat du Principal mais en dépend pour `user_id`. Pas de changement de type d'auth.
- [x] **Workspace context** — utilise `WorkspaceMember.findFirstByUserIdAndIsPrimaryTrue` pour résoudre le workspace primary. Pas de modification du modèle workspace.
- [ ] **Plans / limites** — sans impact.
- [ ] **Navigation / routing frontend** — sans impact (SF backend pure).

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `WorkspaceMemberRepository` | nouvelle méthode `findFirstByUserIdAndIsPrimaryTrue` ajoutée — aucun impact sur les consommateurs existants | aucun (méthode additive) |
| `users` table | nouvelle FK depuis `user_consent_acceptance.user_id` — pas de modif schéma users | aucun (FK additive, ON DELETE non spécifié → RESTRICT par défaut, ce qui est OK : on ne supprime jamais un user qui a accepté quelque chose sans aussi supprimer ses consents par la procédure RGPD effacement) |
| Spring Security configuration | nouvelle route `/api/v1/consent/accept` à autoriser pour utilisateurs authentifiés | smoke test existant `auth.spec.ts` (l'auth générale reste OK) |

### Smoke tests E2E concernés

- [x] `e2e/smoke/auth.spec.ts` — login OAuth doit continuer à fonctionner (le sign-up flow lui-même n'est pas modifié par SF-240-01 ; SF-240-02 ajoutera la checkbox). Raison : vérifier que l'ajout de l'endpoint ne casse pas la chaine d'authentification.
- [ ] Aucun smoke test spécifique à `/consent/accept` en V1 (couvert par tests d'intégration backend).

---

## Dépendances

### Subfeatures bloquantes

- Aucune. SF-240-01 part de master à jour.

### Subfeatures bloquées par celle-ci

- SF-240-02 (frontend sign-up) — peut développer en parallèle avec mock du service, mais l'intégration réelle ne marche qu'après merge de SF-240-01.
- SF-240-03 (frontend paiement) — idem.
- SF-240-04 (DPA téléchargement) — idem ; côté serveur la SF-240-04 enregistre aussi via le même endpoint.

### Questions ouvertes impactées

- Aucune question dans `docs/OPEN_QUESTIONS.md` n'est concernée.

---

## Notes et décisions

### Décision D-01 — Pas d'enum JPA sur `consent_type`

Persisté en `VARCHAR(32)` avec contrainte CHECK applicative. Permet d'ajouter de nouveaux types (`MSA_SIGNED` pour F-134 V9+, etc.) sans migration de schéma — uniquement update de la liste autorisée dans `ConsentAcceptanceService` + alteration de la contrainte CHECK le moment venu.

### Décision D-02 — `version` libre, pas SHA-256 forcé

Le frontend décide du format (date ISO `"2026-05-11"`, semver `"1.0"`, ou hash si jugé pertinent). En V1 on stocke tel quel. Si un audit ultérieur exige une preuve d'intégrité cryptographique, ajouter une SF-240-XX qui :

1. Calcule un hash SHA-256 du contenu des pages CGU/Privacy au build frontend.
2. Le push au backend dans `version`.
3. Stocke en parallèle le hash de référence côté serveur pour vérification.

Hors V1 — `version` libre suffit pour la preuve "telle version a été acceptée à telle date".

### Décision D-03 — `acceptance_ip` en `VARCHAR(45)`, pas `INET`

Choix de portabilité H2 ↔ PostgreSQL. PostgreSQL a un type `INET` natif mais H2 ne le supporte pas, ce qui casse le profil `dev` (H2 en mémoire). `VARCHAR(45)` couvre IPv4 (15 chars max) et IPv6 (39 chars max + zone identifier). Pas de validation regex stricte côté DB — le service applicatif valide.

### Décision D-04 — `workspace_id` NULLABLE et `ON DELETE` non spécifié

`workspace_id` est nullable car `SIGNUP_TERMS` précède la création workspace. Quand le workspace est créé ensuite, on **ne backfille pas** le `workspace_id` des consents pré-existants — c'est volontaire : le consent a été donné par l'utilisateur avant l'existence du workspace, l'attribuer rétroactivement serait inexact historiquement. Pour les acceptations post-workspace (`PAYMENT_TERMS`, `DPA_DOWNLOAD`), le `workspace_id` est rempli automatiquement par le service via `WorkspaceMember.findFirstByUserIdAndIsPrimaryTrue`.

`ON DELETE` non spécifié sur la FK `workspaces(id)` → comportement par défaut PostgreSQL = NO ACTION (équivalent RESTRICT). Si un workspace est supprimé, les consents qui y pointent restent — c'est OK car la suppression d'un workspace est une opération rare (essentiellement super-admin) et les consents conservent leur valeur de preuve indépendamment.

### Décision D-05 — Aucun endpoint GET en V1

Le frontend n'a pas besoin de re-lire les acceptations en V1. L'UX click-wrap consiste à demander à chaque flow (sign-up, paiement, téléchargement DPA) — pas à se souvenir de l'historique. Si un super-admin veut consulter, c'est du SQL direct en V1 ou une SF V2 ultérieure (rattachable à F-178).

### Décision D-06 — Pas de rate limiting V1

Le endpoint n'est pas exposé à des appels massifs (consentement = action ponctuelle de l'utilisateur). Le rate-limiting Spring Security global (s'il existe) couvre le besoin. À évaluer si abus constaté en prod.
