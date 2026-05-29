import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AssignationResidenceService } from './assignation-residence.service';
import {
  AssignationResidenceRequest,
  AssignationResidenceResponse,
} from '../models/assignation-residence.model';

describe('AssignationResidenceService', () => {
  let service: AssignationResidenceService;
  let httpMock: HttpTestingController;

  const BASE_URL = '/api/v1/case-files/case-1/assignation-residence-analysis';

  function response(overrides: Partial<AssignationResidenceResponse> = {}): AssignationResidenceResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationAssignation: '2026-01-15',
      dureeAssignationJours: 45,
      motifAssignation: 'EXECUTION_OQTF',
      obligationsPresentation: 'Présentation hebdomadaire',
      country: 'FRANCE',
      statut: 'EN_COURS',
      dateEcheanceAssignation: '2026-03-01',
      dureeTotaleAutoriseeJours: 45,
      renouvellementPossible: true,
      motifsContestation: ['Disproportion de la mesure', 'Vie privée et familiale'],
      recoursPossible: true,
      delaiRecoursTaHeures: 48,
      ...overrides,
    };
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AssignationResidenceService],
    });
    service = TestBed.inject(AssignationResidenceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('is created', () => {
    expect(service).toBeTruthy();
  });

  it('exposes STANDALONE_TOOL_ID aligned with TOOL_REGISTRY key', () => {
    expect(AssignationResidenceService.STANDALONE_TOOL_ID).toBe('F-IM-42-assignation-residence-fr');
  });

  it('analyze() POSTs to the right URL with the request body', () => {
    const request: AssignationResidenceRequest = {
      dateNotificationAssignation: '2026-01-15',
      dureeAssignationJours: 45,
      motifAssignation: 'EXECUTION_OQTF',
      obligationsPresentation: 'Présentation hebdomadaire',
    };
    let result: AssignationResidenceResponse | undefined;
    service.analyze('case-1', request).subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response());
    expect(result!.statut).toBe('EN_COURS');
    expect(result!.renouvellementPossible).toBe(true);
    expect(result!.delaiRecoursTaHeures).toBe(48);
  });

  it('get() GETs from the right URL', () => {
    let result: AssignationResidenceResponse | undefined;
    service.get('case-1').subscribe((r) => (result = r));
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response({ statut: 'EXPIRE', renouvellementPossible: false, recoursPossible: false, motifsContestation: [] }));
    expect(result!.statut).toBe('EXPIRE');
    expect(result!.renouvellementPossible).toBe(false);
    expect(result!.motifsContestation).toEqual([]);
    expect(result!.recoursPossible).toBe(false);
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
