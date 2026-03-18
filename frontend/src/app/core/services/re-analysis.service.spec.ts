import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReAnalysisService } from './re-analysis.service';

describe('ReAnalysisService', () => {
  let service: ReAnalysisService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ReAnalysisService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('reAnalyze — POST /api/v1/case-files/{id}/re-analyze', () => {
    service.reAnalyze('cf1').subscribe();
    const req = http.expectOne('/api/v1/case-files/cf1/re-analyze');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });
});
