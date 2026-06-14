import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { ContradictoireTimelineComponent } from './contradictoire-timeline.component';
import {
  ContradictoireRound,
  ContradictoireTimeline,
} from '../../core/models/contradictoire.model';
import { Document } from '../../core/models/document.model';
import { ConclusionVersionSummary } from '../../core/models/conclusion.model';

describe('ContradictoireTimelineComponent', () => {
  let fixture: ComponentFixture<ContradictoireTimelineComponent>;
  let component: ContradictoireTimelineComponent;
  let httpMock: HttpTestingController;
  let routerSpy: jest.SpyInstance;

  const CASE_ID = 'case-1';
  const URL = `/api/v1/case-files/${CASE_ID}/contradictoire-rounds`;
  const DOCS_URL = `/api/v1/case-files/${CASE_ID}/documents`;
  const VERSIONS_URL = `/api/v1/case-files/${CASE_ID}/conclusions/versions`;

  function docs(): Document[] {
    return [
      { id: 'doc-1', caseFileId: CASE_ID, originalFilename: 'conclusions-adverses.pdf', contentType: 'application/pdf', fileSize: 1000, createdAt: '' },
      { id: 'doc-2', caseFileId: CASE_ID, originalFilename: 'contrat-travail.pdf', contentType: 'application/pdf', fileSize: 2000, createdAt: '' },
    ];
  }

  function versions(): ConclusionVersionSummary[] {
    return [
      { id: 'v-1', versionNumber: 1, lifecycleStatus: 'DRAFT', status: 'DONE', generatedAt: '', createdAt: '' },
      { id: 'v-2', versionNumber: 2, lifecycleStatus: 'VALIDATED', status: 'DONE', generatedAt: '', createdAt: '' },
    ] as ConclusionVersionSummary[];
  }

  function round(overrides: Partial<ContradictoireRound> = {}): ContradictoireRound {
    return {
      id: 'r1', roundNumber: 1, party: 'OURS', label: 'Saisine',
      datedAt: '2026-06-01', responseDueAt: null,
      sourceDocumentId: null, sourceConclusionId: null, sourceLabel: null,
      createdAt: '', updatedAt: '',
      ...overrides,
    };
  }

  function timeline(overrides: Partial<ContradictoireTimeline> = {}): ContradictoireTimeline {
    return {
      rounds: [
        round({ id: 'r1', roundNumber: 1, party: 'OURS', label: 'Saisine', datedAt: '2026-06-01' }),
        round({ id: 'r2', roundNumber: 2, party: 'ADVERSE', label: 'Conclusions adverses', datedAt: '2026-06-14', responseDueAt: '2026-07-14' }),
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
    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  /** Flushe les 3 requêtes émises au chargement (timeline + documents + versions). */
  function flushInit(
    t: ContradictoireTimeline = timeline(),
    d: Document[] | null = docs(),
    v: ConclusionVersionSummary[] | null = versions(),
  ): void {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(t);
    const docReq = httpMock.expectOne(DOCS_URL);
    if (d === null) {
      docReq.flush('boom', { status: 500, statusText: 'Server Error' });
    } else {
      docReq.flush(d);
    }
    const verReq = httpMock.expectOne(VERSIONS_URL);
    if (v === null) {
      verReq.flush('boom', { status: 500, statusText: 'Server Error' });
    } else {
      verReq.flush(v);
    }
    fixture.detectChanges();
  }

  /** Flushe les requêtes de rafraîchissement (documents + versions) émises à l'ouverture du dialogue. */
  function flushOpenRefresh(d: Document[] = docs(), v: ConclusionVersionSummary[] = versions()): void {
    httpMock.expectOne(DOCS_URL).flush(d);
    httpMock.expectOne(VERSIONS_URL).flush(v);
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
    flushOpenRefresh();
    component.form.patchValue({ party: 'ADVERSE', datedAt: '2026-06-14', responseDueAt: '2026-07-14' });
    component.save();

    const post = httpMock.expectOne((r) => r.method === 'POST' && r.url === URL);
    expect(post.request.body.party).toBe('ADVERSE');
    post.flush(round({ id: 'r3', roundNumber: 1, party: 'ADVERSE', label: null, datedAt: '2026-06-14', responseDueAt: '2026-07-14' }));

    // rechargement
    httpMock.expectOne(URL).flush(timeline());
    expect(component.showForm()).toBe(false);
  });

  // ── SF-282-03 / SF-282-04 : sélecteur de source conditionnel à la partie ──

  it('SF-282-03 : peuple le sélecteur de pièce source via DocumentService.list', () => {
    flushInit();
    expect(component.documents().length).toBe(2);
    expect(component.documentsUnavailable()).toBe(false);

    component.openAdd('ADVERSE');
    flushOpenRefresh();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-source-field"]')).toBeTruthy();
  });

  it('SF-282-04 : party=OURS affiche le sélecteur conclusions, ADVERSE le sélecteur document', () => {
    flushInit();
    expect(component.versions().length).toBe(2);

    component.openAdd('OURS');
    flushOpenRefresh();
    fixture.detectChanges();
    let el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="contra-conclusion-field"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="contra-source-field"]')).toBeNull();

    // bascule vers ADVERSE → sélecteur document
    component.form.controls.party.setValue('ADVERSE');
    fixture.detectChanges();
    el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="contra-source-field"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="contra-conclusion-field"]')).toBeNull();
  });

  it('SF-282-04 : changer de partie réinitialise l’id de l’autre type', () => {
    flushInit();

    component.openAdd('OURS');
    flushOpenRefresh();
    component.form.controls.sourceConclusionId.setValue('v-1');
    component.form.controls.party.setValue('ADVERSE');
    expect(component.form.controls.sourceConclusionId.value).toBeNull();

    component.form.controls.sourceDocumentId.setValue('doc-1');
    component.form.controls.party.setValue('OURS');
    expect(component.form.controls.sourceDocumentId.value).toBeNull();
  });

  it('SF-282-04 : la création d’un round OURS transmet le sourceConclusionId choisi', () => {
    flushInit(timeline({ rounds: [], summary: { currentRoundNumber: 0, awaitingParty: 'OURS', nextDeadline: null } }));

    component.openAdd('OURS');
    flushOpenRefresh();
    component.form.patchValue({ party: 'OURS', datedAt: '2026-06-14', sourceConclusionId: 'v-2' });
    component.save();

    const post = httpMock.expectOne((r) => r.method === 'POST' && r.url === URL);
    expect(post.request.body.sourceConclusionId).toBe('v-2');
    expect(post.request.body.sourceDocumentId).toBeNull();
    post.flush(round({ id: 'r3', roundNumber: 1, party: 'OURS', label: null, datedAt: '2026-06-14', sourceConclusionId: 'v-2', sourceLabel: 'Conclusions v2' }));
    httpMock.expectOne(URL).flush(timeline());
  });

  it('SF-282-03 : la création transmet le sourceDocumentId choisi', () => {
    flushInit(timeline({ rounds: [], summary: { currentRoundNumber: 0, awaitingParty: 'OURS', nextDeadline: null } }));

    component.openAdd('ADVERSE');
    flushOpenRefresh();
    component.form.patchValue({ party: 'ADVERSE', datedAt: '2026-06-14', sourceDocumentId: 'doc-1' });
    component.save();

    const post = httpMock.expectOne((r) => r.method === 'POST' && r.url === URL);
    expect(post.request.body.sourceDocumentId).toBe('doc-1');
    post.flush(round({ id: 'r3', roundNumber: 1, party: 'ADVERSE', label: null, datedAt: '2026-06-14', sourceDocumentId: 'doc-1', sourceLabel: 'conclusions-adverses.pdf' }));
    httpMock.expectOne(URL).flush(timeline());
  });

  it('SF-282-04 : l’édition d’un round OURS lié pré-renseigne le sélecteur conclusions', () => {
    flushInit();

    component.openEdit(round({ id: 'r1', roundNumber: 1, party: 'OURS', label: 'Saisine', datedAt: '2026-06-01', sourceConclusionId: 'v-1', sourceLabel: 'Conclusions v1' }));
    flushOpenRefresh();
    expect(component.form.controls.sourceConclusionId.value).toBe('v-1');
  });

  it('SF-282-04 : un round OURS avec sourceConclusionId affiche un chip → openConclusion (router.navigate ?version)', () => {
    flushInit(timeline({
      rounds: [round({ id: 'r1', roundNumber: 1, party: 'OURS', sourceConclusionId: 'v-2', sourceLabel: 'Conclusions v2' })],
      summary: { currentRoundNumber: 1, awaitingParty: 'ADVERSE', nextDeadline: null },
    }));

    const chip = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-source-1"]') as HTMLButtonElement;
    expect(chip).toBeTruthy();
    expect(chip.textContent).toContain('Conclusions v2');
    chip.click();
    expect(routerSpy).toHaveBeenCalledWith(
      ['/case-files', CASE_ID, 'conclusions'],
      { queryParams: { version: 'v-2' } },
    );
  });

  it('SF-282-04 : un round ADVERSE avec sourceDocumentId → chip ouvre le drawer (openPreview)', () => {
    flushInit(timeline({
      rounds: [round({ id: 'r1', roundNumber: 1, party: 'ADVERSE', sourceDocumentId: 'doc-1', sourceLabel: 'conclusions-adverses.pdf' })],
      summary: { currentRoundNumber: 1, awaitingParty: 'OURS', nextDeadline: null },
    }));

    const chip = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-source-1"]') as HTMLButtonElement;
    expect(chip).toBeTruthy();
    chip.click();

    // openPreview ouvre le drawer + appelle DocumentService.preview
    expect(component.previewRound()?.id).toBe('r1');
    httpMock.expectOne(`${DOCS_URL}/doc-1/preview`).flush({});
    expect(component.previewLoading()).toBe(false);
    expect(component.previewUrl()).not.toBeNull();
  });

  it('SF-282-04 : closePreview referme le drawer et remet les signaux à null', () => {
    flushInit(timeline({
      rounds: [round({ id: 'r1', roundNumber: 1, party: 'ADVERSE', sourceDocumentId: 'doc-1', sourceLabel: 'piece.pdf' })],
      summary: { currentRoundNumber: 1, awaitingParty: 'OURS', nextDeadline: null },
    }));

    component.openPreview(component.rounds()[0]);
    httpMock.expectOne(`${DOCS_URL}/doc-1/preview`).flush({});
    expect(component.previewRound()).not.toBeNull();

    component.closePreview();
    expect(component.previewRound()).toBeNull();
    expect(component.previewUrl()).toBeNull();
    expect(component.previewError()).toBe(false);
    expect(component.previewLoading()).toBe(false);
  });

  it('SF-282-04 : preview indisponible → previewError', () => {
    flushInit(timeline({
      rounds: [round({ id: 'r1', roundNumber: 1, party: 'ADVERSE', sourceDocumentId: 'doc-x', sourceLabel: 'supprimee.pdf' })],
      summary: { currentRoundNumber: 1, awaitingParty: 'OURS', nextDeadline: null },
    }));

    component.openPreview(component.rounds()[0]);
    httpMock.expectOne(`${DOCS_URL}/doc-x/preview`).flush('gone', { status: 404, statusText: 'Not Found' });
    expect(component.previewError()).toBe(true);
    expect(component.previewUrl()).toBeNull();
  });

  it('SF-282-04 : round sans source → aucun chip', () => {
    flushInit(timeline({
      rounds: [round({ id: 'r1', roundNumber: 1, party: 'OURS', sourceLabel: null })],
      summary: { currentRoundNumber: 1, awaitingParty: 'ADVERSE', nextDeadline: null },
    }));

    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-source-1"]')).toBeNull();
  });

  it('SF-282-04 : onGenerateReply navigue avec roundId du dernier round OURS sans source', () => {
    flushInit(timeline({
      rounds: [
        round({ id: 'r1', roundNumber: 1, party: 'OURS', sourceConclusionId: 'v-1', sourceLabel: 'Conclusions v1' }),
        round({ id: 'r2', roundNumber: 2, party: 'ADVERSE', sourceDocumentId: 'doc-1', sourceLabel: 'adverse.pdf' }),
        round({ id: 'r3', roundNumber: 3, party: 'OURS', sourceLabel: null }),
      ],
      summary: { currentRoundNumber: 3, awaitingParty: 'OURS', nextDeadline: null },
    }));

    const spy = jest.fn();
    component.generateReplyRequested.subscribe(spy);
    component.onGenerateReply();

    expect(routerSpy).toHaveBeenCalledWith(
      ['/case-files', CASE_ID, 'conclusions'],
      { queryParams: { roundId: 'r3' } },
    );
    expect(spy).toHaveBeenCalled();
  });

  it('SF-282-04 : onGenerateReply navigue sans roundId si aucun round OURS sans source', () => {
    flushInit(timeline({
      rounds: [round({ id: 'r1', roundNumber: 1, party: 'OURS', sourceConclusionId: 'v-1', sourceLabel: 'Conclusions v1' })],
      summary: { currentRoundNumber: 1, awaitingParty: 'OURS', nextDeadline: null },
    }));

    component.onGenerateReply();
    expect(routerSpy).toHaveBeenCalledWith(
      ['/case-files', CASE_ID, 'conclusions'],
      { queryParams: {} },
    );
  });

  it('SF-282-04 : dégradation gracieuse si listVersions échoue', () => {
    flushInit(timeline(), docs(), null);
    expect(component.versionsUnavailable()).toBe(true);
    expect(component.versions().length).toBe(0);

    component.openAdd('OURS');
    httpMock.expectOne(DOCS_URL).flush(docs());
    httpMock.expectOne(VERSIONS_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-form"]')).toBeTruthy();
  });

  it('SF-282-03 : dégradation gracieuse si DocumentService.list échoue', () => {
    flushInit(timeline(), null);
    expect(component.documentsUnavailable()).toBe(true);
    expect(component.documents().length).toBe(0);

    component.openAdd('ADVERSE');
    httpMock.expectOne(DOCS_URL).flush('boom', { status: 500, statusText: 'Server Error' });
    httpMock.expectOne(VERSIONS_URL).flush(versions());
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="contra-form"]')).toBeTruthy();
  });

  it('SF-282-03 (fix) : rouvrir le dialogue rafraîchit la liste des documents', () => {
    flushInit(); // init : 2 docs
    expect(component.documents().length).toBe(2);

    component.openAdd('ADVERSE'); // doit re-fetch la liste (pièces ajoutées depuis le montage)
    httpMock.expectOne(DOCS_URL).flush([
      ...docs(),
      { id: 'doc-3', caseFileId: CASE_ID, originalFilename: 'piece-ajoutee.pdf', contentType: 'application/pdf', fileSize: 500, createdAt: '' },
    ]);
    httpMock.expectOne(VERSIONS_URL).flush(versions());
    expect(component.documents().length).toBe(3);
  });
});
