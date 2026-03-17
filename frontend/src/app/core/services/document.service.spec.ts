import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DocumentService } from './document.service';
import { Document } from '../models/document.model';

const mockDocument: Document = {
  id: 'doc1', caseFileId: 'cf1', originalFilename: 'contrat.pdf',
  contentType: 'application/pdf', fileSize: 12345, createdAt: '2026-03-17T10:00:00Z'
};

describe('DocumentService', () => {
  let service: DocumentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(DocumentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('list — GET /api/v1/case-files/cf1/documents', () => {
    service.list('cf1').subscribe(docs => {
      expect(docs.length).toBe(1);
      expect(docs[0].originalFilename).toBe('contrat.pdf');
    });
    http.expectOne('/api/v1/case-files/cf1/documents').flush([mockDocument]);
  });

  it('upload — POST /api/v1/case-files/cf1/documents avec FormData', () => {
    const file = new File(['content'], 'contrat.pdf', { type: 'application/pdf' });
    service.upload('cf1', file).subscribe(doc => expect(doc).toEqual(mockDocument));
    const req = http.expectOne('/api/v1/case-files/cf1/documents');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    req.flush(mockDocument);
  });

  it('downloadUrl — retourne l\'URL correcte', () => {
    expect(service.downloadUrl('cf1', 'doc1'))
      .toBe('/api/v1/case-files/cf1/documents/doc1/download');
  });
});
