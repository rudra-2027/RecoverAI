# RecoverAI

RecoverAI is a revenue recovery platform for failed recurring payment mandates. It helps merchants identify why payments failed, estimate recoverability, choose the next best recovery action, and track outcomes from ingestion through audit and reporting.

The application combines a Spring Boot backend, a React operations dashboard, rules-based recovery logic, payment simulation, batch imports, and Gemini-powered business insights. Its business value is reducing manual follow-up, improving recovery timing, and giving teams a clear operational view of missed revenue.

## Project Overview

RecoverAI manages the lifecycle of failed mandates:

1. Failed mandates and payment history are ingested through APIs or batch uploads.
2. The recovery agent analyzes the failure reason and customer payment history.
3. A recoverability score and recommended action are generated.
4. The system retries, escalates, requests customer notification, or stops recovery based on configured rules.
5. Decisions, outcomes, recovered revenue, and audit logs are displayed in the dashboard.

## Features

- Dashboard for recovery metrics, trends, and operational status.
- Failed mandate ingestion and management.
- Batch upload support for CSV, XLS, and XLSX files.
- Automated recovery agent with scoring, retry planning, payment verification, and outcome recording.
- Decision management with confirmation, manual overrides, escalation handling, and batch processing.
- Recovery outcome tracking with recovered revenue totals.
- Audit trail views and CSV exports for mandates and batch reports.
- Gemini-backed AI summaries, insights, decision explanations, and merchant Q&A.
- Runtime recovery settings for retry limits, peak-hour avoidance, escalation thresholds, and merchant-level configuration.
- API key protection for backend API routes when a runtime key is configured.
- Demo simulator for testing recovery scenarios.
- Feedback collection for project review and issue tracking.

## Architecture/Workflow

```text
React Dashboard
      |
      v
Spring Boot REST API
      |
      v
PostgreSQL Persistence
      |
      v
Recovery Agent
  - Failure analysis
  - Recoverability scoring
  - Retry/stop/escalation decision
  - Payment verification
  - Mock payment execution
  - Audit and outcome recording
      |
      v
Metrics, reports, AI insights, and dashboard views
```

Key backend modules include ingestion, batch processing, decisions, outcomes, audit logs, metrics, settings, simulation, and AI insights. The frontend exposes these capabilities through dedicated pages for operators and reviewers.

## Technology Stack

**Frontend**

- React 19
- Vite
- React Router
- Axios
- Recharts
- Tailwind CSS
- Oxlint

**Backend**

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Actuator
- PostgreSQL
- Apache POI for Excel imports
- Google GenAI SDK for Gemini integration
- Maven
- Docker

## Installation

### Prerequisites

- Java 21
- Maven or the included Maven wrapper
- Node.js and npm
- PostgreSQL
- Gemini API key, if AI features are required

### Backend Setup

From the `server` directory, configure the required environment variables:

```env
DB_URL=jdbc:postgresql://localhost:5432/recoverai
DB_USERNAME=postgres
DB_PASSWORD=your_password
GEMINI_API_KEY=your_gemini_api_key
VITE_CLIENT_BASE_URL=http://localhost:5173
```

Then run the API:

```bash
cd server
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd server
.\mvnw.cmd spring-boot:run
```

The backend starts on `http://localhost:8080`.

### Frontend Setup

From the `client` directory:

```bash
cd client
npm install
npm run dev
```

Configure the frontend API URL with:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

The frontend development server starts on `http://localhost:5173`.

## Usage

1. Register or load merchants and failed mandates through the dashboard or API.
2. Upload batch files when processing mandates in bulk.
3. Run the recovery agent for a single mandate, selected mandates, or an entire batch.
4. Review generated decisions, including retry recommendations, escalation reasons, and stop reasons.
5. Confirm or override decisions when manual intervention is required.
6. Track outcomes, recovered revenue, recovery rates, and audit history.
7. Use AI insights to summarize performance, explain decisions, and answer operational questions.

Supported batch files must include columns such as `merchantId`, `customerId`, `mandateId`, `amount`, and `failureReason`. Optional columns include `failureCode`, `retryCount`, `maxRetries`, `failureTimestamp`, `paymentDate`, and `mandateStatus`.

## Future Enhancements

- Replace the mock payment gateway with production payment provider integrations.
- Add authentication and role-based access control for dashboard users.
- Expand analytics with merchant-level benchmarking and cohort analysis.
- Add notification integrations for customer outreach workflows.
- Introduce automated scheduled processing for eligible failed mandates.
