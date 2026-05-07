package com.ontoevolve.domain.education.controller;

import com.ontoevolve.domain.education.entity.StudentEntity;
import com.ontoevolve.domain.education.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentApiController {

    private final StudentRepository studentRepository;

    public StudentApiController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public ResponseEntity<List<StudentEntity>> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentEntity> getStudent(@PathVariable Long id) {
        return studentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<StudentEntity>> searchStudents(@RequestParam("q") String q) {
        return ResponseEntity.ok(studentRepository.findByStudentIdContainingIgnoreCase(q));
    }
}
