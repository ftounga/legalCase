# Mini-spec — F-240 / SF-240-02 Frontend sign-up — checkbox CGU/Privacy

## Identifiant

`F-240 / SF-240-02`

## Feature parente

`F-240` — Conformité contractuelle — click-wrap CGU/CGV/DPA + traçabilité

## Statut

`draft`

## Date de création

2026-05-11

## Branche Git

`feat/SF-240-02-consent-signup-frontend`

> Parallélisable avec **SF-240-03**. **Bloquée à l'intégration** par SF-240-01 (mais peut développer avec mock du service en attendant).

---

## Objectif

Bloquer la création de workspace dans `onboarding.component` tant que l'utilisateur n'a pas explicitement coché une case d'acceptation des CGU et de la politique de confidentialité de LegalCase, puis enregistrer cette acceptation côté backend via `POST /api/v1/consent/accept` avant la création du workspace.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur authentifié arrive sur la page `/onboarding` (premier login OAuth, pas encore de workspace).
2. Le formulaire actuel affiche : champ `name` (nom du workspace), bouton "Créer".
3. **Nouveauté** : sous le champ `name`, une checkbox bloquante affiche le texte :
   « J'ai lu et j'accepte les [CGU](/cgu) et la [politique de confidentialité](/privacy) de LegalCase. »
4. Les liens ouvrent les pages publiques `/cgu` et `/privacy` dans un nouvel onglet (`target="_blank" rel="noopener"`).
5. Le bouton "Créer" reste **désactivé** tant que :
   - le champ `name` est vide ou invalide,
   - OU la checkbox n'est pas cochée,
   - OU une soumission est en cours.
6. À la soumission du formulaire (`submit()`) :
   - **Étape A** — Appel `POST /api/v1/consent/accept` avec `{consentTypes: ["SIGNUP_TERMS", "PRIVACY_POLICY"], version: "2026-05-11"}` via un nouveau service Angular `ConsentService`.
   - **Étape B** — Si la réponse est 201, enchainer avec l'appel existant `workspaceService.createWorkspace(...)`.
   - **Étape C** — Si la réponse de l'étape A est ≠ 201, afficher un MatSnackBar d'erreur, **ne pas** créer le workspace, garder le formulaire éditable.
