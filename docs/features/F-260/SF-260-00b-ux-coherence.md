# F-260 — Cadrage cohérence écran (étape 0 bis)

> Numérotation persistante & ordre des pièces. Skill : `ai-skills/screen-coherence-challenger.md`. 2026-06-09.

## Verdict : **GO avec ajustements**

## Intention + comportement visible

Dans la **liste des documents** (onglet Dossier), chaque document/pièce affiche son **numéro de pièce stable** ; l'avocat peut **réordonner** les documents, ce qui (re)numérote explicitement — le numéro ne glisse jamais tout seul à l'ajout/suppression.

## Rappel étape 0

**GO** (`SF-260-00-coherence.md`) — fiabilisation fondatrice, pas de trou amont ; arbitrage granularité/stabilité renvoyé à la mini-spec.

## Parcours écran réel

1. Onglet **Dossier** → section **Documents** (`#section-documents`, `case-file-detail.component.html:298`, `mat-table docs-table` sur `documents()`).
2. L'avocat ajoute/supprime des documents (boutons existants).
3. **[NOUVEAU]** Chaque ligne affiche un **n° de pièce** ; l'avocat peut **réordonner** (poignée drag ou flèches haut/bas) → renumérotation.
4. Onglet **Décision** : conclusions / fiche prud'homale citent ces numéros (renvois + bordereau SF-98-57).
5. État terminal inchangé : « projet de conclusions généré » (renvois désormais stables).

## Cartographie

| Étape | Zone | Statut |
|---|---|---|
| Liste documents | `case-file-detail` `#section-documents` (mat-table) | ✅ existant |
| **N° de pièce affiché + réordonnancement** | même table (colonne n° + affordance d'ordre) | 🆕 à ajouter |
| Édition pièce (type/label) | `piece-edit-dialog` (F-145) | ✅ existant |
| Renvois/bordereau | onglet Décision (F-98 / fiche / SF-98-57) | ✅ / 🟡 |

## Position candidate

Table des documents (onglet Dossier) : **colonne « N° »** + **affordance de réordonnancement** par ligne. Aucun nouvel écran, aucun nouveau bloc primaire (la section Documents existe déjà).

## Challenge placement
✅ Cohérent — l'avocat numérote/ordonne ses pièces là où il les gère (Dossier), pas ailleurs.

## Challenge lisibilité séquence
⚠️ Ajustement : rendre visible que **réordonner renumérote** (les actes citeront ces n°). Mention discrète ou libellé d'action explicite (« Réordonner les pièces »). Pas de renumérotation subie : l'ordre par défaut est déterministe, le changement est une action avocat.

## Challenge charge écran
✅ Aucune surcharge — une colonne + une poignée dans une table existante. La section Documents reste un bloc primaire unique.

## Challenge état final / continuité
✅ Le numéro stable se propage aux actes (F-98, fiche) et au bordereau (SF-98-57). Pas de dead-end.

## Ajustements IA requis (mini-spec)
1. Colonne « N° » dans la table des documents.
2. Réordonnancement (drag ou flèches) → endpoint de (re)numérotation/ordre persistant.
3. Indiquer que l'ordre pilote la numérotation citée dans les actes.

## Invariants anti-surcharge
1. Pas de nouvel écran ni bloc primaire : tout dans la table Documents existante.
2. Réordonnancement = action explicite ; jamais de glissement automatique des numéros.
3. Le n° affiché = le n° persistant lu par F-98 **et** la fiche prud'homale (source unique).

## Décision finale
**GO avec ajustements.** Insertion naturelle dans la table Documents (colonne n° + réordonnancement), aucune surcharge ; rendre lisible que l'ordre pilote la numérotation des actes. Mini-spec peut démarrer.

## MAJ parcours de référence
`docs/business/parcours-ecran-dossier.md` : la section Documents (onglet Dossier) porte désormais un **numéro de pièce stable + réordonnancement** alimentant les renvois « Pièce n° X » et le bordereau des actes (onglet Décision).
