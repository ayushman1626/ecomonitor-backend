package com.example.demo.repo;

import com.example.demo.model.Interface;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterfaceRepo extends JpaRepository<Interface, UUID> {
     Optional<Interface> findByCreatedBy(User user);
}
