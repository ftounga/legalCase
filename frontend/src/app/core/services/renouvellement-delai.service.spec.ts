import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { RenouvellementDelaiService } from './renouvellement-delai.service';
import {
  RenouvellementDelaiRequest,
  RenouvellementDelaiResponse,
} from '../models/renouvellement-delai.model';

describe('RenouvellementDelaiService', () => {
  let service: RenouvellementDelaiService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/renouvellement-delai-analysis';

  function response(overrides: Partial<RenouvellementDelaiResponse> = {}): RenouvellementDelaiResponse {
    return {
      caseFileId: 'case-1',
      dateExpirationTitre: '2026-09-15',
      dateDepotDossier: null,
      typeTitre: 'Carte pluriannuelle',
      country: 'FRANCE',
      statut: 'A_DEPOSER',
      dateOptimalDepot: '2026-07-15',
      dateDepotImperatif: '2026-09-15',
      joursRestantsAvantOptimal: 30,
      joursRestantsAvantImperatif: 92,
      risqueIrruption: false,
      alerteRetard: false,
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RenouvellementDelaiService],
    });
    service = TestBed.inject(RenouvellementDelaiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(RenouvellementDelaiService.STANDALONE_TOOL_ID).toBe('F-IM-31-renouvellement-delai-depot-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: RenouvellementDelaiRequest = {
      dateExpirationTitre: '2026-09-15',
      dateDepotDossier: null,
      typeTitre: 'Carte pluriannuelle',
    };
    let result: RenouvellementDelaiResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('A_DEPOSER');
    expect(result!.joursRestantsAvantOptimal).toBe(30);
  });

  it('get() GETs from the right URL', () => {
    let result: RenouvellementDelaiResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'EXPIRE', joursRestantsAvantOptimal: -10, risqueIrruption: true, alerteRetard: true }));
    expect(result!.statut).toBe('EXPIRE');
    expect(result!.joursRestantsAvantOptimal).toBe(-10);
    expect(result!.risqueIrruption).toBe(true);
  });

  it('propagates backend error to the subscriber', () => {
    let errStatus: number | undefined;
    service.get('case-1').subscribe({
      next: () => fail('should not succeed'),
      error: (e) => (errStatus = e.status),
    });
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    expect(errStatus).toBe(500);
  });
});
