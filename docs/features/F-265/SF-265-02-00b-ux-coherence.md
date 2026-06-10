# F-265 / SF-265-02 — Cadrage cohérence écran (étape 0 bis)

> Feature à impact écran : ajouter dans le **mode édition** des conclusions une zone de **co-rédaction IA par section** (sélection de section + instruction + bouton régénérer).
> Skill : `ai-skills/screen-coherence-challenger.md`. 2026-06-10.

## Verdict : **GO avec ajustements** — zone intégrée DANS le mode édition (F-264), pas un nouvel écran

## Parcours écran réel de l'avocat
1. Dossier ouvert → onglet/section « Projet de conclusions » (`conclusions-section`).
2. Conclusions `DONE` + `DRAFT` → l'avocat clique **« Modifier »** → mode édition F-264 (barre d'outils markdown + textarea + aperçu live).
3. **(nouveau)** Au-dessus de l'éditeur, une zone **« Co-rédaction IA »** : un menu déroulant des **sections détectées** de l'acte (titres `##`/`###`), un champ **instruction** (« renforce la prescription »), un bouton **« Régénérer cette section »**.
4. Au clic : appel `POST …/sections/regenerate` (SF-265-01) → le markdown régénéré **remplace en place** la section dans le `draftContent` (textarea + aperçu se mettent à jour).
5. L'avocat **relit/ajuste** (l'éditeur reste éditable), puis **« Enregistrer »** (PATCH content existant). Retour en lecture.

## Cartographie écrans/zones existants
| Zone | Existant | Impact SF-265-02 |
|---|---|---|
| Mode lecture (acte rendu) | F-259 | aucun |
| Mode édition (toolbar + textarea + aperçu) | F-264 | **+ zone Co-rédaction IA en tête de la colonne éditeur** |
| Bascule éditeur/aperçu (étroit) | F-264 | inchangée |
| Actions (Enregistrer/Annuler) | SF-98-49 | inchangées |

## Challenge placement / lisibilité / charge / état final
- **Placement** : la zone co-rédaction se place **en haut de la colonne éditeur**, avant la barre d'outils markdown — séquence logique (« je choisis quoi régénérer » → « je vois/édite le résultat »). Repliable par défaut pour ne pas alourdir.
- **Lisibilité séquence** : la régénération **n'auto-sauvegarde pas** ; elle remplit le brouillon → l'avocat garde la main (cohérent avec le pattern F-264 « relire avant enregistrer »).
- **Charge écran** : zone compacte (1 select + 1 input + 1 bouton). Sur écran étroit elle reste dans la colonne éditeur (la bascule éditeur/aperçu existante la masque avec l'éditeur en vue aperçu). Pas de surcharge du mode lecture (zone invisible hors édition).
- **État final / continuité** : après régénération réussie → snackbar discrète « Section régénérée — relisez puis enregistrez » ; le `draftContent` est mis à jour, l'avocat poursuit son édition. En cas d'échec (409/502) → snackbar erreur, brouillon inchangé.

## Invariants anti-surcharge pour la mini-spec
1. **Zone confinée au mode édition** : rien de nouveau en mode lecture.
2. **Repliable / compacte** : 1 select + 1 instruction + 1 bouton ; pas d'écran ni de modale lourde.
3. **Pas d'auto-save** : la régénération remplit le brouillon ; l'avocat relit et enregistre via le bouton existant (continuité F-264).
4. **Round-trip markdown** : remplacement in-place de la section dans `draftContent` (string), aucun stockage modifié.
5. **Détection de section déterministe** : parsing des titres markdown (`##`/`###`) côté front, pas d'appel réseau pour lister les sections.
6. **Désactivation cohérente** : zone désactivée pendant un enregistrement ou une régénération en cours (réutilise `savingContent` + nouveau `regenerating`).

## Enrichissement référentiel
`docs/business/parcours-ecran-dossier.md` : ajouter à l'étape « édition des conclusions » la sous-étape **co-rédaction IA par section** (sélection → instruction → régénération in-place → relecture → enregistrement). (Groupé par l'orchestrateur en fin — pas de commit doc CI-couplé ici.)

**GO avec ajustements.**
