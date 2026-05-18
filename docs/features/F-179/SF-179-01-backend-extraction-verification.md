# Mini-spec — [F-179 / SF-179-01] Backend — extraction des références jurisprudentielles + vérification Sonnet

> Mini-spec produite via `ai-skills/story-writer.md`. À valider avant dev.

---

## Identifiant

`F-179 / SF-179-01`

## Feature parente

`F-179` — Vérification de jurisprudence citée dans les documents uploadés (FR + BE)

## Statut

`ready`

## Date de création

2026-05-18

## Branche Git

`feat/SF-179-01-backend-extraction-verification`

---

## Objectif

Détecter automatiquement les références jurisprudentielles citées dans les documents uploadés d'un dossier (regex + complément Claude Sonnet), les faire vérifier par Sonnet (existence + fidélité de la position alléguée), persister les résultats dans une nouvelle table `jurisprudence_checks` avec isolation workspace, et les exposer via un endpoint GET.

---

## Comportement attendu

### Cas nominal

1. Une `CaseAnalysis` se termine `DONE` dans `CaseAnalysisService.finalizeCaseAnalysis`.
2. En post-traitement **fail-open** (pattern miroir `procedureCheckService.createChecks` / `sourceExplanationGenerator.generate`), le nouveau `JurisprudenceVerificationService.verifyForAnalysis(analysis)` est appelé.
3. Le service charge le texte extrait (`DocumentExtraction.extractedText`, statut `DONE`) de chaque document du dossier.
4. **Extraction des références** : pour chaque document, un pré-filtrage regex repère les formats standard (`Cass. soc. JJ/MM/AAAA n°XX-XX.XXX`, `CE n°XXXXXX`, `CA Paris XX/XX/XXXX`, `Trib. trav. ... JJ/MM/AAAA`, `Cour const. ... n°XX/XXXX`, `Cass. ... n°...`). Le texte des documents (tronqué à un budget de caractères raisonnable par document) est aussi soumis à Sonnet dans le prompt de vérification, pour rattraper les références noyées dans le texte que la regex rate.
5. **Vérification Sonnet** : un appel unique à Sonnet (`AnthropicService.analyzeWithModel` ou `analyzeWithSystemCache`, prompt caching du system prompt) reçoit (a) la liste des candidats regex, (b) des extraits des documents, et renvoie un JSON listant chaque référence avec : `reference` (libellé canonique), `documentName`, `statut` (`VERIFIED`/`SUSPECT`/`NOT_FOUND`/`UNCERTAIN`), `explication` (courte), `positionAlleguee` (la position que le document prête à l'arrêt, si détectable), `claudeConfidence` (`HIGH`/`MEDIUM`/`LOW`).
6. Pour chaque référence renvoyée, une ligne `jurisprudence_checks` est persistée : `caseFileId`, `caseAnalysisId`, `workspaceId`, `documentName`, `reference`, `statut`, `explication`, `positionAlleguee`, `sourceUrl` (null à ce stade — rempli par SF-179-02), `claudeConfidence`, `webSearchUsed` (`false` à ce stade).
7. Les anciennes lignes `jurisprudence_checks` d'une analyse précédente du même dossier ne sont pas supprimées : chaque ligne est rattachée à son `caseAnalysisId` ; la lecture filtre sur la dernière analyse `DONE`.
8. L'endpoint `GET /api/v1/case-files/{caseFileId}/jurisprudence-checks` renvoie la liste des checks de la dernière analyse `DONE` du dossier, groupable par `documentName` côté frontend.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Aucun document avec texte extrait | `verifyForAnalysis` ne persiste rien, log debug ; `CaseAnalysis` reste `DONE` | — |
| Aucune référence détectée (regex vide + Sonnet renvoie liste vide) | Aucune ligne persistée ; `CaseAnalysis` reste `DONE` | — |
| Appel Sonnet échoue (HTTP, timeout, JSON non parsable) | Fail-open : log warn, aucune ligne persistée, `CaseAnalysis` reste `DONE` | — |
| Sonnet renvoie un statut hors des 4 valeurs | La référence est normalisée en `UNCERTAIN` (jamais inventer `NOT_FOUND`) | — |
| Token absent sur le GET | Non autorisé | 401 |
| `caseFileId` inexistant ou hors workspace | Accès refusé (404 camouflage) | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-179 n'est pas un outil décisionnel (calculator/analyzer/generator) — c'est un post-traitement transversal du pipeline IA, analogue à `procedureCheckService` (F-96), `sourceExplanationGenerator` (F-IA-03), `caseDeadlineService` (détection délais). Aucun outil décisionnel impacté.
- [x] **Autres pays** : FR + BE couverts dès cette SF. La regex et le prompt Sonnet listent les formats FR (`Cass. soc.`, `CE`, `CA`) et BE (`Trib. trav.`, `Cour const. BE`, `Cass. BE`). L'adaptation par pays ne concerne que le web search (SF-179-02). Pas de gate `workspaceCountry` : un dossier FR peut citer un arrêt BE et inversement — la détection est agnostique du pays du workspace.
- [x] **Autres domaines** : transversal — toute jurisprudence, tout domaine (Travail / Famille / Immigration). Aucune adaptation métier.
- [x] **Autres UI patterns** : aucun (SF backend pure). L'affichage est SF-179-03.
- [x] **Autres flows transversaux** : pipeline IA / post-traitement `CaseAnalysisService` — voir « Analyse d'impact » ci-dessous.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Post-traitement `CaseAnalysisService.finalizeCaseAnalysis` | Oui | Intégré dans cette SF — nouveau hook fail-open après `caseDeadlineService` / `sourceExplanationGenerator`. |
| `EnrichedAnalysisService` (synthèse enrichie) | Oui | Le même hook est ajouté en post-traitement de `EnrichedAnalysisService` si ce service finalise aussi des `CaseAnalysis` — à vérifier au dev ; sinon non applicable. |
| Outils décisionnels | Non | F-179 n'est pas un outil décisionnel. |
| `WebSearchService` | Oui | Créé / appelé en SF-179-02 — hors scope de cette SF. |
| FR vs BE | Oui | Couvert dans cette SF (regex + prompt bilingues). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (FR + BE ; post-traitement standard + enrichi).
- [x] Subfeature(s) parallèle(s) : SF-179-02 (web search), SF-179-03 (frontend), SF-179-04 (alerte F-IA-03).
- [x] Non applicable aux outils décisionnels (F-179 est un post-traitement transversal, justifié ci-dessus).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend pure. F-179 n'est pas un outil décisionnel (pas de formulaire de saisie avocat, pas de `result_data` par dossier saisi, pas d'entrée `TOOL_REGISTRY`). C'est un post-traitement du pipeline IA. Aucun composant frontend décisionnel livré ici.

