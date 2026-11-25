package com.transport.gestiontransport.service;

import com.transport.gestiontransport.dto.request.LoginRequest;
import com.transport.gestiontransport.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
