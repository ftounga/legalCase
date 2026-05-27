import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { Naturalisation12bisBeService } from './naturalisation-12bis-be.service';
import {
  Naturalisation12bisBeRequest,
  Naturalisation12bisBeResponse,
} from '../models/naturalisation-12bis-be.model';

describe('Naturalisation12bisBeService', () => {
  let service: Naturalisation12bisBeService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/naturalisation-12bis-be-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [Naturalisation12bisBeService],
    });
    service = TestBed.inject(Naturalisation12bisBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: Naturalisation12bisBeRequest = {
      dureeSejour: 72,
      typeSejour: 'ILLIMITE',
      niveauLangue: 'A2',
      preuveIntegration: true,
      preuveEmploi: true,
      menaceOrdrePublic: false,
      condamnationPenale: false,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as Naturalisation12bisBeResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as Naturalisation12bisBeResponse);
  });

  it('exposes STANDALONE_TOOL_ID matching backend dispatcher key', () => {
    expect(Naturalisation12bisBeService.STANDALONE_TOOL_ID).toBe('F-IM-28-naturalisation-12bis-be');
  });
});
