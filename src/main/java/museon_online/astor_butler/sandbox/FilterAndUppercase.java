package museon_online.astor_butler.sandbox;

import java.util.List;
import java.util.stream.Collectors;

/*
Условие:
Дан список строк. Оставить только строки длиной более 3 символов и преобразовать их в верхний регистр.

List<String> words = List.of("hi", "hello", "sun", "stream", "go");

Ожидаемый результат: ["HELLO", "STREAM"]
 */
public class FilterAndUppercase {
    public static void main(String[] args) {
        List<String> words = List.of("hi", "hello", "sun", "stream", "go");
        List<String> result = words.stream()
                        .filter(word -> word.length()>3)
                                .map(String::toUpperCase)
                                        .collect(Collectors.toList());

        System.out.println(result); // [HELLO, STREAM]
    }
}