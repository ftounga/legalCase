import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CaseFileDetailComponent } from './case-file-detail.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { DocumentService } from '../../core/services/document.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { CaseFile } from '../../core/models/case-file.model';
import { Document } from '../../core/models/document.model';

const mockCaseFile: CaseFile = {
  id: 'cf1', title: 'Dossier A', legalDomain: 'EMPLOYMENT_LAW',
  description: 'Description test', status: 'OPEN', createdAt: '2026-03-17T10:00:00Z'
};

const mockDocument: Document = {
  id: 'doc1', caseFileId: 'cf1', originalFilename: 'contrat.pdf',
  contentType: 'application/pdf', fileSize: 12345, createdAt: '2026-03-17T10:00:00Z'
};

describe('CaseFileDetailComponent', () => {
  let fixture: ComponentFixture<CaseFileDetailComponent>;
  let component: CaseFileDetailComponent;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let documentServiceSpy: jasmine.SpyObj<DocumentService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['getById']);
    documentServiceSpy = jasmine.createSpyObj('DocumentService', ['list', 'upload']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    caseFileServiceSpy.getById.and.returnValue(of(mockCaseFile));
    documentServiceSpy.list.and.returnValue(of([mockDocument]));

    await TestBed.configureTestingModule({
      imports: [CaseFileDetailComponent],
      providers: [
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: DocumentService, useValue: documentServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'cf1' }) } } },
        provideAnimationsAsync()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseFileDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit — charge le dossier et les documents', () => {
    expect(caseFileServiceSpy.getById).toHaveBeenCalledWith('cf1');
    expect(documentServiceSpy.list).toHaveBeenCalledWith('cf1');
    expect(component.caseFile()).toEqual(mockCaseFile);
    expect(component.documents().length).toBe(1);
    expect(component.loading()).toBeFalse();
  });

  it('ngOnInit — erreur dossier → snackbar + loading false', () => {
    caseFileServiceSpy.getById.and.returnValue(throwError(() => new Error('404')));
    component.ngOnInit();
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  });

  it('loadDocuments — erreur → snackbar affiché', () => {
    documentServiceSpy.list.and.returnValue(throwError(() => new Error('500')));
    component.loadDocuments('cf1');
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('upload succès → document ajouté en tête de liste', () => {
    const newDoc: Document = { ...mockDocument, id: 'doc2', originalFilename: 'avenant.pdf' };
    documentServiceSpy.upload.and.returnValue(of(newDoc));

    const file = new File(['content'], 'avenant.pdf', { type: 'application/pdf' });
    const event = { target: { files: [file], value: '' } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.documents()[0].originalFilename).toBe('avenant.pdf');
    expect(component.uploading()).toBeFalse();
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('upload erreur → snackbar erreur', () => {
    documentServiceSpy.upload.and.returnValue(throwError(() => new Error('400')));

    const file = new File(['content'], 'bad.exe', { type: 'application/octet-stream' });
    const event = { target: { files: [file], value: '' } } as unknown as Event;

    component.onFileSelected(event);

    expect(component.uploading()).toBeFalse();
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('formatSize — octets', () => {
    expect(component.formatSize(500)).toBe('500 o');
  });

  it('formatSize — kilooctets', () => {
    expect(component.formatSize(2048)).toBe('2.0 Ko');
  });

  it('formatSize — mégaoctets', () => {
    expect(component.formatSize(5 * 1024 * 1024)).toBe('5.0 Mo');
  });

  it('statusLabel OPEN → "Ouvert"', () => {
    expect(component.statusLabel('OPEN')).toBe('Ouvert');
  });
});
