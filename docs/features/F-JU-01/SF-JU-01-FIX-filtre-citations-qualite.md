# Mini-spec — F-JU-01 / SF-JU-01-FIX Filtre qualité des citations jurisprudence

## Identifiant
`F-JU-01 / SF-JU-01-FIX-filtre-citations-qualite`

## Feature parente
`F-JU-01` — citations jurisprudentielles dans les outils décisionnels (FR).

## Statut
`ready`

## Date de création
2026-06-03

## Branche Git
`fix/jurisprudence-citations-quality-filter`

## Type
**Bugfix** (qualité d'affichage de données) — exempté étapes 0 / 0 bis (aucun élément d'écran nouveau).

---

## Contexte / diagnostic
Révélé 2026-06-03 en testant l'outil « Comparateur d'indemnités » (F-DT-09) sur staging. L'endpoint `GET /api/v1/tools/F-DT-09-comparateur-indemnites/jurisprudence-citations?branch=default` renvoie 2 citations de mauvaise qualité :
- `cc soc 2023-10-11, n° 21-25.991` → **`chapeauOfficiel` vide** (`""`), confiance 0,55 → affiché « » (vide) à l'avocat.
- `cc soc 2021-01-20, n° 19-16.283` → hors-sujet (prime d'ancienneté restauration ferroviaire), confiance 0,72 → **non traité ici** (relève de la ré-évaluation outillée, partie B / feature à part).

Cause : ni le service de lecture (`ToolJurisprudenceService.findByToolAndBranch`) ni la requête repository n'excluent les chapeaux vides ou les confiances faibles. Le même service alimente **l'affichage outil (F-JU-01)** ET **les conclusions générées (F-JU-02)** via `ConclusionsJurisprudenceContext` → un seul point de filtrage couvre les deux.

Aligné sur l'invariant projet « silence > erreur » : pas de citation plutôt qu'une citation vide / peu fiable.

---

## Objectif (une phrase)
Ne plus servir ni afficher de citation jurisprudentielle dont le chapeau est vide ou la confiance inférieure au seuil d'affichage, sur tous les chemins de lecture (outils + conclusions).

## Comportement nominal
`findByToolAndBranch(toolId, branche)` ne retourne que les mappings :
- non archivés (`archived = false`, déjà le cas),
- au `chapeauOfficiel` **non null et non vide après trim**,
- à `confidenceScore >= 0.60` (seuil d'affichage, validé PO 2026-06-03),
- triés `confidence DESC, dateArret DESC`, limités au **top 3 parmi les candidats valides**.

Conséquence immédiate : la citation n°2 (vide) disparaît ; le comparateur n'affichera plus que des citations à chapeau plein ≥ 0,60.

## Cas d'erreur / bords
- Aucun mapping valide après filtrage → liste vide → le bloc « Jurisprudence applicable » ne s'affiche pas (`@if (hasCitations)` déjà géré), idem section conclusions.
- Chapeau composé uniquement d'espaces → exclu (trim).
- `confidenceScore` null → exclu (traité comme < seuil).

---

## Solution technique
### Backend
1. **Repository** `ToolJurisprudenceMappingRepository` : nouvelle méthode `@Query` JPQL retournant le top-3 des mappings valides :
   ```
   SELECT m FROM ToolJurisprudenceMapping m
   WHERE m.toolId = :toolId AND m.brancheCalculId = :branche AND m.archived = false
     AND m.chapeauOfficiel IS NOT NULL AND TRIM(m.chapeauOfficiel) <> ''
     AND m.confidenceScore >= :minConfidence
   ORDER BY m.confidenceScore DESC, m.dateArret DESC
   ```
   avec `Pageable` = `PageRequest.of(0, 3)`. Le filtre s'applique **avant** la limite top-3 (ne gâche pas de slot avec une mauvaise citation).
2. **Service** `ToolJurisprudenceService.findByToolAndBranch` : constante `MIN_DISPLAY_CONFIDENCE = new BigDecimal("0.60")`, appelle la nouvelle méthode. L'ancienne méthode `findTop3…` est remplacée (ou conservée si utilisée ailleurs — à vérifier au dev).

### Frontend (défense en profondeur)
3. `tool-jurisprudence-citations.component.html` : masquer la ligne chapeau si vide — `@if (c.chapeauOfficiel) { <p …>« {{ c.chapeauOfficiel }} »</p> }`. Inutile en théorie (backend garantit non vide) mais évite tout « » résiduel.

---

## Critères d'acceptation (vérifiables)
1. `findByToolAndBranch` exclut tout mapping à chapeau vide/blank.
2. `findByToolAndBranch` exclut tout mapping à `confidenceScore < 0.60`.
3. Sur F-DT-09/`default`, après déploiement, l'endpoint ne renvoie plus la citation `21-25.991` (vide). (La citation `19-16.283` reste tant que la ré-évaluation B n'est pas faite — documenté.)
4. Le chemin conclusions (F-JU-02) bénéficie du même filtre (mêmes mappings exclus).
5. Build backend vert ; tests ci-dessous verts.

## Plan de test minimal
- **Unitaire/IT** (`ToolJurisprudenceServiceTest` / `ToolJurisprudenceControllerIT`) : insérer 4 mappings pour un tool/branche fictif — (a) chapeau plein conf 0,90 ; (b) chapeau vide conf 0,80 ; (c) chapeau plein conf 0,55 ; (d) chapeau "   " conf 0,90 → `findByToolAndBranch` ne retourne que (a).
- **Régression** : un mapping valide existant continue d'être renvoyé.
- **Isolation workspace** : N/A (donnée de référence globale, pas de `workspace_id`).
- **Manuel staging** : rouvrir le Comparateur → la citation vide a disparu.

---

## Tables / endpoints / composants impactés
- **Endpoint** : `GET /api/v1/tools/{toolId}/jurisprudence-citations` (comportement filtré).
- **Backend** : `ToolJurisprudenceMappingRepository` (nouvelle requête), `ToolJurisprudenceService` (seuil + appel).
- **F-JU-02** : `ConclusionsJurisprudenceContext` (bénéficie du filtre via le service commun — pas de modif propre).
- **Frontend** : `tool-jurisprudence-citations.component.html` (masquage chapeau vide, défense en profondeur).
- **Table** : `tool_jurisprudence_mappings` (lecture seule — aucune migration, données inchangées).

### Préoccupation transversale cochée : **Outil décisionnel métier**
Composants listés ci-dessus. Le filtre est appliqué au **point de lecture commun** (`findByToolAndBranch`) → couvre F-JU-01 (affichage) et F-JU-02 (conclusions) sans divergence. Pas d'impact Auth/Workspace/Plans/Navigation → smoke E2E auth/nav non requis.

---

## Hors périmètre
- **Ré-évaluation outillée** (requêtes JUDILIBRE ciblées, évaluateur durci, 2ᵉ passe pertinence, re-bootstrap des 121 outils) = **partie B / feature distincte** à tracer dans PRODUCT_SPEC + cadrage cohérence.
- Suppression/arbitrage manuel de mappings (exclu par décision PO : effectif/volumétrie).
- La citation hors-sujet `19-16.283` (conf 0,72) reste visible jusqu'à B (transitoire assumé).
- Modification du seuil de rétention au bootstrap (0,70) → relève de B.
