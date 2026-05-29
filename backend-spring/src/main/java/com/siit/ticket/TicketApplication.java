package com.siit.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.siit.ticket.model.User;
import com.siit.ticket.model.Role;
import com.siit.ticket.repository.UserRepository;
import com.siit.ticket.model.Intendencia;
import com.siit.ticket.repository.IntendenciaRepository;
import com.siit.ticket.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class TicketApplication {

	@jakarta.annotation.PostConstruct
	public void initTimezone() {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Lima"));
		System.out.println("TimeZone configurado por defecto a America/Lima: " + java.time.LocalDateTime.now());
	}

	public static void main(String[] args) {
		SpringApplication.run(TicketApplication.class, args);
	}

	@Bean
	CommandLineRunner init(UserRepository userRepository, RoleRepository roleRepository, 
						   IntendenciaRepository intendenciaRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// 1. Crear roles si no existen
			if (roleRepository.count() == 0) {
				roleRepository.save(new Role("ADMIN"));
				roleRepository.save(new Role("ESPECIALISTA"));
				roleRepository.save(new Role("INSPECTOR"));
				System.out.println("✅ Roles inicializados.");
			}

			// 2. Asegurar que el administrador oficial exista y su password/rol estén restablecidos
			Role adminRole = roleRepository.findByNombreRol("ADMIN").orElseThrow();
			User admin = userRepository.findByUsername("ajimeneza").orElseGet(() -> {
				User newAdmin = new User();
				newAdmin.setUsername("ajimeneza");
				newAdmin.setEmail("ajimeneza@sunafil.gob.pe");
				return newAdmin;
			});
			admin.setRole(adminRole); // Forzamos el rol a ADMIN
			admin.setPassword(passwordEncoder.encode("Sunafil_2026_AJA02"));
			admin.setDebeCambiarPassword(false); // Desactivamos la obligación de cambio para evitar bloqueos
			admin.setActivo(true);
			userRepository.save(admin);
			System.out.println(">>> Administrador oficial inicializado/restablecido (Rol: ADMIN): ajimeneza");



			// 3. Crear Intendencias si no existen
			if (intendenciaRepository.count() == 0) {
				String[][] ires = {
					{"DINI", "Dirección de Inteligencia Inspectiva (DINI)"},
					{"IRE-AMAZONAS", "Intendencia Regional Amazonas"},
					{"IRE-ANCASH", "Intendencia Regional Áncash"},
					{"IRE-APURIMAC", "Intendencia Regional Apurímac"},
					{"IRE-AREQUIPA", "Intendencia Regional Arequipa"},
					{"IRE-AYACUCHO", "Intendencia Regional Ayacucho"},
					{"IRE-CAJAMARCA", "Intendencia Regional Cajamarca"},
					{"IRE-CALLAO", "Intendencia Regional Callao"},
					{"IRE-CUSCO", "Intendencia Regional Cusco"},
					{"IRE-HUANCAVELICA", "Intendencia Regional Huancavelica"},
					{"IRE-HUANUCO", "Intendencia Regional Huánuco"},
					{"IRE-ICA", "Intendencia Regional Ica"},
					{"IRE-JUNIN", "Intendencia Regional Junín"},
					{"IRE-LA-LIBERTAD", "Intendencia Regional La Libertad"},
					{"IRE-LAMBAYEQUE", "Intendencia Regional Lambayeque"},
					{"IRE-LIMA", "Intendencia Regional Lima"},
					{"IRE-LORETO", "Intendencia Regional Loreto"},
					{"IRE-MADRE-DE-DIOS", "Intendencia Regional Madre de Dios"},
					{"IRE-MOQUEGUA", "Intendencia Regional Moquegua"},
					{"IRE-PASCO", "Intendencia Regional Pasco"},
					{"IRE-PIURA", "Intendencia Regional Piura"},
					{"IRE-PUNO", "Intendencia Regional Puno"},
					{"IRE-SAN-MARTIN", "Intendencia Regional San Martín"},
					{"IRE-TACNA", "Intendencia Regional Tacna"},
					{"IRE-TUMBES", "Intendencia Regional Tumbes"},
					{"IRE-UCAYALI", "Intendencia Regional Ucayali"},
					{"ILM", "Intendencia Lima Metropolitana"},
					{"SDIE", "Subdirección de Intervenciones Especiales"}
				};

				for (String[] ireData : ires) {
					Intendencia ire = new Intendencia();
					ire.setCodIre(ireData[0]);
					ire.setNombreIre(ireData[1]);
					intendenciaRepository.save(ire);
				}
				System.out.println("✅ Catálogo de Intendencias (IRE) inicializado.");
			}
		};
	}
}
