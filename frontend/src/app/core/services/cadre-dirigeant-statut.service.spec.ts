import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CadreDirigeantStatutService } from './cadre-dirigeant-statut.service';
import {
  CadreDirigeantStatutRequest,
  CadreDirigeantStatutResponse,
} from '../models/cadre-dirigeant-statut.model';

describe('CadreDirigeantStatutService', () => {
  let service: CadreDirigeantStatutService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/cadre-dirigeant-statut-analysis';

  function response(overrides: Partial<CadreDirigeantStatutResponse> = {}): CadreDirigeantStatutResponse {
    return {
      caseFileId: 'case-1',
      independanceEmploiDuTemps: true,
      autonomieDecision: true,
      remunerationParmiPlusElevees: true,
      participationDirectionEntreprise: true,
      niveauRemunerationConstate: 9000,
      criteres: [
        { critere: 'Indépendance dans l\'organisation de l\'emploi du temps', rempli: true, commentaire: '' },
        { critere: 'Autonomie décisionnelle', rempli: true, commentaire: '' },
        { critere: 'Rémunération parmi les plus élevées', rempli: true, commentaire: '' },
      ],
      criteresRemplis: 3,
      qualification: 'CADRE_DIRIGEANT',
      exclusionDureeTravail: 'EXCLU',
      risqueRappelHeuresSupp: 'FAIBLE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.3111-2 CT ; Cass. soc. (à vérifier par avocat)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CadreDirigeantStatutService],
    });
    service = TestBed.inject(CadreDirigeantStatutService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(CadreDirigeantStatutService.STANDALONE_TOOL_ID).toBe('F-DT-107-cadre-dirigeant-statut');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: CadreDirigeantStatutRequest = {
      independanceEmploiDuTemps: true,
      autonomieDecision: true,
      remunerationParmiPlusElevees: true,
      participationDirectionEntreprise: true,
      niveauRemunerationConstate: 9000,
    };
    let received: CadreDirigeantStatutResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.qualification).toBe('CADRE_DIRIGEANT');
    expect(received!.exclusionDureeTravail).toBe('EXCLU');
    expect(received!.risqueRappelHeuresSupp).toBe('FAIBLE');
  });

  it('get() GETs the figured endpoint and maps a FRAGILE qualification', () => {
    let received: CadreDirigeantStatutResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      participationDirectionEntreprise: false,
      qualification: 'CADRE_DIRIGEANT_FRAGILE',
      risqueRappelHeuresSupp: 'MODERE',
    }));
    expect(received!.qualification).toBe('CADRE_DIRIGEANT_FRAGILE');
    expect(received!.risqueRappelHeuresSupp).toBe('MODERE');
    expect(received!.criteresRemplis).toBe(3);
  });

  it('get() maps a NON_CADRE_DIRIGEANT result (< 3 critères)', () => {
    let received: CadreDirigeantStatutResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      remunerationParmiPlusElevees: false,
      criteres: [
        { critere: 'Indépendance', rempli: true, commentaire: '' },
        { critere: 'Autonomie', rempli: true, commentaire: '' },
        { critere: 'Rémunération', rempli: false, commentaire: 'Non établi' },
      ],
      criteresRemplis: 2,
      qualification: 'NON_CADRE_DIRIGEANT',
      exclusionDureeTravail: 'NON_EXCLU',
      risqueRappelHeuresSupp: 'ELEVE',
    }));
    expect(received!.qualification).toBe('NON_CADRE_DIRIGEANT');
    expect(received!.exclusionDureeTravail).toBe('NON_EXCLU');
    expect(received!.risqueRappelHeuresSupp).toBe('ELEVE');
    expect(received!.criteresRemplis).toBe(2);
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
