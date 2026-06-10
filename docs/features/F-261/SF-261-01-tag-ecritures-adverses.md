# Mini-spec — F-261 / SF-261-01 — Tag « écritures adverses » au niveau document

> Base : `project-governance/templates/subfeature-template.md`. Pré-requis de SF-261-02 (extraction des moyens).

## Identifiant
`F-261 / SF-261-01`

## Feature parente
`F-261` — Conclusions en réponse (programme Conclusions V2)

## Statut
`ready` (étape 0 F-261 GO + décision PO Option A ; étape 0 bis GO avec ajustements)

## Date
2026-06-10

## Branche
`feat/SF-261-01-tag-ecritures-adverses`

## Objectif
> Permettre à l'avocat de **marquer un document comme « écritures adverses »** (les conclusions de la partie adverse), pour désigner la source des moyens à extraire (SF-261-02) puis réfuter (SF-261-03).

## Comportement attendu

### Cas nominal
1. Onglet **Dossier**, table des documents : sur la ligne d'un document, l'avocat active **« Écritures adverses »** (toggle).
2. `PATCH …/documents/{documentId}/adverse-pleadings {adversePleadings:true}` → persiste `adverse_pleadings = true` sur le document. Re-cliquer le repasse à `false`.
3. Le `GET …/documents` expose `adversePleadings` par document.
4. Plusieurs documents peuvent être marqués (jeux d'écritures successifs).

### Cas d'erreur
| Situation | Comportement | HTTP |
|---|---|---|
| `adversePleadings` absent du body | erreur explicite | 400 |
| `documentId` inexistant | introuvable | 404 |
| Document d'un autre dossier / workspace | accès refusé | 404 |

## Analyse de cohérence transversale
- **vs SF-98-56 (marquage citation adverse)** : distinct — SF-98-56 marque une **citation** (`jurisprudence_checks.marked_adverse`, écran Synthèse) ; SF-261-01 marque un **document** (`documents.adverse_pleadings`, onglet Dossier). Libellés distincts, pas de fusion.
- **Consommateur** : SF-261-02 (extraction des moyens sur les documents marqués). Aucun autre consommateur pour l'instant.
- **Pays/domaines** : agnostique (flag pur sur `documents`).

### Résultat du scan
| Cible | Applicable | Traitement |
|---|---|---|
| SF-261-02 (extraction moyens) | Oui | Consommateur aval (SF suivante) |
| SF-98-56 (citation adverse) | Non | Mécanisme distinct (citation vs document) — pas de fusion |
| Autres écrans/outils | Non | flag document isolé |

### Décision
- [x] Étendu à toutes les cibles applicables (flag document, agnostique domaine).
- [x] Distinct de SF-98-56 (citation) — pas de doublon.

## Conformité F-IA-04
- [x] **Non applicable** — pas de composant décisionnel/`TOOL_REGISTRY` ; action sur la table des documents.

## Champs IA à extraire
- [x] **Aucun pré-remplissage** — le marquage est un jugement humain (l'avocat sait quel document est l'acte adverse), pas une extraction IA.

## Critères d'acceptation
- [ ] Toggle « Écritures adverses » présent sur chaque ligne de la table des documents (onglet Dossier).
- [ ] Activer/désactiver persiste `adverse_pleadings` (vérifié après reload).
- [ ] `GET …/documents` expose `adversePleadings`.
- [ ] Isolation workspace : un utilisateur du workspace A ne peut pas marquer un document du workspace B (404).
- [ ] Libellé distinct du marquage de citation adverse (SF-98-56) ; mention sobre de la finalité (réfutation à venir).
- [ ] Plusieurs documents marquables simultanément.

## Périmètre
### Hors scope
- **Extraction des moyens** (SF-261-02) et **réfutation** (SF-261-03).
- Notion de camp généralisée (CLIENT/ADVERSE/NEUTRE) — un **booléen ciblé « écritures adverses »** suffit au besoin de F-261 ; généralisation = backlog si besoin.

## Valeurs initiales
| Champ | Valeur | Règle |
|---|---|---|
| `documents.adverse_pleadings` | `false` | tout document naît non marqué |

## Technique
### Endpoints
| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| PATCH | `/api/v1/case-files/{caseFileId}/documents/{documentId}/adverse-pleadings` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/documents` (existant — expose `adversePleadings`) | Oui | MEMBER |

Body PATCH : `{ "adversePleadings": boolean }` → 200 `DocumentResponse` à jour.

### Tables
| Table | Opération | Notes |
|---|---|---|
| `documents` | ALTER (+`adverse_pleadings` BOOLEAN NOT NULL DEFAULT false) + UPDATE + SELECT | — |

### Migration Liquibase
- [x] Oui — `599-add-adverse-pleadings-to-documents.xml` (numéro à confirmer = prochain libre ; UUID pré-assigné). `adverse_pleadings BOOLEAN NOT NULL DEFAULT false`, rollback drop.

### Backend
- `Document` (+ `adversePleadings`), `DocumentResponse` (+ `adversePleadings`).
- `DocumentController` : + `@PatchMapping(".../{documentId}/adverse-pleadings")` (délègue ; isolation workspace, pattern existant du contrôleur).
- Service documents : `markAdversePleadings(caseFileId, documentId, value, principal)`.

### Composants Angular
- `case-file-detail` table documents : toggle « Écritures adverses » + tooltip de finalité. `markForCheck()`.
- Service documents : `markAdversePleadings(caseFileId, documentId, value)`.
- Modèle TS document : `adversePleadings`.

## Plan de test
### Unitaires (backend)
- [ ] Service : marquage true/false persiste ; 404 cross-workspace ; 400 body absent.
### Intégration
- [ ] `PATCH …/adverse-pleadings` 200 / 400 / 404 ; `GET …/documents` expose `adversePleadings`.
### Frontend (Jest)
- [ ] Toggle rendu ; clic appelle le service ; état mis à jour ; tooltip présent.
### Isolation workspace
- [x] Applicable.

## Analyse d'impact
- [x] **Aucune préoccupation transversale** (pas d'auth/workspace nouveau/plan/navigation ; isolation réutilise le pattern `DocumentController`).
### Smoke E2E
- [ ] Aucun smoke concerné.

## Dépendances
- **Bloque** SF-261-02 (extraction des moyens) puis SF-261-03 (réfutation).

## Notes
- Booléen ciblé `adverse_pleadings` (pas un enum de camp) : précis pour le besoin F-261, extensible plus tard.
- Marquage = jugement humain (l'avocat désigne l'acte adverse) — invariant anti-contresois cohérent avec SF-98-56.
