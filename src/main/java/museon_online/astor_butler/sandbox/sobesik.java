package museon_online.astor_butler.sandbox;
// метод тру/фолс если число четное или нечетное, но нельзя использовать остаток от деления
//(время исполнения константное)
// тест на этот метод
public class sobesik {

    public static boolean isEven(int number) {
        return (number & 1) == 0;
    }

    public static void main(String[] args) {

        int[] testCases = {
                0,
                1,
                2,
                3,
                -1,
                -2,
                Integer.MAX_VALUE,   // 2147483647 (нечётное)
                Integer.MIN_VALUE,   // -2147483648 (чётное)
                999_999_998,
                999_999_999
        };

        for (int num : testCases) {
            System.out.println(num + " -> " + isEven(num));
        }
    }
}