import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Console UI: menu-driven program entry point.
 */
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DatabaseHelper db = new DatabaseHelper();
    private static final StudentManager manager = new StudentManager(db);

    public static void main(String[] args) {
        System.out.println("=== Smart Student Information & Performance Track (SQLite) ===");
        boolean exit = false;
        while (!exit) {
            printMenu();
            int choice = readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1 -> addStudentFlow();
                    case 2 -> updateStudentFlow();
                    case 3 -> deleteStudentFlow();
                    case 4 -> searchStudentFlow();
                    case 5 -> subjectManagementFlow();
                    case 6 -> addMarksFlow();
                    case 7 -> displayResultFlow();
                    case 8 -> attendanceFlow();
                    case 9 -> topPerformersFlow();
                    case 10 -> summaryReportFlow();
                    case 11 -> listStudentsFlow();
                    case 12 -> { exit = true; System.out.println("Exiting... Goodbye!"); }
                    default -> System.out.println("Invalid choice. Try again.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Input error: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("\n---- Menu ----");
        System.out.println("1. Add Student");
        System.out.println("2. Update Student");
        System.out.println("3. Delete Student");
        System.out.println("4. Search Student");
        System.out.println("5. Manage Subjects (list/add)");
        System.out.println("6. Add/Update Marks");
        System.out.println("7. Display Student Result Card");
        System.out.println("8. Attendance Tracking");
        System.out.println("9. Top Performers & Ranking");
        System.out.println("10. Summary Report");
        System.out.println("11. List All Students");
        System.out.println("12. Exit");
    }

    // --- Flows ---
    private static void addStudentFlow() throws SQLException {
        System.out.println("\n[Add Student]");
        String name = readLine("Name: ");
        int age = readInt("Age: ");
        String course = readLine("Course: ");
        String roll = readLine("Roll Number: ");

        Student s = manager.addStudent(name, age, course, roll);
        System.out.println("Added. Assigned ID: " + s.getId());
    }

    private static void updateStudentFlow() throws SQLException {
        System.out.println("\n[Update Student]");
        int id = readInt("Student ID: ");
        System.out.println("Leave blank to skip a field.");
        String name = readLineAllowBlank("New Name: ");
        String ageStr = readLineAllowBlank("New Age: ");
        String course = readLineAllowBlank("New Course: ");
        String roll = readLineAllowBlank("New Roll Number: ");

        Integer age = null;
        if (!ageStr.isBlank()) {
            try { age = Integer.parseInt(ageStr.trim()); }
            catch (NumberFormatException e) { System.out.println("Invalid age. Skipping age update."); }
        }
        boolean ok = manager.updateStudent(id,
                name.isBlank() ? null : name,
                age,
                course.isBlank() ? null : course,
                roll.isBlank() ? null : roll);
        System.out.println(ok ? "Updated." : "Student not found.");
    }

    private static void deleteStudentFlow() throws SQLException {
        System.out.println("\n[Delete Student]");
        int id = readInt("Student ID: ");
        boolean ok = manager.deleteStudent(id);
        System.out.println(ok ? "Deleted." : "Student not found.");
    }

    private static void searchStudentFlow() throws SQLException {
        System.out.println("\n[Search Student]");
        System.out.println("1) By ID  2) By Name  3) By Roll");
        int opt = readInt("Option: ");
        switch (opt) {
            case 1 -> {
                int id = readInt("Enter ID: ");
                Student s = manager.findById(id);
                System.out.println(s != null ? s : "No student found.");
            }
            case 2 -> {
                String name = readLine("Enter name query: ");
                List<Student> list = manager.searchByName(name);
                if (list.isEmpty()) System.out.println("No students found.");
                else list.forEach(System.out::println);
            }
            case 3 -> {
                String roll = readLine("Enter roll: ");
                Student s = manager.findByRoll(roll);
                System.out.println(s != null ? s : "No student found.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private static void subjectManagementFlow() throws SQLException {
        System.out.println("\n[Subjects]");
        System.out.println("Existing subjects:");
        List<String> subs = manager.listSubjects();
        if (subs.isEmpty()) System.out.println("  (none)");
        else subs.forEach(s -> System.out.println("  - " + s));

        String add = readLine("Add a new subject? (y/n): ");
        if (add.equalsIgnoreCase("y")) {
            String name = readLine("Subject name: ");
            int id = db.ensureSubject(name);
            System.out.println("Subject ensured with ID: " + id);
        }
    }

    private static void addMarksFlow() throws SQLException {
        System.out.println("\n[Add/Update Marks]");
        int id = readInt("Student ID: ");
        Student s = manager.findById(id);
        if (s == null) { System.out.println("Student not found."); return; }

        boolean more = true;
        while (more) {
            String subject = readLine("Subject: ");
            double marks = readDouble("Marks (0-100): ");
            boolean ok = manager.addOrUpdateMark(id, subject, marks);
            System.out.println(ok ? "Marks saved." : "Failed to save marks.");
            String cont = readLine("Add more subjects? (y/n): ");
            more = cont.equalsIgnoreCase("y");
        }

        Performance p = manager.getPerformance(id);
        System.out.println("Current Performance: " + p);
    }

    private static void displayResultFlow() throws SQLException {
        System.out.println("\n[Result Card]");
        int id = readInt("Student ID: ");
        System.out.println(manager.buildResultCard(id));
    }

    private static void attendanceFlow() throws SQLException {
        System.out.println("\n[Attendance]");
        int id = readInt("Student ID: ");
        int addTotal = readInt("Classes held (to add): ");
        int addPresent = readInt("Classes attended (to add): ");
        boolean ok = manager.updateAttendance(id, addPresent, addTotal);
        if (!ok) System.out.println("Student not found.");
        else {
            double pct = db.getAttendancePercentage(id);
            System.out.printf("Attendance now: %.2f%%%n", pct);
            if (pct < 75.0) System.out.println("Warning: Attendance below 75%.");
        }
    }

    private static void topPerformersFlow() throws SQLException {
        System.out.println("\n[Top Performers & Ranking]");
        int count = readInt("How many top students to list? ");
        List<Student> ranked = manager.getRankedStudents();
        if (ranked.isEmpty()) { System.out.println("No students available."); return; }

        int rank = 1;
        for (Student s : ranked) {
            if (rank > Math.max(count, 0)) break;
            double pct = db.getPercentageForStudent(s.getId());
            String grade = db.getGradeForPercentage(pct);
            double att = db.getAttendancePercentage(s.getId());
            System.out.printf("#%d ID %d - %s | %%: %.2f | Grade: %s | Attendance: %.2f%%%n",
                    rank++,
                    s.getId(),
                    s.getName(),
                    pct,
                    grade,
                    att);
        }
    }

    private static void summaryReportFlow() throws SQLException {
        System.out.println(manager.buildSummaryReport());
        System.out.println("[All Students]");
        for (Student s : manager.getAllStudents()) {
            double pct = db.getPercentageForStudent(s.getId());
            double att = db.getAttendancePercentage(s.getId());
            String grade = db.getGradeForPercentage(pct);
            System.out.printf("ID %d - %s | %%: %.2f | Grade: %s | Attendance: %.2f%%%n",
                    s.getId(), s.getName(), pct, grade, att);
        }
    }

    private static void listStudentsFlow() throws SQLException {
        System.out.println("\n[Students]");
        List<Student> list = manager.getAllStudents();
        if (list.isEmpty()) { System.out.println("No students found."); return; }
        for (Student s : list) System.out.println(s);
    }

    // --- IO Helpers ---
    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String readLineAllowBlank(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try { return Integer.parseInt(line); }
            catch (NumberFormatException e) { System.out.println("Please enter a valid integer."); }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try { return Double.parseDouble(line); }
            catch (NumberFormatException e) { System.out.println("Please enter a valid number."); }
        }
    }
}
