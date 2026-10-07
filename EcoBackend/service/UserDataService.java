package backend.example.EcoBackend.service;

import backend.example.EcoBackend.entity.UserData;
import backend.example.EcoBackend.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserDataService {
    @Autowired
    private UserDataRepository userDataRepository;

    public UserData saveUserData(UserData userData) {
        return userDataRepository.save(userData);

    }













}
