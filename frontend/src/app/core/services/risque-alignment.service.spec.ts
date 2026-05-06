import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { RisqueAlignmentService } from './risque-alignment.service';
import { RisqueAlignment } from '../models/risque-alignment.model';

describe('RisqueAlignmentService — F-195 SF-195-02', () => {
  let service: RisqueAlignmentService;
  let http: HttpTestingController;

  const CASE_FILE_ID = '33333333-3333-3333-3333-333333333333';
  const URL = `/api/v1/case-files/${CASE_FILE_ID}/risques-alignment`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RisqueAlignmentService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(RisqueAlignmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CA-04 — GET 200 retourne le tableau d\'alignement', () => {
    const payload: RisqueAlignment[] = [
      {
        risqueLibelle: 'Harcèlement moral subi',
        statut: 'VALIDE',
        toolIdsCibles: ['F-DT-11-harcelement-licenciement-nul'],
        raisonEcarte: null,
      },
      {
        risqueLibelle: 'Clause non-concurrence abusive',
        statut: 'ECARTE',
        toolIdsCibles: ['F-DT-24-non-concurrence'],
        raisonEcarte: 'Clause levée à l\'amiable',
      },
    ];
    let received: RisqueAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('GET');
    req.flush(payload);
    expect(received).toEqual(payload);
  });

  it('CA-08 fail-open — GET 404 retourne []', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: RisqueAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 404, statusText: 'Not Found' });
    expect(received).toEqual([]);
    warnSpy.mockRestore();
  });

  it('CA-08 fail-open — GET 500 retourne [] + warn', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: RisqueAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(received).toEqual([]);
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });
});
