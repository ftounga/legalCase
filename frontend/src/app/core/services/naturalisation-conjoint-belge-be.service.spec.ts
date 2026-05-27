import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { NaturalisationConjointBelgeBeService } from './naturalisation-conjoint-belge-be.service';
import {
  NaturalisationConjointBelgeBeRequest,
  NaturalisationConjointBelgeBeResponse,
} from '../models/naturalisation-conjoint-belge-be.model';

describe('NaturalisationConjointBelgeBeService', () => {
  let service: NaturalisationConjointBelgeBeService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/naturalisation-conjoint-belge-be-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NaturalisationConjointBelgeBeService],
    });
    service = TestBed.inject(NaturalisationConjointBelgeBeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: NaturalisationConjointBelgeBeRequest = {
      dateMarriage: '2020-05-15',
      cohabitationLegale: false,
      dureeCohabitationMois: 72,
      niveauLangue: 'A2',
      preuveIntegration: true,
      menaceOrdrePublic: false,
      condamnationPenale: false,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as NaturalisationConjointBelgeBeResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as NaturalisationConjointBelgeBeResponse);
  });

  it('exposes STANDALONE_TOOL_ID matching backend dispatcher key', () => {
    expect(NaturalisationConjointBelgeBeService.STANDALONE_TOOL_ID)
      .toBe('F-IM-29-naturalisation-conjoint-belge-be');
  });
});
