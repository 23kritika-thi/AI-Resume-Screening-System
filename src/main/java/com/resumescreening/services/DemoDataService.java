package com.resumescreening.services;

import com.resumescreening.models.Candidate;
import com.resumescreening.models.Job;
import com.resumescreening.models.ResumeEntry;
import com.resumescreening.models.ScreeningEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Adds clearly marked, fictional demonstration records using the normal resume pipeline. */
public class DemoDataService {

    private static final String DEMO_MARKER = "DEMO DATASET: AI Resume Screening Candidate Ranking System";
    private static final List<DemoJob> JOBS = List.of(
        new DemoJob("java", "Java Developer", "Software Engineering",
            "Build and maintain Java services for a fictional cloud operations platform. "
                + "Collaborate on REST APIs, automated tests, code reviews, and SQL-backed features.",
            "Java, Spring Boot, SQL, REST APIs", "Docker, Maven, AWS", 2),
        new DemoJob("python", "Python Developer", "Platform Engineering",
            "Develop Python services and internal tools, create reliable API integrations, "
                + "and contribute to code quality, deployment, and database design.",
            "Python, Django, REST APIs, PostgreSQL", "Pandas, Docker, AWS", 2),
        new DemoJob("frontend", "Frontend Developer", "Product Engineering",
            "Build accessible, responsive product interfaces and reusable components with a modern "
                + "JavaScript framework. Partner with design and API teams.",
            "JavaScript, React, HTML, CSS", "TypeScript, Jest, Figma", 2),
        new DemoJob("data", "Data Analyst", "Business Intelligence",
            "Prepare and analyze product and operations datasets, develop clear dashboards, "
                + "and communicate evidence-based findings to cross-functional teams.",
            "SQL, Python, Excel, data visualization", "Tableau, Power BI, Pandas", 1),
        new DemoJob("software", "Software Engineer", "Engineering",
            "Contribute across the software lifecycle: design, implementation, testing, debugging, "
                + "and documentation for customer-facing applications.",
            "Java, Python, algorithms, Git", "Docker, testing, SQL", 2),
        new DemoJob("backend", "Backend Developer", "Services Engineering",
            "Design and operate secure backend APIs, data models, and asynchronous services "
                + "for a fictional scheduling product.",
            "Java, Spring Boot, PostgreSQL, REST APIs", "Redis, Docker, AWS", 3),
        new DemoJob("design", "UI/UX Designer", "Product Design",
            "Create usable product flows through research, interaction design, prototyping, "
                + "accessibility reviews, and collaboration with engineering.",
            "Figma, wireframing, prototyping, usability", "User research, Adobe XD, accessibility", 2),
        new DemoJob("fullstack", "Full Stack Developer", "Product Engineering",
            "Deliver end-to-end web features across responsive React interfaces and Node.js services, "
                + "including SQL persistence, testing, and deployment.",
            "JavaScript, React, Node.js, SQL, REST APIs", "TypeScript, Docker, Express", 3)
    );

