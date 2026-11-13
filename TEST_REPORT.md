# Test Report — Phases 2–8

**Latest execution date:** 2026-11-14  
**Environment:** Windows, Java 26.0.2, Maven 3.10.0, JUnit Jupiter 5.11.3  
**Database:** MySQL 8.0.46

## Latest complete build and test results

Command: `mvn clean package`

| Test suite | Tests | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `WeightedResumeRankingEngineTest` | 3 | 3 | 0 | 0 | 0 |
| `DatabaseConfigTest` | 4 | 4 | 0 | 0 | 0 |
| `DatabaseConnectionTest` | 3 | 3 | 0 | 0 | 0 |
| `SchemaScriptTest` | 1 | 1 | 0 | 0 | 0 |
| `DemoDataServiceIntegrationTest` | 1 | 1 | 0 | 0 | 0 |
| `PasswordRecoveryServiceTest` | 5 | 5 | 0 | 0 | 0 |
| `PdfResumeParserTest` | 10 | 10 | 0 | 0 | 0 |
| `RecruiterAuthServiceTest` | 6 | 6 | 0 | 0 | 0 |
| `ReportsAnalyticsTest` | 5 | 5 | 0 | 0 | 0 |
| `ResumeDatabaseIntegrationTest` | 1 | 1 | 0 | 0 | 0 |
| `ResumeProfileExtractorTest` | 5 | 5 | 0 | 0 | 0 |
| `ResumeUploadServiceTest` | 8 | 8 | 0 | 0 | 0 |
| `UIComponentsTest` | 18 | 18 | 0 | 0 | 0 |
| `PasswordHasherTest` | 4 | 4 | 0 | 0 | 0 |
| **Total** | **74** | **74** | **0** | **0** | **0** |

The clean Maven build/package and full test suite passed (`BUILD SUCCESS`). The live MySQL connection test reported `CONNECTED`, and the unreachable-endpoint test confirmed failed connections are reported as disconnected. No test failures, errors, or skips occurred.

## Phase 8 final review and verification

- The latest `mvn clean package` run was executed on 2026-11-14; this report records its verified results.
- Exact final result: 74 tests passed, 0 failures, 0 errors, 0 skipped; 14 test suites; `BUILD SUCCESS`.
- `ResumeDatabaseIntegrationTest` passed against the configured live MySQL server. It covers recruiter registration and profile-name persistence, job creation, resume persistence, screening persistence, report snapshot retrieval, candidate deletion, and shared-file reference behavior.
- CSV/PDF exports were tested by writing files, parsing CSV contents and reopening PDF output with PDFBox. A regression test verifies spreadsheet formula-like data is neutralized in CSV.
- `DemoDataServiceIntegrationTest` created 8 namespaced demo jobs, 18 fictional candidate profiles and extracted resume PDFs, and 32 actual weighted screening results in MySQL. It verified role-specific candidates and score ordering for the Java Developer job, stored extracted profiles, Reports snapshot, CSV/PDF exports, a repeat load with no additional rows, and demo-only cleanup. The test-specific dataset and files were removed after verification; no demo records were left in the database by the test.
- Dashboard UI tests verify database payload refreshes metrics, job rows, and recent candidate summaries. Screening UI tests verify changing the selected job invokes a job-specific result refresh and that the ranking table shows the returned records.
- Settings UI tests cover profile name validation, save/reset callbacks (including reset after saving), database test callback, connection result messages, and non-secret connection target display. The MySQL integration test verifies a profile name update persists and is retrieved from `recruiter_accounts`. Database connection tests cover both the configured live server and an unreachable endpoint.
- `git diff --check` passed.
- No manual GUI walkthrough or live SMTP message delivery was performed for this final phase. Swing view state is covered by automated UI tests; live email delivery still requires manual SMTP configuration and verification.

## Phase 7 Reports and Analytics coverage

