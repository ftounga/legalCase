import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { Regroupement10bisBeService } from './regroupement-10bis-be.service';
import {
  Regroupement10bisBeRequest,
  Regroupement10bisBeResponse,
} from '../models/regroupement-10bis-be.model';

describe('Regroupement10bisBeService', () => {
  let service: Regroupement10bisBeService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/regroupement-10bis-be-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [Regroupement10bisBeService],
    });
    service = TestBed.inject(Regroupement10bisBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: Regroupement10bisBeRequest = {
      lienFamilial: 'CONJOINT',
      typeCarteRegroupant: 'CARTE_A',
      revenusMensuelsNetsRegroupant: 1950,
      dureeSejour: 24,
      dateFinCarteA: '2027-12-31',
      logementConforme: true,
      assuranceMaladie: true,
      menaceOrdrePublic: false,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as Regroupement10bisBeResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as Regroupement10bisBeResponse);
  });

  it('exposes STANDALONE_TOOL_ID matching backend dispatcher key', () => {
    expect(Regroupement10bisBeService.STANDALONE_TOOL_ID).toBe('F-IM-27-regroupement-10bis-be');
  });
});
