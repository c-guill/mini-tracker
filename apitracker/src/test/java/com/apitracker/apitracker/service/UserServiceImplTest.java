package com.apitracker.apitracker.service;

import com.apitracker.apitracker.domain.AppUser;
import com.apitracker.apitracker.dto.UserResponse;
import com.apitracker.apitracker.exception.ResourceNotFoundException;
import com.apitracker.apitracker.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(appUserRepository, new TicketMapper());
    }

    @Test
    void findAll_shouldReturnMappedUsers() {
        List<AppUser> users = List.of(
                new AppUser("user-jd", "John Doe", "JD", "#00875a"),
                new AppUser("user-sc", "Sarah Connor", "SC", "#6554c0")
        );
        when(appUserRepository.findAll()).thenReturn(users);

        List<UserResponse> result = userService.findAll();

        assertEquals(2, result.size());
        assertEquals(new UserResponse("user-jd", "John Doe", "JD", "#00875a"), result.get(0));
        assertEquals(new UserResponse("user-sc", "Sarah Connor", "SC", "#6554c0"), result.get(1));
        verify(appUserRepository).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoUsersExist() {
        when(appUserRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = userService.findAll();

        assertTrue(result.isEmpty());
        verify(appUserRepository).findAll();
    }

    @Test
    void findById_shouldReturnUserWhenFound() {
        AppUser user = new AppUser("user-ad", "Alex Dev", "AD", "#0052cc");
        when(appUserRepository.findById("user-ad")).thenReturn(Optional.of(user));

        UserResponse result = userService.findById("user-ad");

        assertEquals("user-ad", result.id());
        assertEquals("Alex Dev", result.name());
        assertEquals("AD", result.initials());
        assertEquals("#0052cc", result.color());
    }

    @Test
    void findById_shouldThrowResourceNotFoundExceptionWhenMissing() {
        when(appUserRepository.findById("unknown")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findById("unknown")
        );

        assertTrue(ex.getMessage().contains("unknown"));
    }
}
