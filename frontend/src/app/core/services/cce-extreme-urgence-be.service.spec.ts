import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CceExtremeUrgenceBeService } from './cce-extreme-urgence-be.service';
import {
  CceExtremeUrgenceBeRequest,
  CceExtremeUrgenceBeResponse,
} from '../models/cce-extreme-urgence-be.model';

describe('CceExtremeUrgenceBeService', () => {
  let service: CceExtremeUrgenceBeService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/cce-extreme-urgence-be-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CceExtremeUrgenceBeService],
    });
    service = TestBed.inject(CceExtremeUrgenceBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: CceExtremeUrgenceBeRequest = {
      dateActeExecutoire: '2026-05-01',
      typeActe: 'OQT_EXECUTE',
      recoursForme: false,
      dateRecours: null,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as CceExtremeUrgenceBeResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as CceExtremeUrgenceBeResponse);
  });

  it('exposes STANDALONE_TOOL_ID matching backend dispatcher key', () => {
    expect(CceExtremeUrgenceBeService.STANDALONE_TOOL_ID).toBe('F-IM-32-cce-extreme-urgence-5j-be');
  });
});
