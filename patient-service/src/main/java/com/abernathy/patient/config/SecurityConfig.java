package com.abernathy.patient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
           .csrf(csrf -> csrf.disable()) // Désactivé pour faciliter vos tests via Postman
           .authorizeHttpRequests(auth -> auth
               .anyRequest().authenticated() // Bloque toutes les requêtes non authentifiées
            )
           .httpBasic(Customizer.withDefaults()); // Active l'authentification HTTP Basic

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // Création d'un utilisateur en mémoire pour les tests
        UserDetails user = User.builder()
           .username("medecin")
           .password(passwordEncoder.encode("password123"))
           .roles("USER")
           .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Algorithme de hachage robuste exigé par les audits de sécurité
        return new BCryptPasswordEncoder();
    }
}