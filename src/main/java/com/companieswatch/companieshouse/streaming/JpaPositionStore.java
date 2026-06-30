package com.companieswatch.companieshouse.streaming;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaPositionStore implements PositionStore {

    private final StreamPositionRepository repository;

    public JpaPositionStore(StreamPositionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Long load(CompaniesHouseStream stream) {
        return repository.findById(stream.configName())
                .map(StreamPosition::getLastTimepoint)
                .orElse(null);
    }

    @Override
    @Transactional
    public void save(CompaniesHouseStream stream, Long timepoint) {
        if (timepoint == null) {
            return;
        }
        StreamPosition position = repository.findById(stream.configName())
                .orElseGet(() -> new StreamPosition(stream.configName()));
        position.setLastTimepoint(timepoint);
        repository.save(position);
    }

    @Override
    @Transactional
    public void clear(CompaniesHouseStream stream) {
        repository.findById(stream.configName()).ifPresent(p -> {
            p.setLastTimepoint(null);
            repository.save(p);
        });
    }
}
