import java.util.*;

public final class Algorithms {
    static Map<String,Object> lcs(Map<String,Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] length = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                length[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                    ? length[i - 1][j - 1] + 1 : Math.max(length[i - 1][j], length[i][j - 1]);
            }
        }
        StringBuilder sequence = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sequence.append(a.charAt(--i));
                j--;
            } else if (length[i - 1][j] >= length[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return Map.of("length", length[n][m], "sequence", sequence.reverse().toString());
    }

    static Map<String,Object> merge(Map<String,Object> input) {
        List<?> sizes = (List<?>) input.get("sizes");
        int n = sizes.size();
        if (n < 2) return Map.of("cost", 0L, "plan", List.of());
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + ((Number) sizes.get(i)).longValue();
        long[][] cost = new long[n][n];
        int[][] split = new int[n][n];
        for (int width = 2; width <= n; width++) {
            for (int left = 0; left + width <= n; left++) {
                int right = left + width - 1;
                long sum = prefix[right + 1] - prefix[left];
                cost[left][right] = Long.MAX_VALUE;
                for (int k = left; k < right; k++) {
                    long candidate = cost[left][k] + cost[k + 1][right] + sum;
                    if (candidate < cost[left][right]) {
                        cost[left][right] = candidate;
                        split[left][right] = k;
                    }
                }
            }
        }
        List<int[]> plan = new ArrayList<>();
        appendMerges(0, n - 1, split, plan);
        return Map.of("cost", cost[0][n - 1], "plan", plan);
    }

    private static void appendMerges(int left, int right, int[][] split, List<int[]> plan) {
        if (left == right) return;
        int k = split[left][right];
        appendMerges(left, k, split, plan);
        appendMerges(k + 1, right, split, plan);
        plan.add(new int[]{left, k, right});
    }

    static Map<String,Object> tree(Map<String,Object> input) {
        int n = ((Number) input.get("n")).intValue();
        if (n == 0) return Map.of("weight", 0L, "selected", List.of());
        List<?> weights = (List<?>) input.get("weights");
        int[] head = new int[n], next = new int[2 * (n - 1)], to = new int[next.length];
        Arrays.fill(head, -1);
        int arc = 0;
        for (Object row : (List<?>) input.get("edges")) {
            List<?> edge = (List<?>) row;
            int u = ((Number) edge.get(0)).intValue(), v = ((Number) edge.get(1)).intValue();
            to[arc] = v; next[arc] = head[u]; head[u] = arc++;
            to[arc] = u; next[arc] = head[v]; head[v] = arc++;
        }
        int[] parent = new int[n], order = new int[n];
        parent[0] = -1;
        int used = 1;
        for (int i = 0; i < used; i++) {
            int u = order[i];
            for (int e = head[u]; e != -1; e = next[e]) {
                int v = to[e];
                if (v == parent[u]) continue;
                parent[v] = u;
                order[used++] = v;
            }
        }
        long[] take = new long[n], skip = new long[n];
        for (int i = n - 1; i >= 0; i--) {
            int v = order[i];
            take[v] += ((Number) weights.get(v)).longValue();
            if (parent[v] >= 0) {
                take[parent[v]] += skip[v];
                skip[parent[v]] += Math.max(skip[v], take[v]);
            }
        }
        boolean[] chosen = new boolean[n];
        List<Integer> selected = new ArrayList<>();
        for (int v : order) {
            if ((parent[v] < 0 || !chosen[parent[v]]) && take[v] > skip[v]) {
                chosen[v] = true;
                selected.add(v);
            }
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    private static boolean matches(String text, String pattern, int start) {
        for (int j = 0; j < pattern.length(); j++) {
            if (text.charAt(start + j) != pattern.charAt(j)) return false;
        }
        return true;
    }

    static Map<String,Object> naive(Map<String,Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i <= text.length() - pattern.length(); i++) {
            if (matches(text, pattern, i)) starts.add(i);
        }
        return Map.of("positions", starts);
    }

    private static Map<String,Object> boundaries(int n) {
        int[] starts = new int[n + 1];
        for (int i = 0; i <= n; i++) starts[i] = i;
        return Map.of("positions", starts);
    }

    static Map<String,Object> kmp(Map<String,Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        int m = pattern.length();
        if (m == 0) return boundaries(text.length());
        int[] prefix = new int[m];
        for (int i = 1, j = 0; i < m; i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = prefix[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            prefix[i] = j;
        }
        List<Integer> starts = new ArrayList<>();
        for (int i = 0, j = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = prefix[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) {
                starts.add(i - m + 1);
                j = prefix[m - 1];
            }
        }
        return Map.of("positions", starts);
    }

    static Map<String,Object> rk(Map<String,Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        if (m == 0) return boundaries(n);
        if (m > n) return Map.of("positions", List.of());
        final int base = 31, mod = 101;
        int high = 1, target = 0, window = 0;
        for (int i = 0; i < m; i++) {
            target = (base * target + pattern.charAt(i)) % mod;
            window = (base * window + text.charAt(i)) % mod;
            if (i + 1 < m) high = high * base % mod;
        }
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i <= n - m; i++) {
            if (window == target && matches(text, pattern, i)) starts.add(i);
            if (i + m < n) {
                int tail = Math.floorMod(window - high * text.charAt(i), mod);
                window = (base * tail + text.charAt(i + m)) % mod;
            }
        }
        return Map.of("positions", starts);
    }

    private static int secondRank(int[] rank, int i, int width) {
        return i + width < rank.length ? rank[i + width] : -1;
    }

    static Map<String,Object> suffix(Map<String,Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        Integer[] sa = new Integer[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i);
        }
        for (int width = 1; width < n; width *= 2) {
            final int[] old = rank;
            final int step = width;
            Arrays.sort(sa, Comparator.<Integer>comparingInt(i -> old[i])
                .thenComparingInt(i -> secondRank(old, i, step)));
            rank = new int[n];
            for (int j = 1; j < n; j++) {
                int u = sa[j - 1], v = sa[j];
                boolean differs = old[u] != old[v] || secondRank(old, u, step) != secondRank(old, v, step);
                rank[v] = rank[u] + (differs ? 1 : 0);
            }
            if (rank[sa[n - 1]] == n - 1) break;
        }
        int[] position = new int[n], lcp = new int[n];
        for (int i = 0; i < n; i++) position[sa[i]] = i;
        int bestStart = 0, bestLength = 0;
        for (int i = 0, common = 0; i < n; i++) {
            int pos = position[i];
            if (pos == 0) {
                common = 0;
                continue;
            }
            int j = sa[pos - 1];
            while (i + common < n && j + common < n
                    && text.charAt(i + common) == text.charAt(j + common)) common++;
            lcp[pos] = common;
            if (common > bestLength) {
                bestStart = i;
                bestLength = common;
            }
            if (common > 0) common--;
        }
        return Map.of("sa", sa, "lcp", lcp, "repeated", text.substring(bestStart, bestStart + bestLength));
    }

    static Map<String,Object> aho(Map<String,Object> input) {
        List<?> patterns = (List<?>) input.get("patterns"), texts = (List<?>) input.get("texts");
        final int alphabet = 95;
        List<int[]> transitions = new ArrayList<>();
        transitions.add(new int[alphabet]);
        int[] terminal = new int[patterns.size()];
        for (int i = 0; i < terminal.length; i++) {
            String pattern = (String) patterns.get(i);
            int state = 0;
            for (int j = 0; j < pattern.length(); j++) {
                int symbol = pattern.charAt(j) - 32;
                int[] row = transitions.get(state);
                if (row[symbol] == 0) {
                    row[symbol] = transitions.size();
                    transitions.add(new int[alphabet]);
                }
                state = row[symbol];
            }
            terminal[i] = state;
        }
        int states = transitions.size();
        int[] failure = new int[states], queue = new int[states];
        int used = 0;
        for (int child : transitions.get(0)) if (child != 0) queue[used++] = child;
        for (int i = 0; i < used; i++) {
            int u = queue[i];
            int[] row = transitions.get(u), fallback = transitions.get(failure[u]);
            for (int symbol = 0; symbol < alphabet; symbol++) {
                int child = row[symbol];
                if (child == 0) {
                    row[symbol] = fallback[symbol];
                } else {
                    failure[child] = fallback[symbol];
                    queue[used++] = child;
                }
            }
        }
        long[][] counts = new long[texts.size()][terminal.length];
        for (int i = 0; i < texts.size(); i++) {
            String text = (String) texts.get(i);
            long[] visits = new long[states];
            int state = 0;
            for (int j = 0; j < text.length(); j++) {
                state = transitions.get(state)[text.charAt(j) - 32];
                visits[state]++;
            }
            for (int j = used - 1; j >= 0; j--) {
                int u = queue[j];
                visits[failure[u]] += visits[u];
            }
            for (int j = 0; j < terminal.length; j++) counts[i][j] = visits[terminal[j]];
        }
        return Map.of("counts", counts);
    }
}
