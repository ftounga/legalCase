import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { ProcedureCheckAlignmentService } from './procedure-check-alignment.service';
import { ProcedureCheckAlignment } from '../models/procedure-check-alignment.model';

describe('ProcedureCheckAlignmentService — F-193 SF-193-02', () => {
  let service: ProcedureCheckAlignmentService;
  let http: HttpTestingController;

  const CASE_FILE_ID = '22222222-2222-2222-2222-222222222222';
  const URL = `/api/v1/case-files/${CASE_FILE_ID}/procedure-checks-alignment`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProcedureCheckAlignmentService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ProcedureCheckAlignmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CA-01 — GET 200 returns alignment array', () => {
    const payload: ProcedureCheckAlignment[] = [
      {
        checkId: 'c1',
        libelle: 'Notification du licenciement par LRAR',
        critereCode: 'LICENCIEMENT_NOTIFICATION',
        statut: 'VERIFIED',
        expectedValue: 'LRAR',
        raison: null,
        toolIdCible: 'F-DT-08-licenciement-validity',
        matchStatus: 'ALIGNED',
      },
    ];
    let received: ProcedureCheckAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('GET');
    req.flush(payload);
    expect(received).toEqual(payload);
  });

  it('CA-13 fail-open — GET 404 returns []', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: ProcedureCheckAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 404, statusText: 'Not Found' });
    expect(received).toEqual([]);
    warnSpy.mockRestore();
  });

  it('CA-13 fail-open — GET 500 returns [] + warn', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: ProcedureCheckAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(received).toEqual([]);
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });
});
