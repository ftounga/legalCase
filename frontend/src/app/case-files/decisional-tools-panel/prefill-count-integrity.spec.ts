/**
 * F-236 SF-236-05 + F-237 SF-237-01 — Garde-fou CI `prefill-count-integrity`.
 *
 * Vérifie que **chaque** entrée du `TOOL_REGISTRY` du panel décisionnel
 * (F-IA-04) expose une méthode statique `getPrefillCount(input)` conforme au
 * contrat F-177 SF-177-12 / F-236 SF-236-01 :
 *
 *  - la propriété statique `getPrefillCount` existe sur la classe du composant ;
 *  - c'est une fonction (typeof === 'function') ;
 *  - appelée avec `{}` (input vide), elle retourne un nombre fini, non-NaN,
 *    non-négatif.
 *
 * **F-237 SF-237-01 — Extension parité runtime/static** : en plus du check
 * SF-236-05 (présence + signature), ce fichier vérifie que le `static
 * getPrefillCount` du composant **s'aligne comportementalement** avec le
 * helper partagé `<ComponentName>PrefillRules.computePrefillCount` co-localisé
 * dans le dossier du composant. Pour chaque entrée TOOL_REGISTRY non tolérée :
 *
 *  - le fichier helper `<component>-prefill-rules.ts` doit exister à côté du
 *    composant (convention F-236 SF-236-01 §1.1) ;
 *  - il doit exporter un objet `*PrefillRules` avec une fonction
 *    `computePrefillCount(input): number` ;
 *  - sur une batterie de fixtures canoniques (`{}`, `{aiData: null}`, etc.),
 *    le static et le helper doivent retourner exactement la même valeur.
 *
 * Le check est **comportemental** (par fixture) plutôt que de stricte identité
 * de référence (`Component.getPrefillCount === Helper.computePrefillCount`)
 * parce que sur master post-SF-236, les 89 statics délèguent au helper via un
 * wrapper léger (`return Helper.computePrefillCount({ aiData: input.aiData })`),
 * et non par exposition directe du pointeur. La parité comportementale capture
 * le même invariant : si un static dérive du helper (logique copiée, return
 * constant injecté, …), la batterie de fixtures fait apparaître la divergence.
 *
 * **Motivation produit** (mini-specs SF-236-05 + F-237) : sans ce static, le
 * panel F-IA-04 affiche silencieusement un badge faux ou absent
 * (`auto_awesome +N`), ce qui invalide la promesse UX "outils décisionnels
 * assistés par l'IA". Le helper `getToolPrefillCount()` du contract retourne
 * `null` quand le static manque → le panel masque le badge sans alerte → bug
 * produit silencieux. Sans le check parité F-237 SF-237-01, un agent peut
 * faire mentir le compteur (`getPrefillCount` retournant 0 pendant que
 * `prefillFromAi()` remplit N champs) — exactement le bug que F-236 vient de
 * corriger sur F-FA-07.
 *
 * **Pattern miroir** :
 *  - `DecisionToolVisibilityIntegrityIT` (F-164 SF-164-01) — backend Java vérifiant
 *    que tous les `tool_id` du seed `decision_tool_visibility_rules` ont une
 *    entrée frontend.
 *  - `LegalReferentialDescriptionIntegrityIT` (F-225 SF-225-03) — intégrité
 *    description + UX référentiels.
 *
 * Notre test est en Jest (frontend) car la cible est un import TypeScript dynamique
 * via `TOOL_REGISTRY`.
 *
 * **Démonstration de la sensibilité** :
 *  - SF-236-05 : retirer manuellement `static getPrefillCount` d'un composant
 *    aléatoire fait échouer ce test avec un message clair indiquant le
 *    tool_id concerné + le nom de la classe.
 *  - SF-237-01 : remplacer le corps du static par un return constant non-zéro
 *    (drift volontaire) fait échouer le check parité avec un message listant
 *    `tool_id`, classe et fixture qui a divergé.
 */
import { Type } from '@angular/core';