- View tests load actual report snapshots into job and screening tables, verify applicant and score chart buckets, and check the no-jobs empty state.
- CSV tests check dashboard summary data, quoting/escaping of commas and quotes in candidate/job text, complete row shape, and rejection of an invalid destination extension.
- PDF tests reopen the generated file with PDFBox and verify its dashboard totals, job summary, candidate ranking, and score are readable.
- The MySQL integration test verifies a recruiter's persisted job and screening entry are included in `getReportsSnapshot`.
- Full test and package command above includes the Phase 7 tests and all existing authentication, OCR, screening, and UI tests.

## Demo dataset verification

- The authenticated Reports view exposes separate confirmation-protected load and remove actions.
- The integration test exercises the same service using a unique temporary demo namespace to avoid touching other data. It confirms 8 jobs, 18 candidates/resumes, and 32 role-relevant screenings are created via the existing parsing, upload, MySQL, and ranking workflows.
- The repeat-load check reuses all tagged records without adding duplicates; removal only affects the test namespace and preserves an ordinary job created as a control.
- CSV and PDF exports from the populated database snapshot were reopened and checked for demo job and candidate entries. The test removes its own generated database rows and files at completion.
- No live recruiter account was used to seed a persistent demonstration dataset during this task. To populate the signed-in recruiter's database, use **Reports & Analytics → Load Demo Data** and confirm the prompt.

## Scanned PDF OCR coverage

- PDFBox text extraction remains the primary path; an injected test confirms OCR is not invoked for text-based resumes.
- A generated image-only PDF resume is OCR-processed and its candidate name, email, phone, education, and skills are extracted.
- A resume scan with unrelated selectable metadata falls back to OCR and correctly extracts the resume content.
- A scanned PDF is uploaded through the existing upload service; OCR text and extracted candidate details are persisted using the regular workflow.
- Blank scanned PDFs are rejected after OCR and do not enter upload persistence.
- The English Tesseract model is bundled at `src/main/resources/tessdata/eng.traineddata`; the Maven package contains the model.

Historical OCR implementation run: the focused OCR run passed 18 tests across `PdfResumeParserTest` and `ResumeUploadServiceTest`; that earlier full Maven run passed 65 tests. Current authoritative complete-suite totals are in the latest results table above.

## Additional correction coverage

- Candidate deletion is exercised against MySQL, including cascading resume/screening removal and a shared resume path that remains until its final database reference is deleted.
- UI tests cover confirmation acceptance, canceled deletion leaving the candidate row unchanged, and directing the deletion action to the selected candidate.
- Resume file cleanup tests remove a file under managed storage and retain an external file.
- Email extraction covers uppercase addresses, plus-addressing, subdomains, whitespace/newline variations, obfuscated forms, and PDF page-break extraction. MySQL integration verifies the extracted email persists and is returned for candidate/resume display.
- Resume Details tests verify long candidate details are displayed in a wrapped text area with independent vertical scrolling and no horizontal scrollbar.

Historical additional-corrections run: the focused regression run passed (35 tests across the five affected suites); that earlier complete suite passed 60 tests. Current authoritative totals are in the latest results table above.

## Five Phase 5 bug regression checks

- Job table usability: regression test verifies the job table and scroll behavior.
- Candidates versus Resume Screening: UI coverage verifies they are separate screens, with candidate details sourced from stored records.
- Resume-only content validation: parser tests reject the project synopsis; upload-service test confirms rejection happens before storage or database writes. Validation is heuristic.
- Email extraction and persistence: extraction tests cover whitespace/obfuscated email forms; MySQL integration verifies the email is returned in candidate/resume data.
- Uploaded-resume navigation: UI tests verify email is present and the uploaded-resume table supports fixed-width columns with horizontal/vertical scrolling.

Historical five-bug regression run: the focused regression passed (31 tests across the five affected suites); that earlier complete suite passed 56 tests. Current authoritative totals are in the latest results table above.

## Phase 5 coverage

