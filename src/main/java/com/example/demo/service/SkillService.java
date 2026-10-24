package com.example.demo.service;

import com.example.demo.entity.Skill;
import com.example.demo.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SkillService {
    private final SkillRepository skills;

    /** Finds existing skills (case-insensitive) or creates new ones. */
    public Set<Skill> resolve(Set<String> names) {
        Set<Skill> result = new HashSet<>();
        if (names == null) return result;
        for (String raw : names) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty() || name.length() > 60) continue;
            result.add(skills.findByNameIgnoreCase(name)
                    .orElseGet(() -> skills.save(Skill.builder().name(name).build())));
        }
        return result;
    }
}