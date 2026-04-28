package edu.jlu.controllers;

import edu.jlu.models.SleepHealthRecord;
import edu.jlu.services.SleepHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/sleep")
public class RecordDetailController {

    @Autowired
    private SleepHealthService sleepHealthService;

    @GetMapping("/record/{personId}")
    public String recordDetailPage(@PathVariable Integer personId, Model model) {
        SleepHealthRecord record = sleepHealthService.getRecordById(personId);
        model.addAttribute("record", record);
        return "pages/record-detail";
    }
}
