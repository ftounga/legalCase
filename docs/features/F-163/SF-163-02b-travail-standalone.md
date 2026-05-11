# Mini-spec — F-163 / SF-163-02b Travail (FR+BE) en mode simulateur autonome

## Identifiant

`F-163 / SF-163-02b`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-02b-travail-standalone`

---

## Objectif

> Étendre le mode `[standaloneMode]` (pattern canonique livré par SF-163-02a sur `LicenciementSectionComponent`) à **tous les composants décisionnels Travail FR et BE** dont le calculator backend est déjà enregistré dans `SimulatorCalculatorRegistry` (SF-163-03 mergée). Ajoute leurs `tool_id` à `STANDALONE_READY_TOOL_IDS`.

---

## Périmètre des composants à refactorer

**Travail FR (20)** :
- `F-DT-07-anciennete-conges-primes` (`AncienneteSectionComponent`)
- `F-DT-09-comparateur-indemnites` (`IndemniteComparatifSectionComponent`)
- `F-DT-10-rupture-conv-validity` (`RuptureConvSectionComponent`)
- `F-DT-11-harcelement-licenciement-nul` (`HarcelementLicenciementNulSectionComponent`)
- `F-DT-12-discrimination-dommages-interets` (`DiscriminationSectionComponent`)
- `F-DT-13-licenciement-economique` (`LicenciementEconomiqueSectionComponent`)
- `F-DT-15-inaptitude` (`InaptitudeSectionComponent`)
- `F-DT-16-licenciement-nul-detection` (`LicenciementNulDetectionSectionComponent`)
- `F-DT-17-indemnite-precarite-cdd` (`IndemnitePrecariteCddSectionComponent`)
- `F-DT-18-fin-mission-interim` (`FinMissionInterimSectionComponent`)
- `F-DT-19-heures-sup` (`HeuresSupSectionComponent`)
- `F-DT-21-travail-dissimule` (`TravailDissimuleSectionComponent`)
- `F-DT-22-requalification-cdd-cdi` (`RequalificationCddCdiSectionComponent`)
- `F-DT-23-requalification-interim-cdi` (`RequalificationInterimCdiSectionComponent`)
- `F-DT-24-non-concurrence` (`NonConcurrenceSectionComponent`)
- `F-DT-26-conges-payes-indemnite` (`CongesPayesIndemniteSectionComponent`)
- `F-DT-30-protection-rp` (`ProtectionRpSectionComponent`)
- `F-DT-31-transaction` (`TransactionSectionComponent`)
- `F-DT-33-at-mp` (`AtMpSectionComponent`)
- `F-DT-34-refere-prudhomal` (`ReferePrudhomalSectionComponent`)
- `F-DT-35-contestation-are-fr` (`ContestationAreSectionComponent`)

**Travail BE (3)** :
- `F-DT-27-motif-grave-be`
- `F-DT-28-avantages-conventionnels-be`
- `F-DT-29-credit-temps-be`

**Total : ~24 composants** (compteur exact à figer pendant l'inventaire par l'agent).

**Exclus de cette SF** : `F-DT-04-fiche-prudhomale` (PDF) et tout composant `PREFILL_COUNT_ALWAYS_ZERO=true` non enregistré dans le dispatcher.

---

## Pattern canonique à appliquer (issu de SF-163-02a)

Pour chaque composant ci-dessus, **strictement identique au refactor `LicenciementSectionComponent`** :

1. `@Input() standaloneMode: boolean = false`.
2. Bannière 🧪 conditionnelle dans le template HTML (markup et style identiques à `licenciement-section`).
3. `prefillFromAi()` : early return si `standaloneMode`.
4. `coherenceAlerts` computed : `if (this.standaloneMode()) return {};` en première ligne.
5. `triggerRefresh()` non invoqué si standalone (gate sur le `next:` du POST).
6. Endpoint POST : si standalone → `/api/v1/simulators/{toolId}/calculate`, sinon endpoint case-file scoped actuel.
7. Service Angular correspondant : ajout d'une méthode `analyzeStandalone(payload)` qui POST sur le dispatcher, OU paramètre booléen sur la méthode existante.
8. Entrée `TOOL_REGISTRY` du composant : `inputs(ctx) => ({ ..., standaloneMode: ctx.standaloneMode ?? false })`.
9. `STANDALONE_READY_TOOL_IDS` étendue : ajouter chaque `tool_id` couvert par cette SF.
10. Tests Jest existants : 100% verts (mode case-file inchangé). Pour chaque composant, **3 nouveaux tests Jest minimum** : (a) bannière visible si standalone, (b) endpoint switch vérifié, (c) `triggerRefresh` non invoqué.

---

## Critères d'acceptation

- [ ] **CA-01** : tous les 24 composants Travail FR+BE listés ci-dessus ont `@Input() standaloneMode: boolean = false`.
- [ ] **CA-02** : tous les 24 composants affichent la bannière 🧪 si `standaloneMode=true`.
- [ ] **CA-03** : tous les 24 composants bypassent `prefillFromAi` / `coherenceAlerts` / `triggerRefresh` si standalone.
- [ ] **CA-04** : tous les 24 composants POST sur `/api/v1/simulators/{toolId}/calculate` si standalone.
- [ ] **CA-05** : `STANDALONE_READY_TOOL_IDS` contient les 24 `tool_id` après cette SF.
- [ ] **CA-06** : 3 tests Jest nouveaux par composant minimum (= ≥ 72 tests nouveaux au total).
- [ ] **CA-07** : tous les tests Jest existants Travail FR+BE restent verts (régression 0).
- [ ] **CA-08** : `npm run build` production OK.
- [ ] **CA-09** : test manuel staging — pour 3 outils Travail FR échantillonnés, vérifier que (a) clic depuis `/simulators` ouvre `/simulators/:toolId`, (b) bannière visible, (c) POST DevTools sur dispatcher, (d) verdict affiché.

---

## Pattern test Jest (référence)

```typescript
describe('standalone mode', () => {
  beforeEach(() => { component.standaloneMode = true; fixture.detectChanges(); });
  
  it('affiche la bannière 🧪', () => {
    expect(fixture.nativeElement.querySelector('.standalone-banner')).toBeTruthy();
  });
  
  it('POST sur le dispatcher au submit', () => {
    component.onSubmit();
    const req = httpMock.expectOne(`/api/v1/simulators/${TOOL_ID}/calculate`);
    expect(req.request.method).toBe('POST');
  });
  
  it("n'invoque pas triggerRefresh", () => {
    component.onSubmit();
    httpMock.expectOne(...).flush({...});
    expect(refreshServiceSpy.triggerRefresh).not.toHaveBeenCalled();
  });
});
```

---

## Hors scope

- Refactor des composants Famille (SF-163-02c) ou Immigration (SF-163-02d).
- Refactor de Fiche prud'homale ou autres outils PDF (V2).
- Ajout d'outils Travail non enregistrés dans le dispatcher (lister à part dans le rapport agent, créer ticket si pertinent).

---

## Dépendances

- **SF-163-02a** — done (pattern canonique).
- **SF-163-03** — done (dispatcher backend pour les 24 toolIds).

---

## Notes

- Cette SF est **mécanique** : application répétée du pattern SF-163-02a sur 24 composants similaires. Risque principal = régression silencieuse sur le mode case-file existant → couverture test stricte CA-07.
- Estimation effort : ~3-4 jours.
