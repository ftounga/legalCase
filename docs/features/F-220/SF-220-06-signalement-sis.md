# Mini-spec — F-220 / SF-220-06 — Outil signalement SIS (Règl. UE 1860/2018 / CESEDA L.312-3)

## Identifiant
`F-220 / SF-220-06` — tool_id `F-IM-52-signalement-sis-fr` (Immigration FR)
- slug : `signalement-sis`
- statut : `ready`
- date : 2026-06-03
- branche Git : `feat/SF-220-06-signalement-sis`

## Objectif (1 phrase)
Analyser un signalement aux fins de non-admission dans le Système d'information Schengen (SIS) — qui bloque l'entrée dans l'espace Schengen même avec un titre valide — et identifier les voies de contestation / radiation.

## Périmètre / anti-doublon
Distinct de `F-IM-20` (mesures d'éloignement : expulsion / IRTF / IAT) : l'IRTF est une mesure nationale d'interdiction de retour, tandis que le **signalement SIS** est l'inscription dans la base Schengen qui en découle ou qui existe indépendamment (signalement par un autre État membre). L'outil traite le **signalement lui-même** (sa contestation / radiation), non l'IRTF. Situation à fréquence croissante (refoulements aéroport).

## Comportement (branches, branche `default`)
- **POST** `/api/v1/case-files/{caseFileId}/signalement-sis-analysis`
- Body `SignalementSisAnalyzeRequest` : `signalementConnu` (bool), `etatSignalant` (enum `FRANCE` | `AUTRE_ETAT_MEMBRE` | `INCONNU`), `motifSignalement` (enum `IRTF` | `MESURE_ELOIGNEMENT_ETRANGERE` | `MENACE_ORDRE_PUBLIC` | `AUTRE`), `titreSejourValide` (bool), `dateSignalement` (LocalDate, nullable).
- Logique (annotée « à vérifier par avocat ») :
  - si `etatSignalant=FRANCE` → contestation devant l'autorité française + recours administratif/contentieux ; radiation liée à l'effacement de la mesure sous-jacente.
  - si `etatSignalant=AUTRE_ETAT_MEMBRE` → la radiation relève de l'**État signalant** ; orienter vers la procédure de droit d'accès/rectification (autorité de contrôle / point de contact national) ; un titre français valide peut justifier une **consultation** entre États avant non-admission.
  - `titreSejourValide=true` + signalement étranger → souligner le conflit (titre valide vs non-admission) et la procédure de consultation.
- Verdict enum `actionPossible` : `RADIATION_AUTORITE_FR` / `RADIATION_ETAT_SIGNALANT` / `DROIT_ACCES_RECTIFICATION` / `CONSULTATION_ENTRE_ETATS` / `INDETERMINE`.
- Output : `actionPossible` + `demarches` (string[]) + `autoriteCompetente` (string) + `basesJuridiques` (string[]) + `messages` (string[]). Persisté 1:1 dans `signalement_sis_analyses`.
- **GET** `/api/v1/case-files/{caseFileId}/signalement-sis-analysis` → 200 ou 404.

## Cas d'erreur
| Situation | Comportement |
|---|---|
| Gate : `case_file.country` ≠ FRANCE | 400 Bad Request |
| Gate : domaine ≠ DROIT_IMMIGRATION | 400 Bad Request |
| `etatSignalant` ou `motifSignalement` hors enum | 400 Bad Request (validation) |
| GET sans POST / autre workspace | 404 (isolation workspace) |

## Source juridique (à vérifier par avocat)
- **Règlement (UE) 2018/1860** du 28/11/2018 (utilisation du SIS aux fins de retour) (à vérifier par avocat).
- **CESEDA L.312-3** (non-admission / signalement) (à vérifier par avocat).
- Droit d'accès et de rectification des données SIS (à vérifier par avocat).

## Champs IA à extraire (`ImmigrationExtractedData`)
| Champ | Type | Extension |
|---|---|---|
| `sisSignalementConnu` (proxy `signalementConnu`) | bool | Extension record + prompt Immigration |
| `sisEtatSignalant` | texte | Extension record + prompt |
| `sisMotifSignalement` | texte | Extension record + prompt |
| `sisTitreSejourValide` | bool | Extension record + prompt |

**Flag pivot CONTEXTUAL** : `signalement_sis_detecte` (niveau 2, FR-only).

## Critères d'acceptation
- [ ] Les valeurs de `actionPossible` couvertes selon `etatSignalant`.
- [ ] `etatSignalant=AUTRE_ETAT_MEMBRE` ⇒ orientation vers État signalant / droit d'accès (pas de radiation FR directe).
- [ ] `titreSejourValide=true` + signalement étranger ⇒ message conflit + consultation entre États.
- [ ] Distinction explicite avec l'IRTF (F-IM-20) dans les messages.
- [ ] Gate 400 si country≠FRANCE ou domaine≠DROIT_IMMIGRATION.
- [ ] Isolation workspace testée (404 cross-workspace).
- [ ] Tous les champs saisissables pré-remplis par l'IA (F-246), sauf non factualisable.
- [ ] `F-IM-52-signalement-sis-fr` dans `KNOWN_FRONTEND_TOOL_IDS` + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- [ ] Seed : `layer=CONTEXTUAL`, `trigger_field=signalement_sis_detecte` (jamais ALWAYS_ON).

## Plan de test
- **UT** `SignalementSisAnalyzerTest` : ≥ 6 cas (signalant FR, autre État, titre valide vs invalide, par motif, consultation).
- **IT** `SignalementSisControllerIT` : 200 + 400 gate country + 400 gate domaine + 400 validation + 404 isolation workspace.
- **Jest** `signalement-sis-section.component.spec` : rendu form + verdict + bouton désactivé si vide + flush jurisprudence-citations + `getPrefillCount` parité (F-237).

## Tables / endpoints / composants
- Backend : migration `signalement_sis_analyses` (à pré-assigner) + entité + repo + `SignalementSisAnalyzer` + `SignalementSisController`.
- Frontend : `signalement-sis-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-52-signalement-sis-fr` + `decision_tool_visibility_rules` + `KNOWN_NO_DASHBOARD_TILE_IDS` + `KNOWN_FRONTEND_TOOL_IDS`.
- Champs IA (`ImmigrationExtractedData`) : `sisSignalementConnu`, `sisEtatSignalant`, `sisMotifSignalement`, `sisTitreSejourValide` + flag `signalementSisDetecte` — étendre record + prompt Immigration.

## Invariants
- **CONTEXTUAL** (`signalement_sis_detecte`), jamais ALWAYS_ON.
- **Pré-fill IA F-246** sur tous les champs + F-IA-03 sur le pivot.
- Instrumentation visibility + `KNOWN_NO_DASHBOARD_TILE_IDS` ([[feedback_pre_merge_visibility_seed_check]]).
- **1 outil = 1 situation** : contestation/radiation du signalement SIS, distinct de l'IRTF (F-IM-20).

## Hors périmètre
- IRTF / expulsion / IAT (F-IM-20).
- Rédaction du recours (générateur d'actes F-IM-06 en aval).
- Procédures internes d'un autre État membre (renvoi, pas de traitement).
