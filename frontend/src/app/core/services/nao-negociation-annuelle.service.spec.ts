import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { NaoNegociationAnnuelleService } from './nao-negociation-annuelle.service';
import {
  NaoNegociationAnnuelleRequest,
  NaoNegociationAnnuelleResponse,
} from '../models/nao-negociation-annuelle.model';

describe('NaoNegociationAnnuelleService', () => {
  let service: NaoNegociationAnnuelleService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/nao-negociation-annuelle-analysis';

  function response(overrides: Partial<NaoNegociationAnnuelleResponse> = {}): NaoNegociationAnnuelleResponse {
    return {
      caseFileId: 'case-1',
      effectif: 80,
      delegueSyndicalPresent: true,
      blocRemunerationNegocie: true,
      blocEgaliteQvtNegocie: true,
      accordMethodePeriodicite: false,
      dateDerniereNegociation: '2026-02-01',
      periodiciteMois: 12,
      pvDesaccordEtabli: false,
      negociationAboutie: true,
      checklist: [
        { item: 'Bloc rémunération négocié', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Bloc égalité pro / QVT négocié', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Périodicité respectée', conforme: true, obligatoire: true, commentaire: '' },
      ],
      itemsObligatoiresManquants: 0,
      statut: 'CONFORME',
      dateProchaineEcheance: '2027-02-01',
      joursAvantEcheance: 200,
      statutEcheance: 'A_JOUR',
      risqueEntrave: 'FAIBLE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.2242-1 à L.2242-8 CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NaoNegociationAnnuelleService],
    });
    service = TestBed.inject(NaoNegociationAnnuelleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(NaoNegociationAnnuelleService.STANDALONE_TOOL_ID).toBe('F-DT-66-nao-negociation-annuelle');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: NaoNegociationAnnuelleRequest = {
      effectif: 80,
      delegueSyndicalPresent: true,
      blocRemunerationNegocie: true,
      blocEgaliteQvtNegocie: true,
      accordMethodePeriodicite: false,
      dateDerniereNegociation: '2026-02-01',
      periodiciteMois: 12,
      pvDesaccordEtabli: false,
      negociationAboutie: true,
    };
    let received: NaoNegociationAnnuelleResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.statut).toBe('CONFORME');
    expect(received!.risqueEntrave).toBe('FAIBLE');
    expect(received!.statutEcheance).toBe('A_JOUR');
  });

  it('get() GETs the figured endpoint and maps a NON_CONFORME result (échéance dépassée)', () => {
    let received: NaoNegociationAnnuelleResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      itemsObligatoiresManquants: 1,
      statut: 'NON_CONFORME',
      statutEcheance: 'DEPASSEE',
      joursAvantEcheance: -30,
      risqueEntrave: 'MODERE',
    }));
    expect(received!.statut).toBe('NON_CONFORME');
    expect(received!.statutEcheance).toBe('DEPASSEE');
    expect(received!.risqueEntrave).toBe('MODERE');
    expect(received!.itemsObligatoiresManquants).toBe(1);
  });

  it('get() maps a NON_APPLICABLE result (pas de délégué syndical)', () => {
    let received: NaoNegociationAnnuelleResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      delegueSyndicalPresent: false,
      checklist: [],
      itemsObligatoiresManquants: 0,
      statut: 'NON_APPLICABLE',
      dateProchaineEcheance: null,
      joursAvantEcheance: null,
      statutEcheance: null,
      risqueEntrave: 'FAIBLE',
    }));
    expect(received!.statut).toBe('NON_APPLICABLE');
    expect(received!.statutEcheance).toBeNull();
    expect(received!.checklist.length).toBe(0);
  });

  it('get() maps an ELEVE risque result (blocs non négociés)', () => {
    let received: NaoNegociationAnnuelleResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      blocRemunerationNegocie: false,
      blocEgaliteQvtNegocie: false,
      itemsObligatoiresManquants: 2,
      statut: 'NON_CONFORME',
      risqueEntrave: 'ELEVE',
    }));
    expect(received!.risqueEntrave).toBe('ELEVE');
    expect(received!.itemsObligatoiresManquants).toBe(2);
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
