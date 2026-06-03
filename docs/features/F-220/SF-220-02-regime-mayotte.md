# Mini-spec — F-220 / SF-220-02 — Outil régime Mayotte (portée territoriale du titre)

## Identifiant
`F-220 / SF-220-02` — tool_id `F-IM-48-regime-mayotte-fr` (Immigration FR)
- slug : `regime-mayotte`
- statut : `ready`
- date : 2026-06-03
- branche Git : `feat/SF-220-02-regime-mayotte`

## Objectif (1 phrase)
Analyser la **portée territoriale** d'un titre de séjour délivré à Mayotte (non valable en métropole) et les obligations dérogatoires du régime mahorais, pour éviter à l'avocat l'erreur classique sur l'extension hexagonale du titre.

## Périmètre / anti-doublon
Distinct de `F-IM-05-arbre-decisionnel-titre` (arbre généraliste métropolitain) : ici l'objet est la **dérogation territoriale** (Ord. 2014-464, CESEDA L.832-1 et s.), pas le choix du titre. Distinct de `F-IM-08-Mayotte` (s'il s'agit d'un volet AME / social, hors catalogue décisionnel — voir cadrage). L'AME Mayotte est explicitement **écartée** (droit social connexe). Sortie centrée sur la portée territoriale + obligations spécifiques, **pas** un re-clone de l'arbre titre.

## Comportement (branches, branche `default`)
- **POST** `/api/v1/case-files/{caseFileId}/regime-mayotte-analysis`
- Body `RegimeMayotteAnalyzeRequest` : `titreDelivreAMayotte` (bool), `typeTitre` (enum `VPF` | `SALARIE` | `ETUDIANT` | `RESIDENT` | `AUTRE`), `projetDeplacementMetropole` (bool), `dateDelivrance` (LocalDate, nullable).
- Logique (annotée « à vérifier par avocat ») :
  - `titreDelivreAMayotte=true` → `porteeTerritoriale=MAYOTTE_UNIQUEMENT` ; signaler que le titre **ne vaut pas autorisation de circuler/séjourner en métropole** sans visa de circulation/régularisation spécifique.
  - `projetDeplacementMetropole=true` ET portée Mayotte → verdict `BLOCAGE_DEPLACEMENT` + démarches requises.
  - `titreDelivreAMayotte=false` → `porteeTerritoriale=DROIT_COMMUN` → renvoi droit commun.
  - obligations dérogatoires (visa territorialisé, conditions propres) listées selon `typeTitre`.
- Verdict enum `porteeTerritoriale` : `MAYOTTE_UNIQUEMENT` / `DROIT_COMMUN` ; + sous-statut `BLOCAGE_DEPLACEMENT` / `DEPLACEMENT_LIBRE`.
- Output : `porteeTerritoriale` + `obligationsSpecifiques` (string[]) + `demarchesDeplacementMetropole` (string[]) + `basesJuridiques` (string[]) + `messages` (string[]). Persisté 1:1 dans `regime_mayotte_analyses`.
- **GET** `/api/v1/case-files/{caseFileId}/regime-mayotte-analysis` → 200 ou 404.

## Cas d'erreur
| Situation | Comportement |
|---|---|
| Gate : `case_file.country` ≠ FRANCE | 400 Bad Request |
| Gate : domaine ≠ DROIT_IMMIGRATION | 400 Bad Request |
| `typeTitre` hors enum | 400 Bad Request (validation) |
| GET sans POST / autre workspace | 404 (isolation workspace) |

## Source juridique (à vérifier par avocat)
- **Ordonnance n° 2014-464 du 7 mai 2014** (régime mahorais) (à vérifier par avocat).
- **CESEDA L.832-1 et suivants** (dispositions applicables à Mayotte) (à vérifier par avocat).

## Champs IA à extraire (`ImmigrationExtractedData`)
| Champ | Type | Extension |
|---|---|---|
| `mayotteTitreDelivreAMayotte` (proxy `titreDelivreAMayotte`) | bool | Extension record + prompt Immigration |
| `mayotteTypeTitre` | texte | Extension record + prompt |
| `mayotteProjetDeplacementMetropole` | bool | Extension record + prompt |

**Flag pivot CONTEXTUAL** : `mayotte_detecte` (niveau 2, FR-only). Le trigger territoire (`extractDetectedSituations`) n'expose pas encore de champ territoire — s'appuyer sur le **trigger texte F-235** (territoire / `lieu_demande` / mention « Mayotte » dans le dossier). À défaut de détection, la card reste au catalogue (jamais ALWAYS_ON). Décision de visibilité interne à la mini-spec (cf. cadrage étape 0).

## Critères d'acceptation
- [ ] `titreDelivreAMayotte=true` ⇒ `porteeTerritoriale=MAYOTTE_UNIQUEMENT` + obligations listées.
- [ ] `projetDeplacementMetropole=true` + portée Mayotte ⇒ `BLOCAGE_DEPLACEMENT` + démarches.
- [ ] `titreDelivreAMayotte=false` ⇒ renvoi droit commun (pas de re-clone arbre titre).
- [ ] Gate 400 si country≠FRANCE ou domaine≠DROIT_IMMIGRATION.
- [ ] Isolation workspace testée (404 cross-workspace).
- [ ] Tous les champs saisissables pré-remplis par l'IA (F-246), sauf non factualisable.
- [ ] `F-IM-48-regime-mayotte-fr` dans `KNOWN_FRONTEND_TOOL_IDS` + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- [ ] Seed : `layer=CONTEXTUAL`, `trigger_field=mayotte_detecte` (jamais ALWAYS_ON).

## Plan de test
- **UT** `RegimeMayotteAnalyzerTest` : ≥ 5 cas (portée Mayotte, blocage déplacement, droit commun, par type de titre).
- **IT** `RegimeMayotteControllerIT` : 200 + 400 gate country + 400 gate domaine + 404 isolation workspace.
- **Jest** `regime-mayotte-section.component.spec` : rendu form + verdict + bouton désactivé si vide + flush jurisprudence-citations + `getPrefillCount` parité (F-237).

## Tables / endpoints / composants
- Backend : migration `regime_mayotte_analyses` (à pré-assigner) + entité + repo + `RegimeMayotteAnalyzer` + `RegimeMayotteController`.
- Frontend : `regime-mayotte-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-48-regime-mayotte-fr` + `decision_tool_visibility_rules` + `KNOWN_NO_DASHBOARD_TILE_IDS` + `KNOWN_FRONTEND_TOOL_IDS`.
- Champs IA (`ImmigrationExtractedData`) : `mayotteTitreDelivreAMayotte`, `mayotteTypeTitre`, `mayotteProjetDeplacementMetropole` + flag `mayotteDetecte` — étendre record + prompt Immigration.

## Invariants
- **CONTEXTUAL** (`mayotte_detecte` via trigger texte F-235), jamais ALWAYS_ON.
- **Pré-fill IA F-246** sur tous les champs + F-IA-03 sur le pivot.
- Instrumentation visibility + `KNOWN_NO_DASHBOARD_TILE_IDS` ([[feedback_pre_merge_visibility_seed_check]]).
- **1 outil = 1 situation** : portée territoriale du titre mahorais, distinct de F-IM-05.

## Hors périmètre
- AME Mayotte (droit social connexe, écarté du catalogue décisionnel).
- Saint-Martin / Guyane (différés signal terrain).
- Arbre titre généraliste F-IM-05 (renvoi, pas de re-clone).
- Extension du moteur trigger territoire dans `extractDetectedSituations` (V2, hors périmètre).