    private static final List<DemoCandidate> CANDIDATES = List.of(
        new DemoCandidate("morgan-lee", "Morgan Lee", "+1 202 555 0101",
            "Java engineer focused on maintainable APIs and transaction-safe services.",
            "Java, Spring Boot, SQL, REST APIs, Maven, Docker, Git, JUnit",
            "B.S. Computer Science, Redwood State University, 2022",
            3, "Java Developer", "Northstar Labs",
            "Built a service monitoring API with Spring Boot, SQL migrations, and automated tests.",
            List.of("java", "backend")),
        new DemoCandidate("taylor-reed", "Taylor Reed", "+1 202 555 0102",
            "Software developer with experience in Java applications, algorithms, and collaborative delivery.",
            "Java, Python, algorithms, Git, SQL, testing, Maven",
            "B.S. Software Engineering, Lakeview Polytechnic, 2021",
            4, "Software Engineer", "Juniper Works",
            "Implemented a scheduling engine with documented algorithms and regression tests.",
            List.of("java", "software")),
        new DemoCandidate("casey-patel", "Casey Patel", "+1 202 555 0103",
            "Python developer and analyst who turns operational data into dependable tools and insights.",
            "Python, Django, REST APIs, PostgreSQL, Pandas, SQL, Excel, Tableau",
            "B.S. Information Systems, Cedar Valley College, 2023",
            2, "Python Developer", "Bluebird Analytics",
            "Created a Django data-quality dashboard with PostgreSQL queries and Python summaries.",
            List.of("python", "data")),
        new DemoCandidate("jordan-kim", "Jordan Kim", "+1 202 555 0104",
            "Backend developer experienced in Python services and relational data systems.",
            "Python, Django, REST APIs, PostgreSQL, Docker, Java, Spring Boot, Redis",
            "B.S. Computer Engineering, Harbor Point University, 2020",
            5, "Backend Developer", "Maple Circuit",
            "Designed API endpoints and database indexes for a fictional inventory service.",
            List.of("python", "backend")),
        new DemoCandidate("avery-brooks", "Avery Brooks", "+1 202 555 0105",
            "Frontend and full stack developer building accessible interfaces and practical APIs.",
            "JavaScript, React, HTML, CSS, TypeScript, Jest, Node.js, SQL, REST APIs",
            "B.S. Digital Technology, Westbridge University, 2022",
            3, "Frontend Developer", "Orchard Digital",
            "Delivered a responsive React portal backed by Node.js and SQL services.",
            List.of("frontend", "fullstack")),
        new DemoCandidate("riley-chen", "Riley Chen", "+1 202 555 0106",
            "Frontend specialist interested in design systems, accessible interfaces, and testable components.",
            "JavaScript, React, HTML, CSS, TypeScript, Jest, Figma, Git",
            "B.A. Interactive Media, Pinecrest Institute, 2023",
            2, "Frontend Developer", "Silver Fern Studio",
            "Built a keyboard-accessible component library and tested reusable React views.",
            List.of("frontend")),
        new DemoCandidate("quinn-rivera", "Quinn Rivera", "+1 202 555 0107",
            "Data analyst with a foundation in SQL reporting and clear stakeholder communication.",
            "SQL, Python, Excel, data visualization, Power BI, Pandas",
            "B.S. Applied Statistics, Meadowbrook University, 2021",
            4, "Data Analyst", "Clearwater Metrics",
            "Prepared a product usage dashboard and documented weekly SQL analysis.",
            List.of("data")),
        new DemoCandidate("drew-parker", "Drew Parker", "+1 202 555 0108",
            "Analytical software engineer combining data skills with disciplined application development.",
            "SQL, Python, Excel, data visualization, Java, algorithms, Git, testing",
            "B.S. Computer Science, Summit Grove University, 2022",
            3, "Software Engineer", "Willow Logic",
            "Created a Python reporting tool with validated SQL extracts and repeatable tests.",
            List.of("data", "software")),
        new DemoCandidate("skylar-evans", "Skylar Evans", "+1 202 555 0109",
            "Product designer who translates user research into clear flows, prototypes, and accessible interfaces.",
            "Figma, wireframing, prototyping, usability, user research, accessibility",
            "B.A. Human-Centered Design, Briar Hill College, 2021",
            4, "UI/UX Designer", "Mosaic Path",
            "Prototyped an appointment flow and ran moderated usability sessions for a class project.",
            List.of("design")),
        new DemoCandidate("robin-hayes", "Robin Hayes", "+1 202 555 0110",
            "UI/UX designer skilled in interaction patterns, visual prototypes, and iterative feedback.",
            "Figma, wireframing, prototyping, usability, Adobe XD, user research",
            "B.F.A. Interaction Design, Elm Harbor School of Design, 2023",
            2, "Product Designer", "Kindred Interface",
            "Designed and tested an accessible onboarding prototype for a fictional community app.",
            List.of("design")),
        new DemoCandidate("cameron-diaz", "Cameron Diaz", "+1 202 555 0111",
            "Software engineer specializing in backend services, test coverage, and practical data models.",
            "Java, Spring Boot, PostgreSQL, REST APIs, algorithms, Git, Docker, Redis",
            "B.S. Software Engineering, Granite Coast University, 2020",
            5, "Backend Engineer", "Cobalt Bridge",
            "Implemented a versioned REST API with PostgreSQL persistence and integration tests.",
            List.of("software", "backend")),
        new DemoCandidate("alexis-ward", "Alexis Ward", "+1 202 555 0112",
            "Full stack software engineer delivering web features from interface through data layer.",
            "Java, Python, algorithms, Git, JavaScript, React, Node.js, SQL, REST APIs, Docker",
            "B.S. Computer Science, Fairhaven University, 2021",
            4, "Software Engineer", "Pebble Creek Systems",
            "Built an internal workflow tool with React, Node.js, SQL, and automated tests.",
            List.of("software", "fullstack")),
        new DemoCandidate("jamie-bell", "Jamie Bell", "+1 202 555 0113",
            "Java backend developer focused on API reliability, database design, and readable code.",
            "Java, Spring Boot, PostgreSQL, REST APIs, SQL, Docker, Maven, Redis",
            "B.S. Information Technology, Alder Ridge University, 2022",
            3, "Backend Developer", "Copperline Software",
            "Added resilient API endpoints and PostgreSQL persistence to a fictional booking platform.",
            List.of("java", "backend")),
        new DemoCandidate("kendall-price", "Kendall Price", "+1 202 555 0114",
            "Full stack developer experienced in modern web interfaces and API-backed product features.",
            "JavaScript, React, Node.js, SQL, REST APIs, TypeScript, Docker, Express, CSS",
            "B.S. Web Computing, Brookfield Institute, 2021",
            4, "Full Stack Developer", "Lighthouse Product Lab",
            "Delivered a responsive React workflow with an Express API and relational data store.",
            List.of("frontend", "fullstack")),
        new DemoCandidate("payton-gray", "Payton Gray", "+1 202 555 0115",
            "Python developer with a broad engineering foundation and an interest in tested services.",
            "Python, Django, REST APIs, PostgreSQL, Pandas, Java, algorithms, Git, SQL",
            "B.S. Computer Applications, Birchwood University, 2023",
            2, "Python Developer", "Pioneer Grove",
            "Created a Django service that validates data imports and produces audit summaries.",
            List.of("python", "software")),
        new DemoCandidate("emerson-cole", "Emerson Cole", "+1 202 555 0116",
            "Java and web developer who enjoys connecting reliable services with polished interfaces.",
            "Java, Spring Boot, SQL, REST APIs, JavaScript, React, Node.js, Maven, Docker",
            "B.S. Computer Science, Foxglove University, 2020",
            5, "Application Developer", "Crescent Harbor Tech",
            "Built an end-to-end service dashboard with Java APIs and React components.",
            List.of("java", "fullstack")),
        new DemoCandidate("reese-foster", "Reese Foster", "+1 202 555 0117",
            "Design-minded frontend developer who values usability, accessibility, and rapid prototyping.",
            "JavaScript, React, HTML, CSS, Figma, wireframing, prototyping, usability, Jest",
            "B.A. Product Design, Maple Ridge College, 2022",
            3, "Frontend Developer", "Bright Kestrel Studio",
            "Translated Figma prototypes into responsive React screens and documented usability findings.",
            List.of("frontend", "design")),
        new DemoCandidate("finley-ross", "Finley Ross", "+1 202 555 0118",
            "Data professional using Python and SQL to prepare reliable reports and explain trends.",
            "SQL, Python, Excel, data visualization, Tableau, Pandas, PostgreSQL",
            "B.S. Data Science, Stonebrook University, 2022",
            3, "Data Analyst", "Open Meadow Research",
            "Automated a recurring analysis with Python, PostgreSQL, and a Tableau dashboard.",
            List.of("data", "python"))
    );

