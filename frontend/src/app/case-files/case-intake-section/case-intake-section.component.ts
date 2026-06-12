import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseIntakeService } from '../../core/services/case-intake.service';
import {
  AdmissibilityStatus,
  CaseIntake,
  PrescriptionStatus,
} from '../../core/models/case-intake.model';

/**
 * F-285 / SF-285-01 — carte de qualification d'entrée du dossier (intake guidé).
 *
 * En tête de l'onglet Dossier, au-dessus de l'identité (F-243). Restitue un
 * verdict de valeur immédiat : badge de recevabilité + 4 pastilles (type de
 * litige · prescription · juridiction · valeur estimée). Non bloquante : la
 * qualification n'empêche jamais l'upload des pièces.
 *
 * Standalone, OnPush + signals. État muté dans des `subscribe()` →
 * `ChangeDetectorRef.markForCheck()` dans `next` ET `error`
 * (cf. mémoire `feedback_onpush_subscribe_markforcheck`).
 *
 * F-285 n'est PAS un outil décisionnel : pas de `TOOL_REGISTRY`, pas de gate
 * F-IA-03/04, pas de pré-fill IA. Encart de métadonnée de qualification (mirror F-243).
 */
@Component({
  selector: 'app-case-intake-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule],
  templateUrl: './case-intake-section.component.html',
  styleUrl: './case-intake-section.component.scss',
})
export class CaseIntakeSectionComponent implements OnInit {
  @Input({ required: true }) caseFileId!: string;

  private readonly service = inject(CaseIntakeService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly intake = signal<CaseIntake | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly editing = signal(false);
  /** Vrai si le chargement initial a échoué — carte indisponible (non bloquante). */
  readonly unavailable = signal(false);

  readonly qualified = computed(() => this.intake()?.qualified ?? false);

  readonly form = this.fb.group({
    disputeType: this.fb.control<string>('', Validators.maxLength(120)),
    admissibility: this.fb.control<AdmissibilityStatus | ''>(''),
    prescriptionStatus: this.fb.control<PrescriptionStatus | ''>(''),
    prescriptionDeadline: this.fb.control<string>(''),
    jurisdiction: this.fb.control<string>('', Validators.maxLength(120)),
    /** Valeur estimée saisie en EUR (entier), convertie en centimes à l'envoi. */
    estimatedValueEuros: this.fb.control<number | null>(null, Validators.min(0)),
    notes: this.fb.control<string>('', Validators.maxLength(1000)),
  });

  // ── Libellés humains ─────────────────────────────────────────────────────
  readonly admissibilityLabels: Record<AdmissibilityStatus, string> = {
    A_VERIFIER: 'À vérifier',
    RECEVABLE: 'Recevable',
    IRRECEVABLE: 'Irrecevable',
  };
  readonly prescriptionLabels: Record<PrescriptionStatus, string> = {
    A_VERIFIER: 'À vérifier',
    DANS_LES_DELAIS: 'Dans les délais',
    RISQUE_FORCLUSION: 'Risque de forclusion',
    PRESCRIT: 'Prescrit',
  };

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (i) => {
        this.intake.set(i);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.unavailable.set(true);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  startEdit(): void {
    const i = this.intake();
    this.form.reset({
      disputeType: i?.disputeType ?? '',
      admissibility: i?.admissibility ?? '',
      prescriptionStatus: i?.prescriptionStatus ?? '',
      prescriptionDeadline: i?.prescriptionDeadline ?? '',
      jurisdiction: i?.jurisdiction ?? '',
      estimatedValueEuros:
        i?.estimatedValueCents != null ? Math.round(i.estimatedValueCents / 100) : null,
      notes: i?.notes ?? '',
    });
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
  }

  save(): void {
    if (this.form.invalid || this.saving()) return;
    const raw = this.form.getRawValue();
    const euros = raw.estimatedValueEuros;
    const payload = {
      disputeType: raw.disputeType?.trim() || null,
      admissibility: (raw.admissibility || null) as AdmissibilityStatus | null,
      prescriptionStatus: (raw.prescriptionStatus || null) as PrescriptionStatus | null,
      prescriptionDeadline: raw.prescriptionDeadline || null,
      jurisdiction: raw.jurisdiction?.trim() || null,
      estimatedValueCents: euros != null && euros >= 0 ? Math.round(euros * 100) : null,
      notes: raw.notes?.trim() || null,
    };
    this.saving.set(true);
    this.service.update(this.caseFileId, payload).subscribe({
      next: (i) => {
        this.intake.set(i);
        this.editing.set(false);
        this.saving.set(false);
        this.snackBar.open('Qualification du dossier enregistrée.', 'Fermer', {
          duration: 3000,
          panelClass: ['snack-success'],
        });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.saving.set(false);
        const msg =
          err?.error?.message || 'Erreur lors de l’enregistrement de la qualification.';
        this.snackBar.open(msg, 'Fermer', { duration: 5000, panelClass: ['snack-error'] });
        // Le formulaire reste ouvert et éditable.
        this.cdr.markForCheck();
      },
    });
  }

  // ── Affichage du verdict ─────────────────────────────────────────────────
  admissibilityLabel(value: AdmissibilityStatus | null): string {
    return value ? this.admissibilityLabels[value] : 'À qualifier';
  }
  prescriptionLabel(value: PrescriptionStatus | null): string {
    return value ? this.prescriptionLabels[value] : 'À vérifier';
  }

  /** Valeur estimée formatée en euros (à partir des centimes), ou null. */
  estimatedValueDisplay(): string | null {
    const cents = this.intake()?.estimatedValueCents;
    if (cents == null) return null;
    return (cents / 100).toLocaleString('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    });
  }
}
