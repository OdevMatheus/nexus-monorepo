package com.matheushenrique.nexum.services.impl;

import com.matheushenrique.nexum.dtos.request.CreateClientRequest;
import com.matheushenrique.nexum.dtos.request.UpdateClientRequest;
import com.matheushenrique.nexum.dtos.response.ClientResponse;
import com.matheushenrique.nexum.dtos.response.MessageResponse;
import com.matheushenrique.nexum.dtos.response.PageResponse;
import com.matheushenrique.nexum.entities.Client;
import com.matheushenrique.nexum.repositories.ClientRepository;
import com.matheushenrique.nexum.security.exceptions.ClientNotFoundException;
import com.matheushenrique.nexum.security.exceptions.EmailAlreadyInUseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> findAll(int page, int size, String search) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        var result = clientRepository.findAllActiveWithSearch(
                search == null || search.isBlank() ? null : search,
                pageable
        );
        return PageResponse.from(result.map(ClientResponse::from));
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(UUID id) {
        return clientRepository.findByIdAndActiveTrue(id)
                .map(ClientResponse::from)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));
    }

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        if (clientRepository.existsByEmailAndActiveTrue(request.email())) {
            throw new EmailAlreadyInUseException("Email already in use");
        }

        Client client = Client.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .document(request.document())
                .build();

        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse update(UUID id, UpdateClientRequest request) {
        Client client = clientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));

        if (clientRepository.existsByEmailAndActiveTrueAndIdNot(request.email(), id)) {
            throw new EmailAlreadyInUseException("Email already in use");
        }

        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setDocument(request.document());

        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public MessageResponse deactivate(UUID id) {
        Client client = clientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));

        client.setActive(false);
        clientRepository.save(client);

        return new MessageResponse("Client deactivated successfully");
    }
}