import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ReglementInterieurValiditeService } from './reglement-interieur-validite.service';
import {
  ReglementInterieurValiditeRequest,
  ReglementInterieurValiditeResponse,
} from '../models/reglement-interieur-validite.model';

describe('ReglementInterieurValiditeService', () => {
  let service: ReglementInterieurValiditeService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/reglement-interieur-validite-analysis';

  function response(overrides: Partial<ReglementInterieurValiditeResponse> = {}): ReglementInterieurValiditeResponse {
    return {
      caseFileId: 'case-1',
      effectif: 80,
      reglementExiste: true,
      checklist: [
        { item: 'Mesures d\'hygiène et de sécurité', conforme: true, type: 'OBLIGATOIRE', commentaire: '' },
        { item: 'Absence de sanction pécuniaire', conforme: true, type: 'INTERDIT', commentaire: '' },
        { item: 'Consultation du CSE', conforme: true, type: 'PROCEDURE', commentaire: '' },
      ],
      itemsObligatoiresManquants: 0,
      clausesInterditesPresentes: 0,
      statut: 'CONFORME',
      opposabilite: 'OPPOSABLE',
      consequences: ['Le règlement intérieur est opposable aux salariés.'],
      country: 'FRANCE',
      baseJuridique: 'Art. L.1311-1 à L.1322-4, L.1321-1 et s. CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReglementInterieurValiditeService],
    });
    service = TestBed.inject(ReglementInterieurValiditeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(ReglementInterieurValiditeService.STANDALONE_TOOL_ID).toBe('F-DT-100-reglement-interieur-validite');
  });

  it('analyze() POSTs to the figured endpoint with the request body', () => {
    const request: ReglementInterieurValiditeRequest = {
      effectif: 80,
      reglementExiste: true,
      contenuHygieneSecurite: true,
      contenuDiscipline: true,
      contenuDroitsDefense: true,
      contenuHarcelementAgissements: true,
      clauseAtteinteLibertesNonJustifiee: false,
      clauseSanctionPecuniaire: false,
      consultationCseRealisee: true,
      transmissionInspectionTravail: true,
      depotGreffeCph: true,
    };
    let received: ReglementInterieurValiditeResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.statut).toBe('CONFORME');
    expect(received!.opposabilite).toBe('OPPOSABLE');
  });

  it('get() GETs the figured endpoint and maps a NON_CONFORME result (contenu manquant)', () => {
    let received: ReglementInterieurValiditeResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      itemsObligatoiresManquants: 1,
      statut: 'NON_CONFORME',
      opposabilite: 'OPPOSABLE',
    }));
    expect(received!.statut).toBe('NON_CONFORME');
    expect(received!.itemsObligatoiresManquants).toBe(1);
  });

  it('get() maps an INOPPOSABLE result (procédure non respectée)', () => {
    let received: ReglementInterieurValiditeResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      statut: 'INOPPOSABLE',
      opposabilite: 'INOPPOSABLE',
    }));
    expect(received!.statut).toBe('INOPPOSABLE');
    expect(received!.opposabilite).toBe('INOPPOSABLE');
  });

  it('get() maps a NON_REQUIS result (effectif < 50 sans RI)', () => {
    let received: ReglementInterieurValiditeResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      effectif: 20,
      reglementExiste: false,
      checklist: [],
      statut: 'NON_REQUIS',
      opposabilite: 'INOPPOSABLE',
      consequences: [],
    }));
    expect(received!.statut).toBe('NON_REQUIS');
    expect(received!.checklist.length).toBe(0);
  });

  it('get() maps a clause interdite result (clausesInterditesPresentes >= 1)', () => {
    let received: ReglementInterieurValiditeResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    httpMock.expectOne(BASE_URL).flush(response({
      clausesInterditesPresentes: 1,
      statut: 'NON_CONFORME',
    }));
    expect(received!.clausesInterditesPresentes).toBe(1);
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
