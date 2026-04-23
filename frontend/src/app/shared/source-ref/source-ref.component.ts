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

  /** Clic : résout la cible via documentName + piece match, puis ouvre DocumentPreviewDialog. */
  openPreview(): void {
    if (!this.hasPreciseRef() || !this.caseFileId || this.opening()) return;
    this.opening.set(true);
    this.documentService.list(this.caseFileId).subscribe({
      next: (docs: Document[]) => {
        this.opening.set(false);
        const target = this.resolveTarget(docs);
        if (!target) {
          console.warn('[source-ref] Aucun document correspondant à', this.sourceRef);
          return;
        }
        const data: DocumentPreviewDialogData = {
          caseFileId: this.caseFileId!,
          documentId: target.doc.id,
          pieces: target.doc.pieces ?? [],
          initialPieceId: target.pieceId ?? undefined,
        };
        this.dialog.open(DocumentPreviewDialogComponent, { data, width: '900px', maxWidth: '95vw' });
      },
      error: () => {
        this.opening.set(false);
      }
    });
  }

  /**
   * Résout ({@link Document}, pieceId) cibles en 3 niveaux de fallback :
   * 1. filename strict → pieceId via match pièce
   * 2. filename normalisé (trim/lowercase/sans accents) → pieceId via match pièce
   * 3. global : la pièce (type + label + pageStart) dans n'importe quel doc
   *    — couvre le cas où l'IA produit un documentName invalide
   */
  private resolveTarget(docs: Document[]): { doc: Document; pieceId: string | null } | null {
    const ref = this.sourceRef!;
    const docName = ref.documentName ?? '';

    // Niveau 1 — filename strict
    let doc = docs.find(d => d.originalFilename === docName);

    // Niveau 2 — filename normalisé
    if (!doc && docName) {
      const normRef = normalize(docName);
      doc = docs.find(d => normalize(d.originalFilename) === normRef);
    }

    if (doc) {
      return { doc, pieceId: this.resolvePieceIdInDoc(doc) };
    }

    // Niveau 3 — recherche globale par pièce (type + label + pageStart)
    for (const d of docs) {
      const pieceId = this.resolvePieceIdInDoc(d);
      if (pieceId) return { doc: d, pieceId };
    }
    return null;
  }

  /**
   * Matche la pièce dans un document donné par (pieceType, pieceLabel, pageStart).
   * Retourne null si aucun critère ne fait match.
   */
  private resolvePieceIdInDoc(doc: Document): string | null {
    const ref = this.sourceRef!;
    const pieces = doc.pieces ?? [];
    if (pieces.length === 0) return null;

    const refLabel = (ref.pieceLabel ?? '').trim();
    const refType = ref.pieceType;
    const refPage = ref.pageStart;

    // Match exact : type + label + page
    const exact = pieces.find(p =>
      p.type === refType &&
      (p.label ?? '').trim() === refLabel &&
      p.pageStart === refPage
    );
    if (exact) return exact.id;

    // Match normalisé : type + label normalisé + page
    if (refLabel) {
      const normRefLabel = normalize(refLabel);
      const normMatch = pieces.find(p =>
        p.type === refType &&
        normalize(p.label ?? '') === normRefLabel &&
        p.pageStart === refPage
      );
      if (normMatch) return normMatch.id;
    }

    // type + page
    const byTypeAndPage = pieces.find(p => p.type === refType && p.pageStart === refPage);
    if (byTypeAndPage) return byTypeAndPage.id;

    // page incluse dans la plage de la pièce
    const byPageOnly = pieces.find(p => refPage != null
      && p.pageStart <= refPage && p.pageEnd >= refPage);
    return byPageOnly?.id ?? null;
  }
}

/** Normalise pour comparaisons tolérantes : lowercase, trim, sans accents. */
function normalize(s: string): string {
  return (s ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '') // retire les diacritiques
    .toLowerCase()
    .trim();
}
