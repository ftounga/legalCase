import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { SourceRefComponent } from './source-ref.component';
import { DocumentService } from '../../core/services/document.service';
import { Document } from '../../core/models/document.model';
import { DocumentPreviewDialogComponent } from '../../case-files/document-preview-dialog/document-preview-dialog.component';

describe('SourceRefComponent', () => {
  let component: SourceRefComponent;
  let fixture: ComponentFixture<SourceRefComponent>;
  let documentServiceSpy: jest.Mocked<DocumentService>;
  let dialogSpy: { open: jest.Mock };

  const doc: Document = {
    id: 'doc-1',
    caseFileId: 'cf-1',
    originalFilename: 'dossier.pdf',
    contentType: 'application/pdf',
    fileSize: 10_000,
    createdAt: '2026-04-19T10:00:00Z',
    pieces: [
      { id: 'p-1', type: 'CONTRAT', label: 'Contrat Dupont', pageStart: 1, pageEnd: 2, orderIndex: 0 },
      { id: 'p-2', type: 'ATTESTATION', label: 'Attestation Jean', pageStart: 3, pageEnd: 3, orderIndex: 1 }
    ]
  };

  async function setup(props: Partial<SourceRefComponent> = {}) {
    documentServiceSpy = { list: jest.fn().mockReturnValue(of([doc])) } as any;
    dialogSpy = { open: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [SourceRefComponent, NoopAnimationsModule],
      providers: [
        { provide: DocumentService, useValue: documentServiceSpy },
        { provide: MatDialog, useValue: dialogSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SourceRefComponent);
    component = fixture.componentInstance;
    Object.assign(component, props);
    fixture.detectChanges();
  }

  it('U-01 — rend le mode précis avec pieceLabel + docName + pages', async () => {
    await setup({
      caseFileId: 'cf-1',
      sourceRef: { documentName: 'dossier.pdf', pieceType: 'CONTRAT', pieceLabel: 'Contrat Dupont', pageStart: 1, pageEnd: 2 }
    });
    expect(component.hasPreciseRef()).toBe(true);
    expect(component.pieceLabelText()).toBe('Contrat Dupont');
    expect(component.docName()).toBe('dossier.pdf');
    expect(component.pageRange()).toBe('p. 1-2');
  });

  it('U-02 — rend le mode legacy quand sourceRef absent', async () => {
    await setup({ legacySource: 'ancien-doc.pdf' });
    expect(component.hasPreciseRef()).toBe(false);
    expect(component.docName()).toBe('ancien-doc.pdf');
    expect(component.pageRange()).toBeNull();
  });

  it('U-03 — utilise pieceTypeLabel quand pieceLabel est vide', async () => {
    await setup({
      sourceRef: { documentName: 'dossier.pdf', pieceType: 'SMS', pieceLabel: null, pageStart: 2, pageEnd: 2 }
    });
    expect(component.pieceLabelText()).toBe('SMS');
  });

  it('U-04 — affiche "p. X" au lieu de "p. X-X" quand pageStart === pageEnd', async () => {
    await setup({
      sourceRef: { documentName: 'dossier.pdf', pieceType: 'ATTESTATION', pieceLabel: 'A', pageStart: 3, pageEnd: 3 }
    });
    expect(component.pageRange()).toBe('p. 3');
  });

  it('U-05 — clic : ouvre DocumentPreviewDialog avec le bon documentId + initialPieceId', async () => {
    await setup({
      caseFileId: 'cf-1',
      sourceRef: { documentName: 'dossier.pdf', pieceType: 'CONTRAT', pieceLabel: 'Contrat Dupont', pageStart: 1, pageEnd: 2 }
    });
    component.openPreview();
    expect(documentServiceSpy.list).toHaveBeenCalledWith('cf-1');
    expect(dialogSpy.open).toHaveBeenCalledTimes(1);
    const [dialogCmp, cfg] = dialogSpy.open.mock.calls[0];
    expect(dialogCmp).toBe(DocumentPreviewDialogComponent);
    expect(cfg.data.documentId).toBe('doc-1');
    expect(cfg.data.initialPieceId).toBe('p-1');
  });

  it('U-06 — clic sans match documentName : no-op, pas de dialog ouvert', async () => {
    await setup({
      caseFileId: 'cf-1',
      sourceRef: { documentName: 'fichier-introuvable.pdf', pieceType: 'CONTRAT', pieceLabel: 'x', pageStart: 1, pageEnd: 1 }
    });
    component.openPreview();
    expect(documentServiceSpy.list).toHaveBeenCalled();
    expect(dialogSpy.open).not.toHaveBeenCalled();
  });

  it('U-07 — fallback par page quand pieceType/pieceLabel ne matchent pas', async () => {
    await setup({
      caseFileId: 'cf-1',
      sourceRef: { documentName: 'dossier.pdf', pieceType: null, pieceLabel: null, pageStart: 3, pageEnd: 3 }
    });
    component.openPreview();
    const [, cfg] = dialogSpy.open.mock.calls[0];
    expect(cfg.data.initialPieceId).toBe('p-2'); // page 3 fallback
  });

  it('U-08 — mode legacy : openPreview() est un no-op', async () => {
    await setup({ caseFileId: 'cf-1', legacySource: 'old.pdf' });
    component.openPreview();
    expect(documentServiceSpy.list).not.toHaveBeenCalled();
    expect(dialogSpy.open).not.toHaveBeenCalled();
  });
});
