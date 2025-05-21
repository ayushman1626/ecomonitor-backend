package com.example.demo.repo;

import com.example.demo.model.Interface;
import com.example.demo.model.User;
import com.example.demo.model.UserInterface;
import com.example.demo.model.UserInterfaceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserInterfaceRepo extends JpaRepository<UserInterface, UserInterfaceId> {
    Optional<List<UserInterface>> findByUser(User user);
    Optional<UserInterface> findByUserAndInterfaceId(User user, Interface interfaceEntity);
    List<UserInterface> findByInterfaceId(Interface interfaceEntity);
}
