import java.sql.*;
import java.util.*;

/**
 * Handles SQLite connectivity, schema creation, and CRUD queries.
 */
public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:students.db";

    public DatabaseHelper() {
        try (Connection conn = getConnection()) {
            createSchema(conn);
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void createSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS students(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT NOT NULL,
                  age INTEGER NOT NULL,
                  course TEXT NOT NULL,
                  roll TEXT NOT NULL UNIQUE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS subjects(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT NOT NULL UNIQUE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS marks(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  student_id INTEGER NOT NULL,
                  subject_id INTEGER NOT NULL,
                  marks REAL NOT NULL,
                  UNIQUE(student_id, subject_id),
                  FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE,
                  FOREIGN KEY(subject_id) REFERENCES subjects(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS attendance(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  student_id INTEGER NOT NULL UNIQUE,
                  present INTEGER NOT NULL DEFAULT 0,
                  total INTEGER NOT NULL DEFAULT 0,
                  FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE
                );
            """);
        }
    }

    // --- Student CRUD ---
    public int insertStudent(Student s) throws SQLException {
        String sql = "INSERT INTO students(name, age, course, roll) VALUES(?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setInt(2, s.getAge());
            ps.setString(3, s.getCourse());
            ps.setString(4, s.getRoll());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    s.setId(id);
                    ensureAttendanceRow(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to insert student.");
    }

    public boolean updateStudent(Student s) throws SQLException {
        String sql = "UPDATE students SET name=?, age=?, course=?, roll=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setInt(2, s.getAge());
            ps.setString(3, s.getCourse());
            ps.setString(4, s.getRoll());
            ps.setInt(5, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Student getStudentById(int id) throws SQLException {
        String sql = "SELECT id, name, age, course, roll FROM students WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            rs.getString("course"),
                            rs.getString("roll"));
                }
            }
        }
        return null;
    }

    public Student getStudentByRoll(String roll) throws SQLException {
        String sql = "SELECT id, name, age, course, roll FROM students WHERE roll=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roll);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            rs.getString("course"),
                            rs.getString("roll"));
                }
            }
        }
        return null;
    }

    public List<Student> searchStudentsByName(String query) throws SQLException {
        String sql = "SELECT id, name, age, course, roll FROM students WHERE lower(name) LIKE ? ORDER BY name ASC";
        List<Student> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Student(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            rs.getString("course"),
                            rs.getString("roll")));
                }
            }
        }
        return list;
    }

    public List<Student> getAllStudents() throws SQLException {
        String sql = "SELECT id, name, age, course, roll FROM students ORDER BY id ASC";
        List<Student> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("course"),
                        rs.getString("roll")));
            }
        }
        return list;
    }

    // --- Subjects ---
    public int ensureSubject(String name) throws SQLException {
        String sel = "SELECT id FROM subjects WHERE name=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String ins = "INSERT INTO subjects(name) VALUES(?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to ensure subject.");
    }

    public List<String> listSubjects() throws SQLException {
        String sql = "SELECT name FROM subjects ORDER BY name ASC";
        List<String> subjects = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) subjects.add(rs.getString("name"));
        }
        return subjects;
    }

    // --- Marks ---
    public void upsertMark(int studentId, String subjectName, double marks) throws SQLException {
        int subjectId = ensureSubject(subjectName);
        String upsert = """
            INSERT INTO marks(student_id, subject_id, marks) VALUES(?, ?, ?)
            ON CONFLICT(student_id, subject_id) DO UPDATE SET marks=excluded.marks
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(upsert)) {
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ps.setDouble(3, marks);
            ps.executeUpdate();
        }
    }

    public Map<String, Double> getMarksForStudent(int studentId) throws SQLException {
        String sql = """
            SELECT subjects.name AS subject, marks.marks AS marks
            FROM marks
            JOIN subjects ON subjects.id = marks.subject_id
            WHERE marks.student_id = ?
            ORDER BY subjects.name ASC
        """;
        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("subject"), rs.getDouble("marks"));
                }
            }
        }
        return map;
    }

    // --- Attendance ---
    private void ensureAttendanceRow(int studentId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO attendance(student_id, present, total) VALUES(?, 0, 0)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        }
    }

    public void addAttendance(int studentId, int addPresent, int addTotal) throws SQLException {
        String sql = """
            UPDATE attendance
            SET present = present + ?, total = total + ?
            WHERE student_id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addPresent);
            ps.setInt(2, addTotal);
            ps.setInt(3, studentId);
            ps.executeUpdate();
        }
    }

    public int[] getAttendance(int studentId) throws SQLException {
        String sql = "SELECT present, total FROM attendance WHERE student_id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("present"), rs.getInt("total")};
                }
            }
        }
        return new int[]{0, 0};
    }

    // --- Analytics ---
    public double getPercentageForStudent(int studentId) throws SQLException {
        Map<String, Double> marks = getMarksForStudent(studentId);
        if (marks.isEmpty()) return 0.0;
        double total = marks.values().stream().mapToDouble(Double::doubleValue).sum();
        return total / marks.size();
    }

    public String getGradeForPercentage(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B+";
        if (pct >= 60) return "B";
        if (pct >= 50) return "C";
        return "F";
    }

    public List<Student> getRankedStudents() throws SQLException {
        // Compute ranking: percentage desc, attendance desc
        List<Student> students = getAllStudents();
        students.sort((a, b) -> {
            try {
                double pctA = getPercentageForStudent(a.getId());
                double pctB = getPercentageForStudent(b.getId());
                int cmpPct = Double.compare(pctB, pctA);
                if (cmpPct != 0) return cmpPct;

                double attA = getAttendancePercentage(a.getId());
                double attB = getAttendancePercentage(b.getId());
                return Double.compare(attB, attA);
            } catch (SQLException e) {
                return 0;
            }
        });
        return students;
    }

    public double getAttendancePercentage(int studentId) throws SQLException {
        int[] at = getAttendance(studentId);
        int present = at[0], total = at[1];
        return total == 0 ? 0.0 : (present * 100.0) / total;
    }

    public double getClassAveragePercentage() throws SQLException {
        List<Student> students = getAllStudents();
        if (students.isEmpty()) return 0.0;
        double sum = 0.0;
        int count = 0;
        for (Student s : students) {
            sum += getPercentageForStudent(s.getId());
            count++;
        }
        return count == 0 ? 0.0 : sum / count;
    }

    public long getPassCount() throws SQLException {
        List<Student> students = getAllStudents();
        long pass = 0;
        for (Student s : students) {
            double pct = getPercentageForStudent(s.getId());
            if (pct >= 50.0) pass++;
        }
        return pass;
    }

    public Student getHighestScorer() throws SQLException {
        List<Student> students = getAllStudents();
        Student best = null;
        double bestPct = -1;
        for (Student s : students) {
            double pct = getPercentageForStudent(s.getId());
            if (pct > bestPct) {
                bestPct = pct;
                best = s;
            }
        }
        return best;
    }

    public double getAverageAttendancePercentage() throws SQLException {
        List<Student> students = getAllStudents();
        if (students.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Student s : students) {
            sum += getAttendancePercentage(s.getId());
        }
        return sum / students.size();
    }
}
