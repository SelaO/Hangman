package com.netomedia.exam.hangman.server;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netomedia.exam.hangman.model.ServerResponse;

public class HangmanServer {

    private static final String START_NEW_GAME_API = "http://netomedia-hangman.herokuapp.com/startNewGame";
    private static final String GUESS_API = "http://netomedia-hangman.herokuapp.com/guess";

    HttpClient httpClient = HttpClientBuilder.create().build();
    final ObjectMapper objectMapper = new ObjectMapper();

    public ServerResponse startNewGame() throws Exception {
        URIBuilder builder = new URIBuilder(START_NEW_GAME_API);
        ServerResponse response = sendHangmanRequest(builder.build());
        System.out.println("*** Initialized a new Hangman Game! ***");
        return response;
    }

    public ServerResponse guess(String token, String guess) throws Exception {
        URIBuilder builder = new URIBuilder(GUESS_API);
        builder.setParameter("token", token).setParameter("guess", guess);

        ServerResponse response = sendHangmanRequest(builder.build());
        System.out.println("*** Successfully Sent a Guess: " + guess + " request***");
        return response;
    }

    private ServerResponse sendHangmanRequest(URI url) throws Exception {
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("accept", "application/json");

            HttpResponse httpResponse = httpClient.execute(httpGet);

            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + httpResponse.getStatusLine().getStatusCode());
            }

            final ServerResponse serverResponse = objectMapper.readValue(httpResponse.getEntity().getContent(), ServerResponse.class);

            return serverResponse;
        } catch (IOException e) {
            throw new Exception("Unable to Send Hangman Request. Reason:" + e.getMessage());
        }
    }
}
