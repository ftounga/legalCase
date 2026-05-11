# Mini-spec — F-237 / SF-237-03 Refactor lisibilité règle CLAUDE.md composant décisionnel

## Identifiant

`F-237 / SF-237-03`

## Feature parente

`F-237`

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-237-03-refactor-regle-claudemd`

---

## Objectif

Refactorer la règle CLAUDE.md ligne 207 (~80 lignes en une seule cellule de tableau) en sections numérotées séparées et scannables en < 10 secondes. Chaque sous-point devient une ligne distincte du tableau "Blocages automatiques".

---

## Comportement attendu

### Cas nominal

La règle actuelle ligne 207 cumule 6 sous-points : (1) template canonique, (2) checklist visuelle, (3) pré-fill IA, (4) F-IA-03, (5) TOOL_REGISTRY, (6) `static getPrefillCount`. C'est un mur de texte.

Cible : **6 lignes distinctes** dans le tableau "Blocages automatiques" de CLAUDE.md :
- Composant décisionnel sans référence au template canonique → REFUS (P1)
- Composant décisionnel sans checklist cohérence visuelle → REFUS (P2)
- Composant décisionnel sans pré-fill IA fonctionnel → REFUS (P3 — bug produit)
- Composant décisionnel sans validation F-IA-03 → REFUS (P4 — bug produit)
- Composant décisionnel sans entrée TOOL_REGISTRY symétrique → REFUS (P5)
- Composant décisionnel sans `static getPrefillCount` + helper partagé → REFUS (P6, lié au test SF-236-05)

Chaque ligne contient : nom court de la règle, raison succincte (1-2 phrases), référence au pattern canonique, motivation historique brève, lien vers garde-fou CI le cas échéant.

### Cas d'erreur

Aucun — refactor docs pur.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [ ] Autres outils métier : non applicable
- [ ] Autres pays : non applicable
- [ ] Autres domaines : non applicable
- [x] Autres règles CLAUDE.md trop denses (audit opportuniste — si une autre ligne du tableau est aussi un mur de texte, la signaler, ne pas la refactorer dans cette SF)
- [ ] Autres flows transversaux : non applicable

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Règle ligne 207 | Oui | Refactor 6 lignes |
| Autres règles longues | Audit opportuniste | Signaler dans le rapport, traiter en autre SF si justifié |

### Décision

- [x] Étendu à toutes les cibles applicables (refactor cible, audit opportuniste)

---

## Conformité F-IA-04

- [x] **Non applicable** — SF documentation pure.

---

## Critères d'acceptation

- [ ] La règle ligne 207 est remplacée par 6 lignes distinctes dans le tableau "Blocages automatiques"
- [ ] Chaque ligne est < 5 lignes de texte (lisible en < 10s)
- [ ] Le sens et la sévérité des règles sont préservés (pas de relâchement)
- [ ] Les références aux garde-fous CI sont conservées
- [ ] Le rapport identifie les autres règles longues éventuelles à refactorer en SF future

---

## Périmètre

### Hors scope (explicite)

- Modification du contenu des règles (juste la forme)
- Refactor d'autres règles longues du tableau (à traiter séparément si nécessaire)

---

## Plan de test

Pas de tests automatisés — relecture humaine + diff Markdown.

---

## Analyse d'impact

- [x] Aucune préoccupation transversale

---

## Dépendances

Aucune — parallélisable avec SF-237-01.

---

## Notes et décisions

### Préserver la traçabilité

Garder dans chaque ligne refactorée une note historique brève (« Motivation : audit 2026-04-24 », « Garde-fou F-236 SF-236-05 ») pour conserver le lien avec les incidents passés.

### Pas de relâchement

L'objectif est lisibilité, pas affaiblissement. Si un agent doit le découper en 6, chaque sous-règle doit être aussi contraignante que la version originale.
