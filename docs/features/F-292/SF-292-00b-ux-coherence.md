# F-292 — Cadrage cohérence écran (étape 0 bis)

> Suppose étape 0 = **GO**. Feature à impact écran : modifie le rendu de la **carte d'outil** du panneau décisionnel (F-IA-04), onglet **Décision**.

## 1. Parcours écran réel de l'avocat

Ouverture du dossier → onglet **Décision** → colonne gauche = **panneau des outils décisionnels** (cartes `app-decision-tool-card`, une par outil visible) + jauge « X/N outils » (F-159) → colonne droite = verdicts/dashboard + stratégie.

Sur chaque carte aujourd'hui :
- **non calculée** : libellé de l'outil + fallback **« Cliquer pour utiliser »**, pas de bordure colorée ;
- **calculée** : libellé + valeur (`primaryValue`) + **bordure gauche** colorée selon `alertLevel` (vert/ambre/rouge).

## 2. Cartographie des écrans / zones existants

| Zone | État actuel | Touchée par F-292 ? |
|---|---|---|
| Carte d'outil (`app-decision-tool-card`) | 2 rendus (calculé / non calculé) | **OUI** — ajoute un 3ᵉ rendu « pré-rempli non calculé » |
| Bordure gauche de la carte | réservée au verdict (`alertLevel`) | **NON touchée** (canal réservé) |
| Jauge « X/N outils » (F-159) | compte les calculés | inchangée (cohérente avec le nouvel état) |
| Colonne droite (verdicts/stratégie) | — | inchangée |

## 3. Challenge écran

- **Placement** : l'affordance « pré-rempli » vit **sur la carte**, là où l'avocat décide quoi calculer. Pas de nouvelle zone, pas de nouvel onglet. ✅
- **Lisibilité de la séquence** : les 3 états forment une **progression naturelle** vierge → préparé → calculé, lisible d'un balayage de la colonne. ✅
- **Charge de l'écran cible** : risque = surcharger une carte déjà dense. → l'indicateur doit être **léger** (une pastille / un fond très pâle / un liseré), pas un bloc. ⚠️ invariant ci-dessous.
- **Collision couleur** : la **bordure gauche est sanctuarisée pour le verdict**. L'état « pré-rempli » doit donc utiliser un **autre canal** : pastille/chip « IA préparé », ou fond de carte très légèrement teinté **neutre** (gris-bleu pâle, hors palette OK/WARNING/ALERT), ou liseré pointillé. ⚠️
- **État final / continuité** : dès que l'avocat calcule, l'indicateur « préparé » **disparaît** au profit du rendu verdict (pas de cumul des deux signaux). ✅

## 4. Recommandation d'affordance (à confirmer au design/dev)

- Carte **pré-remplie non calculée** : **pastille / chip discret « IA préparé »** (icône type `auto_awesome`) + éventuel **fond très pâle neutre**, le fallback texte devenant **« Prêt à valider — cliquer pour calculer »** (au lieu de « Cliquer pour utiliser »).
- Carte **vierge** : inchangée (« Cliquer pour utiliser », aucun fond).
- Carte **calculée** : inchangée (valeur + bordure verdict).

## 5. Verdict : **GO avec ajustements**

GO, sous réserve des invariants anti-surcharge ci-dessous. La mini-spec tranchera l'affordance exacte (chip vs fond vs liseré) en respectant la charte `docs/DESIGN_SYSTEM.md`.

## 6. Invariants anti-surcharge

1. **Un seul signal à la fois** par carte : « pré-rempli » **xor** « verdict » (jamais les deux affichés simultanément).
2. **Canal distinct du verdict** : ne jamais réutiliser la bordure gauche ni les couleurs OK/WARNING/ALERT pour l'état « pré-rempli ».
3. **Légèreté** : l'indicateur tient en une pastille/chip ou un fond pâle ; il n'agrandit pas la carte ni n'ajoute de ligne de texte longue.
4. **Charte respectée** : couleurs/polices/espacements (multiples de 4px) conformes `DESIGN_SYSTEM.md` ; pas de couleur hors palette.

## 7. Enrichissement du référentiel

À répercuter dans `docs/business/parcours-ecran-dossier.md` (zone « onglet Décision / panneau outils ») : documenter les **3 états visuels** de la carte d'outil une fois F-292 livrée.
