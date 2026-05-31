import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { HarcelementProcedureInterneService } from './harcelement-procedure-interne.service';
import {
  HarcelementProcedureInterneRequest,
  HarcelementProcedureInterneResponse,
} from '../models/harcelement-procedure-interne.model';

describe('HarcelementProcedureInterneService', () => {
  let service: HarcelementProcedureInterneService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/harcelement-procedure-interne-analysis';

  function response(overrides: Partial<HarcelementProcedureInterneResponse> = {}): HarcelementProcedureInterneResponse {
    return {
      caseFileId: 'case-1',
      effectif: 50,
      referentCseDesigne: true,
      referentEmployeurDesigne: false,
      informationAffichageRealisee: true,
      signalementRecu: true,
      dateSignalement: '2026-01-01',
      dateOuvertureEnquete: '2026-01-08',
      enqueteContradictoire: true,
      mesuresConservatoiresPrises: true,
      checklist: [
        { item: 'Référent CSE harcèlement sexuel désigné', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Information / affichage des salariés', conforme: true, obligatoire: true, commentaire: '' },
        { item: 'Enquête contradictoire', conforme: true, obligatoire: true, commentaire: '' },
      ],
      itemsObligatoiresManquants: 0,
      statut: 'CONFORME',
      delaiReactionJours: 7,
      delaiRaisonnable: 'OUI',
      risqueResponsabiliteEmployeur: 'FAIBLE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.1153-5-1, L.2314-1, L.1152-4 CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [HarcelementProcedureInterneService],
    });
    service = TestBed.inject(HarcelementProcedureInterneService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(HarcelementProcedureInterneService.STANDALONE_TOOL_ID).toBe('F-DT-59-harcelement-procedure-interne');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: HarcelementProcedureInterneRequest = {
      effectif: 50,
      referentCseDesigne: true,
      referentEmployeurDesigne: false,
      informationAffichageRealisee: true,
      signalementRecu: true,
      dateSignalement: '2026-01-01',
      dateOuvertureEnquete: '2026-01-08',
      enqueteContradictoire: true,
      mesuresConservatoiresPrises: true,
    };
    let received: HarcelementProcedureInterneResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.statut).toBe('CONFORME');
    expect(received!.risqueResponsabiliteEmployeur).toBe('FAIBLE');
    expect(received!.delaiRaisonnable).toBe('OUI');
  });

  it('get() GETs the figured endpoint and maps a NON_CONFORME result', () => {
    let received: HarcelementProcedureInterneResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      referentCseDesigne: false,
      itemsObligatoiresManquants: 1,
      statut: 'NON_CONFORME',
      risqueResponsabiliteEmployeur: 'MODERE',
    }));
    expect(received!.statut).toBe('NON_CONFORME');
    expect(received!.risqueResponsabiliteEmployeur).toBe('MODERE');
    expect(received!.itemsObligatoiresManquants).toBe(1);
  });

  it('get() maps a CARENCE_GRAVE result (signalement sans enquête contradictoire)', () => {
    let received: HarcelementProcedureInterneResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      enqueteContradictoire: false,
      dateOuvertureEnquete: null,
      delaiReactionJours: null,
      itemsObligatoiresManquants: 2,
      statut: 'CARENCE_GRAVE',
      delaiRaisonnable: 'NON',
      risqueResponsabiliteEmployeur: 'ELEVE',
    }));
    expect(received!.statut).toBe('CARENCE_GRAVE');
    expect(received!.delaiRaisonnable).toBe('NON');
    expect(received!.risqueResponsabiliteEmployeur).toBe('ELEVE');
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
