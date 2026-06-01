import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DelegationSyndicaleService } from './delegation-syndicale.service';
import {
  DelegationSyndicaleRequest,
  DelegationSyndicaleResponse,
} from '../models/delegation-syndicale.model';

describe('DelegationSyndicaleService', () => {
  let service: DelegationSyndicaleService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/delegation-syndicale-analysis';

  function response(overrides: Partial<DelegationSyndicaleResponse> = {}): DelegationSyndicaleResponse {
    return {
      caseFileId: 'case-1',
      effectif: 80,
      typeMandat: 'DELEGUE_SYNDICAL',
      syndicatRepresentatif: true,
      pourcentageScorePersonnel: 15,
      dateDesignation: '2026-03-01',
      checklist: [
        { item: 'Effectif suffisant', conforme: true, commentaire: 'Effectif : 80.' },
        { item: 'Organisation représentative', conforme: true, commentaire: '' },
        { item: 'Score personnel ≥ 10 %', conforme: true, commentaire: 'Score : 15 %.' },
      ],
      statutDesignation: 'REGULIERE',
      statutProtege: 'OUI',
      licenciementEnvisage: false,
      autorisationInspecteurTravail: false,
      risqueNulliteLicenciement: 'SANS_OBJET',
      consequences: [],
      country: 'FRANCE',
      baseJuridique: 'art. L.2143-1 et s. CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DelegationSyndicaleService],
    });
    service = TestBed.inject(DelegationSyndicaleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(DelegationSyndicaleService.STANDALONE_TOOL_ID).toBe('F-DT-69-delegation-syndicale-protection');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: DelegationSyndicaleRequest = {
      effectif: 80,
      typeMandat: 'DELEGUE_SYNDICAL',
      syndicatRepresentatif: true,
      pourcentageScorePersonnel: 15,
      dateDesignation: '2026-03-01',
      licenciementEnvisage: false,
      autorisationInspecteurTravail: false,
    };
    let received: DelegationSyndicaleResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.statutDesignation).toBe('REGULIERE');
    expect(received!.statutProtege).toBe('OUI');
    expect(received!.risqueNulliteLicenciement).toBe('SANS_OBJET');
  });

  it('get() GETs the figured endpoint and maps an IRREGULIERE result', () => {
    let received: DelegationSyndicaleResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      effectif: 30,
      statutDesignation: 'IRREGULIERE',
      checklist: [
        { item: 'Effectif suffisant', conforme: false, commentaire: 'Effectif insuffisant.' },
      ],
      consequences: ['Effectif insuffisant (30 salariés).'],
    }));
    expect(received!.statutDesignation).toBe('IRREGULIERE');
    expect(received!.checklist[0].conforme).toBe(false);
    expect(received!.consequences.length).toBe(1);
  });

  it('get() maps an A_VERIFIER result (DS sans score)', () => {
    let received: DelegationSyndicaleResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      pourcentageScorePersonnel: null,
      statutDesignation: 'A_VERIFIER',
    }));
    expect(received!.statutDesignation).toBe('A_VERIFIER');
    expect(received!.pourcentageScorePersonnel).toBeNull();
  });

  it('get() maps an ELEVE risque result (licenciement sans autorisation)', () => {
    let received: DelegationSyndicaleResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      licenciementEnvisage: true,
      autorisationInspecteurTravail: false,
      risqueNulliteLicenciement: 'ELEVE',
    }));
    expect(received!.risqueNulliteLicenciement).toBe('ELEVE');
    expect(received!.licenciementEnvisage).toBe(true);
  });

  it('get() maps a RSS result (syndicat non représentatif)', () => {
    let received: DelegationSyndicaleResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      typeMandat: 'RSS',
      syndicatRepresentatif: false,
      pourcentageScorePersonnel: null,
      statutDesignation: 'REGULIERE',
    }));
    expect(received!.typeMandat).toBe('RSS');
    expect(received!.syndicatRepresentatif).toBe(false);
  });

  it('get() propagates a 404 error when no analysis exists', () => {
    let errorStatus: number | undefined;
    service.get('case-1').subscribe({
      next: () => fail('should have errored'),
      error: (err) => (errorStatus = err.status),
    });
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(errorStatus).toBe(404);
  });
});
