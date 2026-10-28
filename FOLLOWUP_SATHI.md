# MASTER PRODUCT DEVELOPMENT PROMPT
## Project: FollowUp Saathi — Local Business Customer Follow-up Manager

Act as a senior full-stack software engineer, software architect, user experience designer, database designer, quality assurance engineer, and practical startup product advisor.

Your task is to design and build a complete, functional, secure, beginner-friendly, mobile-responsive web application named **FollowUp Saathi** from scratch to a production-ready first release.

Do not create only a landing page, static dashboard, design mockup, or demonstration with fake functionality. Build a working Minimum Viable Product (MVP) with a real backend, database persistence, authentication, validation, error handling, and tested user workflows.

Explain important implementation decisions clearly because the founder is learning software development and wants to understand, maintain, and extend the codebase independently.

---

# 1. PRODUCT VISION

## 1.1 What is FollowUp Saathi?

FollowUp Saathi is a simple customer enquiry and follow-up management application for small Indian businesses.

Many small businesses receive enquiries through phone calls, website forms, referrals, and messaging applications. Owners often forget to respond, lose track of interested customers, or fail to follow up at the right time.

FollowUp Saathi helps business owners maintain customer records, track enquiry status, schedule follow-ups, prepare messages, and understand which enquiries turn into customers.

The product should reduce missed follow-ups and help business owners organize their daily work.

## 1.2 Target customers

Start with one narrow, practical market:

- Coaching centres and tuition institutes.
- Salons and beauty service businesses.
- Independent service providers and small agencies.

The first release should support a general customer-follow-up workflow without building separate complex modules for every industry.

Keep the data model extensible so that industry-specific features can be introduced later.

## 1.3 Core value proposition

"Har enquiry ka record, har follow-up ka reminder, aur har customer ki progress — ek simple jagah par."

The application must be understandable even to a first-time computer user.

## 1.4 Business model

Use a Software as a Service (SaaS) business model, where customers pay a recurring subscription to use the software.

Proposed pricing hypotheses for later validation:

- Free trial or limited free plan.
- Starter: ₹299 per month.
- Pro: ₹699 per month.

These are experimental prices, not established market prices.

Do not implement a complex subscription payment gateway in the initial release. Design the application so subscription plans and usage limits can be added later.

---

# 2. NON-NEGOTIABLE TECHNOLOGY STACK

Use only the following primary technologies.

## Frontend

- HTML5 (HyperText Markup Language).
- CSS3 (Cascading Style Sheets).
- Vanilla JavaScript (plain JavaScript without a frontend framework).
- Fetch API for backend requests.
- Responsive layouts using CSS Flexbox and CSS Grid.

## Backend

- Java 21.
- Spring Boot 4.0.x, using a stable compatible release.
- Apache Maven.
- Spring Web MVC for REST application programming interfaces.
- Spring Data Java Persistence API (JPA).
- Hibernate Object-Relational Mapping (ORM).
- Spring Security.
- Jakarta Bean Validation.
- Spring Boot Actuator for appropriate operational health checks.

Verify that all selected dependency versions are mutually compatible. If the chosen Spring Boot release requires a different Java version, document the issue and use a compatible stable combination rather than forcing an incompatible setup.

## Database

- MySQL 8.x.
- MySQL Connector/J.
- Version-controlled database migration scripts, preferably using Flyway if compatible with the selected Spring Boot version.

## Development tools

- IntelliJ IDEA for backend development.
- Visual Studio Code for frontend development.
- Git and GitHub for version control.
- Postman or equivalent HTTP client for Application Programming Interface (API) testing.
- Maven Wrapper for reproducible builds.

Do not introduce React, Angular, Vue, Node.js, Python, MongoDB, or another primary development stack.

Small libraries may be introduced only when they provide a clear benefit and remain compatible with the project's simplicity and budget. Explain each additional dependency.

---

# 3. PRODUCT DESIGN PRINCIPLES

The user interface must be exceptionally simple, clean, modern, and attractive.

Design it so that a person with limited technical knowledge can understand the application without training.

## Visual direction

Use a clean, professional Software as a Service dashboard style.

