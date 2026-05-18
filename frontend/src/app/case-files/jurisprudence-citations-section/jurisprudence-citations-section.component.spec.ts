import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { JurisprudenceCitationsSectionComponent } from './jurisprudence-citations-section.component';
import { JurisprudenceCheck } from '../../core/models/jurisprudence-check.model';

describe('JurisprudenceCitationsSectionComponent', () => {
  let fixture: ComponentFixture<JurisprudenceCitationsSectionComponent>;
  let component: JurisprudenceCitationsSectionComponent;

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
    };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JurisprudenceCitationsSectionComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(JurisprudenceCitationsSectionComponent);
    component = fixture.componentInstance;
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
});
