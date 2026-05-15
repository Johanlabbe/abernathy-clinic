package com.abernathy.assessment.bean;

import java.time.LocalDate;

public record PatientBean(
        Long id,
        String prenom,
        String nom,
        LocalDate dateNaissance,
        String genre,
        String adresse,
        String telephone
) {}