- Light background.
- White cards and surfaces.
- Dark, readable text.
- One primary brand color, preferably blue or indigo.
- Green for completed or successful actions.
- Amber for upcoming reminders.
- Red for overdue items and errors.
- Soft borders and restrained shadows.
- Consistent spacing and typography.
- Simple icons accompanied by understandable labels.

Avoid excessive gradients, glassmorphism, decorative animations, unnecessary charts, oversized headings, and cluttered dashboards.

## Usability requirements

1. Every screen must have one obvious primary action.
2. Every form must use simple labels and helpful examples.
3. Avoid unexplained technical terminology.
4. Use readable font sizes and sufficient color contrast.
5. Display clear loading, empty, success, and error states.
6. Ask for confirmation before destructive actions.
7. Make important actions accessible using keyboard navigation.
8. Use visible focus indicators and semantic HTML.
9. Do not rely on color alone to communicate status.
10. Use a mobile-first responsive layout.

Prefer text such as "Add New Enquiry" over vague buttons such as "Create".

## Responsive behavior

Desktop:
- Sidebar navigation.
- Main content area.
- Compact top navigation with search and profile menu.

Mobile:
- Collapsible navigation or a simple bottom navigation.
- Full-width cards.
- Forms that fit small screens.
- Tables that transform into readable cards where appropriate.
- Touch-friendly buttons and controls.

The application must work at mobile, tablet, and desktop widths.

---

# 4. INFORMATION ARCHITECTURE

Implement the following primary screens.

## 4.1 Public landing page

Create a professional landing page with:

- Product logo or text-based wordmark.
- Headline explaining the main benefit.
- Short description.
- Primary "Get Started" button.
- Secondary "Sign In" button.
- Three to four feature explanations.
- Simple workflow illustration using HTML and CSS.
- Pricing preview.
- Frequently asked questions.
- Footer with privacy policy and terms links.

Do not make unsupported claims such as guaranteed sales growth or guaranteed revenue improvement.

## 4.2 Registration page

Fields:

- Full name.
- Business name.
- Email address.
- Mobile number, optional if not required for the chosen workflow.
- Password.
- Confirm password.

Requirements:

- Validate fields on the client and server.
- Check email format.
- Enforce a reasonable password policy.
- Show and hide password controls.
- Provide understandable validation messages.
- Prevent duplicate account registration.
- Never store plaintext passwords.

After successful registration, guide the user through initial setup.

## 4.3 Login page

Include:

- Email and password.
- Show/hide password.
- Remember-me behavior only if implemented securely.
- Logout.
- Friendly invalid-credentials messages.
- Password recovery only when a real recovery flow has been implemented.

Do not show a successful login message unless authentication actually succeeds.

## 4.4 First-time onboarding

Create a short, beginner-friendly onboarding wizard.

Step 1: Business information.
- Business name.
- Business category.
- Preferred language.

Step 2: Basic preferences.
- Time zone, defaulting to Asia/Kolkata.
- Business working days, with sensible defaults.
- Reminder preferences.

Step 3: First action.
- Add the first customer enquiry.
- Or explore clearly labelled sample data.

Allow users to skip non-essential fields.

Store onboarding progress only when appropriate and provide a way to edit business settings later.

## 4.5 Main dashboard

Display the most useful information first.

Summary cards:
- Total enquiries.
- New enquiries.
- Follow-ups due today.
- Overdue follow-ups.
- Converted customers.

Below the cards, show:

- Today's follow-up list.
- Recently added enquiries.
- Upcoming reminders.
- A small enquiry-status summary.

Each section should link to the relevant screen.

Provide a prominent "Add New Enquiry" action.

Do not display hardcoded statistics. All dashboard values must be calculated from the authenticated user's actual database records.

For a new account, show an informative empty state with an action to add the first enquiry.

## 4.6 Enquiries and customers

Create a searchable, filterable enquiry management screen.

Each enquiry should contain:

- Customer name.
- Phone number.
- Email address, optional.
- Business requirement or service of interest.
- Enquiry source.
- Enquiry status.
- Priority.
- Next follow-up date and time.
- Notes.
- Created date.
- Last updated date.

Supported enquiry sources:
- Phone call.
- Walk-in.
- Referral.
- Website.
- Messaging application.
- Other.

Supported statuses:
- New.
- Contacted.
- Interested.
- Follow-up required.
- Converted.
- Not interested.

