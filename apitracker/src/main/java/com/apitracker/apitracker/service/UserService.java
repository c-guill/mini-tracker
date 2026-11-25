package com.apitracker.apitracker.service;

import com.apitracker.apitracker.dto.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> findAll();

    UserResponse findById(String id);
}
