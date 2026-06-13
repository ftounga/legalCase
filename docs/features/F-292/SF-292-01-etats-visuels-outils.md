# SF-292-01 — État visuel « prêt à calculer » sur la carte d'outil décisionnel

> Feature parente : **F-292** (états visuels des outils décisionnels). Étape 0 GO + 0 bis GO avec ajustements (`SF-292-00-coherence.md`, `SF-292-00b-ux-coherence.md`). Frontend pur.

## Objectif (une phrase)

Rendre **visuellement distinct** un outil **pré-rempli par l'IA mais NON encore calculé** (« prêt à calculer ») d'un outil **pré-rempli ET calculé**, sur la carte du panneau décisionnel (F-IA-04).

## Constat (pourquoi)

La carte `decision-tool-card` applique aujourd'hui le **même** traitement « pré-rempli » (badge `auto_awesome` + outline or `tool-card--prefilled`, SF-177-13) **que l'outil soit calculé ou non**. Seuls le texte du corps (« Cliquer pour utiliser » vs verdict) et la fine bordure gauche de verdict changent → l'avocat ne distingue pas, parmi ses outils pré-remplis, **lesquels attendent encore un clic « calculer »**. C'est le chaînon manquant de F-258 (qui ne fait que **compter** les non-calculés au checkpoint de génération).

## Comportement nominal

Trois états sur la carte (le 4ᵉ — calculé sans pré-fill — reste inchangé) :

| État | Condition | Rendu |
|---|---|---|
| **Vierge** | `prefillCount = 0/null` ET non calculé | placeholder **« Cliquer pour utiliser »**, aucun outline |
| **Prêt à calculer** 🆕 | `prefillCount > 0` ET **non calculé** (`summary` sans `primaryValue`) | outline or **pointillé** (`tool-card--prefilled-pending`) + placeholder **« Prêt à valider — cliquer pour calculer »** + badge `auto_awesome` |
| **Pré-rempli + calculé** | `prefillCount > 0` ET calculé | outline or **plein** (`tool-card--prefilled`, inchangé) + **verdict** + bordure gauche verdict + badge `auto_awesome` |

- L'état « prêt à calculer » **disparaît automatiquement** dès que l'outil est calculé (le `summary` devient non vide → bascule sur le rendu calculé). Continuité assurée.

## Cas d'erreur / limites

1. **`prefillCount = null`** (outil non instrumenté pour le pré-fill) → traité comme vierge, aucun état « prêt à calculer » (pas de faux positif).
2. **`summary` présent mais `primaryValue` vide** → considéré non calculé : si `prefillCount > 0` → état « prêt à calculer » (cohérent avec `formatSummary` qui renvoie `null`).
3. **Reduced motion / pas de JS** → l'état repose sur du CSS statique (outline + texte), aucune animation requise.

## Critères d'acceptation vérifiables

- [ ] Carte `prefillCount > 0` + non calculée → classe `tool-card--prefilled-pending` présente **et** placeholder = « Prêt à valider — cliquer pour calculer ».
- [ ] Carte `prefillCount > 0` + calculée → **pas** de classe `tool-card--prefilled-pending` ; outline plein `tool-card--prefilled` conservé ; verdict affiché.
- [ ] Carte `prefillCount = 0` + non calculée → placeholder « Cliquer pour utiliser », ni `--prefilled` ni `--prefilled-pending`.
- [ ] L'outline « prêt à calculer » emprunte la **palette or** (pas de 4ᵉ couleur sémantique) et **n'altère pas** la bordure gauche de verdict (OK/WARNING/ALERT).
- [ ] Aucune régression des tests existants (badge `auto_awesome`, classe `tool-card--prefilled`, placeholder vierge).

## Plan de test minimal

- **Unitaires** (`decision-tool-card.component.spec.ts`) :
  - état « prêt à calculer » : classe + placeholder texte ;
  - état calculé : absence de `--prefilled-pending`, présence du verdict ;
  - état vierge : placeholder inchangé, aucune classe prefill ;
  - non-régression : T-01/T-02/T-03/T-04 existants verts.
- **Isolation workspace** : N/A (composant de présentation pur, aucune donnée serveur, aucun appel réseau).

## Composants impactés

- `frontend/.../decision-tool-card/decision-tool-card.component.ts` (getters `prefilledPending`, `prefilledPendingClass`, `placeholderText`).
- `decision-tool-card.component.html` (ngClass + texte du placeholder).
- `decision-tool-card.component.scss` (`&--prefilled-pending` : outline pointillé).
- `decision-tool-card.component.spec.ts` (tests).

**Aucun** : backend, endpoint, migration, contrat API, modèle.

## Hors périmètre

- Modifier la logique de **calcul** des outils ou le pré-remplissage IA lui-même.
- Toucher la bordure gauche de verdict ou les pills (procédure/pièces/risques).
- Le comptage F-258 au checkpoint (inchangé) et l'écran de composition F-288.

## Analyse transversale

- **Outil décisionnel** : **présentation seule** sur la carte — n'altère aucun outil, ne déclenche aucun calcul → invariant « 1 outil = 1 situation » intact.
- **Auth / workspace / plans / navigation** : aucun. **Pré-fill IA** : consomme `prefillCount` déjà fourni (aucun nouveau champ).
- **Smoke E2E** : aucun (pas d'impact auth/workspace/navigation).
- **Composant partagé** : `decision-tool-card` est mutualisé (panel F-IA-04 + dashboard agrégé). Les 2 consommateurs bénéficient du nouvel état sans changement d'API (input `prefillCount`/`summary` déjà passés). Vérifié : aucun autre pattern de carte d'outil concurrent.
