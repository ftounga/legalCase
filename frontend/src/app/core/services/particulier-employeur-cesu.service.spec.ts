import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ParticulierEmployeurCesuService } from './particulier-employeur-cesu.service';
import {
  ParticulierEmployeurCesuRequest,
  ParticulierEmployeurCesuResponse,
} from '../models/particulier-employeur-cesu.model';

describe('ParticulierEmployeurCesuService', () => {
  let service: ParticulierEmployeurCesuService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/particulier-employeur-cesu-analysis';

  function response(overrides: Partial<ParticulierEmployeurCesuResponse> = {}): ParticulierEmployeurCesuResponse {
    return {
      caseFileId: 'case-1',
      dateEntree: '2020-01-01',
      dateRupture: '2023-06-15',
      categorieEmploye: 'SALARIE_PARTICULIER_EMPLOYEUR',
      causeRupture: 'LICENCIEMENT_MOTIF_PERSONNEL',
      salaireMensuelMoyen: 1200,
      dureePreavisJours: 60,
      dureePreavisLibelle: '2 mois',
      eligibiliteIndemnite: 'DUE',
      motifNonDue: null,
      indemniteLicenciement: 900,
      methodeCalcul: 'CONVENTIONNEL_PE',
      verdictGlobal: 'RUPTURE_REGULIERE',
      country: 'FRANCE',
      baseJuridique: 'CCN salariés du particulier employeur (IDCC 3239, 2021) (à vérifier par avocat)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ParticulierEmployeurCesuService],
    });
    service = TestBed.inject(ParticulierEmployeurCesuService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(ParticulierEmployeurCesuService.STANDALONE_TOOL_ID).toBe('F-DT-108-particulier-employeur-cesu');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: ParticulierEmployeurCesuRequest = {
      dateEntree: '2020-01-01',
      dateRupture: '2023-06-15',
      categorieEmploye: 'SALARIE_PARTICULIER_EMPLOYEUR',
      causeRupture: 'LICENCIEMENT_MOTIF_PERSONNEL',
      salaireMensuelMoyen: 1200,
    };
    let received: ParticulierEmployeurCesuResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.verdictGlobal).toBe('RUPTURE_REGULIERE');
    expect(received!.dureePreavisLibelle).toBe('2 mois');
    expect(received!.methodeCalcul).toBe('CONVENTIONNEL_PE');
  });

  it('get() GETs the figured endpoint and maps the assmat indemnity', () => {
    let received: ParticulierEmployeurCesuResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      categorieEmploye: 'ASSISTANT_MATERNEL',
      causeRupture: 'RETRAIT_ENFANT',
      methodeCalcul: 'INDEMNITE_RUPTURE_ASSMAT',
    }));
    expect(received!.categorieEmploye).toBe('ASSISTANT_MATERNEL');
    expect(received!.methodeCalcul).toBe('INDEMNITE_RUPTURE_ASSMAT');
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

  it('maps a NON_DUE response (faute grave) with motif', () => {
    let received: ParticulierEmployeurCesuResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      causeRupture: 'FAUTE_GRAVE',
      eligibiliteIndemnite: 'NON_DUE',
      motifNonDue: 'Faute grave — indemnité non due',
      indemniteLicenciement: 0,
      verdictGlobal: 'INDEMNITE_NON_DUE',
    }));
    expect(received!.eligibiliteIndemnite).toBe('NON_DUE');
    expect(received!.motifNonDue).toContain('Faute grave');
    expect(received!.indemniteLicenciement).toBe(0);
  });
});
