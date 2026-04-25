package com.abernathy.note.controller;

import com.abernathy.note.model.Note;
import com.abernathy.note.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/note")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/patient/{patId}")
    public ResponseEntity<List<Note>> getPatientNotes(@PathVariable Long patId) {
        return ResponseEntity.ok(noteService.getNotesByPatientId(patId));
    }

    @PostMapping("/add")
    public ResponseEntity<Note> addNote(@RequestBody Note note) {
        return new ResponseEntity<>(noteService.addNote(note), HttpStatus.CREATED);
    }
}