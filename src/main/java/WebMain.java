import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import spark.Request;
import spark.Response;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.options;
import static spark.Spark.patch;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.staticFiles;

public class WebMain {

    private static final Gson GSON = new Gson();
    private static final Map<String, AuthUser> TOKEN_STORE = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        bootstrapSchema();

        port(resolvePort());
        staticFiles.location("/public");

        options("/*", (request, response) -> {
            applyCorsHeaders(response);
            return "ok";
        });

        before((request, response) -> {
            applyCorsHeaders(response);
            response.type("application/json");
        });

        after((request, response) -> {
            if (response.type() == null) {
                response.type("application/json");
            }
        });

        exception(Exception.class, (exception, request, response) ->
                writeError(response, 500, "Unexpected server error."));

        path("/api", () -> {
            path("/auth", () -> {
                post("/signup", (request, response) -> handleSignup(request, response));
                post("/login", (request, response) -> handleLogin(request, response));
            });

            before("/*", (request, response) -> {
                if (request.pathInfo().startsWith("/api/auth")) {
                    return;
                }

                AuthUser user = authenticate(request);
                if (user == null) {
                    haltUnauthorized();
                    return;
                }
                request.attribute("user", user);
            });

            get("/auth/me", (request, response) -> {
                AuthUser user = request.attribute("user");
                return writeSuccess(response, mapOf(
                        "name", user.name,
                        "email", user.email
                ));
            });

            get("/dashboard", (request, response) -> handleDashboard(response));

            get("/tasks", (request, response) -> handleListTasks(request, response));
            post("/tasks", (request, response) -> handleCreateTask(request, response));
            put("/tasks/:id", (request, response) -> handleUpdateTask(request, response));
            patch("/tasks/:id", (request, response) -> handleUpdateTask(request, response));
            delete("/tasks/:id", (request, response) -> handleDeleteTask(request, response));
        });

