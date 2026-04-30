import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { DecisionToolModalArgs, DecisionToolModalCloseValue } from './decision-tool-modal.service';

/**
 * F-177 SF-177-02 — Wrapper MatDialog hébergeant un composant outil décisionnel.
 *
 * Reçu par injection de `MAT_DIALOG_DATA` :
 * - `title`, `icon` → header
 * - `component`, `inputs` → instanciés via `*ngComponentOutlet`
 * - `onSave?` → si défini, bouton "Enregistrer" ; si retour true/Promise<true> ferme le dialog
 *
 * Le service `DecisionToolModalService.open(args)` est le point d'entrée recommandé.
 */
@Component({
  selector: 'app-decision-tool-modal',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './decision-tool-modal.component.html',
  styleUrls: ['./decision-tool-modal.component.scss'],
})
export class DecisionToolModalComponent {
  protected readonly data = inject<DecisionToolModalArgs>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject<MatDialogRef<DecisionToolModalComponent, DecisionToolModalCloseValue>>(
    MatDialogRef,
  );
  private readonly snackBar = inject(MatSnackBar);

  protected readonly saving = signal(false);

  protected get hasOnSave(): boolean {
    return typeof this.data.onSave === 'function';
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }

  protected async onSave(): Promise<void> {
    if (!this.data.onSave || this.saving()) {
      return;
    }
    this.saving.set(true);
    try {
      const result = await Promise.resolve(this.data.onSave());
      if (result) {
        this.dialogRef.close('saved');
      } else {
        this.saving.set(false);
      }
    } catch {
      this.snackBar.open('Erreur lors de l’enregistrement', 'Fermer', { duration: 5000 });
      this.saving.set(false);
    }
  }
}