- Valid multi-page PDF text extraction and normalization.
- JPG, JPEG, PNG, DOC, DOCX and other unsupported extension rejection; renamed non-PDF content rejection.
- Empty, corrupted, unreadable, blank scanned and no-meaningful-text PDF rejection; scanned-resume OCR fallback and existing text-PDF extraction.
- Upload storage behavior: valid file persisted; invalid/duplicate files not stored; file removed after a database persistence failure.
- Candidate name/contact, skill categories, education, experience, internship, project and certification extraction; missing data is not filled with fabricated values.
- Sensitive-field exclusion from ranking, required/preferred skill match breakdowns, weighted experience/text similarity, deterministic rank ordering and score explanations.
- Live MySQL integration for additive schema migration, job persistence, candidate/resume persistence and screening result/component-score persistence. Temporary test records are removed.
- Swing UI coverage includes the PDF-only instruction, upload/resume list/screening controls, authentication and prior responsive dashboard behavior.

## Existing phase coverage

- Recruiter registration/login, duplicate email handling, invalid-password rejection, logout, and database error handling passed.
- Password recovery invalid/expired/reused codes, successful reset, confirmation mismatch, and missing SMTP configuration handling passed.
- Password hashing and existing database/schema checks passed.
- Settings recruiter profile and database connection controls are covered by UI and live MySQL integration tests.

## Changed and added files

- Database/schema: `src/main/resources/schema.sql`, `src/main/java/com/resumescreening/database/DatabaseInitializer.java`, `src/main/java/com/resumescreening/services/DatabaseService.java`.
- Models/services/matching: `Job.java`, `ScreeningResult.java`, `ResumeProfile.java`, `ResumeEntry.java`, `ScreeningEntry.java`, `DashboardMetrics.java`, `PdfResumeParser.java`, `ResumeProfileExtractor.java`, `ResumeUploadService.java`, `ResumeScreeningService.java`, `WeightedResumeRankingEngine.java`.
- UI/application: `Main.java`, `MainFrame.java`, `UITheme.java`, `DashboardView.java`, `JobManagementView.java`, `ResumeWorkspaceView.java`.
- Tests: added `PdfResumeParserTest`, `ResumeProfileExtractorTest`, `ResumeUploadServiceTest`, `ResumeDatabaseIntegrationTest`, and `WeightedResumeRankingEngineTest`; updated `SchemaScriptTest` and `UIComponentsTest`.
- Documentation/configuration: `PROJECT_STATUS.md`, `TEST_REPORT.md`, `.gitignore`.

## Known limitations

- OCR supports English only; low-resolution, rotated, handwritten, or poor-contrast scans may not extract reliably.
- Resume profile extraction uses text/section heuristics and should be recruiter-verified.
- Similarity is deterministic TF-IDF/cosine scoring, not a trained or generative AI model; scores do not make hiring decisions.
- Existing candidate/resume tables do not track recruiter ownership, so resume and candidate records are shared among authenticated recruiter accounts. No ownership column was added.
- SMTP email sending was not exercised; Phase 4 requires the documented SMTP environment variables.
- Candidate and resume totals preserve the current schema semantics and are shared across recruiter accounts. Job and screening report rows are recruiter-scoped.
- Charts are shown in the Reports screen; PDF/CSV exports contain the related underlying summary and screening data rather than embedded charts. PDF's built-in font replaces unsupported non-ASCII glyphs.
- Local database configuration is stored at the project root (or supplied through environment variables); it is excluded from Git and packaged resources. Do not copy real credentials into source-controlled files.

## Files changed for the five reported bugs

- `src/main/java/com/resumescreening/ui/views/JobManagementView.java`
- `src/main/java/com/resumescreening/services/DatabaseService.java`
- `src/main/java/com/resumescreening/ui/views/CandidateDirectoryView.java` (new)
- `src/main/java/com/resumescreening/ui/MainFrame.java`
- `src/main/java/com/resumescreening/services/ResumeContentValidator.java` (new)
- `src/main/java/com/resumescreening/services/PdfResumeParser.java`
- `src/main/java/com/resumescreening/services/ResumeProfileExtractor.java`
- `src/main/java/com/resumescreening/ui/views/ResumeWorkspaceView.java`
- `src/test/java/com/resumescreening/services/PdfResumeParserTest.java`
- `src/test/java/com/resumescreening/services/ResumeProfileExtractorTest.java`
- `src/test/java/com/resumescreening/services/ResumeUploadServiceTest.java`
- `src/test/java/com/resumescreening/services/ResumeDatabaseIntegrationTest.java`
- `src/test/java/com/resumescreening/ui/UIComponentsTest.java`
- `PROJECT_STATUS.md`
- `TEST_REPORT.md`

