package com.leadflow.leadflow_backend.service;
import com.leadflow.leadflow_backend.domain.Lead;
import com.leadflow.leadflow_backend.domain.LeadStatus;
import com.leadflow.leadflow_backend.model.LeadDTO;
import com.leadflow.leadflow_backend.repos.LeadRepository;
import com.leadflow.leadflow_backend.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class LeadService {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TelegramService telegramService;


    public LeadDTO createLead(final LeadDTO leadDTO, String userId) {
        log.info("Creating new lead for user: {}, name: {}", userId, leadDTO.getName());

        final Lead lead = new Lead();
        mapToEntity(leadDTO, lead);

        lead.setUserId(userId);
        lead.setCreatedBy(userId);
        lead.setCreatedAt(LocalDateTime.now());
        lead.setUpdatedAt(LocalDateTime.now());

        final Lead savedLead = leadRepository.save(lead);
        log.info("Lead saved with ID: {}", savedLead.getId());

        if (savedLead.getEmail() != null && !savedLead.getEmail().isBlank()) {
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendEmail(savedLead.getEmail(), savedLead.getName(), "AUTO_NEW_LEAD");
                    log.info("Welcome email sent to: {}", savedLead.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send welcome email: {}", e.getMessage());
                }
            });
        }

        CompletableFuture.runAsync(() -> {
            try {
                telegramService.sendMessage(
                        savedLead.getName() != null ? savedLead.getName() : "New Lead",
                        savedLead.getPhone() != null ? savedLead.getPhone() : "",
                        savedLead.getSource() != null ? savedLead.getSource() : "Direct",
                        "AUTO_NEW_LEAD", "", ""
                );
            } catch (Exception e) {
                log.error("Telegram notification failed: {}", e.getMessage());
            }
        });

        return mapToDTO(savedLead, new LeadDTO());
    }


    public List<LeadDTO> getAllLeadsForUser(String userId, String status) {
        log.info("Fetching leads for user: {}, status: {}", userId, status);
        List<Lead> leads;

        if (status != null && !status.isEmpty()) {
            leads = leadRepository.findByUserIdAndStatus(userId, LeadStatus.valueOf(status));
        } else {
            leads = leadRepository.findByUserId(userId);
        }

        return leads.stream()
                .map(lead -> mapToDTO(lead, new LeadDTO()))
                .collect(Collectors.toList());
    }


    public LeadDTO getLeadById(final String id, String userId) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        if (!lead.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return mapToDTO(lead, new LeadDTO());
    }

    public Lead updateLead(String id, LeadDTO partialLead, String userId) {
        log.info("Updating lead ID: {} for user: {}", id, userId);

        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        if (!existingLead.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (partialLead.getName() != null && !partialLead.getName().isEmpty()) {
            existingLead.setName(partialLead.getName());
        }
        if (partialLead.getEmail() != null && !partialLead.getEmail().isEmpty()) {
            existingLead.setEmail(partialLead.getEmail());
        }
        if (partialLead.getPhone() != null && !partialLead.getPhone().isEmpty()) {
            existingLead.setPhone(partialLead.getPhone());
        }
        if (partialLead.getSource() != null) {
            existingLead.setSource(partialLead.getSource());
        }
        if (partialLead.getStatus() != null) {
            existingLead.setStatus(LeadStatus.valueOf(partialLead.getStatus()));
        }
        if (partialLead.getNotes() != null) {
            existingLead.setNotes(partialLead.getNotes());
        }

        existingLead.setUpdatedAt(LocalDateTime.now());
        return leadRepository.save(existingLead);
    }


    public List<Lead> searchLeads(String query, String userId) {
        log.info("Searching leads for user: {}, query: {}", userId, query);
        List<Lead> leads;

        if (query == null || query.isEmpty()) {
            leads = leadRepository.findByUserId(userId);
        } else if (query.matches("\\d+")) {
            leads = leadRepository.findByPhoneContaining(query);
        } else {
            leads = leadRepository.findByNameContainingIgnoreCase(query);
        }

        return leads.stream()
                .filter(lead -> userId.equals(lead.getUserId()))
                .collect(Collectors.toList());
    }


    public void deleteLead(final String id, String userId) {
        log.warn("Deleting lead ID: {} for user: {}", id, userId);

        Lead existingLead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        if (!existingLead.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        leadRepository.deleteById(id);
        log.info("Lead deleted. ID: {}", id);
    }


    public boolean idExists(final String id) {
        return leadRepository.existsById(id);
    }

    private LeadDTO mapToDTO(final Lead lead, final LeadDTO leadDTO) {
        leadDTO.setId(lead.getId());
        leadDTO.setName(lead.getName());
        leadDTO.setEmail(lead.getEmail());
        leadDTO.setPhone(lead.getPhone());
        leadDTO.setSource(lead.getSource());
        leadDTO.setStatus(lead.getStatus() == null ? null : lead.getStatus().name());
        leadDTO.setNotes(lead.getNotes());
        leadDTO.setUserId(lead.getUserId());
        leadDTO.setCreatedBy(lead.getCreatedBy());
        leadDTO.setCreatedAt(lead.getCreatedAt());
        leadDTO.setUpdatedAt(lead.getUpdatedAt());
        return leadDTO;
    }

    private Lead mapToEntity(final LeadDTO leadDTO, final Lead lead) {
        lead.setName(leadDTO.getName());
        lead.setEmail(leadDTO.getEmail());
        lead.setPhone(leadDTO.getPhone());
        lead.setSource(leadDTO.getSource());
        lead.setStatus(leadDTO.getStatus() == null ? null : LeadStatus.valueOf(leadDTO.getStatus()));
        lead.setNotes(leadDTO.getNotes());
        return lead;
    }
}