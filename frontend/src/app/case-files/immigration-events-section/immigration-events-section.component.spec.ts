import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ImmigrationEventsSectionComponent } from './immigration-events-section.component';
import { ImmigrationTriggerEvent } from '../../core/models/case-analysis.model';

describe('ImmigrationEventsSectionComponent', () => {
  let fixture: ComponentFixture<ImmigrationEventsSectionComponent>;
  let component: ImmigrationEventsSectionComponent;

  const mariage: ImmigrationTriggerEvent = {
    eventCode: 'MARIAGE_RESSORTISSANT_FR',
    eventLabel: 'Mariage avec un ressortissant français',
    eventDate: '2025-03-15',
    sourceDocument: 'acte_mariage_n127.pdf',
    justification: 'Mariage célébré le 15/03/2025, acte n°127 Mairie du 5ème',
    baseLegale: 'Art. L.423-1 CESEDA — conjoint de Français',
    suggestedTitleCode: 'CST_VPF',
    suggestedTitleLabel: 'Carte de séjour temporaire « Vie privée et familiale »',
  };

  async function setup(events: ImmigrationTriggerEvent[]) {
    await TestBed.configureTestingModule({
      imports: [ImmigrationEventsSectionComponent, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(ImmigrationEventsSectionComponent);
    component = fixture.componentInstance;
    component.events = events;
    fixture.detectChanges();
  }

  it('U-01 — rend une carte par événement avec titre, date, justification, base légale', async () => {
    await setup([mariage]);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Mariage avec un ressortissant français');
    expect(el.textContent).toContain('15/03/2025');
    expect(el.textContent).toContain('L.423-1 CESEDA');
    expect(el.textContent).toContain('Vie privée et familiale');
  });

  it('U-02 — aucun événement → rien n\'est rendu', async () => {
    await setup([]);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.event-card')).toBeNull();
    expect(el.querySelector('.events-panel')).toBeNull();
  });

  it('U-03 — iconFor retourne l\'icône appropriée par code', () => {
    const c = TestBed.runInInjectionContext(() => new ImmigrationEventsSectionComponent());
    expect(c.iconFor('MARIAGE_RESSORTISSANT_FR')).toBe('favorite');
    expect(c.iconFor('DOCTORAT_OBTENU')).toBe('school');
    expect(c.iconFor('CDI_OBTENU_SALARIE')).toBe('work');
    expect(c.iconFor('NAISSANCE_ENFANT_FR')).toBe('child_friendly');
    expect(c.iconFor('CODE_INCONNU')).toBe('new_releases');
  });

  it('U-04 — événement sans sourceDocument ni justification → pas d\'erreur, card minimale', async () => {
    const minimal: ImmigrationTriggerEvent = {
      ...mariage,
      sourceDocument: null,
      justification: null,
      eventDate: null,
    };
    await setup([minimal]);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Mariage avec un ressortissant français');
    expect(el.querySelector('.event-card-source')).toBeNull();
    expect(el.querySelector('.event-card-justification')).toBeNull();
    expect(el.querySelector('.event-card-date')).toBeNull();
  });

  it('U-05 — plusieurs événements → plusieurs cards ordonnées', async () => {
    const doctorat: ImmigrationTriggerEvent = {
      eventCode: 'DOCTORAT_OBTENU',
      eventLabel: 'Obtention d\'un doctorat en France',
      eventDate: '2026-10-15',
      sourceDocument: 'attestation_these.pdf',
      justification: 'Soutenance prévue octobre 2026',
      baseLegale: 'Art. L.421-14 CESEDA — passeport-talent chercheur',
      suggestedTitleCode: 'CARTE_PLURIANNUELLE',
      suggestedTitleLabel: 'Carte pluriannuelle « Passeport talent — chercheur »',
    };
    await setup([mariage, doctorat]);
    const el: HTMLElement = fixture.nativeElement;
    const cards = el.querySelectorAll('.event-card');
    expect(cards.length).toBe(2);
    expect(cards[0].textContent).toContain('Mariage');
    expect(cards[1].textContent).toContain('doctorat');
  });
});
