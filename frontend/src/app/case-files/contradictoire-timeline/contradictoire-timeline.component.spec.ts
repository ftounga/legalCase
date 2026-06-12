import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ContradictoireTimelineComponent } from './contradictoire-timeline.component';
import { ContradictoireTimeline } from '../../core/models/contradictoire.model';

describe('ContradictoireTimelineComponent', () => {
  let fixture: ComponentFixture<ContradictoireTimelineComponent>;
  let component: ContradictoireTimelineComponent;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const URL = `/api/v1/case-files/${CASE_ID}/contradictoire-rounds`;

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

  it('charge et rend les rounds + le résumé', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(timeline());
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="contra-round-1"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="contra-round-2"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="contra-summary"]')!.textContent).toContain('À vous');
  });

  it('au tour « à vous » : bouton « Générer ma réplique » présent et émet l’évènement', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(timeline());
    fixture.detectChanges();

    const spy = jest.fn();
    component.generateReplyRequested.subscribe(spy);

    const btn = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-testid="generate-reply-btn"]') as HTMLButtonElement;
    expect(btn).toBeTruthy();
    btn.click();
    expect(spy).toHaveBeenCalled();
  });

  it('état initial vide : montre « Round 1 — votre saisine »', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({
      rounds: [],
      summary: { currentRoundNumber: 0, awaitingParty: 'OURS', nextDeadline: null },
    } as ContradictoireTimeline);
    fixture.detectChanges();

    const txt = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(txt).toContain('Round 1');
    expect(txt).toContain('saisine');
  });

  it('ajoute un échange (POST) puis recharge', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(timeline({ rounds: [], summary: { currentRoundNumber: 0, awaitingParty: 'OURS', nextDeadline: null } }));

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
});