    private final DatabaseService databaseService;
    private final ResumeUploadService uploadService;
    private final ResumeScreeningService screeningService;
    private final String namespace;

    public DemoDataService(DatabaseService databaseService, ResumeUploadService uploadService,
                           ResumeScreeningService screeningService) {
        this(databaseService, uploadService, screeningService, "");
    }

    DemoDataService(DatabaseService databaseService, ResumeUploadService uploadService,
                    ResumeScreeningService screeningService, String namespace) {
        this.databaseService = Objects.requireNonNull(databaseService, "databaseService");
        this.uploadService = Objects.requireNonNull(uploadService, "uploadService");
        this.screeningService = Objects.requireNonNull(screeningService, "screeningService");
        this.namespace = normalizeNamespace(namespace);
    }

    public LoadResult load(int recruiterId) throws IOException, java.sql.SQLException,
        PdfResumeParser.ResumeValidationException, ResumeUploadService.DuplicateResumeException {
        if (recruiterId <= 0) {
            throw new IllegalArgumentException("A signed-in recruiter is required to load demo data.");
        }

        Map<String, Job> jobsByCode = new LinkedHashMap<>();
        List<Job> existingJobs = databaseService.getJobsForRecruiter(recruiterId);
        int jobsCreated = 0;
        int jobsReused = 0;
        for (DemoJob demoJob : JOBS) {
            String title = markedJobTitle(demoJob);
            Job existing = existingJobs.stream()
                .filter(job -> title.equals(job.getTitle()))
                .findFirst()
                .orElse(null);
            if (existing == null) {
                Job job = new Job(recruiterId, title, demoJob.department(), demoJob.description(),
                    demoJob.requiredSkills(), demoJob.minExperienceYears());
                job.setPreferredSkills(demoJob.preferredSkills());
                jobsByCode.put(demoJob.code(), databaseService.createJob(job));
                jobsCreated++;
            } else {
                verifyJob(existing, demoJob);
                jobsByCode.put(demoJob.code(), existing);
                jobsReused++;
            }
        }

        Path sourceDirectory = Files.createTempDirectory("resume-screening-demo-");
        try {
            ResumeLoadResult resumeLoad = loadResumes(sourceDirectory);
            Map<String, ResumeEntry> entriesByCandidate = resumeLoad.entries();
            int screeningsCreated = 0;
            int screeningsReused = 0;

            for (DemoJob demoJob : JOBS) {
                Job job = jobsByCode.get(demoJob.code());
                List<ResumeEntry> associated = CANDIDATES.stream()
                    .filter(candidate -> candidate.jobCodes().contains(demoJob.code()))
                    .map(candidate -> entriesByCandidate.get(candidate.slug()))
                    .toList();
                Set<Integer> screenedResumeIds = new LinkedHashSet<>();
                for (ScreeningEntry entry : databaseService.getLatestScreeningResults(job.getId())) {
                    screenedResumeIds.add(entry.resume().getId());
                }
                List<ResumeEntry> pending = associated.stream()
                    .filter(entry -> !screenedResumeIds.contains(entry.resume().getId()))
                    .toList();
                if (!pending.isEmpty()) {
                    screeningsCreated += screeningService.screen(recruiterId, job.getId(), pending).size();
                }
                screeningsReused += associated.size() - pending.size();
            }

            return new LoadResult(jobsCreated, jobsReused, resumeLoad.created(), resumeLoad.reused(),
                screeningsCreated, screeningsReused);
        } finally {
            deleteTemporarySources(sourceDirectory);
        }
    }

