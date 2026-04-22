import { Component, Input, computed, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { SourceRef } from '../../core/models/case-analysis.model';
import { DocumentService } from '../../core/services/document.service';
import { Document, documentPieceTypeLabel, documentPieceTypeIcon } from '../../core/models/document.model';
import {
  DocumentPreviewDialogComponent,
  DocumentPreviewDialogData
} from '../../case-files/document-preview-dialog/document-preview-dialog.component';

/**
 * F-146 SF-146-02 : badge de source cliquable.
 *
 * <p>Deux modes d'affichage :
 * <ul>
 *   <li><b>Précis</b> (sourceRef non null) : "CONTRAT « label » · doc.pdf · p. 1-2", cliquable
 *       → ouvre le {@link DocumentPreviewDialogComponent} pré-positionné sur la pièce.</li>
 *   <li><b>Legacy</b> (sourceRef null) : simple string `legacySource` non cliquable, comportement
 *       pré-F-146 conservé pour les analyses déjà en DB.</li>
 * </ul>
 */
@Component({
  selector: 'app-source-ref',
  standalone: true,
  imports: [MatIconModule, MatTooltipModule],
  templateUrl: './source-ref.component.html',
  styleUrl: './source-ref.component.scss',
})
export class SourceRefComponent {
  @Input() sourceRef: SourceRef | null | undefined;
  @Input() legacySource: string | null = null;
  @Input() extrait: string | null = null;
  @Input() caseFileId: string | null = null;

  readonly opening = signal(false);

  readonly hasPreciseRef = computed(() => {
    const ref = this.sourceRef;
    return !!ref && !!ref.documentName;
  });

  readonly docName = computed<string | null>(() => {
    if (this.hasPreciseRef()) return this.sourceRef!.documentName;
    return this.legacySource;
  });

  readonly pieceLabelText = computed<string | null>(() => {
    const ref = this.sourceRef;
    if (!ref) return null;
    if (ref.pieceLabel && ref.pieceLabel.trim().length > 0) return ref.pieceLabel;
    if (ref.pieceType) return documentPieceTypeLabel(ref.pieceType);
    return null;
  });

  readonly pieceTypeIconName = computed<string>(() => {
    const ref = this.sourceRef;
    if (ref?.pieceType) return documentPieceTypeIcon(ref.pieceType);
    return 'description';
  });

  readonly pageRange = computed<string | null>(() => {
    const ref = this.sourceRef;
    if (!ref || ref.pageStart == null) return null;
    if (ref.pageEnd == null || ref.pageEnd === ref.pageStart) return `p. ${ref.pageStart}`;
    return `p. ${ref.pageStart}-${ref.pageEnd}`;
  });

  constructor(
    private dialog: MatDialog,
    private documentService: DocumentService
  ) {}

  /** Clic : résout documentName → documentId puis ouvre DocumentPreviewDialog. */
  openPreview(): void {
    if (!this.hasPreciseRef() || !this.caseFileId || this.opening()) return;
    const docName = this.sourceRef!.documentName!;
    this.opening.set(true);
    this.documentService.list(this.caseFileId).subscribe({
      next: (docs: Document[]) => {
        this.opening.set(false);
        const match = docs.find(d => d.originalFilename === docName);
        if (!match) return; // silencieux : doc supprimé ou analyse obsolète
        const initialPieceId = this.resolvePieceId(match);
        const data: DocumentPreviewDialogData = {
          caseFileId: this.caseFileId!,
          documentId: match.id,
          pieces: match.pieces ?? [],
          initialPieceId: initialPieceId ?? undefined,
        };
        this.dialog.open(DocumentPreviewDialogComponent, { data, width: '900px', maxWidth: '95vw' });
      },
      error: () => {
        this.opening.set(false);
      }
    });
  }

  /**
   * Matche la pièce dans le document par (pieceType, pieceLabel, pageStart).
   * Retourne null si aucun match — le dialog s'ouvrira sur la 1ère pièce du doc.
   */
  private resolvePieceId(doc: Document): string | null {
    const ref = this.sourceRef!;
    const pieces = doc.pieces ?? [];
    if (pieces.length === 0) return null;

    const exact = pieces.find(p =>
      p.type === ref.pieceType &&
      (p.label ?? '') === (ref.pieceLabel ?? '') &&
      p.pageStart === ref.pageStart
    );
    if (exact) return exact.id;

    const byTypeAndPage = pieces.find(p => p.type === ref.pieceType && p.pageStart === ref.pageStart);
    if (byTypeAndPage) return byTypeAndPage.id;

    const byPageOnly = pieces.find(p => ref.pageStart != null
      && p.pageStart <= ref.pageStart && p.pageEnd >= ref.pageStart);
    return byPageOnly?.id ?? null;
  }
}
