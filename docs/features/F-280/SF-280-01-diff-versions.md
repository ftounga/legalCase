# Mini-spec — F-280 / SF-280-01 Comparaison de versions (diff lisible)

## Identifiant

`F-280 / SF-280-01`

## Feature parente

`F-280` — Conclusions V4 ⑩ (UX) — Comparaison de versions (diff lisible)

## Statut

`ready`

## Date de création

2026-06-12

## Branche Git

`feat/SF-280-01-diff-versions`

---

## Objectif

Permettre à l'avocat de voir, dans la carte « Projet de conclusions », un **diff lisible (ajouts / suppressions surlignés) entre deux versions** des conclusions, pour auditer ce qu'une régénération ou une édition a modifié.

---

## Comportement attendu

### Cas nominal

1. Carte conclusions en état `DONE`, lecture seule, ≥ 2 versions existantes → un bouton **« Comparer les versions »** apparaît dans la barre d'actions.
2. Au clic, la carte bascule en **mode comparaison** : le rendu acte est remplacé par le panneau diff. Le sélecteur de version du header est désactivé.
3. Le panneau propose deux bornes (selects) : **base** (par défaut la version *précédente* de la version sélectionnée) et **cible** (par défaut la version sélectionnée). Si la version sélectionnée est la V1 (pas de précédente), base = V1, cible = V2 par défaut.
4. Le content des deux bornes est chargé via `ConclusionsService.getVersion` (déjà existant). Pendant le chargement : spinner.
5. Un **diff ligne-à-ligne** est calculé (algorithme LCS pur-TS) et rendu :
   - lignes **inchangées** : neutres,
   - lignes **ajoutées** (présentes dans la cible, absentes de la base) : fond vert, préfixe `+`,
   - lignes **supprimées** (présentes dans la base, absentes de la cible) : fond rouge, préfixe `−`.
6. Une **légende** (vert = ajout, rouge = suppression) et un **résumé compact** (`N ajout(s), M suppression(s)`) sont affichés.
7. Changer une borne recalcule le diff. Un bouton **« Fermer »** quitte le mode comparaison et restaure exactement le rendu acte de la version sélectionnée.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|----------------------|
| `getVersion` échoue (réseau/serveur) | Message « Impossible de charger cette version. » dans le panneau ; pas de crash ; bouton Fermer reste actif |
| Les deux bornes identiques | Diff = 100 % inchangé ; résumé « 0 ajout, 0 suppression » ; pas d'erreur |
| Content `null` (version PENDING/FAILED sélectionnable ?) | Traité comme chaîne vide → tout l'autre côté en ajout/suppression ; pas de crash |
| < 2 versions | Bouton « Comparer » non rendu (impossible d'entrer en mode diff) |

---

## Analyse de cohérence transversale

| Préoccupation | Impactée ? | Détail |
|---------------|-----------|--------|
| Auth / Principal | Non | Aucun changement d'auth ; réutilise les endpoints existants (mêmes gardes workspace côté backend) |
| Workspace context | Non | `getVersion` est déjà scoping workspace côté backend |
| Plans / limites | Non | Aucun nouveau gate |
| Navigation / routing | Non | Aucune route ; mode interne à la carte |
| Outil décisionnel métier | Non | Le diff n'est pas un outil décisionnel (pas de calcul de situation métier), c'est un visualiseur |

**Aucune préoccupation transversale cochée** → pas de liste de composants transversaux requise, pas de smoke E2E obligatoire. (Tests unitaires + composant.)

---

## Critères d'acceptation vérifiables

1. Avec 1 seule version : aucun bouton « Comparer les versions ».
2. Avec ≥ 2 versions : le bouton apparaît ; au clic, le panneau diff s'ouvre et le rendu acte disparaît.
3. Par défaut, base = version précédente, cible = version sélectionnée.
4. Une ligne ajoutée est surlignée en vert (`data-testid="diff-line-added"`), une ligne supprimée en rouge (`data-testid="diff-line-removed"`), une inchangée neutre.
5. Le résumé affiche le bon nombre d'ajouts/suppressions.
6. « Fermer » restaure le rendu acte sans changer la version sélectionnée.
7. L'algorithme LCS est pur et couvert par des tests unitaires (cas : identiques, ajout pur, suppression pur, modification = supp+ajout, vide↔plein).
8. Aucun appel d'écriture serveur déclenché par le mode diff.

---

## Plan de test minimal

**Unitaires (util `conclusion-diff.util.ts`)** :
- `diffLines('a\nb', 'a\nb')` → tout `unchanged`.
- `diffLines('a\nb', 'a\nb\nc')` → `c` `added`.
- `diffLines('a\nb\nc', 'a\nc')` → `b` `removed`.
- `diffLines('a\nb', 'a\nB')` → `b` removed + `B` added (modif ligne).
- `diffLines('', 'x')` / `diffLines('x', '')` → un seul côté.
- `summarize(diff)` → bons compteurs.

**Composant (`conclusions-section.component.spec.ts`)** :
- bouton « Comparer » masqué si 1 version, visible si ≥ 2.
- ouverture → `getVersion` appelé pour les 2 bornes ; rendu acte masqué.
- changement de borne → recalcul.
- fermeture → retour lecture, version sélectionnée inchangée.
- erreur `getVersion` → message, pas de crash.

**Isolation workspace** : N/A (réutilise endpoints déjà scoping ; aucun nouvel accès données).

---

## Tables / endpoints / composants impactés

- **Tables** : aucune.
- **Endpoints** : aucun nouveau. Réutilise `GET /api/v1/case-files/{id}/conclusions/versions/{versionId}` (existant).
- **Composants** :
  - nouveau util `frontend/src/app/case-files/conclusions-section/conclusion-diff.util.ts` (+ spec) — algorithme LCS line-diff pur.
  - `conclusions-section.component.ts/.html/.scss` — mode comparaison (signals, bouton, panneau, légende).

---

## Hors périmètre

- Diff mot-à-mot / intra-ligne (V1 = line-level).
- Diff côte-à-côte synchronisé scroll (V1 = unifié inline).
- Merge / restauration depuis le diff (lecture seule).
- Export du diff (PDF/Word).
- Diff sur autre chose que les conclusions.
