# SF-261-06 — Ré-extraction des moyens adverses sur nouvel acte adverse

> Bugfix (étapes 0/0bis exemptées). Issu de l'analyse 2026-06-22 : la réplique ne réfutait pas les moyens d'un acte adverse ajouté après la 1ʳᵉ génération.

## Objectif (une phrase)
Quand un nouvel **acte adverse** (document `adversePleadings`) a été ajouté depuis la 1ʳᵉ extraction des moyens, **ré-extraire** les moyens adverses (replace-set) à la génération des conclusions, pour que la **réplique réfute les vrais nouveaux moyens** (cas round 2).

## Problème
`AdverseMoyenPersistenceService.loadOrExtract` court-circuitait : un set persisté était retourné **tel quel**, sans jamais re-regarder les écritures adverses. `refresh()` existait mais n'était **jamais appelé**. → Régénérer après ajout d'un acte adverse réfutait les **anciens** moyens.

## Comportement nominal
`loadOrExtract` : si un set est persisté ET qu'un document `adversePleadings` a une `createdAt` **postérieure** au set (max `created_at` des moyens persistés) → `extractAndPersist` (replace-set). Sinon : set conservé (aucun appel LLM, **curation F-288 préservée**, aucun non-déterminisme).

## Cas d'erreur / fail-open
- Aucun set persisté → extraction (inchangé). Set sans `created_at` (théorique) → pas de ré-extraction.
- Toute exception → liste vide (fail-open, inchangé).

## Critères d'acceptation
- **CA1** : set persisté + acte adverse `createdAt` postérieure → ré-extraction (replace-set), la réplique reflète les nouveaux moyens.
- **CA2** : set persisté + acte adverse antérieur (rien de neuf) → set conservé, **aucune** extraction/écriture.
- **CA3** : 1ʳᵉ génération (aucun set) → extraction (inchangé).
- **CA4** : fail-open inchangé.

## Plan de test
- `AdverseMoyenPersistenceServiceTest` : CA1 (ré-extraction) + CA2 (conservation) ; cas existants (1ʳᵉ extraction, lecture persistée, vide, fail-open) inchangés.

## Tables / endpoints / composants
- **`AdverseMoyenPersistenceService.loadOrExtract`** uniquement (+ helper `hasAdverseDocumentNewerThanPersistedSet`). **0 migration, 0 frontend, 0 changement de contrat**. La génération de conclusions (`CaseConclusionService`) bénéficie automatiquement.

## Hors périmètre / limite assumée
- Un document marqué « écritures adverses » **longtemps après** son upload n'est pas détecté (`Document` n'a pas de timestamp de modification) — cas rare. Pourrait être couvert par un timestamp de marquage (suivi).
- Faire que la **synthèse enrichie** ré-analyse en profondeur les nouveaux documents (correctif D, arbitrage séparé). Indépendant : les moyens adverses viennent directement de l'acte adverse (F-261), pas de la synthèse.
