package com.abernathy.front.beans;

public record NoteBean(
    String id,
    Long patId,
    String patient,
    String note
) {}