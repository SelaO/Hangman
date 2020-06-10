package com.netomedia.exam.hangman.player;

import com.netomedia.exam.hangman.model.ServerResponse;
import com.netomedia.exam.hangman.server.HangmanServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HangmanPlayer {

    private static HangmanServer server = new HangmanServer();
    private static final String DICTIONARY_PATH = "dictionary.txt";
    private static final List<String> dictionary = readDictionaryFromFile();

    public static void main(String[] args) throws Exception {
        ServerResponse serverResponse = server.startNewGame();
        if (serverResponse.getError() != null) {
            System.out.println(serverResponse.getError()); // TODO: need spec of errors to handle them better
        }

        String token = serverResponse.getToken();
        List<String> workingDictionary = getWordsOfSizeN(serverResponse.getHangman().length());

        Set<Character> correctLetters = new HashSet<>();
        Set<Character> wrongLetters = new HashSet<>();

        HashMap<Character, Integer> wordOccurrenceBuckets = createWordOccurrenceBuckets(workingDictionary);
        HashMap<Character, Integer> letterOccurrenceBuckets = letterOccurrenceBuckets(workingDictionary);
        List<Character> characters = sortMap(wordOccurrenceBuckets, letterOccurrenceBuckets);

        int solutionArrayIndex = 0;
        while (!serverResponse.getGameEnded()) {

            String chosenLetter = characters.get(solutionArrayIndex).toString();

            serverResponse = server.guess(token, chosenLetter);
            if (serverResponse.getError() != null) {
                System.out.println(serverResponse.getError()); // TODO: need spec of errors to handle them better
                if (serverResponse.getError().indexOf("Sorry, you lost!") > 0) {
                    break;
                }
            }

            token = serverResponse.getToken();

            if (serverResponse.isCorrect()) {
                correctLetters.add(chosenLetter.charAt(0));
            } else {
                wrongLetters.add(chosenLetter.charAt(0));
            }

            int prevWorkingDictionarySize = workingDictionary.size();
            workingDictionary = filterWordsFromDictionaryAccordingToCharacters(workingDictionary, correctLetters, wrongLetters);

            if (prevWorkingDictionarySize == workingDictionary.size()) {
                solutionArrayIndex++; // move to the next letter if nothing has changed
            } else {
                solutionArrayIndex = 0; // take the first most common letter
                wordOccurrenceBuckets = createWordOccurrenceBuckets(workingDictionary);
                letterOccurrenceBuckets = letterOccurrenceBuckets(workingDictionary);
                characters = sortMap(wordOccurrenceBuckets, letterOccurrenceBuckets);
                characters.removeAll(correctLetters); // remove letters we already checked
            }
        }

        System.out.println("Game Over");
        System.out.println(serverResponse);
    }

    private static List<String> filterWordsFromDictionaryAccordingToCharacters(List<String> workingDictionary, Set<Character> correctLetters, Set<Character> wrongLetters) {
        // There's probably a better way with regex but I don't have time
        return workingDictionary.stream().filter(word -> {
            for (int i = 0; i < word.length(); i++) {
                Character currChar = word.charAt(i);
                if (wrongLetters.contains(currChar)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    private static List<Character> sortMap(HashMap<Character, Integer> wordsMap, HashMap<Character, Integer> letterMap) {
        ArrayList<Map.Entry<Character, Integer>> entries = new ArrayList<>(wordsMap.entrySet());
        entries.sort((a, b) -> {
            int comparision = b.getValue() - a.getValue();
            if (comparision == 0) {
                return letterMap.get(b.getKey()) - letterMap.get(a.getKey());
            }
            return comparision;
        });
        return entries.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private static HashMap<Character, Integer> createWordOccurrenceBuckets(List<String> workingDictionary) {
        HashMap<Character, Integer> result = new HashMap<>();
        HashSet<Character> currentLetters = new HashSet<>(); // because we want to count each letter once for every word

        for (String word : workingDictionary) {
            for (int i = 0; i < word.length(); i++) {
                Character currChar = word.charAt(i);
                currentLetters.add(currChar);
            }

            currentLetters.forEach(character -> {
                if (result.containsKey(character)) {
                    result.put(character, result.get(character) + 1);
                } else {
                    result.put(character, 1);
                }
            });
            currentLetters.clear();
        }

        return result;
    }

    private static HashMap<Character, Integer> letterOccurrenceBuckets(List<String> workingDictionary) {
        HashMap<Character, Integer> result = new HashMap<>();

        for (String word : workingDictionary) {
            for (int i = 0; i < word.length(); i++) {
                Character character = word.charAt(i);
                if (result.containsKey(character)) {
                    result.put(character, result.get(character) + 1);
                } else {
                    result.put(character, 1);
                }
            }
        }

        return result;
    }

    private static List<String> getWordsOfSizeN(int n) {
        return dictionary.stream().filter(word -> word.length() == n).collect(Collectors.toList());
    }

    private static List<String> readDictionaryFromFile() {
        Stream<String> stream = new BufferedReader(new InputStreamReader(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(DICTIONARY_PATH)))).lines();
        return stream.collect(Collectors.toList());
    }
}
