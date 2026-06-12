import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import {
  ConclusionDocumentComponent,
  PieceRef,
} from '../../conclusion-document/conclusion-document.component';

/**
 * F-281 — Données de la modale d'aperçu avant export.
 *
 * `content` est le **contenu complet d'export** (markdown augmenté : en-tête de
 * cabinet + cartouche métadonnées + corps + bloc signature), c.-à-d. exactement
 * ce que recevront `DocxExportService` / `PdfExportService`. Les callbacks
 * délèguent au parent les exports Word / PDF (mêmes appels que les boutons de
 * page) ⇒ le fichier téléchargé correspond pixel pour pixel à l'aperçu.
 */
export interface ExportPreviewDialogData {
  /** Markdown complet d'export (rendu via `<app-conclusion-document>`). */
  content: string;
  /** Pièces numérotées (info-bulles fait → pièce, F-266) — facultatif. */
  pieces?: readonly PieceRef[];
  /** Lance l'export Word (délégué au parent). */
  onDownloadWord: () => void;
  /** Lance l'export PDF (délégué au parent). */
  onDownloadPdf: () => void;
}

/**
 * F-281 — Modale d'**aperçu avant téléchargement** des conclusions.
 *
 * <p>Lève le « téléchargement aveugle » : l'avocat voit le rendu **fidèle** de
 * ce qui sera exporté (en-tête cabinet, métadonnées procédurales, corps, bloc
 * signature) avant de cliquer Word ou PDF. La fidélité est garantie **par
 * construction** : la modale réutilise `<app-conclusion-document>` (le même
 * composant de rendu dont dérive le pipeline d'export, SF-259-03) et lui passe
 * le même contenu augmenté que les services d'export.</p>
 *
 * <p>Présentation pure, OnPush, sans appel réseau : les boutons Word / PDF
 * délèguent au parent via les callbacks de `data`, puis ferment la modale.</p>
 */
@Component({
  selector: 'app-export-preview-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    ConclusionDocumentComponent,
  ],
  templateUrl: './export-preview-dialog.component.html',
  styleUrl: './export-preview-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportPreviewDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ExportPreviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ExportPreviewDialogData,
  ) {}

  /** Lance l'export Word puis ferme la modale. */
  downloadWord(): void {
    this.data.onDownloadWord();
    this.dialogRef.close();
  }

  /** Lance l'export PDF puis ferme la modale. */
  downloadPdf(): void {
    this.data.onDownloadPdf();
    this.dialogRef.close();
  }
}
