package Security;


import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cemozan.bankingsystem.Services.DatabaseUserDetailsService;

import JWT.JwtRequestFilter;


@SuppressWarnings("deprecation")
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter{
	@Autowired
	private JwtRequestFilter jwtRequestFilter;	  
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(new DatabaseUserDetailsService());
	}
	
	@Bean
    public PasswordEncoder passwordEncoder(){
        return NoOpPasswordEncoder.getInstance();
    }
	
	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
	    return super.authenticationManagerBean();
	}
	 
	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
			
			httpSecurity
			.csrf()
			.disable()
			.authorizeHttpRequests()
			.antMatchers(HttpMethod.POST, "/accounts/**").hasAuthority("CREATE_ACCOUNT")
			.antMatchers(HttpMethod.PATCH, "/accounts/**").hasAuthority("CREATE_ACCOUNT")
			.antMatchers(HttpMethod.GET, "/accounts/**").hasAuthority("CREATE_ACCOUNT")
			.antMatchers(HttpMethod.PUT, "/accounts/**").hasAuthority("CREATE_ACCOUNT")
			.antMatchers(HttpMethod.DELETE, "/accounts/**").hasAuthority("REMOVE_ACCOUNT")
			.antMatchers("/auth").permitAll()
			.antMatchers(HttpMethod.GET,"/accounts/logs/**").permitAll()
			.anyRequest()
			.authenticated()
			.and()
			.exceptionHandling()
			.accessDeniedHandler(
					(request, response, e) -> {
				      response.setContentType("application/json;charset=UTF-8");
				      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				      response.getWriter().write(new JSONObject().put("message", "Invalid Account Number!").toString());
				    });
			
			
			httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
	}
	
	
	
}
