import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { DocumentPreviewDialogComponent, extractPagesRange } from './document-preview-dialog.component';
import { DocumentService } from '../../core/services/document.service';
import { DocumentPreview } from '../../core/models/document-preview.model';
import { DocumentPieceSummary } from '../../core/models/document.model';

describe('DocumentPreviewDialogComponent', () => {
  let component: DocumentPreviewDialogComponent;
  let fixture: ComponentFixture<DocumentPreviewDialogComponent>;
  let documentServiceSpy: jest.Mocked<DocumentService>;

  const base: DocumentPreview = {
    fileName: 'contrat.pdf',
    mimeType: 'application/pdf',
    fileSize: 500_000,
    pageCount: 3,
    uploadedAt: '2026-04-19T10:00:00Z',
    extractionStatus: 'DONE',
    extractionMethod: 'CLASSIC',
    extractedText: 'Contenu du contrat.',
    charCount: 19,
    textTruncated: false,
    ocrPagesUsed: 0,
    failureReason: null,
  };

  async function setup(preview: DocumentPreview, pieces: DocumentPieceSummary[] = [], initialPieceId?: string) {
    TestBed.resetTestingModule();
    documentServiceSpy = {
      preview: jest.fn().mockReturnValue(of(preview))
    } as any;

    await TestBed.configureTestingModule({
      imports: [DocumentPreviewDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: { caseFileId: 'cf-1', documentId: 'd-1', pieces, initialPieceId } },
        { provide: DocumentService, useValue: documentServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentPreviewDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('affiche le filename et le badge "Extraction classique" pour un CLASSIC DONE', async () => {
    await setup(base);
    expect(component.preview()?.fileName).toBe('contrat.pdf');
    expect(component.badgeClass()).toBe('badge--classic');
    expect(component.badgeLabel()).toBe('Extraction classique');
  });

  it('affiche le badge "OCR Textract" pour une extraction OCR', async () => {
    await setup({ ...base, extractionMethod: 'OCR', ocrPagesUsed: 5 });
    expect(component.badgeClass()).toBe('badge--ocr');
    expect(component.badgeLabel()).toBe('OCR Textract');
  });

  it('affiche le badge "Échec extraction" pour un FAILED', async () => {
    await setup({ ...base, extractionStatus: 'FAILED', extractionMethod: 'NONE',
                  extractedText: null, failureReason: 'EMPTY_TEXT' });
    expect(component.badgeClass()).toBe('badge--error');
    expect(component.badgeLabel()).toBe('Échec extraction');
  });

  it('détecte un texte vide comme isTextEmpty', async () => {
    await setup({ ...base, extractedText: '   ' });
    expect(component.isTextEmpty()).toBe(true);
  });

  it('texte non vide : isTextEmpty retourne false', async () => {
    await setup(base);
    expect(component.isTextEmpty()).toBe(false);
  });

  it('isPdf true pour application/pdf', async () => {
    await setup(base);
    expect(component.isPdf()).toBe(true);
  });

  it('isPdf false pour autre mimeType', async () => {
    await setup({ ...base, mimeType: 'text/plain' });
    expect(component.isPdf()).toBe(false);
  });

  // SF-145-02 : sidebar pièces + sélection
  describe('SF-145-02 — sidebar pièces navigable', () => {
    const pieces: DocumentPieceSummary[] = [
      { id: 'p1', type: 'CONTRAT', label: 'Contrat Dupont', pageStart: 1, pageEnd: 3, orderIndex: 0 },
      { id: 'p2', type: 'PIECE_IDENTITE', label: 'CNI Jean Dupont', pageStart: 4, pageEnd: 4, orderIndex: 1 },
      { id: 'p3', type: 'ATTESTATION', label: 'Attestation collègue', pageStart: 5, pageEnd: 5, orderIndex: 2 },
    ];

    it('U-01 : 3 pièces → sidebar rendue, 1re sélectionnée par défaut', async () => {
      await setup(base, pieces);
      expect(component.hasMultiplePieces()).toBe(true);
      expect(component.selectedPieceId()).toBe('p1');
      const sidebar = fixture.nativeElement.querySelector('.pieces-sidebar');
      expect(sidebar).toBeTruthy();
      const items = fixture.nativeElement.querySelectorAll('.piece-item');
      expect(items.length).toBe(3);
    });

    it('U-02 : initialPieceId respecté', async () => {
      await setup(base, pieces, 'p2');
      expect(component.selectedPieceId()).toBe('p2');
    });

    it('U-03 : selectPiece change la sélection', async () => {
      await setup(base, pieces);
      component.selectPiece('p3');
      fixture.detectChanges();
      expect(component.selectedPieceId()).toBe('p3');
      expect(component.selectedPiece()?.label).toBe('Attestation collègue');
    });

    it('U-04 : 1 seule pièce → pas de sidebar', async () => {
      await setup(base, [pieces[0]]);
      expect(component.hasMultiplePieces()).toBe(false);
      const sidebar = fixture.nativeElement.querySelector('.pieces-sidebar');
      expect(sidebar).toBeFalsy();
    });

    it('U-05 : aucune pièce → pas de sidebar (comportement legacy docs pré-F-145)', async () => {
      await setup(base, []);
      expect(component.hasMultiplePieces()).toBe(false);
      expect(component.selectedPieceId()).toBeNull();
    });

    it('U-06 : pieceHeaderLabel formate correctement "label · p. X-Y"', async () => {
      await setup(base, pieces);
      expect(component.pieceHeaderLabel(pieces[0])).toBe('Contrat Dupont · p. 1–3');
      expect(component.pieceHeaderLabel(pieces[1])).toBe('CNI Jean Dupont · p. 4');
    });

    // SF-145-08 : displayedExtractedText filtre par pièce sélectionnée via marqueurs
    it('U-07 : displayedExtractedText renvoie seulement les pages de la pièce sélectionnée', async () => {
      const textWithMarkers =
        '=== PAGE 1 ===\nContrat page 1\n\n=== PAGE 2 ===\nContrat page 2\n\n=== PAGE 3 ===\nSMS Anne';
      const localPieces: DocumentPieceSummary[] = [
        { id: 'c', type: 'CONTRAT', label: 'Contrat', pageStart: 1, pageEnd: 2, orderIndex: 0 },
        { id: 's', type: 'SMS', label: 'SMS Anne', pageStart: 3, pageEnd: 3, orderIndex: 1 },
      ];
      await setup({ ...base, extractedText: textWithMarkers }, localPieces, 's');

      const displayed = component.displayedExtractedText();
      expect(displayed).toContain('SMS Anne');
      expect(displayed).not.toContain('Contrat page 1');
      expect(displayed).not.toContain('Contrat page 2');
    });
  });

  // SF-148-02 : affichage description visuelle Claude Vision
  describe('SF-148-02 — vision description display', () => {
    const pieces: DocumentPieceSummary[] = [
      { id: 'p1', type: 'SMS', label: 'Échanges Anne', pageStart: 1, pageEnd: 1, orderIndex: 0,
        visualDescription: 'Conversation entre deux numéros, à gauche en vert Fatima, à droite en gris Anne.' },
      { id: 'p2', type: 'CONTRAT', label: 'CDI Dupont', pageStart: 2, pageEnd: 4, orderIndex: 1 },
    ];

    it('U-01 — piece enrichie visible → badge Vision + panneau description affichés', async () => {
      await setup(base, pieces);
      const el: HTMLElement = fixture.nativeElement;
      const badges = el.querySelectorAll('.selected-piece-badge');
      expect(badges.length).toBe(1);
      const panel = el.querySelector('.vision-panel-body');
      expect(panel).not.toBeNull();
      expect(panel!.textContent).toContain('Fatima');
    });

    it('U-02 — selection bascule vers piece non enrichie → badge et panneau disparaissent', async () => {
      await setup(base, pieces);
      component.selectPiece('p2');
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.selected-piece-badge')).toBeNull();
      expect(el.querySelector('.vision-panel-body')).toBeNull();
    });

    it('U-03 — sidebar : pièce enrichie a l\'icône visibility', async () => {
      await setup(base, pieces);
      const el: HTMLElement = fixture.nativeElement;
      const visionIcons = el.querySelectorAll('.piece-item-vision');
      expect(visionIcons.length).toBe(1);
    });

    it('U-05 — SF-148-03 : piece PENDING affiche un spinner dans la sidebar + encart "en cours"', async () => {
      const pending: DocumentPieceSummary[] = [
        { id: 'p1', type: 'SMS', label: 'En attente', pageStart: 1, pageEnd: 1, orderIndex: 0,
          visionStatus: 'PENDING' },
        { id: 'p2', type: 'CONTRAT', label: 'CDI', pageStart: 2, pageEnd: 3, orderIndex: 1,
          visionStatus: 'NOT_APPLICABLE' },
      ];
      await setup(base, pending);
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelectorAll('.piece-item-vision-spinner').length).toBe(1);
      expect(el.querySelector('.selected-piece-badge--pending')).not.toBeNull();
      expect(el.querySelector('.vision-panel--pending')).not.toBeNull();
      expect(el.querySelector('.vision-panel--pending')?.textContent).toContain('LegalCase Vision');
    });

    it('U-04 — aucune pièce enrichie → pas de badge/panel/icône dans la sidebar', async () => {
      const plain: DocumentPieceSummary[] = [
        { id: 'p1', type: 'CONTRAT', label: 'CDI', pageStart: 1, pageEnd: 2, orderIndex: 0 },
        { id: 'p2', type: 'LETTRE', label: 'Licenciement', pageStart: 3, pageEnd: 3, orderIndex: 1 },
      ];
      await setup(base, plain);
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.selected-piece-badge')).toBeNull();
      expect(el.querySelector('.vision-panel-body')).toBeNull();
      expect(el.querySelectorAll('.piece-item-vision').length).toBe(0);
    });
  });

  // SF-145-08 : extractPagesRange unit tests (export pur)
  describe('SF-145-08 — extractPagesRange', () => {
    const text =
      '=== PAGE 1 ===\nPage 1 content\n\n=== PAGE 2 ===\nPage 2 content\n\n=== PAGE 3 ===\nPage 3 content';

    it('retourne une seule page quand pageStart === pageEnd', () => {
      const result = extractPagesRange(text, 2, 2);
      expect(result).toContain('Page 2 content');
      expect(result).not.toContain('Page 1 content');
      expect(result).not.toContain('Page 3 content');
    });

    it('retourne plusieurs pages contiguës', () => {
      const result = extractPagesRange(text, 1, 2);
      expect(result).toContain('Page 1 content');
      expect(result).toContain('Page 2 content');
      expect(result).not.toContain('Page 3 content');
    });

    it('texte sans marqueurs → fallback texte complet (docs pré-SF-145-07)', () => {
      const legacy = 'Ancien texte sans marqueurs de page';
      expect(extractPagesRange(legacy, 1, 1)).toBe(legacy);
    });

    it('aucune page dans la plage → chaîne vide', () => {
      expect(extractPagesRange(text, 99, 99)).toBe('');
    });
  });
});
