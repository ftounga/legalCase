# Mini-spec — F-98 / SF-98-52 — Versions de conclusions (brouillon / validé / déposé + historique)

> Cadrages amont : `SF-98-00-coherence.md` (étape 0, invariant 7 « versions explicites ») + `SF-98-00b-ux-coherence.md` (étape 0 bis — invariant : « toute capacité F-98 ultérieure s'intègre dans la section conclusions »). Pas de nouveau cadrage écran : SF-98-00b anticipe explicitement cette SF.

## Identifiant
`F-98 / SF-98-52`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branches Git (dev parallélisé)
- `feat/SF-98-52-backend-versions`
- `feat/SF-98-52-frontend-versions`

---

## Objectif
Permettre plusieurs **versions** de conclusions par dossier, chacune portant un cycle de vie **brouillon → validé → déposé**, et conserver l'historique des versions générées.

---

## Comportement attendu

### Cas nominal
1. La table `case_conclusions` passe de 1:1 à **1:N** par dossier : chaque génération produit une nouvelle version (`version_number` incrémental).
2. Chaque version porte un `lifecycle_status` : `DRAFT` (brouillon) / `VALIDATED` (validé) / `DEPOSITED` (déposé). À la création : `DRAFT`.
3. `POST .../conclusions/generate` crée désormais une **nouvelle version** (`version_number` = max + 1, `status=PENDING`, `lifecycle_status=DRAFT`) au lieu d'écraser. Le worker remplit cette version.
4. La section « Conclusions » de l'onglet Décision affiche un **sélecteur de version** (la plus récente sélectionnée par défaut), un **badge de cycle de vie**, et un contrôle pour changer le cycle de vie.
5. L'avocat peut faire évoluer le cycle de vie d'une version (brouillon → validé → déposé, et retours possibles).

### Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| Transition de cycle de vie vers une valeur inconnue | Rejet | 400 |
| Passage à `VALIDATED`/`DEPOSITED` d'une version dont la génération n'est pas `DONE` | Rejet — « Seule une version générée peut être validée ou déposée. » | 409 |
| `generate` alors qu'une version est `PENDING`/`PROCESSING` | `409 ALREADY_GENERATING` (garde SF-98-01 conservée) | 409 |
| Version / dossier inexistant ou autre workspace | Accès refusé | 404 |
| Non authentifié | Rejet | 401 |

---

## Analyse de cohérence transversale
- [x] **Outils décisionnels** : non applicable — la section conclusions n'est pas un outil décisionnel.
- [x] **Autres pays / domaines** : le versioning est transversal à F-98 — il bénéficiera automatiquement aux SF-98-02→45 (générique, pas par cellule).
- [x] **Modification d'une SF existante** : SF-98-52 **modifie le contrat de SF-98-01** (`POST generate` crée une version au lieu d'écraser ; `GET .../conclusions` renvoie désormais la version la plus récente + 2 nouveaux champs). Modification documentée, rétrocompatible côté forme du `ConclusionResponse` (champs additifs).
- [x] **Pattern partagé** : `lifecycle_status` est un concept propre à F-98 — pas de service partagé créé.

### Résultat du scan
| Cible | Applicable ? | Traitement |
|---|---|---|
| SF-98-01 (génération) | Oui | Modifiée dans cette SF — `generate` versionné, `GET` renvoie la dernière version |
| SF-98-49 (éditeur) / SF-98-50 (export) | Oui | SF suivantes — consommeront le modèle de version (éditer/exporter une version donnée) |

### Décision
- [x] Étendu à la cible applicable (SF-98-01) dans cette SF ; SF-98-49 et SF-98-50 sont des SF distinctes déjà planifiées.

---

## Conformité F-IA-04
- [x] **Non applicable** — la section conclusions est un générateur de document, pas un outil décisionnel (cf. SF-98-01).

---

## Critères d'acceptation
- [ ] **CA1** — Migration : `case_conclusions` n'a plus de contrainte d'unicité sur `case_file_id` seul ; nouvelles colonnes `version_number` (INT NOT NULL) + `lifecycle_status` (VARCHAR NOT NULL) ; unicité `(case_file_id, version_number)`. Les lignes existantes migrent en `version_number=1`, `lifecycle_status=DRAFT`.
- [ ] **CA2** — `POST .../conclusions/generate` crée une nouvelle version (`version_number` = max+1 du dossier, `DRAFT`, `PENDING`) sans toucher aux versions précédentes.
- [ ] **CA3** — `GET .../conclusions` renvoie la version au `version_number` le plus élevé, avec `versionNumber` + `lifecycleStatus` ajoutés au `ConclusionResponse`.
- [ ] **CA4** — `GET .../conclusions/versions` renvoie la liste des versions du dossier (triée version décroissante).
- [ ] **CA5** — `GET .../conclusions/versions/{versionId}` renvoie le `ConclusionResponse` complet de la version.
- [ ] **CA6** — `PATCH .../conclusions/versions/{versionId}/lifecycle` fait évoluer le cycle de vie ; `409` si la version n'est pas `DONE` et qu'on vise `VALIDATED`/`DEPOSITED` ; `400` si valeur inconnue.
- [ ] **CA7** — Isolation workspace : `404` sur toutes les routes pour un dossier/version d'un autre workspace.
- [ ] **CA8** — Frontend : la section affiche un sélecteur de version + un badge de cycle de vie ; changer de version recharge le contenu ; le contrôle de cycle de vie appelle le `PATCH`.
- [ ] **CA9** — Garde `ALREADY_GENERATING` conservée : refus si une version du dossier est `PENDING`/`PROCESSING`.

