import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DashboardAuditService } from './dashboard-audit.service';
import { DashboardAuditReport } from '../models/dashboard-audit.model';

const mockReport: DashboardAuditReport = {
  ranAt: '2026-05-19T18:00:00Z',
  crashedMappers: [
    {
      toolId: 'indemnite-licenciement',
      crashCount: 3,
      lastExceptionClass: 'NullPointerException',
      lastExceptionMessage: 'tile data missing',
      lastOccurredAt: '2026-05-19T17:45:00Z',
    },
  ],
  dormantTiles: [{ tableName: 'rupture_conventionnelle_analyses', rowCount: 0 }],
  activeTiles: [{ tableName: 'preavis_analyses', rowCount: 31 }],
};

describe('DashboardAuditService', () => {
  let service: DashboardAuditService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(DashboardAuditService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getLatest — GET /api/v1/super-admin/dashboard-audit/latest', () => {
    let received: DashboardAuditReport | undefined;
    service.getLatest().subscribe(res => (received = res));

    const req = http.expectOne('/api/v1/super-admin/dashboard-audit/latest');
    expect(req.request.method).toBe('GET');
    req.flush(mockReport);

    expect(received).toEqual(mockReport);
    expect(received!.crashedMappers.length).toBe(1);
    expect(received!.activeTiles[0].rowCount).toBe(31);
  });

  it('runAudit — POST /api/v1/super-admin/dashboard-audit/run', () => {
    let received: DashboardAuditReport | undefined;
    service.runAudit().subscribe(res => (received = res));

    const req = http.expectOne('/api/v1/super-admin/dashboard-audit/run');
    expect(req.request.method).toBe('POST');
    req.flush(mockReport);

    expect(received).toEqual(mockReport);
  });
});
