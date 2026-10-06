import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('mv_token');
  const openAuth = /\/auth\/(login|register|forgot)\b/.test(req.url);
  if (!token || openAuth || !req.url.includes('/api/')) return next(req);
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
