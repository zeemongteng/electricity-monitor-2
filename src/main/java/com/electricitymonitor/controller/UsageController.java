package com.electricitymonitor.controller;

import com.electricitymonitor.dto.Dtos.UsagePoint;
import com.electricitymonitor.model.PeriodType;
import com.electricitymonitor.service.UsageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    /** Usage per hour / day / month / year. Default counts: 24 hours, 30 days, 12 months, 5 years. */
    @GetMapping
    public List<UsagePoint> usage(@RequestParam Long meterId,
                                  @RequestParam PeriodType type,
                                  @RequestParam(required = false) Integer count) {
        int n = defaultCount(type);
        if (count != null) {
            n = count;
        }
        if (n < 1 || n > 400) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "count must be between 1 and 400");
        }
        return usageService.series(meterId, type, n);
    }

    private int defaultCount(PeriodType type) {
        switch (type) {
            case HOUR:
                return 24;
            case DAY:
                return 30;
            case MONTH:
                return 12;
            default:
                return 5;
        }
    }
}
