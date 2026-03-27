package com.dionialves.AsteraComm.infra.fragment;

import java.util.List;

/**
 * Interface implementada por cada serviço de domínio que precisa
 * ser pesquisável via search-select.
 *
 * O bean deve ser registrado no contexto Spring com o nome igual
 * ao valor do data-field do input (ex: "trunkId", "planId").
 *
 * Exemplo:
 *   @Service("trunkId")
 *   public class TrunkSearchOptionsProvider implements SearchOptionsProvider { ... }
 */
public interface SearchOptionsProvider {
    List<SearchOptionDTO> search(String query);
}
