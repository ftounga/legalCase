# Mini-spec — F-58 / SF-58-02 : Repricing frontend — plans SOLO/TEAM/PRO

---

## Identifiant

`F-58 / SF-58-02`

## Feature parente

`F-58` — Repricing — Plans SOLO/TEAM/PRO

## Statut

`draft`

## Date de création

2026-03-28

## Branche Git

`feat/SF-58-02-repricing-frontend`

---

## Objectif

Mettre à jour workspace-billing, landing page, workspace-admin et super-admin pour afficher les nouveaux plans SOLO (59€) / TEAM (119€) / PRO (249€) avec les bons quotas et la re-analyse enrichie disponible sur tous les plans.

---

## Comportement attendu

### Cas nominal

1. La landing page affiche 4 cards : FREE (0€), SOLO (59€), TEAM (119€), PRO (249€)
2. Re-analyse enrichie cochée (✅) sur tous les plans avec la limite par plan visible
3. La page billing workspace affiche les 3 plans payants, SOLO marqué "Recommandé"
4. workspace-admin affiche les quotas corrects pour le plan actuel
5. super-admin affiche les labels corrects (Essai / Solo / Team / Pro)

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| Plan inconnu reçu de l'API | Affichage dégradé gracieux (label = planCode brut) |

---

## Critères d'acceptation

- [ ] Landing : 4 cards avec prix corrects
- [ ] Landing : re-analyse enrichie visible sur tous les plans (quota affiché)
- [ ] Billing : 3 plans payants, SOLO = "Recommandé"
- [ ] Billing : bouton TEAM stylé distinctement de SOLO et PRO
- [ ] workspace-admin : PLAN_QUOTA mis à jour (SOLO, TEAM, PRO)
- [ ] super-admin : labels Solo / Team / Pro corrects

---

## Périmètre

### Hors scope

- Backend (SF-58-01)
- Coupons early adopter

---

## Technique

### Fichiers impactés

- `workspace-billing.component.ts` — plan objects SOLO/TEAM/PRO
- `workspace-billing.component.html` — Recommandé sur SOLO, bouton TEAM
- `landing.component.html` — 4 cards mises à jour
- `workspace-admin.component.ts` — PLAN_QUOTA record
- `super-admin.component.ts` — labels

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires (Karma)

- [ ] BillingComponent : plan SOLO affiché avec prix 59€
- [ ] BillingComponent : plan TEAM affiché avec prix 119€
- [ ] BillingComponent : plan PRO affiché avec prix 249€
- [ ] BillingComponent : SOLO marqué featured

### Isolation workspace

- [ ] Non applicable

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Plans / limites** — affichage des plans mis à jour

### Smoke tests E2E

- [ ] Aucun concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-58-01 — doit être mergée avant (codes plan SOLO/TEAM/PRO doivent exister en backend)
