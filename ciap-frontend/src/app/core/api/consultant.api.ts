import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, ConsultantResponse } from '../models/api.models';

export interface CreateConsultantRequest {
  fullName: string;
  category: string;
  skills?: string;
  yearsExperience?: number;
  billingRatePerHour?: number;
}

@Injectable({ providedIn: 'root' })
export class ConsultantApi {
  private readonly base = `${environment.apiBaseUrl}/consultants`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 20, q?: string): Observable<ApiResponse<PageResponse<ConsultantResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q?.trim()) params = params.set('q', q.trim());
    return this.http.get<ApiResponse<PageResponse<ConsultantResponse>>>(this.base, { params });
  }

  get(id: number): Observable<ApiResponse<ConsultantResponse>> {
    return this.http.get<ApiResponse<ConsultantResponse>>(`${this.base}/${id}`);
  }

  create(body: CreateConsultantRequest): Observable<ApiResponse<ConsultantResponse>> {
    return this.http.post<ApiResponse<ConsultantResponse>>(this.base, body);
  }
}
