import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CceAnnulationBeService } from './cce-annulation-be.service';
import {
  CceAnnulationBeRequest,
  CceAnnulationBeResponse,
} from '../models/cce-annulation-be.model';

describe('CceAnnulationBeService', () => {
  let service: CceAnnulationBeService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/cce-annulation-be-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CceAnnulationBeService],
    });
    service = TestBed.inject(CceAnnulationBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: CceAnnulationBeRequest = {
      dateNotificationDecision: '2026-05-01',
      typeDecision: 'REFUS_TITRE',
      recoursForme: false,
      dateRecours: null,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as CceAnnulationBeResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as CceAnnulationBeResponse);
  });

  it('exposes STANDALONE_TOOL_ID matching backend dispatcher key', () => {
    expect(CceAnnulationBeService.STANDALONE_TOOL_ID).toBe('F-IM-31-cce-annulation-30j-be');
  });
});
