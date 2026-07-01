package service;

import java.util.List;

import org.springframework.stereotype.Service;

import model.MentorAvailability;
import model.User;
import repository.MentorAvailabilityRepository;

@Service
public class AvailabilityService {
    private final MentorAvailabilityRepository repository;

    public AvailabilityService(MentorAvailabilityRepository repository) {
        this.repository = repository;
    }

    public List<MentorAvailability> getAvailability(User mentor) {
        return repository.findByMentor(mentor);
    }

    public MentorAvailability addSlot(User mentor, MentorAvailability slot) {
        slot.setMentor(mentor);
        return repository.save(slot);
    }

    public void removeSlot(Long slotId, User mentor) {
        MentorAvailability slot = repository.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!slot.getMentor().getId().equals(mentor.getId()))
            throw new SecurityException("Not your slot");
        repository.deleteById(slotId);
    }
}