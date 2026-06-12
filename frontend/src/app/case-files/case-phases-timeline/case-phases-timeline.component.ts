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
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CasePhaseService } from '../../core/services/case-phase.service';
import {
  CASE_PHASE_OPTIONS,
  CasePhase,
  CasePhaseTimeline,
  CasePhaseType,
  casePhaseLabel,
} from '../../core/models/case-phase.model';

/**
 * F-283 / SF-283-01 — frise des phases procédurales (onglet Suivi, en tête).
 * Élève le dossier d'un instantané à une progression datée : où en est-il dans
 * son cycle juridictionnel (Saisine → Conciliation → Fond → Appel → Cassation…).
 * Distinct de la frise contradictoire F-282 (échanges) et du libellé statique
 * « stade procédural » F-243. Compacte : coiffe le contradictoire, ne le
 * concurrence pas (cf. SF-283-00b-ux-coherence — invariant anti-surcharge).
 */
@Component({
  selector: 'app-case-phases-timeline',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './case-phases-timeline.component.html',
  styleUrl: './case-phases-timeline.component.scss',
})
export class CasePhasesTimelineComponent implements OnInit {
  @Input({ required: true }) caseFileId!: string;

  private readonly service = inject(CasePhaseService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly phaseOptions = CASE_PHASE_OPTIONS;

  readonly timeline = signal<CasePhaseTimeline | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);

  readonly phases = computed<CasePhase[]>(() => this.timeline()?.phases ?? []);
  readonly currentPhase = computed<CasePhaseType | null>(() => this.timeline()?.currentPhase ?? null);

  readonly form = this.fb.group({
    phase: this.fb.nonNullable.control<CasePhaseType>('SAISINE', Validators.required),
    label: this.fb.control<string>('', Validators.maxLength(200)),
    enteredAt: this.fb.nonNullable.control<string>('', Validators.required),
    note: this.fb.control<string>('', Validators.maxLength(2000)),
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.service.timeline(this.caseFileId).subscribe({
      next: (t) => {
        this.timeline.set(t);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  openAdd(): void {
    this.editingId.set(null);
    this.form.reset({ phase: 'SAISINE', label: '', enteredAt: '', note: '' });
    this.showForm.set(true);
  }

  openEdit(phase: CasePhase): void {
    this.editingId.set(phase.id);
    this.form.reset({
      phase: phase.phase,
      label: phase.label ?? '',
      enteredAt: phase.enteredAt,
      note: phase.note ?? '',
    });
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    if (this.form.invalid || this.saving()) return;
    const raw = this.form.getRawValue();
    const input = {
      phase: raw.phase,
      label: raw.label?.trim() || null,
      enteredAt: raw.enteredAt,
      note: raw.note?.trim() || null,
    };
    this.saving.set(true);
    const id = this.editingId();
    const op$ = id
      ? this.service.update(this.caseFileId, id, input)
      : this.service.create(this.caseFileId, input);
    op$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.editingId.set(null);
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Échec de l’enregistrement de la phase.', 'Fermer', {
          duration: 5000,
          panelClass: ['snack-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }

  remove(phase: CasePhase): void {
    this.service.delete(this.caseFileId, phase.id).subscribe({
      next: () => this.load(),
      error: () => {
        this.snackBar.open('Échec de la suppression.', 'Fermer', {
          duration: 5000,
          panelClass: ['snack-error'],
        });
      },
    });
  }

  isCurrent(phase: CasePhase): boolean {
    const phases = this.phases();
    return phases.length > 0 && phases[phases.length - 1].id === phase.id;
  }

  phaseLabel(phase: CasePhaseType): string {
    return casePhaseLabel(phase);
  }
}
