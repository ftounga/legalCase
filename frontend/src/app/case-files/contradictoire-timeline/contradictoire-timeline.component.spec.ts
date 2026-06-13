import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ContradictoireTimelineComponent } from './contradictoire-timeline.component';
import { ContradictoireTimeline } from '../../core/models/contradictoire.model';
import { Document } from '../../core/models/document.model';

describe('ContradictoireTimelineComponent', () => {
  let fixture: ComponentFixture<ContradictoireTimelineComponent>;
  let component: ContradictoireTimelineComponent;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const URL = `/api/v1/case-files/${CASE_ID}/contradictoire-rounds`;
  const DOCS_URL = `/api/v1/case-files/${CASE_ID}/documents`;

  function docs(): Document[] {
    return [
      { id: 'doc-1', caseFileId: CASE_ID, originalFilename: 'conclusions-adverses.pdf', contentType: 'application/pdf', fileSize: 1000, createdAt: '' },
      { id: 'doc-2', caseFileId: CASE_ID, originalFilename: 'contrat-travail.pdf', contentType: 'application/pdf', fileSize: 2000, createdAt: '' },
    ];
  }

  function timeline(overrides: Partial<ContradictoireTimeline> = {}): ContradictoireTimeline {
    return {
      rounds: [
        {
          id: 'r1', roundNumber: 1, party: 'OURS', label: 'Saisine',
          datedAt: '2026-06-01', responseDueAt: null,
          sourceDocumentId: null, sourceConclusionId: null,
          createdAt: '', updatedAt: '',
        },
        {
          id: 'r2', roundNumber: 2, party: 'ADVERSE', label: 'Conclusions adverses',
          datedAt: '2026-06-14', responseDueAt: '2026-07-14',
          sourceDocumentId: null, sourceConclusionId: null,
          createdAt: '', updatedAt: '',
        },
      ],
      summary: { currentRoundNumber: 2, awaitingParty: 'OURS', nextDeadline: '2026-07-14' },
      ...overrides,
    };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContradictoireTimelineComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ContradictoireTimelineComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_ID;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  /** Flushe les 2 requêtes émises au chargement (timeline + documents). */
  function flushInit(t: ContradictoireTimeline = timeline(), d: Document[] | null = docs()): void {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(t);
    const docReq = httpMock.expectOne(DOCS_URL);
    if (d === null) {
      docReq.flush('boom', { status: 500, statusText: 'Server Error' });
    } else {
      docReq.flush(d);
    }
    fixture.detectChanges();
  }

  it('charge et rend les rounds + le résumé', () => {
    flushInit();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="contra-round-1"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="contra-round-2"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="contra-summary"]')!.textContent).toContain('À vous');
  });

  it('au tour « à vous » : bouton « Générer ma réplique » présent et émet l’évènement', () => {
    flushInit();

    const spy = jest.fn();
    component.generateReplyRequested.subscribe(spy);

    const btn = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-testid="generate-reply-btn"]') as HTMLButtonElement;
    expect(btn).toBeTruthy();
    btn.click();
    expect(spy).toHaveBeenCalled();
  });

  it('état initial vide : montre « Round 1 — votre saisine »', () => {
    flushInit({
      rounds: [],
      summary: { currentRoundNumber: 0, awaitingParty: 'OURS', nextDeadline: null },
    } as ContradictoireTimeline);

    const txt = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(txt).toContain('Round 1');
    expect(txt).toContain('saisine');
  });

  it('ajoute un échange (POST) puis recharge', () => {
    flushInit(timeline({ rounds: [], summary: { currentRoundNumber: 0, awaitingParty: 'OURS', nextDeadline: null } }));

    component.openAdd('ADVERSE');
    component.form.patchValue({ party: 'ADVERSE', datedAt: '2026-06-14', responseDueAt: '2026-07-14' });
    component.save();

    const post = httpMock.expectOne((r) => r.method === 'POST' && r.url === URL);
    expect(post.request.body.party).toBe('ADVERSE');
    post.flush({ id: 'r3', roundNumber: 1, party: 'ADVERSE', label: null, datedAt: '2026-06-14', responseDueAt: '2026-07-14', sourceDocumentId: null, sourceConclusionId: null, createdAt: '', updatedAt: '' });

    // rechargement
    httpMock.expectOne(URL).flush(timeline());
    expect(component.showForm()).toBe(false);
  });

  // ── SF-282-03 : sélecteur de pièce source ──────────────────────────

  it('SF-282-03 : peuple le sélecteur de pièce source via DocumentService.list', () => {
    flushInit();
    expect(component.documents().length).toBe(2);
    expect(component.documentsUnavailable()).toBe(false);

    component.openAdd('ADVERSE');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-source-field"]')).toBeTruthy();
  });

  it('SF-282-03 : la création transmet le sourceDocumentId choisi', () => {
    flushInit(timeline({ rounds: [], summary: { currentRoundNumber: 0, awaitingParty: 'OURS', nextDeadline: null } }));

    component.openAdd('ADVERSE');
    component.form.patchValue({ party: 'ADVERSE', datedAt: '2026-06-14', sourceDocumentId: 'doc-1' });
    component.save();

    const post = httpMock.expectOne((r) => r.method === 'POST' && r.url === URL);
    expect(post.request.body.sourceDocumentId).toBe('doc-1');
    post.flush({ id: 'r3', roundNumber: 1, party: 'ADVERSE', label: null, datedAt: '2026-06-14', responseDueAt: null, sourceDocumentId: 'doc-1', sourceConclusionId: null, createdAt: '', updatedAt: '' });
    httpMock.expectOne(URL).flush(timeline());
  });

  it('SF-282-03 : l’édition d’un round lié pré-renseigne le sélecteur et transmet sourceDocumentId', () => {
    flushInit();

    component.openEdit({
      id: 'r2', roundNumber: 2, party: 'ADVERSE', label: 'Conclusions adverses',
      datedAt: '2026-06-14', responseDueAt: '2026-07-14',
      sourceDocumentId: 'doc-2', sourceConclusionId: null, createdAt: '', updatedAt: '',
    });
    expect(component.form.controls.sourceDocumentId.value).toBe('doc-2');

    component.save();
    const put = httpMock.expectOne((r) => r.method === 'PUT' && r.url === `${URL}/r2`);
    expect(put.request.body.sourceDocumentId).toBe('doc-2');
    put.flush({ id: 'r2', roundNumber: 2, party: 'ADVERSE', label: 'Conclusions adverses', datedAt: '2026-06-14', responseDueAt: '2026-07-14', sourceDocumentId: 'doc-2', sourceConclusionId: null, createdAt: '', updatedAt: '' });
    httpMock.expectOne(URL).flush(timeline());
  });

  it('SF-282-03 : un round avec sourceDocumentId affiche le nom de la pièce sur la frise', () => {
    flushInit(timeline({
      rounds: [
        { id: 'r1', roundNumber: 1, party: 'ADVERSE', label: 'Conclusions adverses', datedAt: '2026-06-14', responseDueAt: null, sourceDocumentId: 'doc-1', sourceConclusionId: null, createdAt: '', updatedAt: '' },
      ],
      summary: { currentRoundNumber: 1, awaitingParty: 'OURS', nextDeadline: null },
    }));

    const link = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-source-1"]') as HTMLAnchorElement;
    expect(link).toBeTruthy();
    expect(link.textContent).toContain('conclusions-adverses.pdf');
    expect(link.getAttribute('href')).toContain('/documents/doc-1/download');
  });

  it('SF-282-03 : pièce supprimée (id introuvable) → pas de lien cassé', () => {
    flushInit(timeline({
      rounds: [
        { id: 'r1', roundNumber: 1, party: 'ADVERSE', label: 'Conclusions adverses', datedAt: '2026-06-14', responseDueAt: null, sourceDocumentId: 'doc-deleted', sourceConclusionId: null, createdAt: '', updatedAt: '' },
      ],
      summary: { currentRoundNumber: 1, awaitingParty: 'OURS', nextDeadline: null },
    }));

    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-source-1"]')).toBeNull();
    expect(component.sourceDocumentName('doc-deleted')).toBeNull();
  });

  it('SF-282-03 : dégradation gracieuse si DocumentService.list échoue', () => {
    flushInit(timeline(), null);
    expect(component.documentsUnavailable()).toBe(true);
    expect(component.documents().length).toBe(0);

    // Le formulaire reste utilisable
    component.openAdd('ADVERSE');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-form"]')).toBeTruthy();
  });
});
