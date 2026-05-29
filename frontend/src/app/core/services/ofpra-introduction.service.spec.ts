import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { OfpraIntroductionService } from './ofpra-introduction.service';
import {
  OfpraIntroductionRequest,
  OfpraIntroductionResponse,
} from '../models/ofpra-introduction.model';

/**
 * SF-214-18 — Tests du wrapper HttpClient F-IM-33 (OFPRA introduction, FR).
 */
describe('OfpraIntroductionService', () => {
  let service: OfpraIntroductionService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/ofpra-introduction-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OfpraIntroductionService],
    });
    service = TestBed.inject(OfpraIntroductionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('STANDALONE_TOOL_ID aligné sur la clé TOOL_REGISTRY', () => {
    expect(OfpraIntroductionService.STANDALONE_TOOL_ID)
      .toBe('F-IM-33-ofpra-introduction-fr');
  });

  it('analyze : POST vers l\'endpoint figé avec le corps fourni', () => {
    const request: OfpraIntroductionRequest = {
      dateArriveeEnFrance: '2026-01-10',
      passageGudaEffectue: true,
      datePassageGuda: '2026-01-20',
      adaRequise: true,
    };
    const response = { caseFileId: 'case-1' } as OfpraIntroductionResponse;
    service.analyze('case-1', request).subscribe((r) => expect(r).toEqual(response));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateArriveeEnFrance).toBe('2026-01-10');
    expect(req.request.body.passageGudaEffectue).toBe(true);
    req.flush(response);
  });

  it('get : GET vers l\'endpoint figé', () => {
    const response = { caseFileId: 'case-1' } as OfpraIntroductionResponse;
    service.get('case-1').subscribe((r) => expect(r).toEqual(response));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });

  it('analyze : propage une erreur HTTP', () => {
    const request: OfpraIntroductionRequest = {
      dateArriveeEnFrance: '2026-01-10',
      passageGudaEffectue: false,
      adaRequise: false,
    };
    let errored = false;
    service.analyze('case-1', request).subscribe({
      next: () => fail('should not succeed'),
      error: () => (errored = true),
    });
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(errored).toBe(true);
  });
});