---

## Critères d'acceptation

- [ ] Quand une `CaseAnalysis` passe `DONE` et que ses documents citent des arrêts au format standard, des lignes `jurisprudence_checks` sont persistées avec un statut parmi `VERIFIED`/`SUSPECT`/`NOT_FOUND`/`UNCERTAIN`.
- [ ] Quand Sonnet identifie un arrêt réel mais une position alléguée incohérente, le statut persisté est `SUSPECT` et `positionAlleguee` est renseigné.
- [ ] Quand l'appel Sonnet échoue (exception ou JSON non parsable), aucune ligne n'est persistée et la `CaseAnalysis` reste `DONE` (fail-open vérifié par test).
- [ ] Quand Sonnet renvoie un statut hors des 4 valeurs autorisées, la référence est normalisée en `UNCERTAIN`.
- [ ] `GET /api/v1/case-files/{caseFileId}/jurisprudence-checks` renvoie les checks de la dernière analyse `DONE`, chacun avec `documentName`, `reference`, `statut`, `explication`, `positionAlleguee`, `sourceUrl`, `claudeConfidence`, `webSearchUsed`.
- [ ] Un utilisateur du workspace A reçoit 404 sur `GET .../jurisprudence-checks` pour un `caseFileId` du workspace B.
- [ ] La regex détecte au moins les formats `Cass. soc.`, `CE`, `CA <ville>`, `Trib. trav.`, `Cour const.`, `Cass.` FR et BE (tests unitaires dédiés).

---

## Périmètre

### Hors scope (explicite)

