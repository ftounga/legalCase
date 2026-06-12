# F-283 — Cadrage cohérence écran (étape 0 bis)

> Skill `screen-coherence-challenger`. Date : 2026-06-12. Suite de l'étape 0 GO (`SF-283-00-coherence.md`). Exigence PO « beauté écran premier ordre » (gabarit = frise F-282 `contradictoire-timeline`).

## Verdict : **GO avec ajustements**

## Intention métier + comportement visible attendu
Rendre **visible la vie du dossier** :
- **SF-283-01** : une **frise de phases procédurales datées** (Saisine → Conciliation → Fond → Appel → Cassation) dans l'onglet **Suivi**, avec la phase courante en exergue.
- **SF-283-02** : quand des pièces ont été ajoutées **depuis la dernière analyse**, une **carte « vague de pièces »** lisible (combien, lesquelles, depuis quand) avec l'action « relancer l'analyse » — au lieu de l'avertissement plat actuel.

## Parcours écran réel (ouverture dossier → état terminal)
1. Ouvre le dossier → 4 onglets (Dossier / Analyse / Décision / Suivi) + en-tête `app-case-dashboard-stepper`.
2. Onglet **Dossier** (`#section-documents`) : tableau des pièces (upload, n° de pièce, tag adverse). **[existant]** un avertissement plat « N documents récents non couverts » apparaît sur l'onglet Analyse.
3. Onglet **Analyse** : synthèse.
4. Onglet **Décision** : outils → tableau de bord → conclusions.
5. Onglet **Suivi** : **[F-282]** frise contradictoire → `app-case-deadlines-section` (échéances) → `app-case-notes-section` (notes).
6. **[NOUVEAU SF-283-01]** En tête de l'onglet **Suivi**, **avant la frise contradictoire**, une **frise des phases procédurales** (où en est le dossier dans son cycle juridictionnel).
7. **[NOUVEAU SF-283-02]** En tête de l'onglet **Dossier**, **au-dessus du tableau des pièces**, une **carte « vague de pièces »** quand `pendingPieces > 0` — sinon rien (ou un micro-état « à jour » discret).
8. **État terminal** : le dossier n'est plus figé — il porte sa **phase courante** et signale clairement quand l'analyse est **en retard sur les pièces**. La rétention naît de cette continuité visible.

## Cartographie écrans / zones ↔ parcours
| Étape | Zone LegalCase | Statut |
|---|---|---|
| Tableau des pièces | Onglet Dossier, `#section-documents` | ✅ existant |
| Avertissement « docs récents non couverts » | Onglet Analyse, ligne plate `synthesis-outdated` | ✅ existant — *à élever (SF-283-02)* |
| Frise contradictoire | Onglet Suivi, `#section-contradictoire` (F-282) | ✅ existant |
| Échéances / notes | Onglet Suivi | ✅ existant |
| **Frise des phases procédurales** | **Onglet Suivi, en tête (`#section-phases`, avant le contradictoire)** | 🆕 SF-283-01 |
| **Carte « vague de pièces »** | **Onglet Dossier, en tête (`#section-pieces-wave`, avant le tableau)** | 🆕 SF-283-02 |
| Stade courant (libellé) | Onglet Décision (`app-procedure-stage-section`, F-243) | ✅ existant — *non touché* |

## Position candidate + challenge placement
- **Frise des phases (SF-283-01) → onglet Suivi, en première position.** La phase est par nature **procédurale/temporelle** → même maison que la frise contradictoire et les échéances. Placée **avant** le contradictoire car elle est le cadre le plus large (le dossier traverse des phases ; à l'intérieur d'une phase il y a des rounds). ✅ Cohérent. Ordre Suivi : **Phases → Contradictoire → Échéances → Notes** (du macro au micro).
- **Carte « vague de pièces » (SF-283-02) → onglet Dossier, en tête.** L'avocat *ajoute* ses pièces sur l'onglet Dossier → c'est là qu'il doit voir « ces N pièces ne sont pas encore dans l'analyse ». L'action (relancer) y est naturelle. ✅ On **déplace l'intelligence** de l'avertissement plat (Analyse) vers une carte structurée (Dossier), tout en **conservant** un rappel discret côté Analyse (continuité, pas de doublon criard).

