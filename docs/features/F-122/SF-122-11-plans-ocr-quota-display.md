# Mini-spec — F-122 / SF-122-11 Quota OCR affiché dans les plan cards

## Identifiant
`F-122 / SF-122-11`

## Feature parente
`F-122` — OCR pour PDF scannés

## Statut `draft`  · Date `2026-04-19`  · Branche `feat/SF-122-11-plans-ocr-quota-display`

---

## Objectif

Afficher le quota OCR mensuel inclus dans chaque plan (100 / 800 / 3 000 / 10 000 pages pour FREE/SOLO/TEAM/PRO) sur toutes les pages qui listent les plans (landing publique + page billing connectée). Un prospect qui vient sur la landing doit voir combien de pages OCR sont incluses, et un client déjà connecté doit voir la même info sur sa page d'abonnement.

---

## Comportement

### Workspace-billing (`workspace-billing.component.ts`)

Ajouter une feature `"X pages OCR / mois"` dans le tableau `features[]` de chaque plan, au même niveau que "tokens / mois". Valeurs :
- FREE : 100 pages OCR / mois
- SOLO : 800 pages OCR / mois
- TEAM : 3 000 pages OCR / mois
- PRO : 10 000 pages OCR / mois

### Landing (`landing.component.html`)

Chaque carte plan (4 cartes statiques HTML) reçoit une ligne `<li class="plan-feature">` avec le même libellé, placée juste après la ligne "tokens / mois" et avant "Synthèse et questions IA". Mêmes valeurs que workspace-billing.

### Cas d'erreur

- Aucun — changement purement cosmétique côté affichage, pas de logique dynamique
- Si un jour les valeurs back évoluent (`PlanLimitService.MONTHLY_OCR_PAGES` constantes) : il faudra mettre à jour ces affichages manuellement (documenté dans les commentaires)

---

## Critères d'acceptation

- [ ] La page `/workspace/billing` affiche "X pages OCR / mois" dans chaque plan card avec les bonnes valeurs
- [ ] La page `/` (landing) affiche "X pages OCR / mois" dans chaque plan card avec les bonnes valeurs
- [ ] Les valeurs correspondent exactement aux constantes backend `PlanLimitService.{FREE,SOLO,TEAM,PRO}_MONTHLY_OCR_PAGES`
- [ ] Aucune régression sur les autres features existantes (dossiers, documents, analyses, chat, tokens)
- [ ] Style cohérent avec les autres lignes de feature (icône check, même taille de texte)

---

## Plan de test

### Unitaires frontend
- `workspace-billing.component.spec.ts` : test que chaque plan contient une feature dont le label matche la regex `/\d+\s?\d*\s?pages OCR/` avec la bonne valeur (FREE=100, SOLO=800, TEAM=3000, PRO=10000)

### Test visuel
- Lancer `npm start`, ouvrir `/` → voir les 4 cartes plan avec la ligne OCR
- Naviguer vers `/workspace/billing` → voir les 4 cartes plan avec la ligne OCR

### Isolation workspace
- Non applicable — changement d'affichage statique

---

## Tables / endpoints / composants impactés

### Frontend
- `workspace-billing.component.ts` — tableau `plans[].features` étendu de 4 lignes OCR
- `landing.component.html` — 4 lignes `<li>` ajoutées dans les 4 `.pricing-card`

### Backend
- Aucun changement

### Config
- Aucun changement

---

## Hors périmètre

- Packs OCR achetables (→ déjà livré dans SF-122-10)
- Tracking OCR consommé/restant (→ SF-122-12)
- Refactor pour centraliser les quotas (landing/billing dupliquent toujours les valeurs, mais ce n'est pas l'objectif ici)

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres pays (Belgique) | Oui | **Intégrée** — mêmes quotas, mêmes plans, pas de différenciation pays |
| Autres domaines | Oui | **Intégrée** — quotas OCR workspace-scope |
| Autres outils | Non applicable | Feature purement UI d'affichage |
| Page pricing externe (autre que landing) | Non applicable | Pas d'autre surface pricing publique |
| Super-admin quotas edit | **Backlog** — nouvelle feature V2 pour permettre aux admins d'override les quotas OCR sans déploiement |

**Analyse d'impact cross-cutting** :
- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — valeurs affichées seulement, pas de logique de gate modifiée
- [ ] Navigation / routing — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] Pas de nouveau pattern — structure `<li class="plan-feature">` existante réutilisée à l'identique
- [x] Pas de service partagé — valeurs hardcodées (cohérent avec le pattern existant pour tokens/dossiers/documents)
