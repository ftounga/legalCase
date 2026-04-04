# Mini-spec — F-110 / SF-110-02 : Écran "Guides & barèmes" frontend (consultation)

## Identifiant
`F-110 / SF-110-02`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`ready`

## Date de création
`2026-04-04`

## Branche Git
`feat/SF-110-02-referentials-frontend`

---

## Objectif
Afficher un écran "Guides & barèmes" accessible depuis le menu latéral, présentant en lecture seule les référentiels métier du workspace (types de litiges, barème Macron, jalons immigration, pièces requises, barèmes pension/prestation).

---

## Comportement attendu

### Cas nominal
1. L'utilisateur clique sur "Guides & barèmes" dans la sidenav.
2. L'écran charge `GET /api/v1/referentials?domain=<workspace.legalDomain>`.
3. Les sections sont affichées groupées par `referentialType` via des `<mat-expansion-panel>`.
4. Chaque entrée montre : `label`, `country` (si non null), `sourceRef` (si non null) + `valueJson` interprété selon le type.
5. Si le workspace n'a pas de `legalDomain` connu → message "Domaine juridique non configuré".

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| API 400/500 | Message d'erreur inline, pas de crash |
| Workspace sans legalDomain | "Domaine juridique non configuré" |
| Sections vides | "Aucun référentiel disponible pour ce domaine." |

---

## Critères d'acceptation

- [ ] Route `/referentials` ajoutée dans `app.routes.ts`, protégée par `authGuard`
- [ ] Entrée "Guides & barèmes" + icône `menu_book` dans la sidenav (tous les membres authentifiés)
- [ ] `ReferentialService` : `getReferentials(domain)` → `GET /api/v1/referentials?domain=X`
- [ ] `ReferentialsComponent` : panels `mat-expansion-panel` par section avec titres lisibles :
  - LITIGATION_TYPE → "Types de litiges"
  - BAREME_MACRON → "Barème Macron"
  - IMMIGRATION_JALONS → "Jalons procéduraux"
  - IMMIGRATION_PIECES → "Pièces requises"
  - PENSION_TAUX → "Barème pension alimentaire"
  - PRESTATION_COEFF → "Prestation compensatoire"
- [ ] `valueJson` interprété par type : LITIGATION_TYPE → "X an(s) — Art. XXX" ; IMMIGRATION_JALONS → liste jalons ; IMMIGRATION_PIECES → liste pièces ; barèmes numériques → valeurs formatées
- [ ] Spinner `mat-spinner` pendant le chargement
- [ ] `legalDomain` lu depuis le `WorkspaceService` existant
- [ ] Pas de bouton "Modifier" (réservé SF-110-03)

---

## Périmètre

### Hors scope
- Modification des valeurs (SF-110-03)
- Badge "mise à jour disponible" (SF-110-04)
- Signalement anomalie (SF-110-05)

---

## Technique

### Endpoint consommé

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/referentials?domain=X` | Oui | MEMBER |

### Composants Angular

- `ReferentialsComponent` (`referentials/referentials.component.ts`) — écran principal
- `ReferentialService` (`core/services/referential.service.ts`) — HTTP service

### Tables impactées
Aucune (lecture seule via API SF-110-01).

### Migration Liquibase
Non applicable.

---

## Plan de test

### Tests unitaires (Jest)
- [ ] `ReferentialService` — `getReferentials('DROIT_DU_TRAVAIL')` appelle `GET /api/v1/referentials?domain=DROIT_DU_TRAVAIL`
- [ ] `ReferentialsComponent` — affiche les panels pour chaque section retournée
- [ ] `ReferentialsComponent` — affiche message d'erreur si API échoue (HTTP 500)
- [ ] `ReferentialsComponent` — affiche "Aucun référentiel disponible" si sections vides

### Tests d'intégration
Non applicable (composant stateless, GET uniquement).

### Isolation workspace
Déléguée au backend (filtrage par `workspaceId` côté API).

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Navigation / routing frontend** — nouvelle route `/referentials` + entrée sidenav

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `ShellComponent` | ajout lien sidenav | vérifier sidenav dans le test du composant |
| `app.routes.ts` | nouvelle route enfant sous authGuard | smoke test navigation |

### Smoke tests E2E
- [ ] `e2e/smoke/navigation.spec.ts` — routes existantes non cassées

---

## Dépendances

### Subfeatures bloquantes
- SF-110-01 — statut : `done`

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions
- Le `legalDomain` est lu depuis `WorkspaceService` (déjà disponible dans le shell).
- Le rendu de `valueJson` est client-side : pas de nouveau endpoint dédié au rendu.
- Les entrées `country` non null sont affichées avec un badge (ex: "FRANCE", "BELGIQUE").
