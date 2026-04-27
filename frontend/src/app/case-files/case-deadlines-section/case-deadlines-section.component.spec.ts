import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { Subject } from 'rxjs';
import { CaseDeadlinesSectionComponent } from './case-deadlines-section.component';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

function makeDeadline(
  dueDate: string,
  label = 'Prescription',
  overrides: Partial<CaseDeadline> = {}
): CaseDeadline {
  return {
    id: 'd-1',
    label,
    dueDate,
    createdAt: '',
    updatedAt: '',
    source: 'MANUAL',
    aiStatus: null,
    ...overrides
  };
}

describe('CaseDeadlinesSectionComponent', () => {
  let fixture: ComponentFixture<CaseDeadlinesSectionComponent>;
  let component: CaseDeadlinesSectionComponent;
  let deadlineServiceSpy: jest.Mocked<CaseDeadlineService>;
  let snackBarSpy: { open: jest.Mock };
  let dialogSpy: { open: jest.Mock };
  let dialogAfterClosedSubject: Subject<boolean>;

  beforeEach(async () => {
    deadlineServiceSpy = jasmine.createSpyObj('CaseDeadlineService',
      ['list', 'create', 'update', 'delete', 'validateDeadline']);
    deadlineServiceSpy.list.mockReturnValue(of([]));

    snackBarSpy = { open: jest.fn() };
    dialogAfterClosedSubject = new Subject<boolean>();
    dialogSpy = {
      open: jest.fn().mockReturnValue({ afterClosed: () => dialogAfterClosedSubject.asObservable() } as Partial<MatDialogRef<unknown>>)
    };

    await TestBed.configureTestingModule({
      imports: [CaseDeadlinesSectionComponent, NoopAnimationsModule],
      providers: [
        { provide: CaseDeadlineService, useValue: deadlineServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDeadlinesSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    fixture.detectChanges();
  });

  it('U-01: shows empty state when no deadlines (expanded)', () => {
    component.toggleCollapsed();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.deadlines-empty')?.textContent)
      .toContain('Aucun délai');
  });

  it('U-02: deadline in the past has class deadline--past', () => {
    expect(component.deadlineClass('2020-01-01')).toBe('deadline--past');
  });

  it('U-03: deadline within 15 days has class deadline--soon', () => {
    const soon = new Date();
    soon.setDate(soon.getDate() + 7);
    const dateStr = soon.toISOString().slice(0, 10);
    expect(component.deadlineClass(dateStr)).toBe('deadline--soon');
  });

  it('U-04: add button disabled when label or date empty', () => {
    component.toggleCollapsed();
    component.newLabel = '';
    component.newDueDate = '';
    fixture.detectChanges();
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.add-deadline-form button');
    expect(btn.disabled).toBe(true);

    component.newLabel = 'Prescription';
    fixture.detectChanges();
    expect(btn.disabled).toBe(true); // date still empty

    component.newDueDate = '2026-06-01';
    fixture.detectChanges();
    expect(btn.disabled).toBe(false);
  });

  it('U-05: addDeadline calls deadlineService.create with trimmed label and dueDate', () => {
    deadlineServiceSpy.create.mockReturnValue(of(makeDeadline('2026-06-01')));
    component.newLabel = '  Prescription  ';
    component.newDueDate = '2026-06-01';
    component.addDeadline();
    expect(deadlineServiceSpy.create).toHaveBeenCalledWith('cf-1', 'Prescription', '2026-06-01');
  });

  // ── Collapsible (SF-71-01) ────────────────────────────────────────────────

  it('SF71-U-01: section repliée par défaut — contenu masqué', () => {
    expect(component.collapsed()).toBe(true);
    const addForm = fixture.nativeElement.querySelector('.add-deadline-form');
    expect(addForm).toBeNull();
  });

  it('SF71-U-02: après toggleCollapsed() — contenu visible, badge masqué', () => {
    deadlineServiceSpy.list.mockReturnValue(of([makeDeadline('2026-06-01')]));
    component.loadDeadlines();
    component.toggleCollapsed();
    fixture.detectChanges();
    expect(component.collapsed()).toBe(false);
    expect(fixture.nativeElement.querySelector('.add-deadline-form')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.td-chip')).toBeNull();
  });

  it('SF71-U-03: double toggle — retour à l\'état replié, badge visible', () => {
    deadlineServiceSpy.list.mockReturnValue(of([makeDeadline('2026-06-01'), makeDeadline('2026-07-01', 'Autre', { id: 'd-2' })]));
    component.loadDeadlines();
    component.toggleCollapsed();
    component.toggleCollapsed();
    fixture.detectChanges();
    expect(component.collapsed()).toBe(true);
    const badge = fixture.nativeElement.querySelector('.td-chip');
    expect(badge).toBeTruthy();
    expect(badge.textContent).toContain('2 délais');
  });

  // ── SF-97-02 — Détection délais IA ───────────────────────────────────────

  it('SF97-U-01: délai AI+PENDING visible dans propositions IA, absent des confirmés', () => {
    const aiDeadline = makeDeadline('2026-08-01', 'Délai IA', { id: 'ai-1', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();

    expect(component.pendingAiDeadlines()).toContain(aiDeadline);
    expect(component.confirmedDeadlines()).not.toContain(aiDeadline);
  });

  it('SF97-U-02: délai MANUAL visible dans confirmés, absent des propositions IA', () => {
    const manualDeadline = makeDeadline('2026-08-01', 'Délai manuel', { id: 'm-1', source: 'MANUAL', aiStatus: null });
    deadlineServiceSpy.list.mockReturnValue(of([manualDeadline]));
    component.loadDeadlines();

    expect(component.confirmedDeadlines()).toContain(manualDeadline);
    expect(component.pendingAiDeadlines()).not.toContain(manualDeadline);
  });

  it('SF97-U-03: délai AI+ACCEPTED visible dans confirmés, absent des propositions IA', () => {
    const acceptedDeadline = makeDeadline('2026-08-01', 'Délai accepté', { id: 'ai-2', source: 'AI', aiStatus: 'ACCEPTED' });
    deadlineServiceSpy.list.mockReturnValue(of([acceptedDeadline]));
    component.loadDeadlines();

    expect(component.confirmedDeadlines()).toContain(acceptedDeadline);
    expect(component.pendingAiDeadlines()).not.toContain(acceptedDeadline);
  });

  it('SF97-U-04: clic Accepter appelle validateDeadline avec ACCEPT', () => {
    const aiDeadline = makeDeadline('2026-08-01', 'Délai IA', { id: 'ai-1', source: 'AI', aiStatus: 'PENDING' });
    const acceptedDeadline = { ...aiDeadline, aiStatus: 'ACCEPTED' as const };
    deadlineServiceSpy.validateDeadline.mockReturnValue(of(acceptedDeadline));
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();

    component.acceptDeadline(aiDeadline);

    expect(deadlineServiceSpy.validateDeadline).toHaveBeenCalledWith('cf-1', 'ai-1', 'ACCEPT');
  });

  it('SF97-U-05: après ACCEPT réussi — délai dans confirmés, absent de pending', () => {
    const aiDeadline = makeDeadline('2026-08-01', 'Délai IA', { id: 'ai-1', source: 'AI', aiStatus: 'PENDING' });
    const acceptedDeadline = { ...aiDeadline, aiStatus: 'ACCEPTED' as const };
    deadlineServiceSpy.validateDeadline.mockReturnValue(of(acceptedDeadline));
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();

    component.acceptDeadline(aiDeadline);

    expect(component.confirmedDeadlines()).toContainEqual(acceptedDeadline);
    expect(component.pendingAiDeadlines()).toHaveLength(0);
  });

  it('SF97-U-06: clic Rejeter appelle validateDeadline avec REJECT', () => {
    const aiDeadline = makeDeadline('2026-08-01', 'Délai IA', { id: 'ai-1', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.validateDeadline.mockReturnValue(of(null));
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();

    component.rejectDeadline(aiDeadline);

    expect(deadlineServiceSpy.validateDeadline).toHaveBeenCalledWith('cf-1', 'ai-1', 'REJECT');
  });

  it('SF97-U-07: après REJECT — délai absent des deux sections', () => {
    const aiDeadline = makeDeadline('2026-08-01', 'Délai IA', { id: 'ai-1', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.validateDeadline.mockReturnValue(of(null));
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();

    component.rejectDeadline(aiDeadline);

    expect(component.pendingAiDeadlines()).toHaveLength(0);
    expect(component.confirmedDeadlines()).toHaveLength(0);
  });

  it('SF97-U-08: erreur PATCH affiche snackbar erreur', () => {
    const aiDeadline = makeDeadline('2026-08-01', 'Délai IA', { id: 'ai-1', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.validateDeadline.mockReturnValue(throwError(() => new Error('Network error')));
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();

    component.acceptDeadline(aiDeadline);

    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Erreur lors de la validation du délai.',
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });

  it('SF97-U-09: sous-section Propositions IA absente si aucun délai pending', () => {
    const manualDeadline = makeDeadline('2026-08-01', 'Délai manuel', { id: 'm-1', source: 'MANUAL', aiStatus: null });
    deadlineServiceSpy.list.mockReturnValue(of([manualDeadline]));
    component.loadDeadlines();
    component.collapsed.set(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.ai-proposals-section')).toBeNull();
  });

  it('SF97-U-10: sous-section Propositions IA visible si un délai AI PENDING existe', () => {
    const aiDeadline = makeDeadline('2026-08-01', 'Délai IA', { id: 'ai-1', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();
    component.collapsed.set(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.ai-proposals-section')).toBeTruthy();
  });

  // ── SF-97 fix délais expirés ──────────────────────────────────────────────

  it('SF97-U-11: isExpired retourne true pour une date passée', () => {
    expect(component.isExpired('2020-01-01')).toBe(true);
  });

  it('SF97-U-12: isExpired retourne false pour une date future', () => {
    expect(component.isExpired('2099-12-31')).toBe(false);
  });

  it('SF97-U-13: badge Dépassé visible pour un délai AI expiré', () => {
    const aiDeadline = makeDeadline('2020-01-01', 'Délai expiré', { id: 'ai-exp', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();
    component.collapsed.set(false);
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.ai-proposal-expired-badge');
    expect(badge).toBeTruthy();
    expect(badge.textContent).toContain('Dépassé');
  });

  it('SF97-U-14: badge Dépassé absent pour un délai AI futur', () => {
    const aiDeadline = makeDeadline('2099-12-31', 'Délai futur', { id: 'ai-fut', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.list.mockReturnValue(of([aiDeadline]));
    component.loadDeadlines();
    component.collapsed.set(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.ai-proposal-expired-badge')).toBeNull();
  });

  it('SF97-U-15: accepter un délai expiré ouvre le dialog de confirmation', () => {
    const expiredDeadline = makeDeadline('2020-01-01', 'Délai expiré', { id: 'ai-exp', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.list.mockReturnValue(of([expiredDeadline]));
    component.loadDeadlines();

    component.acceptDeadline(expiredDeadline);

    expect(dialogSpy.open).toHaveBeenCalled();
    expect(deadlineServiceSpy.validateDeadline).not.toHaveBeenCalled();
  });

  it('SF97-U-16: confirmer le dialog sur délai expiré appelle validateDeadline', () => {
    const expiredDeadline = makeDeadline('2020-01-01', 'Délai expiré', { id: 'ai-exp', source: 'AI', aiStatus: 'PENDING' });
    const acceptedDeadline = { ...expiredDeadline, aiStatus: 'ACCEPTED' as const };
    deadlineServiceSpy.validateDeadline.mockReturnValue(of(acceptedDeadline));
    deadlineServiceSpy.list.mockReturnValue(of([expiredDeadline]));
    component.loadDeadlines();

    component.acceptDeadline(expiredDeadline);
    dialogAfterClosedSubject.next(true);

    expect(deadlineServiceSpy.validateDeadline).toHaveBeenCalledWith('cf-1', 'ai-exp', 'ACCEPT');
  });

  it('SF97-U-17: annuler le dialog sur délai expiré ne lance pas la validation', () => {
    const expiredDeadline = makeDeadline('2020-01-01', 'Délai expiré', { id: 'ai-exp', source: 'AI', aiStatus: 'PENDING' });
    deadlineServiceSpy.list.mockReturnValue(of([expiredDeadline]));
    component.loadDeadlines();

    component.acceptDeadline(expiredDeadline);
    dialogAfterClosedSubject.next(false);

    expect(deadlineServiceSpy.validateDeadline).not.toHaveBeenCalled();
  });

  it('SF97-U-18: accepter un délai futur ne ouvre pas le dialog', () => {
    const futureDeadline = makeDeadline('2099-12-31', 'Délai futur', { id: 'ai-fut', source: 'AI', aiStatus: 'PENDING' });
    const acceptedDeadline = { ...futureDeadline, aiStatus: 'ACCEPTED' as const };
    deadlineServiceSpy.validateDeadline.mockReturnValue(of(acceptedDeadline));
    deadlineServiceSpy.list.mockReturnValue(of([futureDeadline]));
    component.loadDeadlines();

    component.acceptDeadline(futureDeadline);

    expect(dialogSpy.open).not.toHaveBeenCalled();
    expect(deadlineServiceSpy.validateDeadline).toHaveBeenCalledWith('cf-1', 'ai-fut', 'ACCEPT');
  });

  // ── SF-DT-03-01 — Délais de prescription légaux ──────────────────────────

  it('SFDT03-U-01: délai STATUTORY visible dans statutoryDeadlines, absent des autres', () => {
    const statutory = makeDeadline('2026-08-01', 'Prescription — Licenciement (Art. L1471-1)',
      { id: 's-1', source: 'STATUTORY', aiStatus: null });
    deadlineServiceSpy.list.mockReturnValue(of([statutory]));
    component.loadDeadlines();

    expect(component.statutoryDeadlines()).toContain(statutory);
    expect(component.confirmedDeadlines()).not.toContain(statutory);
    expect(component.pendingAiDeadlines()).not.toContain(statutory);
  });

  it('SFDT03-U-02: sous-section Délais légaux applicables visible si STATUTORY présent', () => {
    const statutory = makeDeadline('2026-08-01', 'Prescription — Licenciement (Art. L1471-1)',
      { id: 's-1', source: 'STATUTORY', aiStatus: null });
    deadlineServiceSpy.list.mockReturnValue(of([statutory]));
    component.loadDeadlines();
    component.collapsed.set(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.statutory-section')).toBeTruthy();
  });

  it('SFDT03-U-03: sous-section Délais légaux applicables absente si aucun STATUTORY', () => {
    const manual = makeDeadline('2026-08-01', 'Délai manuel', { id: 'm-1' });
    deadlineServiceSpy.list.mockReturnValue(of([manual]));
    component.loadDeadlines();
    component.collapsed.set(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.statutory-section')).toBeNull();
  });

  it('SFDT03-U-04: état vide affiché seulement si aucun délai (MANUAL+AI+STATUTORY)', () => {
    deadlineServiceSpy.list.mockReturnValue(of([]));
    component.loadDeadlines();
    component.collapsed.set(false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.deadlines-empty')).toBeTruthy();

    const statutory = makeDeadline('2026-08-01', 'Prescription',
      { id: 's-1', source: 'STATUTORY', aiStatus: null });
    deadlineServiceSpy.list.mockReturnValue(of([statutory]));
    component.loadDeadlines();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.deadlines-empty')).toBeNull();
  });
});