## Challenge lisibilité de la séquence
⚠️ **Ajustement (anti-doublon avertissement).** L'avertissement plat `synthesis-outdated` (onglet Analyse) et la nouvelle carte « vague de pièces » (onglet Dossier) ne doivent pas faire **double emploi visuel**. Décision : la **carte riche** vit sur Dossier ; l'avertissement Analyse est **conservé tel quel mais simplifié** (une ligne renvoyant « voir les pièces récentes » sur l'onglet Dossier). Une seule source structurée.

## Challenge charge écran (invariant 3 blocs)
✅ **Pas de surcharge.**
- Onglet **Suivi** : aujourd'hui 3 zones (contradictoire, échéances, notes). SF-283-01 ajoute la frise de phases **en tête**. → 4 zones. **Ajustement** : la frise de phases est **compacte** (une bande horizontale d'étapes, pas un grand bloc-carte plein), pour que l'onglet ne devienne pas lourd. Elle coiffe le contradictoire comme un fil conducteur — visuellement subordonnée, pas un 4ᵉ bloc concurrent.
- Onglet **Dossier** : la carte « vague de pièces » n'apparaît **que lorsqu'il y a une vague** (`pendingPieces > 0`). 0 surcharge à l'état nominal (dossier à jour).

## Challenge état final / continuité
✅ **Continuité = cœur de F-283.** Phase courante toujours visible (Suivi) ; vague de pièces signalée dès l'upload (Dossier). Pas de dead-end : chaque vague porte son CTA « relancer l'analyse » ; chaque phase porte sa date. État « à jour » = absence discrète de carte (silence > placeholder criard).

## Exigence design impérative (gabarit = frise F-282)
- **Charte `DESIGN_SYSTEM.md`** : navy `#1A3A5C` / or `#C9973A`, Merriweather (titres d'étape/phase), Inter (corps), JetBrains Mono (dates) ; espacements multiples de 4px.
- **Frise des phases** : bande d'étapes type stepper horizontal (puce + libellé + date `JetBrains Mono`), phase courante en navy plein + pulsation discrète sur la puce courante (réutilise le langage `contra-dot--pulse`). Colonne « document » (max-width raisonnable, pas d'étirement plein écran).
- **Carte « vague de pièces »** : carte navy/or sobre, badge compteur or, liste des pièces récentes (nom + date mono), CTA primaire navy « Relancer l'analyse ». États vides/chargement soignés. Champs date **natifs `type=date` lang=fr-FR** (le formulaire d'édition de phase) — **PAS MatDatepicker**.
- Zéro tableau brut, zéro « AI-generic », micro-interactions sobres.

## Invariants anti-surcharge pour la mini-spec
1. **Frise de phases compacte** (bande, pas grand bloc) — coiffe le contradictoire, ne le concurrence pas.
2. **Carte vague de pièces conditionnelle** (`pendingPieces > 0`) — invisible quand le dossier est à jour.
3. **Une seule source structurée du delta de pièces** : carte riche sur Dossier ; l'avertissement Analyse devient un renvoi simple (pas de doublon).
4. **Pas d'écran vide** : 0 phase → « Phase 1 — Saisine » par défaut ; 0 vague → pas de carte.
5. **F-243 non touché** : `app-procedure-stage-section` (Décision) reste le libellé « stade courant ». La frise de phases lit/affiche, ne supprime pas F-243.
6. **Beauté = critère d'acceptation** (revue visuelle PO).

## Décision finale
**GO avec ajustements.** Placements naturels (phases dans Suivi en tête, vague de pièces dans Dossier en tête), aucun bloc primaire concurrent (frise compacte + carte conditionnelle), continuité assurée, anti-doublon de l'avertissement traité. Enrichit `parcours-ecran-dossier.md` (frise de phases ; carte vague de pièces ; dossier vivant).
