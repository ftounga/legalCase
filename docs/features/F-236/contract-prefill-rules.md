# F-236 — Contrat du helper partagé `<ComponentName>PrefillRules` (SF-236-01)

> Ce document spécifie le contrat unique que **chaque** composant outil décisionnel doit
> implémenter dans sa SF-236-02. Le helper extrait la logique de pré-remplissage IA
> dans un module pur, partagé entre la méthode runtime `prefillFromAi()` et le static
> `getPrefillCount(input)`. Cible : **garantie de parité runtime/static par construction**
> et suppression de la dette de duplication.

---

## 1 — Convention de nommage

### 1.1 Fichier

À côté de chaque composant outil :

```
frontend/src/app/case-files/<component-folder>/
├── <component>.component.ts
├── <component>.component.html
├── <component>.component.scss
├── <component>.component.spec.ts
└── <component>-prefill-rules.ts          ← nouveau fichier helper
└── <component>-prefill-rules.spec.ts     ← test Jest dédié (3 cas obligatoires)
```

### 1.2 Symbole exporté

Un objet TypeScript figé exporté nommé `<ComponentName>PrefillRules`.
Exemple :

```typescript
export const ImmigrationTitleDecisionPrefillRules = { ... } as const;
```

### 1.3 Module pur

Le fichier helper :
- **N'importe rien de Angular** (pas de `@Input`, pas de `signal`, pas de `inject`, pas d'`HttpClient`).
- **N'a aucun effet de bord** (pas d'`HttpClient`, pas d'écriture de signal, pas d'accès au DOM).
- Importe seulement des types (records IA, enums, constantes statiques).
- N'utilise aucune dépendance temporelle implicite (`new Date()` autorisé seulement si la valeur est purement dérivée d'inputs explicites).

---

## 2 — Signature attendue

### 2.1 Type d'entrée canonique

Réutilise `PrefillCountInput` déjà exposé par `decision-tool.contract.ts` :

```typescript
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
```

```typescript
// (rappel — déjà existant)
export interface PrefillCountInput {
  aiData?: any;
  procedureChecks?: any[];
  aiQuestions?: any[];
  piecesManquantes?: any[];
  triggerEvents?: any[];
  workspaceCountry?: string;
  synthesis?: any;
}
```

Les composants peuvent typer plus fort en interne (`ImmigrationExtractedData | null`,
`FamilleExtractedData | null`, etc.) en castant `input.aiData`.

### 2.2 Forme du helper

Chaque helper expose **trois familles de membres** :

```typescript
export const <ComponentName>PrefillRules = {
  // ── 1. Constantes top-niveau ───────────────────────────────────
  // Ex: mappings code → enum, ensembles de codes pays, regex, keywords
  TRIGGER_TO_CRITERIA: { ... } as const,
  CODE_TO_MOTIF: { ... } as const,
  ISO_DATE_RE: /^\d{4}-\d{2}-\d{2}$/,

  // ── 2. Functions par champ pré-rempli (pures, retournent valeur ou null) ──
  computeNationaliteUe(input: PrefillCountInput): boolean | null { ... },
  computeMotif(input: PrefillCountInput): string | null { ... },
  computeSituationFamiliale(input: PrefillCountInput): string | null { ... },

  // ── 3. Maître count ─────────────────────────────────────────────
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeNationaliteUe(input) !== null) n++;
    if (this.computeMotif(input) !== null) n++;
    if (this.computeSituationFamiliale(input) !== null) n++;
    return n;
  },
} as const;
```

### 2.3 Convention `compute<Field>` retourne `null` quand absent

- Une fonction `compute<Field>` **retourne `null`** quand la donnée IA n'est pas fiable
  (champ absent, type mauvais, gating pays non rempli, etc.).
- Elle **retourne la valeur** (string, number, boolean, enum) quand le pré-fill doit être posé.
- C'est la seule règle requise pour que `computePrefillCount` puisse compter trivialement
  les champs non-`null`.

### 2.4 Gating workspaceCountry

Quand un outil est mono-pays (BE-only ou FR-only), la fonction maître **et** chaque
fonction par champ doivent court-circuiter sur `workspaceCountry` mismatch :

