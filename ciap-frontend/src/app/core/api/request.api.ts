import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, PageResponse, CreateRequestDto, RequestResponse
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class RequestApi {
  private readonly base = `${environment.apiBaseUrl}/consultancy-requests`;

  constructor(private http: HttpClient) {}

  submit(body: CreateRequestDto): Observable<ApiResponse<RequestResponse>> {
    return this.http.post<ApiResponse<RequestResponse>>(this.base, body);
  }

  get(id: number): Observable<ApiResponse<RequestResponse>> {
    return this.http.get<ApiResponse<RequestResponse>>(`${this.base}/${id}`);
  }

  list(page = 0, size = 20): Observable<ApiResponse<PageResponse<RequestResponse>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<RequestResponse>>>(this.base, { params });
  }
}