import { DecisionToolsPanelComponent } from './decisional-tools-panel.component';

interface FailureReport {
  toolId: string;
  componentName: string;
  reason: string;
}

interface OptionalWarning {
  toolId: string;
  componentName: string;
  missingMember: 'TOOL_LABEL' | 'TOOL_ICON';
}

describe('TOOL_REGISTRY prefill count integrity (F-236 SF-236-05)', () => {
  const registry = DecisionToolsPanelComponent.TOOL_REGISTRY;

  it('TOOL_REGISTRY is exposed and non-empty', () => {
    expect(registry).toBeDefined();
    expect(registry.size).toBeGreaterThan(0);
  });

  it('every TOOL_REGISTRY entry exposes static getPrefillCount returning a non-negative finite number', () => {
    const failures: FailureReport[] = [];

    for (const [toolId, entry] of registry.entries()) {
      const cls = entry.component as unknown as Type<unknown> & {
        getPrefillCount?: unknown;
        name?: string;
      };
      const componentName = (cls as { name?: string }).name ?? '<anonymous>';

      // (a) static getPrefillCount présent
      if (cls.getPrefillCount === undefined || cls.getPrefillCount === null) {
        failures.push({
          toolId,
          componentName,
          reason: 'missing static getPrefillCount property',
        });
        continue;
      }

      // (b) c'est bien une fonction
      if (typeof cls.getPrefillCount !== 'function') {
        failures.push({
          toolId,
          componentName,
          reason: `static getPrefillCount is not a function (typeof = ${typeof cls.getPrefillCount})`,
        });
        continue;
      }

      // (c) appel avec input vide → nombre fini ≥ 0
      let result: unknown;
      try {
        result = (cls.getPrefillCount as (input: object) => unknown)({});
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        failures.push({
          toolId,
          componentName,
          reason: `static getPrefillCount({}) threw: ${message}`,
        });
        continue;
      }

      if (typeof result !== 'number') {
        failures.push({
          toolId,
          componentName,
          reason: `static getPrefillCount({}) returned ${typeof result} (expected number), got: ${JSON.stringify(result)}`,
        });
        continue;
      }
      if (!Number.isFinite(result)) {
        failures.push({
          toolId,
          componentName,
          reason: `static getPrefillCount({}) returned non-finite number (${result})`,
        });
        continue;
      }
      if (result < 0) {
        failures.push({
          toolId,
          componentName,
          reason: `static getPrefillCount({}) returned negative number (${result})`,
        });
        continue;
      }
    }

    if (failures.length > 0) {
      const lines = failures.map(
        (f) => `  - ${f.toolId} (${f.componentName}): ${f.reason}`,
      );
      const msg = [
        `F-236 SF-236-05 invariant violated — ${failures.length} TOOL_REGISTRY entries non conformes :`,
        ...lines,
        '',
        'Chaque entrée TOOL_REGISTRY DOIT exposer `static getPrefillCount(input): number`',
        'conforme au contrat F-177 SF-177-12 / F-236 SF-236-01.',
        'Cf. docs/features/F-236/contract-prefill-rules.md.',
      ].join('\n');
      throw new Error(msg);
    }
  });

  // Bonus — vérification du contrat compagnon `TOOL_LABEL` / `TOOL_ICON`
  // (SF-177-03b/05/07). Ne bloque pas le test (mode `console.warn` seulement)
  // car ce n'est pas le scope de F-236 SF-236-05, mais utile pour audit visuel.
  it('TOOL_LABEL and TOOL_ICON are present on every TOOL_REGISTRY entry (warning only)', () => {
    const warnings: OptionalWarning[] = [];

    for (const [toolId, entry] of registry.entries()) {
      const cls = entry.component as unknown as {
        TOOL_LABEL?: unknown;
        TOOL_ICON?: unknown;
        name?: string;
      };
      const componentName = cls.name ?? '<anonymous>';

      if (typeof cls.TOOL_LABEL !== 'string' || cls.TOOL_LABEL.length === 0) {
        warnings.push({ toolId, componentName, missingMember: 'TOOL_LABEL' });
      }
      if (typeof cls.TOOL_ICON !== 'string' || cls.TOOL_ICON.length === 0) {
        warnings.push({ toolId, componentName, missingMember: 'TOOL_ICON' });
      }
    }

    if (warnings.length > 0) {
      // eslint-disable-next-line no-console
      console.warn(
        `[F-236 SF-236-05] ${warnings.length} TOOL_REGISTRY entries sans ${warnings.map(w => w.missingMember).join('/')}:\n` +
          warnings.map(w => `  - ${w.toolId} (${w.componentName}): missing ${w.missingMember}`).join('\n'),
      );
    }
    // Pas d'expect.fail — c'est un warning consultatif.
    expect(true).toBe(true);
  });
});

