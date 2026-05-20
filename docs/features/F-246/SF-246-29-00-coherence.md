# Cadrage cohérence — F-246 / SF-246-29 — Pré-fill exhaustif IA de l'outil F-DT-38 (rupture de période d'essai)

> Skill source : `ai-skills/feature-coherence-challenger.md`.
> Pré-requis : F-DT-38 livrée 2026-05-20 (PR #1135) avec un pré-fill IA **partiel** (9 champs sur 23 du formulaire).

---

## Périmètre de la subfeature

Pré-remplir par l'IA les **14 champs restants** du formulaire `RupturePeriodeEssaiRequest`
de l'outil décisionnel F-DT-38 (Travail FR — qualification d'une rupture pendant la
période d'essai), pour appliquer l'invariant F-246 « tout champ saisissable d'un outil
décisionnel doit être pré-rempli par l'IA ; seule exception admise = information absente
des documents uploadés » (décision product owner 2026-05-19).

**Backend pur** : extension du record `TravailExtractedData` (sous-objet
`rupturePeriodeEssaiDetail` ≈ 14 nouveaux champs nullables), extension du prompt
`LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` (sous-objet `rupture_periode_essai_detail`
côté JSON), extension de l'extracteur `extractTravailData()`, extension du DTO frontend
`TravailExtractedData` (`case-analysis.model.ts`), extension du helper Jest
`rupture-periode-essai-section-prefill-rules.ts`, branchement du composant Angular.

**Pas de nouvelle section UI, pas de migration DB, pas de changement de visibility.**

---

## Workflow métier de l'avocat (réel)

L'avocat ouvre un dossier de licenciement en période d'essai (contrat + lettre de rupture
+ bulletins). Il déclenche l'analyse IA → la synthèse IA expose `travailExtractedData`.
Il ouvre le panel décisionnel F-IA-04 → la tuile F-DT-38 est visible (visibilité
contextuelle FR + flag IA déjà branchés par PR #1135). Il ouvre la section → le
formulaire (23 champs) doit s'afficher pré-rempli au maximum. Aujourd'hui : 9 champs
seulement le sont, les 14 autres restent vides → l'avocat doit ressaisir manuellement
(catégorie socio-pro, durée d'essai contractuelle, renouvellement, auteur, prévenance,
nature professionnelle ou non du motif, motif économique, atteinte liberté, lettre
motivée, motifs avérés, CCN plus favorable, etc.).

Workflow attendu après SF-246-29 : l'avocat ouvre la section → tout ce qui est dans les
pièces est pré-rempli, badge `auto_awesome` visible, il valide ou corrige et clique
« Calculer ».

---

## Cartographie des features existantes sur ce workflow

| Étape workflow | Feature existante | État |
|---|---|---|
| Analyse IA → synthèse travail | F-DT-08 + pipeline `CaseAnalysisService` | ✅ |
| Panel décisionnel F-IA-04 | F-205 / F-IA-04 / F-237 | ✅ |
| Tuile dashboard F-DT-38 | F-DT-38 (PR #1135) | ✅ |
| Visibilité contextuelle FR | F-DT-38 migration 256 | ✅ |
| Pré-fill helper `rupture-periode-essai-section-prefill-rules.ts` | F-DT-38 SF-DT-38-02 | ⚠️ partiel (9/23) |
| Sous-objet IA `rupture_periode_essai_detail` dans prompt + record | **manquant** | ❌ — objet de cette SF |
| Calculator backend `RupturePeriodeEssaiCalculator` | F-DT-38 | ✅ — inchangé |

---

## Challenge cohérence amont

| Pré-requis fonctionnel | Disponible dans le produit ? |
|---|---|
| Pipeline IA `CaseAnalysisService` produisant `travailExtractedData` | Oui (F-DT-08 + LegalDomainPromptBuilder) |
| Record `TravailExtractedData` extensible via Builder F-234 | Oui (pattern dupliqué 25+ fois) |
| Helper `*-prefill-rules.ts` consommant `aiData` | Oui (SF-DT-38-02) |
| Composant `RupturePeriodeEssaiSectionComponent` avec `prefillFromAi()` + provenance | Oui (SF-DT-38-02) |
| TOOL_REGISTRY binding `aiData: ctx.synthesis?.travailExtractedData` | Oui (SF-DT-38-02) |
| Whitelist d'enum côté backend (`SECTEUR_ACTIVITE_CODES`, etc.) | Oui — pattern à dupliquer pour `CATEGORIE_SOCIO_PROFESSIONNELLE_CODES`, `AUTEUR_RUPTURE_CODES` |
| Validators `isoDateOrNull` / `booleanOrNull` / `boundedIntOrNull` / `normalizeEnumCode` | Oui (déjà utilisés par 18+ SF-246) |
| Prompt `TRAVAIL_INSTRUCTION` PART2 sous limite JVM | Oui — 53 437 / 65 535 octets, marge 12 KB suffisante pour ~5 KB de nouveau sous-objet |

**Verdict amont** : tous les pré-requis sont présents. Aucune SF bloquante.

---

## Challenge cohérence aval

| Sortie de la SF | Exploitée par |
|---|---|
| 14 champs nullables additionnels dans `TravailExtractedData` | Helper Jest `rupture-periode-essai-section-prefill-rules.ts` (étendu dans la SF) ; tile dashboard inchangée |
| Sous-objet `rupture_periode_essai_detail` dans la synthèse IA | Composant `RupturePeriodeEssaiSectionComponent` (étendu pour brancher provenance + handlers) |
| Compteur `getPrefillCount()` recalculé sur 23 champs | Badge « Pré-rempli par l'IA (N) » du panel F-IA-04 |
| Calculator backend `RupturePeriodeEssaiCalculator` | **Inchangé** — les inputs persistés restent les mêmes (`RupturePeriodeEssaiRequest`) |

**Verdict aval** : la SF n'introduit aucune nouvelle sortie consommée. Elle alimente
uniquement le helper de pré-fill frontend. Risque de régression circonscrit au
record `TravailExtractedData` (test `CaseAnalysisResponseTest` à étendre, builder F-234
à propager).

---

## Cohérence transversale (autres outils, autres pays, autres domaines)

- **F-DT-39 jumeau BE** : la rupture de période d'essai BE n'existe pas (statut unique
  2014). Aucun jumeau, ces champs restent `null` pour la BE — le prompt l'impose.
- **F-DT-08 / F-DT-36 / F-DT-24** (autres outils Travail FR à formulaire) : aucun
  recouvrement — les 14 champs sont propres à la période d'essai (catégorie socio-pro
  L.1221-19, durée contractuelle, renouvellement L.1221-23, délai de prévenance
  L.1221-25, auteur de rupture, lettre motivée pondérée Marjolaine, CCN plus favorable
  L.1221-22). Pas de doublon.
- **Outils Famille / Immigration** : non applicable — concept propre à la rupture
  d'essai travail.
- **Domain transversal Outil décisionnel métier** : la SF coche le déclencheur (Travail
  FR, modification d'un outil livré). Composants impactés listés ci-dessous.

---

## Invariants anti-gadget pour la mini-spec

1. **Aucun champ par défaut hallucinatoire** : un champ doit être `null` si l'info n'est
   pas dans les pièces (pas de `CDI` par défaut, pas de `CADRE` par défaut côté record).
2. **Whitelist stricte sur les enums** : `categorie_socio_professionnelle` et
   `auteur_rupture` passent par `normalizeEnumCode()` ou `whitelistedOrNull()` — un code
   hors enum → `null` (jamais de fallback arbitraire).
3. **Dates en `isoDateOrNull()`** : aucune date au format `DD/MM/YYYY` n'est admise —
   fail-open (`null`).
4. **Entiers bornés** : `dureePeriodeEssaiContractuelleMois` ∈ [0, 24],
   `delaiPrevenanceJoursAppliques` ∈ [0, 30], `dureeCddMois` ∈ [0, 36].
5. **Gating pays strict** : le prompt impose `null` pour un dossier travail BE
   (rupture d'essai = mécanisme FR uniquement).
6. **Pas d'extension du Calculator ni des inputs persistés** : `RupturePeriodeEssaiRequest`
   reste inchangée. La SF n'ajoute aucune nouvelle colonne à la table
   `rupture_periode_essai_analyses`.
7. **Parité stricte `getPrefillCount()` ↔ `prefillFromAi()`** : les 23 champs comptés
   sont les 23 champs effectivement pré-remplis (test Jest 0 / partiel / nominal 23).
8. **Saturation prompt** : ajout du sous-objet en PART2 (12 KB de marge, ajout estimé
   ≈ 4-5 KB) — pas besoin de basculer un autre sous-objet.

---

## Verdict

**🟢 GO**.

La SF est une SF de **dette propre** (compléter le pré-fill d'un outil livré la veille).
Aucun pré-requis manquant, aucun risque aval autre que le test du record étendu, scope
strictement délimité (record + prompt + extracteur + DTO frontend + helper + composant —
backend pur sauf le helper et le composant côté frontend qui restent dans la même SF
puisque le binding `aiData` existe déjà).

**Statut PRODUCT_SPEC** : F-246 reste « En cours — 28/29 SF », passera Terminée à la
fin de cette SF.

---

## Étape 0 bis cohérence écran

**Non applicable** — la SF ne déplace aucun élément UI, n'ajoute aucune section, ne
change aucune route. Elle complète le pré-fill d'un composant existant (badges
`auto_awesome` apparaîtront sur 14 champs supplémentaires — addition silencieuse,
pas de restructuration). Exemption documentée par l'invariant CLAUDE.md « impact écran
nul ».
