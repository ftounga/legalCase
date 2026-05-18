import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../shared/confirm-dialog/confirm-dialog.component';
import { JurisprudenceCitationService } from '../../core/services/jurisprudence-citation.service';
import { JurisprudenceCitation } from '../../core/models/jurisprudence-citation.model';

/**
 * F-242 SF-242-02 — zone compacte « Jurisprudence à l'appui » d'un point
 * juridique.
 *
 * <p>Placée par point juridique de la synthèse, adjacente au bouton deeplink
 * F-241 (ordre aller F-241 → retour F-242). Permet de lister / ajouter /
 * éditer / supprimer les citations d'appui via les endpoints SF-242-01.</p>
 *
 * <p>Discrète quand le point n'a aucune citation : un appel à l'action replié
 * « + Jurisprudence à l'appui » déplie le formulaire d'ajout — pas de
 * formulaire déployé en permanence.</p>
 *
 * <p>Distinct de la section F-179 « Jurisprudences citées » (vérification des
 * arrêts présents dans les documents) : aucune fusion.</p>
 */
@Component({
  selector: 'app-jurisprudence-appui',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
  ],
  templateUrl: './jurisprudence-appui.component.html',
  styleUrl: './jurisprudence-appui.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JurisprudenceAppuiComponent {
  /** Identifiant du dossier — requis pour les appels API. */
  @Input({ required: true }) caseFileId!: string;

  /** Index du point juridique dans la synthèse (envoyé au backend à l'ajout). */
  @Input({ required: true }) pointJuridiqueIndex!: number;

  /** Texte du point juridique (snapshot envoyé au backend à l'ajout). */
  @Input({ required: true }) pointJuridiqueTexte!: string;

  /** Citations initiales du point (réparties par le parent au chargement). */
  @Input()
  set citations(value: JurisprudenceCitation[] | null | undefined) {
    this.citationsSignal.set(value ? [...value] : []);
  }

  /** Émis après toute mutation (ajout / édition / suppression réussie). */
  @Output() readonly changed = new EventEmitter<void>();

  /** Citations du point, état local mutable. */
  readonly citationsSignal = signal<JurisprudenceCitation[]>([]);

  /** true quand le formulaire d'ajout est déplié. */
  readonly addFormOpen = signal(false);

  /** id de la citation en cours d'édition, null si aucune. */
  readonly editingId = signal<string | null>(null);

  /** Saisie du formulaire d'ajout. */
  newReference = '';
  newPortee = '';

  /** Saisie du formulaire d'édition. */
  editReference = '';
  editPortee = '';

  /** true pendant un appel API (désactive les boutons). */
  readonly saving = signal(false);

  constructor(
    private readonly citationService: JurisprudenceCitationService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  /** Déplie le formulaire d'ajout (appel à l'action repliée). */
  openAddForm(): void {
    this.addFormOpen.set(true);
  }

  /** Replie le formulaire d'ajout et réinitialise la saisie. */
  cancelAdd(): void {
    this.addFormOpen.set(false);
    this.newReference = '';
    this.newPortee = '';
  }

  /** `reference` non vide requise pour activer le bouton « Ajouter ». */
  canAdd(): boolean {
    return this.newReference.trim().length > 0 && !this.saving();
  }

  /** `reference` non vide requise pour activer le bouton « Enregistrer ». */
  canSaveEdit(): boolean {
    return this.editReference.trim().length > 0 && !this.saving();
  }

  /** POST — crée une citation pour le point juridique courant. */
  add(): void {
    if (!this.canAdd()) return;
    this.saving.set(true);
    this.citationService
      .create(this.caseFileId, {
        pointJuridiqueIndex: this.pointJuridiqueIndex,
        pointJuridiqueTexte: this.pointJuridiqueTexte,
        reference: this.newReference.trim(),
        portee: this.normalize(this.newPortee),
      })
      .subscribe({
        next: created => {
          this.citationsSignal.update(list => [...list, created]);
          this.newReference = '';
          this.newPortee = '';
          this.addFormOpen.set(false);
          this.saving.set(false);
          this.changed.emit();
          this.cdr.markForCheck();
        },
        error: () => {
          this.saving.set(false);
          this.notifyError('Échec de l\'ajout de la citation.');
          this.cdr.markForCheck();
        },
      });
  }

  /** Ouvre le formulaire d'édition d'une citation, pré-rempli. */
  startEdit(citation: JurisprudenceCitation): void {
    this.editingId.set(citation.id);
    this.editReference = citation.reference;
    this.editPortee = citation.portee ?? '';
  }

  /** Annule l'édition en cours. */
  cancelEdit(): void {
    this.editingId.set(null);
    this.editReference = '';
    this.editPortee = '';
  }

  /** PUT — enregistre la citation en cours d'édition. */
  saveEdit(citation: JurisprudenceCitation): void {
    if (!this.canSaveEdit()) return;
    this.saving.set(true);
    this.citationService
      .update(this.caseFileId, citation.id, {
        reference: this.editReference.trim(),
        portee: this.normalize(this.editPortee),
      })
      .subscribe({
        next: updated => {
          this.citationsSignal.update(list =>
            list.map(c => (c.id === updated.id ? updated : c)),
          );
          this.editingId.set(null);
          this.editReference = '';
          this.editPortee = '';
          this.saving.set(false);
          this.changed.emit();
          this.cdr.markForCheck();
        },
        error: () => {
          this.saving.set(false);
          this.notifyError('Échec de la modification de la citation.');
          this.cdr.markForCheck();
        },
      });
  }

  /** Demande confirmation puis DELETE la citation. */
  remove(citation: JurisprudenceCitation): void {
    const data: ConfirmDialogData = {
      title: 'Supprimer la citation',
      message: `Supprimer « ${citation.reference} » de la jurisprudence d'appui de ce point juridique ?`,
      confirmLabel: 'Supprimer',
      confirmColor: 'warn',
    };
    this.dialog
      .open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed()
      .subscribe(confirmed => {
        if (!confirmed) return;
        this.saving.set(true);
        this.cdr.markForCheck();
        this.citationService.delete(this.caseFileId, citation.id).subscribe({
          next: () => {
            this.citationsSignal.update(list =>
              list.filter(c => c.id !== citation.id),
            );
            if (this.editingId() === citation.id) {
              this.editingId.set(null);
            }
            this.saving.set(false);
            this.changed.emit();
            this.cdr.markForCheck();
          },
          error: () => {
            this.saving.set(false);
            this.notifyError('Échec de la suppression de la citation.');
            this.cdr.markForCheck();
          },
        });
      });
  }

  /** Normalise une portée saisie : chaîne vide → null. */
  private normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }

  /** Snackbar d'erreur non bloquante (la saisie n'est pas perdue). */
  private notifyError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: ['snack-error'],
    });
  }
}
