import { TestBed } from '@angular/core/testing';
import { ClientErrorService } from './client-error.service';
import { GlobalErrorHandler } from './global-error-handler';

describe('GlobalErrorHandler', () => {
  let handler: GlobalErrorHandler;
  let clientErrorService: jasmine.SpyObj<ClientErrorService> | { report: jest.Mock };

  beforeEach(() => {
    clientErrorService = { report: jest.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: ClientErrorService, useValue: clientErrorService },
        GlobalErrorHandler
      ]
    });
    handler = TestBed.inject(GlobalErrorHandler);
  });

  // H-01 : handleError(new Error('boom')) → report appelé 1x
  it('reports a thrown Error via ClientErrorService', () => {
    const err = new Error('boom');
    // Suppress console.error pour éviter de polluer la sortie test
    jest.spyOn(console, 'error').mockImplementation(() => undefined);

    handler.handleError(err);

    expect((clientErrorService as { report: jest.Mock }).report).toHaveBeenCalledTimes(1);
    const payload = (clientErrorService as { report: jest.Mock }).report.mock.calls[0][0];
    expect(payload.message).toBe('boom');
    expect(payload.stack).toBeDefined();
    expect(payload.timestamp).toBeDefined();
  });

  // H-02 : handleError(string) → report avec message = string
  it('handles non-Error values gracefully', () => {
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
    handler.handleError('plain string error');
    const payload = (clientErrorService as { report: jest.Mock }).report.mock.calls[0][0];
    expect(payload.message).toBe('plain string error');
  });

  // H-03 : super.handleError appelé (console.error en dev)
  it('still calls super.handleError to preserve dev console output', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    handler.handleError(new Error('dev visible'));
    expect(consoleSpy).toHaveBeenCalled();
  });

  // H-04 : si ClientErrorService throw, handleError ne propage pas
  it('does not propagate exceptions thrown by ClientErrorService', () => {
    jest.spyOn(console, 'error').mockImplementation(() => undefined);
    (clientErrorService as { report: jest.Mock }).report.mockImplementation(() => {
      throw new Error('service down');
    });
    expect(() => handler.handleError(new Error('boom'))).not.toThrow();
  });
});
