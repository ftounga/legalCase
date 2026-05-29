import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AutorisationTravailEmployeurService } from './autorisation-travail-employeur.service';
import {
  AutorisationTravailEmployeurRequest,
  AutorisationTravailEmployeurResponse,
} from '../models/autorisation-travail-employeur.model';

describe('AutorisationTravailEmployeurService', () => {
  let service: AutorisationTravailEmployeurService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/autorisation-travail-employeur-analysis';

  function response(overrides: Partial<AutorisationTravailEmployeurResponse> = {}): AutorisationTravailEmployeurResponse {
    return {
      caseFileId: 'case-1',
      typeContrat: 'CDI',
      posteProposes: 'Développeur',
      nationaliteCandidat: 'Algérienne',
      dureeContratMois: null,
      refusAutorisation: false,
      dateRefusAutorisation: null,
      country: 'FRANCE',
      statut: 'AUTORISATION_REQUISE',
      obligationsDemande: ['Saisine de la plateforme dématérialisée'],
      delaiInstructionOFII: '2 mois',
      recoursPossible: false,
      delaiRecoursTa: null,
      taxeOFII: '55 % du SMIC mensuel',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AutorisationTravailEmployeurService],
    });
    service = TestBed.inject(AutorisationTravailEmployeurService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(AutorisationTravailEmployeurService.STANDALONE_TOOL_ID).toBe('F-IM-46-autorisation-travail-employeur-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: AutorisationTravailEmployeurRequest = {
      typeContrat: 'CDD',
      posteProposes: 'Cuisinier',
      nationaliteCandidat: 'Marocaine',
      dureeContratMois: 12,
      refusAutorisation: true,
      dateRefusAutorisation: '2026-01-15',
    };
    let result: AutorisationTravailEmployeurResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response({ statut: 'RECOURS_POSSIBLE', recoursPossible: true, delaiRecoursTa: '2026-03-16' }));
    expect(result!.statut).toBe('RECOURS_POSSIBLE');
    expect(result!.delaiRecoursTa).toBe('2026-03-16');
  });

  it('get() GETs from the right URL', () => {
    let result: AutorisationTravailEmployeurResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'AUTORISATION_NON_REQUISE', obligationsDemande: [] }));
    expect(result!.statut).toBe('AUTORISATION_NON_REQUISE');
    expect(result!.obligationsDemande).toEqual([]);
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
