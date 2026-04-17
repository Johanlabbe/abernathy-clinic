package com.abernathy.front.controller;

import com.abernathy.front.beans.PatientBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;

@Controller
public class FrontController {

    private final RestClient restClient;

    public FrontController() {
        String encodedCredentials = Base64.getEncoder().encodeToString("medecin:password123".getBytes());

        this.restClient = RestClient.builder()
               .baseUrl("http://localhost:8080")
               .defaultHeader("Authorization", "Basic " + encodedCredentials)
               .build();
    }

    @GetMapping("/patients")
    public String listPatients(Model model) {
        try {
            List<PatientBean> patients = restClient.get()
                   .uri("/patient/all")
                   .retrieve()
                   .body(new ParameterizedTypeReference<List<PatientBean>>() {});
            
            model.addAttribute("patients", patients);
        } catch (Exception e) {
            model.addAttribute("error", "Impossible de récupérer les patients : " + e.getMessage());
        }
        return "patients";
    }
}