package com.abernathy.patient.service;

import com.abernathy.patient.model.Patient;
import com.abernathy.patient.repository.PatientRepository;

import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient introuvable"));
    }


    public Patient savePatient(@NotNull Patient patient) {
        return patientRepository.save(patient);
    }

    public Patient updatePatient(Long id, Patient patientDetails) {
        Patient patient = getPatientById(id);
        patient.setPrenom(patientDetails.getPrenom());
        patient.setNom(patientDetails.getNom());
        patient.setDateNaissance(patientDetails.getDateNaissance());
        patient.setGenre(patientDetails.getGenre());
        patient.setAdresse(patientDetails.getAdresse());
        patient.setTelephone(patientDetails.getTelephone());
        return patientRepository.save(patient);
    }
}