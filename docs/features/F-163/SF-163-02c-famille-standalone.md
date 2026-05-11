# Mini-spec — F-163 / SF-163-02c Famille (FR+BE) en mode simulateur autonome

## Identifiant

`F-163 / SF-163-02c`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-02c-famille-standalone`

---

## Objectif

> Étendre le mode `[standaloneMode]` à **tous les composants décisionnels Famille FR et BE** dont le calculator backend est enregistré dans `SimulatorCalculatorRegistry` (SF-163-03 mergée). Pattern strictement identique à SF-163-02a / SF-163-02b.

---

## Périmètre des composants à refactorer

**Famille FR (~9)** :
- `F-FA-05-partage-immobilier` (`PartageImmobilierSectionComponent`)
- `F-FA-08-divorce-alteration` (`DivorceAlterationSectionComponent`)
- `F-FA-09-divorce-faute` (`DivorceFauteSectionComponent`)
- `F-FA-10-divorce-accepte` (`DivorceAccepteSectionComponent`)
- `F-FA-13-revisions-post-divorce` (`RevisionsPostDivorceSectionComponent`)
- `F-FA-14-ordonnance-protection` (`OrdonnanceProtectionSectionComponent`)
- `F-FA-15-recompenses` (`RecompensesSectionComponent`)
- `F-FA-19-autorite-parentale` (`AutoriteParentaleSectionComponent`)
- `F-FA-19-changement-residence` (`ChangementResidenceSectionComponent`)
- `F-FA-19-desaccords-parentaux` (`DesaccordsParentauxSectionComponent`)
- `F-FA-21-separation-corps` (`SeparationCorpsSectionComponent`)
- `F-FA-24-rapport-succession` (`RapportSuccessionSectionComponent`)
- `F-FA-24-reserve-heriditaire` (`ReserveHeriditaireSectionComponent`)
- `F-FA-26-changement-etat-civil` (`ChangementEtatCivilSectionComponent`)

**Famille BE (1)** :
- `F-FA-11-desunion-irremediable-be`

**Total : ~15 composants** (compteur exact à figer pendant l'inventaire — agent vérifiera la présence dans le registry backend).

**Exclus** : wrappers info-only `F-FA-01-prestation-compensatoire`, `F-FA-02-pension-alimentaire`, `F-FA-04-liquidation-communaute`, `F-152-divorce-consentement-scoring`, `F-153-fourchettes-jaf` (`PREFILL_COUNT_ALWAYS_ZERO=true` — pas de calculator à invoquer). Les wrappers PDF Famille BE (`F-211 SF wrappers`) ne sont pas dans le périmètre. `F-FA-06-calendrier-garde` et `F-FA-07-checklist-divorce` : à vérifier — si dans le registry, à inclure ; sinon hors scope.

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

- [ ] **CA-01** : tous les composants Famille FR+BE éligibles ont `@Input() standaloneMode`.
- [ ] **CA-02** : bannière 🧪 affichée si standalone.
- [ ] **CA-03** : bypass `prefillFromAi` / `coherenceAlerts` / `triggerRefresh`.
- [ ] **CA-04** : POST sur dispatcher si standalone.
- [ ] **CA-05** : `STANDALONE_READY_TOOL_IDS` étendue avec tous les toolIds Famille couverts.
- [ ] **CA-06** : ≥ 3 tests Jest nouveaux par composant (= ≥ 45 tests minimum).
- [ ] **CA-07** : tous les tests Jest existants Famille FR+BE verts.
- [ ] **CA-08** : `npm run build` production OK.
- [ ] **CA-09** : test manuel staging échantillon 3 outils.

---

## Hors scope

- Composants Travail (SF-163-02b) ou Immigration (SF-163-02d).
- Wrappers info-only (`PREFILL_COUNT_ALWAYS_ZERO=true`).
- Wrappers Famille BE F-211 (Divorce DC/DDI/Tribunal/Pacte successoral) — leurs calculators ne sont pas dans le dispatcher V1.

---

## Dépendances

- **SF-163-02a** — done.
- **SF-163-03** — done.

---

## Notes

- Mécanique identique SF-163-02b. Effort estimé : ~2-3 jours (15 composants).
- Particularité Famille : plusieurs composants ont des structures de données plus complexes (ex. partage immobilier avec liste de biens) — l'agent doit vérifier que le POST standalone fonctionne bien avec ces payloads (le dispatcher backend SF-163-03 accepte les mêmes payloads que case-file scoped, donc OK normalement).
