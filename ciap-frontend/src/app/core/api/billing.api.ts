import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, PageResponse, PaymentResponse, GepgCallbackRequest, FinancePayment, FinanceSummary, PaymentStatus
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class BillingApi {
  private readonly base = `${environment.apiBaseUrl}/payments`;

  constructor(private http: HttpClient) {}

  /** Issue a GePG control number for a QUOTED request. */
  issue(requestId: number): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(`${this.base}/issue/${requestId}`, {});
  }

  /** Manual finance confirmation. A reason is required. This does not call GePG. */
  gepgCallback(body: GepgCallbackRequest): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(`${this.base}/gepg-callback`, body);
  }

  flag(id: number, reason: string): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(`${this.base}/${id}/flag`, { reason });
  }

  summary(): Observable<ApiResponse<FinanceSummary>> {
    return this.http.get<ApiResponse<FinanceSummary>>(`${this.base}/summary`);
  }

  search(filters: { status?: PaymentStatus | ''; q?: string; from?: string; to?: string; page?: number; mismatches?: boolean }): Observable<ApiResponse<PageResponse<FinancePayment>>> {
    let params = new HttpParams().set('page', filters.page ?? 0).set('size', 10);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.q?.trim()) params = params.set('q', filters.q.trim());
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    if (filters.mismatches) params = params.set('mismatches', 'true');
    return this.http.get<ApiResponse<PageResponse<FinancePayment>>>(this.base, { params });
  }

  listForRequest(requestId: number): Observable<ApiResponse<PaymentResponse[]>> {
    return this.http.get<ApiResponse<PaymentResponse[]>>(`${this.base}/request/${requestId}`);
  }
}
