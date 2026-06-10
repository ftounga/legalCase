# Mini-spec — F-261 / SF-261-02 (+03 travail FR) — Extraction & réfutation des moyens adverses

> Programme Conclusions V2 / F-261. Dépend de SF-261-01 (tag document adverse, livrée PR #1619).
> Backend-only. Couvre l'**extraction** (SF-261-02, vague TRAVAIL FR) **et l'injection/réfutation** (SF-261-03, mécanisme uniforme) — inséparables pour livrer de la valeur. Immigration/Famille = vagues d'extraction suivantes.

## Identifiant
`F-261 / SF-261-02` (+ mécanisme SF-261-03)

## Statut
`ready`

## Branche
`feat/SF-261-02-extraction-moyens-adverses`

## Objectif
> Extraire les **moyens** de la partie adverse depuis le(s) document(s) marqué(s) « écritures adverses » (SF-261-01), et générer des conclusions qui les **réfutent moyen par moyen**.

## Comportement attendu

### Cas nominal (travail FR)
1. À la génération des conclusions, le backend charge les documents du dossier avec `adverse_pleadings = true` et leur **texte extrait**.
2. Si le domaine = `DROIT_DU_TRAVAIL` / `FRANCE` et qu'il y a du texte adverse : appel LLM **d'extraction** (prompt dédié travail FR) → liste de **moyens adverses** structurés `{ thèse, fondements (articles/bases invoqués), piècesInvoquées }`.
3. Ces moyens alimentent une section **« MOYENS ADVERSES À RÉFUTER »** du prompt de génération ; le LLM produit une **réfutation moyen par moyen** (le moyen est mal fondé / contredit par les faits / la jurisprudence invoquée est inopérante).
4. Domaines non encore couverts (immigration/famille) ou aucun document adverse : **aucune extraction, aucune section** (no-op).

### Cas d'erreur / bords
| Situation | Comportement |
|---|---|
| Aucun document `adverse_pleadings` | pas d'extraction, pas de section (no-op) |
| Texte adverse vide / extraction LLM échoue | fail-open : pas de section, génération normale (log) |
| Domaine non couvert (im/fa) | no-op (framework prêt, vague future) |
| Moyen non identifiable | l'extraction ne fabrique rien ; liste vide → pas de section |

## Analyse de cohérence transversale
- **Source** : documents marqués SF-261-01 (`adverse_pleadings`) + leur texte extrait (`DocumentExtraction`).
- **vs SF-98-56** (citations adverses) : complémentaire — « MOYENS ADVERSES À RÉFUTER » (arguments) coexiste avec « JURISPRUDENCE ADVERSE À RÉFUTER » (citations), sections distinctes, pas de doublon.
- **Domaines** : extraction **par domaine** (travail FR cette SF ; im/fa = vagues). L'injection (section builder + consigne) est **uniforme**.

### Résultat du scan
| Cible | Applicable | Traitement |
|---|---|---|
| Extraction travail FR | Oui | Intégré (prompt dédié) |
| Extraction immigration/famille | Oui (futur) | Framework prêt (switch domaine → vide), **vagues suivantes** |
| Injection builder (uniforme) | Oui | Intégré (section + consigne, 45 cellules) |
| SF-98-56 (citations) | Non (complémentaire) | Coexistence, libellés distincts |

### Décision
- [x] Framework domaine + vague travail FR dans cette SF ; immigration/famille = vagues backlog explicites.
- [x] Injection uniforme intégrée (mécanisme SF-261-03).

## Conformité F-IA-04
- [x] **Non applicable** — pas de composant décisionnel ; extraction + enrichissement du prompt côté backend.

## Champs IA à extraire
- [x] **Aucun pré-remplissage d'outil** — l'extraction produit des **moyens adverses** (intrant de génération), pas un pré-remplissage de formulaire décisionnel.

## Critères d'acceptation
- [ ] Génération sur dossier travail FR avec ≥ 1 document marqué adverse → section « MOYENS ADVERSES À RÉFUTER » dans le prompt, alimentée par les moyens extraits.
- [ ] L'acte généré **réfute les moyens réels** du document adverse (pas génériques).
- [ ] Aucun document adverse / domaine non couvert → **aucune section** (no-op).
- [ ] Extraction **fail-open** (échec LLM / texte vide → génération normale sans section).
- [ ] **Anti-invention** : pas de moyen adverse fabriqué hors du texte ; non-régression SF-98-55 (anti-jargon) / SF-98-56 (citations).
- [ ] Domaine im/fa : framework renvoie vide (vague future), aucune erreur.

## Périmètre
### Hors scope
- **Extraction immigration / famille** (vagues suivantes — prompts dédiés).
- **Persistance / affichage des moyens extraits** (extraction paresseuse à la génération en MVP ; pas de table, pas d'UI). Optimisation (cache) + affichage = backlog.
- Saisie manuelle des moyens (option C de l'étape 0).

## Technique
### Pas d'endpoint, pas de migration
Extraction **paresseuse dans le flux de génération** (`CaseConclusionService.prepare`). Aucun nouvel endpoint, aucune table, aucun job.

### Backend
- Record `AdverseMoyen(String these, List<String> fondements, List<String> piecesInvoquees)` (ou équivalent).
- Service `AdverseMoyensExtractor` : `extract(List<String> texteAdverse, LegalDomain, country) : List<AdverseMoyen>`. Switch domaine : `DROIT_DU_TRAVAIL`+`FRANCE` → appel LLM (prompt dédié, JSON structuré, `temperature=0`, gate `AiCallContext` obligatoire — pattern `AnthropicService`) ; autres → `List.of()`. Fail-open (try/catch → vide).
- `CaseConclusionService.prepare` : charger les documents `adverse_pleadings=true` + leur texte extrait (`DocumentExtraction`/extractionRepository) → `adverseMoyensExtractor.extract(...)` → passer à `ConclusionPromptInput`.
- `CaseConclusionPromptBuilder` : + champ `List<AdverseMoyenToRefute> adverseMoyens` dans `ConclusionPromptInput` ; méthode `appendAdverseMoyensToRefute` → section `=== MOYENS ADVERSES À RÉFUTER ===` (par moyen : thèse + fondements + pièces) ; consigne de réfutation moyen par moyen (enrichir `REDACTION_QUALITY_GUARD` ou une garde dédiée, sans casser SF-98-55/56) : « Réfute chaque moyen adverse listé : démontre qu'il est mal fondé / contredit par les faits et les pièces / que la jurisprudence invoquée est inopérante. N'invente pas de moyen adverse. »

### Tables / migration
- [x] **Aucune** (extraction paresseuse, pas de stockage).

## Plan de test
### Unitaires (backend)
- [ ] `AdverseMoyensExtractor` : domaine travail FR + texte → appelle le LLM (mocké) → parse les moyens ; domaine im/fa → vide ; texte vide → vide ; échec LLM → vide (fail-open).
- [ ] `CaseConclusionPromptBuilder` : section « MOYENS ADVERSES À RÉFUTER » présente si moyens non vides ; absente sinon ; garde contient la consigne de réfutation des moyens ; non-régression SF-98-55/56.
- [ ] `CaseConclusionService.prepare` : charge bien les docs adverses + appelle l'extracteur (mocké) ; pas d'appel si aucun doc adverse.
### Isolation workspace
- [x] Réutilise le chargement des documents déjà borné au dossier/workspace.

## Analyse d'impact
- [x] **Aucune préoccupation transversale** (pas d'auth/workspace nouveau/plan/navigation/endpoint/schéma). ⚠️ **Gate Anthropic** : l'appel d'extraction DOIT passer par `AnthropicService` avec `AiCallContext` (record obligatoire) — pattern existant.
### Smoke E2E
- [ ] Aucun (backend pur) — validé par UT + staging.

## Dépendances
- SF-261-01 (tag document adverse) — `done` (PR #1619).
- SF-98-55/56 — `done` (la réfutation s'y conforme et coexiste).

## Notes et décisions
- **Extraction paresseuse à la génération** (vs job/table persistés) : MVP self-contained, pas de schéma. Limite : re-extrait à chaque génération (coût LLM) — optimisation (cache) = backlog.
- **Framework domaine** : travail FR implémenté ; immigration/famille = `List.of()` (vagues suivantes, prompts dédiés).
- **Anti-invention + fail-open** : à défaut de moyens fiables, aucune section (silence > erreur), cohérent avec les invariants F-179/SF-98-56.
