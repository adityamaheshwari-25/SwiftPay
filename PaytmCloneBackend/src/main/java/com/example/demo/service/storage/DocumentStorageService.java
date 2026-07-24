package com.example.demo.service.storage;

import java.util.Optional;

public interface DocumentStorageService {

	void store(String storageKey, byte[] data, String contentType);

	Optional<byte[]> load(String storageKey);

	void delete(String storageKey);
}
