import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CarteResidentService } from './carte-resident.service';
import { CarteResidentRequest, CarteResidentResponse } from '../models/carte-resident.model';

describe('CarteResidentService', () => {
  let service: CarteResidentService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/carte-resident-analysis';

  function response(overrides: Partial<CarteResidentResponse> = {}): CarteResidentResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      verdict: 'ELIGIBLE',
      chipsCriteresNonRemplis: [],
      atouts: ['Séjour régulier de 5 ans continu'],
      baseJuridique: 'CESEDA L. 426-1',
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CarteResidentService],
    });
    service = TestBed.inject(CarteResidentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(CarteResidentService.STANDALONE_TOOL_ID).toBe('F-IM-36-carte-resident-l4261-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: CarteResidentRequest = {
      dureeSejourRegulierAnnees: 5,
      typesTitresAnterieurs: 'VPF',
      niveauIntegration: 'FORT',
      ressourcesMensuellesNettes: 1850,
      condamnationsPenalesGraves: false,
    };
    let result: CarteResidentResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.verdict).toBe('ELIGIBLE');
    expect(result!.atouts.length).toBe(1);
  });

  it('get() GETs from the right URL', () => {
    let result: CarteResidentResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ verdict: 'INADMISSIBLE', chipsCriteresNonRemplis: ['Condamnation pénale grave'] }));
    expect(result!.verdict).toBe('INADMISSIBLE');
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
