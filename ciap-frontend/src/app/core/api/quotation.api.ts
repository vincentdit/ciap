import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, CreateQuotationRequest, QuotationResponse, ContractResponse
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class QuotationApi {
  private readonly base = `${environment.apiBaseUrl}/quotations`;

  constructor(private http: HttpClient) {}

  create(body: CreateQuotationRequest): Observable<ApiResponse<QuotationResponse>> {
    return this.http.post<ApiResponse<QuotationResponse>>(this.base, body);
  }

  listForRequest(requestId: number): Observable<ApiResponse<QuotationResponse[]>> {
    return this.http.get<ApiResponse<QuotationResponse[]>>(`${this.base}/request/${requestId}`);
  }

  send(id: number): Observable<ApiResponse<QuotationResponse>> {
    return this.http.post<ApiResponse<QuotationResponse>>(`${this.base}/${id}/send`, {});
  }

  accept(id: number): Observable<ApiResponse<ContractResponse>> {
    return this.http.post<ApiResponse<ContractResponse>>(`${this.base}/${id}/accept`, {});
  }
}
