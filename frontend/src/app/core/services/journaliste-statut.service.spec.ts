import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { JournalisteStatutService } from './journaliste-statut.service';
import {
  JournalisteStatutRequest,
  JournalisteStatutResponse,
} from '../models/journaliste-statut.model';

describe('JournalisteStatutService', () => {
  let service: JournalisteStatutService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/journaliste-statut-analysis';

  function response(overrides: Partial<JournalisteStatutResponse> = {}): JournalisteStatutResponse {
    return {
      caseFileId: 'case-1',
      dateEntree: '2018-01-01',
      dateRupture: '2023-06-15',
      typeRupture: 'CLAUSE_CESSION',
      salaireMensuelMoyen: 3000,
      carteIdentiteProfessionnelle: true,
      cessionTitreConstatee: true,
      changementOrientationConstate: false,
      statutJournaliste: 'CONFIRME',
      clauseValide: 'VALIDE',
      motif: 'Cession du titre constatée (art. L.7112-5 1°)',
      indemniteCongediement: 18000,
      ancienneteAnnees: 6,
      commissionArbitraleRequise: false,
      verdictGlobal: 'RUPTURE_ASSIMILEE_LICENCIEMENT',
      country: 'FRANCE',
      baseJuridique: 'Art. L.7111-1 à L.7113-12 CT ; L.7112-3 ; L.7112-4 ; L.7112-5 (à vérifier par avocat)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [JournalisteStatutService],
    });
    service = TestBed.inject(JournalisteStatutService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(JournalisteStatutService.STANDALONE_TOOL_ID).toBe('F-DT-105-journaliste-statut');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: JournalisteStatutRequest = {
      dateEntree: '2018-01-01',
      dateRupture: '2023-06-15',
      typeRupture: 'CLAUSE_CESSION',
      salaireMensuelMoyen: 3000,
      carteIdentiteProfessionnelle: true,
      cessionTitreConstatee: true,
      changementOrientationConstate: false,
    };
    let received: JournalisteStatutResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.verdictGlobal).toBe('RUPTURE_ASSIMILEE_LICENCIEMENT');
    expect(received!.clauseValide).toBe('VALIDE');
    expect(received!.indemniteCongediement).toBe(18000);
  });

  it('get() GETs the figured endpoint and maps the commission arbitrale flag', () => {
    let received: JournalisteStatutResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      ancienneteAnnees: 18,
      commissionArbitraleRequise: true,
      verdictGlobal: 'COMMISSION_ARBITRALE',
    }));
    expect(received!.commissionArbitraleRequise).toBe(true);
    expect(received!.verdictGlobal).toBe('COMMISSION_ARBITRALE');
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

  it('maps a NON_VALIDE clause (clause de conscience sans constat) with motif', () => {
    let received: JournalisteStatutResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      typeRupture: 'CLAUSE_CONSCIENCE',
      cessionTitreConstatee: false,
      changementOrientationConstate: false,
      clauseValide: 'NON_VALIDE',
      motif: "Changement notable d'orientation non constaté (art. L.7112-5 2°/3°)",
      verdictGlobal: 'INDEMNITE_NON_DUE',
      indemniteCongediement: 0,
    }));
    expect(received!.clauseValide).toBe('NON_VALIDE');
    expect(received!.motif).toContain('orientation');
    expect(received!.indemniteCongediement).toBe(0);
  });
});
