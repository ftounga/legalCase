# Mini-spec — F-255 / SF-255-02b Extension formulaire admin pour STRIPE_DISCOUNT

## Identifiant
`F-255 / SF-02b` (extension de SF-02)

## Objectif
Ajouter au formulaire admin `/super-admin/promo-codes` les 4 champs requis pour créer un code de type STRIPE_DISCOUNT (`valueOffType`, `valueOffAmount`, `currency`, `duration`), livrés côté backend par SF-255-04 (PR #1258) mais absents du formulaire.

**Symptôme observé** : sélectionner « Réduction sur abonnement (Stripe Checkout) » + valider → 400 `VALIDATION_FAILED` du backend (champs obligatoires absents du payload).

## Cycle de gouvernance
Extension UI mineure d'un formulaire existant, **étapes 0 + 0 bis déjà couvertes** par les docs F-255 globaux (`SF-255-00-coherence.md` et `SF-255-00b-ux-coherence.md`). Pas de nouveau cadrage requis.

## Comportement attendu

### Visibilité conditionnelle des champs

| Type sélectionné | Champs visibles |
|---|---|
| `TRIAL_EXTENSION` (défaut) | `valueDays` (déjà OK), masquer les 4 Stripe |
| `STRIPE_DISCOUNT` | masquer `valueDays`, afficher `valueOffType` + `valueOffAmount` + `duration` |
| `STRIPE_DISCOUNT` + `valueOffType=AMOUNT` | + afficher `currency` (sinon masqué, `EUR` envoyé d'office si AMOUNT) |
| `STRIPE_DISCOUNT` + `valueOffType=PERCENT` | masquer `currency` (le `valueOffAmount` est un % entre 1 et 100) |

### Validations conditionnelles (Validators dynamiques)

| Champ | Quand requis | Min/Max |
|---|---|---|
| `valueDays` | si `type=TRIAL_EXTENSION` | 1..365 |
| `valueOffType` | si `type=STRIPE_DISCOUNT` | enum `PERCENT` ou `AMOUNT` |
| `valueOffAmount` | si `type=STRIPE_DISCOUNT` | si `PERCENT` : 1..100 (pourcentage) ; si `AMOUNT` : 100..100000 centimes EUR (= 1€ à 1000€) |
| `currency` | si `type=STRIPE_DISCOUNT` ET `valueOffType=AMOUNT` | `EUR` (V1) — value pré-remplie + select avec 1 option |
| `duration` | si `type=STRIPE_DISCOUNT` | enum `ONCE` \| `REPEATING_3` \| `FOREVER` |

### Payload envoyé au backend

Pour TRIAL_EXTENSION (inchangé) :
```json
{ "code": "...", "type": "TRIAL_EXTENSION", "valueDays": 30, "partnerLabel": "...", "maxUses": 50, "expiresAt": "2026-12-31T23:59:59Z" }
```

Pour STRIPE_DISCOUNT (nouveau) :
```json
{
  "code": "PARTNER10",
  "type": "STRIPE_DISCOUNT",
  "valueDays": null,
  "valueOffType": "PERCENT",
  "valueOffAmount": 10,
  "currency": null,
  "duration": "ONCE",
  "partnerLabel": "...",
  "maxUses": 50,
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

(Si `valueOffType=AMOUNT` : `currency: "EUR"` et `valueOffAmount` en centimes.)

## Critères d'acceptation

- [ ] **C1** — Par défaut (`TRIAL_EXTENSION`), comportement identique à actuel : `valueDays` visible/required, champs Stripe absents du DOM.
- [ ] **C2** — Sélection `STRIPE_DISCOUNT` → `valueDays` disparaît, `valueOffType` + `valueOffAmount` + `duration` apparaissent (3 nouveaux champs).
- [ ] **C3** — Si `valueOffType=AMOUNT` → champ `currency` apparaît (select avec `EUR` pré-rempli, valeur unique V1). Si `valueOffType=PERCENT` → `currency` absent du DOM.
- [ ] **C4** — Validation client : `valueOffAmount` 1..100 si PERCENT, 100..100000 si AMOUNT (en centimes EUR, soit 1€ à 1000€).
- [ ] **C5** — Submit STRIPE_DISCOUNT nominal → 201, code apparaît dans la table avec ses paramètres Stripe.
- [ ] **C6** — Submit STRIPE_DISCOUNT avec champ manquant côté client → bouton « Créer » disabled + `mat-error` visible.
- [ ] **C7** — Switch entre les 2 types → les validators sont correctement appliqués/retirés (pas de validators résiduels qui bloquent le submit).
- [ ] **C8** — Tests Jest existants (T-01 à T-08 sur SuperAdminPromoCodesComponent) restent verts. **+ tests nouveaux** :
  - T-09 : sélection STRIPE_DISCOUNT → `valueDays` retiré du form, 3 champs Stripe ajoutés
  - T-10 : sélection AMOUNT → `currency` ajouté, value `EUR`
  - T-11 : sélection PERCENT → `currency` retiré
  - T-12 : submit STRIPE_DISCOUNT/PERCENT → payload contient `valueOffType`, `valueOffAmount`, `duration`, pas de `valueDays` ni `currency`
  - T-13 : submit STRIPE_DISCOUNT/AMOUNT → payload contient `currency: 'EUR'` en plus

## Périmètre

### Inclus
- Modification : `frontend/src/app/super-admin/promo-codes/super-admin-promo-codes.component.ts` (FormGroup + helper validators)
- Modification : `frontend/src/app/super-admin/promo-codes/super-admin-promo-codes.component.html` (4 nouveaux mat-form-field conditionnels)
- Modification : `frontend/src/app/super-admin/promo-codes/super-admin-promo-codes.component.spec.ts` (+5 tests)
- Modification : `frontend/src/app/super-admin/promo-codes/promo-code.model.ts` (DTO TS étendu si pas déjà fait)

### Hors-scope
- Affichage des paramètres Stripe dans la table de listing (V2 si besoin — le `partnerLabel` + `type` suffisent V1 pour identifier).
- Édition d'un code existant (toujours hors scope V1).
- Pré-fill de `currency` ailleurs que EUR (multi-devise V2).

## Technique

### TypeScript

Renommer `applyValueDaysValidators(type)` en `applyTypeValidators(type)` et étendre la logique :

```ts
private applyTypeValidators(type: PromoCodeType): void {
  if (type === 'TRIAL_EXTENSION') {
    this.form.get('valueDays')?.setValidators([Validators.required, Validators.min(1), Validators.max(365)]);
    this.form.get('valueOffType')?.clearValidators();
    this.form.get('valueOffAmount')?.clearValidators();
    this.form.get('currency')?.clearValidators();
    this.form.get('duration')?.clearValidators();
  } else { // STRIPE_DISCOUNT
    this.form.get('valueDays')?.clearValidators();
    this.form.get('valueOffType')?.setValidators([Validators.required]);
    // valueOffAmount validators appliqués dans applyValueOffTypeValidators
    this.form.get('duration')?.setValidators([Validators.required]);
    this.applyValueOffTypeValidators(this.form.get('valueOffType')?.value);
  }
  // updateValueAndValidity() sur tous les champs touchés
}

private applyValueOffTypeValidators(valueOffType: PromoCodeValueOffType | null): void {
  if (valueOffType === 'PERCENT') {
    this.form.get('valueOffAmount')?.setValidators([Validators.required, Validators.min(1), Validators.max(100)]);
    this.form.get('currency')?.clearValidators();
    this.form.get('currency')?.setValue(null);
  } else if (valueOffType === 'AMOUNT') {
    this.form.get('valueOffAmount')?.setValidators([Validators.required, Validators.min(100), Validators.max(100000)]);
    this.form.get('currency')?.setValidators([Validators.required]);
    this.form.get('currency')?.setValue('EUR');
  }
}
```

Subscriptions :
- `form.get('type').valueChanges.subscribe(t => this.applyTypeValidators(t))` (déjà partiellement en place pour `valueDays`)
- `form.get('valueOffType').valueChanges.subscribe(v => this.applyValueOffTypeValidators(v))` (nouveau)

Modèle TS (à compléter dans `promo-code.model.ts`) :

```ts
export type PromoCodeValueOffType = 'PERCENT' | 'AMOUNT';
export type PromoCodeDuration = 'ONCE' | 'REPEATING_3' | 'FOREVER';

export interface PromoCodeCreateRequest {
  code: string;
  type: PromoCodeType;
  valueDays?: number | null;
  valueOffType?: PromoCodeValueOffType | null;
  valueOffAmount?: number | null;
  currency?: string | null;
  duration?: PromoCodeDuration | null;
  partnerLabel: string;
  maxUses: number;
  expiresAt: string;
}
```

### HTML

Sous le `@if (isTrialExtension())` existant, ajouter un `@if (isStripeDiscount())` (signal symétrique à créer) qui contient les 3 (ou 4) champs Stripe. Pattern de visibilité comme `valueDays`.

### Self-checks

```bash
cd frontend
npx tsc --noEmit -p tsconfig.app.json
npx jest super-admin-promo-codes --silent
```

## Préoccupations transversales
- **Plans / limites** ✅ (déjà couvert F-255 globalement)
- Navigation : aucun changement de route
- Auth / Workspace : aucun changement

## Dépendances
- ✅ SF-255-02 mergée (PR #1255)
- ✅ SF-255-04 backend mergé (PR #1258) — contrat figé

## Liens
- Étape 0 globale : `docs/features/F-255/SF-255-00-coherence.md`
- Backend SF-04 : `backend/src/main/java/fr/ailegalcase/billing/PromoCodeCreateRequest.java`
- Composant à étendre : `frontend/src/app/super-admin/promo-codes/super-admin-promo-codes.component.ts`
