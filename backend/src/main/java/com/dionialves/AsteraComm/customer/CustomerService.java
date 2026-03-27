package com.dionialves.AsteraComm.customer;

import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.customer.dto.CustomerCreateDTO;
import com.dionialves.AsteraComm.customer.dto.CustomerResponseDTO;
import com.dionialves.AsteraComm.customer.dto.CustomerSummaryDTO;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CircuitRepository circuitRepository;

    public Page<CustomerResponseDTO> getAll(String search, Boolean enabled, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        Page<Customer> page;
        if (enabled != null && hasSearch) {
            page = customerRepository.findByEnabledAndNameContainingIgnoreCase(enabled, search, pageable);
        } else if (enabled != null) {
            page = customerRepository.findByEnabled(enabled, pageable);
        } else if (hasSearch) {
            page = customerRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            page = customerRepository.findAll(pageable);
        }
        return page.map(this::toResponseDTO);
    }

    private CustomerResponseDTO toResponseDTO(Customer c) {
        int count = (int) circuitRepository.countByCustomerId(c.getId());
        return new CustomerResponseDTO(c.getId(), c.getName(), c.isEnabled(), count, c.getCreatedAt(),
                c.getUpdatedAt());
    }

    public List<CustomerSummaryDTO> findAllSummary() {
        return customerRepository.findAllSummary();
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public Customer create(CustomerCreateDTO dto) {
        Customer customer = new Customer();

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("O nome do cliente é obrigatório.");
        }

        if (dto.getName().length() < 5 || dto.getName().length() > 100) {
            throw new BusinessException("O nome do cliente deve conter entre 5 e 100 caracteres.");
        }

        customer.setName(dto.getName());
        customer.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    public Customer update(Long id, CustomerCreateDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
        customer.setName(dto.getName());
        customer.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        customer.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
        if (circuitRepository.existsByCustomerId(id)) {
            throw new BusinessException("Cliente possui circuito(s) vinculado(s). Use a desativação.");
        }
        customerRepository.delete(customer);
    }

    public Customer disable(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
        customer.setEnabled(false);
        customer.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }
}
