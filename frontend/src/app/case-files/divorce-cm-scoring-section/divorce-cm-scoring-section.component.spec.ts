import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DivorceCmScoringSectionComponent } from './divorce-cm-scoring-section.component';
import {
  CaseAnalysisResult,
  DivorceConsentementValidityDetection,
  DivorceConsentementScoring,
} from '../../core/models/case-analysis.model';

describe('DivorceCmScoringSectionComponent', () => {
  let fixture: ComponentFixture<DivorceCmScoringSectionComponent>;
  let component: DivorceCmScoringSectionComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DivorceCmScoringSectionComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DivorceCmScoringSectionComponent);
    component = fixture.componentInstance;
  });

  it('expose les statics F-177 attendus', () => {
    expect(DivorceCmScoringSectionComponent.TOOL_LABEL).toBe('VALIDITÉ DIVORCE CONSENTEMENT MUTUEL');
    expect(DivorceCmScoringSectionComponent.TOOL_ICON).toBe('verified');
    expect(DivorceCmScoringSectionComponent.getPrefillCount()).toBe(0);
  });

  it('rend l\'état vide quand synthesis est null', () => {
    component.synthesis = null;
    fixture.detectChanges();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Aucune analyse de validité divorce CM');
    expect(component.hasData).toBe(false);
  });

  it('rend l\'état vide quand seule la détection est disponible (pas de scoring)', () => {
    const detection: DivorceConsentementValidityDetection = {
      detections: { DC_MAJORITE: { reponse: 'OUI' } },
    };
    component.synthesis = {
      divorceConsentementValidityDetection: detection,
      divorceConsentementScoring: null,
    } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    expect(component.hasData).toBe(false);
  });

  it('délègue au composant child quand détection et scoring sont fournis', () => {
    const detection: DivorceConsentementValidityDetection = {
      detections: {
        DC_MAJORITE: { reponse: 'OUI' },
        DC_CONSENTEMENT_LIBRE: { reponse: 'OUI' },
        DC_CONVENTION_EQUITABLE: { reponse: 'OUI' },
        DC_ENFANT_MINEUR_ENTENDU: { reponse: 'INCONNU' },
        DC_DELAI_REFLEXION_15J: { reponse: 'OUI' },
        DC_NOTAIRE_DEPOT: { reponse: 'OUI' },
        DC_INDEPENDANCE_AVOCATS: { reponse: 'OUI' },
      },
    };
    const scoring: DivorceConsentementScoring = {
      score: 86,
      verdict: 'VALIDE',
      criteresValides: ['DC_MAJORITE', 'DC_CONSENTEMENT_LIBRE'],
      criteresNonValides: [],
      criteresInconnus: ['DC_ENFANT_MINEUR_ENTENDU'],
    };
    component.synthesis = {
      divorceConsentementValidityDetection: detection,
      divorceConsentementScoring: scoring,
    } as unknown as CaseAnalysisResult;
    fixture.detectChanges();
    expect(component.hasData).toBe(true);
    // Le composant child est rendu (présence d'un sous-élément)
    const childEl = (fixture.nativeElement as HTMLElement).querySelector(
      'app-divorce-consentement-scoring-section',
    );
    expect(childEl).not.toBeNull();
  });
});
