import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TribunalTravailFicheSectionComponent } from './tribunal-travail-fiche-section.component';

describe('TribunalTravailFicheSectionComponent', () => {
  let component: TribunalTravailFicheSectionComponent;
  let fixture: ComponentFixture<TribunalTravailFicheSectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TribunalTravailFicheSectionComponent, NoopAnimationsModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(TribunalTravailFicheSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'test-id';
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('affiche le header "Requête tribunal du travail"', () => {
    const header = fixture.nativeElement.querySelector('.section-title');
    expect(header?.textContent).toContain('Requête tribunal du travail');
  });

  it('collapsed par défaut', () => {
    expect(component.collapsed()).toBe(true);
    expect(fixture.nativeElement.querySelector('.fiche-form')).toBeNull();
  });

  it('toggleCollapsed ouvre le formulaire', () => {
    component.toggleCollapsed();
    fixture.detectChanges();
    expect(component.collapsed()).toBe(false);
    expect(fixture.nativeElement.querySelector('.fiche-form')).toBeTruthy();
  });

  it('addDemande ajoute une demande au FormArray', () => {
    expect(component.demandesArray.length).toBe(0);
    component.addDemande();
    expect(component.demandesArray.length).toBe(1);
  });

  it('removeDemande supprime une demande', () => {
    component.addDemande();
    component.addDemande();
    expect(component.demandesArray.length).toBe(2);
    component.removeDemande(0);
    expect(component.demandesArray.length).toBe(1);
  });
});
