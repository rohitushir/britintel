package com.companieswatch.companieshouse.streaming;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamPositionRepository extends JpaRepository<StreamPosition, String> {
}