Supported priorities:
- Low.
- Medium.
- High.

Required functionality:
- Add enquiry.
- View enquiry details.
- Edit enquiry.
- Change status.
- Schedule the next follow-up.
- Search by customer name or phone number.
- Filter by status, priority, and date.
- Sort by creation date and next follow-up date.
- Paginate large result sets.
- Archive an enquiry.
- Confirm destructive actions.

Avoid creating duplicate customer records accidentally. Warn the user about a possible duplicate within the same business account.

## 4.7 Enquiry details

Create a dedicated detail screen containing:

- Customer information.
- Current status.
- Notes.
- Follow-up history.
- Next scheduled follow-up.
- Quick actions.

Quick actions:
- Edit details.
- Add a note.
- Schedule a follow-up.
- Mark as contacted.
- Mark as converted.
- Archive the enquiry.

Every successful status change must persist in the database.

## 4.8 Follow-up management

Create a dedicated follow-up screen with three sections:

- Due Today.
- Upcoming.
- Overdue.

Each follow-up should show:

- Customer name.
- Contact details where appropriate.
- Scheduled date and time.
- Reason for follow-up.
- Current enquiry status.
- Quick action to complete the follow-up.

Allow users to:
- Schedule a follow-up.
- Reschedule it.
- Mark it as completed.
- Record the outcome.
- Schedule the next follow-up.

A completed follow-up must remain available in history.

Do not silently mark a follow-up as completed simply because its scheduled time has passed.

## 4.9 Notes and follow-up history

Maintain a chronological history for each enquiry.

Record relevant events such as:

- Enquiry created.
- Status changed.
- Follow-up scheduled.
- Follow-up completed.
- Follow-up rescheduled.
- Note added.
- Enquiry converted.

Each event should contain a timestamp and the authenticated user responsible for the action.

Never invent historical events or display fabricated activity.

## 4.10 Message templates

Create editable, ready-to-copy message templates.

Initial templates:
- First response to a new enquiry.
- Follow-up after an unanswered enquiry.
- Appointment confirmation.
- Reminder before an appointment.
- Thank-you message after conversion.

Support placeholders such as:

{{customerName}}
{{businessName}}
{{serviceName}}
{{appointmentDate}}

Provide a preview and a copy-to-clipboard action.

If a required placeholder has no value, handle it clearly instead of sending broken text.

The initial release must not automatically send WhatsApp messages or text messages.

Provide an optional action to open the user's preferred messaging application only when a valid, correctly encoded contact link can be generated.

Automatic messaging can be added later through approved provider integrations, explicit customer consent, and appropriate policy compliance.

## 4.11 Reports

Create a simple reporting screen showing:

- Enquiries received in a selected period.
- Enquiries by status.
- Follow-ups completed.
- Conversions.
- Conversion rate, calculated using a clearly documented formula.

Handle zero denominators safely.

Clearly label date ranges and avoid presenting sample statistics as real business results.

Exporting reports to CSV (Comma-Separated Values) can be included if it fits the initial scope and can be implemented securely.

## 4.12 Settings

Include:

- Business profile.
- User profile.
- Language preference.
- Time zone.
- Working days and hours.
- Reminder preferences.
- Password change.
- Account logout.
- Data export, if implemented.
- Account deletion with explicit confirmation.

Make it possible to edit business information after onboarding.

---

# 5. SCOPE CONTROL

The first release must focus on customer enquiries and follow-ups.

## Must-have features

- Registration and authentication.
- Business onboarding.
- Secure customer records.
- Enquiry creation and editing.
- Status and priority management.
- Follow-up scheduling.
- Follow-up history.
- Dashboard.
- Search and filtering.
- Message templates.
- Basic reports.
- Responsive user interface.
- Validation and error handling.
- Database persistence.
- Documentation and tests.

## Explicitly out of scope for version one

Do not build these unless the core product is working and customer feedback justifies the work:

- Native Android or iOS application.
- Autonomous Artificial Intelligence agents.
- Paid Artificial Intelligence APIs.
- Automatic WhatsApp messaging.
- Complex billing and accounting.
- Inventory management.
- Payroll.
- Payment gateway integration.
- Multi-branch enterprise management.
- Complex team permissions.
- Marketing automation campaigns.
- Complicated charting and analytics.

