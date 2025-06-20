package com.najarro.oauth2contacts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .authorizeHttpRequests(authorizeRequest -> authorizeRequest
                    .requestMatchers("/", "error").permitAll()
                    .requestMatchers("/home").authenticated()
                    .requestMatchers("/contact-form", "/save-contact").authenticated()
                    .anyRequest().authenticated())
            .oauth2Login(oauth -> oauth
                    .loginPage("/")
                    .defaultSuccessUrl("/home", true)
                    .redirectionEndpoint(redirection -> redirection
                            .baseUri("/login/oauth2/code/*")
                    )
            )
            .logout(logout -> logout
                    .logoutSuccessUrl("/")
                    .permitAll()
            )  
            .build();
  }

  @Bean
  public RestTemplate restTemplate(){
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    return new RestTemplate(requestFactory);
  }
}
