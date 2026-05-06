import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { RisqueStatusService } from './risque-status.service';
import { RisqueStatus } from '../models/risque-status.model';

describe('RisqueStatusService — F-195 SF-195-02', () => {
  let service: RisqueStatusService;
  let http: HttpTestingController;

  const CASE_FILE_ID = '11111111-1111-1111-1111-111111111111';
  const URL = `/api/v1/case-files/${CASE_FILE_ID}/risques/status`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RisqueStatusService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(RisqueStatusService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CA-01 — PUT 200 retourne le statut upserté (statut VALIDE)', () => {
    const expected: RisqueStatus = {
      risqueLibelleOriginal: 'Harcèlement moral subi',
      statut: 'VALIDE',
      raisonEcarte: null,
      updatedAt: '2026-05-06T12:00:00Z',
    };
    let received: RisqueStatus | undefined;
    service.updateStatus(CASE_FILE_ID, 'Harcèlement moral subi', 'VALIDE')
      .subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      risqueLibelleOriginal: 'Harcèlement moral subi',
      statut: 'VALIDE',
      raisonEcarte: null,
    });
    req.flush(expected);
    expect(received).toEqual(expected);
  });

  it('CA-02 — PUT inclut raisonEcarte si fourni (statut ECARTE)', () => {
    let received: RisqueStatus | undefined;
    service.updateStatus(
      CASE_FILE_ID,
      'Clause non-concurrence abusive',
      'ECARTE',
      { raisonEcarte: 'Clause levée à l\'amiable' },
    ).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({
      risqueLibelleOriginal: 'Clause non-concurrence abusive',
      statut: 'ECARTE',
      raisonEcarte: 'Clause levée à l\'amiable',
    });
    req.flush({
      risqueLibelleOriginal: 'Clause non-concurrence abusive',
      statut: 'ECARTE',
      raisonEcarte: 'Clause levée à l\'amiable',
    });
    expect(received?.raisonEcarte).toBe('Clause levée à l\'amiable');
  });

  it('A_CREUSER reset — PUT envoie raisonEcarte=null', () => {
    service.updateStatus(CASE_FILE_ID, 'Risque flou', 'A_CREUSER').subscribe();
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({
      risqueLibelleOriginal: 'Risque flou',
      statut: 'A_CREUSER',
      raisonEcarte: null,
    });
    req.flush({});
  });

  it('CA-erreur — PUT 400 propage l\'erreur (le composant doit rollback)', () => {
    const errorSpy = jest.fn();
    service.updateStatus(CASE_FILE_ID, 'Risque X', 'VALIDE')
      .subscribe({ next: () => {}, error: errorSpy });
    http.expectOne(URL).flush(
      { message: 'statut invalide' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(errorSpy).toHaveBeenCalled();
  });

  it('CA-erreur — PUT 500 propage l\'erreur', () => {
    const errorSpy = jest.fn();
    service.updateStatus(CASE_FILE_ID, 'Risque X', 'VALIDE')
      .subscribe({ next: () => {}, error: errorSpy });
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(errorSpy).toHaveBeenCalled();
  });

  it('update() expose la version "payload complet" pour usages avancés', () => {
    service.update(CASE_FILE_ID, {
      risqueLibelleOriginal: 'Discrimination',
      statut: 'VALIDE',
    }).subscribe();
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({
      risqueLibelleOriginal: 'Discrimination',
      statut: 'VALIDE',
    });
    req.flush({});
  });
});
