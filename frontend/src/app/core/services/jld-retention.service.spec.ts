import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { JldRetentionService } from './jld-retention.service';
import { JldRetentionRequest, JldRetentionResponse } from '../models/jld-retention.model';

describe('JldRetentionService', () => {
  let service: JldRetentionService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/jld-retention-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [JldRetentionService],
    });
    service = TestBed.inject(JldRetentionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: JldRetentionRequest = {
      dateNotificationPlacement: '2026-05-08',
      motifPlacement: 'EXECUTION_OQTF',
      recoursForme: false,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as JldRetentionResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as JldRetentionResponse);
  });
});
