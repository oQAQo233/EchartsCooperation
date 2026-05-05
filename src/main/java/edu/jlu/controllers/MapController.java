package edu.jlu.controllers;

import edu.jlu.models.CountryDistribution;
import edu.jlu.services.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/sleep/map")
public class MapController {

    @Autowired
    private MapService mapService;

    @GetMapping
    public String mapPage() {
        return "pages/world-map";
    }

    @GetMapping("/api/country-data")
    @ResponseBody
    public List<CountryDistribution> getCountryData() {
        return mapService.getCountryDistribution();
    }
}
