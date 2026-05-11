# Mini-spec — F-163 / SF-163-02d Immigration (FR+BE) en mode simulateur autonome

## Identifiant

`F-163 / SF-163-02d`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-02d-immigration-standalone`

---

## Objectif

> Étendre le mode `[standaloneMode]` à **tous les composants décisionnels Immigration FR et BE** dont le calculator backend est enregistré dans `SimulatorCalculatorRegistry` (SF-163-03 mergée). Pattern strictement identique à SF-163-02a / SF-163-02b / SF-163-02c.

---

## Périmètre des composants à refactorer

**Immigration FR (~16)** :
- `F-IM-05-arbre-decisionnel-titre` (`ImmigrationTitleDecisionSectionComponent`) — composant **canonique de référence** F-IA-04, pattern de tous les autres
- `F-IM-06-recours` (`ImmigrationRecoursSectionComponent`)
- `F-IM-07-droit-au-travail` (`DroitAuTravailSectionComponent`)
- `F-IM-08-oqtf-avec-delai-fr` (`OqtfAvecDelaiSectionComponent`)
- `F-IM-08-oqtf-sans-delai-fr` (`OqtfSansDelaiSectionComponent`)
- `F-IM-08-referes-admin-fr` (`ReferesAdminSectionComponent`)
- `F-IM-09-aes-metiers-tension`
- `F-IM-09-aes-famille`
- `F-IM-09-aes-etudiant`
- `F-IM-09-aes-humanitaire`
- `F-IM-11-changement-statut`
- `F-IM-12-asile-avance`
- `F-IM-13-naturalisation`
- `F-IM-17-regime-algerien` (CONTEXTUAL `nationalite='Algérienne'`)
- `F-IM-19-mineurs`
- `F-IM-20-mesures-eloignement`
- `F-IM-21-jld-retention-fr` (`JldRetentionSectionComponent`)
- `F-IM-22-dublin-recours-fr` (`DublinRecoursSectionComponent`)
- `F-IM-23-crrv-refus-visa-fr` (`CrrvRefusVisaSectionComponent`)
- `F-IM-24-victime-violences-l4256-fr` (`VictimeViolencesL4256SectionComponent`)

**Immigration BE (~5)** :
- `F-IM-08-annexe13-be`
- `F-IM-14-9bis-humanitaire-be`
- `F-IM-14-9ter-medical-be`
- `F-IM-14-40bis-cohabitant-ue-be`
- `F-IM-14-40ter-familial-belge-be`

**Total : ~25 composants** (compteur exact à figer pendant l'inventaire — agent vérifiera la présence dans le registry backend).

**Exclus** : wrappers info-only (`PREFILL_COUNT_ALWAYS_ZERO=true`), composants Immigration sans calculator dans le dispatcher.

---

## Pattern canonique à appliquer (issu de SF-163-02a)

**Strictement identique au refactor `LicenciementSectionComponent`** :

1. `@Input() standaloneMode: boolean = false`.
2. Bannière 🧪 conditionnelle dans le template HTML.
3. `prefillFromAi()` : early return si `standaloneMode`.
4. `coherenceAlerts` computed : `if (this.standaloneMode()) return {};`.
5. `triggerRefresh()` non invoqué si standalone.
6. Endpoint POST switch → `/api/v1/simulators/{toolId}/calculate`.
7. Service Angular : méthode `analyzeStandalone` ou paramètre boolean.
8. Entrée `TOOL_REGISTRY` : propage `standaloneMode` via `inputs(ctx)`.
9. `STANDALONE_READY_TOOL_IDS` étendue.
10. Tests Jest existants verts + ≥ 3 nouveaux par composant.

---

## Critères d'acceptation

- [ ] **CA-01** : tous les ~25 composants Immigration FR+BE éligibles ont `@Input() standaloneMode`.
- [ ] **CA-02** : bannière 🧪 affichée si standalone.
- [ ] **CA-03** : bypass `prefillFromAi` / `coherenceAlerts` / `triggerRefresh`.
- [ ] **CA-04** : POST sur dispatcher si standalone.
- [ ] **CA-05** : `STANDALONE_READY_TOOL_IDS` étendue avec tous les toolIds Immigration couverts.
- [ ] **CA-06** : ≥ 3 tests Jest nouveaux par composant (= ≥ 75 tests minimum).
- [ ] **CA-07** : tous les tests Jest existants Immigration FR+BE verts.
- [ ] **CA-08** : `npm run build` production OK.
- [ ] **CA-09** : test manuel staging échantillon 3 outils (1 FR + 1 BE + 1 régime spécial Algérien).

---

## Hors scope

- Composants Travail (SF-163-02b) ou Famille (SF-163-02c).
- Wrappers info-only.

---

## Dépendances

- **SF-163-02a** — done.
- **SF-163-03** — done.

---

## Notes

- Le composant **`ImmigrationTitleDecisionSectionComponent`** est le pattern canonique F-IA-04 de référence. Soigner particulièrement son refactor — c'est lui que les futurs nouveaux outils copieront.
- `F-IM-17-regime-algerien` est CONTEXTUAL (déclenché si `nationalite='Algérienne'`). En mode standalone, le composant doit fonctionner sans dépendance à la nationalité IA — l'avocat saisit lui-même.
- Effort estimé : ~3-4 jours (~25 composants).