// ---------------------------------------------------------------------------
// F-237 SF-237-01 — Parité runtime ↔ static via helper PrefillRules.
//
// Objectif : pour chaque entrée TOOL_REGISTRY, vérifier que le static
// `getPrefillCount` du composant est **arrimé** au helper partagé
// `<ComponentName>PrefillRules.computePrefillCount` co-localisé (contrat
// F-236 SF-236-01).
//
// Forme de la vérification — **parité comportementale par fixture**.
// La cible idéale de la mini-spec est l'« identité de référence »
// (`Component.getPrefillCount === Helper.computePrefillCount`). Sur master
// post-SF-236, les 89 statics délèguent en **wrapper léger** :
//
//   static getPrefillCount(input) {
//     return XxxPrefillRules.computePrefillCount({ aiData: input.aiData });
//   }
//
// → l'identité stricte échouerait pour tous (le static est une nouvelle
// closure, pas le même pointeur). La parité comportementale capture le
// même invariant **par construction** : pour une batterie de fixtures
// canoniques, `static(F)` doit retourner exactement
// `helper.computePrefillCount(F)`. Toute dérive (logique copiée/divergente,
// fonction détachée du helper) produit une différence détectable.
//
// Périmètre de tolérance — 14 composants sans helper sur master :
//  - Étiquettes `static readonly PREFILL_COUNT_ALWAYS_ZERO = true;` à venir
//    en SF-237-02 ; en attendant, listés dans `TOLERATED_NO_HELPER` ci-bas.
//
// Garde-fou symétrique (P0) :
//  - `PREFILL_COUNT_ALWAYS_ZERO === true` ⇒ on vérifie strictement
//    `getPrefillCount({}) === 0` (cohérence de l'étiquette).
//
// **Démonstration de la sensibilité** (test du test) :
//  - Remplacer `static getPrefillCount` d'un composant aléatoire par une
//    fonction locale détachée du helper avec une logique qui diverge sur
//    `{}` (ex. retourner `1` au lieu de `0`) → le test échoue avec un message
//    listant `tool_id`, nom de classe et fixture qui a divergé.
//  - Restaurer la délégation → le test passe.
// ---------------------------------------------------------------------------

import * as path from 'path';
import * as fs from 'fs';

/**
 * Convention de dérivation `classe Angular → dossier kebab-case`.
 *
 * Règle de base (97/98 cas) : retirer le suffixe `Component`, convertir
 * la PascalCase en kebab-case.
 *
 * Exceptions (1/98) : `BelgianCohabitantUeBeSectionComponent` réside dans le
 * dossier `belgian-40bis-section` (alias historique conservé par F-DT-26 BE).
 */
