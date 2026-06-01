package com.oscar.backend.controller;

import com.oscar.backend.mapper.DbpingMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class DbPingController {

    private final DbpingMapper dbpingMapper;

    public  DbPingController(DbpingMapper dbpingMapper) {
        this.dbpingMapper = dbpingMapper;
    }

    @GetMapping("/api/db/ping")
    public Map<String, Object> pingDatabase(){
        Integer result = dbpingMapper.ping();

        return Map.of(
                "status","OK",
                "database", "hpairdb",
                "result", result,
                "time", LocalDateTime.now().toString()
        );
    }
}