7. Le flux existant après création workspace (redirect dashboard, etc.) est conservé.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|----------------------|
| Checkbox non cochée | Bouton désactivé visuellement, pas de soumission |
| Erreur réseau sur `POST /consent/accept` | MatSnackBar "Impossible d'enregistrer votre acceptation, merci de réessayer", formulaire restauré, workspace **non créé** |
| 400 backend (type inconnu) | MatSnackBar erreur générique ("Une erreur est survenue"), log console pour debug |
| 401 sur consent | Redirection /login (interceptor existant) |
| Création workspace échoue après consent OK | Comportement existant inchangé (MatSnackBar), le consent reste enregistré (cas acceptable : l'utilisateur a accepté, il retentera la création workspace) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable (modif onboarding form, pas un outil décisionnel).
- [x] **Autres pays** : transversal — la checkbox s'applique à tout utilisateur FR ou BE, le texte est en français en V1 (pas de NL).
- [x] **Autres domaines** : transversal.
- [x] **Autres UI patterns** : checkbox MatCheckbox + label avec liens — pattern existant déjà utilisé dans le projet (à confirmer dans `frontend/src/app/shared/`). Sinon création d'un pattern minimal sans extraction de composant partagé (pas de réutilisation immédiate prévue).
- [x] **Autres flows transversaux** :
  - **Auth / Principal** — l'utilisateur est déjà authentifié (a passé OAuth), pas de changement.
  - **Workspace context** — l'acceptation est faite AVANT création workspace, le `workspace_id` côté backend sera NULL (cf. SF-240-01 D-04).
  - **Navigation / routing frontend** — les liens vers `/cgu` et `/privacy` doivent rester accessibles publiquement (déjà OK via F-74).

### Cas spécifique : nouveau pattern UI

Le composant introduit un **service `ConsentService`** qui sera également consommé par SF-240-03 et SF-240-04 — c'est un service applicatif partagé à placer dans `frontend/src/app/core/services/consent.service.ts`. Pas de composant UI partagé en V1 (la checkbox sign-up et la modale paiement ont des présentations différentes).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| SF-240-03 (frontend paiement) | Oui — partage `ConsentService` | Intégré dans cette SF (le service est créé ici, SF-240-03 le consomme tel quel) |
| SF-240-04 (DPA téléchargement) | Indirect — `ConsentService` peut être réutilisé côté frontend si on veut tracer le téléchargement front-side, mais l'option choisie est tracking serveur (cf. SF-240-04) | Non applicable côté frontend |

### Décision

- [x] Étendu à toutes les cibles applicables : oui (ConsentService partagé créé ici).
- [x] Subfeature(s) parallèle(s) : SF-240-03 consomme le même service.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF de flow d'inscription, pas un composant décisionnel métier. Aucune entrée `TOOL_REGISTRY`, pas de pré-fill IA, pas de validation F-IA-03 (le sign-up est antérieur à toute analyse IA).

---

## Impact par domaine métier

- [x] Transversal — aucune adaptation par domaine ni par pays. Le texte de la checkbox est en français (FR + BE francophone Wallonie+Bruxelles ; pas de NL en V1).

---

## Parité des domaines métier (niveau ≥ 5)

- [x] **Non applicable** — pas un outil décisionnel.

---

## Critères d'acceptation

- [ ] **CA-01** : la page `/onboarding` affiche une checkbox MatCheckbox bloquante sous le champ `name` avec un label cliquable contenant les liens `/cgu` et `/privacy` (ouverts en nouvel onglet).
- [ ] **CA-02** : le bouton "Créer" est désactivé tant que la checkbox n'est pas cochée (même si le champ `name` est valide).
- [ ] **CA-03** : à la soumission, le frontend appelle `POST /api/v1/consent/accept` avec `{consentTypes: ["SIGNUP_TERMS", "PRIVACY_POLICY"], version: "2026-05-11"}` AVANT `workspaceService.createWorkspace(...)`.
- [ ] **CA-04** : si le POST consent renvoie 201, la création workspace s'enchaîne normalement.
- [ ] **CA-05** : si le POST consent échoue (réseau, 400, 500), un MatSnackBar d'erreur s'affiche et le workspace n'est pas créé.
- [ ] **CA-06** : si la création workspace échoue après consent OK, le consent reste enregistré (cas acceptable, l'utilisateur peut retenter).
- [ ] **CA-07** : nouveau service `ConsentService` créé sous `frontend/src/app/core/services/consent.service.ts` avec méthode `acceptConsent(request: ConsentAcceptanceRequest): Observable<ConsentAcceptanceResponse>`.
- [ ] **CA-08** : tests Jest sur `OnboardingComponent` : (a) checkbox initialement non cochée → bouton désactivé, (b) checkbox cochée + name valide → bouton actif, (c) submit appelle consentService puis workspaceService dans le bon ordre, (d) consentService échec → workspace non créé.
- [ ] **CA-09** : tests Jest sur `ConsentService` : (a) `acceptConsent` POST vers `/api/v1/consent/accept` avec le bon body, (b) propagation correcte de la réponse, (c) propagation correcte des erreurs HTTP.
- [ ] **CA-10** : `npm run build` reste vert. Aucune régression des tests existants `OnboardingComponent.spec.ts`.

---

## Périmètre

### Hors scope (explicite)

- Versioning dynamique des CGU (la version `"2026-05-11"` est hardcodée en constante côté frontend pour V1). En V2 : lire la version depuis un endpoint backend ou depuis le build manifest.
- Affichage du contenu des CGU/Privacy directement dans une modale (préférence : redirection vers les pages publiques `/cgu` et `/privacy` en nouvel onglet — moins disruptif pour l'utilisateur).
- Animation ou tooltip "à quoi sert ce consentement ?" — V2 si confusion utilisateur constatée.
- Possibilité de refuser explicitement et de quitter l'app (le user est déjà authentifié OAuth — il peut se déconnecter via le menu existant).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `acceptedTerms` (FormControl) | `false` | Toujours `false` à l'ouverture du formulaire |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|-------|-------------|--------|-------|
| `acceptedTerms` | Oui | boolean === `true` | `Validators.requiredTrue` |
| `name` | Oui (existant) | non vide | inchangé |

---

## Technique

### Composants Angular modifiés

- `OnboardingComponent` (`frontend/src/app/onboarding/onboarding.component.ts` + `.html`) — ajout du FormControl `acceptedTerms`, ajout de la checkbox dans le template, modification de `submit()` pour POST consent avant createWorkspace.

### Composants Angular créés

- Aucun nouveau composant (le service `ConsentService` est un service, pas un composant).

### Services Angular créés

- `frontend/src/app/core/services/consent.service.ts` — `ConsentService` avec méthode `acceptConsent(request: ConsentAcceptanceRequest): Observable<ConsentAcceptanceResponse>`.

### Modèles TypeScript créés

- `frontend/src/app/core/models/consent.model.ts` — `ConsentAcceptanceRequest`, `ConsentAcceptanceResponse`, `Acceptance`, type `ConsentType` (union des 4 valeurs).

### Endpoint(s) consommé(s)

| Méthode | URL | Provenance |
|---------|-----|------------|
| POST | `/api/v1/consent/accept` | SF-240-01 (contrat API figé) |

### Migration Liquibase

- [x] Non applicable (frontend pur).

---

## Plan de test

### Tests unitaires (Jest)

- [ ] **OB-01** `OnboardingComponent.spec.ts` — checkbox initialement non cochée → bouton désactivé.
- [ ] **OB-02** `OnboardingComponent.spec.ts` — name valide + checkbox cochée → bouton actif.
- [ ] **OB-03** `OnboardingComponent.spec.ts` — submit() appelle `consentService.acceptConsent` AVANT `workspaceService.createWorkspace` (assertion sur l'ordre).
- [ ] **OB-04** `OnboardingComponent.spec.ts` — `consentService.acceptConsent` échoue → `workspaceService.createWorkspace` n'est pas appelé + MatSnackBar d'erreur.
- [ ] **OB-05** `OnboardingComponent.spec.ts` — `consentService.acceptConsent` OK mais `workspaceService.createWorkspace` échoue → MatSnackBar d'erreur, le consent reste enregistré (pas de rollback côté serveur — comportement acceptable).
- [ ] **OB-06** `OnboardingComponent.spec.ts` — non-régression : le flux existant (name valide, sans checkbox dans le scénario legacy si testé) reste cohérent → mise à jour des specs existantes pour cocher la box dans tous les tests qui assertent la soumission.
- [ ] **CS-01** `ConsentService.spec.ts` — `acceptConsent` POST le bon URL et body.
- [ ] **CS-02** `ConsentService.spec.ts` — propage la réponse 201.
- [ ] **CS-03** `ConsentService.spec.ts` — propage les erreurs HTTP.

### Tests d'intégration

- [ ] Aucun (SF frontend pure, le contrat backend est testé en SF-240-01).

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — sans modification.
- [ ] Workspace context — sans modification (le flow workspace creation existant reste inchangé).
- [ ] Plans / limites — sans impact.
- [x] **Navigation / routing frontend** — les liens `/cgu` et `/privacy` doivent rester publics (vérifié : ces routes sont déjà publiques via F-116 SSG).

### Composants existants potentiellement impactés

| Composant / Endpoint | Impact | Test de non-régression |
|----------------------|--------|------------------------|
| `OnboardingComponent.spec.ts` (existant) | Tous les tests qui asserent la soumission doivent désormais cocher la checkbox | Mise à jour des fixtures dans le même PR |
| `app.routes.ts` | Aucun (routes publiques `/cgu`, `/privacy` déjà existantes via F-74) | Aucun |

### Smoke tests E2E concernés

- [x] `e2e/smoke/auth.spec.ts` — vérifier que le flux OAuth login + onboarding fonctionne bout en bout après la modification (la checkbox doit être cochée dans le smoke test).
- [ ] Aucun autre.

---

## Dépendances

### Subfeatures bloquantes

- SF-240-01 — peut développer en parallèle avec mock du service ; intégration réelle après merge SF-240-01.

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

### Décision D-01 — Ordre : consent AVANT workspace

L'acceptation est enregistrée avant la création workspace pour deux raisons :

1. **Légal** : preuve que l'utilisateur a accepté avant tout accès au service.
2. **UX** : si le consent POST échoue (réseau), l'utilisateur n'a pas créé un workspace orphelin sans consent associé.

### Décision D-02 — Version `"2026-05-11"` hardcodée côté frontend en V1

La version est gérée comme une constante TypeScript dans `consent.service.ts` :

```typescript
export const CURRENT_CONSENT_VERSION = '2026-05-11';
```

À bumper manuellement quand les CGU ou la politique de confidentialité sont modifiées. En V2 : récupération depuis un endpoint backend ou depuis le build manifest.

### Décision D-03 — Pas de modale d'affichage des CGU dans l'onboarding

Préférence : redirection vers les pages publiques `/cgu` et `/privacy` en nouvel onglet (`target="_blank" rel="noopener"`). Pourquoi :

- Moins disruptif que d'embarquer 50 pages de CGU dans une modale.
- Cohérent avec le pattern utilisé par les concurrents (Stripe, Doctrine, Lexis+).
- Permet à l'utilisateur de garder son onboarding ouvert dans un onglet pendant qu'il lit les CGU.
