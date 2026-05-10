/**
 * F-236 SF-236-05 — Garde-fou CI `prefill-count-integrity`.
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
 * **Motivation produit** (mini-spec SF-236-05) : sans ce static, le panel
 * F-IA-04 affiche silencieusement un badge faux ou absent (`auto_awesome +N`),
 * ce qui invalide la promesse UX "outils décisionnels assistés par l'IA". Le
 * helper `getToolPrefillCount()` du contract retourne `null` quand le static
 * manque → le panel masque le badge sans alerte → bug produit silencieux.
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
 * **Démonstration de la sensibilité** : retirer manuellement `static getPrefillCount`
 * d'un composant aléatoire fait échouer ce test avec un message clair indiquant
 * le tool_id concerné + le nom de la classe.
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
