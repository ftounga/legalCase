import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { RetainedPisteAlignmentService } from './retained-piste-alignment.service';
import { RetainedPisteAlignment } from '../models/retained-piste-alignment.model';

describe('RetainedPisteAlignmentService — F-192 SF-192-02', () => {
  let service: RetainedPisteAlignmentService;
  let http: HttpTestingController;

  const CASE_FILE_ID = '11111111-1111-1111-1111-111111111111';
  const URL = `/api/v1/case-files/${CASE_FILE_ID}/retained-pistes-alignment`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RetainedPisteAlignmentService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(RetainedPisteAlignmentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CA-01 — GET 200 returns alignment array', () => {
    const payload: RetainedPisteAlignment[] = [
      {
        pisteId: 'p1',
        texte: 'Demander le statut Passeport Talent — Chercheur',
        baseJuridique: 'L.421-14 CESEDA',
        horizonTemporel: 'Court terme',
        conditions: ['Doctorat obtenu'],
        toolIdCible: 'F-IM-05-arbre-decisionnel-titre',
        matchStatus: 'ALIGNED',
      },
    ];
    let received: RetainedPisteAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('GET');
    req.flush(payload);
    expect(received).toEqual(payload);
  });

  it('CA-10 fail-open — GET 404 returns []', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: RetainedPisteAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 404, statusText: 'Not Found' });
    expect(received).toEqual([]);
    warnSpy.mockRestore();
  });

  it('CA-10 fail-open — GET 500 returns [] + warn', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    let received: RetainedPisteAlignment[] | undefined;
    service.getForCaseFile(CASE_FILE_ID).subscribe(v => (received = v));
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(received).toEqual([]);
    expect(warnSpy).toHaveBeenCalled();
    warnSpy.mockRestore();
  });
});
