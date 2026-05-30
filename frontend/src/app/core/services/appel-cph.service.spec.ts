import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AppelCphService } from './appel-cph.service';
import {
  AppelCphRequest,
  AppelCphResponse,
} from '../models/appel-cph.model';

describe('AppelCphService', () => {
  let service: AppelCphService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/appel-cph-analysis';

  function response(overrides: Partial<AppelCphResponse> = {}): AppelCphResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationJugement: '2026-01-15',
      partieAppelante: 'SALARIE',
      modeNotification: 'SIGNIFICATION',
      representationConstituee: 'AVOCAT',
      jugementEnDernierRessort: false,
      dateLimiteAppel: '2026-02-15',
      joursRestants: 20,
      verdict: 'DELAI_OUVERT',
      checklist: [
        { libelle: 'Déclaration d\'appel RPVA', obligatoire: true, bloquant: false, baseJuridique: 'art. 901 CPC' },
      ],
      renvoiPourvoiCassation: null,
      country: 'FRANCE',
      baseJuridique: 'art. 538 CPC ; R. 1461-1 CPC',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AppelCphService],
    });
    service = TestBed.inject(AppelCphService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(AppelCphService.STANDALONE_TOOL_ID).toBe('F-DT-86-appel-cph-cour-appel');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: AppelCphRequest = {
      dateNotificationJugement: '2026-01-15',
      partieAppelante: 'SALARIE',
      modeNotification: 'SIGNIFICATION',
      representationConstituee: 'AVOCAT',
      jugementEnDernierRessort: false,
    };
    let result: AppelCphResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.verdict).toBe('DELAI_OUVERT');
    expect(result!.joursRestants).toBe(20);
    expect(result!.dateLimiteAppel).toBe('2026-02-15');
    expect(result!.checklist.length).toBe(1);
  });

  it('get() GETs from the right URL and maps a VOIE_FERMEE verdict', () => {
    let result: AppelCphResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      verdict: 'VOIE_FERMEE',
      jugementEnDernierRessort: true,
      joursRestants: 0,
      renvoiPourvoiCassation: 'Pourvoi en cassation devant la chambre sociale — voir F-DT-87',
    }));
    expect(result!.verdict).toBe('VOIE_FERMEE');
    expect(result!.jugementEnDernierRessort).toBe(true);
    expect(result!.renvoiPourvoiCassation).toContain('F-DT-87');
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
