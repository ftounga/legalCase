# F-266 — Cadrage cohérence écran (étape 0 bis)

> Feature : Traçabilité fait→pièce au survol + export à en-tête cabinet. Skill : `ai-skills/screen-coherence-challenger.md`. 2026-06-10.
> Étape 0 : **GO avec ajustements** (`SF-266-00-coherence.md`) — périmètre réduit à fait→pièce + export en-tête (montant→calcul abandonné F-263, article→texte différé).

## Verdict : **GO avec ajustements** — la traçabilité s'ajoute **sans nouvel élément de chrome** (décoration du rendu existant) ; l'export en-tête s'ajoute **dans le flux d'export existant** (champ avant téléchargement).

---

## Parcours écran réel de l'avocat (état actuel)

Onglet **Décision** du dossier → section **Conclusions** (`conclusions-section`) :
1. Bandeau de transparence + statut de génération.
2. Sélecteur de version + cycle de vie (DRAFT/VALIDATED/DEPOSITED) + bandeaux F-258 / SF-98-53 (péremption).
3. **Aperçu « acte »** (`ConclusionDocumentComponent`, F-259/F-264) — feuille blanche, markdown rendu, renvois « Pièce n° X » en texte brut.
4. Actions : Copier, **Exporter Word**, **Exporter PDF**, Modifier (→ éditeur F-264 + co-rédaction F-265).

## Cartographie écrans / zones existants

| Zone | Composant | F-266 y touche ? |
|---|---|---|
| Aperçu « acte » (lecture) | `ConclusionDocumentComponent` | **SF-266-01** — décore les `Pièce n° X` (tooltip), aucun nouvel élément de layout |
| Barre d'actions export | `conclusions-section` (boutons Word/PDF) | **SF-266-02** — un champ « En-tête du cabinet » + le flux d'export existant |
| Éditeur / co-rédaction | textarea F-264 + zone F-265 | **non touché** (le survol est une aide de **lecture/confiance**, pas d'édition) |

## Challenge — placement, lisibilité, charge, état final

### SF-266-01 (fait→pièce au survol)
- **Placement** : aucune nouvelle zone. Le `Pièce n° X` reconnu devient survolable **in-place** dans l'aperçu (léger soulignement pointillé + curseur d'aide + `title`/tooltip natif). → **zéro surcharge** : la densité de l'écran ne change pas.
- **Lisibilité de séquence** : la traçabilité arrive **là où l'avocat lit déjà** (l'aperçu), au moment où il vérifie → cohérent avec le besoin « vérifier sans quitter l'acte ».
- **Charge** : un `title` natif (ou tooltip léger) — pas de panneau, pas de modale, pas de second chargement réseau (pièces déjà en mémoire).
- **État final / continuité** : non destructif, pas d'état persistant ; si pièce introuvable → pas de décoration (silencieux). Continuité parfaite avec l'aperçu existant.

### SF-266-02 (export en-tête cabinet)
- **Placement** : un champ **« En-tête du cabinet (optionnel) »** (textarea compacte repliable) **à côté des boutons d'export**, ou une petite zone au-dessus d'eux. Pré-rempli vide ; mémorisé le temps de la session (signal), **non persisté serveur**.
- **Lisibilité de séquence** : l'avocat saisit son en-tête → clique Exporter → le fichier porte l'en-tête. Séquence linéaire, pas de rupture.
- **Charge** : un seul champ optionnel ; replié par défaut pour ne pas alourdir la barre d'actions (révélé via « Ajouter un en-tête de cabinet »). → surcharge minimale, opt-in.
- **État final / continuité** : le `content` markdown **n'est pas modifié** ; l'aperçu et les versions restent identiques. Seul le **fichier exporté** change. Réversible (champ vidé = export neutre comme avant).

## Invariants anti-surcharge pour la mini-spec

1. **Pas de nouveau chrome lourd** : fait→pièce = décoration in-place (tooltip natif), pas de panneau/modale ; export en-tête = un champ optionnel **replié par défaut**.
2. **Aucune régression de l'aperçu** : le rendu markdown→HTML reste identique hors décoration des `Pièce n° X`.
3. **Le `content` n'est jamais modifié** par F-266 (ni le survol ni l'en-tête d'export).
4. **Opt-in** : l'en-tête d'export est facultatif ; sans saisie, l'export reste neutre (comportement actuel inchangé).
5. **Silencieux si pas d'ancrage** : `Pièce n° X` sans pièce connue → aucune décoration (pas de tooltip vide, pas d'erreur).

## Enrichissement référentiel

`docs/business/parcours-ecran-dossier.md` — onglet Décision / section Conclusions : ajout de deux aides à l'**aperçu acte** : (a) **survol d'un renvoi « Pièce n° X »** révèle le libellé/type de la pièce (confiance, vérification sans quitter l'acte) ; (b) **export Word/PDF** peut désormais porter un **en-tête de cabinet** saisi à l'export (non persisté). Le `content` markdown reste la source unique, inchangée. (Mise à jour groupée par l'orchestrateur — pas de commit docs dans cette run.)

## Décision finale

**GO avec ajustements.** Les deux ajouts s'insèrent dans des zones existantes sans surcharge (décoration in-place + champ opt-in replié). Étape 0 bis validée pour SF-266-01 et SF-266-02.
