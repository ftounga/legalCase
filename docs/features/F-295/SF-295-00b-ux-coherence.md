# F-295 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO avec ajustements

Le re-design « carte compacte repliable » de la stratégie de dossier est cohérent à l'écran : il **améliore** la charge de l'onglet Décision et la lisibilité de la séquence, sans déplacer la feature ni rompre la continuité. GO conditionné au respect des invariants anti-surcharge ci-dessous (notamment : repli ≠ suppression, actions et date préservées, état par défaut, responsive).

## Intention métier + comportement visible attendu

Re-designer l'affichage de la stratégie de dossier (F-286) dans l'onglet **Décision** : aujourd'hui un **pavé markdown complet** (`[innerHTML]`) affiché en permanence en coiffe de la colonne verdict, qui repousse le tableau de bord et les outils vers le bas et dont le texte continu est jugé peu lisible. Comportement attendu : une **carte compacte** qui montre d'emblée un **résumé en tête** (voie procédurale / posture / priorité des chefs, en quelques lignes ou chips) + un bouton **« Voir le détail »** qui **déplie** le markdown complet à la demande. Frontend pur — la stratégie reste générée et stockée à l'identique (F-286 inchangé côté génération).

## Rappel verdict feature-coherence-challenger (étape 0)

**Exempté.** F-295 est un **re-design de présentation** d'une feature déjà livrée (F-286), sans changement de workflow métier (mêmes briques amont = verdicts calculés + synthèse ; même brique aval = consultation puis conclusions). CLAUDE.md exempte l'étape 0 (cohérence fonctionnelle) pour un pur re-design ; **seule l'étape 0 bis (cohérence écran) s'applique** — c'est précisément l'objet du présent document. La cohérence fonctionnelle de la stratégie a été tranchée GO au 13ᵉ passage du parcours de référence (F-286, `docs/features/F-286/SF-286-00-coherence.md`).

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

Source : `docs/business/parcours-ecran-dossier.md` (référentiel, 14 passages) + écrans réellement codés (`case-file-detail.component.html` lignes 700-787, `case-strategy.component.*`). Parcours **vérifié sur le code**, pas hypothétique.

1. L'avocat ouvre un dossier → écran **détail du dossier**, 4 onglets (`case-file-detail`).
2. Onglet **Dossier** : métadonnées, **stade procédural** (F-243), import et liste des pièces.
3. Onglet **Analyse** : lancement du pipeline IA, accès à la **synthèse** (faits, points juridiques, risques, questions).
4. Onglet **Décision** — l'avocat arrive dans un **espace 2 colonnes** (`decision-space`, F-244) : **colonne de saisie** (outils, gauche) ↔ **colonne de verdict** (droite, sticky). Un bandeau de couplage explicite « renseignez à gauche, le verdict se consolide à droite ».
5. Il remplit les **outils décisionnels** pertinents à gauche (`app-decisional-tools-panel`), chacun dans un modal F-177 (calcul + bloc jurisprudence F-JU-01).
6. À droite, **en coiffe de la colonne verdict**, la carte **« Stratégie de dossier »** (`app-case-strategy`, F-286) consolide en lecture les verdicts CALCULÉS + la synthèse → reco (référé/fond, concilier/plaider, priorité des chefs, séquencement). ⬅ **zone re-designée par F-295**
7. Sous la stratégie, le **tableau de bord décisionnel** (`app-case-dashboard`) affiche les verdicts agrégés.
8. Plus bas dans la même colonne, une **carte CTA « Projet de conclusions »** (F-267) mène à la page dédiée pleine largeur `/case-files/:id/conclusions`.
9. L'avocat **génère puis relit le projet de conclusions** sur cette page dédiée (F-98), consolidation de synthèse + stade + outils + jurisprudence.
10. Il copie / exporte le projet, le finalise dans son traitement de texte.
11. Onglet **Suivi** : échéances, notes, calendrier procédural jusqu'à l'audience.
12. **État terminal** : projet de conclusions généré (cf. section dédiée).

## État terminal du processus (explicite)

