# SF-283-06 — La synthèse enrichie intègre l'analyse profonde des documents (nouveaux inclus)

> Cadrage étape 0 : `SF-283-06-00-coherence.md` (GO). 0 bis exempt (backend pur). Issu de l'analyse 2026-06-22 (point D).

## Objectif (une phrase)
Injecter, dans le prompt de **synthèse enrichie** (`/re-analyze`), l'**analyse profonde par document** (`DocumentAnalysis` DONE : faits / points juridiques / risques) de chaque pièce du dossier — y compris les **documents ajoutés depuis l'analyse initiale**, déjà analysés en profondeur à l'upload — pour qu'ils soient **réellement intégrés** à la synthèse.

## Problème
`EnrichedAnalysisService.buildEnrichedPrompt` n'injectait que la synthèse précédente + des **extraits bruts tronqués (2000 car.)** des documents (`buildRawDocumentsSection`), **jamais** les `DocumentAnalysis` profondes. Un document ajouté au round 2 n'était donc intégré que superficiellement (ou pas du tout si son extraction n'était pas finie au moment du `/re-analyze`).

## Comportement nominal
Nouvelle section `[Analyses approfondies des documents — y compris ceux ajoutés depuis l'analyse initiale]` dans le prompt enrichi : pour chaque document du dossier ayant une `DocumentAnalysis DONE`, son `analysisResult` (JSON), borné à **3000 car./document**. La section des extraits bruts (baseline) est **conservée**.

- Sources : `documentRepository.findByCaseFile_IdOrderByCreatedAtDesc` + `documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, DONE)`.
- **Aucune analyse déclenchée, aucune attente** : on lit ce qui existe (les nouveaux docs ont déjà leur `DocumentAnalysis DONE` via le pipeline d'upload).

## Cas d'erreur / fail-open
- Aucune `DocumentAnalysis DONE` → section vide, l'enrichie tourne normalement (extraits bruts + synthèse précédente).
- Analyse d'un doc encore PENDING/PROCESSING → ignorée (pas de blocage). Limite : si on relance l'enrichie *avant* la fin de l'analyse profonde d'un nouveau doc (rare, quelques secondes), ce doc reste sur les extraits bruts ce run.

## Critères d'acceptation
- **CA1** : un document avec `DocumentAnalysis DONE` → son `analysisResult` figure dans le prompt enrichi, sous `[Analyses approfondies des documents…]`, libellé `"<fichier> (analyse)"`.
- **CA2** : aucune analyse profonde → section absente, prompt enrichi inchangé par ailleurs.
- **CA3** : un nouveau document (ajouté après l'analyse initiale, déjà analysé) est inclus comme les autres.
- **CA4** : extraits bruts toujours présents (baseline non cassée).
- **CA5** : cap par doc à 3000 car. respecté.

## Plan de test
- `EnrichedAnalysisServiceTest` : injection de l'analyse profonde (CA1/CA3) ; sections vides par défaut (CA2) ; cas existants (synthèse précédente, Q&R, chat, extraits bruts) intacts.
- `JobFailureLoggingTest` : construction du service mise à jour (constructeur).

## Tables / endpoints / composants
- **`EnrichedAnalysisService`** : injection `DocumentAnalysisRepository` + `buildDocumentAnalysesSection` + appel dans `buildEnrichedPrompt`. **0 migration, 0 frontend, 0 changement de contrat / d'écran.**

## Hors périmètre
- Déclencher/attendre l'analyse documentaire dans le flux enrichi (inutile : déjà faite à l'upload ; éviterait la limite « relancé trop tôt » mais ajouterait une barrière async risquée — non retenu).
- Rafraîchissement auto du pré-remplissage des outils après enrichie (mineur, F5).
