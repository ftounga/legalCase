import { Component, Inject, ViewChild, ElementRef, signal, computed, AfterViewInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DocumentService } from '../../core/services/document.service';
import { DocumentPreview } from '../../core/models/document-preview.model';

export interface DocumentPreviewDialogData {
  caseFileId: string;
  documentId: string;
}

@Component({
  selector: 'app-document-preview-dialog',
  standalone: true,
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatDialogModule, MatIconModule, MatTabsModule, MatProgressSpinnerModule],
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
  pdfRendered = signal(false);

  @ViewChild('pdfCanvas') pdfCanvas?: ElementRef<HTMLCanvasElement>;

  readonly isPdf = computed(() => this.preview()?.mimeType === 'application/pdf');

  constructor(
    public dialogRef: MatDialogRef<DocumentPreviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DocumentPreviewDialogData,
    private documentService: DocumentService
  ) {
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
    if (index === 1 && !this.pdfRendered() && this.isPdf()) {
      // Laisse le temps au template de rendre le canvas avant de dessiner
      setTimeout(() => this.renderPdfFirstPage(), 50);
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

  private async renderPdfFirstPage(): Promise<void> {
    if (!this.pdfCanvas || this.pdfRendered()) return;
    this.pdfRendering.set(true);
    this.pdfError.set(null);
    try {
      // Import dynamique : évite de gonfler le bundle initial.
      const pdfjs = await import('pdfjs-dist');
      // Worker via CDN officiel (même version que la lib npm, fallback si dist local indispo)
      pdfjs.GlobalWorkerOptions.workerSrc =
        `https://cdn.jsdelivr.net/npm/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

      const pdfUrl = `/api/v1/case-files/${this.data.caseFileId}/documents/${this.data.documentId}/download`;
      const loadingTask = pdfjs.getDocument({ url: pdfUrl, withCredentials: true });
      const pdf = await loadingTask.promise;
      const page = await pdf.getPage(1);
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
      this.pdfRendered.set(true);
    } catch (err) {
      console.error('PDF render failed', err);
      this.pdfError.set('Aperçu visuel indisponible pour ce document.');
    } finally {
      this.pdfRendering.set(false);
    }
  }
}
