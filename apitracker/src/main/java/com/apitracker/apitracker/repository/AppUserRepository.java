package com.apitracker.apitracker.repository;

import com.apitracker.apitracker.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
}
