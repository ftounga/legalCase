import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';

import { AuthService } from '../../core/services/auth.service';
import { SuperAdminConfirmDialogComponent } from '../super-admin-confirm-dialog.component';
import { fadeInUp } from '../../shared/animations';

import { PromoCodeAdminService } from './promo-code-admin.service';
import {
  PromoCodeCreateRequest,
  PromoCodeDto,
  PromoCodeDuration,
  PromoCodeType,
  PromoCodeValueOffType,
} from './promo-code.model';

/**
 * F-255 SF-255-02 — Écran super-admin de gestion des codes promo.
 *
 * <p>Deux blocs :</p>
 * <ul>
 *   <li>Formulaire de création (en haut) — soumet `POST /api/v1/super-admin/promo-codes`</li>
 *   <li>Table des codes existants (en bas) — listing temps réel + bouton « Désactiver » par ligne</li>
 * </ul>
 *
 * <p>Pattern : composant standalone Angular 19, signal-based state, OnPush,
 * confirmation destructive via MatDialog, toasts via MatSnackBar, palette
 * DESIGN_SYSTEM (navy/or, rouge sobre pour la désactivation).</p>
 */
@Component({
  selector: 'app-super-admin-promo-codes',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './super-admin-promo-codes.component.html',
  styleUrl: './super-admin-promo-codes.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class SuperAdminPromoCodesComponent implements OnInit {
  // Dépendances Angular 19 — pattern inject()
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly promoCodeService = inject(PromoCodeAdminService);

  // Signal-based state
  readonly codes = signal<PromoCodeDto[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);

  /** Colonnes affichées par la table — ordre = ordre visuel. */
  readonly displayedColumns = [
    'code',
    'type',
    'partnerLabel',
    'uses',
    'expiresAt',
    'active',
    'createdAt',
    'actions',
  ];

  /** YYYY-MM-DD du 31 décembre de l'année courante — défaut sensé pour les campagnes annuelles. */
  private readonly defaultExpiresAt = `${new Date().getFullYear()}-12-31`;

  /**
   * Formulaire de création. Champs conditionnels :
   * - `valueDays` : actif si `type = TRIAL_EXTENSION`.
   * - `valueOffType`, `valueOffAmount`, `duration` : actifs si `type = STRIPE_DISCOUNT`.
   * - `currency` : actif si `type = STRIPE_DISCOUNT` ET `valueOffType = AMOUNT`.
   */
  readonly form: FormGroup = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(64),
      Validators.pattern(/^[A-Za-z0-9_\- ]+$/)]],
    type: ['TRIAL_EXTENSION' as PromoCodeType, [Validators.required]],
    valueDays: [30, [Validators.required, Validators.min(1), Validators.max(365)]],
    // SF-255-02b — champs Stripe (branche STRIPE_DISCOUNT)
    valueOffType: [null as PromoCodeValueOffType | null],
    valueOffAmount: [null as number | null],
    currency: [null as string | null],
    duration: [null as PromoCodeDuration | null],
    partnerLabel: ['', [Validators.required, Validators.maxLength(100)]],
    maxUses: [50, [Validators.required, Validators.min(1), Validators.max(100_000)]],
    expiresAt: [this.defaultExpiresAt, [Validators.required]],
  });

  /** Signal dérivé du contrôle `type` (signal effect via reactive forms). */
  private readonly typeSignal = signal<PromoCodeType>('TRIAL_EXTENSION');
  readonly isTrialExtension = computed(() => this.typeSignal() === 'TRIAL_EXTENSION');
  readonly isStripeDiscount = computed(() => this.typeSignal() === 'STRIPE_DISCOUNT');

  /** Signal dérivé du contrôle `valueOffType` (visibilité du champ `currency`). */
  private readonly valueOffTypeSignal = signal<PromoCodeValueOffType | null>(null);
  readonly isAmountOff = computed(() => this.valueOffTypeSignal() === 'AMOUNT');
  readonly isPercentOff = computed(() => this.valueOffTypeSignal() === 'PERCENT');

  ngOnInit(): void {
    if (!this.auth.currentUser()?.isSuperAdmin) {
      this.router.navigate(['/case-files']);
      return;
    }

    // Suit l'état du contrôle `type` pour activer/désactiver dynamiquement
    // les validateurs `valueDays` (TRIAL_EXTENSION) et Stripe (STRIPE_DISCOUNT).
    this.form.get('type')!.valueChanges.subscribe((value: PromoCodeType) => {
      this.typeSignal.set(value);
      this.applyTypeValidators(value);
    });
    // SF-255-02b — change de min/max sur valueOffAmount + (de)active currency.
    this.form.get('valueOffType')!.valueChanges.subscribe(
      (value: PromoCodeValueOffType | null) => {
        this.valueOffTypeSignal.set(value);
        this.applyValueOffTypeValidators(value);
      },
    );
    this.applyTypeValidators(this.form.get('type')!.value as PromoCodeType);

    this.loadCodes();
  }

  // ---------------------------------------------------------------------------
  // Public actions
  // ---------------------------------------------------------------------------

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);

    const req = this.buildRequest();
    this.promoCodeService.createCode(req).subscribe({
      next: (created: PromoCodeDto) => {
        this.submitting.set(false);
        this.snackBar.open(`Code « ${created.code} » créé.`, 'Fermer', {
          duration: 3000, panelClass: ['snack-success'],
        });
        this.resetForm();
        this.loadCodes();
      },
      error: (err: HttpErrorLike) => {
        this.submitting.set(false);
        this.handleError(err, 'Création du code promo impossible.');
      },
    });
  }

  confirmDeactivate(code: PromoCodeDto): void {
    const ref = this.dialog.open(SuperAdminConfirmDialogComponent, {
      width: '480px',
      data: {
        message: `Désactiver le code « ${code.code} » (${code.partnerLabel}) ? Le code restera en table mais ne pourra plus être consommé.`,
      },
    });
    ref.afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;
      this.deactivate(code);
    });
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private loadCodes(): void {
    this.loading.set(true);
    this.promoCodeService.listCodes().subscribe({
      next: (codes: PromoCodeDto[]) => {
        this.codes.set(codes);
        this.loading.set(false);
      },
      error: (err: HttpErrorLike) => {
        this.loading.set(false);
        if (err?.status === 403) {
          this.router.navigate(['/case-files']);
          return;
        }
        this.snackBar.open('Impossible de charger les codes promo.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error'],
        });
      },
    });
  }

  private deactivate(code: PromoCodeDto): void {
    this.promoCodeService.deactivateCode(code.id).subscribe({
      next: (updated: PromoCodeDto) => {
        // Met à jour la ligne dans la table sans recharger toute la liste.
        this.codes.update(list => list.map(c => (c.id === updated.id ? updated : c)));
        this.snackBar.open(`Code « ${updated.code} » désactivé.`, 'Fermer', {
          duration: 3000, panelClass: ['snack-success'],
        });
      },
      error: (err: HttpErrorLike) => {
        this.handleError(err, 'Désactivation du code impossible.');
      },
    });
  }

  private buildRequest(): PromoCodeCreateRequest {
    const raw = this.form.getRawValue() as {
      code: string;
      type: PromoCodeType;
      valueDays: number | null;
      valueOffType: PromoCodeValueOffType | null;
      valueOffAmount: number | null;
      currency: string | null;
      duration: PromoCodeDuration | null;
      partnerLabel: string;
      maxUses: number;
      expiresAt: string;
    };
    const isStripe = raw.type === 'STRIPE_DISCOUNT';
    // Le backend impose code en MAJ — on uppercase côté client pour l'aperçu UX.
    // Le datepicker natif HTML renvoie un YYYY-MM-DD ; le backend attend un
    // Instant. On envoie en ISO-8601 fin de journée UTC pour rester cohérent.
    // SF-255-02b — payload nettoyé : on n'envoie jamais les champs résiduels
    // de la branche non-active (TRIAL n'envoie pas les Stripe, STRIPE n'envoie
    // pas valueDays). Le backend refuse les combinaisons mixtes.
    return {
      code: (raw.code ?? '').trim().toUpperCase(),
      type: raw.type,
      valueDays: isStripe ? null : raw.valueDays,
      valueOffType: isStripe ? raw.valueOffType : null,
      valueOffAmount: isStripe ? raw.valueOffAmount : null,
      currency: isStripe && raw.valueOffType === 'AMOUNT' ? raw.currency : null,
      duration: isStripe ? raw.duration : null,
      partnerLabel: (raw.partnerLabel ?? '').trim(),
      maxUses: raw.maxUses,
      expiresAt: this.toIsoEndOfDay(raw.expiresAt),
    };
  }

  private toIsoEndOfDay(date: string): string {
    if (!date) return '';
    // `datetime-local` ou `date` renvoie YYYY-MM-DD ou YYYY-MM-DDTHH:mm. On
    // accepte les deux et on aligne sur 23:59:59 UTC quand seul la date est
    // fournie (cas attendu en V1).
    if (date.length === 10) {
      return `${date}T23:59:59Z`;
    }
    if (!date.endsWith('Z')) {
      return `${date}:00Z`;
    }
    return date;
  }

  /**
   * SF-255-02b — applique les validateurs conditionnels selon le `type`
   * sélectionné. TRIAL_EXTENSION active `valueDays`, STRIPE_DISCOUNT active
   * `valueOffType` + `valueOffAmount` + `duration` (et délègue `currency` +
   * bornes de `valueOffAmount` à {@link #applyValueOffTypeValidators}).
   */
  private applyTypeValidators(type: PromoCodeType): void {
    const valueDays = this.form.get('valueDays')!;
    const valueOffType = this.form.get('valueOffType')!;
    const valueOffAmount = this.form.get('valueOffAmount')!;
    const currency = this.form.get('currency')!;
    const duration = this.form.get('duration')!;

    if (type === 'TRIAL_EXTENSION') {
      valueDays.setValidators([Validators.required, Validators.min(1), Validators.max(365)]);
      valueDays.enable({ emitEvent: false });

      // Nettoyer les validateurs Stripe pour éviter qu'un résidu bloque le submit
      // après un switch STRIPE → TRIAL_EXTENSION.
      valueOffType.clearValidators();
      valueOffAmount.clearValidators();
      currency.clearValidators();
      duration.clearValidators();
      // Reset des valeurs Stripe pour cohérence du signal `isAmountOff`.
      valueOffType.setValue(null, { emitEvent: false });
      valueOffAmount.setValue(null, { emitEvent: false });
      currency.setValue(null, { emitEvent: false });
      duration.setValue(null, { emitEvent: false });
      this.valueOffTypeSignal.set(null);
    } else {
      // STRIPE_DISCOUNT
      valueDays.clearValidators();
      valueDays.disable({ emitEvent: false });

      valueOffType.setValidators([Validators.required]);
      duration.setValidators([Validators.required]);
      // Bornes de `valueOffAmount` + `currency` selon `valueOffType` courant.
      this.applyValueOffTypeValidators(valueOffType.value as PromoCodeValueOffType | null);
    }
    valueDays.updateValueAndValidity({ emitEvent: false });
    valueOffType.updateValueAndValidity({ emitEvent: false });
    valueOffAmount.updateValueAndValidity({ emitEvent: false });
    currency.updateValueAndValidity({ emitEvent: false });
    duration.updateValueAndValidity({ emitEvent: false });
  }

  /**
   * SF-255-02b — bornes de `valueOffAmount` + (de)activation de `currency`
   * selon le type de réduction sélectionné.
   *
   * <ul>
   *   <li>{@code PERCENT} → `valueOffAmount` 1..100, `currency` masqué (null).</li>
   *   <li>{@code AMOUNT} → `valueOffAmount` 100..100000 (centimes EUR, 1€..1000€),
   *       `currency` requis avec `EUR` pré-rempli.</li>
   * </ul>
   */
  private applyValueOffTypeValidators(valueOffType: PromoCodeValueOffType | null): void {
    const valueOffAmount = this.form.get('valueOffAmount')!;
    const currency = this.form.get('currency')!;

    if (valueOffType === 'PERCENT') {
      valueOffAmount.setValidators([Validators.required, Validators.min(1), Validators.max(100)]);
      currency.clearValidators();
      currency.setValue(null, { emitEvent: false });
    } else if (valueOffType === 'AMOUNT') {
      valueOffAmount.setValidators([Validators.required, Validators.min(100), Validators.max(100_000)]);
      currency.setValidators([Validators.required]);
      // V1 : EUR pré-rempli (seule option disponible côté UI).
      if (currency.value !== 'EUR') {
        currency.setValue('EUR', { emitEvent: false });
      }
    } else {
      // valueOffType absent (cas initial STRIPE_DISCOUNT pas encore choisi).
      valueOffAmount.clearValidators();
      currency.clearValidators();
      currency.setValue(null, { emitEvent: false });
    }
    valueOffAmount.updateValueAndValidity({ emitEvent: false });
    currency.updateValueAndValidity({ emitEvent: false });
  }

  private resetForm(): void {
    this.form.reset({
      code: '',
      type: 'TRIAL_EXTENSION' as PromoCodeType,
      valueDays: 30,
      valueOffType: null,
      valueOffAmount: null,
      currency: null,
      duration: null,
      partnerLabel: '',
      maxUses: 50,
      expiresAt: this.defaultExpiresAt,
    });
    this.typeSignal.set('TRIAL_EXTENSION');
    this.valueOffTypeSignal.set(null);
    this.applyTypeValidators('TRIAL_EXTENSION');
  }

  private handleError(err: HttpErrorLike, fallback: string): void {
    if (err?.status === 403) {
      this.router.navigate(['/case-files']);
      return;
    }
    const message = err?.error?.message ?? fallback;
    this.snackBar.open(message, 'Fermer', {
      duration: 5000, panelClass: ['snack-error'],
    });
  }

  // ---------------------------------------------------------------------------
  // UI helpers (consommés par le template)
  // ---------------------------------------------------------------------------

  typeLabel(type: PromoCodeType): string {
    return type === 'TRIAL_EXTENSION' ? 'Extension essai' : 'Réduction Stripe';
  }
}

/**
 * Structure minimaliste d'une erreur HttpClient sérialisée par
 * {@code GlobalExceptionHandler} backend (`{ error, message, code }`).
 */
interface HttpErrorLike {
  status?: number;
  error?: {
    message?: string;
    code?: string;
  };
}