const FOLDER_OVERRIDES: ReadonlyMap<string, string> = new Map([
  // Dossier alias historique conservé par F-DT-26 BE.
  ['BelgianCohabitantUeBeSectionComponent', 'belgian-40bis-section'],
  // Le mapping kebab-case standard collerait les chiffres au préfixe :
  // `Belgian9bis` → `belgian9bis-section` au lieu de `belgian-9bis-section`.
  ['Belgian9bisSectionComponent', 'belgian-9bis-section'],
  ['Belgian9terSectionComponent', 'belgian-9ter-section'],
  ['Belgian40terSectionComponent', 'belgian-40ter-section'],
  // SF-207-08b — le dossier réel insère un tiret entre `obligatoire` et `45`
  // (`outplacement-be-obligatoire-45-section`), alors que la dérivation
  // kebab-case par défaut produirait `outplacement-be-obligatoire45-section`.
  ['OutplacementBeObligatoire45SectionComponent', 'outplacement-be-obligatoire-45-section'],
  // SF-212-20 — le dossier réel insère un tiret entre `a` et `pied`
  // (`mise-a-pied-disciplinaire-section`), alors que la dérivation
  // kebab-case standard collerait `APied` → `apied`.
  ['MiseAPiedDisciplinaireSectionComponent', 'mise-a-pied-disciplinaire-section'],
  // SF-215-04 — le dossier réel insère un tiret entre `regroupement` et `10ter`
  // (`regroupement-10ter-be-section`), alors que la dérivation kebab-case
  // standard collerait `Regroupement10ter` → `regroupement10ter`. Même cas
  // de figure que Belgian9bis / Belgian40ter ci-dessus.
  ['Regroupement10terBeSectionComponent', 'regroupement-10ter-be-section'],
  // SF-215-06 — idem pour `regroupement-10bis-be-section`.
  ['Regroupement10bisBeSectionComponent', 'regroupement-10bis-be-section'],
  // SF-215-08 — idem pour `naturalisation-12bis-be-section` :
  // `Naturalisation12bis` collerait `naturalisation12bis`.
  ['Naturalisation12bisBeSectionComponent', 'naturalisation-12bis-be-section'],
  // SF-219-05b — le dossier réel insère un tiret entre `general` et `30sem`
  // (`outplacement-be-general-30sem-section`), alors que la dérivation
  // kebab-case standard collerait `General30sem` → `general30sem`. Même
  // cas de figure que Naturalisation12bis ci-dessus.
  ['OutplacementBeGeneral30semSectionComponent', 'outplacement-be-general-30sem-section'],
  // SF-219-08b — le dossier réel insère un tiret entre `cct` et `32bis`
  // (`transfert-entreprise-cct-32bis-section`), alors que la dérivation
  // kebab-case standard produirait `Cct32bis` → `cct32bis`.
  ['TransfertEntrepriseCct32bisSectionComponent', 'transfert-entreprise-cct-32bis-section'],
  // SF-219-10b — le dossier réel insère un tiret entre `cct` et `5`
  // (`delegue-syndical-cct-5-section`), alors que la dérivation
  // kebab-case standard collerait `Cct5` → `cct5`.
  ['DelegueSyndicalCct5SectionComponent', 'delegue-syndical-cct-5-section'],
  // SF-219-14b — le dossier réel insère un tiret entre `cct` et `322`
  // (`interim-be-cct-322-section`), alors que la dérivation kebab-case
  // standard collerait `Cct322` → `cct322`. Même pattern que Belgian9bis.
  ['InterimBeCct322SectionComponent', 'interim-be-cct-322-section'],
  // SF-219-16b — le dossier réel insère des tirets entre `cct`, `85` et `149`
  // (`teletravail-be-cct-85-149-section`), alors que la dérivation kebab-case
  // standard collerait `Cct85149` → `cct85149`. Même pattern que
  // InterimBeCct322 / Belgian9bis.
  ['TeletravailBeCct85149SectionComponent', 'teletravail-be-cct-85-149-section'],
  // SF-219-18b — le dossier réel insère un tiret entre `semaine` et `4`
  // (`semaine-4-jours-be-section`), alors que la dérivation kebab-case
  // standard collerait `Semaine4Jours` → `semaine4-jours-be-section`.
  // Même pattern que Belgian9bis / Naturalisation12bis.
  ['Semaine4JoursBeSectionComponent', 'semaine-4-jours-be-section'],
  // SF-221-01 : F-IM-53 prorogation carte A BE — le dossier suit la convention
  // `carte-a-prorogation-be-section`, alors que la dérivation kebab-case standard
  // collerait `CarteAProrogation` → `carte-aprorogation` (deux capitales `AP`
  // consécutives sans frontière minuscule→majuscule).
  ['CarteAProrogationBeSectionComponent', 'carte-a-prorogation-be-section'],
]);

