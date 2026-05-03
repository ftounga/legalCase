---
feature: F-177
subfeature: SF-177-13
title: Améliorer la visibilité du badge "Pré-rempli IA" sur les cards décisionnelles
domain: Frontend (transversal 3 domaines × 2 pays)
estimation: 1-1,5 h
status: Ready to dev
---

# SF-177-13 — Visibilité du badge "Pré-rempli IA" sur les cards décisionnelles

## Objectif

Rendre le pré-remplissage IA des outils décisionnels **immédiatement perceptible au coup d'œil** sur les cards (panel F-IA-04 + dashboard décisionnel F-184) en remplaçant le petit cercle 22 px discret introduit par SF-177-12 par : (a) un **pill agrandi** avec compteur intégré (~52 × 28 px, fond or saturé, icône blanche `auto_awesome` + chiffre) ; (b) une **bordure or premium 2 px** + shadow doré léger sur **toute** la card pré-remplie. Double signal visuel : la card complète signale "pré-remplie" au coup d'œil, le pill quantifie au focus.

## Contexte (origine du besoin)

SF-177-12 (mergée 2026-04-30, PR #742) a introduit le badge `auto_awesome` 22 × 22 px en haut à droite des cards décisionnelles avec compteur en tooltip uniquement. Constat utilisateur 2026-05-03 (ntounga@gmail.com en staging) : le badge est trop discret — couleur or pâle (`#C9A54B` sur fond `rgba(201,165,75,0.15)`), icône 16 px, aucun texte visible — l'avocat ne perçoit pas que des outils sont déjà pré-remplis. C'est un manque de visibilité d'une **différenciation produit clé** : "l'IA a déjà fait le travail, ouvre et valide".

Décision produit : option 5 (combo pill agrandi + bordure or premium) — cohérent avec le langage "premium = or" installé par SF-184-01 sur le dashboard décisionnel.

## Comportement nominal

### Card pré-remplie (`prefillCount > 0`)

1. **Bordure** : `2 px solid #C9973A` au lieu de la bordure standard (variable `--color-divider` ou équivalent) — appliquée sur le sélecteur racine `.tool-card`.
2. **Shadow** : `0 2px 8px rgba(201, 151, 58, 0.18)` en plus de la shadow existante (cumul ou remplacement selon résultat visuel).
3. **Badge transformé** : remplacement du `<span class="tool-card__badge tool-card__badge--prefill">` (cercle 22 × 22) par un pill rectangulaire arrondi avec :
   - dimensions : `min-width: 48px; height: 28px; padding: 0 8px;`
   - `border-radius: 14px` (pill complet)
   - background `#C9973A` (or saturé canonique DESIGN_SYSTEM.md)
   - color `#FFFFFF` (icône + chiffre)
   - contenu : `<mat-icon>auto_awesome</mat-icon>` (taille 14 px) + `<span class="prefill-count">{{ prefillCount }}</span>` (Inter 13 px bold)
   - tooltip conservé : `prefillTooltip` (texte "Pré-rempli par l'IA — N champs")
   - `aria-label` adapté : "Pré-rempli par l'IA, N champs"

### Card non pré-remplie (`prefillCount === 0` ou `null`)

1. Bordure standard (inchangée).
2. Pas de shadow doré.
3. Pas de badge `--prefill` (comportement actuel `@if (showPrefillBadge)` conservé).

### Dashboard décisionnel F-184 (cards verdict)

Si les cards verdict du dashboard utilisent le même composant `app-decision-tool-card`, le rendu est uniforme automatiquement. À vérifier en début de dev (grep usage du composant).

## Cas d'erreur

- `prefillCount` indéfini ou non numérique : `showPrefillBadge` retourne `false` → pas de badge (comportement actuel SF-177-12).
- `prefillCount === 0` : aucune bordure or, aucun pill (card "vierge" visuellement identique aux cards sans pré-fill).
- Card en état "alerte" (alertLevelClass = `tool-card--alert` ou similaire) : la bordure or **ne doit pas** masquer la bordure d'alerte. Règle de priorité : si la card a un alertLevelClass actif, garder la bordure d'alerte (rouge / orange) prioritaire, le badge pill or reste affiché en plus. À vérifier visuellement en dev.

## Critères d'acceptation

- [ ] Card avec `prefillCount > 0` : bordure 2 px or `#C9973A` + shadow doré léger appliqués sur `.tool-card`
- [ ] Card avec `prefillCount === 0` ou `null` : aucune bordure or, aucun shadow doré (rendu inchangé)
- [ ] Badge `--prefill` transformé en pill rectangulaire ~48-52 × 28 px avec icône blanche `auto_awesome` (14 px) + compteur Inter 13 px bold
- [ ] Background du pill : `#C9973A` (or saturé), color : blanc — couleurs DESIGN_SYSTEM.md
- [ ] Tooltip conservé sur le pill (texte SF-177-12 préservé)
- [ ] `aria-label` adapté : "Pré-rempli par l'IA, N champs"
- [ ] Cards en état alerte (rouge / orange) gardent la bordure d'alerte prioritaire — la bordure or ne masque pas le signal d'alerte
- [ ] Suite Jest verte : tests existants `decision-tool-card` (≥ X) + ≥ 2 nouveaux tests SF-177-13
- [ ] Aucun changement comportemental (clic, navigation, modal) — pure refonte visuelle du badge et de la bordure

## Plan de test minimal

### Tests Jest SF-177-13 (≥ 2 nouveaux)

| ID | Cas | Vérification |
|----|-----|--------------|
| T-01 | Card avec `prefillCount = 5` | Le badge `--prefill` est rendu en pill, contient `<mat-icon>auto_awesome</mat-icon>` + `<span class="prefill-count">5</span>`. Le `.tool-card` a la classe ou attribut indiquant l'état pré-rempli (ex: `.tool-card--prefilled` ou `[data-prefilled="true"]`) qui déclenche la bordure or via SCSS. |
| T-02 | Card avec `prefillCount = 0` | Pas de pill rendu. Pas de classe `--prefilled` sur `.tool-card`. Bordure standard. |

### Non-régression (existants)

- Tests existants `decision-tool-card.component.spec.ts` (alertes, états disabled, click handlers, etc.) doivent rester verts.

### Validation visuelle staging (obligatoire post-merge)

- [ ] Sur dossier réel avec ≥ 5 outils décisionnels pré-remplis (ex: Chen 4 immigration ou Travail FR riche) : les cards pré-remplies se distinguent au coup d'œil parmi les non-pré-remplies, pill bien lisible avec compteur.
- [ ] Card pré-remplie + en alerte : bordure d'alerte reste visible, pill or affiché en plus.
- [ ] Mobile / desktop / écran large : pill ne déborde pas du header de la card sur titres longs.

## Tables / endpoints / composants impactés

- **Aucune table impactée** (frontend pur).
- **Aucun endpoint impacté**.
- **Composants modifiés** :
  - `frontend/src/app/case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.html` (badge pill : icône + count visible).
  - `frontend/src/app/case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.ts` (input ou computed exposant `prefillCount` au template + classe modificateur sur `.tool-card`).
  - `frontend/src/app/case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.scss` (styles pill agrandi + bordure or premium + shadow).
  - `frontend/src/app/case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.spec.ts` (≥ 2 nouveaux tests SF-177-13).

## Hors périmètre (volontaire)

- **Modification de l'icône** — `auto_awesome` reste l'icône canonique IA (Material + Google Gemini + Copilot).
- **Animation pulse au mount** — discutée mais écartée pour rester dans une enveloppe 1-1,5 h. Peut être ajoutée en SF-177-14 si l'effet visuel reste insuffisant après staging.
- **Pré-fill côté composant outil** — la SF ne touche pas aux composants décisionnels qui calculent `getPrefillCount()` (logique inchangée).
- **Service partagé / nouveau composant** — pure modification du composant `decision-tool-card` existant.
- **Backend** — zéro impact.
- **Adaptation par domaine ou pays** — transversal, aucune adaptation.

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : zéro impact direct — la SF modifie le composant **partagé** `decision-tool-card`. Tous les outils utilisant ce composant (panel F-IA-04 + dashboard F-184) bénéficient automatiquement de la nouvelle visibilité — c'est l'effet recherché (cohérence visuelle uniforme).
- [x] **Autres pays** : aucun impact — pas de différence FR/BE.
- [x] **Autres domaines** : aucun impact — pas de différence Travail/Immigration/Famille.
- [x] **Autres UI patterns** : la nouvelle bordure or premium est cohérente avec le pattern installé par SF-184-01 (`.decisional-summary-panel` bordure 2 px or `#C9973A`). Pas de pattern concurrent à harmoniser.
- [x] **Autres flows transversaux** : aucun impact (pas auth / workspace / plans / routing).

### Niveaux de vérification

- [x] **Modèle TypeScript** : ajout d'un getter computed `prefilledClass` ou équivalent (logique simple) — pas de nouveau signal partagé.
- [x] **Record/DTO backend** : non concerné.
- [x] **Service / logique métier** : non concerné.
- [x] **Entité JPA + schéma DB** : non concerné.
- [x] **Tests existants** : `decision-tool-card.component.spec.ts` couvre déjà le badge actuel — les nouveaux tests s'ajoutent, les anciens sont adaptés si nécessaire.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Composant `decision-tool-card` partagé | Oui | Modifié dans cette SF — propage automatiquement à panel F-IA-04 + dashboard F-184 |
| Autres badges (`--coherence`, `--metier`) | Non applicable — out of scope | Restent inchangés (cercle 22 px). Si problème de visibilité émerge sur ces badges, créer SF dédiée. |
| Pattern bordure or premium | Cohérent avec SF-184-01 | Réutilisation du langage existant — pas de pattern concurrent |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (composant partagé = couverture automatique).
- [ ] Subfeature(s) parallèle(s) — non applicable.
- [ ] Backlog — non applicable (autres badges restent OK pour l'instant).
- [x] Non applicable aux autres badges (justification : la SF cible **uniquement** la visibilité du pré-remplissage IA, qui est la différenciation produit clé. Les badges `--coherence` (rouge alerte) et `--metier` (orange alerte) sont déjà visibles par leur palette d'alerte).

## Impact par domaine métier

Cette SF est **transversale** et ne touche aucune logique métier :
- pas de différence Travail / Immigration / Famille,
- pas de différence FR / BE,
- aucune adaptation par domaine.

## Nouveau pattern UI ou service partagé

- **Pas de nouveau composant partagé** — modification du composant existant `decision-tool-card`.
- **Pas de nouveau service**.
- **Pattern visuel renforcé** : la bordure or 2 px `#C9973A` + shadow `rgba(201,151,58,0.18)` est le **même langage** que SF-184-01 (panel `.decisional-summary-panel`). Cohérence "premium = or" maintenue.

## Préoccupations transversales

| Préoccupation | Concerné ? |
|---------------|-----------|
| Auth / Principal | Non |
| Workspace context | Non |
| Plans / limites | Non |
| Navigation / routing | Non |
| Outil décisionnel métier | Non (pas de changement métier — pure refonte visuelle d'un état déjà calculé par SF-177-12) |

## Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — la SF ne touche ni à l'auth, ni au workspace context, ni au routing.

## Notes d'implémentation

- **Self-check pré-commit** :
  - `grep -n "tool-card__badge--prefill" frontend/src/app/case-files/decisional-tools-panel/decision-tool-card/` → vérifier que le sélecteur SCSS est mis à jour (pill au lieu de cercle).
  - Inspection visuelle dans `npm start` local sur dossier de test (au moins 1 card pré-remplie + 1 non pré-remplie côte à côte) avant de commiter.
- **Test visuel staging obligatoire** avant de marquer Done.
- **Cohérence DESIGN_SYSTEM.md** : `#C9973A` (or canonique) déjà utilisé par `.decisional-summary-panel` (SF-184-01) — pas de nouvelle couleur introduite.

## Estimation

1-1,5 h dev + tests + review.

## Référence backlog

- `docs/PRODUCT_SPEC.md` — F-177 (à rouvrir : 10/10 SF Terminée → 10/11 En cours, ré-Terminée 11/11 après merge SF-177-13).
- Origine : badge SF-177-12 (PR #742) jugé trop discret par l'utilisateur en staging 2026-05-03.
- Décision visuelle : option 5 (combo pill + bordure or) parmi 5 propositions présentées 2026-05-03.