---

## Périmètre
### Hors scope
- Édition du contenu d'une version — SF-98-49.
- Export Word — SF-98-50.
- Bandeau « à régénérer » — SF-98-53.
- Suppression de versions, comparaison/diff entre versions — non prévu en V1.

---

## Valeurs initiales
| Champ | Valeur initiale | Règle |
|---|---|---|
| `version_number` | `max(version_number du dossier) + 1` | calculé au déclenchement |
| `lifecycle_status` | `DRAFT` | toujours à la création |
| `status` | `PENDING` | inchangé (SF-98-01) |

---

## Technique

### Contrat API (FIGÉ — parallélisation back/front)

| Méthode | URL | Réponses |
|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/conclusions/generate` | `202 {"status":"PENDING","versionNumber":N}` ; `409` ; `404` ; `401` |
| GET | `/api/v1/case-files/{caseFileId}/conclusions` | `200 ConclusionResponse` (version la plus récente) ; `404` ; `401` |
| GET | `/api/v1/case-files/{caseFileId}/conclusions/versions` | `200 ConclusionVersionSummary[]` ; `404` ; `401` |
| GET | `/api/v1/case-files/{caseFileId}/conclusions/versions/{versionId}` | `200 ConclusionResponse` ; `404` ; `401` |
| PATCH | `/api/v1/case-files/{caseFileId}/conclusions/versions/{versionId}/lifecycle` | body `{"lifecycleStatus":"DRAFT\|VALIDATED\|DEPOSITED"}` → `200 ConclusionResponse` ; `400` ; `409` ; `404` ; `401` |

`ConclusionResponse` (SF-98-01) **+ 2 champs** : `versionNumber` (int), `lifecycleStatus` (`DRAFT|VALIDATED|DEPOSITED`).
`ConclusionVersionSummary` : `{ id, versionNumber, lifecycleStatus, status, generatedAt, createdAt }`.

### Tables impactées
| Table | Opération |
|---|---|
| `case_conclusions` | ALTER : drop unique(case_file_id), add `version_number`, add `lifecycle_status`, add unique(case_file_id, version_number) |

### Migration Liquibase
- [x] Oui — `234-add-versioning-to-case-conclusions.xml` (rollback : drop colonnes + restore unique constraint).

### Composants
- Backend : entité `CaseConclusion` (+ 2 champs), enum `ConclusionLifecycleStatus`, `CaseConclusionRepository` (`findByCaseFileIdOrderByVersionNumberDesc`, `findFirstBy...`, `existsByCaseFileIdAndStatusIn`), `CaseConclusionController` (3 endpoints ajoutés), `CaseConclusionCommandService` (versioning du `generate` + lifecycle), `ConclusionVersionSummary` DTO.
- Frontend : `ConclusionsService` (3 méthodes ajoutées), `conclusion.model.ts` (+champs, `ConclusionVersionSummary`, `ConclusionLifecycleStatus`), `ConclusionsSectionComponent` (sélecteur de version + badge + contrôle de cycle de vie).

---

## Plan de test
### Backend
- [ ] UT `CaseConclusionCommandServiceTest` : `generate` versionné (v1 → v2 → v3), garde `ALREADY_GENERATING`, transition de cycle de vie, garde `409` non-DONE, `400` valeur inconnue.
- [ ] IT `CaseConclusionControllerIT` : les 3 nouveaux endpoints, `GET .../conclusions` = dernière version, isolation workspace `404`, `401`.
### Frontend (Jest)
- [ ] `conclusions-section.component.spec.ts` : sélecteur de version, changement de version → recharge, badge de cycle de vie, `PATCH` lifecycle.
- [ ] `conclusions.service.spec.ts` : URLs des 3 méthodes.
### Isolation workspace
- [x] Applicable — testée dans `CaseConclusionControllerIT`.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — SF additive ; modifie le contrat F-98 interne uniquement ; pas d'impact auth/workspace/plans/navigation.
- [x] Aucun smoke test E2E concerné.

---

## Dépendances
- `F-98 / SF-98-01` — **done** (PR #985 + #986).

## Notes et décisions
- `GET .../conclusions` est **conservé** (rétrocompatibilité) et renvoie la version la plus récente — évite de casser tout consommateur existant.
- Cycle de vie : transitions libres entre les 3 états (l'avocat peut revenir de `VALIDATED` à `DRAFT`), seule contrainte : la génération doit être `DONE` pour `VALIDATED`/`DEPOSITED`.
