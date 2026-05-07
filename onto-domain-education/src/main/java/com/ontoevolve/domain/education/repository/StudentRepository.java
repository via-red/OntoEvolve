package com.ontoevolve.domain.education.repository;

import com.ontoevolve.domain.education.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    List<StudentEntity> findByStudentIdContainingIgnoreCase(String studentId);
    List<StudentEntity> findBySchool(String school);
}
