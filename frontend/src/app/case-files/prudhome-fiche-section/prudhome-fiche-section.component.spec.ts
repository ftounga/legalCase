import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { PrudhomeFicheSectionComponent } from './prudhome-fiche-section.component';
import { PrudhomeFicheService } from '../../core/services/prudhome-fiche.service';
import { PrudhomeFiche } from '../../core/models/prudhome-fiche.model';

function makeFiche(overrides: Partial<PrudhomeFiche> = {}): PrudhomeFiche {
  return {
    id: 'f-1',
    demandeur: { nom: 'Dupont', prenom: 'Jean', adresse: null, telephone: null, email: null, profession: null },
    defendeur: { nom: 'Renault SAS', adresse: null, siret: null, representant: null },
    demandes: [{ label: 'Indemnité', montant: 5000 }],
    faitsTexte: 'Licenciement abusif',
    moyensDroitTexte: 'Art. L1235-3',
    piecesList: [{ numero: 1, nom: 'contrat.pdf' }],
    updatedAt: '2026-04-04T10:00:00Z',
    ...overrides
  };
}

describe('PrudhomeFicheSectionComponent', () => {
  let fixture: ComponentFixture<PrudhomeFicheSectionComponent>;
  let component: PrudhomeFicheSectionComponent;
  let ficheServiceSpy: jest.Mocked<PrudhomeFicheService>;
  let snackBarSpy: { open: jest.Mock };

  beforeEach(async () => {
    ficheServiceSpy = {
      get: jest.fn(),
      save: jest.fn()
    } as unknown as jest.Mocked<PrudhomeFicheService>;
    ficheServiceSpy.get.mockReturnValue(of(makeFiche()));

    snackBarSpy = { open: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [PrudhomeFicheSectionComponent, NoopAnimationsModule],
      providers: [
        { provide: PrudhomeFicheService, useValue: ficheServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PrudhomeFicheSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    fixture.detectChanges();
  });

  // SFDT02-01 : chargement fiche existante → formulaire pré-rempli
  it('SFDT02-01 should prefill form from existing fiche', () => {
    expect(component.form.get('demandeur.nom')?.value).toBe('Dupont');
    expect(component.form.get('defendeur.nom')?.value).toBe('Renault SAS');
    expect(component.demandesArray.length).toBe(1);
    expect(component.pieces()).toHaveLength(1);
    expect(component.pieces()[0].nom).toBe('contrat.pdf');
  });

  // SFDT02-02 : GET en erreur → formulaire conserve ses valeurs par défaut (vides)
  it('SFDT02-02 should keep default empty form values on GET error', () => {
    // Re-invoke ngOnInit with error — the form was already populated in beforeEach
    // We simulate a fresh component state by patching the service and calling ngOnInit
    component.form.reset();
    component.demandesArray.clear();
    ficheServiceSpy.get.mockReturnValue(throwError(() => new Error('Network error')));

    // ngOnInit calls get() — on error the form should stay empty
    component.ngOnInit();

    expect(component.form.get('demandeur.nom')?.value).toBeFalsy();
    expect(component.demandesArray.length).toBe(0);
  });

  // SFDT02-03 : sauvegarde nominale → toast succès
  it('SFDT02-03 should show success toast on save', () => {
    ficheServiceSpy.save.mockReturnValue(of(makeFiche()));
    component.collapsed.set(false);

    component.save();

    expect(ficheServiceSpy.save).toHaveBeenCalledWith('cf-1', expect.any(Object));
    expect(snackBarSpy.open).toHaveBeenCalledWith('Fiche enregistrée', 'Fermer', expect.any(Object));
  });

  // SFDT02-04 : sauvegarde en erreur → toast erreur, données conservées
  it('SFDT02-04 should show error toast on save failure', () => {
    ficheServiceSpy.save.mockReturnValue(throwError(() => new Error('500')));
    const nomBefore = component.form.get('demandeur.nom')?.value;

    component.save();

    expect(snackBarSpy.open).toHaveBeenCalledWith('Erreur lors de la sauvegarde', 'Fermer', expect.any(Object));
    expect(component.form.get('demandeur.nom')?.value).toBe(nomBefore);
  });

  // SFDT02-05 : ajout et suppression d'une demande
  it('SFDT02-05 should add and remove demandes', () => {
    const before = component.demandesArray.length;
    component.addDemande();
    expect(component.demandesArray.length).toBe(before + 1);

    component.removeDemande(0);
    expect(component.demandesArray.length).toBe(before);
  });
});
