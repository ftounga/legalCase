import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AppelCphService } from './appel-cph.service';
import { AppelCphRequest, AppelCphResponse } from '../models/appel-cph.model';

/**
 * SF-218-02 — Tests du service HttpClient `AppelCphService` (F-DT-86).
 */
describe('AppelCphService', () => {
  let service: AppelCphService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/appel-cph-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AppelCphService],
    });
    service = TestBed.inject(AppelCphService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('expose le tool id canonique', () => {
    expect(AppelCphService.STANDALONE_TOOL_ID).toBe('F-DT-86-appel-cph-cour-appel');
  });

  it('calculate → POST sur la bonne URL avec le body', () => {
    const request: AppelCphRequest = {
      dateNotificationJugement: '2026-05-10',
      partieAppelante: 'SALARIE',
      modeNotification: 'SIGNIFICATION',
      representationConstituee: 'AVOCAT',
      jugementEnDernierRessort: false,
    };
    let received: AppelCphResponse | undefined;
    service.calculate('case-1', request).subscribe(r => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateNotificationJugement).toBe('2026-05-10');
    req.flush({ caseFileId: 'case-1', statut: 'DELAI_OUVERT', joursRestants: 20 } as AppelCphResponse);

    expect(received?.statut).toBe('DELAI_OUVERT');
  });

  it('get → GET sur la bonne URL', () => {
    let received: AppelCphResponse | undefined;
    service.get('case-1').subscribe(r => (received = r));

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ caseFileId: 'case-1', statut: 'VOIE_FERMEE', joursRestants: 0 } as AppelCphResponse);

    expect(received?.statut).toBe('VOIE_FERMEE');
  });

  it('get → propage une erreur 404', () => {
    let errored = false;
    service.get('case-1').subscribe({ error: () => (errored = true) });

    httpMock.expectOne(BASE_URL).flush(null, { status: 404, statusText: 'Not Found' });
    expect(errored).toBe(true);
  });
});
