# SF-162-06 — Popups pour les blocs courts

## Objectif

Pour les blocs **courts** de la synthèse (Pièces manquantes, Questions ouvertes), faire en sorte que cliquer sur le badge ouvre un **popup modal** contenant la liste, plutôt que de scroller dans la pile inférieure (comportement SF-162-01) ou que d'introduire encore une nouvelle page dédiée. Ces blocs sont synthétiques par nature : ils ont rarement plus de 5-10 items et ne méritent pas une page entière.

## Contexte

Dernière SF de F-162. Après SF-162-05, les blocs riches (Timeline, Faits, Points juridiques, Risques) ont chacun leur page dédiée. Les autres blocs (Pièces manquantes, Questions ouvertes) sont courts et bénéficient mieux d'un popup que d'un scroll-to-anchor.

## Comportement nominal

1. Cliquer sur le badge "Pièces manquantes" → ouverture d'un `MatDialog` listant les pièces (texte + index numéroté).
2. Cliquer sur le badge "Questions ouvertes" → ouverture d'un `MatDialog` listant les questions.
3. Le dialog porte le titre du bloc, un compteur, une liste numérotée, un bouton "Fermer".
4. Le badge passe d'un `routerLink`/`scrollToBlock` simple à une action `popup` quand applicable. Le descripteur `SynthesisBadge` gagne un champ optionnel `popup?: 'pieces' | 'questions-ouvertes'` qui prend le pas sur `route` et `anchor`.

## Cas d'erreur / edge cases

- Liste vide → le badge n'est pas rendu (déjà géré par le filtre `count > 0`).
- Échap ou clic en dehors → ferme le dialog.

## Critères d'acceptation

- [ ] Composant `SynthesisShortBlockDialogComponent` standalone créé.
- [ ] Cliquer le badge "Pièces manquantes" ouvre le dialog avec la liste.
- [ ] Cliquer le badge "Questions ouvertes" ouvre le dialog avec la liste.
- [ ] Les autres badges (avec `route`) continuent de naviguer ; les badges sans `popup` ni `route` continuent de scroller.
- [ ] Tests Jest U1-U3 verts.
- [ ] DESIGN_SYSTEM.md respecté.

### Étape post-merge (au moment où l'utilisateur confirme le merge)

- [ ] `docs/PRODUCT_SPEC.md` : F-162 marquée Terminée 6/6, ligne ajoutée à l'historique des évolutions.
- [ ] Commit direct sur master.

## Plan de test minimal

- **Jest** :
  - U1 : badges "Pièces manquantes" et "Questions ouvertes" exposent un champ `popup` (et **pas** de `route`).
  - U2 : `openPopup('pieces')` invoque `MatDialog.open` avec la bonne config.
  - U3 : `SynthesisShortBlockDialogComponent` rend la liste passée en data.
- **Smoke E2E** : non requis.

## Tables / endpoints / composants impactés

- **Composants** :
  - Nouveau : `synthesis-short-block-dialog/synthesis-short-block-dialog.component.{ts,html,scss,spec.ts}`
  - Modifié : `synthesis.component.{ts,html}` — ajout de `openPopup()`, champ `popup?` sur `SynthesisBadge`, branche template @if (badge.popup) avant @if (badge.route).
- **Aucun nouvel endpoint, aucune migration**.

## Hors périmètre

- Refonte complète de la pile inférieure (les panels Pièces / Questions ouvertes restent dans la pile, qui peut maintenant être considérée comme "vue détaillée alternative").
- Dialog d'édition / annotation → backlog.

## Analyse de cohérence transversale

- **Préoccupations transversales** : aucune.
- **Nouveau pattern UI** : popup modal avec liste d'items courts. `MatDialog` est déjà utilisé ailleurs (`DiscardReasonDialogComponent` dans la synthèse). Pas de duplication, on s'aligne sur l'usage existant. Pour éviter la dette de convergence : ce dialog est paramétrable par `data: { title; items; icon }` et **pourra être ré-utilisé** si d'autres blocs courts émergent (ex. si une SF future pose un bloc "alertes systèmes"). Documenté dans le commit.
- **Impact par domaine métier** : transversal, identique 3 domaines × 2 pays.

## Contrat API

Aucun.
