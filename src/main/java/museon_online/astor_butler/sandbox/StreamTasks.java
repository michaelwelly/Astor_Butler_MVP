package museon_online.astor_butler.sandbox;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StreamTasks {
    public static void main(String[] args) {

        List<String> words =
                List.of("apple", "banana", "apple", "orange", "banana", "apple");

        Map<String, Long> frequency =
                words.stream()
                        .collect(Collectors.groupingBy(
                                word -> word,
                                Collectors.counting()
                        ));

        System.out.println(frequency);
}
}
