package museon_online.astor_butler.sandbox;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/*
Условие:
Дан список слов. Найти самое длинное слово и сразу вернуть его в верхнем регистре как Optional<String>.

 */
public class LongestUppercase {
    public static void main(String[] args) {
        List<String> words = List.of("java", "stream", "collector", "api");
        Optional<String> result = words.stream()
                        .max(Comparator.comparingInt(String::length))
                .map(String::toUpperCase);

        System.out.println(result); // Optional[COLLECTOR]
    }
}