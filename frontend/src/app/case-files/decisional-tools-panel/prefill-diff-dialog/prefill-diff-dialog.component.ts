import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface PrefillDiffEntry {
  toolId: string;
  label: string;
  icon: string;
  delta: number;
}

export interface PrefillDiffDialogData {
  entries: PrefillDiffEntry[];
}

@Component({
  selector: 'app-prefill-diff-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Outils enrichis par l'analyse</h2>
    <mat-dialog-content>
      @if (data.entries.length === 0) {
        <p class="empty">Aucun outil enrichi.</p>
      } @else {
        <ul class="entries" role="list">
          @for (entry of data.entries; track entry.toolId) {
            <li class="entry">
              <mat-icon class="entry__icon">{{ entry.icon }}</mat-icon>
              <span class="entry__label">{{ entry.label }}</span>
              <span class="entry__delta">+{{ entry.delta }} {{ entry.delta === 1 ? 'champ' : 'champs' }}</span>
            </li>
          }
        </ul>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="close()">Fermer</button>
    </mat-dialog-actions>
  `,
  styleUrl: './prefill-diff-dialog.component.scss',
})
export class PrefillDiffDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<PrefillDiffDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: PrefillDiffDialogData,
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
