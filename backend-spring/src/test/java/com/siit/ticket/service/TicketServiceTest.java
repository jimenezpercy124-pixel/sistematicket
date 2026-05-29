package com.siit.ticket.service;

import com.siit.ticket.model.Ticket;
import com.siit.ticket.model.User;
import com.siit.ticket.repository.TicketRepository;
import com.siit.ticket.repository.TicketSequenceRepository;
import com.siit.ticket.repository.UserRepository;
import com.siit.ticket.repository.TicketLogRepository;
import com.siit.ticket.model.TicketSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketSequenceRepository sequenceRepository;

    @Mock
    private TicketLogRepository ticketLogRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FileService fileService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TicketService ticketService;

    @BeforeEach
    public void setUp() throws Exception {
        // Inyectar el tiempo de expiración mediante reflexión para la prueba unitaria
        java.lang.reflect.Field tokenExpHours = TicketService.class.getDeclaredField("tokenExpirationHours");
        tokenExpHours.setAccessible(true);
        tokenExpHours.set(ticketService, 72);
        
        java.lang.reflect.Field frontendUrl = TicketService.class.getDeclaredField("frontendUrl");
        frontendUrl.setAccessible(true);
        frontendUrl.set(ticketService, "http://localhost:4200");
    }

    @Test
    public void testCreateTicketWithFileSuccess() {
        User creator = new User();
        creator.setIdUsuario(1L);
        creator.setUsername("percy");
        creator.setEmail("percy@sunafil.gob.pe");
        creator.setCodIre("APURIMAC");

        Ticket ticket = new Ticket();
        ticket.setAsunto("No carga el portal de inspectores");
        ticket.setCodIre("APURIMAC");
        ticket.setCreador(creator);

        TicketSequence sequence = new TicketSequence("APURIMAC", 0L);
        when(sequenceRepository.findById("APURIMAC")).thenReturn(Optional.of(sequence));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.createTicketWithFile(ticket, null);

        assertNotNull(result);
        assertNotNull(result.getIdTicket());
        assertTrue(result.getIdTicket().startsWith("TK-APURIMAC-"));
        assertEquals("PENDIENTE", result.getEstado());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    public void testUpdateStatusToResolvedSetsTokenAndExpiration() {
        Ticket ticket = new Ticket();
        ticket.setIdTicket("TK-APURIMAC-0526-0000001");
        ticket.setEstado("PENDIENTE");

        when(ticketRepository.findById(ticket.getIdTicket())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.updateStatus(ticket.getIdTicket(), "RESUELTO");

        assertNotNull(result);
        assertEquals("PENDIENTE_CONFIRMACION", result.getEstado());
        assertNotNull(result.getTokenConfirmacion());
        assertNotNull(result.getFechaExpiracionToken());
        assertTrue(result.getFechaExpiracionToken().isAfter(LocalDateTime.now().plusHours(71)));
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    public void testProcessConfirmationExpiredTokenReturnsFailure() {
        String token = "test-expired-uuid-token";
        Ticket ticket = new Ticket();
        ticket.setIdTicket("TK-APURIMAC-0526-0000001");
        ticket.setTokenConfirmacion(token);
        // Configurar expirado hace una hora
        ticket.setFechaExpiracionToken(LocalDateTime.now().minusHours(1));

        when(ticketRepository.findByTokenConfirmacion(token)).thenReturn(Optional.of(ticket));

        java.util.Map<String, Object> response = ticketService.processConfirmation(token, "confirmar", null, null);

        assertNotNull(response);
        assertFalse((Boolean) response.get("success"));
        assertTrue(((String) response.get("message")).contains("El enlace ha expirado"));
        assertNull(ticket.getTokenConfirmacion());
        assertNull(ticket.getFechaExpiracionToken());
        verify(ticketRepository, times(1)).save(ticket);
    }

    @Test
    public void testProcessConfirmationValidTokenSuccess() {
        String token = "test-valid-uuid-token";
        Ticket ticket = new Ticket();
        ticket.setIdTicket("TK-APURIMAC-0526-0000001");
        ticket.setEstado("PENDIENTE_CONFIRMACION");
        ticket.setTokenConfirmacion(token);
        ticket.setFechaExpiracionToken(LocalDateTime.now().plusHours(12));
        
        User creator = new User();
        creator.setUsername("percy");
        creator.setEmail("percy@sunafil.gob.pe");
        ticket.setCreador(creator);

        when(ticketRepository.findByTokenConfirmacion(token)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        java.util.Map<String, Object> response = ticketService.processConfirmation(token, "confirmar", null, null);

        assertNotNull(response);
        assertTrue((Boolean) response.get("success"));
        assertEquals("RESUELTO", ticket.getEstado());
        assertNull(ticket.getTokenConfirmacion());
        assertNull(ticket.getFechaExpiracionToken());
        verify(ticketRepository, times(1)).save(ticket);
    }
}
