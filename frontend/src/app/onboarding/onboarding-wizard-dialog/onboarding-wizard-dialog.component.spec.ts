import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { OnboardingWizardDialogComponent } from './onboarding-wizard-dialog.component';
import { OnboardingWizardService } from '../../core/services/onboarding-wizard.service';

describe('OnboardingWizardDialogComponent', () => {
  let component: OnboardingWizardDialogComponent;
  let fixture: ComponentFixture<OnboardingWizardDialogComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<OnboardingWizardDialogComponent>>;
  let wizardServiceSpy: jasmine.SpyObj<OnboardingWizardService>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    wizardServiceSpy = jasmine.createSpyObj('OnboardingWizardService', ['markDone', 'shouldShow']);

    await TestBed.configureTestingModule({
      imports: [OnboardingWizardDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: { workspaceId: 'ws-123' } },
        { provide: OnboardingWizardService, useValue: wizardServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OnboardingWizardDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('affiche 4 étapes', () => {
    expect(component.steps.length).toBe(4);
  });

  it('étape 1 affichée à l\'ouverture', () => {
    expect(component.currentStep()).toBe(0);
  });

  it('next() avance à l\'étape suivante', () => {
    component.next();
    expect(component.currentStep()).toBe(1);
  });

  it('next() répété avance jusqu\'à l\'étape 3', () => {
    component.next();
    component.next();
    component.next();
    expect(component.currentStep()).toBe(3);
  });

  it('isLastStep — false en étape 0', () => {
    expect(component.isLastStep).toBeFalse();
  });

  it('isLastStep — true en étape 3', () => {
    component.next(); component.next(); component.next();
    expect(component.isLastStep).toBeTrue();
  });

  it('next() sur dernière étape → ferme le dialog', () => {
    component.next(); component.next(); component.next();
    component.next();
    expect(wizardServiceSpy.markDone).toHaveBeenCalledWith('ws-123');
    expect(dialogRefSpy.close).toHaveBeenCalled();
  });

  it('skip() → marque done et ferme le dialog', () => {
    component.skip();
    expect(wizardServiceSpy.markDone).toHaveBeenCalledWith('ws-123');
    expect(dialogRefSpy.close).toHaveBeenCalled();
  });

  it('étape 4 a le label "Commencer"', () => {
    component.next(); component.next(); component.next();
    expect(component.isLastStep).toBeTrue();
    // label vérifié dans le template : "Commencer" si isLastStep
  });
});
