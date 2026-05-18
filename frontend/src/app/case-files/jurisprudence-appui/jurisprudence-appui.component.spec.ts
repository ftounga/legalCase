import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { JurisprudenceAppuiComponent } from './jurisprudence-appui.component';
import { JurisprudenceCitationService } from '../../core/services/jurisprudence-citation.service';
import { JurisprudenceCitation } from '../../core/models/jurisprudence-citation.model';

const CASE_FILE_ID = 'cf-1';

const makeCitation = (over: Partial<JurisprudenceCitation> = {}): JurisprudenceCitation => ({
  id: 'cit-1',
  pointJuridiqueIndex: 0,
  pointJuridiqueTexte: 'Licenciement pour faute grave',
  reference: 'Cass. soc. 12 oct. 2022, n° 21-12345',
  portee: 'La faute grave doit être établie',
  createdAt: '2026-05-18T09:00:00Z',
  updatedAt: '2026-05-18T09:00:00Z',
  ...over,
});

describe('JurisprudenceAppuiComponent (F-242 SF-242-02)', () => {
  let fixture: ComponentFixture<JurisprudenceAppuiComponent>;
  let component: JurisprudenceAppuiComponent;
  let citationService: jest.Mocked<JurisprudenceCitationService>;
  let dialog: jest.Mocked<MatDialog>;
  let snackBar: jest.Mocked<MatSnackBar>;

  beforeEach(async () => {
    citationService = jasmine.createSpyObj('JurisprudenceCitationService', [
      'list', 'create', 'update', 'delete',
    ]) as any;
    dialog = jasmine.createSpyObj('MatDialog', ['open']) as any;
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']) as any;

    await TestBed.configureTestingModule({
      imports: [JurisprudenceAppuiComponent, NoopAnimationsModule],
      providers: [
        { provide: JurisprudenceCitationService, useValue: citationService },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(JurisprudenceAppuiComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
    component.pointJuridiqueIndex = 3;
    component.pointJuridiqueTexte = 'Préavis non respecté';
  });

  // U-1 : zone repliée quand aucune citation — seul l'appel à l'action visible.
  it('U1: collapsed CTA when the point has no citation', () => {
    component.citations = [];
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Jurisprudence à l\'appui');
    expect(fixture.nativeElement.querySelector('.juris-appui__cta')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.juris-appui__form')).toBeFalsy();
    expect(component.addFormOpen()).toBe(false);
  });

  // U-2 : la liste des citations existantes est rendue.
  it('U2: renders the existing citations of the point', () => {
    component.citations = [
      makeCitation({ id: 'a', reference: 'Réf A' }),
      makeCitation({ id: 'b', reference: 'Réf B', portee: null }),
    ];
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('.juris-appui__item');
    expect(items.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Réf A');
    expect(fixture.nativeElement.textContent).toContain('Réf B');
  });

  // U-3 : openAddForm déplie le formulaire d'ajout.
  it('U3: openAddForm reveals the add form', () => {
    component.citations = [];
    fixture.detectChanges();

    component.openAddForm();
    fixture.detectChanges();

    expect(component.addFormOpen()).toBe(true);
    expect(fixture.nativeElement.querySelector('.juris-appui__form--add')).toBeTruthy();
  });

  // U-4 : ajout — POST avec pointJuridiqueIndex / pointJuridiqueTexte du point courant.
  it('U4: add posts the citation with the current point index and text', () => {
    component.citations = [];
    const created = makeCitation({ id: 'new', reference: 'Nouvelle réf' });
    citationService.create.mockReturnValue(of(created));

    component.openAddForm();
    component.newReference = '  Nouvelle réf  ';
    component.newPortee = '  La portée  ';
    component.add();

    expect(citationService.create).toHaveBeenCalledWith(CASE_FILE_ID, {
      pointJuridiqueIndex: 3,
      pointJuridiqueTexte: 'Préavis non respecté',
      reference: 'Nouvelle réf',
      portee: 'La portée',
    });
    expect(component.citationsSignal()).toContain(created);
    expect(component.addFormOpen()).toBe(false);
    expect(component.newReference).toBe('');
  });

  // U-5 : portée vide → null dans le payload.
  it('U5: empty portee is sent as null', () => {
    component.citations = [];
    citationService.create.mockReturnValue(of(makeCitation({ portee: null })));

    component.newReference = 'Réf';
    component.newPortee = '   ';
    component.add();

    expect(citationService.create).toHaveBeenCalledWith(
      CASE_FILE_ID,
      jasmine.objectContaining({ portee: null }),
    );
  });

  // U-6 : reference vide → ajout impossible (canAdd false, pas d'appel API).
  it('U6: empty reference blocks the add', () => {
    component.citations = [];
    component.newReference = '   ';

    expect(component.canAdd()).toBe(false);
    component.add();
    expect(citationService.create).not.toHaveBeenCalled();
  });

  // U-7 : erreur API à l'ajout → snackbar, saisie conservée.
  it('U7: API error on add shows a snackbar and keeps the input', () => {
    component.citations = [];
    citationService.create.mockReturnValue(throwError(() => new Error('boom')));

    component.newReference = 'Réf saisie';
    component.newPortee = 'Portée saisie';
    component.add();

    expect(snackBar.open).toHaveBeenCalled();
    expect(component.newReference).toBe('Réf saisie');
    expect(component.newPortee).toBe('Portée saisie');
    expect(component.saving()).toBe(false);
  });

  // U-8 : édition — PUT avec reference / portee.
  it('U8: saveEdit puts the updated reference and portee', () => {
    const original = makeCitation({ id: 'edit-me' });
    component.citations = [original];
    const updated = makeCitation({ id: 'edit-me', reference: 'Réf corrigée' });
    citationService.update.mockReturnValue(of(updated));

    component.startEdit(original);
    expect(component.editingId()).toBe('edit-me');
    component.editReference = 'Réf corrigée';
    component.editPortee = '';
    component.saveEdit(original);

    expect(citationService.update).toHaveBeenCalledWith(CASE_FILE_ID, 'edit-me', {
      reference: 'Réf corrigée',
      portee: null,
    });
    expect(component.editingId()).toBeNull();
    expect(component.citationsSignal()[0]).toEqual(updated);
  });

  // U-9 : reference vide en édition → enregistrement impossible.
  it('U9: empty reference blocks the edit save', () => {
    const original = makeCitation();
    component.citations = [original];
    component.startEdit(original);
    component.editReference = '  ';

    expect(component.canSaveEdit()).toBe(false);
    component.saveEdit(original);
    expect(citationService.update).not.toHaveBeenCalled();
  });

  // U-10 : suppression confirmée → DELETE, citation retirée.
  it('U10: confirmed remove deletes the citation', () => {
    const citation = makeCitation({ id: 'to-delete' });
    component.citations = [citation];
    dialog.open.mockReturnValue({ afterClosed: () => of(true) } as any);
    citationService.delete.mockReturnValue(of(undefined));

    component.remove(citation);

    expect(dialog.open).toHaveBeenCalled();
    expect(citationService.delete).toHaveBeenCalledWith(CASE_FILE_ID, 'to-delete');
    expect(component.citationsSignal()).toEqual([]);
  });

  // U-11 : suppression annulée dans le dialogue → pas de DELETE.
  it('U11: cancelled confirmation does not delete', () => {
    const citation = makeCitation({ id: 'kept' });
    component.citations = [citation];
    dialog.open.mockReturnValue({ afterClosed: () => of(false) } as any);

    component.remove(citation);

    expect(citationService.delete).not.toHaveBeenCalled();
    expect(component.citationsSignal()).toEqual([citation]);
  });

  // U-12 : erreur API à la suppression → snackbar, citation conservée.
  it('U12: API error on delete shows a snackbar and keeps the citation', () => {
    const citation = makeCitation({ id: 'err' });
    component.citations = [citation];
    dialog.open.mockReturnValue({ afterClosed: () => of(true) } as any);
    citationService.delete.mockReturnValue(throwError(() => new Error('boom')));

    component.remove(citation);

    expect(snackBar.open).toHaveBeenCalled();
    expect(component.citationsSignal()).toEqual([citation]);
    expect(component.saving()).toBe(false);
  });

  // U-13 : `changed` émis après un ajout réussi.
  it('U13: emits changed after a successful add', () => {
    component.citations = [];
    citationService.create.mockReturnValue(of(makeCitation()));
    const emitSpy = jest.fn();
    component.changed.subscribe(emitSpy);

    component.newReference = 'Réf';
    component.add();

    expect(emitSpy).toHaveBeenCalled();
  });
});
