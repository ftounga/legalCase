# SF-283-06-00 — Cadrage cohérence : synthèse enrichie intégrant les nouveaux documents en profondeur

> Étape 0 (skill `feature-coherence-challenger`). Issu de l'analyse 2026-06-22.

## Workflow métier réel
1. L'avocat construit son dossier, lance l'analyse → **synthèse**.
2. **Round 2** : il reçoit/ajoute de nouvelles pièces (ex. conclusions adverses), les uploade.
3. Dès l'upload, chaque document est **analysé en profondeur automatiquement** (pipeline `ExtractionService → ChunkingService → DocumentAnalysisService` : une `DocumentAnalysis DONE` est créée en quelques secondes).
4. L'avocat clique **« Relancer l'analyse »** (= synthèse enrichie, `/re-analyze`) pour que le dossier reflète la curation **et** les nouvelles pièces.

## Cartographie / cohérence
- **Amont** ✅ : au moment du `/re-analyze`, les nouveaux documents ont **déjà** une `DocumentAnalysis DONE` (analyse profonde) — aucun pré-requis manquant, aucune barrière async à créer.
- **Constat (le trou)** : la synthèse enrichie (`EnrichedAnalysisService.buildEnrichedPrompt`) n'injectait que la **synthèse précédente + des extraits bruts tronqués (2000 car.)** des documents. Elle **n'utilisait pas** les `DocumentAnalysis` profondes → les nouveaux documents n'étaient intégrés que superficiellement (voire ignorés si extraction non finie).
- **Aval** ✅ : la synthèse enrichie alimente conclusions (F-98/F-261) et outils ; une synthèse mieux nourrie des nouveaux docs améliore directement la réplique.

## Verdict : **GO**
Invariants anti-gadget imposés à la mini-spec :
1. **Aucune nouvelle analyse déclenchée, aucune attente** (pas de barrière async) — on lit les `DocumentAnalysis DONE` qui existent déjà.
2. **Fail-open** : analyse manquante / en cours → ignorée, l'enrichie tourne quand même.
3. **Tokens bornés** : cap par document (le JSON par doc est structuré).
4. **Ne pas casser la baseline** : on **conserve** les extraits bruts (vérification baseline) ; on **ajoute** la section profonde.
5. Aucune écriture, aucune migration, aucun changement de contrat / d'écran.

## Note 0 bis
Feature **purement backend** (enrichissement du prompt de synthèse enrichie) — **aucun élément d'écran nouveau** → étape 0 bis exemptée.
