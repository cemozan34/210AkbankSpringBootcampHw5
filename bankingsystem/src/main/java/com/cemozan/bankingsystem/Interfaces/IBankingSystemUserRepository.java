package com.cemozan.bankingsystem.Interfaces;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.cemozan.bankingsystem.Models.User;

@Repository
public interface IBankingSystemUserRepository extends CrudRepository<User, Integer>{
	public User findByUsername(String username);
}
