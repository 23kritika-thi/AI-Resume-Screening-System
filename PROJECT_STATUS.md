# AI-Based Resume Screening and Candidate Ranking System

## Current status

**Phase 8 is complete and verified.** Phases 1–7 remain complete; no additional phase or feature work was started.

| Phase | Status | Scope |
|---|---|---|
| 1. Environment and requirements | Complete | Java, Maven, MySQL |
| 2. Database foundation | Complete | Existing MySQL tables, JDBC services and models |
| 3. Desktop UI foundation | Complete | Java Swing, FlatLaf, dashboard and navigation |
| 4. Recruiter authentication | Complete | Registration, login, password hashing/recovery and logout |
| 5. Jobs, resumes and screening | Complete | Job criteria, validated PDF upload, text extraction, candidate profiles, explainable matching and ranking |
| 6. Further screening/review enhancements | Pending | Not started in this task |
| 7. Reporting and exports | Complete | MySQL-backed dashboard, job-wise and screening analytics, charts, PDF and CSV export |
| 8. Final testing and delivery | Complete | Final code review, CSV safety fix, credential packaging safeguard, documentation, full Maven verification |

## Phase 5 functionality

- Recruiters can create job descriptions with required and preferred skills and minimum experience.
- The resume workspace selects files through a Swing file chooser and accepts only `.pdf` files with a PDF signature.
- PDFBox extracts selectable text first. If it finds no meaningful text, PDFBox renders every page and the bundled Tess4J/Tesseract English model performs OCR. Blank, unreadable, corrupted, and still-textless PDFs are rejected before file or database storage.
- Accepted files are stored under `storage/resumes/` using a SHA-256 filename; original file names remain metadata. Duplicate content is rejected. Extracted text and candidate contact fields are persisted using prepared statements.
- Candidate name, email, phone, skill categories, education, work experience, internships, projects, certifications, and explicitly stated years of experience are extracted conservatively. Recruiters can enter a name when it cannot be inferred.
- Resumes can be screened against a selected recruiter-owned job. Results persist in `screening_results`, are ranked by score, and display required skills (50 points), preferred skills (20), relevant experience (15), and TF-IDF/cosine text similarity (15). Matched and missing skills and component explanations are available in the UI.
- Candidate results can be sorted and filtered. The score is a decision-support signal; there is no automated hiring decision.
- Sensitive attribute-labeled lines and numeric tokens are excluded from matching/similarity, and experience scoring requires explicit years-of-experience wording. Sensitive attributes are not criteria.
- Dashboard metrics and job/screening lists are sourced from MySQL rather than sample statistics or candidate records.

## Phase 5 bug fixes (2026-11-13)

The five reported Phase 5 issues have been addressed and verified:

- Job creation now uses a compact form beside a resizable jobs table, with bounded column widths and usable horizontal/vertical scrolling.
- Candidates now shows stored candidate records and contact details; Resume Screening remains the separate upload, job-selection and matching workflow.
- Resume parsing applies resume-content heuristics before upload persistence, rejecting the tested project synopsis without creating a candidate, resume row, or stored file.
- Email detection normalizes common PDF text spacing and `[at]`/`[dot]` obfuscation; candidate and uploaded-resume views display the stored email.
- Uploaded-resume columns use fixed widths with horizontal and vertical scrolling so all columns are accessible.

No database schema changes were made for these fixes. Resume-content identification is heuristic and cannot guarantee rejection of every unrelated document.

## Additional corrections (2026-11-13)

- Candidates supports confirmed deletion of the selected record. MySQL deletes the candidate and cascades its resumes and screening results; only file paths no longer referenced by any resume row are eligible for cleanup. File cleanup is restricted to the managed resume directory, and canceled confirmation makes no changes.
- Email extraction now handles case-insensitive Unicode-aware addresses, plus-addressing/subdomains, PDF soft hyphens, and common whitespace or line-break placement around email punctuation. Extracted emails are verified through MySQL persistence and both candidate/resume table views.
- Resume Details wraps long text in its own scroll pane with vertical scrolling, keeping the details viewport constrained within the screening layout.

