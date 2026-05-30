import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ApprentissageRuptureService } from './apprentissage-rupture.service';
import {
  ApprentissageRuptureRequest,
  ApprentissageRuptureResponse,
} from '../models/apprentissage-rupture.model';

describe('ApprentissageRuptureService', () => {
  let service: ApprentissageRuptureService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/apprentissage-rupture-analysis';

  function response(): ApprentissageRuptureResponse {
    return {
      caseFileId: 'case-1',
      dateDebutContrat: '2026-01-06',
      dateRupture: '2026-04-06',
      auteurRupture: 'EMPLOYEUR',
      motifRupture: 'SANS_MOTIF',
      apprentiMajeur: true,
      joursDepuisDebut: 90,
      periode: 'APRES_45_JOURS',
      validite: 'NON_VALIDE',
      motif: 'Rupture sans motif après 45 jours : rupture irrégulière.',
      consequences: ['Saisine du conseil de prud\'hommes.'],
      verdictGlobal: 'RUPTURE_IRREGULIERE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.6222-18 CT (à vérifier par avocat)',
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApprentissageRuptureService],
    });
    service = TestBed.inject(ApprentissageRuptureService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('exposes the STANDALONE_TOOL_ID aligned on TOOL_REGISTRY', () => {
    expect(ApprentissageRuptureService.STANDALONE_TOOL_ID)
      .toBe('F-DT-110-apprentissage-rupture');
  });

  it('analyze() POSTs the request to the analysis endpoint', () => {
    const request: ApprentissageRuptureRequest = {
      dateDebutContrat: '2026-01-06',
      dateRupture: '2026-04-06',
      auteurRupture: 'EMPLOYEUR',
      motifRupture: 'SANS_MOTIF',
      apprentiMajeur: true,
    };
    let received: ApprentissageRuptureResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(received!.verdictGlobal).toBe('RUPTURE_IRREGULIERE');
    expect(received!.periode).toBe('APRES_45_JOURS');
  });

  it('get() GETs the analysis endpoint', () => {
    let received: ApprentissageRuptureResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response());
    expect(received!.caseFileId).toBe('case-1');
  });

  it('get() propagates a 404 when no analysis exists', () => {
    let errStatus: number | undefined;
    service.get('case-1').subscribe({ error: (e) => (errStatus = e.status) });
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'Not Found' });
    expect(errStatus).toBe(404);
  });
});
