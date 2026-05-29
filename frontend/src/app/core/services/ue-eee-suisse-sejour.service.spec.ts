import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { UeEeeSuisseSejourService } from './ue-eee-suisse-sejour.service';
import {
  UeEeeSuisseSejourRequest,
  UeEeeSuisseSejourResponse,
} from '../models/ue-eee-suisse-sejour.model';

describe('UeEeeSuisseSejourService', () => {
  let service: UeEeeSuisseSejourService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/ue-eee-suisse-sejour-analysis';

  function response(overrides: Partial<UeEeeSuisseSejourResponse> = {}): UeEeeSuisseSejourResponse {
    return {
      caseFileId: 'case-1',
      nationalite: 'Italienne',
      estCitoyenUE: true,
      membreFamilleNonUE: false,
      dureeSejourMois: 72,
      activiteProfessionnelle: 'SALARIE',
      country: 'FRANCE',
      droitSejourAutomatique3Mois: true,
      droitSejourPlus5Ans: true,
      titreObtenu: 'ATTESTATION_ENREGISTREMENT',
      conditionsRespectees: ['Séjour régulier de plus de 5 ans', 'Activité salariée'],
      situationMembreNonUE: null,
      baseJuridique: 'Directive 2004/38/CE — art. L. 233-1 et s. du CESEDA',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UeEeeSuisseSejourService],
    });
    service = TestBed.inject(UeEeeSuisseSejourService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(UeEeeSuisseSejourService.STANDALONE_TOOL_ID).toBe('F-IM-44-ue-eee-suisse-sejour-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: UeEeeSuisseSejourRequest = {
      nationalite: 'Italienne',
      estCitoyenUE: true,
      membreFamilleNonUE: false,
      dureeSejourMois: 72,
      activiteProfessionnelle: 'SALARIE',
    };
    let result: UeEeeSuisseSejourResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.droitSejourPlus5Ans).toBe(true);
    expect(result!.titreObtenu).toBe('ATTESTATION_ENREGISTREMENT');
    expect(result!.conditionsRespectees.length).toBe(2);
  });

  it('get() GETs from the right URL', () => {
    let result: UeEeeSuisseSejourResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({
      droitSejourPlus5Ans: false,
      titreObtenu: 'CARTE_SEJOUR_MEMBRE_FAMILLE',
      situationMembreNonUE: 'Le conjoint non-UE relève d\'une carte de séjour « membre de famille ».',
    }));
    expect(result!.droitSejourPlus5Ans).toBe(false);
    expect(result!.titreObtenu).toBe('CARTE_SEJOUR_MEMBRE_FAMILLE');
    expect(result!.situationMembreNonUE).toContain('membre de famille');
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
