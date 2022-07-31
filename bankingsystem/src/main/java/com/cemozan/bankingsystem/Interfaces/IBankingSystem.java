package com.cemozan.bankingsystem.Interfaces;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.cemozan.bankingsystem.Models.AccountCreateRequest;
import com.cemozan.bankingsystem.Models.AccountCreateResponse;
import com.cemozan.bankingsystem.Models.IncreaseBalance;
import com.cemozan.bankingsystem.Models.Log;
import com.cemozan.bankingsystem.Models.MoneyTransferRequest;
import com.cemozan.bankingsystem.Models.MoneyTransferResponse;


public interface IBankingSystem {
	public ResponseEntity<AccountCreateResponse> createAccount(@RequestBody AccountCreateRequest request);
	public ResponseEntity<?> getAccountDetails(@PathVariable String accountNumber);
	public ResponseEntity<?> increaseBalance(@PathVariable String accountNumber, @RequestBody IncreaseBalance request);
	public ResponseEntity<MoneyTransferResponse> moneyTransfer(@PathVariable String fromAccountNumber, @RequestBody MoneyTransferRequest request) throws IOException;
	public ResponseEntity<ArrayList<Log>> getLogs(@PathVariable String accountNumber);
	public ResponseEntity<?> delete(@PathVariable String id);
}
