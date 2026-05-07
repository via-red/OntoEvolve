package com.ontoevolve.domain.education.data;

import com.ontoevolve.domain.education.entity.StudentEntity;
import com.ontoevolve.domain.education.repository.StudentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入 UCI Student Performance CSV 数据到数据库。
 */
@Service
public class StudentDataImporter {

    private static final Logger log = LoggerFactory.getLogger(StudentDataImporter.class);
    private final StudentRepository studentRepository;

    public StudentDataImporter(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @PostConstruct
    public void autoImport() {
        if (studentRepository.count() > 0) {
            log.info("学生数据已存在 ({} 条)，跳过导入", studentRepository.count());
            return;
        }

        // 尝试多个可能的路径
        String[] paths = {
            "data/student-data/student-mat.csv",
            "../data/student-data/student-mat.csv",
            "../../data/student-data/student-mat.csv"
        };

        for (String basePath : paths) {
            String matPath = basePath;
            String porPath = basePath.replace("student-mat.csv", "student-por.csv");
            try {
                int matCount = importCsv(matPath, "GP");
                int porCount = importCsv(porPath, "MS");
                log.info("学生数据导入完成：数学 {} 人，葡萄牙语 {} 人，共 {} 人",
                        matCount, porCount, matCount + porCount);
                return;
            } catch (Exception e) {
                log.debug("路径 {} 不可用: {}", basePath, e.getMessage());
            }
        }

        // 最后的回退：尝试 classpath
        log.warn("尝试从 classpath 导入学生数据...");
        try {
            int matCount = importCsvFromClasspath("student-mat.csv", "GP");
            int porCount = importCsvFromClasspath("student-por.csv", "MS");
            log.info("学生数据导入完成（classpath）：数学 {} 人，葡萄牙语 {} 人，共 {} 人",
                    matCount, porCount, matCount + porCount);
        } catch (Exception e2) {
            log.warn("学生数据导入失败（不影响系统运行，仅演示数据不可用）: {}", e2.getMessage());
        }
    }

    public int importCsv(String filePath, String schoolPrefix) {
        List<StudentEntity> students = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String header = br.readLine(); // skip header
            if (header == null) return 0;
            String line;
            int idx = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                students.add(parseLine(line, schoolPrefix, idx++));
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV 导入失败: " + filePath, e);
        }
        studentRepository.saveAll(students);
        return students.size();
    }

    private int importCsvFromClasspath(String resourceName, String schoolPrefix) {
        List<StudentEntity> students = new ArrayList<>();
        InputStream is = getClass().getClassLoader().getResourceAsStream("data/" + resourceName);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(resourceName);
        }
        if (is == null) {
            log.warn("classpath 中未找到 {}", resourceName);
            return 0;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            br.readLine(); // header
            String line;
            int idx = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                students.add(parseLine(line, schoolPrefix, idx++));
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV 导入失败(classpath): " + resourceName, e);
        }
        studentRepository.saveAll(students);
        return students.size();
    }

    private StudentEntity parseLine(String line, String schoolPrefix, int idx) {
        // CSV with semicolon delimiter, fields may be quoted with "
        String[] parts = parseCsvLine(line);
        StudentEntity s = new StudentEntity();
        s.setSchool(getString(parts, 0, schoolPrefix));
        s.setSex(getString(parts, 1, "F"));
        s.setAge(getInt(parts, 2, 16));
        s.setAddress(getString(parts, 3, "U"));
        s.setFamsize(getString(parts, 4, "LE3"));
        s.setPstatus(getString(parts, 5, "T"));
        s.setMedu(getInt(parts, 6, 2));
        s.setFedu(getInt(parts, 7, 2));
        s.setMjob(getString(parts, 8, "other"));
        s.setFjob(getString(parts, 9, "other"));
        s.setReason(getString(parts, 10, "other"));
        s.setGuardian(getString(parts, 11, "mother"));
        s.setTraveltime(getInt(parts, 12, 1));
        s.setStudytime(getInt(parts, 13, 2));
        s.setFailures(getInt(parts, 14, 0));
        s.setSchoolsup(getString(parts, 15, "no"));
        s.setFamsup(getString(parts, 16, "no"));
        s.setPaid(getString(parts, 17, "no"));
        s.setActivities(getString(parts, 18, "no"));
        s.setNursery(getString(parts, 19, "yes"));
        s.setHigher(getString(parts, 20, "yes"));
        s.setInternet(getString(parts, 21, "no"));
        s.setRomantic(getString(parts, 22, "no"));
        s.setFamrel(getInt(parts, 23, 4));
        s.setFreetime(getInt(parts, 24, 3));
        s.setGoout(getInt(parts, 25, 3));
        s.setDalc(getInt(parts, 26, 1));
        s.setWalc(getInt(parts, 27, 1));
        s.setHealth(getInt(parts, 28, 4));
        s.setAbsences(getInt(parts, 29, 0));
        s.setG1(getInt(parts, 30, 0));
        s.setG2(getInt(parts, 31, 0));
        s.setG3(getInt(parts, 32, 0));
        String gender = "F".equals(s.getSex()) ? "F" : "M";
        s.setStudentId("S" + s.getSchool() + gender + String.format("%04d", idx + 1));
        return s;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ';' && !inQuotes) {
                fields.add(current.toString().trim().replaceAll("^\"|\"$", ""));
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim().replaceAll("^\"|\"$", ""));
        return fields.toArray(new String[0]);
    }

    private String getString(String[] parts, int idx, String def) {
        return idx < parts.length && !parts[idx].isEmpty() ? parts[idx] : def;
    }

    private int getInt(String[] parts, int idx, int def) {
        if (idx < parts.length && !parts[idx].isEmpty()) {
            try { return Integer.parseInt(parts[idx]); } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