- Fallback web search Légifrance / Juridat → SF-179-02 (`sourceUrl` reste `null`, `webSearchUsed` reste `false` ici).
- Affichage frontend → SF-179-03.
- Alerte cohérence F-IA-03 → SF-179-04.
- Statut markable avocat (vu / traité / écarté) sur les checks → hors V1 (cf. cadrage étape 0).
- Export du rapport de vérification → hors scope F-179 (V2).
- Génération proactive de jurisprudence → interdit par invariant anti-gadget 1.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `id` | UUID | généré à l'INSERT |
| `web_search_used` | `false` | toujours `false` à la création SF-179-01 ; passe `true` si SF-179-02 enrichit |
| `source_url` | `null` | rempli par SF-179-02 si web search aboutit |
| `created_at` | auto | renseigné par la base |
| `workspace_id` | workspace du dossier | dérivé de `caseFile.workspace.id` |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `reference` | Oui | 500 | texte libre (libellé de l'arrêt) | Non | `trim()` |
| `document_name` | Oui | 500 | nom de fichier | Non | — |
| `statut` | Oui | 20 | `VERIFIED` / `SUSPECT` / `NOT_FOUND` / `UNCERTAIN` | Non | défaut `UNCERTAIN` si valeur hors enum |
| `explication` | Non | 2000 | texte libre | Non | — |
| `position_alleguee` | Non | 2000 | texte libre | Non | — |
| `source_url` | Non | 1000 | URL | Non | — |
| `claude_confidence` | Non | 10 | `HIGH` / `MEDIUM` / `LOW` | Non | — |
| `web_search_used` | Oui | — | booléen | Non | — |

Notes :
- `statut` : aucune valeur hors enum n'est persistée — toute valeur inconnue retournée par Sonnet est ramenée à `UNCERTAIN`.
- Budget de texte par document soumis à Sonnet : borné (constante, ex. ~6000 caractères/doc) pour maîtriser le coût IA.

---

## Technique

### Contrat API (figé — pour parallélisation SF-179-03)

**`GET /api/v1/case-files/{caseFileId}/jurisprudence-checks`** — Auth oui, rôle `MEMBER`.

Réponse `200` :
```json
{
  "checks": [
    {
      "id": "uuid",
      "documentName": "conclusions_adverses.pdf",
      "reference": "Cass. soc. 25 septembre 2013, n° 12-17.516",
      "statut": "SUSPECT",
      "explication": "L'arrêt existe mais concerne la prescription, non la nullité du licenciement comme allégué.",
      "positionAlleguee": "La partie adverse prétend que cet arrêt fonde la nullité du licenciement.",
      "sourceUrl": null,
      "claudeConfidence": "HIGH",
      "webSearchUsed": false
    }
  ]
}
```
`checks` est `[]` si aucune référence détectée. Statuts possibles : `VERIFIED`, `SUSPECT`, `NOT_FOUND`, `UNCERTAIN`.
Erreurs : `401` token absent, `404` dossier inexistant ou hors workspace.

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/jurisprudence-checks` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `jurisprudence_checks` | CREATE (migration) + INSERT + SELECT | Nouvelle table. |
| `case_analyses` | SELECT | Lecture de la dernière analyse `DONE`. |
| `document_extractions` | SELECT | Lecture du texte extrait. |

### Migration Liquibase

- [x] Oui — `245-create-jurisprudence-checks.xml` (plage F-179 assignée : 245-249).
- Colonnes : `id` (uuid PK), `case_file_id` (uuid FK `case_files`, not null), `case_analysis_id` (uuid FK `case_analyses`, not null), `workspace_id` (uuid FK `workspaces`, not null), `document_name` (varchar 500, not null), `reference` (varchar 500, not null), `statut` (varchar 20, not null), `explication` (text), `position_alleguee` (text), `source_url` (varchar 1000), `claude_confidence` (varchar 10), `web_search_used` (boolean, not null, default false), `created_at` (timestamptz, not null).
- Index sur `case_analysis_id` et sur `case_file_id`. Rollback : `dropTable`.

### Composants Java

- `JurisprudenceCheck` — entité JPA.
- `JurisprudenceCheckRepository` — `findByCaseAnalysisId`, `findByCaseFileId`.
- `JurisprudenceCheckStatus` — enum `VERIFIED` / `SUSPECT` / `NOT_FOUND` / `UNCERTAIN` (ou constantes String).
- `JurisprudenceReferenceExtractor` — regex de pré-filtrage FR + BE (classe testable isolément).
- `JurisprudenceVerificationService` — orchestration : extraction + prompt Sonnet + persistance ; méthode `verifyForAnalysis(CaseAnalysis)` fail-open.
- `JurisprudenceCheckController` — `GET .../jurisprudence-checks`, isolation workspace.
- `JurisprudenceCheckResponse` — DTO de réponse.
- Hook dans `CaseAnalysisService.finalizeCaseAnalysis` (après `sourceExplanationGenerator`), en `try/catch` log warn (fail-open).

---

## Plan de test

### Tests unitaires

- [ ] `JurisprudenceReferenceExtractor` — détecte `Cass. soc. ... n°...`, `CE n°...`, `CA Paris ...`, `Trib. trav. ...`, `Cour const. ... n°...`, `Cass. ... n°...` (FR + BE).
- [ ] `JurisprudenceReferenceExtractor` — texte sans référence → liste vide.
- [ ] `JurisprudenceVerificationService` — Sonnet renvoie 3 références → 3 lignes persistées avec les bons statuts.
- [ ] `JurisprudenceVerificationService` — Sonnet renvoie un statut inconnu → normalisé `UNCERTAIN`.
- [ ] `JurisprudenceVerificationService` — exception Sonnet → aucune persistance, pas de propagation (fail-open).
- [ ] `JurisprudenceVerificationService` — aucun document avec texte → aucune persistance.

### Tests d'intégration

- [ ] `GET /api/v1/case-files/{id}/jurisprudence-checks` → 200 avec liste des checks de la dernière analyse `DONE`.
- [ ] `GET .../jurisprudence-checks` → 200 `[]` si aucune référence.
- [ ] `GET .../jurisprudence-checks` → 401 sans token.
- [ ] `GET .../jurisprudence-checks` → 404 si `caseFileId` d'un autre workspace.

### Isolation workspace

- [x] Applicable — un utilisateur du workspace A ne peut pas lire les `jurisprudence_checks` d'un dossier du workspace B (404 camouflage). Test IT dédié.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale au sens du tableau** — mais **le pipeline IA est touché** : nouveau hook de post-traitement dans `CaseAnalysisService.finalizeCaseAnalysis`. Listé ci-dessous.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `CaseAnalysisService.finalizeCaseAnalysis` | Nouveau hook fail-open ajouté après `sourceExplanationGenerator`. Une exception du hook ne doit PAS faire échouer l'analyse. | Test : hook qui lève une exception → `CaseAnalysis` reste `DONE`. |
| `EnrichedAnalysisService` | Si ce service finalise aussi des `CaseAnalysis`, le hook y est ajouté symétriquement. | Vérification au dev ; test IT analyse enrichie si applicable. |
| Coût IA pipeline | +1 appel Sonnet par dossier. | Documenté ci-dessous. |

### Coût IA estimé

+1 appel Sonnet par analyse de dossier : prompt = liste candidats regex + extraits documents bornés (~6000 car/doc × N docs, plafonné), output JSON court. System prompt mis en cache (F-142-04). Estimation : **~0,03-0,07 €/dossier** pour la vérification (sans web search). Web search (SF-179-02) ajoute un coût marginal rare. Total cible F-179 ≤ 0,10 €/dossier — négligeable vs ARR ~2 300 €/an/client. Pas de nouveau gate de plan : le coût est absorbé dans le coût d'analyse existant.

### Smoke tests E2E concernés

- [x] Aucun smoke test `e2e/smoke/` directement concerné — F-179 n'ajoute ni route ni guard ni changement d'auth/workspace. Le hook est un post-traitement fail-open. Les tests backend (`mvnw verify`) couvrent la non-régression du pipeline.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. SF-179-01 démarre en premier.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- Le service est **fail-open** strict : pattern identique à `procedureCheckService` / `sourceExplanationGenerator` / `caseDeadlineService` dans `finalizeCaseAnalysis`. Une exception ne casse jamais l'analyse.
- **Prompt unique** : extraction (complément Claude) + vérification fusionnées dans un seul appel Sonnet pour maîtriser le coût (1 appel, pas 2). Les candidats regex sont fournis comme indices ; Sonnet peut en ajouter et statuer.
- `web_search_used` et `source_url` existent en table dès SF-179-01 (la migration 245 porte le schéma complet) ; SF-179-02 ne fait que les renseigner — pas de nouvelle migration en SF-179-02.
- Statut `UNCERTAIN` est le **défaut de sécurité** : toute incertitude (statut inconnu, confiance basse non vérifiable) y bascule plutôt que vers `NOT_FOUND`.
