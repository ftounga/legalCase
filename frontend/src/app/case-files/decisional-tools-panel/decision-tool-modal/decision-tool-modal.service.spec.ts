import { Component, ViewContainerRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DecisionToolModalArgs, DecisionToolModalService } from './decision-tool-modal.service';
import { DecisionToolModalComponent } from './decision-tool-modal.component';

@Component({ selector: 'app-stub', standalone: true, template: '' })
class StubComponent {}

describe('DecisionToolModalService', () => {
  let service: DecisionToolModalService;
  let dialogSpy: jest.SpyInstance;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatDialogModule],
    });
    service = TestBed.inject(DecisionToolModalService);
    const dialog = TestBed.inject(MatDialog);
    dialogSpy = jest.spyOn(dialog, 'open').mockReturnValue({
      afterClosed: () => ({ subscribe: () => undefined }),
      close: jest.fn(),
    } as unknown as ReturnType<MatDialog['open']>);
  });

  it('open appelle MatDialog.open avec le composant modal et la config attendue', () => {
    const args: DecisionToolModalArgs = {
      toolId: 'F-IM-05',
      title: 'TITRE',
      icon: 'badge',
      component: StubComponent,
      inputs: { x: 1 },
    };

    service.open(args);

    expect(dialogSpy).toHaveBeenCalledTimes(1);
    const [componentArg, configArg] = dialogSpy.mock.calls[0];
    expect(componentArg).toBe(DecisionToolModalComponent);
    expect(configArg.width).toBe('90vw');
    expect(configArg.maxWidth).toBe('1200px');
    expect(configArg.height).toBe('90vh');
    expect(configArg.maxHeight).toBe('900px');
    expect(configArg.panelClass).toBe('decision-tool-modal-panel');
    expect(configArg.autoFocus).toBe('first-tabbable');
    expect(configArg.restoreFocus).toBe(true);
    expect(configArg.data).toBe(args);
  });

  it('open retourne le MatDialogRef obtenu de MatDialog.open', () => {
    const args: DecisionToolModalArgs = {
      toolId: 'X',
      title: 'X',
      icon: 'x',
      component: StubComponent,
      inputs: {},
    };
    const ref = service.open(args);
    expect(ref).toBeDefined();
  });

  describe('SF-177-14 — propagation viewContainerRef', () => {
    it('T-01: open propage viewContainerRef à MatDialog.open quand fourni', () => {
      const mockVcr = { element: { nativeElement: {} } } as unknown as ViewContainerRef;
      const args: DecisionToolModalArgs = {
        toolId: 'F-IM-05',
        title: 'TITRE',
        icon: 'badge',
        component: StubComponent,
        inputs: {},
        viewContainerRef: mockVcr,
      };

      service.open(args);

      const [, configArg] = dialogSpy.mock.calls[0];
      expect(configArg.viewContainerRef).toBe(mockVcr);
    });

    it('open sans viewContainerRef passe undefined (rétrocompatible)', () => {
      const args: DecisionToolModalArgs = {
        toolId: 'X',
        title: 'X',
        icon: 'x',
        component: StubComponent,
        inputs: {},
      };

      service.open(args);

      const [, configArg] = dialogSpy.mock.calls[0];
      expect(configArg.viewContainerRef).toBeUndefined();
    });
  });
});
