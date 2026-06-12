# SF-279-00 — Cadrage de cohérence fonctionnelle (F-279)

**Feature** : F-279 — Conclusions V4 ⑨ (UX) — Feedback de sauvegarde explicite + autosave brouillon
**Skill** : `ai-skills/feature-coherence-challenger.md`
**Date** : 2026-06-12

## 1. Workflow métier réel de l'avocat (cible)

1. L'avocat ouvre un dossier, lance l'analyse, renseigne le stade procédural.
2. Il **génère** un projet de conclusions (F-98) → une version `DRAFT`.
3. Il **relit** l'acte dans l'aperçu « document natif » (F-264).
4. Il **édite** le texte : barre d'outils markdown, co-rédaction par section (F-265),
   complète les emplacements `[ … ]` (SF-266-03).
5. Il **enregistre** (`PATCH .../content`) → la version est mise à jour, le mode édition se ferme.
6. Il fait évoluer le cycle de vie (`DRAFT → VALIDATED → DEPOSITED`), exporte (Word/PDF).

**Friction observée à l'étape 5 (audit conclusions 2026-06-12)** :
- « Enregistrer » ferme l'éditeur en silence (snackbar absente aujourd'hui : le succès
  ferme simplement le mode édition, cf. `saveContent()` → `editing.set(false)`).
  L'avocat doute : « est-ce vraiment sauvegardé ? ».
- **Aucune protection contre la perte** : si le navigateur plante / la session expire / un
  onglet est fermé pendant l'édition d'un acte de plusieurs pages, **tout le travail saisi
  est perdu** (le brouillon ne vit que dans le signal `draftContent`).

## 2. Cartographie des features existantes sur ce workflow

| Étape | Feature | État |
|-------|---------|------|
| Génération | F-98 / SF-98-01 | livré |
| Aperçu natif | F-264 / SF-264-01 | livré |
| Édition markdown | SF-98-49 + barre outils SF-264-01 | livré |
| Co-rédaction section | F-265 / SF-265-02 | livré |
| Garde placeholders avant export | SF-266-03 | livré |
| Enregistrement serveur | `PATCH .../content` (SF-98-49) | livré |
| **Feedback de save explicite** | — | **manquant (F-279)** |
| **Protection anti-perte du brouillon** | — | **manquant (F-279)** |
| Garde anti-écrasement à la régénération | F-278 | À faire (autre SF) |
| Reprise récapitulative | F-271 | À faire (autre SF) |

## 3. Challenge de cohérence

### Amont (pré-requis présents ?)
- ✅ Un mode édition existe (`editing`, `draftContent`, `savingContent`).
- ✅ Un endpoint de persistance existe (`updateContent`).
- ✅ La version éditable est identifiée de façon déterministe (`editable() = DONE + DRAFT`).
- ✅ Aucun pré-requis manquant : F-279 se greffe sur du code en place.

### Aval (la sortie est-elle exploitable ?)
- L'indicateur « ✓ Enregistré / Modifié » **lève le doute** immédiatement (sortie consommée par l'avocat).
- L'autosave **brouillon** restitue le travail après crash → l'avocat reprend là où il en était.
- **Anti-conflit avec les versions** : le seul écrit serveur reste l'« Enregistrer » explicite.
  L'autosave n'écrit **jamais** côté serveur → il ne crée pas de version, ne déclenche pas
  le polling, n'entre pas en conflit avec F-271 (reprise) ni F-278 (régénération). Round-trip
  markdown intact (F-264).

## 4. Anti-doublon / anti-gadget

- **Pas de doublon** : aucun feedback de save ni autosave n'existe aujourd'hui.
- **Pas de chevauchement avec F-278/F-271** : F-278 = confirmation *avant régénération* ;
  F-271 = reprise *récapitulative à la génération*. F-279 = **sauvegarde du contenu édité**
  (état dirty + restauration locale). Ces trois features sont disjointes ; la directive PO
  (coupler F-271/F-278) ne concerne pas F-279.
- **Décision d'architecture (réversible 🟠) — autosave LOCAL, pas serveur** :
  un autosave serveur (PATCH throttlé) mute le `content` persisté de la version en silence →
  casse le modèle mental « l'enregistrement est explicite » (F-278), risque de conflit versions,
  charge backend, et n'apporte rien de plus que le local pour le risque réel (crash/déconnexion).
  → **localStorage par (caseFileId, versionId)**, debounce, restauration proposée à la
  ré-entrée en édition. Zéro endpoint, zéro migration, réversible. Tracé comme arbitrage.

## 5. Invariants anti-gadget pour la mini-spec

1. **Un seul écrit serveur** : seul « Enregistrer » appelle `updateContent`. L'autosave est local.
2. **Pas d'écrasement silencieux** : la restauration d'un brouillon local est **proposée**
   (l'avocat choisit Restaurer / Ignorer), jamais imposée.
3. **Brouillon local = transitoire** : purgé après un enregistrement serveur réussi, après
   « Annuler », et après sortie du cycle `DRAFT` (validation/dépôt).
4. **Dirty state honnête** : « ✓ Enregistré » ne s'affiche que si le brouillon == contenu serveur ;
   « Modifié » dès qu'il diverge.
5. **Round-trip markdown** : aucune transformation du `content` ; on stocke le markdown brut.
6. **Dégradation propre** : `localStorage` indisponible (mode privé/quota) → autosave désactivé
   silencieusement, le reste de l'éditeur fonctionne.
7. **Uniforme aux 3 domaines** (travail/immigration/famille) : aucune logique métier domaine.

## 6. Verdict

**GO** — F-279 comble une friction UX réelle (doute + perte de travail) sans doublon,
sans backend, sans toucher au modèle de versions. Impact écran limité (indicateur + bandeau
de restauration dans la zone d'édition existante) → étape 0 bis requise.

→ Statut PRODUCT_SPEC : `À faire` → dev.
