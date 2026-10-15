import { HttpEventType, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { successMessage } from '../shared/success-messages';
import { ToastService } from '../shared/toast.service';

/** Confirms every completed change (added, updated, deleted, saved) with a short success message. */
export const successInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const message = successMessage(req.method, req.url);
  if (!message) return next(req);
  return next(req).pipe(
    tap((event) => {
      if (event.type === HttpEventType.Response && event.status >= 200 && event.status < 300) toast.success(message);
    }),
  );
};
