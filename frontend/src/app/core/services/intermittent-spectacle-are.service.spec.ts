import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IntermittentSpectacleAreService } from './intermittent-spectacle-are.service';
import {
  IntermittentSpectacleAreRequest,
  IntermittentSpectacleAreResponse,
} from '../models/intermittent-spectacle-are.model';

describe('IntermittentSpectacleAreService', () => {
  let service: IntermittentSpectacleAreService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/intermittent-spectacle-are-analysis';

  function response(overrides: Partial<IntermittentSpectacleAreResponse> = {}): IntermittentSpectacleAreResponse {
    return {
      caseFileId: 'case-1',
      annexe: 'ANNEXE_8_TECHNICIENS',
      dateFinContrat: '2024-03-31',
      heuresTravaillees12Mois: 520,
      nombreCachets: 0,
      heuresFormationDispensees: 0,
      heuresCachets: 0,
      heuresFormationRetenues: 0,
      heuresTotalesRetenues: 520,
      seuilHeures: 507,
      ouvertureDroits: 'OUVERTS',
      heuresManquantes: 0,
      heuresExcedentaires: 13,
      statut: 'DROITS_OUVERTS',
      dateProchainExamen: '2025-03-31',
      country: 'FRANCE',
      baseJuridique: 'Règlement Unedic — annexes 8 et 10 ; affiliation 507 h / 12 mois (à vérifier par avocat / France Travail)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [IntermittentSpectacleAreService],
    });
    service = TestBed.inject(IntermittentSpectacleAreService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(IntermittentSpectacleAreService.STANDALONE_TOOL_ID).toBe('F-DT-106-intermittent-spectacle-are');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: IntermittentSpectacleAreRequest = {
      annexe: 'ANNEXE_8_TECHNICIENS',
      dateFinContrat: '2024-03-31',
      heuresTravaillees12Mois: 520,
      nombreCachets: 0,
      heuresFormationDispensees: 0,
    };
    let received: IntermittentSpectacleAreResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.ouvertureDroits).toBe('OUVERTS');
    expect(received!.heuresExcedentaires).toBe(13);
    expect(received!.statut).toBe('DROITS_OUVERTS');
  });

  it('get() GETs the figured endpoint and maps the cachets conversion (annexe 10)', () => {
    let received: IntermittentSpectacleAreResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      annexe: 'ANNEXE_10_ARTISTES',
      heuresTravaillees12Mois: 300,
      nombreCachets: 15,
      heuresCachets: 180,
      heuresTotalesRetenues: 480,
      ouvertureDroits: 'NON_OUVERTS',
      heuresManquantes: 27,
      heuresExcedentaires: 0,
      statut: 'DROITS_NON_OUVERTS',
    }));
    expect(received!.heuresCachets).toBe(180);
    expect(received!.ouvertureDroits).toBe('NON_OUVERTS');
    expect(received!.heuresManquantes).toBe(27);
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

  it('maps an A_VERIFIER statut (total dans la marge ± 10 h du seuil)', () => {
    let received: IntermittentSpectacleAreResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      heuresTravaillees12Mois: 505,
      heuresTotalesRetenues: 505,
      ouvertureDroits: 'NON_OUVERTS',
      heuresManquantes: 2,
      statut: 'A_VERIFIER',
    }));
    expect(received!.statut).toBe('A_VERIFIER');
    expect(received!.heuresManquantes).toBe(2);
  });
});
