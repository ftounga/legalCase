import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JurisprudenceCitationService } from './jurisprudence-citation.service';
import { JurisprudenceCitation } from '../models/jurisprudence-citation.model';

const CASE_FILE_ID = 'cf-1';
const BASE = `/api/v1/case-files/${CASE_FILE_ID}/jurisprudence-citations`;

const sampleCitation: JurisprudenceCitation = {
  id: 'cit-1',
  pointJuridiqueIndex: 0,
  pointJuridiqueTexte: 'Licenciement pour faute grave',
  reference: 'Cass. soc. 12 oct. 2022, n° 21-12345',
  portee: 'La faute grave doit être établie par l\'employeur',
  createdAt: '2026-05-18T09:00:00Z',
  updatedAt: '2026-05-18T09:00:00Z',
};

describe('JurisprudenceCitationService (F-242 SF-242-02)', () => {
  let service: JurisprudenceCitationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(JurisprudenceCitationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // S-1 : GET liste les citations du dossier.
  it('list — GET sur la bonne URL', () => {
    let received: JurisprudenceCitation[] | undefined;
    service.list(CASE_FILE_ID).subscribe(r => (received = r.citations));

    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('GET');
    req.flush({ citations: [sampleCitation] });

    expect(received).toEqual([sampleCitation]);
  });

  // S-2 : POST avec le payload de création complet.
  it('create — POST avec pointJuridiqueIndex / pointJuridiqueTexte / reference / portee', () => {
    service
      .create(CASE_FILE_ID, {
        pointJuridiqueIndex: 2,
        pointJuridiqueTexte: 'Préavis non respecté',
        reference: 'Cass. soc. 3 mai 2018, n° 16-26796',
        portee: 'Le préavis est dû',
      })
      .subscribe();

    const req = http.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      pointJuridiqueIndex: 2,
      pointJuridiqueTexte: 'Préavis non respecté',
      reference: 'Cass. soc. 3 mai 2018, n° 16-26796',
      portee: 'Le préavis est dû',
    });
    req.flush(sampleCitation);
  });

  // S-3 : PUT cible la citation et envoie reference / portee.
  it('update — PUT sur .../{citationId} avec reference / portee', () => {
    service
      .update(CASE_FILE_ID, 'cit-9', { reference: 'Réf. modifiée', portee: null })
      .subscribe();

    const req = http.expectOne(`${BASE}/cit-9`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ reference: 'Réf. modifiée', portee: null });
    req.flush(sampleCitation);
  });

  // S-4 : DELETE cible la citation.
  it('delete — DELETE sur .../{citationId}', () => {
    let completed = false;
    service.delete(CASE_FILE_ID, 'cit-7').subscribe(() => (completed = true));

    const req = http.expectOne(`${BASE}/cit-7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(completed).toBe(true);
  });
});