    public RemoveResult remove(int recruiterId) throws IOException, java.sql.SQLException {
        DatabaseService.DemoDataCleanup deleted = databaseService.deleteDemoData(recruiterId, namespace);
        ResumeUploadService.FileCleanupResult files =
            uploadService.removeOrphanedFiles(deleted.orphanedResumePaths());
        return new RemoveResult(deleted.jobsDeleted(), deleted.candidatesDeleted(),
            deleted.candidatesRetained(), files.deleted(), files.retained());
    }

    private ResumeLoadResult loadResumes(Path sourceDirectory) throws IOException,
        java.sql.SQLException, PdfResumeParser.ResumeValidationException,
        ResumeUploadService.DuplicateResumeException {
        List<ResumeEntry> existingEntries = databaseService.getResumes();
        Map<String, ResumeEntry> entries = new LinkedHashMap<>();
        int created = 0;
        int reused = 0;
        for (DemoCandidate candidate : CANDIDATES) {
            String email = candidateEmail(candidate);
            String fileName = resumeFileName(candidate);
            List<ResumeEntry> sameEmail = existingEntries.stream()
                .filter(entry -> email.equalsIgnoreCase(entry.candidate().getEmail()))
                .toList();
            ResumeEntry existing = sameEmail.stream()
                .filter(entry -> fileName.equals(entry.resume().getFileName()))
                .findFirst()
                .orElse(null);
            if (existing != null) {
                if (existing.resume().getExtractedText() == null
                    || !existing.resume().getExtractedText().contains(DEMO_MARKER)
                    || !candidate.name().equals(existing.candidate().getFullName())) {
                    throw new java.sql.SQLException(
                        "A tagged demo identity conflicts with existing data; no existing record was changed."
                    );
                }
                entries.put(candidate.slug(), existing);
                reused++;
                continue;
            }
            if (!sameEmail.isEmpty()) {
                throw new java.sql.SQLException(
                    "A reserved demo email is already used by a different resume; no existing record was changed."
                );
            }

            Path source = sourceDirectory.resolve(fileName);
            writeResumePdf(source, candidate, email);
            ResumeUploadService.UploadResult uploaded = uploadService.upload(source, null);
            verifyExtractedProfile(candidate, email, uploaded.profile());
            entries.put(candidate.slug(), uploaded.entry());
            created++;
        }
        return new ResumeLoadResult(entries, created, reused);
    }