```typescript
computeMotif(input: PrefillCountInput): string | null {
  if ((input.workspaceCountry ?? 'FRANCE') !== 'BELGIQUE') return null;
  // ... reste de la logique BE
},
```

Ainsi le pré-fill et le count restent symétriques en environnement croisé.

---

## 3 — Exemple canonique : refactor F-IM-05 (immigration-title-decision-section)

### 3.1 Avant — duplication runtime / static (état actuel sur master)

```typescript
// immigration-title-decision-section.component.ts (extrait simplifié)

const TRIGGER_TO_CRITERIA: Record<string, { motif: string; situationFamiliale?: string }> = {
  MARIAGE_FRANCAIS: { motif: 'FAMILLE', situationFamiliale: 'MARIE' },
  // ...
};

const CODE_TO_MOTIF: Record<string, string> = {
  ETUDIANT: 'ETUDES',
  SALARIE: 'TRAVAIL',
  // ...
};

@Component({ /* ... */ })
export class ImmigrationTitleDecisionSectionComponent {
  static getPrefillCount(input: { aiData?: any; triggerEvents?: any[]; /* ... */ }): number {
    const ai = input.aiData;
    const triggers = Array.isArray(input.triggerEvents) ? input.triggerEvents : [];
    if (!ai && triggers.length === 0) return 0;
    let count = 0;
    if (ai && typeof ai.nationaliteUe === 'boolean') count++;
    const firstTrigger = triggers[0]?.eventCode ?? triggers[0]?.event_code;
    if (firstTrigger && TRIGGER_TO_CRITERIA[firstTrigger]) {
      const criteria = TRIGGER_TO_CRITERIA[firstTrigger];
      count++;
      if (criteria.situationFamiliale) count++;
      return count;
    }
    if (!ai) return count;
    const code = typeof ai.typeTitreSejourCode === 'string' ? ai.typeTitreSejourCode.toUpperCase() : null;
    if (code && CODE_TO_MOTIF[code]) { count++; return count; }
    if (typeof ai.typeTitreSejour === 'string' && ai.typeTitreSejour.length > 0) {
      const t = ai.typeTitreSejour.toUpperCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
      if (t.includes('ETUDIANT') || t.includes('SALARIE') /* ... */) count++;
    }
    return count;
  }

  private prefillFromAi(): void {
    if (!this.aiData && !this.triggerEvents?.length) return;
    if (this.aiData && typeof this.aiData.nationaliteUe === 'boolean') {
      this.nationaliteUe.set(this.aiData.nationaliteUe);
      this.provenanceNationaliteUe.set('IA');
    }
    const firstTrigger = this.triggerEvents?.[0]?.eventCode;
    if (firstTrigger && TRIGGER_TO_CRITERIA[firstTrigger]) {
      const criteria = TRIGGER_TO_CRITERIA[firstTrigger];
      this.motif.set(criteria.motif);
      this.provenanceMotif.set('IA');
      if (criteria.situationFamiliale) {
        this.situationFamiliale.set(criteria.situationFamiliale);
        this.provenanceSituationFamiliale.set('IA');
      }
      return;
    }
    if (!this.aiData) return;
    const code = this.aiData.typeTitreSejourCode?.toUpperCase();
    if (code && CODE_TO_MOTIF[code]) {
      this.motif.set(CODE_TO_MOTIF[code]);
      this.provenanceMotif.set('IA');
      return;
    }
    if (this.aiData.typeTitreSejour) {
      const t = this.aiData.typeTitreSejour.toUpperCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
      let detected: string | null = null;
      if (t.includes('ETUDIANT')) detected = 'ETUDES';
      else if (t.includes('SALARIE')) detected = 'TRAVAIL';
      // ...
      if (detected) {
        this.motif.set(detected);
        this.provenanceMotif.set('IA');
      }
    }
  }
}
```

