import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { SaisieRemunerationService } from './saisie-remuneration.service';
import {
  SaisieRemunerationRequest,
  SaisieRemunerationResponse,
} from '../models/saisie-remuneration.model';

describe('SaisieRemunerationService', () => {
  let service: SaisieRemunerationService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/saisie-remuneration-analysis';

  function response(overrides: Partial<SaisieRemunerationResponse> = {}): SaisieRemunerationResponse {
    return {
      caseFileId: 'case-1',
      remunerationNetteMensuelle: 2000,
      nombrePersonnesACharge: 0,
      creanceTotale: 5000,
      creanceAlimentaire: false,
      verdict: 'SAISISSABLE',
      quotiteSaisissableMensuelle: 320,
      montantLaisseAuSalarie: 1680,
      fractionInsaisissable: 635,
      nombreMoisRecouvrement: 16,
      tranches: [
        { rang: 1, borneInferieure: 0, borneSuperieure: 350, quotite: '1/20', montantTranche: 350, montantSaisissable: 17.5 },
      ],
      anneeBareme: 2026,
      country: 'FRANCE',
      baseJuridique: 'R. 3252-1 à R. 3252-5 ; L. 3252-2 et L. 3252-3 Code travail',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SaisieRemunerationService],
    });
    service = TestBed.inject(SaisieRemunerationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(SaisieRemunerationService.STANDALONE_TOOL_ID).toBe('F-DT-89-saisie-arret-remuneration');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: SaisieRemunerationRequest = {
      remunerationNetteMensuelle: 2000,
      nombrePersonnesACharge: 0,
      creanceTotale: 5000,
      creanceAlimentaire: false,
    };
    let received: SaisieRemunerationResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.verdict).toBe('SAISISSABLE');
    expect(received!.quotiteSaisissableMensuelle).toBe(320);
  });

  it('get() GETs the figured endpoint', () => {
    let received: SaisieRemunerationResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ verdict: 'INSAISISSABLE', quotiteSaisissableMensuelle: 0, nombreMoisRecouvrement: null }));
    expect(received!.verdict).toBe('INSAISISSABLE');
    expect(received!.nombreMoisRecouvrement).toBeNull();
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
