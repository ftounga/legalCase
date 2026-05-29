import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { VictimeTraiteService } from './victime-traite.service';
import { VictimeTraiteRequest, VictimeTraiteResponse } from '../models/victime-traite.model';

describe('VictimeTraiteService', () => {
  let service: VictimeTraiteService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/victime-traite-analysis';

  function response(overrides: Partial<VictimeTraiteResponse> = {}): VictimeTraiteResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      verdict: 'ELIGIBLE_PROBABLE',
      chipsCriteresManquants: [],
      mesuresProtection: ['Carte de séjour temporaire « vie privée et familiale »'],
      risqueVictimeEnDanger: false,
      baseJuridique: 'CESEDA L. 425-1',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [VictimeTraiteService],
    });
    service = TestBed.inject(VictimeTraiteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(VictimeTraiteService.STANDALONE_TOOL_ID).toBe('F-IM-35-victime-traite-l4251-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: VictimeTraiteRequest = {
      plainteDeposee: true,
      collaborationOCRTEH: true,
      datePlainte: '2026-01-10',
      titreActuel: null,
      presenceAutoriteRefugieDetectee: false,
    };
    let result: VictimeTraiteResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.verdict).toBe('ELIGIBLE_PROBABLE');
    expect(result!.mesuresProtection.length).toBe(1);
  });

  it('get() GETs from the right URL', () => {
    let result: VictimeTraiteResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ verdict: 'NON_ELIGIBLE', risqueVictimeEnDanger: false }));
    expect(result!.verdict).toBe('NON_ELIGIBLE');
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