Design the code so future features can be added without rewriting the entire application.

---

# 6. DATABASE DESIGN

Use MySQL with proper relational integrity.

Design a normalized schema using the following entities.

## User

Suggested fields:
- id.
- full_name.
- email.
- password_hash.
- enabled.
- created_at.
- updated_at.

## Business

Suggested fields:
- id.
- owner_user_id.
- business_name.
- business_category.
- preferred_language.
- time_zone.
- working_days.
- created_at.
- updated_at.

## Enquiry

Suggested fields:
- id.
- business_id.
- customer_name.
- phone_number.
- email.
- service_name.
- source.
- status.
- priority.
- next_follow_up_at.
- notes.
- archived.
- created_at.
- updated_at.

## FollowUp

Suggested fields:
- id.
- enquiry_id.
- scheduled_at.
- reason.
- status.
- outcome.
- completed_at.
- created_at.
- updated_at.

## EnquiryActivity

Suggested fields:
- id.
- enquiry_id.
- actor_user_id.
- event_type.
- description.
- created_at.

## MessageTemplate

Suggested fields:
- id.
- business_id, nullable only if global default templates are deliberately supported.
- template_name.
- message_body.
- created_at.
- updated_at.

## BusinessSettings

Suggested fields:
- id.
- business_id.
- reminder_preferences.
- working_hours.
- created_at.
- updated_at.

Adjust the schema where necessary, but document all decisions.

Important requirements:

1. Use appropriate primary keys and foreign keys.
2. Add indexes for frequently queried fields.
3. Enforce unique account email addresses.
4. Use suitable date/time types and consistent time-zone handling.
5. Store timestamps consistently and convert them for display.
6. Use decimal types for monetary values if monetary fields are introduced later.
7. Prevent invalid orphan records.
8. Add appropriate transaction boundaries.
9. Document deletion and archival behavior.
10. Use database migrations for schema evolution.

Do not store every activity inside one unstructured text field.

Never expose password hashes, internal security fields, or unrelated user information in API responses.

---

# 7. BACKEND ARCHITECTURE

Use a clean layered architecture.

Recommended package structure:

com.followupsaathi
├── config
├── security
├── controller
├── dto
├── entity
├── repository
├── service
├── mapper
├── exception
└── FollowUpSaathiApplication.java

Responsibilities:

- Controller: HTTP requests and responses.
- DTO (Data Transfer Object): request and response data contracts.
- Service: business rules and transactions.
- Repository: database operations.
- Entity: persistent domain models.
- Mapper: entity-to-DTO conversion.
- Security: authentication and authorization.
- Exception: centralized error handling.
- Config: application configuration.

Do not put business logic inside controllers.

Use constructor injection for dependencies.

Avoid field injection, unnecessary static state, circular dependencies, and large classes that do too many things.

Use clear naming conventions and keep methods small enough to understand.

Do not expose database entities directly as API response objects.

Use appropriate HTTP status codes and structured error responses.

---

# 8. REST API REQUIREMENTS

Implement documented REST endpoints.

Authentication:
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout, if required by the chosen session strategy
- GET /api/auth/me

Business:
- GET /api/business
- PUT /api/business

Enquiries:
- POST /api/enquiries
- GET /api/enquiries
- GET /api/enquiries/{id}
- PUT /api/enquiries/{id}
- PATCH /api/enquiries/{id}/status
- POST /api/enquiries/{id}/archive

Follow-ups:
- POST /api/enquiries/{id}/follow-ups
- GET /api/follow-ups
- GET /api/follow-ups/today
- GET /api/follow-ups/upcoming
- GET /api/follow-ups/overdue
- PATCH /api/follow-ups/{id}/complete
- PATCH /api/follow-ups/{id}/reschedule

Activities:
- GET /api/enquiries/{id}/activities

Message templates:
- GET /api/message-templates
- POST /api/message-templates
- PUT /api/message-templates/{id}
- DELETE /api/message-templates/{id}

Reports:
- GET /api/reports/summary

Settings:
- GET /api/settings
- PUT /api/settings

Adjust the endpoint list when necessary, but document every implemented endpoint.

Requirements:

