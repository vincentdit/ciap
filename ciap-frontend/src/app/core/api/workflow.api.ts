import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, RequestResponse, ReviewDto, AssignDto } from '../models/api.models';

/** Drives the consultancy-request state machine. Each method maps to a backend transition. */
@Injectable({ providedIn: 'root' })
export class WorkflowApi {
  private readonly base = `${environment.apiBaseUrl}/consultancy-requests`;

  constructor(private http: HttpClient) {}

  private post<T>(id: number, action: string, body?: unknown): Observable<ApiResponse<T>> {
    return this.http.post<ApiResponse<T>>(`${this.base}/${id}/${action}`, body ?? {});
  }

  startReview(id: number)  { return this.post<RequestResponse>(id, 'start-review'); }
  review(id: number, dto: ReviewDto) { return this.post<RequestResponse>(id, 'review', dto); }
  awaitPayment(id: number) { return this.post<RequestResponse>(id, 'await-payment'); }
  confirmPayment(id: number) { return this.post<RequestResponse>(id, 'confirm-payment'); }
  assign(id: number, dto: AssignDto) { return this.post<RequestResponse>(id, 'assign', dto); }
  startExecution(id: number) { return this.post<RequestResponse>(id, 'start-execution'); }
  submitReport(id: number) { return this.post<RequestResponse>(id, 'submit-report'); }
  close(id: number) { return this.post<RequestResponse>(id, 'close'); }
  cancel(id: number) { return this.post<RequestResponse>(id, 'cancel'); }
}
