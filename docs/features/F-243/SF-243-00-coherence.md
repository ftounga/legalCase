# F-243 — Stade procédural du dossier (Document de cadrage cohérence — étape 0)

**Date** : 2026-05-15
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Origine** : F-243 a été identifiée comme **trou fonctionnel amont** lors du cadrage de cohérence de F-98 (`docs/features/F-98/SF-98-00-coherence.md`). Le présent document est donc un cadrage léger : il confirme le placement de F-243 dans le workflow métier et fige le découpage SF + le périmètre des valeurs.

---

## Verdict global

**GO**

F-243 est une feature de **saisie pure** : l'avocat renseigne sur son dossier la juridiction, le stade procédural et la position. Aucun pré-requis fonctionnel amont manquant. Le seul « aval » est F-98, dont F-243 est explicitement le pré-requis — l'ordre est correct.

---

## Intention métier (1 phrase)

Permettre à l'avocat de renseigner sur chaque dossier le **stade procédural** (juridiction + stade + position juridique), afin que le dossier porte cette information structurante — consommée d'abord par l'affichage, puis par F-98 (génération de conclusions).

---

## Workflow métier réel — position de F-243

Workflow repris de `SF-98-00-coherence.md` (source : pratique avocat contentieux + signal terrain Renversez). F-243 correspond à l'**étape 8** :

```
... → 7. Définition des pistes stratégiques
    → 8. CHOIX juridiction + stade procédural + position   ⬅ F-243
    → 9. Rédaction des conclusions                          ⬅ F-98
    → ...
```

Dans la vraie vie du cabinet, l'avocat décide « je saisis le CPH en bureau de jugement, côté salarié » **avant** de rédiger quoi que ce soit. F-243 matérialise cette décision dans le produit.

---

## Cartographie features ↔ workflow (challenge amont/aval)

| Élément | Couvert ? | Analyse |
|---|---|---|
| Le dossier existe (entité `CaseFile`) | ✅ | F-43 livrée — F-243 ajoute des champs à `case_files` |
| Le dossier porte un domaine juridique + le workspace un pays | ✅ | `CaseFile.legalDomain` + `Workspace.country` livrés — servent à filtrer les valeurs proposées |
| Endpoint de modification d'un champ du dossier | ✅ | `PATCH /api/v1/case-files/{id}` existe (pattern à répliquer) |
| Endpoint de référentiel pour servir des valeurs au frontend | ✅ | `GET /api/v1/referentials?domain=X` existe (`ReferentialController`) |
| Écran dossier pour afficher/saisir | ✅ | `case-file-detail.component` livré — ajout d'une `mat-card` section |
| **Consommateur aval** (génération conclusions) | 🟡 | F-98 — pas encore développée, mais F-243 est son pré-requis assumé. Ordre correct, pas un trou |

**Challenge amont** : aucun trou. Toutes les briques nécessaires à une feature de saisie existent.
**Challenge aval** : F-98 est le consommateur ; F-243 doit être livrée avant. Conforme à l'ordre prévu.

---

## Périmètre des valeurs (juridiction / stade / position)

F-243 doit couvrir un **référentiel exhaustif** des 6 combinaisons domaine × pays — pas seulement le besoin de la V1 de F-98. Raison : c'est de la donnée référentielle (coût marginal faible), et un référentiel partiel obligerait à re-toucher F-243 à chaque nouvelle SF de F-98.

Les valeurs proposées à l'avocat **dépendent du domaine du dossier et du pays du workspace** (un dossier travail FR ne propose pas « CNDA »). Le référentiel détaillé (liste exacte des juridictions/stades/positions par combinaison) est figé dans la mini-spec SF-243-01, à partir de la matrice de `SF-98-00-coherence.md` :

- **Travail FR/BE** : CPH / Tribunal du travail, Cour d'appel / Cour du travail, Cassation × fond / référé / départage / appel / pourvoi × demandeur / défendeur / appelant / intimé
- **Immigration FR/BE** : TA / CCE, CAA, CE, CNDA, Préfecture/Office des étrangers × recours / référé / appel / cassation / demande de titre × requérant / demandeur de titre
- **Famille FR/BE** : JAF / Tribunal de la famille, CA, Cassation, TJ × divorce fond / mesures provisoires / référé / ordonnance de protection / appel / pourvoi / filiation / succession × demandeur / défendeur / requérant / appelant / intimé

---

## Découpage en SF

| SF | Périmètre | Parallélisable |
|---|---|---|
| **SF-243-01 backend** | Migration `case_files` (3 champs : juridiction, stade, position) + entité + DTO + endpoint `PATCH /api/v1/case-files/{id}/procedure-stage` + endpoint référentiel des valeurs par domaine/pays + tests UT/IT | — |
| **SF-243-02 frontend** | Composant section `procedure-stage` dans `case-file-detail` : affichage + édition (3 sélecteurs dépendants domaine/pays) + appel API + tests Jest. **Contrat API importé de SF-243-01** | ✅ avec SF-243-01 si contrat figé |

Les 2 SF seront développées **en parallèle** (back/front, contrat API figé dans la mini-spec SF-243-01 — cf. skill `parallel-frontback-delivery`).

---

## Invariants anti-gadget pour les mini-specs

1. **Valeurs contextuelles** : les juridictions/stades/positions proposés dépendent du domaine du dossier + pays du workspace. Ne jamais proposer une valeur hors domaine (pas de « CNDA » sur un dossier travail).
2. **Champ optionnel** : un dossier en phase pré-contentieuse n'a pas encore de stade procédural. La saisie doit être possible mais non obligatoire — pas de blocage du dossier si non renseigné.
3. **Cohérence interne des 3 champs** : la position dépend du stade (« appelant » n'a de sens qu'avec un stade « appel » ; « demandeur au pourvoi » qu'avec « cassation »). Le frontend filtre les positions valides selon le stade choisi.
4. **Modifiable** : le stade procédural évolue (un dossier passe du fond à l'appel). La saisie doit être ré-éditable à tout moment.
5. **Lisible en l'état** : l'encart affiche les libellés humains (« Conseil de prud'hommes — Bureau de jugement — Demandeur »), pas les codes techniques.

---

## Décision finale

**GO.** F-243 démarre. Découpage en 2 SF parallélisables (SF-243-01 backend + SF-243-02 frontend). Périmètre des valeurs = référentiel exhaustif 6 combinaisons domaine × pays. Prochaine étape : mini-specs SF-243-01 et SF-243-02 avec contrat API figé.

---

## Liens
- `docs/features/F-98/SF-98-00-coherence.md` — cadrage parent (matrice des juridictions/stades/positions)
- `ai-skills/feature-coherence-challenger.md` — skill appliquée
- `ai-skills/parallel-frontback-delivery.md` — parallélisation back/front
- `docs/PRODUCT_SPEC.md` — F-243, F-98
