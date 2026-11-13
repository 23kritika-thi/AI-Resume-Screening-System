# AI-Based Resume Screening and Candidate Ranking System

A Java desktop application for recruiter account management, job creation, PDF resume processing, explainable candidate screening, ranking, and reporting. The application uses deterministic skill matching and TF-IDF/cosine text similarity as recruiter decision support; it does not make hiring decisions.

## Features

- Recruiter registration, login, logout, and one-time email-code password recovery.
- Settings for persistent recruiter display-name updates and a live database connection check; account email and connection credentials are read-only in the UI.
- Job management with required/preferred skills and minimum experience.
- Candidate records, resume upload and deletion, with managed resume file cleanup.
- PDFBox text extraction with bundled English Tesseract OCR fallback for scanned PDFs.
- Resume-content validation, contact/skill/profile extraction, and duplicate-file detection.
- Explainable screening scores: required skills (50%), preferred skills (20%), experience (15%), and job/resume text similarity (15%).
- Recruiter-scoped reports with database-backed totals, job and screening tables, charts, plus CSV and PDF export.
- An authenticated Reports-screen option to load or separately remove a fictional, idempotent demo dataset.
- MySQL persistence through JDBC prepared statements.

Resume extraction and resume-only validation use heuristics. Review extracted candidate details before relying on them. OCR accuracy depends on scan clarity, page orientation, and English-language text.

## Technologies

- Java 21 or later (Maven compiles with `--release 21`)
- Maven
- Java Swing and FlatLaf
- MySQL and MySQL Connector/J
- Apache PDFBox
- Tess4J/Tesseract with the bundled English model
- JUnit 5

## Prerequisites

1. Install a JDK 21+ and Maven 3.9+ (the project was verified with Maven 3.10.0).
2. Install and start MySQL 8.x.
3. Ensure the MySQL account used by the application can create the database and tables during initial setup, and can read/write the application tables afterward.
4. On Windows, use PowerShell in the project root. If `mvn` is not on `PATH`, run the Maven wrapper executable by its full path, for example `& 'C:\Tools\apache-maven-3.10.0\apache-maven-3.10.0\bin\mvn.cmd'`.

## MySQL setup and configuration

The default database name is `resume_screening_db` on `localhost:3306`. Configure the connection without committing credentials.

### Environment variables (recommended)

Set these in the same shell from which you run Maven:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "resume_screening_db"
$env:DB_USER = "your_mysql_user"
$env:DB_PASSWORD = "your_mysql_password"
```

Database environment variables are `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD`. JVM system properties (`db.host`, `db.port`, `db.name`, `db.user`, `db.password`) take precedence over environment variables.

Alternatively, copy `database.properties.example` to the project root as `database.properties` and edit the local copy. This external file is ignored by Git and takes precedence over the classpath resource. Do not put credentials in source-controlled files or package them in the application JAR.

Initialize the schema and required additive migrations once:

```powershell
mvn exec:java "-Dexec.args=--setup-db"
```

The account needs the database/table creation privileges required for the initial setup. Existing installations are checked and receive only the required additive Phase 5 columns; Phase 7 adds no schema changes. The GUI also attempts the Phase 5 migration when starting.

## Load fictional demo data

1. Start the application and sign in with your existing recruiter account.
2. Open **Reports & Analytics** and choose **Load Demo Data** under “Fictional demo dataset.”
3. Confirm the prompt. The application adds 8 recruiter-owned jobs and 18 fictional candidate resumes, then screens those candidates against relevant demo jobs using the existing ranking service.

Jobs are marked `[DEMO]`, candidates use the reserved `demo.resume-screening.example` email domain, and generated PDFs are stored under the usual `storage/resumes/` directory after passing through PDFBox extraction and the normal resume upload service. Demo profile phone numbers use the fictional 202-555-0101 through 202-555-0118 range. The loader is safe to repeat and does not overwrite existing records. No login or database credentials are embedded in the demo data.

To remove the dataset, use **Remove Demo Data** in the same section and confirm. It removes tagged demo jobs and candidate records only when they are not still referenced by screenings outside the demo jobs. Shared resume files are retained while referenced elsewhere.

## Email configuration for password recovery

Password recovery delivery requires authenticated SMTP configuration. Set all of these environment variables before launching:

```powershell
$env:SMTP_HOST = "smtp.example.com"
$env:SMTP_PORT = "587"
$env:SMTP_USERNAME = "your_smtp_username"
$env:SMTP_PASSWORD = "your_smtp_password"
$env:SMTP_FROM = "recovery@example.com"
```

The application requires STARTTLS and validates the server certificate. If SMTP is unavailable or unconfigured, recovery displays an error instead of claiming the email was sent. Do not commit SMTP credentials.

## Build, test, and run

From the project root:

```powershell
mvn clean package
mvn exec:java
```

The full Maven test suite, including MySQL integration, runs during `mvn clean package`. The MySQL integration test requires a reachable configured MySQL server with the necessary permissions. To run diagnostics instead of opening the GUI:

```powershell
mvn exec:java "-Dexec.args=--cli"
```

The generated JAR is not a shaded/fat JAR; use `mvn exec:java` to run with Maven-managed dependencies.

## Main application modules

| Package | Responsibility |
|---|---|
| `com.resumescreening.ui` | Swing application frame, authentication, dashboard, jobs, candidates, resume screening, and reports views |
| `com.resumescreening.services` | JDBC-facing workflows, authentication/recovery, PDF/OCR parsing, resume upload, screening, demo data, and report export |
| `com.resumescreening.database` | Database configuration, JDBC connections, schema initialization, and additive migrations |
| `com.resumescreening.models` | Recruiter, job, candidate, resume, screening, metrics, and report data |
| `com.resumescreening.ai` | Explainable weighted matching and ranking |
| `com.resumescreening.utils` | Password hashing utilities |
| `src/main/resources/schema.sql` | MySQL table definitions |
| `src/main/resources/tessdata` | Tesseract English model and its notices/license |

Uploaded resume PDFs are stored under `storage/resumes/`; that directory is local application data and excluded from Git.

## Privacy and limitations

- Candidate/resume records are shared across recruiter accounts because the existing schema has no recruiter ownership field on those tables. Jobs and screening reports are recruiter-scoped.
- Resume parsing, profile extraction, and resume-content validation are heuristic; false acceptance/rejection and OCR errors remain possible.
- OCR currently bundles English only. Poor-quality, rotated, handwritten, or low-contrast scans may not extract reliably.
- Ranking is deterministic matching and similarity, not a trained or generative model, and must not be used as an automatic hiring decision.
- Report charts are displayed in the application; CSV/PDF exports include tabular report data. The PDF's built-in font replaces unsupported non-ASCII glyphs.
- SMTP delivery and the Swing interface require environment-specific manual verification; the automated test report records which database and export checks were run.
