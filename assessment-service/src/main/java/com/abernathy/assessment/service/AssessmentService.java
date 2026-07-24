package com.abernathy.assessment.service;

import com.abernathy.assessment.bean.NoteBean;
import com.abernathy.assessment.bean.PatientBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class AssessmentService {

    private static final List<String> TRIGGER_TERMS = List.of(
            "Hémoglobine A1C",
            "Microalbumine",
            "Taille",
            "Poids",
            "Fumeur",
            "Fumeuse",
            "Anormal",
            "Cholestérol",
            "Vertige",
            "Rechute",
            "Réaction",
            "Anticorps"
    );

    private final RestClient restClient;

    public AssessmentService(RestClient gatewayRestClient) {
        this.restClient = gatewayRestClient;
    }

    public String assess(Long patId) {
        PatientBean patient = restClient.get()
                .uri("/patient/" + patId)
                .retrieve()
                .body(PatientBean.class);

        List<NoteBean> notes = restClient.get()
                .uri("/note/patient/" + patId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<NoteBean>>() {});

        int age = Period.between(patient.dateNaissance(), LocalDate.now()).getYears();
        String gender = patient.genre();
        int triggerCount = countTriggers(notes);

        return determineRisk(age, gender, triggerCount);
    }

    private int countTriggers(List<NoteBean> notes) {
        if (notes == null || notes.isEmpty()) return 0;

        String allNotes = notes.stream()
                .map(NoteBean::note)
                .reduce("", (a, b) -> a + " " + b)
                .toLowerCase();

        int count = 0;
        for (String term : TRIGGER_TERMS) {
            if (allNotes.contains(term.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private String determineRisk(int age, String gender, int triggerCount) {
        boolean isYoung = age < 30;
        boolean isMale = "M".equalsIgnoreCase(gender);

        if (isYoung) {
            if (isMale) {
                if (triggerCount >= 5) return "EarlyOnset";
                if (triggerCount >= 3) return "InDanger";
            } else {
                if (triggerCount >= 7) return "EarlyOnset";
                if (triggerCount >= 4) return "InDanger";
            }
            return "None";
        } else {
            if (triggerCount >= 8) return "EarlyOnset";
            if (triggerCount >= 6) return "InDanger";
            if (triggerCount >= 2) return "Borderline";
            return "None";
        }
    }
}
