# Mini-spec — F-218d / SF-218d-00 — Refactor consolidation slots `TravailExtractedData` + scaffold `Sf218dDetail` (prérequis)

## Identifiant
`F-218d / SF-218d-00` (refactor prérequis backend)

## Statut
`ready`

## Date
2026-06-03

## Branche Git
`feat/SF-218d-00-refactor-scaffold`

## Contexte / motif
`TravailExtractedData` (record `CaseAnalysisResponse.java`) est au **plafond JVM 254 params** (255 slots). Tout nouvel outil Travail FR est bloqué (`too many parameters` au compile). Prérequis dur de F-218d : libérer des slots via consolidation `@JsonUnwrapped` (pattern F-256 déjà éprouvé sur 27 sous-records), puis ajouter le sous-record consolidé `Sf218dDetail` des 9 outils F-218d.

## Objectif (1 phrase)
Libérer 11 slots du constructeur canonical de `TravailExtractedData` en regroupant deux clusters de flags booléens existants dans des sous-records `@JsonUnwrapped`, puis ajouter le sous-record `Sf218dDetail` + les instructions prompt PART36-44, **sans modifier le JSON produit** (clés plates inchangées).

## Comportement attendu

### Refactor (invariant : JSON inchangé)
1. **Cluster A — `Sf166ContextualFlagsDetail`** (record `@JsonUnwrapped`) : regroupe les 8 flags FR racine actuels `rappelSalaireDetecte`, `travailDissimuleDetecte`, `clauseNonConcurrenceDetectee`, `statutProtegeDetecte`, `transactionEnvisagee`, `atMpDetecte`, `urgenceProcedurale`, `contestationAreEnvisagee` (lignes ~141-148). 8 params → 1. Libère 7 slots.
2. **Cluster B — `Sf204ContextualFlagsDetail`** : regroupe les 5 flags BE racine `harcelementBeDetecte`, `discriminationBeDetectee`, `inaptitudeMedicaleBeDetectee`, `heuresSupMentionneesBe`, `motifGraveBeEnvisage` (lignes ~154-158). 5 params → 1. Libère 4 slots.
3. Mettre à jour `extractTravailData()` (construction builder) et **tous les sites de lecture** (`t.rappelSalaireDetecte()` → `t.sf166ContextualFlagsDetail().rappelSalaireDetecte()`), principalement dans les tests (`CaseAnalysisResponseTest.java`, ~84 assertions).
4. `DecisionToolVisibilityService` lit le `JsonNode` brut par clé snake_case → **aucune modification** (clés plates préservées par `@JsonUnwrapped`).

### Scaffold (rendu possible par le refactor)
5. Ajouter `Sf218dDetail` (`@JsonUnwrapped`, 1 param) : 9 flags pivot F-218d + champs valeur de pré-remplissage (cf. brief scaffold). Tous logés dans ce seul sous-record.
6. Ajouter `LegalDomainPromptBuilder` PART36-44 (9 instructions, une par flag).

## Cas d'erreur / garde-fous
| Situation | Attendu |
|---|---|
| Compile après refactor seul | params canonical réduits de 11 → vert |
| Compile après ajout `Sf218dDetail` | 254 − 11 + 1 = **244 params** → vert |
| `CaseAnalysisResponseSerializationParityTest` | JSON HTTP/persistance **identique** (clés plates) |
| Toute assertion de test sur un flag déplacé | adaptée à l'accès via sous-record |

## Critères d'acceptation
- [ ] `Sf166ContextualFlagsDetail` (8 flags) et `Sf204ContextualFlagsDetail` (5 flags) créés, `@JsonUnwrapped`.
- [ ] Params canonical `TravailExtractedData` : 254 → 243 après refactor seul ; 244 après ajout `Sf218dDetail`.
- [ ] `mvn compile` vert ; `mvn test` cible (`*CaseAnalysisResponse*`, `*Serialization*`, `*PromptBuilder*`, `*DecisionToolVisibility*`) vert.
- [ ] **JSON inchangé** : test parité sérialisation vert, aucune clé JSON modifiée.
- [ ] `Sf218dDetail` ajouté (9 flags + champs valeur) ; PART36-44 ajoutées.
- [ ] `DecisionToolVisibilityService` non modifié (preuve : grep des clés snake_case inchangé).

## Plan de test
- Compile complet backend.
- Tests ciblés : extraction (`CaseAnalysisResponseTest`), parité sérialisation, prompt builder, visibility service.
- Suite backend complète avant merge.

## Tables / endpoints / composants impactés
- `CaseAnalysisResponse.java` (record `TravailExtractedData` + 3 nouveaux sous-records : Sf166/Sf204/Sf218dDetail).
- `LegalDomainPromptBuilder.java` (PART36-44).
- Tests : `CaseAnalysisResponseTest`, `CaseAnalysisResponseSerializationParityTest`.
- **Aucune** migration DB, **aucun** endpoint, **aucun** changement de contrat JSON.

## Hors périmètre
- Cluster C (RCC BE) et autres consolidations (différées — non nécessaires ici).
- Les 9 analyzers/controllers/tables/composants F-218d (SF-218-37→54, vagues suivantes).