- Validate request bodies.
- Return consistent error responses.
- Support pagination and filtering.
- Validate path variables.
- Prevent unauthorized cross-business access.
- Use transactions for multi-step database operations.
- Return appropriate HTTP status codes.
- Avoid returning stack traces or sensitive information.
- Document example requests and responses.

Never claim an endpoint is implemented merely because it appears in this specification.

---

# 9. AUTHENTICATION AND SECURITY

Security is mandatory, not an optional final step.

Use Spring Security with a deliberately selected authentication strategy.

Prefer secure server-side sessions using HttpOnly cookies and appropriate SameSite settings for this first-party web application when compatible with the deployment architecture.

If session authentication is selected:

- Configure session creation and invalidation correctly.
- Protect state-changing requests against Cross-Site Request Forgery (CSRF).
- Use secure cookie settings in production.
- Implement appropriate login and logout behavior.

If a different strategy is chosen, explain its security trade-offs before implementation.

Additional requirements:

1. Store passwords using a suitable adaptive password-hashing algorithm.
2. Never store or log plaintext passwords.
3. Enforce authorization on every protected backend operation.
4. Scope customer records to the correct business.
5. Never trust a business identifier supplied by the browser without authorization checks.
6. Prevent insecure direct object reference vulnerabilities.
7. Validate and normalize user input.
8. Escape untrusted data before displaying it in the browser.
9. Use parameterized database access.
10. Restrict cross-origin requests to explicitly trusted origins.
11. Apply reasonable login rate limiting where practical.
12. Keep secrets out of Git.
13. Use environment variables for database credentials and production secrets.
14. Return generic authentication errors where detailed errors could reveal account information.
15. Avoid collecting unnecessary personal data.
16. Provide a privacy policy appropriate to the deployed service.
17. Define account deletion and data-retention behavior.
18. Use HTTPS in production.

Do not disable CSRF protection merely to make requests easier during development.

Do not commit real passwords, API keys, private credentials, or production customer records.

---

# 10. FRONTEND IMPLEMENTATION

Use semantic HTML, organized CSS, and modular vanilla JavaScript.

Suggested structure:

frontend/
├── index.html
├── login.html
├── register.html
├── onboarding.html
├── dashboard.html
├── enquiries.html
├── enquiry-details.html
├── follow-ups.html
├── reports.html
├── settings.html
├── css/
│   ├── variables.css
│   ├── base.css
│   ├── layout.css
│   ├── components.css
│   └── pages.css
├── js/
│   ├── api.js
│   ├── auth.js
│   ├── validation.js
│   ├── navigation.js
│   ├── dashboard.js
│   ├── enquiries.js
│   ├── follow-ups.js
│   ├── templates.js
│   └── settings.js
└── assets/

You may improve this structure where needed.

Requirements:

- Keep reusable interface elements consistent.
- Avoid duplicating large blocks of JavaScript.
- Use one centralized API request helper.
- Handle authentication expiry and unauthorized responses.
- Display loading indicators during network requests.
- Prevent accidental duplicate submissions.
- Provide clear empty states.
- Display server validation errors near relevant fields.
- Avoid inserting untrusted server data using unsafe innerHTML operations.
- Never put database credentials or privileged secrets in frontend code.
- Do not store session identifiers or sensitive authentication tokens in localStorage.
- Ensure navigation behaves correctly after login and logout.
- Ensure every visible button performs a real action.

If the frontend and backend use different origins during development, configure CORS (Cross-Origin Resource Sharing) carefully and document the exact setup.

For deployment, prefer serving the frontend and backend through a compatible same-origin arrangement where practical.

---

# 11. USER EXPERIENCE DETAILS

Make everyday actions require as few steps as reasonably possible.

Example: Adding a new enquiry.

1. The user clicks "Add New Enquiry".
2. A simple form opens.
3. The user enters the customer's name and contact details.
4. The user optionally selects the service, priority, and next follow-up.
5. The user clicks "Save Enquiry".
6. The backend validates and stores the record.
7. The interface confirms success.
8. The new enquiry appears in the list and dashboard.

If the operation fails, explain the problem and preserve the user's input where safe.

Use consistent buttons:

- Primary: Save, Add Enquiry, Schedule Follow-up.
- Secondary: Cancel, Back, Edit.
- Destructive: Archive, Delete Account.

