import { TestBed } from '@angular/core/testing';
import { DialogService } from './dialog.service';

describe('DialogService', () => {
  let service: DialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DialogService);
  });

  it('starts with no dialog', () => {
    expect(service.state()).toBeNull();
  });

  it('creates a delete confirmation with defaults', async () => {
    const promise = service.confirm('Delete this record?');
    expect(service.state()).toEqual({
      title: 'Confirm delete',
      message: 'Delete this record?',
      okText: 'Delete',
      cancelText: 'Cancel',
      info: false,
    });
    service.close(true);
    await expectAsync(promise).toBeResolvedTo(true);
    expect(service.state()).toBeNull();
  });

  it('supports custom confirmation text and cancel result', async () => {
    const promise = service.confirm('Continue?', {
      title: 'Question',
      okText: 'Yes',
      cancelText: 'No',
    });
    expect(service.state()?.title).toBe('Question');
    expect(service.state()?.okText).toBe('Yes');
    expect(service.state()?.cancelText).toBe('No');
    service.close(false);
    await expectAsync(promise).toBeResolvedTo(false);
  });

  it('creates an information notice', async () => {
    const promise = service.notice('Saved successfully', 'Success');
    expect(service.state()).toEqual({
      title: 'Success',
      message: 'Saved successfully',
      okText: 'OK',
      cancelText: 'Cancel',
      info: true,
    });
    service.close(true);
    await expectAsync(promise).toBeResolvedTo(true);
  });
});
