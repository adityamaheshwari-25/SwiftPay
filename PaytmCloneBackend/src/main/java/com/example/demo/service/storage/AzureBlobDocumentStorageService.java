package com.example.demo.service.storage;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.example.demo.exception.DocumentStorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AzureBlobDocumentStorageService implements DocumentStorageService {

	private final BlobContainerClient containerClient;

	@Override
	public void store(String storageKey, byte[] data, String contentType) {
		try {
			BlobClient blobClient = containerClient.getBlobClient(storageKey);
			blobClient.upload(BinaryData.fromBytes(data), true);
			blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
		} catch (BlobStorageException ex) {
			throw new DocumentStorageException("Unable to store document in Azure Blob Storage", ex);
		}
	}

	@Override
	public Optional<byte[]> load(String storageKey) {
		try {
			BlobClient blobClient = containerClient.getBlobClient(storageKey);
			return Optional.of(blobClient.downloadContent().toBytes());
		} catch (BlobStorageException ex) {
			if (ex.getStatusCode() == 404) {
				return Optional.empty();
			}
			throw new DocumentStorageException("Unable to read document from Azure Blob Storage", ex);
		}
	}

	@Override
	public void delete(String storageKey) {
		try {
			containerClient.getBlobClient(storageKey).deleteIfExists();
		} catch (BlobStorageException ex) {
			throw new DocumentStorageException("Unable to delete document from Azure Blob Storage", ex);
		}
	}
}
