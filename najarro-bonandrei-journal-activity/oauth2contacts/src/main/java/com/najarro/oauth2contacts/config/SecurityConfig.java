package com.najarro.oauth2contacts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .authorizeHttpRequests(authorizeRequest ->
                    authorizeRequest.anyRequest().authenticated())
            .formLogin(form ->
                    form.defaultSuccessUrl("/home", true))
            .oauth2Login(oauth ->
                    oauth.defaultSuccessUrl("/home", true))
            .build();
  }
}
