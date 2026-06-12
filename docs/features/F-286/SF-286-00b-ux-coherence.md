# SF-286-00b — Cadrage de cohérence écran (étape 0 bis)

**Feature** : F-286 — Stratégie de dossier unifiée
**Skill** : `ai-skills/screen-coherence-challenger.md`
**Date** : 2026-06-12
**Pré-requis** : étape 0 GO (`SF-286-00-coherence.md`).

---

## 1 — Parcours écran réel de l'avocat (rappel)

Détail du dossier = **4 onglets** (F-244) : Dossier (0), Analyse (1), Décision (2), Suivi (3). Contenus rendus
en permanence, masqués via `[hidden]`. Source : `docs/business/parcours-ecran-dossier.md`.

Parcours pertinent pour F-286 :
1. Onglet **Analyse** → l'avocat lit la **synthèse** (faits / risques / pistes).
2. Onglet **Décision** → il **remplit les outils** (`app-decisional-tools-panel`, colonne gauche « saisie »).
3. Onglet **Décision** → il consulte le **tableau de bord décisionnel** (`app-case-dashboard`, colonne droite
   « verdict », sticky) — **verdicts agrégés** des outils calculés.
4. Onglet **Décision** → carte CTA vers le **projet de conclusions** (page dédiée).

L'avocat consulte donc **déjà les verdicts agrégés dans la colonne droite de l'onglet Décision**. C'est là qu'il
raisonne « qu'est-ce que tout ça veut dire stratégiquement ? ».

---

## 2 — Où placer la recommandation stratégique ? (candidats)

| Candidat | Pour | Contre | Verdict |
|---|---|---|---|
| **Onglet Décision, colonne verdict, AU-DESSUS du tableau de bord** | L'avocat y consulte déjà les verdicts agrégés ; la reco est la lecture de plus haute altitude des mêmes verdicts → continuité parfaite. Sticky → reste visible pendant la saisie. | Risque d'alourdir la colonne. | ✅ **RETENU** |
| Onglet Synthèse (`/case-files/:id/synthesis`) | La synthèse y vit déjà. | La synthèse décrit le dossier (faits/risques) ; elle n'a **pas** les verdicts calculés des outils → l'avocat devrait faire l'aller-retour. La stratégie consolide les **verdicts**, pas la synthèse seule. | ❌ |
| Onglet Suivi | — | Le Suivi = cycle de vie / échéances, pas raisonnement stratégique de fond. Hors sujet. | ❌ |
| Nouvel onglet « Stratégie » | Visibilité dédiée. | Casse la structure F-244 à 4 onglets ; fragmente le raisonnement stratégie/verdicts sur 2 onglets. | ❌ |

**Décision (réversible, tracée)** :
- gate : placement écran de la reco stratégique ;
- décision : **colonne verdict de l'onglet Décision, juste au-dessus du tableau de bord décisionnel** ;
- pourquoi : l'avocat y consulte déjà les verdicts agrégés → la reco est leur lecture de plus haute altitude,
  continuité de lecture sans aller-retour inter-écrans ;
- alternative : onglet Synthèse (rejetée — n'a pas les verdicts calculés) ;
- réversible : oui (déplacement d'un composant front, aucune contrainte de données).

---

## 3 — Charge de l'écran cible : seuil des 3 blocs primaires (POINT DUR)

`parcours-ecran-dossier.md` impose : **un onglet ne dépasse pas ~3 blocs primaires**. L'onglet Décision en porte
déjà 3 : `app-decisional-tools-panel`, `app-case-dashboard`, carte CTA conclusions.

**Contrainte respectée** : la reco stratégique n'est **PAS un 4ᵉ bloc primaire**. Elle est un **en-tête de
synthèse de la colonne verdict** — conceptuellement la **même brique « consolidation des verdicts »** que le
tableau de bord, dont elle est la lecture haute. Elle vit **dans** la section `decision-space__verdict`, attachée
au tableau de bord (au-dessus), comme une carte de coiffe. Précédent : F-JU-01 / F-206 / F-214 ont tous enrichi
l'**intérieur** de blocs existants sans en créer de nouveau. Même discipline ici.

> Invariant anti-surcharge : la reco stratégique coiffe le tableau de bord décisionnel ; elle ne devient jamais
> un bloc primaire autonome ni un onglet. Si la colonne verdict devenait trop lourde, on replie la reco
> (carte avec corps masquable), on n'ajoute pas de bloc.

---

## 4 — Lisibilité de la séquence + état final / continuité

- **Séquence amont** : l'avocat calcule des outils (gauche) → le tableau de bord se remplit (droite) → la reco
  stratégique coiffe le tableau et **lit ces verdicts**. La continuité saisie → verdict → stratégie est
  visuellement linéaire de gauche à droite puis de haut en bas dans la colonne droite.
- **État « pas encore calculé »** (POINT DUR, reprise F-258) : tant que peu/pas d'outils sont calculés, la reco
  **n'invente rien**. Elle affiche un **encart honnête** « Calculez vos outils décisionnels pour obtenir une
  recommandation stratégique fondée » + compteur d'outils restant à calculer (semantique F-258), et un CTA
  « Générer la stratégie » désactivé/explicite. Un signal de stratégie ne doit jamais être orphelin de ses
  données.
- **État de chargement** : génération LLM → état soigné (spinner + « Analyse stratégique en cours… »), jamais
  d'écran figé. Le composant suit OnPush + `markForCheck`.
- **État final / continuité aval** : la reco est consultable et copiable ; elle peut éclairer la rédaction des
  conclusions (page dédiée), sans rompre l'état terminal « projet de conclusions généré » (inchangé). Un lien
  léger « Rédiger mes conclusions » depuis la reco renforce le maillon (réutilise le CTA existant).

---

## 5 — Invariants anti-surcharge pour la mini-spec

1. La reco stratégique est une **carte de coiffe de la colonne verdict** (onglet Décision), **jamais** un 4ᵉ bloc
   primaire ni un onglet.
2. Design = gabarit F-282 (carte « document » max-width raisonnable, navy/or, Merriweather/Inter/JetBrains Mono,
   pills sémantiques, états vides/chargement soignés, CTA navy).
3. **Honnêteté du vide** : aucun rendu de stratégie quand 0 outil calculé → encart F-258 + CTA explicite.
4. La reco **ne déplace, ne masque, ne réordonne aucun outil** ni le tableau de bord — pur ajout en lecture.
5. Date de génération affichée en JetBrains Mono ; reco regénérable (le dossier vit, les verdicts changent).

---

## 6 — Verdict

## ✅ GO

Placement cohérent (colonne verdict de l'onglet Décision, au-dessus du tableau de bord), seuil des 3 blocs
primaires **respecté** (carte de coiffe, pas de 4ᵉ bloc), états vides/chargement/terminal traités, invariant
1 outil = 1 situation préservé (pur ajout en lecture). Enrichissement du référentiel ci-dessous.

→ Étape suivante : mini-spec SF-286-01.