✅ Inchangé et déjà tranché (cadrage F-98, 2026-05-18) : l'état terminal métier = **« projet de conclusions généré »** (page `/conclusions`). F-295 ne touche pas l'état terminal — elle agit en **amont** (consultation de la stratégie, étape 6), sur la **présentation** d'un intrant décisionnel. Le chemin vers l'état terminal (lien « Rédiger mes conclusions » dans le pied de la carte + carte CTA conclusions dessous) doit rester intact, y compris à l'état replié.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| 4. Espace décisionnel 2 colonnes | `decision-space` (`case-file-detail.html:705`) | ✅ existant (F-244) |
| 5. Saisie des outils | `app-decisional-tools-panel` (col. gauche, `:710`) | ✅ existant |
| **6. Stratégie de dossier (coiffe verdict)** | **`app-case-strategy` (col. verdict, `:733`)** | ✅ existant (F-286) — **zone re-designée F-295** |
| 7. Tableau de bord décisionnel | `app-case-dashboard` dans `.decisional-summary-panel` (`:738`) | ✅ existant (F-184) |
| 8. Accès conclusions | `.conclusions-cta` (`:763`) | ✅ existant (F-267) |
| 9. Génération conclusions | `app-conclusions-section`, route `/conclusions` | ✅ existant (F-98/F-267) |

La carte stratégie n'est **pas** un bloc primaire : le référentiel la classe explicitement « coiffe du tableau de bord, pas un bloc primaire » (`parcours-ecran-dossier.md:49`). L'onglet Décision reste à 3 blocs primaires (outils, tableau de bord, accès conclusions). F-295 ne change pas ce comptage.

## Position candidate de la feature (écran, zone, points d'entrée)

**Aucun déplacement.** La feature reste **exactement où elle est** : onglet Décision → colonne de verdict (`decision-space__verdict`) → coiffe, au-dessus du tableau de bord (`case-file-detail.html:733`). F-295 ne change que le **rendu interne** du composant `app-case-strategy` :
- **état par défaut** : carte compacte = en-tête (titre + pilule « N verdicts pris en compte ») + **résumé** (quelques lignes / chips : voie procédurale, posture, priorité des chefs) + pied (date + « Rédiger mes conclusions » + « Régénérer »).
- **point d'entrée du détail** : bouton **« Voir le détail »** dans la carte → déplie le markdown complet (`contentHtml()`, déjà calculé) au sein de la même carte. Repli/dépli **in situ**, sans navigation ni modal.

Les états vides (`strat-empty`, `strat-empty--input` F-258) et l'état génération (`strat-generating`) restent inchangés — ils ne portent pas de markdown long à replier.

## Challenge placement

*La stratégie doit-elle rester en coiffe de la colonne verdict ?* **Oui.** À l'étape 6, l'avocat vient de calculer ses outils (étape 5) et lit la lecture de plus haute altitude de leurs verdicts juste avant de descendre dans le détail du tableau de bord. Le placement « coiffe verdict » est celui validé au 13ᵉ passage et n'est pas en cause : le test 2026-06-15 reproche la **place prise** (pavé permanent), pas l'**emplacement**. Le re-design répond au reproche **sans bouger** la feature. Placement : ✅ conservé.

## Challenge lisibilité de la séquence

L'UI rend déjà visible l'ordre des étapes : bandeau de couplage (saisie gauche → verdict droite, `:686`), colonne verdict ordonnée stratégie → tableau de bord → conclusions. F-295 **renforce** la lisibilité de la séquence : aujourd'hui le pavé markdown intégral en tête **noie** le passage à l'œil entre « lecture de synthèse stratégique » et « tableau de bord détaillé » ; un résumé compact rend la transition stratégie → tableau de bord plus nette (haute altitude condensée, puis détail des verdicts). **Point de vigilance** : le **résumé compact doit rester fidèle** à la hiérarchie de la reco (la voie procédurale / posture / priorité des chefs sont les axes que F-286 produit déjà) — sinon le résumé devient un 4ᵉ contenu décorrélé du détail. Séquence : ✅ améliorée, sous réserve que résumé et détail soient la même information à deux niveaux de zoom.

## Challenge charge écran

C'est **le point central du test**. La colonne verdict empile : stratégie (coiffe) + tableau de bord + carte CTA conclusions. Aujourd'hui la stratégie affiche un **markdown complet en permanence** → en coiffe, elle repousse le tableau de bord et la suite « sous la ligne de flottaison », alors que c'est la zone que l'avocat veut atteindre après une lecture rapide. La carte compacte **réduit la hauteur par défaut** de la coiffe → le tableau de bord remonte dans le champ de vision. C'est exactement la réponse au « prend trop de place ». Charge : ✅ **réduite** par le re-design (aucun bloc ajouté, hauteur par défaut diminuée). Aucun éclatement en onglet/écran/drawer nécessaire : le repli in situ suffit, et est même préférable (la stratégie reste contextuellement collée à ses verdicts).

## Challenge état final / continuité

