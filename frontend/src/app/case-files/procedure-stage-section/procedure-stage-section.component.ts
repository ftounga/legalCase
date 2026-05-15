import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  Input,
  OnInit,
  signal
} from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProcedureStageService } from '../../core/services/procedure-stage.service';
import {
  ProcedurePositionOption,
  ProcedureStage,
  ProcedureStageOption,
  ProcedureStageOptions,
  ProcedureStageUpdate
} from '../../core/models/procedure-stage.model';

/**
 * F-243 / SF-243-02 — Encart « Stade procédural » du dossier.
 *
 * Affiche le stade procédural courant (juridiction / stade / position) et
 * permet de l'éditer via 3 sélecteurs `<select>` natifs en cascade :
 *  - le stade est filtré sur la juridiction choisie ;
 *  - la position est filtrée sur le stade choisi.
 *
 * Standalone, OnPush + signals. Comme l'état est muté dans des `subscribe()`,
 * un `ChangeDetectorRef.markForCheck()` est appelé dans `next` ET `error`
 * (cf. mémoire `feedback_onpush_subscribe_markforcheck`).
 *
 * F-243 n'est PAS un outil décisionnel : pas de `TOOL_REGISTRY`, pas de
 * gate F-IA-03, pas de pré-fill IA. Encart de saisie de métadonnée du dossier.
 */
@Component({
  selector: 'app-procedure-stage-section',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './procedure-stage-section.component.html',
  styleUrl: './procedure-stage-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProcedureStageSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  /** Domaine juridique du dossier (DROIT_DU_TRAVAIL, etc.). */
  @Input() legalDomain!: string;
  /** Pays du workspace (FRANCE | BELGIQUE). */
  @Input() workspaceCountry = 'FRANCE';

  /** Référentiel des valeurs possibles (chargé une fois). */
  readonly options = signal<ProcedureStageOptions | null>(null);
  /** Stade procédural courant persisté. */
  readonly current = signal<ProcedureStage | null>(null);
  /** Vrai si le chargement initial a échoué — encart indisponible. */
  readonly unavailable = signal(false);
  /** Mode édition du formulaire. */
  readonly editing = signal(false);
  /** Sauvegarde en cours. */
  readonly saving = signal(false);

  /** Valeurs en cours d'édition (null = champ vide). */
  readonly formJurisdiction = signal<string | null>(null);
  readonly formStage = signal<string | null>(null);
  readonly formPosition = signal<string | null>(null);

  /** Stades filtrés sur la juridiction sélectionnée. */
  readonly availableStages = computed<ProcedureStageOption[]>(() => {
    const opts = this.options();
    const jur = this.formJurisdiction();
    if (!opts || !jur) {
      return [];
    }
    return opts.stages.filter(s => s.jurisdictionCode === jur);
  });

  /** Positions filtrées sur le stade sélectionné. */
  readonly availablePositions = computed<ProcedurePositionOption[]>(() => {
    const opts = this.options();
    const stage = this.formStage();
    if (!opts || !stage) {
      return [];
    }
    return opts.positions.filter(p => p.stageCodes.includes(stage));
  });

  /** Vrai si au moins un champ du stade courant est renseigné. */
  readonly hasValue = computed<boolean>(() => {
    const c = this.current();
    return !!c && (!!c.jurisdiction || !!c.stage || !!c.position);
  });

  constructor(
    private procedureStageService: ProcedureStageService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.procedureStageService.getOptions(this.legalDomain, this.workspaceCountry).subscribe({
      next: opts => {
        this.options.set(opts);
        this.cdr.markForCheck();
      },
      error: () => {
        this.unavailable.set(true);
        this.snackBar.open(
          'Stade procédural indisponible — impossible de charger le référentiel.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] }
        );
        this.cdr.markForCheck();
      }
    });

    this.procedureStageService.get(this.caseFileId).subscribe({
      next: stage => {
        this.current.set(stage);
        this.cdr.markForCheck();
      },
      error: () => {
        this.unavailable.set(true);
        this.snackBar.open(
          'Stade procédural indisponible — impossible de charger les données du dossier.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] }
        );
        this.cdr.markForCheck();
      }
    });
  }

  /** Ouvre le formulaire d'édition pré-rempli avec le stade courant. */
  startEdit(): void {
    const c = this.current();
    this.formJurisdiction.set(c?.jurisdiction ?? null);
    this.formStage.set(c?.stage ?? null);
    this.formPosition.set(c?.position ?? null);
    this.editing.set(true);
  }

  /** Annule l'édition sans persister. */
  cancelEdit(): void {
    this.editing.set(false);
  }

  /**
   * Changement de juridiction → réinitialise stade + position en cascade.
   * `value` vide ('') est normalisé en `null`.
   */
  onJurisdictionChange(value: string): void {
    this.formJurisdiction.set(value || null);
    this.formStage.set(null);
    this.formPosition.set(null);
  }

  /** Changement de stade → réinitialise la position en cascade. */
  onStageChange(value: string): void {
    this.formStage.set(value || null);
    this.formPosition.set(null);
  }

  /** Changement de position. */
  onPositionChange(value: string): void {
    this.formPosition.set(value || null);
  }

  /** Persiste la combinaison courante (les 3 champs peuvent être null). */
  save(): void {
    const payload: ProcedureStageUpdate = {
      jurisdiction: this.formJurisdiction(),
      stage: this.formStage(),
      position: this.formPosition()
    };
    this.saving.set(true);
    this.procedureStageService.update(this.caseFileId, payload).subscribe({
      next: stage => {
        this.current.set(stage);
        this.editing.set(false);
        this.saving.set(false);
        this.snackBar.open('Stade procédural enregistré.', 'Fermer', {
          duration: 3000,
          panelClass: ['snack-success']
        });
        this.cdr.markForCheck();
      },
      error: err => {
        this.saving.set(false);
        const msg = err?.error?.message || err?.error
          || 'Erreur lors de l\'enregistrement du stade procédural.';
        this.snackBar.open(msg, 'Fermer', {
          duration: 5000,
          panelClass: ['snack-error']
        });
        // Le formulaire reste ouvert et éditable.
        this.cdr.markForCheck();
      }
    });
  }
}
