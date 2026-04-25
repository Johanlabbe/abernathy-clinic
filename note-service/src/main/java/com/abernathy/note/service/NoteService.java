package com.abernathy.note.service;

import com.abernathy.note.model.Note;
import com.abernathy.note.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<Note> getNotesByPatientId(Long patId) {
        return noteRepository.findByPatId(patId);
    }

    public Note addNote(Note note) {
        return noteRepository.save(note);
    }
}