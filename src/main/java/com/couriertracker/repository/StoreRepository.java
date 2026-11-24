package com.couriertracker.repository;

import com.couriertracker.domain.Store;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Repository
public class StoreRepository {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String storesFilePath;

    private volatile List<Store> stores = Collections.emptyList();

    public StoreRepository(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${courier.tracking.stores-file-path:classpath:stores.json}") String storesFilePath) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.storesFilePath = storesFilePath;
    }

    @PostConstruct
    public void loadStores() {
        Resource resource = resourceLoader.getResource(storesFilePath);
        try (InputStream inputStream = resource.getInputStream()) {
            List<Store> loaded = objectMapper.readValue(inputStream, new TypeReference<>() {});
            this.stores = Collections.unmodifiableList(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load stores from " + storesFilePath, e);
        }
    }

    public List<Store> findAll() {
        return stores;
    }
}