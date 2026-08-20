import { Component, inject, signal, input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentApi } from '../../core/api/document.api';
import {
  DocumentResponse, DocumentCategory, DOCUMENT_CATEGORIES
} from '../../core/models/api.models';

@Component({
  selector: 'app-documents-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="card panel">
      <h3>Documents</h3>

      @if (msg()) { <div class="alert alert-ok">{{ msg() }}</div> }
      @if (err()) { <div class="alert alert-error">{{ err() }}</div> }

      @if (loading()) {
        <p class="muted small">Loading documents…</p>
      } @else if (docs().length === 0) {
        <p class="muted small">No documents attached yet.</p>
      } @else {
        <ul class="doc-list">
          @for (d of docs(); track d.id) {
            <li>
              <div class="d-info">
                <span class="d-name">{{ d.originalFilename }}</span>
                <span class="pill pill-neutral">{{ catLabel(d.category) }}</span>
                <span class="d-meta">{{ sizeLabel(d.sizeBytes) }}</span>
              </div>
              <button class="btn btn-ghost sm" [disabled]="downloading() === d.id" (click)="download(d)">
                @if (downloading() === d.id) { Downloading… } @else { Download }
              </button>
            </li>
          }
        </ul>
      }

      <div class="upload">
        <h4>Upload a document</h4>
        <div class="up-row">
          <select [(ngModel)]="category">
            @for (c of categories; track c.value) { <option [value]="c.value">{{ c.label }}</option> }
          </select>
          <input type="text" [(ngModel)]="description" placeholder="Description (optional)" />
        </div>
        <div class="up-row">
          <input type="file" (change)="onFile($event)" />
          <button class="btn btn-primary sm" [disabled]="uploading() || !selectedFile" (click)="upload()">
            @if (uploading()) { <span class="spinner"></span> Uploading… } @else { Upload }
          </button>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .panel { padding: 1.3rem 1.5rem; margin-bottom: 1.2rem; }
    .panel h3 { font-size: 1.1rem; margin-bottom: 0.9rem; }
    .panel h4 { font-size: 0.82rem; text-transform: uppercase; letter-spacing: 0.03em; color: var(--ink-faint); margin: 0 0 0.6rem; }
    .small { font-size: 0.88rem; }
    .doc-list { list-style: none; margin: 0 0 1rem; padding: 0; }
    .doc-list li { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: 0.6rem 0; border-bottom: 1px solid var(--line); }
    .doc-list li:last-child { border-bottom: none; }
    .d-info { display: flex; align-items: center; gap: 0.7rem; flex-wrap: wrap; }
    .d-name { font-weight: 500; font-size: 0.92rem; }
    .d-meta { font-size: 0.8rem; color: var(--ink-faint); }
    .btn.sm { padding: 0.45rem 0.8rem; font-size: 0.85rem; }
    .upload { border-top: 1px solid var(--line); padding-top: 1rem; }
    .up-row { display: flex; gap: 0.5rem; margin-bottom: 0.6rem; flex-wrap: wrap; align-items: center; }
    .up-row select, .up-row input[type=text] { padding: 0.5rem 0.6rem; border: 1px solid var(--line-2); border-radius: var(--radius-sm); font-family: var(--font-body); font-size: 0.88rem; }
    .up-row input[type=text] { flex: 1; min-width: 160px; }
  `]
})
export class DocumentsPanelComponent implements OnInit {
  requestId = input.required<number>();

  private api = inject(DocumentApi);

  protected docs = signal<DocumentResponse[]>([]);
  protected loading = signal(true);
  protected uploading = signal(false);
  protected downloading = signal<number | null>(null);
  protected msg = signal<string | null>(null);
  protected err = signal<string | null>(null);

  protected categories = DOCUMENT_CATEGORIES;
  protected category: DocumentCategory = 'OTHER';
  protected description = '';
  protected selectedFile: File | null = null;

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.api.listForRequest(this.requestId()).subscribe({
      next: (res) => { this.docs.set(res.data); this.loading.set(false); },
      error: (e) => {
        this.loading.set(false);
        if (e?.status !== 403) this.err.set('Could not load documents.');
      }
    });
  }

  onFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length ? input.files[0] : null;
  }

  upload(): void {
    if (!this.selectedFile) return;
    this.uploading.set(true);
    this.clear();
    this.api.upload(this.requestId(), this.selectedFile, this.category, this.description || undefined)
      .subscribe({
        next: () => {
          this.uploading.set(false);
          this.msg.set('Document uploaded.');
          this.description = '';
          this.selectedFile = null;
          this.load();
        },
        error: (e) => {
          this.uploading.set(false);
          if (e?.status === 413) this.err.set('File is too large (max 25 MB).');
          else if (e?.status === 403) this.err.set('You are not permitted to upload here.');
          else this.err.set('Upload failed. Please try again.');
        }
      });
  }

  download(d: DocumentResponse): void {
    this.downloading.set(d.id);
    this.clear();
    this.api.download(d.id).subscribe({
      next: (blob) => {
        this.downloading.set(null);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = d.originalFilename;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.downloading.set(null); this.err.set('Download failed.'); }
    });
  }

  catLabel(c: DocumentCategory): string {
    return this.categories.find(x => x.value === c)?.label ?? c;
  }

  sizeLabel(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  private clear(): void { this.msg.set(null); this.err.set(null); }
}
