# Mini-spec — F-220 / SF-220-01 — Outil régime tunisien (accord franco-tunisien 17/03/1988)

## Identifiant
`F-220 / SF-220-01` — tool_id `F-IM-47-regime-tunisien-fr` (Immigration FR)
- slug : `regime-tunisien`
- statut : `ready`
- date : 2026-06-03
- branche Git : `feat/SF-220-01-regime-tunisien`

## Objectif (1 phrase)
Aiguiller l'avocat sur le régime de séjour applicable à un ressortissant tunisien : droit commun CESEDA **sauf** particularités dérogatoires de l'accord franco-tunisien du 17/03/1988 (étudiant, commerçant, salarié), sans recréer un faux régime parallèle fermé.

## Périmètre / anti-doublon
Distinct de `F-IM-17-regime-algerien` (régime algérien fermé, accord 1968) : la Tunisie reste **largement renvoyée au CESEDA**, l'accord 1988 ne fait qu'apporter des particularités ponctuelles. Distinct de `F-IM-05-arbre-decisionnel-titre` (arbre généraliste) : ici, l'aiguillage est commandé par la **nationalité tunisienne**. L'outil affiche « droit commun CESEDA SAUF [particularités accord 1988] », **jamais** un régime CRA autonome cloné de l'algérien (invariant cadrage). Ne réplique pas l'arbre titre F-IM-05.

## Comportement (branches, branche `default`)
- **POST** `/api/v1/case-files/{caseFileId}/regime-tunisien-analysis`
- Body `RegimeTunisienAnalyzeRequest` : `categorie` (enum `ETUDIANT` | `COMMERCANT` | `SALARIE` | `FAMILIAL` | `AUTRE`), `dureeSejourEnvisageeMois` (int, nullable), `titreEnCours` (bool), `dejaResident` (bool).
- Logique (particularités accord 1988 — annotées « à vérifier par avocat ») :
  - `ETUDIANT` → carte « étudiant » selon art. de l'accord ; rappel des stipulations propres sur le renouvellement et la durée.
  - `COMMERCANT` → particularité notable de l'accord (catégorie spécifique tunisienne, conditions propres) ; signaler la divergence avec le CESEDA généraliste.
  - `SALARIE` → particularités de délivrance / opposabilité de la situation de l'emploi selon l'accord + protocole.
  - `FAMILIAL` / `AUTRE` → **renvoi au droit commun CESEDA** (l'accord ne déroge pas) → message « régime de droit commun, voir F-IM-05 / outils CESEDA ».
- Verdict enum `regime` : `ACCORD_1988_DEROGATOIRE` / `DROIT_COMMUN_CESEDA` / `MIXTE` (particularité ponctuelle sur un point, droit commun sinon).
- Output : `regime` + `particularitesApplicables` (string[]) + `basesJuridiques` (string[]) + `renvoiDroitCommun` (bool) + `messages` (string[]). Persisté 1:1 case_file dans `regime_tunisien_analyses`.
- **GET** `/api/v1/case-files/{caseFileId}/regime-tunisien-analysis` → 200 ou 404.

## Cas d'erreur
| Situation | Comportement |
|---|---|
| Gate : `case_file.country` ≠ FRANCE | 400 Bad Request |
| Gate : domaine ≠ DROIT_IMMIGRATION | 400 Bad Request |
| `categorie` absent / hors enum | 400 Bad Request (validation) |
| `dureeSejourEnvisageeMois` < 0 | 400 Bad Request |
| GET sans POST préalable / autre workspace | 404 (isolation workspace) |

## Source juridique (à vérifier par avocat)
- **Accord franco-tunisien du 17/03/1988 relatif au séjour et au travail** + protocole + avenants (à vérifier par avocat).
- **CESEDA** (droit commun par renvoi) (à vérifier par avocat).

## Champs IA à extraire (`ImmigrationExtractedData`)
| Champ | Type | Extension |
|---|---|---|
| `regimeTunisienCategorie` (proxy `categorie`) | texte | Extension record + prompt `LegalDomainPromptBuilder` Immigration |
| `regimeTunisienDureeSejour` | int | Extension record + prompt |
| `regimeTunisienTitreEnCours` | bool | Extension record + prompt |

**Flag pivot CONTEXTUAL** : `regime_tunisien_detecte` (niveau 2, FR-only). Trigger texte F-235 sur `nationalite=Tunisienne` (mécanique déjà prête) — préférer le rattachement à `nationalite` plutôt qu'un champ pivot dédié si le runtime F-235 l'expose ; sinon flag `regimeTunisienDetecte` dans le record.

## Critères d'acceptation
- [ ] Les 3 valeurs de `regime` couvertes + `particularitesApplicables` non vide pour `ETUDIANT`/`COMMERCANT`/`SALARIE`.
- [ ] `FAMILIAL`/`AUTRE` ⇒ `renvoiDroitCommun=true` + message renvoi CESEDA, **pas** de faux régime fermé.
- [ ] Gate 400 si country≠FRANCE ou domaine≠DROIT_IMMIGRATION.
- [ ] Isolation workspace testée (404 cross-workspace).
- [ ] Tous les champs saisissables pré-remplis par l'IA (F-246), sauf non factualisable.
- [ ] `F-IM-47-regime-tunisien-fr` dans `KNOWN_FRONTEND_TOOL_IDS` + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- [ ] Seed : `layer=CONTEXTUAL`, `trigger_field=regime_tunisien_detecte` (jamais ALWAYS_ON).

## Plan de test
- **UT** `RegimeTunisienAnalyzerTest` : ≥ 6 cas (chaque catégorie + renvoi droit commun + verdict MIXTE).
- **IT** `RegimeTunisienControllerIT` : 200 nominal + 400 gate country + 400 gate domaine + 400 validation + 404 isolation workspace.
- **Jest** `regime-tunisien-section.component.spec` : rendu form + verdict + bouton désactivé si vide + flush jurisprudence-citations + `getPrefillCount` parité runtime/static (F-237).

## Tables / endpoints / composants
- Backend : migration `regime_tunisien_analyses` (à pré-assigner) + entité + repo + `RegimeTunisienAnalyzer` + `RegimeTunisienController`.
- Frontend : `regime-tunisien-section.component` (+ .html/.scss/.spec + prefill-rules) + entrée `TOOL_REGISTRY` `F-IM-47-regime-tunisien-fr` + `decision_tool_visibility_rules` + `KNOWN_NO_DASHBOARD_TILE_IDS` + `KNOWN_FRONTEND_TOOL_IDS`.
- Champs IA (`ImmigrationExtractedData`) : `regimeTunisienCategorie`, `regimeTunisienDureeSejour`, `regimeTunisienTitreEnCours` + flag `regimeTunisienDetecte` — étendre record + prompt Immigration.

## Invariants
- **CONTEXTUAL** (`regime_tunisien_detecte` / `nationalite=Tunisienne`), jamais ALWAYS_ON.
- **Pré-fill IA F-246** sur tous les champs + F-IA-03 sur le pivot.
- Instrumentation visibility + `KNOWN_NO_DASHBOARD_TILE_IDS` ([[feedback_pre_merge_visibility_seed_check]]).
- **1 outil = 1 situation** : aiguillage nationalité tunisienne, distinct de F-IM-05 / F-IM-17.

## Hors périmètre
- Régime algérien (F-IM-17), régime marocain 1983 / sénégalais 2006 (différés signal terrain).
- Arbre titre généraliste (F-IM-05) : l'outil renvoie vers lui pour le droit commun, ne le re-cloner pas.
- Aucun régime CRA / fermé cloné de l'algérien.
