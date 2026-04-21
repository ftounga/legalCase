import { Component, Inject, ViewChild, ElementRef, signal, computed, AfterViewInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentService } from '../../core/services/document.service';
import { DocumentPreview } from '../../core/models/document-preview.model';
import {
  DocumentPieceSummary, documentPieceTypeIcon, documentPieceTypeLabel
} from '../../core/models/document.model';

export interface DocumentPreviewDialogData {
  caseFileId: string;
  documentId: string;
  /** SF-145-02 : pièces identifiées dans le document (vide si SF-145-01 n'a pas tourné). */
  pieces?: DocumentPieceSummary[];
  /** SF-145-02 : pièce à sélectionner à l'ouverture (sinon la 1ère). */
  initialPieceId?: string;
}

@Component({
  selector: 'app-document-preview-dialog',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe,
    MatButtonModule, MatDialogModule, MatIconModule,
    MatTabsModule, MatProgressSpinnerModule, MatTooltipModule
  ],
  templateUrl: './document-preview-dialog.component.html',
  styleUrl: './document-preview-dialog.component.scss',
})
export class DocumentPreviewDialogComponent implements AfterViewInit {
  preview = signal<DocumentPreview | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  activeTab = signal<number>(0);
  pdfRendering = signal(false);
  pdfError = signal<string | null>(null);
  /** Page actuellement rendue dans le canvas (pour éviter re-render si inchangé). */
  pdfRenderedPage = signal<number | null>(null);

  readonly pieces = signal<DocumentPieceSummary[]>([]);
  readonly selectedPieceId = signal<string | null>(null);

  readonly selectedPiece = computed<DocumentPieceSummary | null>(() => {
    const id = this.selectedPieceId();
    return this.pieces().find(p => p.id === id) ?? null;
  });

  readonly hasMultiplePieces = computed(() => this.pieces().length > 1);

  @ViewChild('pdfCanvas') pdfCanvas?: ElementRef<HTMLCanvasElement>;

  readonly isPdf = computed(() => this.preview()?.mimeType === 'application/pdf');

  readonly pieceTypeIcon = documentPieceTypeIcon;
  readonly pieceTypeLabel = documentPieceTypeLabel;

  constructor(
    public dialogRef: MatDialogRef<DocumentPreviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DocumentPreviewDialogData,
    private documentService: DocumentService
  ) {
    this.pieces.set(data.pieces ?? []);
    // Sélection initiale : pieceId explicite > 1ère pièce > rien.
    const initial = data.initialPieceId ?? this.pieces()[0]?.id ?? null;
    this.selectedPieceId.set(initial);

    this.documentService.preview(data.caseFileId, data.documentId).subscribe({
      next: p => {
        this.preview.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger l\'aperçu de ce document.');
        this.loading.set(false);
      }
    });
  }

  ngAfterViewInit(): void {
    // Le canvas n'existe que quand on est sur l'onglet 2 — le render se fera via onTabChange.
  }

  onTabChange(index: number): void {
    this.activeTab.set(index);
    if (index === 1 && this.isPdf()) {
      setTimeout(() => this.renderPdfForSelectedPiece(), 50);
    }
  }

  selectPiece(pieceId: string): void {
    if (this.selectedPieceId() === pieceId) return;
    this.selectedPieceId.set(pieceId);
    // Re-rend la page si on est sur l'onglet Aperçu
    if (this.activeTab() === 1 && this.isPdf()) {
      setTimeout(() => this.renderPdfForSelectedPiece(), 50);
    }
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }

  badgeClass(): string {
    const p = this.preview();
    if (!p) return '';
    if (p.extractionStatus === 'FAILED') return 'badge--error';
    if (p.extractionMethod === 'OCR') return 'badge--ocr';
    if (p.extractionMethod === 'CLASSIC') return 'badge--classic';
    return 'badge--pending';
  }

  badgeLabel(): string {
    const p = this.preview();
    if (!p) return '';
    if (p.extractionStatus === 'FAILED') return 'Échec extraction';
    if (p.extractionStatus !== 'DONE') return 'En cours';
    if (p.extractionMethod === 'OCR') return 'OCR Textract';
    return 'Extraction classique';
  }

  isTextEmpty(): boolean {
    const p = this.preview();
    if (!p || p.extractionStatus !== 'DONE') return false;
    return !p.extractedText || p.extractedText.trim().length === 0;
  }

  pieceHeaderLabel(piece: DocumentPieceSummary): string {
    const base = piece.label ?? documentPieceTypeLabel(piece.type);
    const pages = piece.pageStart === piece.pageEnd
      ? `p. ${piece.pageStart}`
      : `p. ${piece.pageStart}–${piece.pageEnd}`;
    return `${base} · ${pages}`;
  }

  private renderPdfForSelectedPiece(): void {
    const piece = this.selectedPiece();
    const pageIndex = piece ? piece.pageStart : 1;
    if (this.pdfRenderedPage() === pageIndex) return; // évite re-render inutile
    this.renderPdfPage(pageIndex);
  }

  private async renderPdfPage(pageIndex: number): Promise<void> {
    if (!this.pdfCanvas) return;
    this.pdfRendering.set(true);
    this.pdfError.set(null);
    try {
      const pdfjs = await import('pdfjs-dist');
      pdfjs.GlobalWorkerOptions.workerSrc = `/pdf.worker.min.mjs?v=${pdfjs.version}`;
      const pdfUrl = `/api/v1/case-files/${this.data.caseFileId}/documents/${this.data.documentId}/content`;
      const loadingTask = pdfjs.getDocument({ url: pdfUrl });
      const pdf = await loadingTask.promise;
      const safePageIndex = Math.min(Math.max(1, pageIndex), pdf.numPages);
      const page = await pdf.getPage(safePageIndex);
      const viewport = page.getViewport({ scale: 1 });
      const canvas = this.pdfCanvas.nativeElement;
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('No 2D context');

      const targetWidth = Math.min(600, canvas.parentElement?.clientWidth ?? 600);
      const scale = targetWidth / viewport.width;
      const scaledViewport = page.getViewport({ scale });
      canvas.width = scaledViewport.width;
      canvas.height = scaledViewport.height;

      await page.render({ canvasContext: ctx, viewport: scaledViewport }).promise;
      this.pdfRenderedPage.set(pageIndex);
    } catch (err) {
      console.error('PDF render failed', err);
      this.pdfError.set('Aperçu visuel indisponible pour ce document.');
      this.pdfRenderedPage.set(null);
    } finally {
      this.pdfRendering.set(false);
    }
  }
}
