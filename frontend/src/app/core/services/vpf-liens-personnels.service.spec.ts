import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { VpfLiensPersonnelsService } from './vpf-liens-personnels.service';
import {
  VpfLiensPersonnelsRequest,
  VpfLiensPersonnelsResponse,
} from '../models/vpf-liens-personnels.model';

describe('VpfLiensPersonnelsService', () => {
  let service: VpfLiensPersonnelsService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/vpf-liens-personnels-analysis';

  function response(overrides: Partial<VpfLiensPersonnelsResponse> = {}): VpfLiensPersonnelsResponse {
    return {
      caseFileId: 'case-1',
      dureeResidenceFranceMois: 120,
      entreeEnFranceMineur: true,
      enfantsEnFrance: true,
      conjointEnFrance: false,
      parentsEnFrance: false,
      situationFamilialeAlEtranger: null,
      niveauIntegration: 'FORT',
      ancienneConvictionPenale: false,
      country: 'FRANCE',
      verdict: 'ELIGIBLE_PROBABLE',
      score: 78,
      chipsCriteresNonRemplis: [],
      recommandations: ['Joindre les justificatifs de scolarité'],
      baseJuridique: 'CESEDA L.423-23',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [VpfLiensPersonnelsService],
    });
    service = TestBed.inject(VpfLiensPersonnelsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID matching the registry key', () => {
    expect(VpfLiensPersonnelsService.STANDALONE_TOOL_ID).toBe('F-IM-27-vpf-liens-personnels-l42323-fr');
  });

  it('analyze() POSTs request to the correct URL and returns the response', () => {
    const request: VpfLiensPersonnelsRequest = {
      dureeResidenceFranceMois: 120,
      entreeEnFranceMineur: true,
      enfantsEnFrance: true,
      conjointEnFrance: false,
      parentsEnFrance: false,
      situationFamilialeAlEtranger: null,
      niveauIntegration: 'FORT',
      ancienneConvictionPenale: false,
    };
    let received: VpfLiensPersonnelsResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());

    expect(received!.verdict).toBe('ELIGIBLE_PROBABLE');
    expect(received!.score).toBe(78);
  });

  it('get() GETs the analysis from the correct URL', () => {
    let received: VpfLiensPersonnelsResponse | undefined;
    service.get('case-1').subscribe((r) => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ verdict: 'DOSSIER_A_CONSOLIDER', score: 42 }));

    expect(received!.verdict).toBe('DOSSIER_A_CONSOLIDER');
    expect(received!.score).toBe(42);
  });

  it('get() propagates a 404 error', () => {
    let errStatus: number | undefined;
    service.get('case-1').subscribe({ error: (e) => (errStatus = e.status) });

    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'Not Found' });
    expect(errStatus).toBe(404);
  });
});
