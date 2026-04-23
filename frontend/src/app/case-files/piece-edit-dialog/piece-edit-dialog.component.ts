import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DocumentService } from '../../core/services/document.service';
import {
  DocumentPieceSummary, DocumentPieceType,
  documentPieceTypeLabel, documentPieceTypeIcon,
} from '../../core/models/document.model';

export interface PieceEditDialogData {
  caseFileId: string;
  documentId: string;
  piece: DocumentPieceSummary;
  /** Types autorisés pour le domaine du workspace courant. */
  allowedTypes: DocumentPieceType[];
}

/**
 * SF-145-11 : dialog d'édition du type + label d'une pièce. Permet à
 * l'avocat de corriger une classification erronée de l'IA.
 */
@Component({
  selector: 'app-piece-edit-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatDialogModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatSelectModule, MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './piece-edit-dialog.component.html',
  styleUrl: './piece-edit-dialog.component.scss',
})
export class PieceEditDialogComponent {
  type: DocumentPieceType;
  label: string;
  readonly submitting = signal(false);

  readonly pieceLabel = documentPieceTypeLabel;
  readonly pieceIcon = documentPieceTypeIcon;

  private readonly dialogRef = inject<MatDialogRef<PieceEditDialogComponent, DocumentPieceSummary | null>>(MatDialogRef);
  private readonly documentService = inject(DocumentService);
  private readonly snackBar = inject(MatSnackBar);

  constructor(@Inject(MAT_DIALOG_DATA) public data: PieceEditDialogData) {
    this.type = data.piece.type;
    this.label = data.piece.label ?? '';
  }

  /** Types triés pour affichage (labels humains). */
  get sortedTypes(): DocumentPieceType[] {
    return [...this.data.allowedTypes].sort((a, b) =>
      documentPieceTypeLabel(a).localeCompare(documentPieceTypeLabel(b), 'fr')
    );
  }

  get hasChanges(): boolean {
    const cleanedLabel = (this.label ?? '').trim();
    const originalLabel = (this.data.piece.label ?? '').trim();
    return this.type !== this.data.piece.type || cleanedLabel !== originalLabel;
  }

  cancel(): void {
    if (this.submitting()) return;
    this.dialogRef.close(null);
  }

  save(): void {
    if (this.submitting() || !this.hasChanges) return;
    this.submitting.set(true);
    this.documentService.updatePiece(
      this.data.caseFileId, this.data.documentId, this.data.piece.id,
      this.type, this.label.trim() || null
    ).subscribe({
      next: (updated) => {
        this.submitting.set(false);
        this.snackBar.open('Pièce reclassifiée', 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
        this.dialogRef.close(updated);
      },
      error: () => {
        this.submitting.set(false);
        this.snackBar.open(
          'Impossible de reclassifier la pièce. Réessayez.',
          'Fermer',
          { duration: 5000, panelClass: ['snack-error'] }
        );
      }
    });
  }
}
