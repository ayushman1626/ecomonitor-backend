package com.example.demo.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleVerifierService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleVerifierService() {
        // Replace with your actual Google Client ID
        String GOOGLE_CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID";
        verifier = new GoogleIdTokenVerifier
                .Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(List.of(GOOGLE_CLIENT_ID)) // Set the client ID to verify against
                .build();
    }

    public GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            return (idToken != null) ? idToken.getPayload() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

