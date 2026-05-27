import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { Regroupement10terBeService } from './regroupement-10ter-be.service';
import {
  Regroupement10terBeRequest,
  Regroupement10terBeResponse,
} from '../models/regroupement-10ter-be.model';

describe('Regroupement10terBeService', () => {
  let service: Regroupement10terBeService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/regroupement-10ter-be-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [Regroupement10terBeService],
    });
    service = TestBed.inject(Regroupement10terBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: Regroupement10terBeRequest = {
      lienFamilial: 'CONJOINT',
      typeCarteRegroupant: 'CARTE_B',
      revenusMensuelsNetsRegroupant: 1950,
      dureeSejour: 24,
      logementConforme: true,
      assuranceMaladie: true,
      menaceOrdrePublic: false,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as Regroupement10terBeResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as Regroupement10terBeResponse);
  });

  it('exposes STANDALONE_TOOL_ID matching backend dispatcher key', () => {
    expect(Regroupement10terBeService.STANDALONE_TOOL_ID).toBe('F-IM-26-regroupement-10ter-be');
  });
});
