# SF-162-01 — Page synthèse refondue : grille de badges + navigation

## Objectif

Transformer le haut de l'écran `SynthesisComponent` (actuellement une **pile verticale** de tous les blocs) en une **grille de cartes/badges** synthétiques par bloc, avec compteurs et navigation par clic vers le détail (page dédiée pour blocs riches, panel inline expandable pour les autres). C'est l'étape socle de F-162.

## Contexte

Aujourd'hui `synthesis.component.html` empile 12+ panels expansibles (`mat-expansion-panel`) sur la même page :
- Chronologie, Faits, Points juridiques, Risques, Questions ouvertes, Pièces manquantes
- Indemnités estimées (FR/BE), Pension alimentaire, Prestation compensatoire, Liquidation communauté
- Pistes stratégiques, Checklist procédurale, Questions complémentaires (Q&A IA)
- Sections immigration / divorce dédiées

**Problèmes** :
1. Page longue, scroll fatigant.
2. F-161 (caps relevés ~5×) et F-160 (pagination historique) **aggravent** la longueur.
3. La hiérarchie d'importance n'est pas perceptible visuellement — tout est traité au même rang.

## Comportement nominal

1. **Tête de page** : la grille de badges (~3 colonnes responsive) précède immédiatement le bandeau version + le risk-badge.
2. **Chaque badge** affiche :
   - Icône (réutilise les `panel-icon--*` existants : `gavel`, `balance`, `warning_amber`, `timeline`, `report_problem`, `quiz`, `checklist`, `lightbulb`, `calculate`, `family_restroom`, `account_balance`)
   - Libellé (ex. "Faits", "Risques", "Timeline")
   - Compteur principal en JetBrains Mono (ex. "12")
   - Sous-libellé secondaire optionnel (ex. "dont 3 critiques" si `riskLevel=ELEVE` sur le badge Risques)
3. **Cliquer un badge** :
   - Pour les blocs riches (Faits, Risques, Timeline, Points juridiques, Pistes stratégiques, Checklist) → scroll fluide + ouverture du panel correspondant **dans la même page** (pas encore de page dédiée — ça arrive en SF-162-02 à -05).
   - Pour les blocs courts (Pièces manquantes, Questions ouvertes) → ouvre le panel correspondant.
4. **Badges masqués** automatiquement si le bloc correspondant est vide (compteur = 0).
5. La pile verticale des panels existants **reste en dessous** — la grille est un **résumé navigable**, pas un remplacement total. Cela laisse la pile fonctionnelle pendant la transition vers les pages dédiées (SF-162-02 à -05).

## Cas d'erreur / edge cases

- `synthesis()` null → grille non rendue (continuer à afficher l'état de chargement / streaming actuel).
- Aucun bloc avec données → grille vide → ne pas rendre le wrapper du tout.
- Mobile (≤ 720 px) : grille 1 colonne, badges pleine largeur.

## Critères d'acceptation

- [ ] La grille de badges apparaît juste sous le `synthesis-header` quand `synthesis()` est non null.
- [ ] Les badges respectent l'ordre canonique : Timeline → Faits → Points juridiques → Risques → Pistes stratégiques → Checklist → Questions complémentaires → Pièces manquantes → Questions ouvertes.
- [ ] Chaque badge masqué quand son bloc est vide.
- [ ] Cliquer un badge scrolle vers le panel correspondant (id `section-*` déjà présents pour `section-pieces`, `section-checklist`, `section-questions`, `section-pistes`).
- [ ] Le panel cible est en état `expanded` après scroll (réutiliser un mécanisme similaire au scroll-and-highlight SF-IA-03-19 existant).
- [ ] Mobile : grille rend 1 colonne sans cassure visuelle.
- [ ] DESIGN_SYSTEM.md respecté : couleurs navy/or/secondaire, JetBrains Mono pour les compteurs, Inter pour le reste.

## Plan de test minimal

- **Jest unitaires** :
  - U1 : `synthesisBadges` computed retourne uniquement les blocs non vides.
  - U2 : ordre canonique respecté.
  - U3 : badge `Timeline` compteur = `synthesis().timeline.length`.
  - U4 : `scrollToBlock(id)` invoque `scrollIntoView` sur l'élément cible.
- **Smoke E2E** : non requis (pas de chemin auth/routing modifié).

## Tables / endpoints / composants impactés

- **Composant Angular** : `frontend/src/app/case-files/synthesis/synthesis.component.ts` + `.html` + `.scss`
- **Tests** : `frontend/src/app/case-files/synthesis/synthesis.component.spec.ts`
- **Aucun nouvel endpoint, aucune migration**.

## Hors périmètre

- Pages dédiées (Timeline, Faits, Points juridiques, Risques) → SF-162-02 à SF-162-05.
- Popups blocs courts → SF-162-06.
- Refonte du composant `SynthesisHeaderComponent` ou export PDF → reste inchangé.

## Analyse de cohérence transversale

- **Préoccupations transversales** : aucune.
- **Nouveau pattern UI** : grille de cartes-badges « compteur + libellé + icône + clic ». Pas d'usage similaire ailleurs aujourd'hui — donc inline simple. Si ce pattern est repris ailleurs (dashboard décisionnel F-184 ?), une factorisation viendra plus tard. Pas de dette de convergence immédiate.
- **Impact par domaine métier** : transversal — affecte tous les domaines (Travail / Immigration / Famille) et les 2 pays (FR / BE) de manière identique. C'est de l'infra UI partagée sur l'écran de synthèse.

## Contrat API

Aucun nouvel endpoint. Frontend pure consommation des données déjà présentes dans `synthesis()`, `procedureChecks()`, `questions()`, `strategicOptions()`.
