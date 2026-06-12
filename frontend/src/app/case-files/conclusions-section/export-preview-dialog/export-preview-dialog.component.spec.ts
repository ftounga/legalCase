import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  ExportPreviewDialogComponent,
  ExportPreviewDialogData,
} from './export-preview-dialog.component';

describe('F-281 — ExportPreviewDialogComponent', () => {
  let fixture: ComponentFixture<ExportPreviewDialogComponent>;
  let component: ExportPreviewDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<ExportPreviewDialogComponent>>;
  let data: ExportPreviewDialogData;
  let wordSpy: jasmine.Spy;
  let pdfSpy: jasmine.Spy;

  beforeEach(async () => {
    wordSpy = jasmine.createSpy('onDownloadWord');
    pdfSpy = jasmine.createSpy('onDownloadPdf');
    data = {
      content: '**Juridiction :** CPH\n\n---\n\n## FAITS\nLe salarié…',
      pieces: [],
      onDownloadWord: wordSpy,
      onDownloadPdf: pdfSpy,
    };
    dialogRefSpy = jasmine.createSpyObj<MatDialogRef<ExportPreviewDialogComponent>>(
      'MatDialogRef',
      ['close'],
    );

    await TestBed.configureTestingModule({
      imports: [ExportPreviewDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExportPreviewDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('rend le contenu augmenté via app-conclusion-document', () => {
    const doc = fixture.nativeElement.querySelector(
      '[data-testid="export-preview-document"]',
    );
    expect(doc).not.toBeNull();
    // Le rendu du markdown doit contenir le cartouche de métadonnées.
    expect(fixture.nativeElement.textContent).toContain('Juridiction');
  });

  it('bouton Word → invoque le callback puis ferme la modale', () => {
    component.downloadWord();
    expect(wordSpy).toHaveBeenCalledTimes(1);
    expect(dialogRefSpy.close).toHaveBeenCalledTimes(1);
  });

  it('bouton PDF → invoque le callback puis ferme la modale', () => {
    component.downloadPdf();
    expect(pdfSpy).toHaveBeenCalledTimes(1);
    expect(dialogRefSpy.close).toHaveBeenCalledTimes(1);
  });

  it('les boutons du template sont câblés sur les handlers', () => {
    const wordBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="export-preview-word-btn"]',
    );
    const pdfBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="export-preview-pdf-btn"]',
    );
    expect(wordBtn).not.toBeNull();
    expect(pdfBtn).not.toBeNull();
    wordBtn.click();
    expect(wordSpy).toHaveBeenCalled();
    pdfBtn.click();
    expect(pdfSpy).toHaveBeenCalled();
  });
});
