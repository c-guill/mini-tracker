package com.apitracker.apitracker.service;

import com.apitracker.apitracker.domain.AppUser;
import com.apitracker.apitracker.dto.UserResponse;
import com.apitracker.apitracker.exception.ResourceNotFoundException;
import com.apitracker.apitracker.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final AppUserRepository appUserRepository;
    private final TicketMapper ticketMapper;

    public UserServiceImpl(AppUserRepository appUserRepository, TicketMapper ticketMapper) {
        this.appUserRepository = appUserRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    public List<UserResponse> findAll() {
        return appUserRepository.findAll().stream()
                .map(ticketMapper::toUserResponse)
                .toList();
    }

    @Override
    public UserResponse findById(String id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id : " + id));
        return ticketMapper.toUserResponse(user);
    }
}