No schema changes were required.

## Scanned PDF OCR (2026-11-13)

- Added a Tess4J OCR fallback after PDFBox text extraction finds fewer than 20 meaningful characters. Normal text PDFs continue through the existing PDFBox path without OCR.
- Bundled the Tesseract English `tessdata_fast` model in application resources; no separately installed Tesseract executable or network download is required.
- OCR output goes through the existing resume-content validator, profile extraction, upload storage, candidate persistence, and screening workflow. The tested scanned resume extracts name, email, phone, education, and skills.
- Blank scans and OCR output without meaningful content are rejected before candidate or file persistence. Scanned project documents continue to be subject to the existing resume-content checks.

OCR accuracy depends on scan clarity, page orientation, and English-language text. Other OCR languages are not bundled.

## Phase 7: Reports and Analytics

- The Reports navigation opens a dedicated, scrollable analytics view, separate from Candidates and Resume Screening.
- Four summary cards show total jobs, candidate profiles, uploaded resumes, and distinct screened candidates. Counts use existing `DatabaseService` queries and stored screening results.
- Job-wise reporting shows each recruiter-owned job, distinct applicants represented by stored screening results, and average latest screening score.
- Screening results show ranking, candidate, total score, weighted score components, and screened time. Two Java2D charts show applicants by job and score buckets.
- Recruiters can refresh the data and export the current snapshot to CSV or PDF. CSV includes dashboard totals and job/screening rows; PDF includes dashboard totals, job summaries, rankings, score breakdowns, and matched/missing skills.
- Loading, empty, database-error, and export-error states are surfaced in the Reports view. Database access uses the existing schema and prepared statements; no schema changes were needed.

Report totals for candidates and resumes follow the existing schema/query semantics: candidate and resume counts are shared across recruiter accounts because those records do not carry recruiter ownership. Job and screening rows are limited to the logged-in recruiter's jobs.

## Fictional demo dataset

- Reports & Analytics provides an explicit, confirmation-protected **Load Demo Data** action for the currently authenticated recruiter, plus a separate demo-only cleanup action.
- The loader creates 8 marked job descriptions and 18 fictional candidate resumes using reserved `.example` email addresses and fictional 202-555-01xx phone numbers.
- Resume PDFs are generated temporarily and then sent through the existing PDFBox parser, resume validator, profile extractor, upload service, MySQL persistence, and weighted screening service. Candidate data is not inserted as fabricated extracted text.
- Screening associations are role-specific; the integration test produced 32 scored candidate/job results. The existing Reports view and CSV/PDF exporters use the persisted records.
- Repeated loads reuse tagged jobs/resumes and do not duplicate screening rows. Existing records are not overwritten. Cleanup deletes tagged jobs, removes demo-only candidate records, and retains candidates or files still referenced outside the demo data.
- No schema changes were made. UI loading uses the signed-in recruiter; no credentials are added or displayed.
- Fixed stale post-load views: Dashboard cards, job totals, and recent rankings now reload from MySQL; Resume Screening reloads jobs, candidates, and resumes; Candidate Ranking selects a demo job after load and reloads that job's persisted ranking. Changing the selected job also retrieves only that job's saved ranking.

## Phase 8: Final review and delivery

