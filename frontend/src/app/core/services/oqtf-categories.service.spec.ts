import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { OqtfCategoriesService } from './oqtf-categories.service';
import {
  OqtfCategoriesRequest,
  OqtfCategoriesResponse,
} from '../models/oqtf-categories.model';

describe('OqtfCategoriesService', () => {
  let service: OqtfCategoriesService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/oqtf-categories-analysis';

  function response(overrides: Partial<OqtfCategoriesResponse> = {}): OqtfCategoriesResponse {
    return {
      caseFileId: 'case-1',
      categorieL611: 'CAT_3',
      dateNotificationOqtf: '2025-01-15',
      motifOqtf: null,
      country: 'FRANCE',
      categorieLibelle: '3° — Entrée ou séjour irrégulier',
      moyensDefense: ['Vie privée et familiale (art. 8 CEDH)', 'Erreur de droit'],
      baseJuridique: 'CESEDA L.611-1, 3°',
      delaiRecours: '30 jours',
      procedureParallele: null,
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OqtfCategoriesService],
    });
    service = TestBed.inject(OqtfCategoriesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(OqtfCategoriesService.STANDALONE_TOOL_ID).toBe('F-IM-29-oqtf-categories-l6111-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: OqtfCategoriesRequest = {
      categorieL611: 'CAT_3',
      dateNotificationOqtf: '2025-01-15',
      motifOqtf: 'Séjour irrégulier',
    };
    let received: OqtfCategoriesResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received?.categorieLibelle).toBe('3° — Entrée ou séjour irrégulier');
  });

  it('get() GETs the analysis for the case file', () => {
    let received: OqtfCategoriesResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response());
    expect(received?.categorieL611).toBe('CAT_3');
  });

  it('get() maps a CAT_7 response with procedureParallele', () => {
    let received: OqtfCategoriesResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      categorieL611: 'CAT_7',
      categorieLibelle: '7° — Remise à un autre État membre (Dublin)',
      procedureParallele: 'Recours Dublin (F-IM-22) — règlement (UE) 604/2013',
    }));
    expect(received?.categorieL611).toBe('CAT_7');
    expect(received?.procedureParallele).toContain('F-IM-22');
  });
});
