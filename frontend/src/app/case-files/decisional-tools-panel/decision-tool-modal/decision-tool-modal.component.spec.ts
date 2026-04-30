import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { DecisionToolModalComponent } from './decision-tool-modal.component';
import { DecisionToolModalArgs } from './decision-tool-modal.service';

@Component({
  selector: 'app-test-stub',
  standalone: true,
  template: '<div class="stub-rendered" [attr.data-foo]="foo"></div>',
})
class StubToolComponent {
  @Input() foo: string | null = null;
}

describe('DecisionToolModalComponent', () => {
  let component: DecisionToolModalComponent;
  let fixture: ComponentFixture<DecisionToolModalComponent>;
  let dialogRefMock: { close: jest.Mock };
  let snackBarMock: { open: jest.Mock };

  function setup(data: Partial<DecisionToolModalArgs>) {
    const fullData: DecisionToolModalArgs = {
      toolId: 'F-IM-05-titre-sejour',
      title: 'TITRE DE SÉJOUR RECOMMANDÉ',
      icon: 'badge',
      component: StubToolComponent,
      inputs: { foo: 'bar' },
      ...data,
    };

    dialogRefMock = { close: jest.fn() };
    snackBarMock = { open: jest.fn() };

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, DecisionToolModalComponent, StubToolComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: fullData },
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MatSnackBar, useValue: snackBarMock },
      ],
    });

    fixture = TestBed.createComponent(DecisionToolModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('rendu', () => {
    it('rend titre + icône depuis MAT_DIALOG_DATA', () => {
      setup({});
      const title = fixture.debugElement.query(By.css('.modal-header__title'));
      const icon = fixture.debugElement.query(By.css('.modal-header__icon'));
      expect(title.nativeElement.textContent.trim()).toBe('TITRE DE SÉJOUR RECOMMANDÉ');
      expect(icon.nativeElement.textContent.trim()).toBe('badge');
    });

    it('instancie le composant outil via componentOutlet avec ses inputs', () => {
      setup({});
      const stub = fixture.debugElement.query(By.css('.stub-rendered'));
      expect(stub).not.toBeNull();
      expect(stub.nativeElement.getAttribute('data-foo')).toBe('bar');
    });

    it('expose data-tool-id sur le header pour tests E2E / tracking', () => {
      setup({});
      const header = fixture.debugElement.query(By.css('.modal-header'));
      expect(header.nativeElement.getAttribute('data-tool-id')).toBe('F-IM-05-titre-sejour');
    });
  });

  describe('bouton Enregistrer', () => {
    it('absent quand onSave undefined', () => {
      setup({ onSave: undefined });
      const saveBtn = fixture.debugElement.query(By.css('button[color="primary"]'));
      expect(saveBtn).toBeNull();
    });

    it('présent quand onSave défini', () => {
      setup({ onSave: () => true });
      const saveBtn = fixture.debugElement.query(By.css('button[color="primary"]'));
      expect(saveBtn).not.toBeNull();
    });
  });

  describe('Annuler', () => {
    it('Click Annuler ferme le dialog sans argument', () => {
      setup({ onSave: () => true });
      const cancel = fixture.debugElement.queryAll(By.css('button')).find((b) =>
        (b.nativeElement.textContent ?? '').includes('Annuler'),
      );
      cancel!.nativeElement.click();
      expect(dialogRefMock.close).toHaveBeenCalledWith();
    });

    it('Click bouton fermer header ferme le dialog sans argument', () => {
      setup({ onSave: () => true });
      const close = fixture.debugElement.query(By.css('.modal-header__close'));
      close.nativeElement.click();
      expect(dialogRefMock.close).toHaveBeenCalledWith();
    });
  });

  describe('Enregistrer — comportements', () => {
    function clickSave() {
      const saveBtn = fixture.debugElement.query(By.css('button[color="primary"]'));
      saveBtn.nativeElement.click();
    }

    it('onSave retourne true synchrone : ferme avec saved', async () => {
      setup({ onSave: () => true });
      clickSave();
      await Promise.resolve();
      expect(dialogRefMock.close).toHaveBeenCalledWith('saved');
    });

    it('onSave retourne false synchrone : ne ferme pas, bouton ré-activé', async () => {
      setup({ onSave: () => false });
      clickSave();
      await Promise.resolve();
      expect(dialogRefMock.close).not.toHaveBeenCalled();
      expect(component['saving']()).toBe(false);
    });

    it('onSave retourne Promise<true> : ferme avec saved après résolution', async () => {
      setup({ onSave: () => Promise.resolve(true) });
      clickSave();
      await fixture.whenStable();
      expect(dialogRefMock.close).toHaveBeenCalledWith('saved');
    });

    it('onSave retourne Promise<false> : ne ferme pas, bouton ré-activé', async () => {
      setup({ onSave: () => Promise.resolve(false) });
      clickSave();
      await fixture.whenStable();
      expect(dialogRefMock.close).not.toHaveBeenCalled();
      expect(component['saving']()).toBe(false);
    });

    it('onSave lance exception : MatSnackBar appelé, dialog ne ferme pas', async () => {
      setup({
        onSave: () => {
          throw new Error('boom');
        },
      });
      clickSave();
      await Promise.resolve();
      expect(snackBarMock.open).toHaveBeenCalled();
      expect(dialogRefMock.close).not.toHaveBeenCalled();
      expect(component['saving']()).toBe(false);
    });

    it('onSave Promise.reject : MatSnackBar appelé, dialog ne ferme pas', async () => {
      setup({ onSave: () => Promise.reject(new Error('http')) });
      clickSave();
      await fixture.whenStable();
      expect(snackBarMock.open).toHaveBeenCalled();
      expect(dialogRefMock.close).not.toHaveBeenCalled();
    });

    it('double-click sur Enregistrer ne déclenche onSave qu\'une fois', async () => {
      const onSave = jest.fn(() => Promise.resolve(true));
      setup({ onSave });
      clickSave();
      clickSave();
      await fixture.whenStable();
      expect(onSave).toHaveBeenCalledTimes(1);
    });
  });
});
