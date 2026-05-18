# F-242 — Cadrage cohérence écran (étape 0 bis)

> Produit via la skill `ai-skills/screen-coherence-challenger.md`.

## Verdict : GO avec ajustements

## Intention métier + comportement visible attendu

L'avocat saisit, **sous chaque point juridique de la synthèse**, la (les) référence(s) de jurisprudence d'appui qu'il a retenue(s) — référence + une ligne de portée. Visible : un champ compact « Jurisprudence à l'appui » par point juridique, à proximité du bouton deeplink F-241. Ces citations sont ensuite reprises dans les conclusions générées.

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** (`SF-242-00-coherence.md`) — toutes les briques amont (F-241 connecteur deeplink) et le débouché aval (F-98 conclusions) sont livrés. Réserve : déclencheur « ≥ 5 signaux terrain » non formellement atteint, dev sur override product owner.

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

Source : `docs/business/parcours-ecran-dossier.md` (référentiel, 3 passages : F-243 / F-98 / F-244) + écrans réellement codés (`case-file-detail`, `synthesis.component`). ⚠ L'étape 7 (le « retour » de jurisprudence) est reconstruite — **hypothèse à valider auprès d'un avocat**.

1. Ouvre le dossier → écran détail dossier, **4 onglets** (Dossier / Analyse / Décision / Suivi — depuis F-244).
2. Onglet **Dossier** — pièces, identité, stade procédural.
3. Onglet **Analyse** — lance l'analyse ; consulte la **synthèse** (sous-écran de l'onglet Analyse).
4. Sur la synthèse : lit la chronologie, les faits, les **points juridiques**, les risques.
5. Pour un point juridique, ouvre Doctrine / Lexis Plus / Lextenso via le bouton deeplink **F-241**.
6. Lit les arrêts chez l'éditeur, **sélectionne** ceux qui appuient son argumentaire, revient sur LegalCase.
7. **(F-242)** Saisit la (les) référence(s) d'appui sous le point juridique concerné.
8. Onglet **Décision** — outils décisionnels, tableau de bord, contrôle de cohérence F-IA-03.
9. Génère le **projet de conclusions** (F-98, bas de l'onglet Décision) — qui reprend les citations F-242.
10. Relit / édite / exporte les conclusions (SF-98-49 / 50 / 51).
11. **État terminal — projet de conclusions généré et exporté.**
12. Onglet **Suivi** — accompagnement procédural post-conclusions.

## État terminal du processus (explicite)

✅ Déjà tranché par le cadrage écran F-98 (`SF-98-00b-ux-coherence.md`, 2026-05-18) : l'état terminal du traitement métier d'un dossier = **« projet de conclusions généré »**. F-242 **ne déplace pas** cet état terminal — il enrichit l'**input** de l'étape 9 (génération). Son output n'est pas un nouvel état terminal : c'est un intrant de l'état terminal existant.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| 2 onglet Dossier | `case-file-detail` onglet Dossier | ✅ existant |
| 3 onglet Analyse → synthèse | `synthesis.component` (sous-écran) | ✅ existant |
| 4 points juridiques | panneau repliable `section-points-juridiques` | ✅ existant |
| 5 recherche jurispru (aller) | boutons deeplink **F-241** | ✅ existant |
| 6 lecture / sélection des arrêts | éditeur tiers (Doctrine…) | hors LegalCase |
| 7 **saisie citation d'appui (retour)** | **F-242 (la feature challengée)** | 🟡 backlog |
| 8 onglet Décision | outils décisionnels + tableau de bord | ✅ existant |
| 9 génération conclusions | `app-conclusions-section` (onglet Décision) | ✅ existant |
| (adjacent) vérif jurispru des documents | section **F-179** `app-jurisprudence-citations-section` | ✅ existant |

## Position candidate de la feature

- **Écran** : sous-écran **synthèse** (onglet Analyse).
- **Zone** : à l'intérieur du panneau repliable **existant** `section-points-juridiques`, en **per-item** — sous chaque point juridique.
- **Point d'entrée** : aucun nouveau. L'avocat est déjà sur la synthèse, en train de lire les points juridiques (étape 4-7). Le champ F-242 est immédiatement adjacent au bouton deeplink F-241 du même point juridique.

## Challenge placement

✅ **Correct.** L'étape 7 du parcours (le « retour ») se joue exactement là : sur le point juridique, au moment où l'avocat a la référence en main. Co-localiser F-242 avec F-241 referme la boucle au même endroit (l'« aller » et le « retour » sur le même point juridique). Alternative écartée : une section ou un écran dédié → romprait l'adjacence aller/retour et imposerait un aller-retour subi.

## Challenge lisibilité de la séquence

La boucle **aller (F-241) → retour (F-242)** doit être lisible sans action : sur un point juridique, le bouton « Ouvrir dans Doctrine » puis le champ « Jurisprudence à l'appui » co-localisés, dans cet ordre. Risque si F-242 est posé loin de F-241 : l'avocat ne perçoit pas que c'est le retour de la même action → ajustement requis (adjacence imposée).

## Challenge charge écran

Point clé. **F-242 n'ajoute aucun bloc primaire** sur l'écran synthèse. Blocs primaires actuels : badge de risque, callout de streaming, badges de synthèse, puis panneaux repliables timeline / faits / points juridiques / risques / questions ouvertes / **« Jurisprudences citées » (F-179)** / chat. F-242 enrichit les **items** d'un panneau **déjà existant et repliable** (`section-points-juridiques`) — le nombre de blocs primaires est **inchangé**.

⚠️ **Coexistence de 3 briques « jurisprudence »** sur la synthèse après F-242 :
- **F-241** — boutons deeplink, par point juridique (chercher chez l'éditeur).
- **F-242** — champ d'appui, par point juridique (ce que l'avocat ramène).
- **F-179** — section « Jurisprudences citées », vérification des arrêts trouvés **dans les documents** du dossier.

Risque de confusion fonctionnelle, pas de surcharge structurelle. Ajustement requis : libellés explicitement distincts, **ne pas fusionner** F-242 dans la section F-179 (rôles différents : F-179 *vérifie* ce qui est dans les documents ; F-242 = ce que l'avocat *ajoute* comme appui).

## Challenge état final / continuité

Après saisie d'une citation, l'avocat continue vers l'onglet Décision → génération des conclusions (étape 9). Continuité à garantir :
- (a) la citation apparaît effectivement dans le projet de conclusions généré (invariant fonctionnel de l'étape 0) ;
- (b) si une version de conclusions **existe déjà**, ajouter / modifier une citation doit marquer cette version `stale` (bandeau « à régénérer » SF-98-53) — sinon l'avocat croit ses conclusions à jour alors qu'elles ignorent la nouvelle citation.

Pas de dead-end : la citation a un débouché explicite (les conclusions).

## Ajustements IA requis

À intégrer dans la mini-spec :

1. **Adjacence F-241 / F-242** — le champ de saisie F-242 est placé par point juridique, immédiatement à côté du bouton deeplink F-241, dans l'ordre aller → retour.
2. **Compacité** — champ référence + portée (1 ligne) ; discret / replié quand aucune citation n'est saisie, pour ne pas alourdir la liste des points juridiques.
3. **Libellé distinct de F-179** — distinguer clairement « Jurisprudence à l'appui » (F-242, ajoutée par l'avocat) de la section « Jurisprudences citées » (F-179, vérification des documents). Pas de fusion.
4. **Continuité SF-98-53** — l'ajout / modification / suppression d'une citation rend les versions de conclusions déjà générées `stale`.

## Invariants anti-surcharge pour la mini-spec

1. F-242 n'ajoute **aucun nouveau bloc primaire** sur l'écran synthèse — uniquement un enrichissement per-item du panneau « Points juridiques » existant.
2. Le champ de saisie reste **compact** (référence + portée) — pas de sous-formulaire déployé ni de modale lourde.
3. Les **3 briques jurisprudence** (F-179 / F-241 / F-242) gardent des libellés distincts et un rôle lisible ; aucune fusion.
4. Toute citation saisie a une **continuité explicite** : reprise dans les conclusions générées + déclenchement du `stale` SF-98-53.

## Décision finale

**GO avec ajustements.** Le placement est juste (per-item dans le panneau Points juridiques existant, adjacent à F-241), l'écran n'absorbe aucun bloc primaire nouveau, l'état terminal est inchangé. Les 4 ajustements ci-dessus (adjacence, compacité, libellé distinct de F-179, continuité `stale`) sont à intégrer dans la mini-spec.

Étape suivante : **étape 1 — mini-spec** (`SF-242-01-*`), avec arbitrage des options techniques (recommandation : option δ « citation manuelle structurée » de PRODUCT_SPEC).

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` — 4ᵉ passage (F-242) : la note « écran synthèse » est enrichie pour acter que le panneau « Points juridiques » héberge désormais le **retour de la chaîne jurisprudence** (F-242), et la coexistence des 3 briques jurisprudence (F-179 vérification documents / F-241 aller / F-242 retour).
