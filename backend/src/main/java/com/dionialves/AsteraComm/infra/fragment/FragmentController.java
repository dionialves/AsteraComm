package com.dionialves.AsteraComm.infra.fragment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Endpoint para o fragment search-select.
 * Cada domínio registra sua própria implementação de SearchOptionsProvider.
 * A chave do mapa é o valor do atributo data-field do input de busca
 * (ex: "trunkId", "planId", "customerId", "circuitId").
 */
@Controller
@RequestMapping("/fragments")
public class FragmentController {

    private final Map<String, SearchOptionsProvider> providers;

    @Autowired
    public FragmentController(Map<String, SearchOptionsProvider> providers) {
        this.providers = providers;
    }

    @GetMapping("/search-options")
    public String searchOptions(@RequestParam String field,
                                @RequestParam(defaultValue = "") String q,
                                Model model) {
        SearchOptionsProvider provider = providers.get(field);
        List<SearchOptionDTO> options = provider != null ? provider.search(q) : List.of();
        model.addAttribute("options", options);
        return "fragments/search-select :: search-options(options=${options})";
    }

    @GetMapping("/search-options-json")
    @ResponseBody
    public List<SearchOptionDTO> searchOptionsJson(@RequestParam String field,
                                                   @RequestParam(defaultValue = "") String q) {
        SearchOptionsProvider provider = providers.get(field);
        return provider != null ? provider.search(q) : List.of();
    }
}
