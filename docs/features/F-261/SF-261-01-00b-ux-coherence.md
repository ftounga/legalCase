# SF-261-01 — Cadrage cohérence écran (étape 0 bis)

> Tag « écritures adverses » au niveau document. Skill : `ai-skills/screen-coherence-challenger.md`. 2026-06-10.

## Verdict : **GO avec ajustements**

## Intention + comportement visible
Dans la **table des documents** (onglet Dossier), l'avocat peut **marquer un document comme « écritures adverses »** (les conclusions de la partie adverse). Ce marquage désigne le document dont les **moyens** seront extraits (SF-261-02) puis réfutés (SF-261-03).

## Rappel étape 0
F-261 **GO avec ajustements** (`SF-261-00-coherence.md`), décision PO **Option A** (tag doc + extraction IA). SF-261-01 = le tag.

## Parcours écran réel
1. Onglet **Dossier** → section **Documents** (`case-file-detail` `#section-documents`, `mat-table docs-table`) — porte déjà colonne « N° » + réordonnancement (F-260).
2. L'avocat uploade les écritures adverses comme document.
3. **[NOUVEAU]** Sur la ligne du document, une action **« Écritures adverses »** (toggle) le marque comme tel.
4. (SF-261-02) Les moyens en sont extraits ; (SF-261-03) l'acte les réfute.
5. État terminal inchangé : « projet de conclusions généré » — désormais en réponse aux moyens adverses.

## Cartographie
| Zone | Statut |
|---|---|
| Table documents (`#section-documents`) | ✅ existant |
| **Marqueur « écritures adverses » par ligne** | 🆕 à ajouter |
| Édition pièce / N° / réordonnancement | ✅ existant (F-145/F-260) |

## Position candidate
Table des documents : **action/toggle inline « Écritures adverses »** par document. Aucun nouvel écran, aucun bloc primaire (la section Documents existe déjà ; elle porte déjà des actions par ligne).

## Challenge placement
✅ Cohérent — l'avocat marque le document là où il gère ses documents (Dossier), au moment de l'upload des écritures adverses.

## Challenge lisibilité séquence
⚠️ Ajustement léger : indiquer **à quoi sert** le marquage (il alimente la réfutation des moyens dans les conclusions). Mention discrète / tooltip (« Les moyens de ce document seront réfutés dans les conclulsions »). Pas de promesse forte tant que SF-261-02/03 ne sont pas livrées → libellé sobre.

## Challenge charge écran
✅ Aucune surcharge — une action de plus dans une ligne existante (à côté de « N° » et du réordonnancement). La section Documents reste un bloc primaire unique. Attention design : ne pas confondre avec le marquage des **citations** adverses (SF-98-56, qui vit sur l'écran Synthèse) — libellés distincts (« écritures adverses » = document ; « adverse à réfuter » = citation).

## Challenge état final / continuité
✅ Le marquage est une **pré-étape** : il prépare SF-261-02/03. Tant que celles-ci ne sont pas là, le marquage persiste sans effet visible dans l'acte — **assumé** (livraison séquencée 01→02→03), libellé sobre pour ne pas surpromettre.

## Ajustements IA requis (mini-spec)
1. Toggle « Écritures adverses » inline dans la table des documents.
2. Mention sobre de la finalité (réfutation des moyens, à venir).
3. Distinction visuelle/libellé vs le marquage de citation adverse (SF-98-56, autre écran).

## Invariants anti-surcharge
1. Pas de nouvel écran ni bloc primaire : action dans la table Documents existante.
2. Marquage = action explicite avocat ; jamais déduit.
3. Libellé distinct du marquage citation adverse (SF-98-56).

## Décision finale
**GO avec ajustements.** Insertion naturelle dans la table des documents (action inline), aucune surcharge ; rendre sobrement lisible la finalité ; distinguer du marquage de citation SF-98-56. Mini-spec peut démarrer.

## MAJ parcours de référence
`docs/business/parcours-ecran-dossier.md` : la table des documents (onglet Dossier) porte désormais, outre N° + réordonnancement (F-260), un **marqueur « écritures adverses »** désignant le document dont les moyens seront réfutés dans les conclusions (F-261).
