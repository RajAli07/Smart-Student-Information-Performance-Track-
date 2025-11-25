import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents computed performance details for a student.
 * Marks are provided as a map subject -> marks (0..100). Totals are computed.
 */
public class Performance {
    private final Map<String, Double> marksBySubject = new LinkedHashMap<>();
    private double total;
    private double percentage;
    private String grade;

    public void putMark(String subject, double marks) {
        marksBySubject.put(subject, marks);
        recalc();
    }

    public Map<String, Double> getMarks() { return marksBySubject; }
    public double getTotal() { return total; }
    public double getPercentage() { return percentage; }
    public String getGrade() { return grade; }

    private void recalc() {
        total = marksBySubject.values().stream().mapToDouble(Double::doubleValue).sum();
        int n = marksBySubject.size();
        percentage = (n == 0) ? 0.0 : total / n;
        grade = calculateGrade(percentage);
    }

    private String calculateGrade(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B+";
        if (pct >= 60) return "B";
        if (pct >= 50) return "C";
        return "F";
    }

    @Override
    public String toString() {
        return String.format("Performance{Total=%.2f, Percentage=%.2f, Grade=%s, Marks=%s}",
                total, percentage, grade, marksBySubject);
    }
}
