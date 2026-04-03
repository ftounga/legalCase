import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TimeReportService } from './time-report.service';
import { TimeReportRow } from '../models/time-tracking.models';

describe('TimeReportService', () => {
  let service: TimeReportService;
  let httpMock: HttpTestingController;

  const mockRow: TimeReportRow = {
    caseFileId: 'cf-1',
    caseFileTitle: 'Dossier Dupont',
    userId: 'u-1',
    userDisplayName: 'Alice',
    totalSeconds: 3600,
    ratePerHour: 200,
    totalAmount: 200
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(TimeReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getReport() appelle GET /api/v1/workspace/time-report?month=2026-04', () => {
    service.getReport('2026-04').subscribe(rows => {
      expect(rows).toEqual([mockRow]);
    });
    const req = httpMock.expectOne(r => r.url === '/api/v1/workspace/time-report' && r.params.get('month') === '2026-04');
    expect(req.request.method).toBe('GET');
    req.flush([mockRow]);
  });

  it('getReport() retourne un tableau vide si aucune entrée', () => {
    service.getReport('2020-01').subscribe(rows => {
      expect(rows).toEqual([]);
    });
    const req = httpMock.expectOne(r => r.url === '/api/v1/workspace/time-report');
    req.flush([]);
  });

  it('exportCsv() appelle GET /api/v1/workspace/time-report/export?month=2026-04', () => {
    service.exportCsv('2026-04').subscribe(blob => {
      expect(blob).toBeTruthy();
    });
    const req = httpMock.expectOne(r =>
      r.url === '/api/v1/workspace/time-report/export' && r.params.get('month') === '2026-04'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['csv'], { type: 'text/csv' }));
  });
});
