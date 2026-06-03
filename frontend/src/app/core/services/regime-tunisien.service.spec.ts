import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { RegimeTunisienService } from './regime-tunisien.service';
import {
  RegimeTunisienRequest,
  RegimeTunisienResponse,
} from '../models/regime-tunisien.model';

describe('RegimeTunisienService', () => {
  let service: RegimeTunisienService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/regime-tunisien-analysis';

  function response(overrides: Partial<RegimeTunisienResponse> = {}): RegimeTunisienResponse {
    return {
      caseFileId: 'case-1',
      categorie: 'ETUDIANT',
      dureeSejourEnvisageeMois: 12,
      titreEnCours: false,
      dejaResident: false,
      country: 'FRANCE',
      regime: 'ACCORD_1988_DEROGATOIRE',
      particularitesApplicables: ['Carte étudiant accord 1988'],
      basesJuridiques: ['Accord franco-tunisien du 17/03/1988'],
      renvoiDroitCommun: false,
      messages: [],
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RegimeTunisienService],
    });
    service = TestBed.inject(RegimeTunisienService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(RegimeTunisienService.STANDALONE_TOOL_ID).toBe('F-IM-47-regime-tunisien-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: RegimeTunisienRequest = {
      categorie: 'ETUDIANT',
      dureeSejourEnvisageeMois: 12,
      titreEnCours: false,
      dejaResident: false,
    };
    let result: RegimeTunisienResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.regime).toBe('ACCORD_1988_DEROGATOIRE');
    expect(result!.renvoiDroitCommun).toBe(false);
  });

  it('get() GETs from the right URL', () => {
    let result: RegimeTunisienResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ regime: 'DROIT_COMMUN_CESEDA', renvoiDroitCommun: true, categorie: 'FAMILIAL' }));
    expect(result!.regime).toBe('DROIT_COMMUN_CESEDA');
    expect(result!.renvoiDroitCommun).toBe(true);
  });

  it('propagates backend error to the subscriber', () => {
    let errStatus: number | undefined;
    service.get('case-1').subscribe({
      next: () => fail('should not succeed'),
      error: (e) => (errStatus = e.status),
    });
    httpMock.expectOne(BASE_URL).flush({ message: 'boom' }, { status: 400, statusText: 'Bad Request' });
    expect(errStatus).toBe(400);
  });
});
