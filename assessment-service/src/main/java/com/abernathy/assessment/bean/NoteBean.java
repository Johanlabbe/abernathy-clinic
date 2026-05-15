package com.abernathy.assessment.bean;

public record NoteBean(
        String id,
        Long patId,
        String patient,
        String note
) {}
