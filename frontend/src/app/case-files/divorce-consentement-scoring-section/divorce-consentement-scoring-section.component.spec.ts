import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DivorceConsentementScoringSectionComponent } from './divorce-consentement-scoring-section.component';
import {
  DivorceConsentementScoring,
  DivorceConsentementValidityDetection,
} from '../../core/models/case-analysis.model';

describe('DivorceConsentementScoringSectionComponent', () => {
  let fixture: ComponentFixture<DivorceConsentementScoringSectionComponent>;
  let component: DivorceConsentementScoringSectionComponent;

  const detection: DivorceConsentementValidityDetection = {
    detections: {
      DC_MAJORITE: { reponse: 'OUI', justification: 'majeurs' },
      DC_CONSENTEMENT_LIBRE: { reponse: 'OUI', justification: 'aucun vice' },
      DC_CONVENTION_EQUITABLE: { reponse: 'NON', justification: 'déséquilibre' },
      DC_DELAI_REFLEXION_15J: { reponse: 'INCONNU', justification: null as any },
    },
  };

  const scoringOk: DivorceConsentementScoring = {
    score: 100,
    verdict: 'VALIDE',
    criteresValides: ['DC_MAJORITE'],
    criteresNonValides: [],
    criteresInconnus: [],
  };

  const scoringMoyen: DivorceConsentementScoring = {
    score: 57,
    verdict: 'RISQUE_MOYEN',
    criteresValides: ['DC_MAJORITE', 'DC_CONSENTEMENT_LIBRE'],
    criteresNonValides: ['DC_CONVENTION_EQUITABLE'],
    criteresInconnus: ['DC_DELAI_REFLEXION_15J'],
  };

  const scoringElev: DivorceConsentementScoring = {
    score: 30,
    verdict: 'RISQUE_ELEVE_NULLITE',
    criteresValides: [],
    criteresNonValides: ['DC_MAJORITE'],
    criteresInconnus: [],
  };

  async function setup(det: DivorceConsentementValidityDetection | null,
                       sc: DivorceConsentementScoring | null) {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [DivorceConsentementScoringSectionComponent, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(DivorceConsentementScoringSectionComponent);
    component = fixture.componentInstance;
    component.detection = det;
    component.scoring = sc;
    fixture.detectChanges();
  }

  it('U-06 — affiche le score dans la jauge', async () => {
    await setup(detection, scoringMoyen);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('57');
  });

  it('U-07 — affiche les 7 critères dans l\'ordre', async () => {
    await setup(detection, scoringMoyen);
    const el: HTMLElement = fixture.nativeElement;
    const items = el.querySelectorAll('.critere');
    expect(items.length).toBe(7);
    expect(items[0].textContent).toContain('Les deux époux majeurs');
    expect(items[6].textContent).toContain('Un avocat distinct par époux');
  });

  it('U-08 — verdictClass retourne la bonne classe', () => {
    const c = TestBed.runInInjectionContext(() => new DivorceConsentementScoringSectionComponent());
    expect(c.verdictClass('VALIDE')).toContain('ok');
    expect(c.verdictClass('RISQUE_MOYEN')).toContain('warn');
    expect(c.verdictClass('RISQUE_ELEVE_NULLITE')).toContain('error');
  });

  it('U-09 — section cachée si detection absente', async () => {
    await setup(null, null);
    expect(fixture.nativeElement.querySelector('.dc-panel')).toBeNull();
  });

  it('U-10 — iconFor mappe OUI/NON/INCONNU', () => {
    const c = TestBed.runInInjectionContext(() => new DivorceConsentementScoringSectionComponent());
    expect(c.iconFor('OUI')).toBe('check_circle');
    expect(c.iconFor('NON')).toBe('cancel');
    expect(c.iconFor('INCONNU')).toBe('help_outline');
    expect(c.iconFor(null)).toBe('help_outline');
  });

  it('U-11 — verdictLabel humanise les 3 verdicts', async () => {
    const c = TestBed.runInInjectionContext(() => new DivorceConsentementScoringSectionComponent());
    expect(c.verdictLabel('VALIDE')).toContain('Validité');
    expect(c.verdictLabel('RISQUE_MOYEN')).toContain('modéré');
    expect(c.verdictLabel('RISQUE_ELEVE_NULLITE')).toContain('nullité');
  });
});
