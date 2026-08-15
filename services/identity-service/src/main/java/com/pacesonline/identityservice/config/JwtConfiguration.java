package com.pacesonline.identityservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {
    
    @Bean
    @Profile({"local","test"})
    KeyPair jwtKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) jwtKeyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) jwtKeyPair.getPrivate();
        return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
    }

    @Bean
    @Profile("prod")
    KeyPair productionJwtKeyPair(TokenProperties tokenProperties, ResourceLoader resourceLoader
    ) throws IOException {

        if (tokenProperties.privateKeyLocation() == null
                || tokenProperties.publicKeyLocation() == null) {
            throw new IllegalStateException(
                    "JWT key locations must be configured in production"
            );
        }

        Resource publicKeyResource = resourceLoader.getResource(tokenProperties.publicKeyLocation());

        Resource privateKeyResource = resourceLoader.getResource(tokenProperties.privateKeyLocation());

        RSAPublicKey publicKey;

        try (InputStream inputStream = publicKeyResource.getInputStream()) {
            publicKey = RsaKeyConverters
                    .x509()
                    .convert(inputStream);
        }

        RSAPrivateKey privateKey;

        try (InputStream inputStream =
                    privateKeyResource.getInputStream()) {
            privateKey = RsaKeyConverters
                    .pkcs8()
                    .convert(inputStream);
        }

        return new KeyPair(publicKey, privateKey);
    }
}