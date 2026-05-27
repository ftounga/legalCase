import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { SinglePermitBeService } from './single-permit-be.service';
import {
  SinglePermitBeRequest,
  SinglePermitBeResponse,
} from '../models/single-permit-be.model';

describe('SinglePermitBeService', () => {
  let service: SinglePermitBeService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/single-permit-be-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SinglePermitBeService],
    });
    service = TestBed.inject(SinglePermitBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: SinglePermitBeRequest = {
      dateDebutPermit: '2026-01-01',
      dateFinPermit: '2026-12-31',
      regionInstruction: 'WALLONIE',
      typeActivite: 'SALARIE',
      motifDemande: 'RENOUVELLEMENT',
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as SinglePermitBeResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as SinglePermitBeResponse);
  });

  it('exposes STANDALONE_TOOL_ID matching backend dispatcher key', () => {
    expect(SinglePermitBeService.STANDALONE_TOOL_ID).toBe('F-IM-25-single-permit-be');
  });
});
