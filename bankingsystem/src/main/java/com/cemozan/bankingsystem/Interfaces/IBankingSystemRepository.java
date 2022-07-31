package com.cemozan.bankingsystem.Interfaces;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.cemozan.bankingsystem.Models.Account;

@Repository
public interface IBankingSystemRepository extends CrudRepository<Account, Integer>{
	
	public Account findByAccountNumber(String accountNumber);
	
}
