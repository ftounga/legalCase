import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { PieceManquanteStatusService } from './piece-manquante-status.service';
import { PieceManquanteStatus } from '../models/piece-manquante-status.model';

describe('PieceManquanteStatusService — F-194 SF-194-02', () => {
  let service: PieceManquanteStatusService;
  let http: HttpTestingController;

  const CASE_FILE_ID = '11111111-1111-1111-1111-111111111111';
  const URL = `/api/v1/case-files/${CASE_FILE_ID}/pieces-manquantes/status`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PieceManquanteStatusService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(PieceManquanteStatusService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('CA-01 — PUT 200 retourne le statut upserté (statut OBTENUE)', () => {
    const expected: PieceManquanteStatus = {
      pieceLibelleOriginal: 'Contrat de travail',
      statut: 'OBTENUE',
      raisonNonApp: null,
      destinataire: null,
      updatedAt: '2026-05-06T12:00:00Z',
    };
    let received: PieceManquanteStatus | undefined;
    service.updateStatus(CASE_FILE_ID, 'Contrat de travail', 'OBTENUE')
      .subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      pieceLibelleOriginal: 'Contrat de travail',
      statut: 'OBTENUE',
      raisonNonApp: null,
      destinataire: null,
      documentId: null,
    });
    req.flush(expected);
    expect(received).toEqual(expected);
  });

  it('CA-04 — PUT inclut destinataire si fourni (statut A_DEMANDER)', () => {
    let received: PieceManquanteStatus | undefined;
    service.updateStatus(
      CASE_FILE_ID,
      'Contrat de travail',
      'A_DEMANDER',
      { destinataire: 'Client' },
    ).subscribe(v => (received = v));
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({
      pieceLibelleOriginal: 'Contrat de travail',
      statut: 'A_DEMANDER',
      raisonNonApp: null,
      destinataire: 'Client',
      documentId: null,
    });
    req.flush({
      pieceLibelleOriginal: 'Contrat de travail',
      statut: 'A_DEMANDER',
      destinataire: 'Client',
      raisonNonApp: null,
    });
    expect(received?.destinataire).toBe('Client');
  });

  it('CA-03 — PUT inclut raisonNonApp si fourni (statut NON_APPLICABLE)', () => {
    service.updateStatus(
      CASE_FILE_ID,
      'Acte de mariage',
      'NON_APPLICABLE',
      { raisonNonApp: 'Concubinage simple, pas de mariage' },
    ).subscribe();
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({
      pieceLibelleOriginal: 'Acte de mariage',
      statut: 'NON_APPLICABLE',
      raisonNonApp: 'Concubinage simple, pas de mariage',
      destinataire: null,
      documentId: null,
    });
    req.flush({
      pieceLibelleOriginal: 'Acte de mariage',
      statut: 'NON_APPLICABLE',
      raisonNonApp: 'Concubinage simple, pas de mariage',
    });
  });

  it('CA-14 — PUT 400 propage l\'erreur (le composant doit rollback)', () => {
    const errorSpy = jest.fn();
    service.updateStatus(CASE_FILE_ID, 'Pièce X', 'OBTENUE')
      .subscribe({ next: () => {}, error: errorSpy });
    http.expectOne(URL).flush(
      { message: 'statut invalide' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(errorSpy).toHaveBeenCalled();
  });

  it('CA-14 — PUT 500 propage l\'erreur', () => {
    const errorSpy = jest.fn();
    service.updateStatus(CASE_FILE_ID, 'Pièce X', 'OBTENUE')
      .subscribe({ next: () => {}, error: errorSpy });
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    expect(errorSpy).toHaveBeenCalled();
  });

  it('update() expose la version "payload complet" pour usages avancés', () => {
    service.update(CASE_FILE_ID, {
      pieceLibelleOriginal: 'Bulletin de salaire',
      statut: 'A_DEMANDER',
      destinataire: 'Ex-employeur',
    }).subscribe();
    const req = http.expectOne(URL);
    expect(req.request.body).toEqual({
      pieceLibelleOriginal: 'Bulletin de salaire',
      statut: 'A_DEMANDER',
      destinataire: 'Ex-employeur',
    });
    req.flush({});
  });
});
