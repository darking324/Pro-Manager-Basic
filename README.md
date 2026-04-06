# ProManage Basic

ProManage Basic is a Java and PostgreSQL based project scheduling system.
It prioritizes weekly project execution using revenue prediction plus deadline urgency.

## Current Implementation Status

This implementation pass includes the following upgrades:

- Added Maven build configuration.
- Moved DB credentials out of hardcoded Java constants into config resolution.
- Added input validation for project creation.
- Added status validation for filtered queries.
- Fixed scheduling correctness by reducing pending deadlines every simulated week.
- Made weekly scheduling updates transactional (commit or rollback as one unit).
- Added confirmation UX before running schedule generation.
- Added database setup scripts and optional indexes.
- Updated README with complete setup and troubleshooting instructions.

## Core Features

- Add new projects with title, deadline (days), and revenue.
- Auto-generate project code in the format Proj001, Proj002, etc.
- View projects by all states.
- Generate weekly schedule with max 5 projects (Mon to Fri).
- Predict expected weekly revenue using weighted moving average.
- Track weekly revenue history.

## Project Status Lifecycle

- PENDING: Waiting for scheduling.
- SCHEDULED: Selected for current week.
- COMPLETED: Automatically moved from last week's SCHEDULED before new run.
- EXPIRED: Deadline missed or urgent project rejected.

## Scheduling Model

For each pending project with deadline > 0, score is calculated as:

- score = (0.5 * revenue) + (0.3 * urgency * 5000) - (0.2 * futurePenalty * revenue)
- urgency = 1 / deadline
- futurePenalty = 1 when deadline is flexible and revenue is low vs expected baseline

Then projects are sorted by score and assigned greedily to the latest available day before deadline.

## Architecture

- Main.java: Console menu and user flow.
- ProjectDAO.java: Database read/write methods.
- Scheduler.java: Weekly transactional scheduling pipeline.
- Project.java: Domain model.
- DBConnection.java: JDBC connection using DBConfig values.
- DBConfig.java: Configuration resolver (env vars > properties > defaults).
- InputValidator.java: Validation helpers for input safety.
- ProjectStatus.java: Allowed status enum.
- SchedulingConfig.java: Centralized scoring and schedule constants.

## Prerequisites

- Java 17+
- PostgreSQL 12+
- Maven 3.9+ (recommended)

If Maven is not installed, direct javac compile still works for local execution.

## Database Setup

1. Create a database named promanage.
2. Run schema script:

```sql
\i database/schema.sql
```

3. Optional performance indexes:

```sql
\i database/indexes.sql
```

## Configuration

Configuration resolution order:

1. Environment variables
2. src/main/resources/application.properties
3. Safe hardcoded defaults

Supported environment variables:

- PROMANAGE_DB_URL
- PROMANAGE_DB_USER
- PROMANAGE_DB_PASSWORD

Sample file is available at:

- src/main/resources/application.properties.example

For local development, copy that file to:

- src/main/resources/application.properties

## Build and Run

### Option A: Maven (recommended)

Compile:

```bash
mvn -DskipTests compile
```

Run:

```bash
mvn -DskipTests exec:java -Dexec.mainClass=Main
```

Note: If exec plugin is not configured in your IDE, run Main directly from IDE after compile.

### Option B: javac fallback

Compile:

```bash
javac -d out src/*.java
```

Run:

```bash
java -cp out Main
```

## Console Menu

1. Add Project
2. View All Projects
3. Generate Weekly Schedule
4. View Scheduled Projects
5. View Revenue History
6. View Pending Projects
7. View Completed Projects
8. View Expired Projects
9. Exit

When option 3 is selected, the app asks for confirmation because it performs state-changing operations.

## Transactional Weekly Scheduling Flow

On schedule generation, the following happens in one transaction:

1. Move SCHEDULED -> COMPLETED.
2. Reduce PENDING deadlines by 7 days.
3. Expire overdue pending projects (deadline <= 0).
4. Score and allocate schedule.
5. Mark selected projects as SCHEDULED.
6. Expire urgent rejected projects.
7. Insert weekly revenue history.

If any step fails, all changes are rolled back.

## Known Limitations

- Current UX is terminal based (CLI).
- No authentication/authorization yet.
- No REST API yet in this codebase.
- No automated tests committed yet.

## Modernization Roadmap (Next)

- Spring Boot backend with REST APIs.
- React + TypeScript frontend dashboard.
- Schedule preview API + persisted assignment history.
- Unit/integration/UI tests with CI.

## Troubleshooting

- Database connection failed:
  - Check PostgreSQL is running.
  - Verify DB URL, user, password in env vars or application.properties.

- relation or sequence does not exist:
  - Re-run database/schema.sql.

- Maven command not found:
  - Install Maven and verify with mvn -v.
  - Use javac fallback commands if needed.

## Author

Abhishek Kumar