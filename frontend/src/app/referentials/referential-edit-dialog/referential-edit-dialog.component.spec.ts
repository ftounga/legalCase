import { TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ReferentialEditDialogComponent, ReferentialEditDialogData } from './referential-edit-dialog.component';

const mockDialogRef = { close: jasmine.createSpy('close') };

function createComponent(sectionType: string, valueJson: string) {
  const data: ReferentialEditDialogData = {
    entry: { key: 'KEY', label: 'Test', valueJson, isSystem: true },
    sectionType,
  };
  TestBed.configureTestingModule({
    imports: [ReferentialEditDialogComponent],
    providers: [
      provideAnimationsAsync(),
      { provide: MatDialogRef, useValue: mockDialogRef },
      { provide: MAT_DIALOG_DATA, useValue: data },
    ],
  });
  const fixture = TestBed.createComponent(ReferentialEditDialogComponent);
  fixture.detectChanges();
  return fixture.componentInstance;
}

describe('ReferentialEditDialogComponent — formulaires typés', () => {
  beforeEach(() => mockDialogRef.close.calls.reset());

  // EDT-01 : LITIGATION_TYPE sérialise correctement
  it('EDT-01 : LITIGATION_TYPE → sérialise {years, article}', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    expect(c.form.get('litigYears')?.value).toBe(3);
    expect(c.form.get('litigArticle')?.value).toBe('Art. L3245-1');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({ years: 3, article: 'Art. L3245-1' });
  });

  // EDT-02 : LITIGATION_TYPE invalide si années hors borne
  it('EDT-02 : LITIGATION_TYPE → invalide si années = 0', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    c.form.get('litigYears')?.setValue(0);
    expect(c.form.get('litigYears')?.hasError('min')).toBe(true);
    expect(c.form.invalid).toBe(true);
  });

  it('EDT-02b : LITIGATION_TYPE → invalide si années = 31', () => {
    const c = createComponent('LITIGATION_TYPE', '{"years":3,"article":"Art. L3245-1"}');
    c.form.get('litigYears')?.setValue(31);
    expect(c.form.get('litigYears')?.hasError('max')).toBe(true);
  });

  // EDT-03 : BAREME_MACRON toggle true → {"supported":true}
  it('EDT-03 : BAREME_MACRON → toggle true sérialise {"supported":true}', () => {
    const c = createComponent('BAREME_MACRON', '{"supported":true}');
    expect(c.form.get('baremeSupported')?.value).toBe(true);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({ supported: true });
  });

  it('EDT-03b : BAREME_MACRON → toggle false sérialise {"supported":false}', () => {
    const c = createComponent('BAREME_MACRON', '{"supported":false}');
    c.form.get('baremeSupported')?.setValue(false);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual({ supported: false });
  });

  // EDT-04 : PENSION_TAUX grille 5×2 → matrice correcte
  it('EDT-04 : PENSION_TAUX → valeurs pré-remplies en %, sérialise en /100', () => {
    const c = createComponent('PENSION_TAUX', '[[0.18,0.11],[0.26,0.16],[0.30,0.19],[0.33,0.21],[0.35,0.22]]');
    expect(c.form.get('pension_0_0')?.value).toBeCloseTo(18, 1);
    expect(c.form.get('pension_0_1')?.value).toBeCloseTo(11, 1);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const matrix = JSON.parse(result.valueJson);
    expect(matrix.length).toBe(5);
    expect(matrix[0][0]).toBeCloseTo(0.18, 3);
    expect(matrix[0][1]).toBeCloseTo(0.11, 3);
  });

  // EDT-05 : PENSION_TAUX invalide si cellule > 100
  it('EDT-05 : PENSION_TAUX → invalide si cellule > 100', () => {
    const c = createComponent('PENSION_TAUX', '[[0.18,0.11],[0.26,0.16],[0.30,0.19],[0.33,0.21],[0.35,0.22]]');
    c.form.get('pension_2_0')?.setValue(101);
    expect(c.form.get('pension_2_0')?.hasError('max')).toBe(true);
    expect(c.form.invalid).toBe(true);
  });

  // EDT-06 : PRESTATION_COEFF sérialise correctement
  it('EDT-06 : PRESTATION_COEFF → sérialise {coeff, dureeReferenceAns}', () => {
    const c = createComponent('PRESTATION_COEFF', '{"coeff":0.30,"dureeReferenceAns":8}');
    expect(c.form.get('prestCoeff')?.value).toBeCloseTo(30, 1);
    expect(c.form.get('prestDuree')?.value).toBe(8);
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    const parsed = JSON.parse(result.valueJson);
    expect(parsed.coeff).toBeCloseTo(0.30, 3);
    expect(parsed.dureeReferenceAns).toBe(8);
  });

  // EDT-07 : IMMIGRATION_PIECES newlines → string[]
  it('EDT-07 : IMMIGRATION_PIECES → newlines sérialisés en string[], lignes vides filtrées', () => {
    const c = createComponent('IMMIGRATION_PIECES', '["Passeport","Titre de séjour"]');
    expect(c.form.get('piecesText')?.value).toContain('Passeport');
    c.form.get('piecesText')?.setValue('Passeport\n\nTitre de séjour\n');
    c.submit();
    const result = mockDialogRef.close.calls.mostRecent().args[0];
    expect(JSON.parse(result.valueJson)).toEqual(['Passeport', 'Titre de séjour']);
  });

  // EDT-08 : dialog s'ouvre avec valeurs pré-remplies
  it('EDT-08 : dialog pré-remplit label depuis entry.label', () => {
    const c = createComponent('IMMIGRATION_JALONS', '[{"label":"test","offsetDays":30}]');
    expect(c.form.get('label')?.value).toBe('Test');
  });

  // Default (IMMIGRATION_JALONS) : textarea JSON conservé
  it('IMMIGRATION_JALONS → textarea JSON, validation JSON valide', () => {
    const c = createComponent('IMMIGRATION_JALONS', '[{"label":"test","offsetDays":30}]');
    expect(c.form.get('valueJson')).not.toBeNull();
    c.form.get('valueJson')?.setValue('invalid json');
    expect(c.form.get('valueJson')?.hasError('invalidJson')).toBe(true);
  });
});
