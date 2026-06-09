# Mini-spec — F-98 / SF-98-57 — Bordereau de pièces dans les conclusions

> Base : `project-governance/templates/subfeature-template.md`. Dépend de F-260 (numérotation persistante, livrée PR #1613).

## Identifiant
`F-98 / SF-98-57`

## Feature parente
`F-98` — Génération de conclusions

## Statut
`ready` (étape 0 GO avec ajustements ; F-260 livrée → dépendance levée ; étape 0 bis non applicable, cf. ci-dessous)

## Date
2026-06-09

## Branche
`feat/SF-98-57-bordereau-pieces`

---

## Objectif
> Terminer le projet de conclusions par une annexe **« Bordereau de pièces communiquées »** : la liste numérotée des pièces du dossier, cohérente avec les renvois « Pièce n° X » de l'acte.

## Comportement attendu

### Cas nominal
1. À la génération des conclusions, après que le LLM a produit le corps de l'acte, le backend **assemble de façon déterministe** (hors-LLM) une section finale `## BORDEREAU DE PIÈCES COMMUNIQUÉES` listant les pièces du dossier, dans l'ordre de leur **numéro persistant** (`piece_number`, F-260) : `1. <label> (<type lisible>)`, `2. …`.
2. La section est **ajoutée à la fin** du `content` des conclusions (donc relue/éditée SF-98-49, exportée Word/PDF SF-98-50/51, versionnée SF-98-52 comme le reste de l'acte).
3. Les numéros du bordereau = ceux des renvois « Pièce n° X » du corps (même source `loadNumberedPieces`) → **cohérence garantie par construction**.

### Cas d'erreur / bords
| Situation | Comportement |
|---|---|
| Dossier sans aucune pièce | **Aucune** section bordereau ajoutée (pas de rubrique vide / « néant ») |
| Génération LLM échoue (FAILED) | Pas de bordereau (rien à annexer) ; comportement FAILED inchangé |
| Label de pièce vide | Repli sur un libellé neutre (ex. nom de fichier source) — jamais de ligne vide |

---

## Analyse de cohérence transversale
- **Source de numérotation** : `loadNumberedPieces` (F-260, `piece_number` persistant) — **déjà** utilisée pour les renvois dans le prompt → le bordereau réutilise exactement la même liste. Aucune divergence.
- **Pays/domaines** : agnostique (assemblage pur sur la liste de pièces) → vaut FR + BE, 3 domaines, sans code spécifique.
- **Exports** : le bordereau étant dans `content` (markdown), les exports Word/PDF existants (SF-98-50/51) le rendent **sans modification** (à vérifier au dev : le rendu markdown des titres `##` et listes est déjà géré).

### Résultat du scan
| Cible | Applicable | Traitement |
|---|---|---|
| `loadNumberedPieces` (F-260) | Oui | Réutilisée telle quelle (source unique) |
| Exports Word/PDF (SF-98-50/51) | Oui | Le bordereau passe par le `content` existant — vérifier rendu listes/titres |
| Fiche prud'homale (a déjà un bordereau) | Non | Hors scope (bordereau distinct, déjà existant pour la fiche) |

### Décision
- [x] Étendu à toutes les cellules de conclusions (assemblage commun, agnostique).
- [x] Non applicable à la fiche prud'homale (elle a déjà son propre bordereau).

## Conformité F-IA-04
- [x] **Non applicable** — pas de composant décisionnel ; enrichissement du contenu généré côté backend.

## Champs IA à extraire
- [x] **Aucun pré-remplissage** — le bordereau est assemblé depuis les pièces existantes, pas extrait par l'IA.

## Étape 0 bis (cohérence écran)
- [x] **Non applicable** — SF-98-57 n'ajoute **aucun élément d'UI nouveau** ni ne déplace d'élément : le bordereau est du **contenu généré** au sein de l'acte, affiché par la `conclusions-section` existante (onglet Décision) et exporté par l'existant. Pas de placement/navigation/densité à challenger. (Correction de la note de `SF-98-57-00-coherence.md` qui anticipait une 0 bis : l'assemblage déterministe backend-only retire l'impact écran.)

---

## Critères d'acceptation
- [ ] L'acte généré se **termine** par une section « BORDEREAU DE PIÈCES COMMUNIQUÉES » listant les pièces numérotées (numéro + label + type lisible).
- [ ] Les numéros du bordereau correspondent **exactement** aux renvois « Pièce n° X » du corps (même `piece_number`).
- [ ] Le bordereau liste **uniquement** les pièces réelles du dossier (aucune pièce inventée) — garanti par assemblage déterministe hors-LLM.
- [ ] Dossier sans pièce → **aucune** section bordereau.
- [ ] Le bordereau apparaît dans le `content` (donc éditable SF-98-49, exporté Word/PDF, versionné).
- [ ] Anti-jargon (non-régression SF-98-55) : libellés métier, pas de nom de fichier brut ni de type technique seul dans le bordereau (type traduit lisible).

## Périmètre
### Hors scope
- Bordereau comme **document séparé** (export dédié) — le bordereau est intégré à l'acte (cf. étape 0).
- Personnalisation de l'ordre du bordereau indépendamment de la numérotation (l'ordre = `piece_number`, piloté par F-260).

## Technique
### Endpoints
Aucun nouvel endpoint. L'assemblage se fait dans le flux `POST .../conclusions/generate` existant.

### Backend — fichiers
- `CaseConclusionService` : après réception du contenu LLM (finalize), **appendre** la section bordereau construite depuis la liste `NumberedPiece` (déjà chargée pour le prompt). Helper `buildBordereau(List<NumberedPiece>)` (déterministe ; vide → chaîne vide). Réutiliser `humanizeToolId`-style pour le type lisible (ou un mapping `DocumentPieceType` → libellé déjà existant côté backend si disponible).
- Aucune migration.

### Tables impactées
Aucune (lecture des pièces déjà en place).

## Plan de test
### Unitaires (backend)
- [ ] `buildBordereau` : liste non vide → section formatée, numéros = `piece_number`, ordre croissant ; liste vide → chaîne vide.
- [ ] `CaseConclusionService` : le `content` finalisé se termine par le bordereau quand pièces présentes ; inchangé si 0 pièce.
- [ ] Cohérence : les numéros du bordereau == ceux injectés dans le prompt (même source).
- [ ] Anti-jargon : le bordereau ne contient pas de type brut d'enum ni de nom de fichier quand un label existe.
### Intégration
- [ ] Génération bout-en-bout (mock LLM) → `content` contient la section bordereau en fin d'acte.
### Isolation workspace
- [x] Non applicable directement (réutilise la lecture des pièces déjà bornée au dossier/workspace dans `loadNumberedPieces`).

## Analyse d'impact
- [x] **Aucune préoccupation transversale** (pas d'auth/workspace/plan/navigation ; pas de nouvel endpoint ; pas de frontend).
### Smoke E2E
- [ ] Aucun smoke concerné (backend pur) — validé par UT/IT + validation staging.

## Dépendances
- **F-260** (numérotation persistante) — `done` (PR #1613). Indispensable pour la stabilité des numéros du bordereau.
- SF-98-55 (anti-jargon) — `done` (le bordereau respecte la garde).

## Notes et décisions
- **Assemblage déterministe hors-LLM** (vs instruction au LLM) : garantit zéro pièce hallucinée et une correspondance exacte avec les renvois — choix le plus sûr (invariant anti-gadget 2 de l'étape 0).
- **Backend-only** : le bordereau est du contenu de l'acte ; aucun changement frontend (rendu/édition/export existants).
