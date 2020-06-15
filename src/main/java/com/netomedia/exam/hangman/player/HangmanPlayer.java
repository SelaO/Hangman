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
        HashMap<Character, Integer> letterOccurrenceBuckets = createLetterOccurrenceBuckets(workingDictionary);
        List<Character> characters = sortMap(wordOccurrenceBuckets, letterOccurrenceBuckets);

        String chosenLetterOrWord;
        while (!serverResponse.getGameEnded()) {

            if (workingDictionary.size() == 1) {
                chosenLetterOrWord = workingDictionary.get(0);
            } else {
                chosenLetterOrWord = characters.get(0).toString();
            }

            serverResponse = server.guess(token, chosenLetterOrWord);
            if (serverResponse.getError() != null) {
                System.out.println(serverResponse.getError()); // TODO: need spec of errors to handle them better
                if (serverResponse.getError().indexOf("Sorry, you lost!") > 0) {
                    break;
                }
            }

            token = serverResponse.getToken();

            if (serverResponse.isCorrect()) {
                correctLetters.add(chosenLetterOrWord.charAt(0));
            } else {
                wrongLetters.add(chosenLetterOrWord.charAt(0));
            }

            int prevWorkingDictionarySize = workingDictionary.size();
            workingDictionary = filterWordsFromDictionaryAccordingToCharacters(workingDictionary, correctLetters, wrongLetters);

            if (prevWorkingDictionarySize != workingDictionary.size()) {
                wordOccurrenceBuckets = createWordOccurrenceBuckets(workingDictionary);
                letterOccurrenceBuckets = createLetterOccurrenceBuckets(workingDictionary);
                characters = sortMap(wordOccurrenceBuckets, letterOccurrenceBuckets);
            }
            characters.removeAll(correctLetters); // remove letters we already checked

        }

        System.out.println("Game Over");
        System.out.println(serverResponse);
    }

    private static List<String> filterWordsFromDictionaryAccordingToCharacters(List<String> workingDictionary, Set<Character> correctLetters, Set<Character> wrongLetters) {
        // There's probably a better way with regex but I don't have time
        return workingDictionary.stream().filter(word -> {
            boolean result = correctLetters.size() <= 0;
            for (int i = 0; i < word.length(); i++) {
                Character currChar = word.charAt(i);
                if (wrongLetters.contains(currChar)) {
                    return false;
                }

                if(correctLetters.contains(currChar)) {
                    result = true; // don't add words that don't contain any correct letter
                }
            }
            return result;
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

    private static HashMap<Character, Integer> createLetterOccurrenceBuckets(List<String> workingDictionary) {
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