        get("/health", (request, response) -> {
            response.type("text/plain");
            return "ok";
        });
    }

    private static int resolvePort() {
        String env = System.getenv("PORT");
        if (env == null || env.isBlank()) {
            return 8080;
        }

        try {
            return Integer.parseInt(env.trim());
        } catch (NumberFormatException ignored) {
            return 8080;
        }
    }

    private static void bootstrapSchema() {
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(120) NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            st.execute("ALTER TABLE projects ADD COLUMN IF NOT EXISTS description TEXT");
            st.execute("ALTER TABLE projects ADD COLUMN IF NOT EXISTS priority VARCHAR(10)");
            st.execute("ALTER TABLE projects ADD COLUMN IF NOT EXISTS due_date DATE");

            st.execute("""
                    UPDATE projects
                    SET priority = CASE
                        WHEN revenue >= 80000 THEN 'HIGH'
                        WHEN revenue >= 30000 THEN 'MEDIUM'
                        ELSE 'LOW'
                    END
                    WHERE priority IS NULL
                    """);

            st.execute("""
                    UPDATE projects
                    SET description = CONCAT('Projected revenue: ', revenue)
                    WHERE description IS NULL
                    """);

            st.execute("""
                    UPDATE projects
                    SET due_date = CURRENT_DATE + deadline
                    WHERE due_date IS NULL
                    """);

        } catch (SQLException e) {
            throw new RuntimeException("Schema bootstrap failed: " + e.getMessage(), e);
        }
    }

    private static Object handleSignup(Request request, Response response) {
        SignupRequest payload;
        try {
            payload = GSON.fromJson(request.body(), SignupRequest.class);
        } catch (JsonSyntaxException e) {
            return writeError(response, 400, "Invalid request body.");
        }

        String name;
        String email;
        String password;

        try {
            name = InputValidator.requireNonBlank("name", payload == null ? null : payload.name);
            email = InputValidator.requireNonBlank("email", payload == null ? null : payload.email).toLowerCase(Locale.ROOT);
            password = InputValidator.requireNonBlank("password", payload == null ? null : payload.password);
        } catch (IllegalArgumentException e) {
            return writeError(response, 400, e.getMessage());
        }

        if (!email.contains("@") || password.length() < 6) {
            return writeError(response, 400, "Use a valid email and password (min 6 chars).");
        }

        String sql = "INSERT INTO users(name, email, password_hash) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, BCrypt.hashpw(password, BCrypt.gensalt(12)));
            ps.executeUpdate();
            return writeSuccess(response, mapOf("message", "Signup successful."));

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("unique")) {
                return writeError(response, 409, "Email is already registered.");
            }
            return writeError(response, 500, "Failed to create account.");
        }
    }

    private static Object handleLogin(Request request, Response response) {
        LoginRequest payload;
        try {
            payload = GSON.fromJson(request.body(), LoginRequest.class);
        } catch (JsonSyntaxException e) {
            return writeError(response, 400, "Invalid request body.");
        }

        String email;
        String password;

        try {
            email = InputValidator.requireNonBlank("email", payload == null ? null : payload.email).toLowerCase(Locale.ROOT);
            password = InputValidator.requireNonBlank("password", payload == null ? null : payload.password);
        } catch (IllegalArgumentException e) {
            return writeError(response, 400, e.getMessage());
        }

        String sql = "SELECT name, email, password_hash FROM users WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return writeError(response, 401, "Invalid credentials.");
                }

                String hash = rs.getString("password_hash");
                if (!BCrypt.checkpw(password, hash)) {
                    return writeError(response, 401, "Invalid credentials.");
                }

                AuthUser user = new AuthUser(rs.getString("name"), rs.getString("email"));
                String token = UUID.randomUUID().toString();
                TOKEN_STORE.put(token, user);

                return writeSuccess(response, mapOf(
                        "token", token,
                        "user", mapOf("name", user.name, "email", user.email)
                ));
            }

        } catch (SQLException e) {
            return writeError(response, 500, "Failed to log in.");
        }
    }

    private static Object handleDashboard(Response response) {
        String sql = """
                SELECT
                    COUNT(*) AS total,
                    COUNT(*) FILTER (WHERE status = 'PENDING') AS todo,
                    COUNT(*) FILTER (WHERE status = 'SCHEDULED') AS in_progress,
                    COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed,
                    COUNT(*) FILTER (WHERE status = 'EXPIRED') AS expired,
                    COALESCE(SUM(revenue), 0) AS total_revenue,
                    COUNT(*) FILTER (WHERE due_date <= CURRENT_DATE + 7 AND status IN ('PENDING', 'SCHEDULED')) AS due_soon
                FROM projects
                """;

        List<Map<String, Object>> revenueTrend = new ArrayList<>();
        String trendSql = "SELECT week_no, total_revenue, created_at FROM weekly_revenue_history ORDER BY week_no DESC LIMIT 8";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             PreparedStatement tps = con.prepareStatement(trendSql);
             ResultSet trs = tps.executeQuery()) {

            while (trs.next()) {
                revenueTrend.add(mapOf(
                        "weekNo", trs.getInt("week_no"),
                        "totalRevenue", trs.getInt("total_revenue"),
                        "createdAt", trs.getTimestamp("created_at").toString()
                ));
            }

            if (!rs.next()) {
                return writeSuccess(response, mapOf());
            }

            return writeSuccess(response, mapOf(
                    "total", rs.getInt("total"),
                    "todo", rs.getInt("todo"),
                    "inProgress", rs.getInt("in_progress"),
                    "completed", rs.getInt("completed"),
                    "expired", rs.getInt("expired"),
                    "totalRevenue", rs.getInt("total_revenue"),
                    "dueSoon", rs.getInt("due_soon"),
                    "revenueTrend", revenueTrend
            ));

        } catch (SQLException e) {
            return writeError(response, 500, "Failed to load dashboard.");
        }
    }

    private static Object handleListTasks(Request request, Response response) {
        String statusParam = trimToNull(request.queryParams("status"));
        String priorityParam = trimToNull(request.queryParams("priority"));
        String fromDateParam = trimToNull(request.queryParams("fromDate"));
        String toDateParam = trimToNull(request.queryParams("toDate"));
        String qParam = trimToNull(request.queryParams("q"));

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    project_code,
                    title,
                    COALESCE(description, '') AS description,
                    status,
                    COALESCE(priority, 'MEDIUM') AS priority,
                    COALESCE(due_date, CURRENT_DATE + deadline) AS due_date,
                    deadline,
                    revenue
                FROM projects
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        String dbStatus = uiStatusToDb(statusParam);
        if (dbStatus != null) {
            sql.append(" AND status = ?");
            params.add(dbStatus);
        }

        if (priorityParam != null) {
            String normalizedPriority = normalizePriority(priorityParam);
            if (normalizedPriority != null) {
                sql.append(" AND COALESCE(priority, 'MEDIUM') = ?");
                params.add(normalizedPriority);
            }
        }

        if (fromDateParam != null) {
            try {
                LocalDate from = LocalDate.parse(fromDateParam);
                sql.append(" AND COALESCE(due_date, CURRENT_DATE + deadline) >= ?");
                params.add(Date.valueOf(from));
            } catch (Exception e) {
                return writeError(response, 400, "Invalid fromDate. Use YYYY-MM-DD.");
            }
        }

        if (toDateParam != null) {
            try {
                LocalDate to = LocalDate.parse(toDateParam);
                sql.append(" AND COALESCE(due_date, CURRENT_DATE + deadline) <= ?");
                params.add(Date.valueOf(to));
            } catch (Exception e) {
                return writeError(response, 400, "Invalid toDate. Use YYYY-MM-DD.");
            }
        }

        if (qParam != null) {
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(COALESCE(description, '')) LIKE ?)");
            String q = "%" + qParam.toLowerCase(Locale.ROOT) + "%";
            params.add(q);
            params.add(q);
        }

        sql.append(" ORDER BY id DESC");

        List<Map<String, Object>> tasks = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapOf(
                            "id", rs.getInt("id"),
                            "projectCode", rs.getString("project_code"),
                            "title", rs.getString("title"),
                            "description", rs.getString("description"),
                            "status", dbStatusToUi(rs.getString("status")),
                            "priority", rs.getString("priority").toLowerCase(Locale.ROOT),
                            "dueDate", rs.getDate("due_date").toLocalDate().toString(),
                            "deadlineDays", rs.getInt("deadline"),
                            "revenue", rs.getInt("revenue")
                    ));
                }
            }

            return writeSuccess(response, tasks);

        } catch (SQLException e) {
            return writeError(response, 500, "Failed to fetch tasks.");
        }
    }

    private static Object handleCreateTask(Request request, Response response) {
        TaskPayload payload;
        try {
            payload = GSON.fromJson(request.body(), TaskPayload.class);
        } catch (JsonSyntaxException e) {
            return writeError(response, 400, "Invalid request body.");
        }

        TaskInput input;
        try {
            input = normalizeTaskInput(payload);
        } catch (IllegalArgumentException e) {
            return writeError(response, 400, e.getMessage());
        }

        String sql = """
                INSERT INTO projects(project_code, title, description, deadline, revenue, status, priority, due_date)
                VALUES ('Proj' || LPAD(nextval('projects_id_seq')::text, 3, '0'), ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, project_code
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, input.title);
            ps.setString(2, input.description);
            ps.setInt(3, input.deadlineDays);
            ps.setInt(4, input.revenue);
            ps.setString(5, input.dbStatus);
            ps.setString(6, input.priority);
            ps.setDate(7, Date.valueOf(input.dueDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return writeSuccess(response, mapOf(
                            "id", rs.getInt("id"),
                            "projectCode", rs.getString("project_code")
                    ));
                }
            }

            return writeError(response, 500, "Task creation failed.");

        } catch (SQLException e) {
            return writeError(response, 500, "Task creation failed.");
        }
    }

    private static Object handleUpdateTask(Request request, Response response) {
        int id;
        try {
            id = Integer.parseInt(request.params(":id"));
        } catch (NumberFormatException e) {
            return writeError(response, 400, "Invalid task id.");
        }

        TaskPayload payload;
        try {
            payload = GSON.fromJson(request.body(), TaskPayload.class);
        } catch (JsonSyntaxException e) {
            return writeError(response, 400, "Invalid request body.");
        }

        TaskInput input;
        try {
            input = normalizeTaskInput(payload);
        } catch (IllegalArgumentException e) {
            return writeError(response, 400, e.getMessage());
        }

        String sql = """
                UPDATE projects
                SET title = ?, description = ?, deadline = ?, revenue = ?, status = ?, priority = ?, due_date = ?
                WHERE id = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, input.title);
            ps.setString(2, input.description);
            ps.setInt(3, input.deadlineDays);
            ps.setInt(4, input.revenue);
            ps.setString(5, input.dbStatus);
            ps.setString(6, input.priority);
            ps.setDate(7, Date.valueOf(input.dueDate));
            ps.setInt(8, id);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                return writeError(response, 404, "Task not found.");
            }

            return writeSuccess(response, mapOf("message", "Task updated."));

        } catch (SQLException e) {
            return writeError(response, 500, "Failed to update task.");
        }
    }

    private static Object handleDeleteTask(Request request, Response response) {
        int id;
        try {
            id = Integer.parseInt(request.params(":id"));
        } catch (NumberFormatException e) {
            return writeError(response, 400, "Invalid task id.");
        }

        String sql = "DELETE FROM projects WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                return writeError(response, 404, "Task not found.");
            }
            return writeSuccess(response, mapOf("message", "Task deleted."));
        } catch (SQLException e) {
            return writeError(response, 500, "Failed to delete task.");
        }
    }

    private static TaskInput normalizeTaskInput(TaskPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        String title = InputValidator.requireNonBlank("title", payload.title);
        String description = payload.description == null ? "" : payload.description.trim();

        String status = payload.status == null ? "todo" : payload.status.trim();
        String dbStatus = uiStatusToDb(status);
        if (dbStatus == null) {
            throw new IllegalArgumentException("Status must be one of: todo, in_progress, completed.");
        }

        String priority = normalizePriority(payload.priority);
        if (priority == null) {
            priority = "MEDIUM";
        }

        LocalDate dueDate;
        try {
            String dueDateRaw = InputValidator.requireNonBlank("dueDate", payload.dueDate);
            dueDate = LocalDate.parse(dueDateRaw);
        } catch (Exception e) {
            throw new IllegalArgumentException("dueDate must be in format YYYY-MM-DD.");
        }

        long diffDays = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        int deadlineDays = (int) Math.max(diffDays, 1);

        Integer providedRevenue = payload.revenue;
        int revenue = providedRevenue != null ? providedRevenue : revenueFromPriority(priority);
        revenue = InputValidator.requirePositive("revenue", revenue);

        return new TaskInput(title, description, dbStatus, priority, dueDate, deadlineDays, revenue);
    }

    private static int revenueFromPriority(String priority) {
        return switch (priority) {
            case "HIGH" -> 90000;
            case "LOW" -> 15000;
            default -> 40000;
        };
    }

    private static String dbStatusToUi(String status) {
        if (status == null) {
            return "todo";
        }

        return switch (status.toUpperCase(Locale.ROOT)) {
            case "SCHEDULED" -> "in_progress";
            case "COMPLETED" -> "completed";
            case "EXPIRED" -> "completed";
            default -> "todo";
        };
    }

    private static String uiStatusToDb(String status) {
        if (status == null) {
            return null;
        }

        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "todo" -> "PENDING";
            case "in_progress" -> "SCHEDULED";
            case "completed" -> "COMPLETED";
            case "expired" -> "EXPIRED";
            default -> null;
        };
    }

    private static String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }

        String normalized = priority.trim().toUpperCase(Locale.ROOT);
        if ("LOW".equals(normalized) || "MEDIUM".equals(normalized) || "HIGH".equals(normalized)) {
            return normalized;
        }

        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static AuthUser authenticate(Request request) {
        String header = request.headers("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        String token = header.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return null;
        }

        return TOKEN_STORE.get(token);
    }

    private static void haltUnauthorized() {
        throw halt(401, GSON.toJson(mapOf(
                "success", false,
                "message", "Unauthorized"
        )));
    }

    private static String writeSuccess(Response response, Object data) {
        response.status(200);
        return GSON.toJson(mapOf(
                "success", true,
                "data", data
        ));
    }

    private static String writeError(Response response, int status, String message) {
        response.status(status);
        return GSON.toJson(mapOf(
                "success", false,
                "message", message
        ));
    }

    private static void applyCorsHeaders(Response response) {
        response.header("Access-Control-Allow-Origin", "*");
        response.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length - 1; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private record AuthUser(String name, String email) {
    }

    private static final class SignupRequest {
        String name;
        String email;
        String password;
    }

    private static final class LoginRequest {
        String email;
        String password;
    }

    private static final class TaskPayload {
        String title;
        String description;
        String status;
        String priority;
        String dueDate;
        Integer revenue;
    }

    private record TaskInput(
            String title,
            String description,
            String dbStatus,
            String priority,
            LocalDate dueDate,
            int deadlineDays,
            int revenue
    ) {
    }
}
