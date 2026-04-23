import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ImmigrationStrategyComparatorSectionComponent } from './immigration-strategy-comparator-section.component';
import { ImmigrationStrategyScenario } from '../../core/models/case-analysis.model';

describe('ImmigrationStrategyComparatorSectionComponent', () => {
  let fixture: ComponentFixture<ImmigrationStrategyComparatorSectionComponent>;
  let component: ImmigrationStrategyComparatorSectionComponent;

  const s1: ImmigrationStrategyScenario = {
    scenarioLabel: 'Changement immédiat',
    scenarioDescription: 'Déposer maintenant une demande VPF au titre du mariage.',
    baseLegale: 'Art. L.423-1 CESEDA',
    targetTitleCode: 'CST_VPF',
    targetTitleLabel: 'Carte VPF',
    delayDaysEstimate: '90-180',
    riskLevel: 'FAIBLE',
    riskJustification: 'Conditions remplies',
    requiredAdditionalPieces: ['Justificatif vie commune'],
    advantages: ['Droit au travail plein'],
    drawbacks: ['Perte mention Recherche'],
  };

  const s2: ImmigrationStrategyScenario = {
    scenarioLabel: 'Attendre expiration',
    scenarioDescription: 'Conserver le statut étudiant jusqu\'à soutenance.',
    baseLegale: 'Art. L.422-3 CESEDA',
    targetTitleCode: null,
    targetTitleLabel: 'Renouvellement étudiant',
    delayDaysEstimate: '60-120',
    riskLevel: 'MOYEN',
    riskJustification: 'Dépend calendrier thèse',
    requiredAdditionalPieces: [],
    advantages: ['Conservation mention Recherche'],
    drawbacks: ['Pas de droit au travail plein', 'Délai contraint'],
  };

  async function setup(scenarios: ImmigrationStrategyScenario[]) {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ImmigrationStrategyComparatorSectionComponent, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(ImmigrationStrategyComparatorSectionComponent);
    component = fixture.componentInstance;
    component.scenarios = scenarios;
    fixture.detectChanges();
  }

  it('U-05 — 2 scenarii → 2 cards affichées en grille', async () => {
    await setup([s1, s2]);
    const el: HTMLElement = fixture.nativeElement;
    const cards = el.querySelectorAll('.strategy-card');
    expect(cards.length).toBe(2);
    expect(cards[0].textContent).toContain('Changement immédiat');
    expect(cards[1].textContent).toContain('Attendre expiration');
  });

  it('U-06 — 0 ou 1 scénario → composant caché (pas de valeur comparative)', async () => {
    await setup([]);
    expect(fixture.nativeElement.querySelector('.strategies-panel')).toBeNull();

    await setup([s1]);
    expect(fixture.nativeElement.querySelector('.strategies-panel')).toBeNull();
  });

  it('U-07 — badge risque avec la bonne classe CSS selon niveau', () => {
    const c = TestBed.runInInjectionContext(() => new ImmigrationStrategyComparatorSectionComponent());
    expect(c.riskClass('FAIBLE')).toContain('faible');
    expect(c.riskClass('MOYEN')).toContain('moyen');
    expect(c.riskClass('ELEVE')).toContain('eleve');
    expect(c.riskClass(null)).toContain('unknown');
  });

  it('U-08 — listes avantages/inconvénients/pièces rendues', async () => {
    await setup([s1, s2]);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Droit au travail plein');
    expect(el.textContent).toContain('Perte mention Recherche');
    expect(el.textContent).toContain('Justificatif vie commune');
    expect(el.textContent).toContain('Délai contraint');
  });

  it('U-09 — riskLabel retourne les bons libellés', () => {
    const c = TestBed.runInInjectionContext(() => new ImmigrationStrategyComparatorSectionComponent());
    expect(c.riskLabel('FAIBLE')).toBe('Risque faible');
    expect(c.riskLabel('MOYEN')).toBe('Risque moyen');
    expect(c.riskLabel('ELEVE')).toBe('Risque élevé');
    expect(c.riskLabel(null)).toContain('qualifier');
  });
});
