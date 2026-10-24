package com.example.demo.service;

import com.example.demo.enums.ApplicationStatus;
import com.example.demo.enums.InternshipStatus;
import com.example.demo.repository.ApplicationRepository;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.InternshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final ApplicationRepository applications;
    private final InternshipRepository internships;
    private final CompanyRepository companies;

    public Map<String, Object> placementSummary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) byStatus.put(s.name(), 0L);
        long total = 0;
        for (Object[] row : applications.countByStatus()) {
            long n = (Long) row[1];
            byStatus.put(((ApplicationStatus) row[0]).name(), n);
            total += n;
        }

        Map<String, Long> listings = new LinkedHashMap<>();
        for (InternshipStatus s : InternshipStatus.values()) listings.put(s.name(), 0L);
        for (Object[] row : internships.countByStatus()) {
            listings.put(((InternshipStatus) row[0]).name(), (Long) row[1]);
        }

        long accepted = byStatus.get("ACCEPTED");
        long completed = byStatus.get("COMPLETED");
        long placed = accepted + completed;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalApplications", total);
        out.put("applicationsByStatus", byStatus);
        out.put("internshipsByStatus", listings);
        out.put("totalCompanies", companies.count());
        out.put("placementRatePercent", total == 0 ? 0.0 : round(placed * 100.0 / total));
        out.put("completionRatePercent", placed == 0 ? 0.0 : round(completed * 100.0 / placed));
        return out;
    }

    public String exportCsv() {
        StringBuilder sb = new StringBuilder("student,company,internship,status,applied_at\n");
        List<Object[]> rows = applications.exportRows();
        for (Object[] r : rows) {
            sb.append(csv(r[0])).append(',').append(csv(r[1])).append(',')
                    .append(csv(r[2])).append(',').append(csv(r[3])).append(',').append(csv(r[4])).append('\n');
        }
        return sb.toString();
    }

    private static double round(double v) { return Math.round(v * 10.0) / 10.0; }

    private static String csv(Object o) {
        String s = o == null ? "" : o.toString();
        // neutralise spreadsheet formula injection
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}