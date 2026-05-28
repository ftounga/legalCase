import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AesmMenaBeService } from './aesm-mena-be.service';
import {
  AesmMenaBeRequest,
  AesmMenaBeResponse,
} from '../models/aesm-mena-be.model';

describe('AesmMenaBeService', () => {
  let service: AesmMenaBeService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/aesm-mena-be-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AesmMenaBeService],
    });
    service = TestBed.inject(AesmMenaBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body (8 fields composite)', () => {
    const req: AesmMenaBeRequest = {
      ageActuel: 16,
      dateArriveeBelgique: '2023-09-01',
      tuteurDesigne: true,
      integrationScolaire: true,
      dureeScolaire: 24,
      projetVieElabore: true,
      perspectiveAutonomie: true,
      menaceOrdrePublic: false,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as AesmMenaBeResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as AesmMenaBeResponse);
  });

  it('exposes STANDALONE_TOOL_ID matching backend dispatcher key', () => {
    expect(AesmMenaBeService.STANDALONE_TOOL_ID).toBe('F-IM-30-aesm-mena-be');
  });
});
