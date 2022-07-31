package com.cemozan.bankingsystem.Controllers;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cemozan.bankingsystem.Interfaces.IBankingSystem;
import com.cemozan.bankingsystem.Models.AccountCreateRequest;
import com.cemozan.bankingsystem.Models.AccountCreateResponse;
import com.cemozan.bankingsystem.Models.IncreaseBalance;
import com.cemozan.bankingsystem.Models.Log;
import com.cemozan.bankingsystem.Models.LoginRequest;
import com.cemozan.bankingsystem.Models.LoginResponse;
import com.cemozan.bankingsystem.Models.MoneyTransferRequest;
import com.cemozan.bankingsystem.Models.MoneyTransferResponse;
import com.cemozan.bankingsystem.Services.DatabaseUserDetailsService;

import JWT.JWTTokenUtil;




@RestController
public class BankingSystemController {
	
	@Autowired
	private IBankingSystem service;
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private JWTTokenUtil jwtTokenUtil;
	
	@Autowired
	private DatabaseUserDetailsService userDetailsService;
	
	@PostMapping(path = "/accounts", consumes = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<AccountCreateResponse> createAccount(@RequestBody AccountCreateRequest request){

		return this.service.createAccount(request);
		
    }
	
	@GetMapping(path = "/accounts/{id}")
	public ResponseEntity<?> getAccountDetails(@PathVariable String id){
		
		return this.service.getAccountDetails(id);
	}
	
	@PatchMapping(path="accounts/{accountNumber}", consumes = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> increaseBalance(@PathVariable String accountNumber, @RequestBody IncreaseBalance request){
		return this.service.increaseBalance(accountNumber, request);
	}

	@PutMapping(path="accounts/{fromAccountNumber}", consumes = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<MoneyTransferResponse> moneyTransfer(@PathVariable String fromAccountNumber, @RequestBody MoneyTransferRequest request) throws IOException{
		return this.service.moneyTransfer(fromAccountNumber, request);
		
	}
	
	@CrossOrigin("http://localhost:6162")
	@GetMapping(path="accounts/logs/{accountNumber}")
	public ResponseEntity<ArrayList<Log>> getLogs(@PathVariable String accountNumber){
		return this.service.getLogs(accountNumber);
	}
	
	@DeleteMapping("/accounts/{id}")
	public ResponseEntity<?> delete(@PathVariable String id) {
		return this.service.delete(id);
	}
	
	@PostMapping("/auth")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
		
		System.out.println("Deneme 123");
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		} catch (BadCredentialsException e) {
			return ResponseEntity.badRequest().body("Bad credentials");
		} catch (DisabledException e) {
		}
		final UserDetails userDetails = userDetailsService
				.loadUserByUsername(request.getUsername());

		final String token = jwtTokenUtil.generateToken(userDetails);
		LoginResponse resp = new LoginResponse();
		resp.setStatus("success");
		resp.setToken(token);
        return ResponseEntity.ok().body(resp);
	}

}