function deriveFolderFromClassName(className: string): string {
  const override = FOLDER_OVERRIDES.get(className);
  if (override) return override;
  const base = className.replace(/Component$/, '');
  // Conversion PascalCase → kebab-case, insertion d'un `-` :
  //  - entre minuscule/digit et majuscule (`RuptureConv` → `rupture-conv`)
  //  - entre lettre et digit (`L4256` → `l-4256`) — rarement nécessaire, géré
  //    par FOLDER_OVERRIDES quand le dossier ne suit pas la règle (cas BE).
  return base.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase();
}

/**
 * F-237 SF-237-02 — Liste de tolérance VIDE.
 *
 * État post SF-237-02 (mergée 2026-05-11) :
 *  - Les 11 wrappers `count=0` (case-deadlines, jld-retention, dublin-recours,
 *    crrv-refus-visa, victime-violences-l4256, divorce-cm-scoring,
 *    fourchettes-jaf, liquidation-communaute, pension-alimentaire,
 *    prestation-compensatoire, rupture-amiable-info) portent désormais
 *    l'étiquette déclarative `static readonly PREFILL_COUNT_ALWAYS_ZERO = true;`
 *    qui les exempte automatiquement du check helper (gardé par le bloc
 *    `if (cls.PREFILL_COUNT_ALWAYS_ZERO === true) continue;` ci-dessous).
 *  - Les 3 statics qui contenaient de la logique inline (divorce-faute-section,
 *    transaction-section, travail-procedure-section) ont leur helper
 *    extrait : `<component>-prefill-rules.ts` + `<component>-prefill-rules.spec.ts`.
 *
 * La règle est donc strictement appliquée à 100 % des composants TOOL_REGISTRY
 * non étiquetés `PREFILL_COUNT_ALWAYS_ZERO`. Aucune dérogation tolérée.
 *
 * **REFUS** : tout ajout futur dans ce set sans extraction du helper
 * correspondant doit être bloqué en review.
 */
const TOLERATED_NO_HELPER: ReadonlySet<string> = new Set<string>();

/**
 * Batterie de fixtures canoniques utilisées pour la parité comportementale.
 *
 * Chaque fixture est un `PrefillCountInput` partiel. Pour chaque composant
 * et chaque fixture F : `static.getPrefillCount(F)` doit retourner exactement
 * `helper.computePrefillCount(F)`. Toute divergence ⇒ FAIL.
 *
 * Choix : fixtures **agnostiques au domaine** (champs `aiData` vides ou
 * placeholders non métier) → font la jonction sur le contrat top-level
 * sans casser la sémantique pays/domaine. Couvre les 99% de cas de drift
 * pratique (static détaché, copie locale dérivante, return constant injecté).
 */
const PARITY_FIXTURES: ReadonlyArray<{ label: string; input: Record<string, unknown> }> = [
  { label: 'empty {}', input: {} },
  { label: 'aiData null', input: { aiData: null } },
  { label: 'aiData undefined + arrays empty', input: { aiData: undefined, procedureChecks: [], aiQuestions: [], piecesManquantes: [], triggerEvents: [] } },
  { label: 'aiData {}', input: { aiData: {} } },
  { label: 'workspaceCountry FRANCE no aiData', input: { workspaceCountry: 'FRANCE' } },
  { label: 'workspaceCountry BELGIQUE no aiData', input: { workspaceCountry: 'BELGIQUE' } },
];

interface ParityFailure {
  toolId: string;
  componentName: string;
  reason: string;
}