No schema or database-table changes were made for these bug fixes.

## Files changed for the additional corrections

- `src/main/java/com/resumescreening/services/DatabaseService.java`
- `src/main/java/com/resumescreening/services/ResumeUploadService.java`
- `src/main/java/com/resumescreening/services/PdfResumeParser.java`
- `src/main/java/com/resumescreening/services/ResumeProfileExtractor.java`
- `src/main/java/com/resumescreening/ui/MainFrame.java`
- `src/main/java/com/resumescreening/ui/views/CandidateDirectoryView.java`
- `src/main/java/com/resumescreening/ui/views/ResumeWorkspaceView.java`
- `src/test/java/com/resumescreening/services/PdfResumeParserTest.java`
- `src/test/java/com/resumescreening/services/ResumeProfileExtractorTest.java`
- `src/test/java/com/resumescreening/services/ResumeUploadServiceTest.java`
- `src/test/java/com/resumescreening/services/ResumeDatabaseIntegrationTest.java`
- `src/test/java/com/resumescreening/ui/UIComponentsTest.java`
- `PROJECT_STATUS.md`
- `TEST_REPORT.md`

## Files changed for scanned PDF OCR

- `pom.xml`
- `src/main/java/com/resumescreening/services/PdfResumeParser.java`
- `src/main/java/com/resumescreening/services/PdfOcrService.java` (new)
- `src/main/resources/tessdata/eng.traineddata` (bundled OCR model)
- `src/main/resources/tessdata/LICENSE`
- `src/main/resources/tessdata/NOTICE.md`
- `src/test/java/com/resumescreening/services/PdfResumeParserTest.java`
- `src/test/java/com/resumescreening/services/ResumeUploadServiceTest.java`
- `PROJECT_STATUS.md`
- `TEST_REPORT.md`

## Files changed for Phase 7 Reports and Analytics

- `src/main/java/com/resumescreening/models/JobAnalytics.java` (new)
- `src/main/java/com/resumescreening/models/ReportsSnapshot.java` (new)
- `src/main/java/com/resumescreening/services/DatabaseService.java`
- `src/main/java/com/resumescreening/services/ReportExportService.java` (new)
- `src/main/java/com/resumescreening/ui/MainFrame.java`
- `src/main/java/com/resumescreening/ui/components/StatCard.java`
- `src/main/java/com/resumescreening/ui/views/ReportsAnalyticsView.java` (new)
- `src/test/java/com/resumescreening/services/ReportsAnalyticsTest.java` (new)
- `src/test/java/com/resumescreening/services/ResumeDatabaseIntegrationTest.java`
- `PROJECT_STATUS.md`
- `TEST_REPORT.md`

## Files changed for Phase 8 final testing and delivery

- `README.md` (new)
- `.gitignore`
- `src/main/java/com/resumescreening/Main.java`
- `src/main/java/com/resumescreening/services/ReportExportService.java`
- `src/main/java/com/resumescreening/ui/views/SettingsView.java` (new)
- `src/main/java/com/resumescreening/services/RecruiterAuthService.java`
- `src/main/java/com/resumescreening/services/DatabaseService.java`
- `src/main/java/com/resumescreening/ui/views/DashboardView.java`
- `src/main/java/com/resumescreening/ui/MainFrame.java`
- `src/test/java/com/resumescreening/services/ReportsAnalyticsTest.java`
- `src/test/java/com/resumescreening/services/ResumeDatabaseIntegrationTest.java`
- `src/test/java/com/resumescreening/ui/UIComponentsTest.java`
- `PROJECT_STATUS.md`
- `TEST_REPORT.md`
- Moved the existing local `src/main/resources/database.properties` to project-root `database.properties`; the local configuration was preserved and both locations are ignored by Git.
