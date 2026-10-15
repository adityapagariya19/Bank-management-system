package com.meridian.web;

import com.meridian.dto.Dtos.*;
import com.meridian.service.DbExplorerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/db")
public class DbController {
    private final DbExplorerService db;

    public DbController(DbExplorerService db) {
        this.db = db;
    }

    @GetMapping("/tables") public List<TableInfo> tables() { return db.tables(); }
    @GetMapping("/tables/{name}") public TableData table(@PathVariable String name, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "15") int size) { return db.browse(name, page, size); }
    @GetMapping("/relations") public List<Relation> relations() { return db.relations(); }
    @PostMapping("/query") public QueryResult query(@Valid @RequestBody QueryRequest r) { return db.query(r.sql()); }
}
