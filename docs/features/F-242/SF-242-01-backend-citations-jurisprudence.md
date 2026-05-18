# Mini-spec — F-242 / SF-242-01 — Backend : citations de jurisprudence d'appui + injection dans le générateur de conclusions

> Cadrages amont : `SF-242-00-coherence.md` (étape 0 — verdict GO) + `SF-242-00b-ux-coherence.md` (étape 0 bis — GO avec ajustements). Option technique retenue : **option δ** de `PRODUCT_SPEC.md` (citation manuelle structurée — référence + portée, pas le texte intégral).

## Identifiant
`F-242 / SF-242-01`

## Feature parente
`F-242` — Citation jurispru structurée + enrichissement conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
`feat/SF-242-01-backend-citations-jurisprudence`

---

## Objectif
Persister les citations de jurisprudence d'appui saisies par l'avocat (rattachées à un point juridique d'un dossier) et les injecter dans le prompt de génération des conclusions F-98, avec une garde anti-hallucination.

---

## Comportement attendu

### Cas nominal
1. L'avocat (frontend SF-242-02) saisit, pour un point juridique de la synthèse, une référence de jurisprudence (« Cass. soc. 12 oct. 2022, n° 21-12345 ») + une ligne de portée.
2. Le backend persiste la citation, rattachée au dossier et au point juridique (index + snapshot du texte).
3. À la génération de conclusions (F-98), `CaseConclusionPromptBuilder` ajoute une section `=== JURISPRUDENCE À L'APPUI ===` au message utilisateur, regroupant les citations par point juridique ; le prompt système porte une garde : **n'invente aucune référence de jurisprudence**.
4. Toute création / modification / suppression de citation rend les versions de conclusions déjà générées `stale` (réutilise SF-98-53).

### Contrat API

Toutes les routes sont sous le dossier et soumises à l'isolation workspace.

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| `GET` | `/api/v1/case-files/{caseFileId}/jurisprudence-citations` | — | `200` `{ "citations": [JurisprudenceCitationResponse] }` |
| `POST` | `/api/v1/case-files/{caseFileId}/jurisprudence-citations` | `CreateJurisprudenceCitationRequest` | `201` `JurisprudenceCitationResponse` |
| `PUT` | `/api/v1/case-files/{caseFileId}/jurisprudence-citations/{citationId}` | `UpdateJurisprudenceCitationRequest` | `200` `JurisprudenceCitationResponse` |
| `DELETE` | `/api/v1/case-files/{caseFileId}/jurisprudence-citations/{citationId}` | — | `204` |

```
CreateJurisprudenceCitationRequest {
  pointJuridiqueIndex : int      // index du point juridique dans la synthèse, ≥ 0, requis
  pointJuridiqueTexte : string   // snapshot du texte du point juridique, requis, ≤ 2000
  reference           : string   // ex. "Cass. soc. 12 oct. 2022, n° 21-12345", requis, ≤ 255
  portee              : string?  // une ligne de portée, optionnel, ≤ 500
}
UpdateJurisprudenceCitationRequest {
  reference : string             // requis, ≤ 255
  portee    : string?            // optionnel, ≤ 500
}
JurisprudenceCitationResponse {
  id                  : UUID
  pointJuridiqueIndex : int
  pointJuridiqueTexte : string
  reference           : string
  portee              : string | null
  createdAt           : ISO-8601
  updatedAt           : ISO-8601
}
```

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|---|---|---|
| `reference` absente / vide / > 255 | Message explicite | `400` |
| `portee` > 500 | Message explicite | `400` |
| `pointJuridiqueIndex` absent / < 0, ou `pointJuridiqueTexte` absent / vide | Message explicite | `400` |
| Dossier inexistant | « Case file not found » | `404` |
| Citation inexistante | « Citation not found » | `404` |
| Dossier d'un autre workspace | Traité comme inexistant (pas de fuite) | `404` |

---

## Analyse de cohérence transversale

- [x] **Outil décisionnel ?** Non — F-242 est une feature d'**enrichissement / saisie** (niveau 0, comme F-241). Pas de gate F-IA-03, pas de `TOOL_REGISTRY`, pas de `decision_tool_visibility_rules`, pas de parité domaines obligatoire.
- [x] **Refresh dashboard F-IA-02** : non concerné — une citation n'alimente aucune card du tableau de bord décisionnel.
- [x] **Workspace context** : les citations sont des données de dossier, **isolées par workspace** (mêmes contrôles que `case_files` / `case_conclusions`). Listé comme préoccupation transversale → cf. critères CA6.
- [x] **Auth / Principal, Plans / limites, Navigation / routing** : non concernés (aucun nouveau type d'auth, aucun quota, aucune route frontend nouvelle — la route est une route API additive).
- [x] **Ajout additif** : nouvelle table, nouveau contrôleur, modification ciblée de `CaseConclusionPromptBuilder` / `ConclusionPromptInput` et du calcul `stale` (SF-98-53). Aucune logique d'outil décisionnel touchée.

---

## Conformité F-IA-04
- [x] **Non applicable** — F-242 n'est pas un outil décisionnel.

---

## Critères d'acceptation
- [ ] **CA1** — table `case_jurisprudence_citations` créée par migration Liquibase ; CRUD complet exposé par les 4 routes du contrat API.
- [ ] **CA2** — validation : `reference` requise (≤ 255), `portee` ≤ 500, `pointJuridiqueIndex` ≥ 0, `pointJuridiqueTexte` requis (≤ 2000) → `400` sinon.
- [ ] **CA3** — `CaseConclusionPromptBuilder.buildUserMessage` ajoute une section `=== JURISPRUDENCE À L'APPUI ===` regroupant les citations du dossier par point juridique ; section absente / « Aucune » si le dossier n'a aucune citation.
- [ ] **CA4** — `buildSystemPrompt` porte une **garde anti-hallucination transverse** : l'IA ne doit citer aucune référence de jurisprudence non fournie (par symétrie avec « n'invente aucun chiffre »). Appliquée à toutes les cellules, comme la consigne de style SF-98-47.
- [ ] **CA5** — création / modification / suppression d'une citation rend les versions de conclusions déjà générées `stale` (le calcul `stale` de SF-98-53 intègre le `updatedAt` max des citations).
- [ ] **CA6** — isolation workspace : un dossier d'un autre workspace renvoie `404` ; les citations ne fuient jamais entre workspaces.

---

## Périmètre
### Hors scope
- Tout le frontend (→ SF-242-02).
- L'import du texte intégral des arrêts, le scraping d'éditeur (option β écartée), l'extension navigateur (option γ écartée).
- L'exposition des citations dans les outils décisionnels (aval secondaire — différé, cf. cadrage étape 0).
- Le ré-ancrage des citations après ré-analyse du dossier (une ré-analyse peut décaler les index ; le snapshot `pointJuridiqueTexte` permet l'affichage, le ré-ancrage automatique est hors V1).

---

## Technique

### Tables / Migration
Nouvelle table `case_jurisprudence_citations` (migration Liquibase — prochain numéro disponible) :

| Colonne | Type | Contrainte |
|---|---|---|
| `id` | UUID | PK |
| `case_file_id` | UUID | FK → `case_files(id)`, not null |
| `workspace_id` | UUID | not null (isolation directe) |
| `point_juridique_index` | int | not null |
| `point_juridique_texte` | varchar(2000) | not null |
| `reference` | varchar(255) | not null |
| `portee` | varchar(500) | null |
| `created_at` | timestamp | not null |
| `updated_at` | timestamp | not null |

Index sur `case_file_id`.

### Composants
- Backend (neufs) : `JurisprudenceCitation` (entité JPA), `JurisprudenceCitationRepository`, `JurisprudenceCitationService`, `JurisprudenceCitationController`, DTOs requêtes/réponse.
- Backend (modifiés) : `ConclusionPromptInput` (nouveau champ `List<JurisprudenceCitationForPrompt>`), `CaseConclusionPromptBuilder` (section + garde), le service qui assemble l'input de génération (chargement des citations), le calcul `stale` de la lecture des conclusions (SF-98-53).
- Frontend : aucun.

### Contrat API
Figé ci-dessus — consommé par SF-242-02 (parallélisable).

---

## Plan de test
### Backend (UT + IT)
- [ ] `JurisprudenceCitationControllerIT` : CRUD complet, validations `400`, dossier/citation inexistants `404`, isolation workspace `404`.
- [ ] `CaseConclusionPromptBuilderTest` : la section `=== JURISPRUDENCE À L'APPUI ===` apparaît avec les citations groupées par point juridique ; la garde anti-hallucination est présente dans le prompt système ; sans citation, comportement inchangé.
- [ ] Test du calcul `stale` : une citation créée/modifiée après la génération d'une version la marque `stale`.
### Isolation workspace
- [ ] Couverte par `JurisprudenceCitationControllerIT` (CA6).

---

## Analyse d'impact
- [x] Préoccupation transversale **Workspace context** cochée — composants impactés listés (CA6 + contrôleur). Aucune autre préoccupation.
- [x] Smoke tests E2E : aucun concerné (additif backend ; aucun parcours auth/workspace/navigation modifié).

## Dépendances
- F-98 (Terminée) — `CaseConclusionPromptBuilder`, `ConclusionPromptInput`, mécanisme `stale` SF-98-53.
- F-241 (Terminée) — amont fonctionnel (l'avocat trouve les arrêts via les deeplinks).

## Notes et décisions
- Option δ retenue (citation manuelle structurée) : friction faible, risque très faible, pas d'API tierce — cf. arbitrage des 4 options dans `PRODUCT_SPEC.md` F-242.
- La garde anti-hallucination comble une faille du générateur actuel : aujourd'hui aucune consigne n'interdit à l'IA d'inventer des arrêts (seul « n'invente aucun chiffre » existe).
