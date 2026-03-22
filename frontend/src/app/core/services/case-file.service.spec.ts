import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CaseFileService } from './case-file.service';
import { Page } from '../models/page.model';
import { CaseFile } from '../models/case-file.model';

const mockCaseFile: CaseFile = {
  id: 'cf1', title: 'Dossier A', legalDomain: 'DROIT_DU_TRAVAIL',
  description: null, status: 'OPEN', createdAt: '2026-03-17T10:00:00Z'
};

describe('CaseFileService', () => {
  let service: CaseFileService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(CaseFileService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('list — GET /api/v1/case-files avec params page/size', () => {
    const page: Page<CaseFile> = { content: [mockCaseFile], totalElements: 1, totalPages: 1, size: 20, number: 0 };
    service.list(0, 20).subscribe(result => {
      expect(result.content.length).toBe(1);
      expect(result.totalElements).toBe(1);
    });
    const req = http.expectOne(r => r.url === '/api/v1/case-files');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(page);
  });

  it('getById — GET /api/v1/case-files/:id', () => {
    service.getById('cf1').subscribe(result => expect(result).toEqual(mockCaseFile));
    http.expectOne('/api/v1/case-files/cf1').flush(mockCaseFile);
  });

  it('create — POST /api/v1/case-files', () => {
    service.create({ title: 'Nouveau' })
      .subscribe(result => expect(result).toEqual(mockCaseFile));
    const req = http.expectOne('/api/v1/case-files');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Nouveau');
    req.flush(mockCaseFile);
  });
});
