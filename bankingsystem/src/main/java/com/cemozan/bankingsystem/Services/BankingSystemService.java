package com.cemozan.bankingsystem.Services;




import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
//import com.cemozan.bankingsystem.Models.User;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.cemozan.bankingsystem.Interfaces.IBankingSystem;
import com.cemozan.bankingsystem.Interfaces.IBankingSystemRepository;
import com.cemozan.bankingsystem.Interfaces.IBankingSystemUserRepository;
import com.cemozan.bankingsystem.Models.Account;
import com.cemozan.bankingsystem.Models.AccountCreateRequest;
import com.cemozan.bankingsystem.Models.AccountCreateResponse;
import com.cemozan.bankingsystem.Models.AccountCreateSuccessResponse;
import com.cemozan.bankingsystem.Models.AccountDetails;
import com.cemozan.bankingsystem.Models.IncreaseBalance;
import com.cemozan.bankingsystem.Models.Log;
import com.cemozan.bankingsystem.Models.MoneyTransferRequest;
import com.cemozan.bankingsystem.Models.MoneyTransferResponse;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

@Service
public class BankingSystemService implements IBankingSystem{
	
	
	
	@Autowired
	private IBankingSystemRepository repository;
	
	@Autowired
	IBankingSystemUserRepository userRepo;
	
