# Mini-spec — F-IA-03 / SF-IA-03-15c Propagation popover source (Immigration)

## Identifiant

`F-IA-03 / SF-IA-03-15c`

## Feature parente

`F-IA-03` — Contrôle de cohérence sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-IA-03-15c-popover-propagation-immigration`

---

## Objectif

Clôturer la propagation du popover de source enrichi en couvrant les 3 outils Immigration (F-IM-05 Titre de séjour, F-IM-06 Recours, F-IM-07 Droit au travail). Après cette SF, **10/10 outils décisionnels F-IA-03** bénéficient du popover.

---

## Comportement attendu

### Cas nominal

Identique à SF-IA-03-15b. Les 3 composants Immigration adoptent la directive `[appCoherencePopover]` déjà livrée par 15b.

Mapping champ → sourceKey par composant :
- **F-IM-05 TitleDecisionSection** : `MOTIF → IM05_MOTIF`, `NATIONALITE_UE → nationalite_ue` (générique).
- **F-IM-06 RecoursSection** : `RECOURS_TYPE → IM06_RECOURS_TYPE`, `DATE_NOTIFICATION → date_notification_decision_contestee` (générique).
- **F-IM-07 WorkRightSection** : champ unique `coherenceAlert() → IM07_TITRE_TYPE`.

Le prompt Haiku (enrichi par 15b) produit déjà les sourcekeys `IM05_MOTIF`, `IM06_RECOURS_TYPE`, `IM07_TITRE_TYPE` — aucune modification backend nécessaire.

### Cas d'erreur

Identiques à 15b (fallback template si explication absente, fail-open HTTP).

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 10/10 outils couverts après cette SF. Plus de propagation F-IA-03 à prévoir.
- [x] **Autres pays** : FR + BE déjà couverts par les composants existants.
- [x] **Autres domaines** : Immigration couvert ici. Tous les 3 domaines (Travail, Famille, Immigration) maintenant dotés du popover.
- [x] **Autres UI patterns** : 5 zones connexes (F-69/92/93/94/96) déjà en backlog scan rétrospectif 15a.
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification

- [x] **Modèle TypeScript** : réutilise `SourceExplanation` (15a).
- [x] **DTO backend** : aucun changement.
- [x] **Service / logique métier** : aucun changement backend (prompt Haiku déjà enrichi en 15b).
- [x] **Entité JPA + DB** : aucun changement.
- [x] **Tests existants** : 3 composants Immigration à adapter pour mocker `/source-explanations`.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — cette SF consomme la directive `[appCoherencePopover]` déjà livrée par 15b, aucun nouveau pattern introduit.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-IM-05 Titre séjour | Oui | **Intégré** (2 champs) |
| F-IM-06 Recours | Oui | **Intégré** (2 champs) |
| F-IM-07 Droit au travail | Oui | **Intégré** (1 champ) |

### Décision

- [x] Étendu aux 3 outils Immigration. Clôture de F-IA-03-15.

---

## Critères d'acceptation

- [ ] `ImmigrationTitleDecisionSectionComponent` (F-IM-05) : les 2 champs `MOTIF` et `NATIONALITE_UE` utilisent la directive `[appCoherencePopover]` avec mapping vers `IM05_MOTIF` et `nationalite_ue`. Chargement source explanations au `ngOnInit`.
- [ ] `ImmigrationRecoursSectionComponent` (F-IM-06) : les 2 champs `RECOURS_TYPE` et `DATE_NOTIFICATION` utilisent la directive avec mapping vers `IM06_RECOURS_TYPE` et `date_notification_decision_contestee`.
- [ ] `ImmigrationWorkRightSectionComponent` (F-IM-07) : le badge unique `coherenceAlert()` utilise la directive avec sourceKey `IM07_TITRE_TYPE`.
- [ ] Tests frontend : specs adaptés pour mocker `/source-explanations` (1 call HTTP supplémentaire par composant).
- [ ] Build frontend vert, build backend vert, tests verts (non-régression 974+).

---

## Périmètre

### Hors scope

- Modifications backend (prompt Haiku déjà enrichi en 15b).
- Zones F-69/F-92/F-93/F-94/F-96 (backlog scan rétrospectif 15a).
- Ajout de nouveaux types d'action.

---

## Technique

### Endpoint(s)

Aucun nouveau. Réutilise `GET /api/v1/case-files/{id}/source-explanations`.

### Tables impactées / Migration

Aucune / Non applicable.

### Composants Angular

- `ImmigrationTitleDecisionSectionComponent`
- `ImmigrationRecoursSectionComponent`
- `ImmigrationWorkRightSectionComponent`

---

## Plan de test

### Tests frontend

- [ ] Specs `ImmigrationTitleDecisionSectionComponent` adaptés (mock `/source-explanations`).
- [ ] Specs `ImmigrationRecoursSectionComponent` adaptés.
- [ ] Specs `ImmigrationWorkRightSectionComponent` adaptés.

### Isolation workspace

- [x] Non applicable — déjà couvert par l'endpoint partagé (SF-IA-03-15a).

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Aucune**.

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| 3 composants Immigration | HTTP call `/source-explanations` ajouté | Specs adaptés |

### Smoke tests E2E

- [x] Aucun.

---

## Dépendances

### Bloquantes

- `SF-IA-03-15a Done` — infrastructure (endpoint, popover, service).
- `SF-IA-03-15b Done` — directive, prompt Haiku enrichi.

### Questions ouvertes

- [x] Aucune.

---

## Notes et décisions

- **Pourquoi SF dédiée et pas regroupée dans 15b** : Immigration est un domaine distinct avec ses propres composants et fichiers. Séparer permet (i) une granularité de merge claire par domaine, (ii) une durée bornée à 1 jour par SF, (iii) une traçabilité précise dans l'historique de PRODUCT_SPEC.md.
- **Pattern rodé** : cette SF est mécanique après 15a/15b. Le risque technique est minimal.