    private void verifyExtractedProfile(DemoCandidate candidate, String email,
                                        com.resumescreening.models.ResumeProfile profile)
        throws java.sql.SQLException {
        if (!candidate.name().equals(profile.candidateName())
            || !email.equalsIgnoreCase(profile.email())
            || profile.phone().isBlank()
            || profile.skills().isEmpty()
            || profile.education().isEmpty()
            || profile.experienceYears() != candidate.experienceYears()) {
            throw new java.sql.SQLException(
                "The resume extraction pipeline did not recover the expected fictional profile data for "
                    + candidate.slug() + " (name=" + profile.candidateName()
                    + ", email=" + profile.email() + ", phonePresent=" + !profile.phone().isBlank()
                    + ", skillCount=" + profile.skills().size()
                    + ", educationLines=" + profile.education().size()
                    + ", years=" + profile.experienceYears() + ")."
            );
        }
    }

    private void verifyJob(Job job, DemoJob expected) throws java.sql.SQLException {
        if (!expected.department().equals(job.getDepartment())
            || !expected.description().equals(job.getDescription())
            || !expected.requiredSkills().equals(job.getRequiredSkills())
            || !expected.preferredSkills().equals(job.getPreferredSkills())
            || expected.minExperienceYears() != job.getMinExperienceYears()) {
            throw new java.sql.SQLException(
                "A tagged demo job conflicts with existing data; no existing record was changed."
            );
        }
    }

    private String markedJobTitle(DemoJob job) {
        return "[DEMO] " + job.title() + (namespace.isEmpty() ? "" : " (" + namespace + ")");
    }

    private String candidateEmail(DemoCandidate candidate) {
        return candidate.slug() + (namespace.isEmpty() ? "" : "." + namespace)
            + "@demo.resume-screening.example";
    }

    private String resumeFileName(DemoCandidate candidate) {
        return "DEMO-" + candidate.slug() + (namespace.isEmpty() ? "" : "-" + namespace) + ".pdf";
    }

    private void writeResumePdf(Path destination, DemoCandidate candidate, String email) throws IOException {
        List<String> content = new ArrayList<>(List.of(
            candidate.name(),
            DEMO_MARKER,
            "Fictional college project demonstration profile",
            "Email: " + email,
            "Phone: " + candidate.phone(),
            "",
            "Professional Summary",
            candidate.summary(),
            "",
            "Technical Skills",
            candidate.skills(),
            "",
            "Education",
            candidate.education(),
            "",
            "Work Experience",
            candidate.experienceYears() + " years of professional experience.",
            candidate.role() + " | " + candidate.organization(),
            "Contributed to planning, implementation, peer review, and documentation.",
            "",
            "Projects",
            candidate.project(),
            "",
            "Certifications",
            "Project portfolio and coursework verified by fictional demonstration content."
        ));
        List<String> lines = content.stream().flatMap(line -> wrap(line, 92).stream()).toList();
        try (PDDocument document = new PDDocument()) {
            int lineIndex = 0;
            while (lineIndex < lines.size()) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    stream.newLineAtOffset(52, 740);
                    int pageLines = 0;
                    while (lineIndex < lines.size() && pageLines < 48) {
                        String line = lines.get(lineIndex++);
                        if (isHeading(line)) {
                            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                        } else {
                            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        }
                        stream.showText(line);
                        stream.newLineAtOffset(0, -14);
                        pageLines++;
                    }
                    stream.endText();
                }
            }
            document.save(destination.toFile());
        }
    }

    private List<String> wrap(String value, int width) {
        if (value.isBlank() || value.length() <= width) {
            return List.of(value);
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private boolean isHeading(String line) {
        return line.equals("Professional Summary") || line.equals("Technical Skills")
            || line.equals("Education") || line.equals("Work Experience")
            || line.equals("Projects") || line.equals("Certifications")
            || line.equals("Fictional college project demonstration profile");
    }

    private void deleteTemporarySources(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static String normalizeNamespace(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        if (normalized.isEmpty() || normalized.length() > 24) {
            throw new IllegalArgumentException("The demo namespace must contain 1 to 24 letters or digits.");
        }
        return normalized;
    }

    public record LoadResult(int jobsCreated, int jobsReused, int resumesCreated, int resumesReused,
                             int screeningsCreated, int screeningsReused) {
    }

    public record RemoveResult(int jobsDeleted, int candidatesDeleted, int candidatesRetained,
                               int filesDeleted, int filesRetained) {
    }

    private record ResumeLoadResult(Map<String, ResumeEntry> entries, int created, int reused) {
    }

    private record DemoJob(String code, String title, String department, String description,
                           String requiredSkills, String preferredSkills, int minExperienceYears) {
    }

    private record DemoCandidate(String slug, String name, String phone, String summary, String skills,
                                 String education, int experienceYears, String role, String organization,
                                 String project, List<String> jobCodes) {
        private DemoCandidate {
            jobCodes = List.copyOf(jobCodes);
        }
    }
}
