import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CaseFileStatsService } from './case-file-stats.service';
import { CaseFileStats } from '../models/case-file-stats.model';

describe('CaseFileStatsService', () => {
  let service: CaseFileStatsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CaseFileStatsService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(CaseFileStatsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call GET /api/v1/case-files/{id}/stats', () => {
    const caseFileId = 'abc-123';
    const mockStats: CaseFileStats = { documentCount: 3, analysisCount: 2, totalTokens: 12540 };

    service.getStats(caseFileId).subscribe(stats => {
      expect(stats).toEqual(mockStats);
    });

    const req = httpMock.expectOne(`/api/v1/case-files/${caseFileId}/stats`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStats);
  });
});
