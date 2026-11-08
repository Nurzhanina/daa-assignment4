import java.util.*;

class SearchTiming {
    static final String[] names = {"naive", "kmp", "rk"};
    static volatile Object result;

    static Map<String,Object> run(int method, Map<String,Object> request) {
        return switch (method) {
            case 0 -> Algorithms.naive(request);
            case 1 -> Algorithms.kmp(request);
            default -> Algorithms.rk(request);
        };
    }

    public static void main(String[] args) {
        System.out.println("case,n,m,seed,method,r1_us,r2_us,r3_us,median_us,matches");
        for (int n : new int[]{300, 1200, 3000}) {
            int m = n / 10;
            for (boolean repetitive : new boolean[]{false, true}) {
                long seed = 2515 + n;
                Random random = new Random(seed);
                StringBuilder chars = new StringBuilder();
                for (int i = 0; i < n; i++) chars.append(repetitive ? 'a' : (char) ('a' + random.nextInt(4)));
                String text = chars.toString();
                String pattern = text.substring(n / 3, n / 3 + m);
                Map<String,Object> input = Map.of("text", text, "pattern", pattern);
                List<Integer> expected = new ArrayList<>();
                for (int i = 0; i <= n - m; i++) if (text.startsWith(pattern, i)) expected.add(i);
                for (int method = 0; method < 3; method++) {
                    if (!expected.equals(run(method, input).get("positions"))) throw new AssertionError();
                    for (int j = 0; j < 20; j++) result = run(method, input);
                }
                double[][] samples = new double[3][3];
                for (int r = 0; r < 3; r++) {
                    for (int turn = 0; turn < 3; turn++) {
                        int method = (turn + r) % 3;
                        long start = System.nanoTime();
                        for (int j = 0; j < 20; j++) result = run(method, input);
                        samples[method][r] = (System.nanoTime() - start) / 20000.0;
                    }
                }
                for (int method = 0; method < 3; method++) {
                    double[] s = samples[method], sorted = s.clone();
                    Arrays.sort(sorted);
                    System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%s,%.3f,%.3f,%.3f,%.3f,%d%n",
                        repetitive ? "repetitive" : "random", n, m, seed, names[method],
                        s[0], s[1], s[2], sorted[1], expected.size());
                }
            }
        }
    }
}