- Reviewed authentication/recovery, job and candidate persistence, PDFBox/OCR processing, ranking, reporting/export, MySQL resource handling, and project documentation.
- CSV export now prefixes spreadsheet formula-like values from job/candidate data so opening exported reports in spreadsheet software does not execute those values as formulas. A regression test covers this behavior.
- Moved the machine-local `database.properties` out of Maven resources to the supported project-root external configuration location. Added both local configuration paths to `.gitignore`, preventing database credentials from being included in source or the packaged JAR. The file contents were preserved; no credential values are documented.
- Added this README with prerequisites, setup, environment variables, build/run commands, module descriptions, and limitations.
- The Settings page replaces the former placeholder with a functional recruiter profile form. The recruiter's display name is validated and persisted in `recruiter_accounts`; after save it updates the active session, application header, and dashboard greeting. Reset discards unsaved edits.
- The signed-in email is displayed read-only. Database host, database name, and username are displayed read-only without exposing the password. A background connection check reports success or actionable failure status.
- Database connection credentials remain configured through the existing environment/local-properties mechanism; no settings screen stores secrets or modifies database configuration.
- Final `mvn clean package`: 74 tests passed, 0 failures, 0 errors, 0 skipped. The MySQL-backed integration tests passed, including recruiter profile persistence and the demo PDF upload/screening/report/export/idempotency/cleanup workflow. Database connection checks also passed for both a reachable server and an unreachable endpoint.
- Manual GUI workflows and live SMTP delivery were not separately exercised during Phase 8; see [TEST_REPORT.md](./TEST_REPORT.md).

## Database changes

No tables were added or unrelated tables changed. With approval, five nullable/defaulted columns were added to the existing tables:

- `jobs.preferred_skills`
- `screening_results.required_skills_score`
- `screening_results.preferred_skills_score`
- `screening_results.experience_score`
- `screening_results.text_similarity_score`

The application applies these additive migrations on GUI startup. To initialize a fresh schema or apply them explicitly, run:

```powershell
mvn exec:java '-Dexec.args=--setup-db'
```

## Verification

The final Phase 8 clean Maven package/test run passed on 2026-11-14:

- 74 tests across 14 test suites
- 74 passed; 0 failures, 0 errors, 0 skipped
- Live MySQL connectivity reported `CONNECTED`
- Database integration covered job creation, candidate/resume persistence and deletion, shared resume file references, email retrieval, weighted screening results, and report snapshot job/applicant data
- OCR tests covered PDFBox-first behavior, scanned resume parsing/profile extraction, low-quality text-layer fallback, upload persistence, and blank scan rejection
- Reports tests covered populated/empty analytics states, chart distributions, CSV escaping and summary rows, readable PDF generation, invalid export destinations, and MySQL report retrieval
- CSV regression coverage confirms formula-like candidate values are neutralized for spreadsheet consumers
- Regression UI checks covered candidate deletion confirmation/cancellation, candidate directory separation, email display, and independent Resume Details scrolling
- Demo dataset integration verified 8 jobs, 18 candidates/resumes, 32 screening results, CSV/PDF export, repeat-load idempotency, and safe removal. Test data was cleaned up; use the authenticated Reports screen to persist the demo dataset for a demonstration.

See [TEST_REPORT.md](./TEST_REPORT.md) for per-suite results.

## Limitations

- OCR currently supports English only; low-resolution, rotated, handwritten, or poor-contrast scans may not extract reliably.
- Name and section extraction is heuristic. Recruiters should verify extracted details before using them.
- Scoring is a transparent Java heuristic using skill coverage, explicit experience text, and TF-IDF/cosine similarity; it is not a trained model or a hiring recommendation.
- The existing schema has no recruiter ownership field on candidates/resumes. The current candidate/resume directory is therefore shared across recruiter accounts; job and screening records remain associated with the owning recruiter/job. Tenant-level resume isolation requires a separately approved schema change.
- SMTP recovery still requires the environment variables documented in the Phase 4 implementation; real email delivery is outside Phase 5 validation.
- Report charts are displayed in the application; exported PDF/CSV files contain tabular summaries and screening details rather than embedded charts. The PDF uses a built-in font and substitutes unsupported non-ASCII glyphs.
- The packaged JAR intentionally does not contain local database credentials. Configure `DB_*` environment variables or provide a local, ignored project-root `database.properties`.