Use confirmation dialogs for destructive operations.

Provide clear feedback for copied messages and completed follow-ups.

Never show fake success notifications.

---

# 12. NOTIFICATIONS AND REMINDERS

The first release should support in-app follow-up lists and clear overdue indicators.

An overdue follow-up should be derived from its scheduled time and completion status, not from a manually maintained flag that can become inconsistent.

Use the business's configured time zone when calculating "today".

For the first release, do not promise background push notifications, email delivery, or WhatsApp delivery unless those services have actually been configured and tested.

Prepare the architecture for future reminder delivery through a scheduled job or notification service.

Any future automated reminders must respect user preferences, consent, delivery failures, and provider limits.

---

# 13. TESTING REQUIREMENTS

Create automated tests for the important business rules.

## Backend unit tests

Test:

- Enquiry creation.
- Enquiry status transitions.
- Follow-up scheduling.
- Follow-up completion.
- Overdue follow-up calculation.
- Conversion-rate calculation.
- Input validation.
- Duplicate detection.

## Backend integration tests

Test:

- Registration and login.
- Protected endpoints.
- Database persistence.
- Authorization boundaries.
- Invalid requests.
- Missing records.
- Follow-up filtering.
- Business data isolation.
- Error handling.

## Frontend checks

Verify:

- Forms validate correctly.
- Loading and error states work.
- Search and filtering work.
- Buttons trigger their intended actions.
- Layout adapts to mobile screens.
- Unauthorized users cannot access protected screens.
- Empty states are understandable.

Use a separate test database or isolated test environment.

Never run destructive tests against production data.

The build must pass before the release is considered complete.

---

# 14. DOCUMENTATION

Create and maintain the following files:

README.md
- Product overview.
- Features.
- Screenshots when available.
- Technology stack.
- Prerequisites.
- Setup instructions.
- Application startup commands.
- Test commands.
- Deployment overview.

SETUP_GUIDE.md
- Java installation verification.
- Maven Wrapper usage.
- MySQL database setup.
- Environment variables.
- Backend startup.
- Frontend startup.
- Troubleshooting.

API_DOCUMENTATION.md
- All implemented endpoints.
- Authentication requirements.
- Request examples.
- Response examples.
- Error responses.

DATABASE_SCHEMA.md
- Tables and relationships.
- Constraints and indexes.
- Migration instructions.

SECURITY.md
- Security decisions.
- Secrets management.
- Authentication strategy.
- Data isolation.
- Production checklist.

.env.example
- Placeholder configuration only.
- No real credentials.

Also provide a useful .gitignore file.

Never put real secrets in documentation.

---

# 15. LOCAL DEVELOPMENT AND DEPLOYMENT

The application must run locally on a Windows 11 development machine.

Provide exact commands for:

1. Checking the Java version.
2. Checking Maven Wrapper availability.
3. Creating the MySQL database and user.
4. Configuring environment variables.
5. Starting Spring Boot.
6. Opening the frontend.
7. Running tests.
8. Building the application.
9. Checking backend health.
10. Troubleshooting common errors.

Make all setup instructions consistent with the actual files in the repository.

For production deployment:

- Select an affordable hosting option compatible with the stack.
- Check current provider pricing and free-tier limits before recommending a specific plan.
- Use managed MySQL or another appropriately secured database setup.
- Configure HTTPS.
- Keep secrets in the deployment environment.
- Run database migrations safely.
- Configure backups and verify restoration procedures.
- Use logs and health checks.
- Avoid exposing development-only endpoints.
- Document the deployment process.

Do not claim the application is publicly deployed unless a real deployment has been completed and verified.

---

# 16. FUTURE ROADMAP

Keep future features separate from the first release.

Phase 2:
- Email reminders.
- Optional approved messaging integrations.
- Customer import from CSV.
- Improved reports.
- Custom fields.
- Better onboarding.
- Subscription plans and usage limits.

Phase 3:
- Official WhatsApp Business Platform integration.
- Team accounts and role-based permissions.
- Recurring subscription billing.
- More advanced customer analytics.
- Optional Artificial Intelligence assistance for drafting messages and summarizing notes.