**Problèmes** :
- `TRIGGER_TO_CRITERIA` et la cascade triggerEvents → CODE_TO_MOTIF → texte sont dupliqués.
- L'évolution d'une branche dans le runtime sans mise à jour symétrique du static = bug
  (badge faux ou erreur d'audit).

### 3.2 Après — helper `ImmigrationTitleDecisionPrefillRules`

#### `immigration-title-decision-section-prefill-rules.ts`

```typescript
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

const TRIGGER_TO_CRITERIA: Record<string, { motif: string; situationFamiliale?: string }> = {
  MARIAGE_FRANCAIS: { motif: 'FAMILLE', situationFamiliale: 'MARIE' },
  // ...
};

const CODE_TO_MOTIF: Record<string, string> = {
  ETUDIANT: 'ETUDES',
  SALARIE: 'TRAVAIL',
  // ...
};

const TEXT_KEYWORDS: ReadonlyArray<{ kw: readonly string[]; motif: string }> = [
  { kw: ['ETUDIANT', 'STUDENT'], motif: 'ETUDES' },
  { kw: ['SALARIE', 'TRAVAIL'], motif: 'TRAVAIL' },
  { kw: ['FAMILLE', 'VPF'], motif: 'FAMILLE' },
  { kw: ['ASILE', 'REFUGIE'], motif: 'ASILE' },
];

function normalize(s: string): string {
  return s.toUpperCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

function firstTriggerCode(input: PrefillCountInput): string | null {
  const triggers = Array.isArray(input.triggerEvents) ? input.triggerEvents : [];
  const t = triggers[0];
  return t?.eventCode ?? t?.event_code ?? null;
}

export const ImmigrationTitleDecisionPrefillRules = {
  TRIGGER_TO_CRITERIA,
  CODE_TO_MOTIF,

  /** Renvoie la valeur boolean ou `null` si pas pré-remplissable. */
  computeNationaliteUe(input: PrefillCountInput): boolean | null {
    const ai = input.aiData;
    if (!ai || typeof ai.nationaliteUe !== 'boolean') return null;
    return ai.nationaliteUe;
  },

  /** Cascade triggerEvents → typeTitreSejourCode → heuristique texte. */
  computeMotif(input: PrefillCountInput): string | null {
    const code = firstTriggerCode(input);
    if (code && TRIGGER_TO_CRITERIA[code]) {
      return TRIGGER_TO_CRITERIA[code].motif;
    }
    const ai = input.aiData;
    if (!ai) return null;
    const titreCode = typeof ai.typeTitreSejourCode === 'string'
      ? ai.typeTitreSejourCode.toUpperCase()
      : null;
    if (titreCode && CODE_TO_MOTIF[titreCode]) {
      return CODE_TO_MOTIF[titreCode];
    }
    if (typeof ai.typeTitreSejour === 'string' && ai.typeTitreSejour.length > 0) {
      const t = normalize(ai.typeTitreSejour);
      const match = TEXT_KEYWORDS.find(({ kw }) => kw.some(k => t.includes(k)));
      if (match) return match.motif;
    }
    return null;
  },

  /** Posé seulement quand un triggerEvent l'impose explicitement. */
  computeSituationFamiliale(input: PrefillCountInput): string | null {
    const code = firstTriggerCode(input);
    if (code && TRIGGER_TO_CRITERIA[code]?.situationFamiliale) {
      return TRIGGER_TO_CRITERIA[code].situationFamiliale!;
    }
    return null;
  },

  /** Maître : compte les champs non-null. */
  computePrefillCount(input: PrefillCountInput): number {
    let n = 0;
    if (this.computeNationaliteUe(input) !== null) n++;
    if (this.computeMotif(input) !== null) n++;
    if (this.computeSituationFamiliale(input) !== null) n++;
    return n;
  },
} as const;
```

#### `immigration-title-decision-section.component.ts` (extrait après refacto)

```typescript
import { ImmigrationTitleDecisionPrefillRules as Rules } from
  './immigration-title-decision-section-prefill-rules';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

@Component({ /* ... */ })
export class ImmigrationTitleDecisionSectionComponent {
  static readonly TOOL_LABEL = 'TITRE DE SÉJOUR RECOMMANDÉ';
  static readonly TOOL_ICON = 'account_tree';

  /** F-236 SF-236-02 : délègue au helper pur (parité runtime/static garantie). */
  static getPrefillCount(input: PrefillCountInput): number {
    return Rules.computePrefillCount(input);
  }

  // ... @Inputs et signals inchangés ...

  private prefillFromAi(): void {
    const input: PrefillCountInput = {
      aiData: this.aiData,
      triggerEvents: this.triggerEvents ?? undefined,
      // (autres sources non utilisées par F-IM-05 — restent absentes)
    };
    const ue = Rules.computeNationaliteUe(input);
    if (ue !== null) {
      this.nationaliteUe.set(ue);
      this.provenanceNationaliteUe.set('IA');
    }
    const motif = Rules.computeMotif(input);
    if (motif !== null) {
      this.motif.set(motif);
      this.provenanceMotif.set('IA');
    }
    const sit = Rules.computeSituationFamiliale(input);
    if (sit !== null) {
      this.situationFamiliale.set(sit);
      this.provenanceSituationFamiliale.set('IA');
    }
  }
}
```

**Bénéfice** :
- Une seule source de vérité (`Rules.compute<Field>`).
- `getPrefillCount` ne peut plus diverger de `prefillFromAi()` car le runtime
  consomme **exactement les mêmes fonctions pures**.
- Les tests unitaires Jest s'écrivent sur le module helper (pas besoin de TestBed Angular).
- Refactorisation incrémentale : on peut migrer un seul composant sans toucher aux autres.

---

## 4 — Pattern de test Jest obligatoire (3 cas)

Chaque helper doit être livré avec un fichier `<component>-prefill-rules.spec.ts`
contenant **au minimum** ces 3 cas :

### 4.1 Cas 0 — Aucune source IA disponible

```typescript
import { ImmigrationTitleDecisionPrefillRules as Rules } from
  './immigration-title-decision-section-prefill-rules';

describe('ImmigrationTitleDecisionPrefillRules', () => {
  it('returns 0 when no aiData and no triggerEvents', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: undefined, triggerEvents: [] })).toBe(0);
  });

  it('returns 0 when aiData is empty object', () => {
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });
});
```

### 4.2 Cas M — Sources partielles (1 < M < N champs)

```typescript
it('returns M when only some fields are present', () => {
  const input = { aiData: { nationaliteUe: true } };
  expect(Rules.computePrefillCount(input)).toBe(1);
  expect(Rules.computeNationaliteUe(input)).toBe(true);
  expect(Rules.computeMotif(input)).toBeNull();
  expect(Rules.computeSituationFamiliale(input)).toBeNull();
});

it('returns 1 when only typeTitreSejourCode is present', () => {
  const input = { aiData: { typeTitreSejourCode: 'ETUDIANT' } };
  expect(Rules.computePrefillCount(input)).toBe(1);
  expect(Rules.computeMotif(input)).toBe('ETUDES');
});
```

### 4.3 Cas N — Toutes les sources alimentent (count = max théo.)

```typescript
it('returns N (= max théo.) when all sources alimentent', () => {
  const input = {
    aiData: { nationaliteUe: false, typeTitreSejourCode: 'SALARIE' },
    triggerEvents: [{ eventCode: 'MARIAGE_FRANCAIS' }],
  };
  // triggerEvents prend priorité → motif=FAMILLE + situationFamiliale=MARIE
  // + nationaliteUe(boolean) = 3 fields
  expect(Rules.computePrefillCount(input)).toBe(3);
  expect(Rules.computeNationaliteUe(input)).toBe(false);
  expect(Rules.computeMotif(input)).toBe('FAMILLE');
  expect(Rules.computeSituationFamiliale(input)).toBe('MARIE');
});
```

### 4.4 Cas optionnel — Gating pays (mono-pays uniquement)

```typescript
// Pour les helpers BE-only (ex: belgian-9bis)
it('returns 0 when workspaceCountry !== BELGIQUE', () => {
  const input = { aiData: { dateDepotProcedure: '2026-04-01' }, workspaceCountry: 'FRANCE' };
  expect(Belgian9bisPrefillRules.computePrefillCount(input)).toBe(0);
});
```

---

## 5 — Règle d'usage côté composant — parité runtime / static

### 5.1 Le static est un délégateur trivial

```typescript
static getPrefillCount(input: PrefillCountInput): number {
  return Rules.computePrefillCount(input);
}
```

Pas de logique dupliquée, pas de cas particulier. Si le static fait autre chose,
le test d'intégrité (SF-236-05) doit échouer.

### 5.2 Le runtime appelle les fonctions par champ + pose les signals

```typescript
private prefillFromAi(): void {
  const input: PrefillCountInput = this.buildPrefillInput(); // helper privé
  const v = Rules.computeXxx(input);
  if (v !== null) {
    this.xxx.set(v);
    this.provenanceXxx.set('IA');
  }
  // ... répété pour chaque champ
}

private buildPrefillInput(): PrefillCountInput {
  return {
    aiData: this.aiData,
    triggerEvents: this.triggerEvents ?? undefined,
    procedureChecks: this.procedureChecks ?? undefined,
    aiQuestions: this.aiQuestions ?? undefined,
    piecesManquantes: this.piecesManquantes ?? undefined,
    workspaceCountry: this.workspaceCountry,
    synthesis: undefined, // ou (si pertinent) la sub-section spécifique
  };
}
```

### 5.3 Garantie de parité par construction

Si une nouvelle source/champ est ajouté :
1. Modifier `compute<Field>` dans le helper (1 endroit).
2. Ajouter le test Jest qui couvre le nouveau champ (cas M et N étendus).
3. Modifier le runtime pour appeler la nouvelle fonction et poser le signal.

Il est **impossible** d'ajouter un champ runtime sans que le static le compte —
puisque le static est un appel `computePrefillCount()` qui agrège l'ensemble.
La parité est intrinsèque au design.

### 5.4 Le test d'intégrité CI (SF-236-05)

Le test parcourt `TOOL_REGISTRY` et vérifie que chaque composant :
- Expose `static getPrefillCount` (P0 — sauf wrappers explicitement listés).
- Que `getPrefillCount` retourne `Rules.computePrefillCount(...)` strictement
  (vérification structurelle ou comportementale via une suite de fixtures partagées).
- Que le helper existe au chemin canonique `<component>-prefill-rules.ts`.
- Que la suite Jest 3-cas (0/M/N) existe pour chaque helper.

---

## 6 — Migration progressive

Ordre d'implémentation recommandé pour SF-236-02 :

1. **Vague A — Travail** (29 composants) : essentiellement des outils mono-`salaireBrutMensuel` + 1-2 dates → contrat ultra-simple.
2. **Vague B — Immigration** (18 composants) : intègre le gating pays BE — pattern `computeXxx(input) → null si workspaceCountry mismatch`.
3. **Vague C — Famille** (32 composants) : volumineux mais homogènes — détections boolean + dates ISO + montants EUR.

Chaque vague livre :
- N helpers `<component>-prefill-rules.ts`.
- N specs Jest 3-cas.
- N composants refacto'd (runtime + static délégant au helper).

---

## 7 — Hors scope du contrat

- Les composants `count=0` (wrappers informationnels) **n'ont pas de helper**. Ils
  conservent l'implémentation `static getPrefillCount(): number { return 0; }` triviale.
- La validation F-IA-03 (`coherenceAlerts`, `CoherenceAlertBuilder`) **n'est pas affectée**
  par F-236. Elle reste dans le composant Angular (signaux + `computed`).
- Le pré-fill via `set inferredChecklistType()` (F-IM-01) est conservé — le helper
  expose la logique de `getPrefillCount`, pas la mécanique de propagation Angular.

---

**Référence canonique** : `immigration-title-decision-section` post-SF-236-02.
**Garde-fou CI** : `DecisionToolPrefillIntegrityIT` (SF-236-05) — bloque tout merge si :
- Un composant TOOL_REGISTRY n'a pas de helper.
- Un helper n'a pas de spec Jest.
- Le `getPrefillCount` runtime diverge de `Rules.computePrefillCount`.
