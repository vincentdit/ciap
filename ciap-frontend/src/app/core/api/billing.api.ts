import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PaymentResponse, GepgCallbackRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class BillingApi {
  private readonly base = `${environment.apiBaseUrl}/payments`;

  constructor(private http: HttpClient) {}

  /** Issue a GePG control number for a QUOTED request. */
  issue(requestId: number): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(`${this.base}/issue/${requestId}`, {});
  }

  /** Simulate / record a GePG settlement callback (FINANCE/ADMIN). */
  gepgCallback(body: GepgCallbackRequest): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(`${this.base}/gepg-callback`, body);
  }

  listForRequest(requestId: number): Observable<ApiResponse<PaymentResponse[]>> {
    return this.http.get<ApiResponse<PaymentResponse[]>>(`${this.base}/request/${requestId}`);
  }
}
