import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { JurisprudenceCitationsSectionComponent } from './jurisprudence-citations-section.component';
import { JurisprudenceCheck } from '../../core/models/jurisprudence-check.model';
import { JurisprudenceCheckService } from '../../core/services/jurisprudence-check.service';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('JurisprudenceCitationsSectionComponent', () => {
  let fixture: ComponentFixture<JurisprudenceCitationsSectionComponent>;
  let component: JurisprudenceCitationsSectionComponent;
  let serviceSpy: jasmine.SpyObj<JurisprudenceCheckService>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  function check(over: Partial<JurisprudenceCheck>): JurisprudenceCheck {
    return {
      id: over.id ?? 'id-' + Math.random(),
      documentName: over.documentName ?? 'conclusions.pdf',
      reference: over.reference ?? 'Cass. soc. n° 12-17.516',
      statut: over.statut ?? 'VERIFIED',
      explication: over.explication ?? null,
      positionAlleguee: over.positionAlleguee ?? null,
      sourceUrl: over.sourceUrl ?? null,
      claudeConfidence: over.claudeConfidence ?? null,
      webSearchUsed: over.webSearchUsed ?? false,
      markedAdverse: over.markedAdverse ?? false,
    };
  }

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj('JurisprudenceCheckService', [
      'getChecks',
      'markAdverse',
    ]);
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [JurisprudenceCitationsSectionComponent, NoopAnimationsModule],
      providers: [
        { provide: JurisprudenceCheckService, useValue: serviceSpy },
        { provide: MatSnackBar, useValue: snackSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(JurisprudenceCitationsSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
  });

  it('renders nothing when there is no check', () => {
    component.checks = [];
    fixture.detectChanges();
    const panel = fixture.nativeElement.querySelector('#section-jurisprudence-citations');
    expect(panel).toBeNull();
  });

  it('groups checks by document name', () => {
    component.checks = [
      check({ documentName: 'conclusions_adverses.pdf', reference: 'A' }),
      check({ documentName: 'conclusions_adverses.pdf', reference: 'B' }),
      check({ documentName: 'pieces.pdf', reference: 'C' }),
    ];
    fixture.detectChanges();
    expect(component.groups().length).toBe(2);
    const adverseGroup = component.groups().find(g => g.documentName === 'conclusions_adverses.pdf');
    expect(adverseGroup?.checks.length).toBe(2);
  });

  it('exposes the right badge metadata per statut', () => {
    expect(component.statutClass('SUSPECT')).toBe('jc-badge--suspect');
    expect(component.statutClass('UNCERTAIN')).toBe('jc-badge--uncertain');
    expect(component.statutClass('VERIFIED')).toBe('jc-badge--verified');
    expect(component.statutClass('NOT_FOUND')).toBe('jc-badge--not-found');
    expect(component.statutLabel('SUSPECT')).toBe('Suspecte');
    expect(component.statutIcon('VERIFIED')).toBe('check_circle');
  });

  it('renders a source link only when sourceUrl is present', () => {
    component.checks = [
      check({ id: '1', reference: 'Avec lien', sourceUrl: 'https://legifrance.gouv.fr/x' }),
      check({ id: '2', reference: 'Sans lien', sourceUrl: null }),
    ];
    fixture.detectChanges();
    const links = fixture.nativeElement.querySelectorAll('.jc-source-link');
    expect(links.length).toBe(1);
    expect(links[0].getAttribute('href')).toBe('https://legifrance.gouv.fr/x');
    expect(links[0].getAttribute('target')).toBe('_blank');
  });

  it('shows the manual-verification banner when all checks are UNCERTAIN', () => {
    component.checks = [
      check({ id: '1', statut: 'UNCERTAIN' }),
      check({ id: '2', statut: 'UNCERTAIN' }),
    ];
    fixture.detectChanges();
    expect(component.allUncertain()).toBe(true);
    expect(fixture.nativeElement.querySelector('.jc-uncertain-banner')).not.toBeNull();
  });

  it('does not show the banner when at least one check is conclusive', () => {
    component.checks = [
      check({ id: '1', statut: 'UNCERTAIN' }),
      check({ id: '2', statut: 'VERIFIED' }),
    ];
    fixture.detectChanges();
    expect(component.allUncertain()).toBe(false);
    expect(fixture.nativeElement.querySelector('.jc-uncertain-banner')).toBeNull();
  });

  it('counts suspect references and reflects them in the summary', () => {
    component.checks = [
      check({ id: '1', statut: 'SUSPECT' }),
      check({ id: '2', statut: 'VERIFIED' }),
      check({ id: '3', statut: 'SUSPECT' }),
    ];
    fixture.detectChanges();
    expect(component.suspectCount()).toBe(2);
    expect(component.totalCount()).toBe(3);
    expect(component.summary()).toContain('2 suspecte');
  });

  it('flags only SUSPECT checks as alerts', () => {
    expect(component.isAlert(check({ statut: 'SUSPECT' }))).toBe(true);
    expect(component.isAlert(check({ statut: 'VERIFIED' }))).toBe(false);
    expect(component.isAlert(check({ statut: 'NOT_FOUND' }))).toBe(false);
    expect(component.isAlert(check({ statut: 'UNCERTAIN' }))).toBe(false);
  });

  // ---- SF-179-04 : alerte de cohérence sur arrêt SUSPECT ----

  it('renders a coherence alert badge only on SUSPECT checks', () => {
    component.checks = [
      check({ id: '1', statut: 'SUSPECT' }),
      check({ id: '2', statut: 'VERIFIED' }),
      check({ id: '3', statut: 'UNCERTAIN' }),
    ];
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.jc-alert');
    expect(alerts.length).toBe(1);
  });

  it('builds an alert tooltip confronting position and reality', () => {
    const tooltip = component.alertTooltip(
      check({
        statut: 'SUSPECT',
        positionAlleguee: 'Fonde la nullité du licenciement.',
        explication: 'L\'arrêt concerne en réalité la prescription.',
      }),
    );
    expect(tooltip).toContain('incohérente');
    expect(tooltip).toContain('Alléguée : Fonde la nullité du licenciement.');
    expect(tooltip).toContain('Réalité : L\'arrêt concerne en réalité la prescription.');
  });

  it('falls back to a generic message when explication is missing', () => {
    const tooltip = component.alertTooltip(
      check({ statut: 'SUSPECT', positionAlleguee: 'X', explication: null }),
    );
    expect(tooltip).toContain('à vérifier');
  });

  it('marks SUSPECT items with the alert CSS class', () => {
    component.checks = [check({ id: '1', statut: 'SUSPECT' })];
    fixture.detectChanges();
    const item = fixture.nativeElement.querySelector('.jc-item');
    expect(item.classList).toContain('jc-item--alert');
  });

  // ---- F-98 SF-98-56 : marquage « adverse à réfuter » ----

  it('renders the adverse-marking toggle only on SUSPECT / NOT_FOUND checks', () => {
    component.checks = [
      check({ id: '1', statut: 'SUSPECT' }),
      check({ id: '2', statut: 'NOT_FOUND' }),
      check({ id: '3', statut: 'VERIFIED' }),
      check({ id: '4', statut: 'UNCERTAIN' }),
    ];
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="mark-adverse-1"]'),
    ).not.toBeNull();
    expect(
      fixture.nativeElement.querySelector('[data-testid="mark-adverse-2"]'),
    ).not.toBeNull();
    expect(
      fixture.nativeElement.querySelector('[data-testid="mark-adverse-3"]'),
    ).toBeNull();
    expect(
      fixture.nativeElement.querySelector('[data-testid="mark-adverse-4"]'),
    ).toBeNull();
  });

  it('canMarkAdverse is true only for SUSPECT / NOT_FOUND', () => {
    expect(component.canMarkAdverse(check({ statut: 'SUSPECT' }))).toBe(true);
    expect(component.canMarkAdverse(check({ statut: 'NOT_FOUND' }))).toBe(true);
    expect(component.canMarkAdverse(check({ statut: 'VERIFIED' }))).toBe(false);
    expect(component.canMarkAdverse(check({ statut: 'UNCERTAIN' }))).toBe(false);
  });

  it('does not render the toggle when caseFileId is missing', () => {
    component.caseFileId = null;
    component.checks = [check({ id: '1', statut: 'SUSPECT' })];
    fixture.detectChanges();
    expect(
      fixture.nativeElement.querySelector('[data-testid="mark-adverse-1"]'),
    ).toBeNull();
  });

  it('clicking the toggle calls the service with the right args and updates state', () => {
    const c = check({ id: '1', statut: 'SUSPECT', markedAdverse: false });
    serviceSpy.markAdverse.and.returnValue(of({ ...c, markedAdverse: true }));
    component.checks = [c];
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="mark-adverse-1"]',
    );
    btn.click();

    expect(serviceSpy.markAdverse).toHaveBeenCalledWith('case-1', '1', true);
    expect(
      component.checksSignal().find((x) => x.id === '1')?.markedAdverse,
    ).toBe(true);
  });

  it('un-marks (false) when the check is already marked', () => {
    const c = check({ id: '1', statut: 'NOT_FOUND', markedAdverse: true });
    serviceSpy.markAdverse.and.returnValue(of({ ...c, markedAdverse: false }));
    component.checks = [c];
    fixture.detectChanges();

    fixture.nativeElement
      .querySelector('[data-testid="mark-adverse-1"]')
      .click();

    expect(serviceSpy.markAdverse).toHaveBeenCalledWith('case-1', '1', false);
    expect(
      component.checksSignal().find((x) => x.id === '1')?.markedAdverse,
    ).toBe(false);
  });

  it('shows a snackbar and keeps state on service error', () => {
    const c = check({ id: '1', statut: 'SUSPECT', markedAdverse: false });
    serviceSpy.markAdverse.and.returnValue(throwError(() => new Error('boom')));
    component.checks = [c];
    fixture.detectChanges();

    fixture.nativeElement
      .querySelector('[data-testid="mark-adverse-1"]')
      .click();

    expect(snackSpy.open).toHaveBeenCalled();
    expect(
      component.checksSignal().find((x) => x.id === '1')?.markedAdverse,
    ).toBe(false);
  });

  it('shows the continuity note only when at least one markable citation exists', () => {
    component.checks = [
      check({ id: '1', statut: 'VERIFIED' }),
      check({ id: '2', statut: 'UNCERTAIN' }),
    ];
    fixture.detectChanges();
    expect(
      fixture.nativeElement.querySelector('[data-testid="adverse-continuity-note"]'),
    ).toBeNull();

    component.checks = [
      check({ id: '1', statut: 'VERIFIED' }),
      check({ id: '2', statut: 'SUSPECT' }),
    ];
    fixture.detectChanges();
    expect(
      fixture.nativeElement.querySelector('[data-testid="adverse-continuity-note"]'),
    ).not.toBeNull();
  });
});