Phase 4:
- Multi-language support.
- Industry-specific workflows.
- Improved mobile experience.
- International time zones and localization.
- Additional integrations based on real customer demand.

Do not implement these features prematurely.

---

# 17. IMPLEMENTATION WORKFLOW

Follow this sequence.

## Stage 1: Inspect and plan

- Inspect the existing repository before changing files.
- If the repository is empty, establish the project structure.
- Confirm Java, Spring Boot, Maven, and MySQL compatibility.
- Define the database schema and migrations.
- Define the API contracts.
- Establish the visual design system.
- Identify security risks and dependencies.

Deliver a concise architecture plan before large-scale implementation.

## Stage 2: Build the foundation

- Initialize the backend.
- Configure database connectivity.
- Configure migrations.
- Implement centralized error handling.
- Configure Spring Security.
- Implement authentication.
- Add automated tests.

Verify the application starts successfully.

## Stage 3: Build the core product

Implement one complete workflow at a time:

1. Business onboarding.
2. Enquiry creation and listing.
3. Enquiry details and editing.
4. Follow-up scheduling and completion.
5. Dashboard calculations.
6. Message templates.
7. Reports and settings.

For each workflow, implement the frontend, backend, database behavior, and tests before moving to the next one.

## Stage 4: Integration

- Connect all frontend screens to real API endpoints.
- Remove temporary hardcoded business data.
- Test loading, success, and error states.
- Verify authorization across multiple business accounts.
- Fix all major integration errors.

## Stage 5: Quality and release

- Run the complete test suite.
- Verify database migrations.
- Check responsive layouts.
- Review security configuration.
- Test backup and recovery procedures.
- Complete documentation.
- Prepare a release checklist.

---

# 18. CODING AGENT RULES

These rules are mandatory.

1. Do not generate the entire project as one enormous, unexplained code dump.
2. Work in small, verifiable stages.
3. Before modifying existing code, inspect the relevant files.
4. Never silently replace working code with an incompatible implementation.
5. Explain what each important file does.
6. Prefer readable, maintainable code over clever shortcuts.
7. Do not create empty classes or placeholder endpoints that pretend to implement features.
8. Do not use fake API responses as production functionality.
9. Do not claim tests passed unless they were actually run and passed.
10. If a command fails, inspect the error and correct the underlying cause.
11. Keep configuration and source code consistent.
12. Use appropriate exception handling without swallowing errors.
13. Never disable security to make a test pass.
14. Do not add unnecessary dependencies.
15. Do not implement all future features in version one.
16. Ask a question only when a missing decision materially affects architecture, security, cost, or product behavior. Otherwise choose a reasonable default and document it.
17. If a required integration cannot be configured, explain the limitation and provide a safe, working alternative.
18. Preserve the founder's ability to understand and maintain the code independently.

At the end of every stage, report:

- What was implemented.
- Files created or modified.
- Commands executed.
- Tests executed and their actual results.
- Known limitations.
- Exact next steps.

Do not advance past a failed critical stage without resolving the failure or explicitly documenting why it remains blocked.

---

# 19. DEFINITION OF DONE

The first release is complete only when all applicable acceptance criteria are satisfied.

- A new user can register and sign in.
- Business onboarding works.
- An enquiry can be created and saved to MySQL.
- The user can view, edit, search, and filter enquiries.
- Follow-ups can be scheduled, completed, and rescheduled.
- Dashboard figures are derived from real data.
- Activity history is persisted.
- Message templates work and can be copied.
- Protected data cannot be accessed by another business.
- Server-side validation and centralized error handling work.
- The interface is usable on mobile and desktop.
- Automated tests cover the core business logic and security boundaries.
- The application builds successfully.
- Setup and API documentation match the implemented product.
- No production credentials are committed.
- Any unimplemented or unverified functionality is explicitly disclosed.

## FINAL INSTRUCTION

Start by inspecting the available development environment and repository.

Then produce the implementation plan, proposed folder structure, database relationship design, and first milestone.

Build the application incrementally, test each milestone, and keep the code understandable for a Java and Spring Boot learner.

Prioritize a working, secure, simple product that real small businesses can use over an impressive but unfinished collection of features.

The objective is not merely to create an application. The objective is to create a maintainable product that can be tested with real customers, improved through feedback, and eventually operated as a sustainable subscription business.