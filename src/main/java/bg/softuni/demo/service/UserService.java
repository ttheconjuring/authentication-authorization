package bg.softuni.demo.service;

import bg.softuni.demo.model.User;
import bg.softuni.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return this.userRepository.findAll();
    }

}
