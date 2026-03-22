import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AdminUsageService } from './admin-usage.service';
import { WorkspaceUsageSummary } from '../models/workspace-usage-summary.model';

const mockSummary: WorkspaceUsageSummary = {
  totalTokensInput: 1000,
  totalTokensOutput: 500,
  totalCost: 0.0045,
  byUser: [{ userId: 'u1', userEmail: 'test@test.com', tokensInput: 1000, tokensOutput: 500, totalCost: 0.0045 }],
  byCaseFile: [{ caseFileId: 'cf1', caseFileTitle: 'Dossier A', tokensInput: 1000, tokensOutput: 500, totalCost: 0.0045 }],
  monthlyTokensUsed: 0,
  monthlyTokensBudget: 0
};

describe('AdminUsageService', () => {
  let service: AdminUsageService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AdminUsageService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getSummary — GET /api/v1/admin/usage', () => {
    service.getSummary().subscribe(res => {
      expect(res.totalTokensInput).toBe(1000);
      expect(res.byUser.length).toBe(1);
      expect(res.byCaseFile.length).toBe(1);
    });
    const req = http.expectOne('/api/v1/admin/usage');
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });
});
