# Mini-spec — F-98 / SF-98-47 — Style mimicking (génération adaptée au style appris)

> Cadrages amont : `SF-98-46-00-coherence.md` (étape 0). SF backend pure → pas de cadrage écran (l'indicateur de style côté écran est porté par SF-98-48).

## Identifiant
`F-98 / SF-98-47`

## Feature parente
`F-98` — Génération de courrier / conclusions (bloc style learning)

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-47-backend-style-mimicking` — **SF backend pure**.

---

## Objectif
Faire en sorte que la génération de conclusions (SF-98-01 et cellules suivantes) **adopte le style rédactionnel appris** du cabinet, à partir des signatures de style produites par SF-98-46.

---

## Comportement attendu

### Cas nominal
1. Lors de la génération d'une version de conclusions (worker `CaseConclusionService`), le `CaseConclusionPromptBuilder` récupère les **signatures de style actives** du workspace du dossier (`style_corpus_documents` où `active = true` et `status = DONE`).
2. S'il en existe au moins une, le prompt système intègre une consigne d'adaptation de style reprenant ces signatures.
3. Le projet généré adopte le style ; la ligne `case_conclusions` enregistre `style_applied = true`.
4. `ConclusionResponse` expose `styleApplied` (booléen) — consommé par la section « Conclusions » (indicateur SF-98-48, ajustement b2 du cadrage écran).

### Cas d'erreur / dégradation
| Situation | Comportement |
|---|---|
| Aucune signature de style active pour le workspace | Génération **générique** inchangée (comportement SF-98-01) ; `style_applied = false`. Aucune erreur. |
| Signatures présentes mais lecture en échec | Fail-open : génération générique, `style_applied = false`, log d'avertissement. |

---

## Analyse de cohérence transversale
- [x] **Modifie** `CaseConclusionPromptBuilder` (SF-98-01) et `CaseConclusion` (+ `style_applied`) — extension additive, aucun contrat cassé (`styleApplied` est un champ additionnel du `ConclusionResponse`).
- [x] **Transversal F-98** : le style mimicking bénéficie à toutes les cellules de la matrice (le `CaseConclusionPromptBuilder` est partagé).
- [x] Dépend de l'entité `StyleCorpusDocument` (SF-98-46) — Java dependency → SF-98-47 développée après le merge de SF-98-46.

### Décision
- [x] Étendu au point d'injection unique (`CaseConclusionPromptBuilder`). Aucune duplication.

## Conformité F-IA-04
- [x] **Non applicable** — SF backend pure.

---

## Critères d'acceptation
- [ ] **CA1** — Avec ≥ 1 signature de style active sur le workspace, le prompt système de génération intègre une consigne d'adaptation de style reprenant les signatures.
- [ ] **CA2** — La ligne `case_conclusions` générée porte `style_applied = true` ; `ConclusionResponse.styleApplied = true`.
- [ ] **CA3** — Sans signature active, la génération est identique au comportement SF-98-01 ; `style_applied = false`.
- [ ] **CA4** — Lecture des signatures en échec → fail-open (génération générique, `style_applied = false`, pas d'exception propagée).
- [ ] **CA5** — Seules les signatures `active = true` ET `status = DONE` sont utilisées.
- [ ] **CA6** — Le style injecté est une **description de style**, jamais un contenu de dossier (garanti par SF-98-46 : la signature ne contient pas de donnée client).

---

## Périmètre
### Hors scope
- Ingestion du corpus — SF-98-46.
- Écran cabinet + indicateur visuel — SF-98-48 (consomme `styleApplied`).
- Pondération / sélection fine des signatures (toutes les signatures actives sont utilisées en V1).

---

## Technique

### Tables impactées
| Table | Opération |
|---|---|
| `case_conclusions` | ALTER — ajout `style_applied` BOOLEAN NOT NULL défaut `false` |
| `style_corpus_documents` | SELECT (lecture des signatures actives) |

### Migration Liquibase
- [x] Oui — `{NNN}-add-style-applied-to-case-conclusions.xml` — **prochain numéro libre sur `origin/master`** au moment du dev. Rollback : drop column.

### Composants Backend
- `CaseConclusionPromptBuilder` : nouvelle entrée — les signatures de style actives ; le `buildSystemPrompt` (ou `buildUserMessage`) intègre la consigne d'adaptation quand des signatures existent.
- `CaseConclusionService` (worker) : charge les signatures actives du workspace, les passe au prompt builder, renseigne `style_applied`.
- `CaseConclusion` (+ `styleApplied`), `ConclusionResponse` (+ `styleApplied`).
- `StyleCorpusRepository` : `findByWorkspaceIdAndActiveTrueAndStatus(...)` (ajouté côté SF-98-46 ou ici).

### Contrat API
Aucun nouvel endpoint. `ConclusionResponse` gagne `styleApplied: boolean` (champ additif).

---

## Plan de test
### Backend (UT)
- [ ] `CaseConclusionPromptBuilderTest` : avec signatures → prompt contient la consigne de style ; sans → prompt inchangé.
- [ ] `CaseConclusionServiceTest` : génération avec signatures actives → `style_applied = true` ; sans → `false` ; lecture en échec → fail-open `false`.
- [ ] `CaseConclusionControllerIT` : `ConclusionResponse.styleApplied` reflété correctement.
### Isolation workspace
- [x] Applicable — les signatures lues sont celles du workspace du dossier (testé).

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — extension additive de la génération existante ; pas d'impact auth/navigation/plans.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- **SF-98-46** (corpus de style) — doit être **mergée** avant le dev de SF-98-47 (dépendance Java sur l'entité `StyleCorpusDocument`).
- SF-98-01 (génération) — done.

## Notes et décisions
- **Dégradation propre** (invariant 7 du cadrage) : sans corpus, F-98 fonctionne exactement comme avant. Le style learning est strictement additif.
- `styleApplied` alimente l'indicateur de transparence de la section « Conclusions » (ajustement b2 du cadrage écran, livré par SF-98-48).
