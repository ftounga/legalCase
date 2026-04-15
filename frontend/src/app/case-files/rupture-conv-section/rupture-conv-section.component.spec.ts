import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { RuptureConvSectionComponent } from './rupture-conv-section.component';
import { RuptureConvService } from '../../core/services/rupture-conv.service';
import { RuptureConvResponse } from '../../core/models/rupture-conv.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('RuptureConvSectionComponent', () => {
  let fixture: ComponentFixture<RuptureConvSectionComponent>;
  let component: RuptureConvSectionComponent;
  let rcService: jest.Mocked<RuptureConvService>;
  let snackBar: jest.Mocked<MatSnackBar>;
  let refreshService: CaseDashboardRefreshService;

  const fakeResponse: RuptureConvResponse = {
    caseFileId: 'case-1',
    country: 'FRANCE',
    scoreRisque: 25,
    verdict: 'INVALIDE',
    criteres: [
      { code: 'RC_CONSENTEMENT', label: 'Consentement', reponse: 'NON',
        pointsRisque: 25, bloquant: true, commentaire: 'NON CONFORME (bloquant)' },
      { code: 'RC_DELAI_RETRACTATION', label: 'Délai', reponse: 'OUI',
        pointsRisque: 0, bloquant: true, commentaire: 'Conforme' },
      { code: 'RC_HOMOLOGATION', label: 'Homologation', reponse: 'OUI',
        pointsRisque: 0, bloquant: true, commentaire: 'Conforme' },
      { code: 'RC_ASSISTANCE', label: 'Assistance', reponse: 'OUI',
        pointsRisque: 0, bloquant: false, commentaire: 'Conforme' },
      { code: 'RC_INDEMNITE', label: 'Indemnité', reponse: 'OUI',
        pointsRisque: 0, bloquant: true, commentaire: 'Conforme' },
      { code: 'RC_ENTRETIENS', label: 'Entretien', reponse: 'OUI',
        pointsRisque: 0, bloquant: false, commentaire: 'Conforme' },
    ],
  };

  beforeEach(async () => {
    rcService = { get: jest.fn(), analyze: jest.fn() } as any;
    snackBar = { open: jest.fn() } as any;
    refreshService = new CaseDashboardRefreshService();

    await TestBed.configureTestingModule({
      imports: [RuptureConvSectionComponent],
      providers: [
        provideNoopAnimations(),
        { provide: RuptureConvService, useValue: rcService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: CaseDashboardRefreshService, useValue: refreshService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RuptureConvSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
  });

  it('starts with all criteres INCONNU and shows form on GET 404', () => {
    rcService.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    fixture.detectChanges();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
    Object.values(component.reponses()).forEach(v => expect(v).toBe('INCONNU'));
  });

  it('preloads result and hides form when GET returns 200', () => {
    rcService.get.mockReturnValue(of(fakeResponse));
    fixture.detectChanges();
    expect(component.showForm()).toBe(false);
    expect(component.result()).toEqual(fakeResponse);
    expect(component.reponses()['RC_CONSENTEMENT']).toBe('NON');
    expect(component.reponses()['RC_DELAI_RETRACTATION']).toBe('OUI');
  });

  it('updates reponses when setReponse is called', () => {
    rcService.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    fixture.detectChanges();
    component.setReponse('RC_CONSENTEMENT', 'NON');
    expect(component.reponses()['RC_CONSENTEMENT']).toBe('NON');
    expect(component.reponses()['RC_DELAI_RETRACTATION']).toBe('INCONNU');
  });

  it('POST analyze success: shows result, triggers dashboard refresh', () => {
    rcService.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    rcService.analyze.mockReturnValue(of(fakeResponse));
    const triggerSpy = jest.spyOn(refreshService, 'triggerRefresh');
    fixture.detectChanges();
    component.setReponse('RC_CONSENTEMENT', 'NON');
    component.analyze();
    expect(rcService.analyze).toHaveBeenCalledWith('case-1',
      expect.objectContaining({ country: 'FRANCE' }));
    expect(component.result()).toEqual(fakeResponse);
    expect(component.showForm()).toBe(false);
    expect(triggerSpy).toHaveBeenCalledTimes(1);
  });

  it('POST analyze error: shows snackbar, no refresh', () => {
    rcService.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    rcService.analyze.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const triggerSpy = jest.spyOn(refreshService, 'triggerRefresh');
    fixture.detectChanges();
    component.analyze();
    expect(snackBar.open).toHaveBeenCalledWith('Erreur lors de l\'analyse', 'Fermer',
      expect.objectContaining({ duration: 4000 }));
    expect(triggerSpy).not.toHaveBeenCalled();
    expect(component.analyzing()).toBe(false);
  });

  it('editForm puts back showForm true without clearing reponses', () => {
    rcService.get.mockReturnValue(of(fakeResponse));
    fixture.detectChanges();
    expect(component.showForm()).toBe(false);
    component.editForm();
    expect(component.showForm()).toBe(true);
    // reponses préservées depuis le résultat
    expect(component.reponses()['RC_CONSENTEMENT']).toBe('NON');
  });

  it('verdictColor returns design-system colors for each verdict', () => {
    expect(component.verdictColor('VALIDE')).toBe('#27AE60');
    expect(component.verdictColor('RISQUE_MODERE')).toBe('#F59E0B');
    expect(component.verdictColor('RISQUE_ELEVE')).toBe('#E67E22');
    expect(component.verdictColor('INVALIDE')).toBe('#C0392B');
    expect(component.verdictColor(undefined)).toBe('#6B7A8D');
  });

  it('pre-fills reponses from aiData.detections when GET 404', () => {
    rcService.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    component.aiData = {
      detections: {
        RC_CONSENTEMENT: { reponse: 'OUI' },
        RC_INDEMNITE: { reponse: 'NON', justification: 'Indemnité trop faible' },
      },
    };
    fixture.detectChanges();
    expect(component.reponses()['RC_CONSENTEMENT']).toBe('OUI');
    expect(component.reponses()['RC_INDEMNITE']).toBe('NON');
    expect(component.reponses()['RC_DELAI_RETRACTATION']).toBe('INCONNU');
  });

  it('F-96 VERIFIED on RC_HOMOLOGATION + user set NON → blocker alert', () => {
    rcService.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    component.procedureChecks = [
      { id: 'p1', ordre: 1, description: 'Homologation', statut: 'VERIFIED',
        raison: 'Homologation obtenue', critereCode: 'RC_HOMOLOGATION' },
    ];
    fixture.detectChanges();
    component.setReponse('RC_HOMOLOGATION', 'NON');
    const alert = component.coherenceAlerts()['RC_HOMOLOGATION'];
    expect(alert).toBeTruthy();
    expect(alert.level).toBe('blocker');
    expect(alert.source).toBe('F96');
    expect(alert.expectedReponse).toBe('OUI');
  });

  it('IA question answered "oui" on RC_ASSISTANCE + user set NON → warning alert (non-blocker)', () => {
    rcService.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    component.aiQuestions = [
      { id: 'q1', orderIndex: 1, questionText: 'Assistance documentée ?',
        answerText: 'oui', critereCode: 'RC_ASSISTANCE' },
    ];
    fixture.detectChanges();
    component.setReponse('RC_ASSISTANCE', 'NON');
    const alert = component.coherenceAlerts()['RC_ASSISTANCE'];
    expect(alert).toBeTruthy();
    expect(alert.level).toBe('warning');
    expect(alert.source).toBe('QUESTION_IA');
    expect(alert.expectedReponse).toBe('OUI');
  });

  it('aiData detection OUI on RC_INDEMNITE + user set NON → blocker IA alert', () => {
    rcService.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    component.aiData = {
      detections: {
        RC_INDEMNITE: { reponse: 'OUI', justification: 'Indemnité conforme' },
      },
    };
    fixture.detectChanges();
    // aiData pre-fills to OUI; override user choice manually
    component.setReponse('RC_INDEMNITE', 'NON');
    const alert = component.coherenceAlerts()['RC_INDEMNITE'];
    expect(alert).toBeTruthy();
    expect(alert.level).toBe('blocker');
    expect(alert.source).toBe('IA');
    expect(alert.aiReponse).toBe('OUI');
  });

  it('no alerts when showForm is false', () => {
    rcService.get.mockReturnValue(of(fakeResponse));
    component.aiData = {
      detections: { RC_CONSENTEMENT: { reponse: 'OUI' } },
    };
    fixture.detectChanges();
    expect(component.showForm()).toBe(false);
    expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
  });
});
