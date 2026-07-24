package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

@Configuration
public class AzureBlobStorageConfig {

	@Bean
	public BlobContainerClient kycBlobContainerClient(BlobStorageProperties properties) {
		HttpClient httpClient = new JdkHttpClientBuilder().build();
		TokenCredential credential = new DefaultAzureCredentialBuilder()
				.httpClient(httpClient)
				.build();

		BlobServiceClient serviceClient = new BlobServiceClientBuilder()
				.endpoint(properties.getEndpoint())
				.credential(credential)
				.httpClient(httpClient)
				.buildClient();

		return serviceClient.getBlobContainerClient(properties.getContainer());
	}
}