	public ResponseEntity<AccountCreateResponse> createAccount(@RequestBody AccountCreateRequest request) {
		
		String[] validTypes = {"TL","DOLAR","Altın"};
		boolean isTypeValid = false;
		for(String type : validTypes) {
			
			if (type.equals(request.getType())) {
				isTypeValid = true;
				break;
			}
		}
		
		if (isTypeValid == false) {

			String message = "Invalid Account Type " + request.getType();
			AccountCreateResponse accountCreateResponse = new AccountCreateResponse();
			accountCreateResponse.setMessage(message);
			return ResponseEntity
					.badRequest()
					.body(accountCreateResponse);
			
		}else {
			
				
			Account account = new Account();
			
			long number = (long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L;
			String accountNo = Long.toString(number);
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			String currentTime = Long.toString(timestamp.getTime());

			
			account.setAccountNumber(accountNo);
			account.setBalance(0);
			account.setDeleted(0);
			account.setLastModified(currentTime);
			account.setName(request.getName());
			account.setSurname(request.getSurname());
			account.setTc(request.getTc());
			account.setType(request.getType());
			account.setEmail(request.getEmail());
			
			User authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			com.cemozan.bankingsystem.Models.User user = userRepo.findByUsername(authUser.getUsername());
			
			account.setUserId(user.getId());
			
			
			
			if(repository.save(account) != null) {
				
				Long acoountNumber = Long.parseLong(accountNo);
				AccountCreateSuccessResponse accountCreateSuccessResponse = new AccountCreateSuccessResponse();
				accountCreateSuccessResponse.setAccountNumber(acoountNumber);
				accountCreateSuccessResponse.setMessage("Account Created");
				return ResponseEntity
						.ok()
						.lastModified(timestamp.getTime())
						.body(accountCreateSuccessResponse);
				
			}else {
				return ResponseEntity
						.internalServerError()
						.body(null);
			}
			
		}
	}

	
	public ResponseEntity<?> getAccountDetails(@PathVariable String accountNumber){
		
		Reader reader;
        AccountDetails accountDetails = new AccountDetails();
		try {
		    //read configuration and create session factory
			reader = Resources.getResourceAsReader("myBatis_conf.xml");
			SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
			// open session, session is connection between database and us(like JDBC connection)
			SqlSession session = sqlSessionFactory.openSession();
			// call query from mapper.xml files with defined id
			Account account = session.selectOne("findByAccountNumber",accountNumber);
			
			if (account != null) {
			    User authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			    com.cemozan.bankingsystem.Models.User u = userRepo.findByUsername(authUser.getUsername());
				if(account.getUserId() == (u.getId())) {
					accountDetails.setAccountNumber(accountNumber);
					accountDetails.setBalance(Float.toString(account.getBalance()));
					accountDetails.setEmail(account.getEmail());
					accountDetails.setName(account.getName());
					accountDetails.setSurname(account.getSurname());
					accountDetails.setTc(account.getTc());
					accountDetails.setTimestamp(account.getLastModified());
					accountDetails.setType(account.getType());
					return ResponseEntity
							.ok()
							.lastModified(Long.parseLong(account.getLastModified()))
							.body(accountDetails);
				}else {
					MoneyTransferResponse rsp = new MoneyTransferResponse();
					rsp.setMessage("Unauthorized access");
					return ResponseEntity.badRequest().body(rsp);
				}
				
			}else {
				return ResponseEntity.badRequest().body(accountDetails);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(accountDetails);
		}
	}


	
	@SuppressWarnings({ "unchecked", "rawtypes", "resource" })
	public ResponseEntity<?> increaseBalance(@PathVariable String accountNumber, @RequestBody IncreaseBalance request) {
		
		Properties props = new Properties();
	    props.put("bootstrap.servers", "localhost:9092");
	    props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
	    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
	    Producer<Integer, String> producer = new KafkaProducer<Integer, String>(props);
	    
	    String writedTxtToDb = "";
	
		String amount = request.getAmount();
		Account account = repository.findByAccountNumber(accountNumber);
		
		AccountDetails accountDetails = new AccountDetails();
		if (account != null) {
			//User authUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			User authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		    com.cemozan.bankingsystem.Models.User u = userRepo.findByUsername(authUser.getUsername());
			if(account.getUserId() == (u.getId())) {
				Float oldBalance = account.getBalance();
				Float newBalance = oldBalance + Float.parseFloat(amount);
				account.setBalance(newBalance);
				
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				String currentTime = Long.toString(timestamp.getTime());
				account.setLastModified(currentTime);
				

				if(repository.save(account) != null) {
					accountDetails.setAccountNumber(accountNumber);
					accountDetails.setBalance(Float.toString(account.getBalance()));
					accountDetails.setEmail(account.getEmail());
					accountDetails.setName(account.getName());
					accountDetails.setSurname(account.getSurname());
					accountDetails.setTc(account.getTc());
					accountDetails.setTimestamp(account.getLastModified());
					accountDetails.setType(account.getType());
					
					writedTxtToDb = accountNumber + " nolu hesaba " + request.getAmount() + " " + account.getType() + " yatırılmıştır.";
					//   2341231231 nolu hesaba 100 [hesap tipi] yatırılmıştır.
					ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
					producer.send(producerRecord);
					
					return ResponseEntity.ok().body(accountDetails);
				}else {
					return ResponseEntity.internalServerError().body(accountDetails);
				}
			}else {
				MoneyTransferResponse rsp = new MoneyTransferResponse();
				rsp.setMessage("Unauthorized access");
				return ResponseEntity.badRequest().body(rsp);
			}
			
		}else {
			return ResponseEntity.badRequest().body(accountDetails);
		}
		
	}
	
	
	@SuppressWarnings({ "resource", "unused", "rawtypes", "unchecked" })
	public ResponseEntity<MoneyTransferResponse> moneyTransfer(@PathVariable String fromAccountNumber, @RequestBody MoneyTransferRequest request) throws IOException{
		
		MoneyTransferResponse moneyTransferResponse = new MoneyTransferResponse();
		
		Account fromAccount = repository.findByAccountNumber(fromAccountNumber);
		Account toAccount = repository.findByAccountNumber(request.getTransferredAccountNumber());
		
		Connection conn = null;
		 
		Properties props = new Properties();
	    props.put("bootstrap.servers", "localhost:9092");
	    props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");
	    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
	    Producer<Integer, String> producer = new KafkaProducer<Integer, String>(props);
	    
	    String writedTxtToDb = "";
		
		
		if (fromAccount != null && toAccount != null) {
			//User authUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			User authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		    com.cemozan.bankingsystem.Models.User u = userRepo.findByUsername(authUser.getUsername());
			if(fromAccount.getUserId() == (u.getId())) {
				if(fromAccount.getType().equals("DOLAR") && toAccount.getType().equals("DOLAR")) {
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Float.parseFloat(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						
						
						try {

							Class.forName("com.mysql.cj.jdbc.Driver");
							String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
							conn = DriverManager.getConnection(url, "cemozan", "123456");
							conn.setAutoCommit(false);
							
							String queryOne = "UPDATE accounts SET balance = ? WHERE account_number = ?";
							PreparedStatement ps = conn.prepareStatement(queryOne);
							Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
							ps.setFloat(1, newFromAccountBalance);
							ps.setString(2, fromAccount.getAccountNumber());
							ps.executeUpdate();

							String queryTwo = "UPDATE accounts SET balance = ? WHERE account_number = ?";
							ps = conn.prepareStatement(queryTwo);
							Float newToAccountBalance = oldToAccountBalance + Float.parseFloat(request.getAmount());
							ps.setFloat(1, newToAccountBalance);
							ps.setString(2, toAccount.getAccountNumber());
							ps.executeUpdate();
							conn.commit();
							moneyTransferResponse.setMessage("Transferred Successfully");
							
							writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
							ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
							producer.send(producerRecord);
							
							
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());
							String currentTime = Long.toString(timestamp.getTime());
							toAccount.setLastModified(currentTime);
							fromAccount.setLastModified(currentTime);
							
							repository.save(toAccount);
							repository.save(fromAccount);
							
						    return ResponseEntity.ok().body(moneyTransferResponse);
		

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SQLException e) {
							try {
								conn.rollback();
							} catch (SQLException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							e.printStackTrace();
						}  
						
						
					}
				}else if(fromAccount.getType().equals("DOLAR") && toAccount.getType().equals("TL")) {
					
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Long.parseLong(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						try {
							HttpResponse<JsonNode> response =  Unirest.get("https://api.collectapi.com/economy/exchange?int="+request.getAmount()+"&to=TRY&base=USD")
									  .header("content-type", "application/json")
									  .header("authorization", "apikey 1HsyxyFajXCgMXrvgZOo6t:215vHnXoOm34zQoezFbax3")
									  .asJson();
							
							// Parsing the "response"
							JSONObject responsejson = response.getBody().getObject();
							JSONObject result = responsejson.getJSONObject("result");
							org.json.JSONArray data = result.getJSONArray("data");
							JSONObject object = data.getJSONObject(0);
							 
							Float calculated =  Float.parseFloat(Double.toString((double) object.get("calculated")));
							
					
							try {

								Class.forName("com.mysql.cj.jdbc.Driver");
								String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
								conn = DriverManager.getConnection(url, "cemozan", "123456");
								conn.setAutoCommit(false);
								
								String queryOne = "UPDATE accounts SET balance = ? WHERE account_number = ?";
								PreparedStatement ps = conn.prepareStatement(queryOne);
								Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
								ps.setFloat(1, newFromAccountBalance);
								ps.setString(2, fromAccount.getAccountNumber());
								ps.executeUpdate();

								String queryTwo = "UPDATE accounts SET balance = ? WHERE account_number = ?";
								ps = conn.prepareStatement(queryTwo);
								Float newToAccountBalance = oldToAccountBalance + calculated;
								ps.setFloat(1, newToAccountBalance);
								ps.setString(2, toAccount.getAccountNumber());
								ps.executeUpdate();
								conn.commit();
								moneyTransferResponse.setMessage("Transferred Successfully");
								
								
								writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
								ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
								producer.send(producerRecord);
								
								
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								String currentTime = Long.toString(timestamp.getTime());
								toAccount.setLastModified(currentTime);
								fromAccount.setLastModified(currentTime);
								
								repository.save(toAccount);
								repository.save(fromAccount);
								
							    return ResponseEntity.ok().body(moneyTransferResponse);
			

							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								try {
									conn.rollback();
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								e.printStackTrace();
							}
							
							

						}catch (Exception e) {
							System.out.println(e);
							return ResponseEntity.internalServerError().body(moneyTransferResponse);
						}
					}
				}else if(fromAccount.getType().equals("TL") && toAccount.getType().equals("DOLAR")) {
					
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Long.parseLong(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						try {
							HttpResponse<JsonNode> response =  Unirest.get("https://api.collectapi.com/economy/exchange?int="+request.getAmount()+"&to=USD&base=TRY")
									  .header("content-type", "application/json")
									  .header("authorization", "apikey 1HsyxyFajXCgMXrvgZOo6t:215vHnXoOm34zQoezFbax3")
									  .asJson();
							
							// Parsing the "response"
							JSONObject responsejson = response.getBody().getObject();
							JSONObject result = responsejson.getJSONObject("result");
							org.json.JSONArray data = result.getJSONArray("data");
							JSONObject object = data.getJSONObject(0);
							 
							Float calculated =  Float.parseFloat(Double.toString((double) object.get("calculated")));
							
							
							try {

								Class.forName("com.mysql.cj.jdbc.Driver");
								String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
								conn = DriverManager.getConnection(url, "cemozan", "123456");
								conn.setAutoCommit(false);
								
								String queryOne = "UPDATE accounts SET balance = ? WHERE account_number = ?";
								PreparedStatement ps = conn.prepareStatement(queryOne);
								Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
								ps.setFloat(1, newFromAccountBalance);
								ps.setString(2, fromAccount.getAccountNumber());
								ps.executeUpdate();

								String queryTwo = "UPDATE accounts SET balance = ? WHERE account_number = ?";
								ps = conn.prepareStatement(queryTwo);
								Float newToAccountBalance = oldToAccountBalance + calculated;
								ps.setFloat(1, newToAccountBalance);
								ps.setString(2, toAccount.getAccountNumber());
								ps.executeUpdate();
								conn.commit();
								moneyTransferResponse.setMessage("Transferred Successfully");
								
								
								writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
								ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
								producer.send(producerRecord);
								
								
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								String currentTime = Long.toString(timestamp.getTime());
								toAccount.setLastModified(currentTime);
								fromAccount.setLastModified(currentTime);
								
								repository.save(toAccount);
								repository.save(fromAccount);
								
								
							    return ResponseEntity.ok().body(moneyTransferResponse);
			

							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								try {
									conn.rollback();
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								e.printStackTrace();
							} 


						}catch (Exception e) {
							System.out.println(e);
							return ResponseEntity.internalServerError().body(moneyTransferResponse);
						}
					}
				}else if(fromAccount.getType().equals("TL") && toAccount.getType().equals("TL")) {
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Float.parseFloat(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						
						
						try {

							Class.forName("com.mysql.cj.jdbc.Driver");
							String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
							conn = DriverManager.getConnection(url, "cemozan", "123456");
							conn.setAutoCommit(false);
							
							String queryOne = "UPDATE accounts SET balance = ? WHERE account_number = ?";
							PreparedStatement ps = conn.prepareStatement(queryOne);
							Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
							ps.setFloat(1, newFromAccountBalance);
							ps.setString(2, fromAccount.getAccountNumber());
							ps.executeUpdate();

							String queryTwo = "UPDATE accounts SET balance = ? WHERE account_number = ?";
							ps = conn.prepareStatement(queryTwo);
							Float newToAccountBalance = oldToAccountBalance + Float.parseFloat(request.getAmount());
							ps.setFloat(1, newToAccountBalance);
							ps.setString(2, toAccount.getAccountNumber());
							ps.executeUpdate();
							conn.commit();
							moneyTransferResponse.setMessage("Transferred Successfully");
							
							
							writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
							
							// 2341231231 hesaptan 4513423423 hesaba 100 [hesap tipi] transfer edilmiştir.
							ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
							producer.send(producerRecord);
							
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());
							String currentTime = Long.toString(timestamp.getTime());
							toAccount.setLastModified(currentTime);
							fromAccount.setLastModified(currentTime);
							
							repository.save(toAccount);
							repository.save(fromAccount);
							
							
						    return ResponseEntity.ok().body(moneyTransferResponse);
		

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SQLException e) {
							try {
								conn.rollback();
							} catch (SQLException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							e.printStackTrace();
						} 

						
					}
				}else if(fromAccount.getType().equals("Altın") && toAccount.getType().equals("DOLAR")) {
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Long.parseLong(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						try {
							
							HttpResponse<JsonNode> response1 =  Unirest.get("https://api.collectapi.com/economy/goldPrice")
									  .header("content-type", "application/json")
									  .header("authorization", "apikey 1HsyxyFajXCgMXrvgZOo6t:215vHnXoOm34zQoezFbax3")
									  .asJson();
							
							JSONObject responsejson1 = response1.getBody().getObject();
							org.json.JSONArray result1 = responsejson1.getJSONArray("result");
							JSONObject object1 = result1.getJSONObject(0);
							
							Float goldPriceTRY =  Float.parseFloat(Double.toString((double) object1.get("buying"))) * Float.parseFloat(request.getAmount());
							String goldPriceTRYInString = Float.toString(goldPriceTRY);
							
							HttpResponse<JsonNode> response =  Unirest.get("https://api.collectapi.com/economy/exchange?int="+goldPriceTRYInString+"&to=USD&base=TRY")
									  .header("content-type", "application/json")
									  .header("authorization", "apikey 1HsyxyFajXCgMXrvgZOo6t:215vHnXoOm34zQoezFbax3")
									  .asJson();
							
							// Parsing the "response"
							JSONObject responsejson = response.getBody().getObject();
							JSONObject result = responsejson.getJSONObject("result");
							org.json.JSONArray data = result.getJSONArray("data");
							JSONObject object = data.getJSONObject(0);
							 
							Float calculated =  Float.parseFloat(Double.toString((double) object.get("calculated")));
							
							
							try {

								Class.forName("com.mysql.cj.jdbc.Driver");
								String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
								conn = DriverManager.getConnection(url, "cemozan", "123456");
								conn.setAutoCommit(false);
							
								Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
								Float newToAccountBalance = oldToAccountBalance + calculated;
									
								fromAccount.setBalance(newFromAccountBalance);
								toAccount.setBalance(newToAccountBalance);
								repository.save(fromAccount);
								repository.save(toAccount);
								conn.commit();
								
								
								moneyTransferResponse.setMessage("Transferred Successfully");
								
								
								writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
								ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
								producer.send(producerRecord);
								
								
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								String currentTime = Long.toString(timestamp.getTime());
								toAccount.setLastModified(currentTime);
								fromAccount.setLastModified(currentTime);
								
								repository.save(toAccount);
								repository.save(fromAccount);
								
								
							    return ResponseEntity.ok().body(moneyTransferResponse);
			

							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								try {
									conn.rollback();
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								e.printStackTrace();
							} 


						}catch (Exception e) {
							System.out.println(e);
							return ResponseEntity.internalServerError().body(moneyTransferResponse);
						}
					}
				}else if(fromAccount.getType().equals("Altın") && toAccount.getType().equals("TL")) {
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Long.parseLong(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						try {
							
							HttpResponse<JsonNode> response1 =  Unirest.get("https://api.collectapi.com/economy/goldPrice")
									  .header("content-type", "application/json")
									  .header("authorization", "apikey 1HsyxyFajXCgMXrvgZOo6t:215vHnXoOm34zQoezFbax3")
									  .asJson();
							
							JSONObject responsejson1 = response1.getBody().getObject();
							org.json.JSONArray result1 = responsejson1.getJSONArray("result");
							JSONObject object1 = result1.getJSONObject(0);
							
							Float goldPriceTRY =  ((Float.parseFloat(Double.toString((double) object1.get("buying")))) * Float.parseFloat(request.getAmount()));
							String goldPriceTRYInString = Float.toString(goldPriceTRY);
						
							try {

								Class.forName("com.mysql.cj.jdbc.Driver");
								String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
								conn = DriverManager.getConnection(url, "cemozan", "123456");
								conn.setAutoCommit(false);
								
								
								Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
								
								Float newToAccountBalance = oldToAccountBalance + goldPriceTRY;
								
								
								fromAccount.setBalance(newFromAccountBalance);
								toAccount.setBalance(newToAccountBalance);
								repository.save(fromAccount);
								repository.save(toAccount);
								conn.commit();
								
								moneyTransferResponse.setMessage("Transferred Successfully");
								
								
								writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
								ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
								producer.send(producerRecord);
								
								
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								String currentTime = Long.toString(timestamp.getTime());
								toAccount.setLastModified(currentTime);
								fromAccount.setLastModified(currentTime);
								
								repository.save(toAccount);
								repository.save(fromAccount);
								
								
							    return ResponseEntity.ok().body(moneyTransferResponse);
			

							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								try {
									conn.rollback();
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								e.printStackTrace();
							} 


						}catch (Exception e) {
							System.out.println(e);
							return ResponseEntity.internalServerError().body(moneyTransferResponse);
						}
					}
				}else if(fromAccount.getType().equals("Altın") && toAccount.getType().equals("Altın")) {
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Long.parseLong(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						try {				
							try {

								Class.forName("com.mysql.cj.jdbc.Driver");
								String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
								conn = DriverManager.getConnection(url, "cemozan", "123456");
								conn.setAutoCommit(false);
		
								Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
								Float newToAccountBalance = oldToAccountBalance + Float.parseFloat(request.getAmount());
		
								fromAccount.setBalance(newFromAccountBalance);
								toAccount.setBalance(newToAccountBalance);
								repository.save(fromAccount);
								repository.save(toAccount);
								conn.commit();
								
								moneyTransferResponse.setMessage("Transferred Successfully");
								
								
								writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
								ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
								producer.send(producerRecord);
								
								
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								String currentTime = Long.toString(timestamp.getTime());
								toAccount.setLastModified(currentTime);
								fromAccount.setLastModified(currentTime);
								
								repository.save(toAccount);
								repository.save(fromAccount);
								
								
							    return ResponseEntity.ok().body(moneyTransferResponse);
			

							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								try {
									conn.rollback();
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								e.printStackTrace();
							} 


						}catch (Exception e) {
							System.out.println(e);
							return ResponseEntity.internalServerError().body(moneyTransferResponse);
						}
					}
				}else if(fromAccount.getType().equals("TL") && toAccount.getType().equals("Altın")) {
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Long.parseLong(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						try {
							
							HttpResponse<JsonNode> response1 =  Unirest.get("https://api.collectapi.com/economy/goldPrice")
									  .header("content-type", "application/json")
									  .header("authorization", "apikey 1HsyxyFajXCgMXrvgZOo6t:215vHnXoOm34zQoezFbax3")
									  .asJson();
							
							JSONObject responsejson1 = response1.getBody().getObject();
							org.json.JSONArray result1 = responsejson1.getJSONArray("result");
							JSONObject object1 = result1.getJSONObject(0);
							
							Float goldPriceTRY =  Float.parseFloat(Double.toString((double) object1.get("buying"))) * Float.parseFloat(request.getAmount());
							
							try {

								Class.forName("com.mysql.cj.jdbc.Driver");
								String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
								conn = DriverManager.getConnection(url, "cemozan", "123456");
								conn.setAutoCommit(false);
								
								
								Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
								Float newToAccountBalance = oldToAccountBalance + (Float.parseFloat(request.getAmount()) / goldPriceTRY);
								
								fromAccount.setBalance(newFromAccountBalance);
								toAccount.setBalance(newToAccountBalance);
								repository.save(fromAccount);
								repository.save(toAccount);
								conn.commit();
								
								moneyTransferResponse.setMessage("Transferred Successfully");
								
								
								writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
								ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
								producer.send(producerRecord);
								
								
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								String currentTime = Long.toString(timestamp.getTime());
								toAccount.setLastModified(currentTime);
								fromAccount.setLastModified(currentTime);
								
								repository.save(toAccount);
								repository.save(fromAccount);
								
								
							    return ResponseEntity.ok().body(moneyTransferResponse);
			

							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								try {
									conn.rollback();
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								e.printStackTrace();
							} 


						}catch (Exception e) {
							System.out.println(e);
							return ResponseEntity.internalServerError().body(moneyTransferResponse);
						}
					}
				}else if(fromAccount.getType().equals("DOLAR") && toAccount.getType().equals("Altın")) {
					Float oldFromAccountBalance = fromAccount.getBalance();
					Float oldToAccountBalance = toAccount.getBalance();
					
					if(Long.parseLong(request.getAmount()) > oldFromAccountBalance) {
						moneyTransferResponse.setMessage("Insufficient Balance");
						return ResponseEntity.badRequest().body(moneyTransferResponse);
					}else {
						try {
							
							HttpResponse<JsonNode> response1 =  Unirest.get("https://api.collectapi.com/economy/goldPrice")
									  .header("content-type", "application/json")
									  .header("authorization", "apikey 1HsyxyFajXCgMXrvgZOo6t:215vHnXoOm34zQoezFbax3")
									  .asJson();
							
							JSONObject responsejson1 = response1.getBody().getObject();
							org.json.JSONArray result1 = responsejson1.getJSONArray("result");
							JSONObject object1 = result1.getJSONObject(0);
							
							Float goldPriceTRY =  Float.parseFloat(Double.toString((double) object1.get("buying"))) * Float.parseFloat(request.getAmount());
							String goldPriceTRYInString = Float.toString(goldPriceTRY);
							
							HttpResponse<JsonNode> response =  Unirest.get("https://api.collectapi.com/economy/exchange?int="+goldPriceTRYInString+"&to=USD&base=TRY")
									  .header("content-type", "application/json")
									  .header("authorization", "apikey 1HsyxyFajXCgMXrvgZOo6t:215vHnXoOm34zQoezFbax3")
									  .asJson();
							
							// Parsing the "response"
							JSONObject responsejson = response.getBody().getObject();
							JSONObject result = responsejson.getJSONObject("result");
							org.json.JSONArray data = result.getJSONArray("data");
							JSONObject object = data.getJSONObject(0);
							 
							Float goldPriceUSD =  Float.parseFloat(Double.toString((double) object.get("calculated")));
							
							
							try {

								Class.forName("com.mysql.cj.jdbc.Driver");
								String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
								conn = DriverManager.getConnection(url, "cemozan", "123456");
								conn.setAutoCommit(false);
								
								
								Float newFromAccountBalance = oldFromAccountBalance - Float.parseFloat(request.getAmount());
								
								Float newToAccountBalance = oldToAccountBalance + (Float.parseFloat(request.getAmount()) / goldPriceUSD);
								
								fromAccount.setBalance(newFromAccountBalance);
								toAccount.setBalance(newToAccountBalance);
								repository.save(fromAccount);
								repository.save(toAccount);
								conn.commit();
								moneyTransferResponse.setMessage("Transferred Successfully");
								
								
								writedTxtToDb = fromAccountNumber + " hesaptan "+toAccount.getAccountNumber()+ " hesaba "+request.getAmount()+" "+fromAccount.getType()+" transfer edilmiştir.";
								ProducerRecord producerRecord = new ProducerRecord<Integer, String>("logs", writedTxtToDb);
								producer.send(producerRecord);
								
								
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());
								String currentTime = Long.toString(timestamp.getTime());
								toAccount.setLastModified(currentTime);
								fromAccount.setLastModified(currentTime);
								
								repository.save(toAccount);
								repository.save(fromAccount);
								
								
							    return ResponseEntity.ok().body(moneyTransferResponse);
			

							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SQLException e) {
								try {
									conn.rollback();
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								e.printStackTrace();
							} 


						}catch (Exception e) {
							System.out.println(e);
							return ResponseEntity.internalServerError().body(moneyTransferResponse);
						}
					}
				}
				
			}else {
				MoneyTransferResponse rsp = new MoneyTransferResponse();
				rsp.setMessage("Unauthorized access");
				return ResponseEntity.badRequest().body(rsp);
			}
			
		}
		
		
		return null;
		
	}


	public ResponseEntity<ArrayList<Log>> getLogs(String accountNumber) {
		ArrayList<Log> logs = new ArrayList<>();
		Connection conn = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/bootcampdb?useServerPrepStmts=true";
			conn = DriverManager.getConnection(url, "cemozan", "123456");
			conn.setAutoCommit(false);
			
			String queryOne = "SELECT * FROM logs";
			PreparedStatement ps = conn.prepareStatement(queryOne);
			ResultSet rs = ps.executeQuery(queryOne);
			while (rs.next()) {
		        String detail = rs.getString("detail");
		        
		        if (isWordPresent(detail, accountNumber)) {
		        	Log log = new Log();
		        	log.setLog(detail);
		        	logs.add(log);
		        }
		        
			}
			conn.commit();
			return ResponseEntity.ok().body(logs);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(logs);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(logs);
		}		
	}


	public ResponseEntity<?> delete(String id) {
		
		Account account = repository.findByAccountNumber(id);
		
		
		try {
			if(account != null && account.isDeleted() != 1) {
				
				 //User authUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
				 User authUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
				 com.cemozan.bankingsystem.Models.User u = userRepo.findByUsername(authUser.getUsername());
					if(account.getUserId() == (u.getId())) {
						account.setDeleted(1);
						Timestamp timestamp = new Timestamp(System.currentTimeMillis());
						String currentTime = Long.toString(timestamp.getTime());
						account.setLastModified(currentTime);
						if(repository.save(account) != null) {
							return ResponseEntity.ok()
									.lastModified(Long.parseLong(account.getLastModified()))
									.body(true);
						}else {
							return ResponseEntity.badRequest().body(false);
						}
						
					}else {
						MoneyTransferResponse rsp = new MoneyTransferResponse();
						rsp.setMessage("Unauthorized access");
						return ResponseEntity.badRequest().body(rsp);
					}			
			}else {
				return ResponseEntity.badRequest().body(false);
			}
			
		}catch (Exception e) {
			return ResponseEntity.internalServerError().body(null);
		}
		
		
	}
	
	
	static boolean isWordPresent(String sentence, String word)
	{
	    // To break the sentence in words
	    String []s = sentence.split(" ");
	 
	    // To temporarily store each individual word
	    for ( String temp :s)
	    {
	 
	        // Comparing the current word
	        // with the word to be searched
	        if (temp.compareTo(word) == 0)
	        {
	            return true;
	        }
	    }
	    return false;
	}


	
	
}
