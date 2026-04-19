import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { DocumentPreviewDialogComponent } from './document-preview-dialog.component';
import { DocumentService } from '../../core/services/document.service';
import { DocumentPreview } from '../../core/models/document-preview.model';

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

  async function setup(preview: DocumentPreview) {
    documentServiceSpy = {
      preview: jest.fn().mockReturnValue(of(preview))
    } as any;

    await TestBed.configureTestingModule({
      imports: [DocumentPreviewDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: { caseFileId: 'cf-1', documentId: 'd-1' } },
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
});