describe('F-237 SF-237-01 — parité runtime/static via helper PrefillRules', () => {
  const registry = DecisionToolsPanelComponent.TOOL_REGISTRY;
  // Racine relative au fichier de spec, qui réside dans
  // `frontend/src/app/case-files/decisional-tools-panel/`.
  const caseFilesRoot = path.join(__dirname, '..');

  it('every PREFILL_COUNT_ALWAYS_ZERO === true component returns exactly 0 on empty input', () => {
    const failures: ParityFailure[] = [];

    for (const [toolId, entry] of registry.entries()) {
      const cls = entry.component as unknown as {
        PREFILL_COUNT_ALWAYS_ZERO?: unknown;
        getPrefillCount?: (input: object) => unknown;
        name?: string;
      };
      if (cls.PREFILL_COUNT_ALWAYS_ZERO !== true) continue;
      const componentName = cls.name ?? '<anonymous>';

      if (typeof cls.getPrefillCount !== 'function') {
        failures.push({ toolId, componentName, reason: 'marked PREFILL_COUNT_ALWAYS_ZERO but getPrefillCount is missing' });
        continue;
      }
      let result: unknown;
      try {
        result = cls.getPrefillCount({});
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        failures.push({ toolId, componentName, reason: `marked PREFILL_COUNT_ALWAYS_ZERO but getPrefillCount({}) threw: ${message}` });
        continue;
      }
      if (result !== 0) {
        failures.push({
          toolId,
          componentName,
          reason: `marked PREFILL_COUNT_ALWAYS_ZERO but getPrefillCount({}) returned ${JSON.stringify(result)} (expected 0)`,
        });
      }
    }

    if (failures.length > 0) {
      throw new Error(
        `F-237 SF-237-01 — PREFILL_COUNT_ALWAYS_ZERO inconsistency (${failures.length}):\n` +
          failures.map((f) => `  - ${f.toolId} (${f.componentName}): ${f.reason}`).join('\n'),
      );
    }
  });

  it('every non-tolerated TOOL_REGISTRY entry has a co-located helper PrefillRules behaviorally aligned with its static getPrefillCount', () => {
    const failures: ParityFailure[] = [];
    const checkedCount = { total: 0, withHelper: 0, tolerated: 0, exempted: 0 };

    for (const [toolId, entry] of registry.entries()) {
      checkedCount.total += 1;

      const cls = entry.component as unknown as {
        PREFILL_COUNT_ALWAYS_ZERO?: unknown;
        getPrefillCount?: (input: object) => unknown;
        name?: string;
      };
      const componentName = cls.name ?? '<anonymous>';

      // Étiquette déclarative explicite (SF-237-02) — exempté
      if (cls.PREFILL_COUNT_ALWAYS_ZERO === true) {
        checkedCount.exempted += 1;
        continue;
      }

      const folder = deriveFolderFromClassName(componentName);

      // Tolérance temporaire (sera vidée par SF-237-02)
      if (TOLERATED_NO_HELPER.has(folder)) {
        checkedCount.tolerated += 1;
        continue;
      }

      // 1) Le static doit exister et être une fonction
      if (typeof cls.getPrefillCount !== 'function') {
        failures.push({
          toolId,
          componentName,
          reason: `static getPrefillCount missing or not a function (deriveFolder=${folder})`,
        });
        continue;
      }

      // 2) Vérifier la présence physique du helper à côté du composant
      const helperFilePath = path.join(caseFilesRoot, folder, `${folder}-prefill-rules.ts`);
      if (!fs.existsSync(helperFilePath)) {
        failures.push({
          toolId,
          componentName,
          reason: `helper file not found at expected path '${folder}/${folder}-prefill-rules.ts' (resolved: ${helperFilePath})`,
        });
        continue;
      }

      // 3) Importer le helper et trouver l'export `*PrefillRules`
      let helperModule: Record<string, unknown>;
      try {
        // require dynamique — ts-jest transpile .ts à la volée.
        helperModule = require(`../${folder}/${folder}-prefill-rules`) as Record<string, unknown>;
      } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        failures.push({
          toolId,
          componentName,
          reason: `helper module import failed: ${message}`,
        });
        continue;
      }

      const rulesKey = Object.keys(helperModule).find((k) => /PrefillRules$/.test(k));
      if (!rulesKey) {
        failures.push({
          toolId,
          componentName,
          reason: `helper module '${folder}-prefill-rules.ts' exports no '*PrefillRules' symbol`,
        });
        continue;
      }
      const rules = helperModule[rulesKey] as { computePrefillCount?: unknown };
      if (typeof rules.computePrefillCount !== 'function') {
        failures.push({
          toolId,
          componentName,
          reason: `helper '${rulesKey}' has no computePrefillCount function`,
        });
        continue;
      }
      // Important : conserver `this` lié au helper (certains
      // `computePrefillCount` appellent `this.computeXxx(...)`).
      const helperCompute = (input: Record<string, unknown>): unknown =>
        (rules.computePrefillCount as (this: typeof rules, input: Record<string, unknown>) => unknown).call(rules, input);
      const staticCompute = cls.getPrefillCount as (input: Record<string, unknown>) => unknown;

      checkedCount.withHelper += 1;

      // 4) Parité comportementale sur la batterie de fixtures
      for (const { label, input } of PARITY_FIXTURES) {
        let staticResult: unknown;
        let helperResult: unknown;
        let staticErr: unknown = null;
        let helperErr: unknown = null;
        try {
          staticResult = staticCompute(input);
        } catch (err) {
          staticErr = err;
        }
        try {
          helperResult = helperCompute(input);
        } catch (err) {
          helperErr = err;
        }

        if (staticErr !== null || helperErr !== null) {
          // Au moins l'un des deux a thrown — divergence (les deux doivent
          // soit retourner la même valeur, soit throw symétriquement).
          if (staticErr === null || helperErr === null) {
            failures.push({
              toolId,
              componentName,
              reason: `parity divergence on fixture '${label}' — static ${staticErr ? 'threw' : 'returned ' + JSON.stringify(staticResult)}, helper ${helperErr ? 'threw' : 'returned ' + JSON.stringify(helperResult)}`,
            });
          }
          // Si les deux ont thrown, on tolère (cas pathologique symétrique).
          continue;
        }

        if (staticResult !== helperResult) {
          failures.push({
            toolId,
            componentName,
            reason: `parity divergence on fixture '${label}' — static returned ${JSON.stringify(staticResult)}, helper.${rulesKey}.computePrefillCount returned ${JSON.stringify(helperResult)}`,
          });
        }
      }
    }

    // Sanity log — décommenter pour vérifier la couverture lors d'un audit.
    // eslint-disable-next-line no-console
    // console.log(`[F-237 SF-237-01] checked: total=${checkedCount.total}, withHelper=${checkedCount.withHelper}, tolerated=${checkedCount.tolerated}, exempted=${checkedCount.exempted}`);

    if (failures.length > 0) {
      const lines = failures.map((f) => `  - ${f.toolId} (${f.componentName}): ${f.reason}`);
      const msg = [
        `F-237 SF-237-01 parity violation — ${failures.length} TOOL_REGISTRY entries non conformes :`,
        ...lines,
        '',
        'Chaque entrée TOOL_REGISTRY non-tolérée DOIT exposer un helper',
        "co-localisé `<component>-prefill-rules.ts` exportant un objet `*PrefillRules`",
        "dont la fonction `computePrefillCount(input)` retourne la même valeur que le",
        '`static getPrefillCount(input)` du composant sur les fixtures canoniques.',
        'Cf. docs/features/F-236/contract-prefill-rules.md §5 et docs/features/F-237/SF-237-01.',
      ].join('\n');
      throw new Error(msg);
    }

    // Le test doit avoir effectivement vérifié au moins 1 composant avec helper.
    // (Garde-fou : si la convention `deriveFolderFromClassName` rate, le test
    // ne doit pas être silencieux.)
    expect(checkedCount.withHelper).toBeGreaterThan(0);
  });
});
