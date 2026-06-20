import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ProblemDetail } from '../models/problem-detail';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      notifications.error(buildMessage(error));
      return throwError(() => error);
    }),
  );
};

function buildMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Sem conexão com o servidor. Verifique sua rede e tente de novo.';
  }

  const problem = error.error as ProblemDetail | undefined;

  if (problem?.errors) {
    const fields = Object.values(problem.errors).join(' • ');
    if (fields) return fields;
  }

  if (problem?.detail) {
    const ref = problem.correlationId ? ` (ref: ${problem.correlationId})` : '';
    return problem.detail + ref;
  }

  return 'Não foi possível concluir a operação. Tente novamente.';
}