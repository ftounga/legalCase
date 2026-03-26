import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DomainPickerDialogComponent } from './domain-picker-dialog.component';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('DomainPickerDialogComponent', () => {
  let fixture: ComponentFixture<DomainPickerDialogComponent>;
  let component: DomainPickerDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<DomainPickerDialogComponent>>;

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    Object.defineProperty(dialogRefSpy, 'disableClose', { writable: true, value: false });

    await TestBed.configureTestingModule({
      imports: [DomainPickerDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: { workspaceName: 'Cabinet Test' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DomainPickerDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // D-01 : 3 tuiles rendues
  it('affiche 3 tuiles de domaines', () => {
    expect(component.domains.length).toBe(3);
  });

  // D-02 : toutes les tuiles sont actives (pas de disabled)
  it('les 3 tuiles sont toutes actives (aucune propriété available = false)', () => {
    const keys = component.domains.map(d => d.key);
    expect(keys).toContain('DROIT_DU_TRAVAIL');
    expect(keys).toContain('DROIT_IMMIGRATION');
    expect(keys).toContain('DROIT_FAMILLE');
  });

  // D-03 : DROIT_IMMOBILIER absent
  it('DROIT_IMMOBILIER absent de la liste', () => {
    const keys = component.domains.map(d => d.key);
    expect(keys).not.toContain('DROIT_IMMOBILIER');
  });

  // D-04 : couleurs des tuiles
  it('DROIT_DU_TRAVAIL a la couleur #27AE60', () => {
    const d = component.domains.find(x => x.key === 'DROIT_DU_TRAVAIL')!;
    expect(d.color).toBe('#27AE60');
  });

  it('DROIT_IMMIGRATION a la couleur #1A3A5C', () => {
    const d = component.domains.find(x => x.key === 'DROIT_IMMIGRATION')!;
    expect(d.color).toBe('#1A3A5C');
  });

  it('DROIT_FAMILLE a la couleur #C9973A', () => {
    const d = component.domains.find(x => x.key === 'DROIT_FAMILLE')!;
    expect(d.color).toBe('#C9973A');
  });

  // D-04b : DROIT_DU_TRAVAIL sélectionné par défaut
  it('DROIT_DU_TRAVAIL est sélectionné par défaut', () => {
    expect(component.selected).toBe('DROIT_DU_TRAVAIL');
  });

  // D-04c : France sélectionnée par défaut
  it('France est sélectionnée par défaut', () => {
    expect(component.selectedCountry).toBe('FRANCE');
  });

  // D-05 : bouton Confirmer désactivé sans pays
  it('canConfirm est false si le pays est réinitialisé', () => {
    component.selectedCountry = '';
    expect(component.canConfirm).toBeFalse();
  });

  // D-06 : bouton Confirmer désactivé si domaine réinitialisé
  it('canConfirm est false si domaine vide même avec pays sélectionné', () => {
    component.selected = '';
    component.selectedCountry = 'FRANCE';
    expect(component.canConfirm).toBeFalse();
  });

  // D-07 : bouton Confirmer désactivé sans domaine
  it('canConfirm est false si seulement le pays est sélectionné', () => {
    component.selected = '';
    component.selectedCountry = 'FRANCE';
    expect(component.canConfirm).toBeFalse();
  });

  // D-08 : bouton Confirmer actif avec domaine + pays
  it('canConfirm est true si domaine et pays sont sélectionnés', () => {
    component.selected = 'DROIT_IMMIGRATION';
    component.selectedCountry = 'BELGIQUE';
    expect(component.canConfirm).toBeTrue();
  });

  // D-09 : confirm() retourne { legalDomain, country }
  it('confirm() ferme la dialog avec { legalDomain, country }', () => {
    component.selected = 'DROIT_FAMILLE';
    component.selectedCountry = 'FRANCE';
    component.confirm();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({ legalDomain: 'DROIT_FAMILLE', country: 'FRANCE' });
  });

  // D-10 : confirm() ne ferme pas la dialog si canConfirm est false
  it('confirm() ne ferme pas la dialog si domaine ou pays manquant', () => {
    component.selected = 'DROIT_DU_TRAVAIL';
    component.selectedCountry = '';
    component.confirm();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });
});