Après lecture (résumé ou détail déplié), l'avocat : (a) descend vers le tableau de bord (dessous), ou (b) part rédiger ses conclusions via « Rédiger mes conclusions » / la carte CTA, ou (c) régénère. Ces 3 sorties existent et **doivent toutes survivre au re-design** :
- le **pied de carte** (date de génération + lien conclusions + bouton Régénérer) reste visible **à l'état replié comme déplié** — il n'est pas dans la zone repliable ;
- le **détail markdown** n'est jamais supprimé : repli ≠ perte, dépli toujours possible ;
- **pas de dead-end** : replier ne masque aucune action, déplier n'ouvre aucun cul-de-sac (pas de modal bloquant, pas de navigation hors écran).
Continuité : ✅ préservée sous réserve des invariants.

## Ajustements IA requis

1. **Repli ≠ suppression** : le bouton « Voir le détail » déplie le markdown complet **dans la carte** (in situ), réversible (« Masquer le détail »). Jamais de navigation, modal ou suppression du contenu.
2. **Résumé = même information à deux niveaux de zoom** : le résumé compact (voie / posture / priorité des chefs) doit dériver de la reco F-286 existante, pas introduire une information nouvelle décorrélée du détail.
3. **Pied de carte hors zone repliable** : date de génération + lien « Rédiger mes conclusions » + bouton « Régénérer » restent visibles à l'état replié.
4. **État par défaut = replié (compact)** : la carte s'ouvre compacte ; c'est la finalité même de la feature.
5. **Responsive** : sous 1024 px la colonne verdict s'empile sous la saisie ; la carte compacte doit rester lisible (résumé + bouton) et le dépli ne doit pas casser l'empilement mobile (`@media max-width: 640px` déjà présent).
6. **Charte inchangée** : navy/or, Merriweather/Inter/JetBrains Mono ; pas de 4ᵉ couleur introduite par les chips de résumé (réutiliser pilule/divider existants). *(Rappel : le style fin relève de DESIGN_SYSTEM / étape 3 ; ici on ne fixe que la contrainte « pas de nouvelle couleur ».)*

## Invariants anti-surcharge pour la mini-spec

- **INV-1 — Repli jamais destructeur** : le markdown complet de la stratégie reste accessible par dépli, jamais supprimé, jamais déporté hors de la carte (pas de modal/route).
- **INV-2 — Actions et date préservées** : « Régénérer », « Rédiger mes conclusions » et la date de génération restent visibles à l'état replié comme déplié.
- **INV-3 — État par défaut replié** : la carte s'affiche compacte par défaut ; le dépli est une action explicite de l'avocat.
- **INV-4 — Pas de bloc primaire nouveau** : l'onglet Décision reste à 3 blocs primaires ; la stratégie demeure une carte de coiffe, pas un 4ᵉ bloc.
- **INV-5 — Pas de déplacement** : la feature reste en coiffe de `decision-space__verdict`, au-dessus du tableau de bord ; aucun changement de zone, route ou point d'entrée.
- **INV-6 — Résumé fidèle** : le résumé compact est une vue condensée de la reco existante, pas une nouvelle production ni un nouvel appel LLM.
- **INV-7 — Frontend pur** : aucune écriture en base, aucune migration, aucun appel LLM nouveau ; F-286 (génération/stockage) strictement inchangé. GET stratégie reste en lecture seule.
- **INV-8 — États vides intacts** : `strat-empty`, `strat-empty--input` (F-258) et `strat-generating` ne sont pas concernés par le repli (rien à replier) — comportement préservé.
- **INV-9 — Responsive préservé** : compact + dépli fonctionnent dans la colonne empilée < 1024 px et le breakpoint 640 px existant.
- **INV-10 — Pas de 4ᵉ couleur** : chips/résumé réutilisent navy/or/divider existants (cf. DESIGN_SYSTEM).

## Décision finale

**GO avec ajustements.** Le re-design « carte compacte repliable » est la bonne réponse au reproche du test (encombrement de la coiffe de la colonne verdict) : il **réduit** la charge de l'écran et **renforce** la lisibilité de la séquence, **sans déplacer** la feature ni rompre la continuité vers l'état terminal. La mini-spec doit intégrer les 10 invariants anti-surcharge (en particulier INV-1, INV-2, INV-3, INV-7). Aucun pré-requis bloquant (pas de refonte d'écran saturé nécessaire).

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` : entrée d'historique ajoutée (15ᵉ passage, F-295) + note sur la zone stratégie précisant que son affichage est une **carte compacte repliable** (résumé par défaut + dépli du détail), la classant toujours « coiffe, pas un bloc primaire ».
