import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, PageResponse, CreateRequestDto, RequestEvent, RequestResponse,
  EligibleAssignee, AssignmentRecord
} from '../models/api.models';

export interface RequestListQuery {
  status?: string;
  q?: string;
  type?: string;
  from?: string;
  to?: string;
  overdue?: boolean;
}

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

  events(id: number): Observable<ApiResponse<RequestEvent[]>> {
    return this.http.get<ApiResponse<RequestEvent[]>>(`${this.base}/${id}/events`);
  }

  assignees(id: number): Observable<ApiResponse<EligibleAssignee[]>> {
    return this.http.get<ApiResponse<EligibleAssignee[]>>(`${this.base}/${id}/assignees`);
  }

  assignments(id: number): Observable<ApiResponse<AssignmentRecord[]>> {
    return this.http.get<ApiResponse<AssignmentRecord[]>>(`${this.base}/${id}/assignments`);
  }

  list(page = 0, size = 20, filters?: string | RequestListQuery): Observable<ApiResponse<PageResponse<RequestResponse>>> {
    const query: RequestListQuery = typeof filters === 'string' ? { status: filters } : (filters ?? {});
    let params = new HttpParams().set('page', page).set('size', size);
    if (query.status) params = params.set('status', query.status);
    if (query.q?.trim()) params = params.set('q', query.q.trim());
    if (query.type) params = params.set('type', query.type);
    if (query.from) params = params.set('from', query.from);
    if (query.to) params = params.set('to', query.to);
    if (query.overdue) params = params.set('overdue', 'true');
    return this.http.get<ApiResponse<PageResponse<RequestResponse>>>(this.base, { params });
  }

  route(id: number, departmentId: number): Observable<ApiResponse<RequestResponse>> {
    return this.http.post<ApiResponse<RequestResponse>>(`${this.base}/${id}/route`, { departmentId });
  }
}
