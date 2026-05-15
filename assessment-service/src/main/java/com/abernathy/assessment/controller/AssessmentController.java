package com.abernathy.assessment.controller;

import com.abernathy.assessment.service.AssessmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assessment")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/{patId}")
    public ResponseEntity<String> getAssessment(@PathVariable Long patId) {
        String result = assessmentService.assess(patId);
        return ResponseEntity.ok(result);
    }
}
