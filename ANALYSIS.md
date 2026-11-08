# Assignment 4 · Dynamic programming and string processing

Nurzhan Zholaman · SE-2515 · Java

## Environment and reproduction

Windows 11, AMD Ryzen 5 7535HS, 15.25 GiB usable RAM. Local measurements used OpenJDK 25.0.1, Java 17 compilation target, and a 384 MiB heap. The starter reads UTF-8 JSON Lines. Each command allocates its own state. From the repository root, with Python 3.12+ and a JDK on PATH, run:

```sh
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/student/examples.jsonl
python tests/student/verify.py
javac --release 17 -cp Java/build -d Java/build tests/student/SearchTiming.java
java -Xmx384m -cp Java/build SearchTiming
```

The first two commands compile the application. To run it interactively after setup use `java -Xmx384m -cp Java/build Main`. The verifier accepts a JAVA environment variable pointing to the executable. In IntelliJ IDEA select a JDK and mark `Java/source` as Sources Root, then run Main. No library downloads are necessary for the Java implementation.

## Algorithms

**LCS.** Let D[i,j] be the maximum common subsequence length for the first i and j characters. The empty row and column are zero. If the last characters match, an optimum can use them together: its final shared character can be moved to these final positions, or a matching pair can extend a common subsequence of the preceding prefixes. Thus D[i,j]=1+D[i−1,j−1]. On a mismatch, at least one final character is unused, giving max(D[i−1,j],D[i,j−1]). Iteration by increasing i and j fills all predecessors first. Reconstruction walks diagonally on a match and otherwise follows a neighbour with equal optimum value, preferring up on ties. Reversing the recorded characters restores input order. For lengths n,m the time and auxiliary table size are O((n+1)(m+1)); the witness takes O(L+1) space for L characters. The fixture aXbYc / abc separates subsequence from substring.

**Adjacent merge.** D[l,r] is the minimum cost to turn the closed original interval into one block. Singletons cost zero. The last merge must join [l,k] to [k+1,r] for some l≤k<r. Its price is prefix[r+1]−prefix[l]. Minimizing that price plus both child costs considers all possible last merges; each candidate can be executed by finishing the two children first. Increasing interval width makes child optima available. The first minimum split is saved. A postorder traversal emits the child instructions before [l,k,r], so both blocks exist when consumed. For n≤1 the result is empty. Time O(n³), auxiliary O(n²), plan O(n), with constant costs for empty input. Reconstruction recursion is bounded by 200. Long prefix sums and costs accommodate a 200-billion combined size and cumulative costs above 32 bits. In [1,100,1] the outside ones cannot merge first.

**Tree independent set.** Root the tree at vertex 0 using integer adjacency arrays. The traversal records every parent before its children. In reverse order, take[v] is the optimum containing v and skip[v] the optimum excluding it. A chosen vertex forces every child to skip. An excluded vertex allows each child its better state. The resulting recurrences add the vertex weight to the sum of child skips, or sum child maxima, respectively. Leaf values are its weight and zero. Distinct child subtrees have no cross edges, so their optima combine independently. Forward reconstruction skips a child of a chosen parent, otherwise choosing take only on strict improvement. Ties favour skipping. The empty set handles negative weights. Two linear traversals and reconstruction take O(n+1) time and auxiliary space; selected IDs take O(k+1). The 100,000-vertex chain checks depth and 64-bit totals without recursion.

**Naive search.** For text length n and pattern length m, try every start from 0 through n−m inclusive. Compare characters until the first disagreement or the end of the pattern. A successful start advances by just one, preserving overlaps. For m=0 the inner loop succeeds immediately at every boundary, including n. For m>n the start loop is empty. Time O(nm+n+1), auxiliary O(1), result O(z+1) for z matches. The empty pattern and the starts 0,1,2 in aaaaa / aaa test these bounds.

**KMP.** prefix[i] is the longest proper border length of pattern[0..i]. During its construction and the text scan, j is the length of the currently matched prefix. On mismatch, prefix[j−1] is the next longest candidate compatible with the characters already seen. Repeating this step visits all viable shorter borders. A matching character extends the prefix. At length m report i−m+1 and fall back to prefix[m−1], preserving overlapping occurrences. Empty patterns return the boundary array before indexing. Each fallback decreases j, whose total increases are linear in the input consumed. Time O(n+m+z+1), auxiliary O(m+1), output O(z+1).

**Exact Rabin–Karp.** A window hash is a base-31 polynomial over ASCII codes modulo 101 with the highest power at the left. Subtract the outgoing code times 31^(m−1), reduce with floorMod, multiply by 31 and add the incoming code. The invariant is equality to a fresh polynomial evaluation on that window. Different fingerprints rule out equality. Equal fingerprints trigger a full character comparison. For example ab and dj both hash to 75; only actual ab starts are returned. Empty and oversized patterns are handled first. For 1≤m≤n and c candidates the time is O(n+m+cm), auxiliary O(1), output O(z+1). Every window can be a candidate. The bounded residues and printable ASCII keep intermediate hash arithmetic within int. Positions are within the input length.

**Suffix array and Kasai LCP.** Initially each rank is its character code. With prefix width w, sort starting indices by the two ranks at i and i+w. A missing right half has rank −1, so a shorter equal-prefix suffix sorts first. Renumber equal pairs equally in sorted order, then double w. Inductively these pairs compare the first 2w characters correctly. All distinct ranks certify the final order; otherwise full length eventually distinguishes every suffix. Only n nonempty suffix indices are stored. Java comparison sorting uses O(n log n) per round, hence O(n log²(n+1)) total.

Kasai uses the inverse permutation to find each suffix's immediate predecessor. If the two suffixes share h characters, removing the first character preserves at least h−1 common characters with the shifted predecessor. The new immediate predecessor shares at least that much because equal-prefix suffixes are contiguous. Resume comparisons from this lower bound, resetting at rank zero. Increments are linear in total (ordinary steps drop at most one and rank zero occurs once). The largest adjacent LCP equals the longest repeat length because suffixes beginning with any repeated substring form a contiguous sorted interval. Return one substring attaining that maximum; ties keep the first maximum encountered by text index. Empty input produces empty arrays. Kasai time O(n+1), auxiliary and total output O(n+1). Sorting uses arrays of indices and ranks throughout. The all-equal case checks overlap, exact predecessor indexing and omission of the empty suffix.

**Aho–Corasick.** Each trie node represents one prefix. Keep a terminal node for every original dictionary position, sharing nodes for duplicates. The 95 transitions per state index printable ASCII by code minus 32. Breadth-first construction fills a missing transition from the failure state's completed row. For a real child the same fallback transition supplies its longest proper suffix present in the trie. Root children fail to root. Failure states are shallower, so their rows are ready when needed. For each text, begin at root with fresh visit counts. Each character reaches the longest matching suffix-prefix. All shorter pattern endings at that position lie on its failure chain. Reverse BFS propagates visits once from each state to its failure parent, counting all such endings without enumerating individual matches. Reading terminal counts in original order preserves duplicate columns. A fresh row handles empty and repeated texts.

With total dictionary length L, p patterns, S states and T texts, building takes O(L+95S+p+1) time and O(95S+p+1) auxiliary storage. Scanning and propagation cost O(sum of text lengths + T(S+p+1)); output is O(T(p+1)+1). Counts use long. The maximum-duplicate fixture produces one million output cells from a trie with only eleven states.

## Evidence

The official setup, all 44 public cases and the repeated mixed batch passed locally. The student verifier writes complete actual answers for its 50 named-by-input fixtures in `tests/student/examples-results.jsonl`, aligned with `examples.jsonl`. The same seed (2515) regenerates 280 further small requests. All 330 cases run forwards and backwards in one process. See `tests/student/results.txt` for the recorded outcomes.

| Command | Independent expected result or witness check | Observed |
|---|---|---|
| lcs | Enumerate small subsequences; both a and b are optimal for ab/ba. The maximum 1000×1000 identical inputs have optimum 1000. | Pass |
| merge | Enumerate legal adjacent moves. Replay the returned plan. Four equal billion-size blocks cost eight billion. For 200 equal blocks, a minimum-depth-sum binary tree has 56 leaves at depth 7 and 144 at depth 8, and can preserve the original order. | Pass |
| tree | Exhaust all vertex subsets on small trees and check the returned set. A chain of 100,000 equal billion-weight vertices allows 50,000 selections, one per adjacent pair. | Pass |
| naive / kmp / rk | Check each start independently using Python startswith. Cover printable punctuation, case, empty boundaries, overlaps, dj/ab collisions and subtracting a larger outgoing term. Full-size KMP yields 100,001 starts; RK with n=10,000 and m=2,000 yields 8,001. | Pass |
| suffix | Sort literal suffixes only in the small oracle and measure each LCP directly. For 50,000 equal characters, SA descends, LCP is 0..49,999 and the repeat has length 49,999. | Pass |
| aho | Count each pattern independently in each text. A 100,000-character pattern in 200,000 equal characters matches 100,001 times. Ten thousand copies of ten a's in each 2,000-character text match 1,991 times per column. | Pass |

Witness checks also accept two distinct optima and reject an invalid answer for each of lcs, merge, tree and suffix. Large fixtures are generated by verify.py, avoiding large committed outputs. Tests verify the optimum and the witness using the supplied semantic validator or explicit full-array comparisons.

## Search measurements

The driver `tests/student/SearchTiming.java` uses a random text over a,b,c,d with java.util.Random(2515+n), or a repetitive all-a text. Each pattern is the text slice of length n/10 starting at floor(n/3). At each size, all three methods see the same pair and must match a startsWith-based reference before timing. Each method receives 20 untimed warm-up calls per case. Each of the three samples averages 20 complete method calls, retaining each result in a volatile field. The order of methods rotates between samples. System.nanoTime measures elapsed time; figures below are microseconds per call. Each call includes preprocessing, search and creation of the result. Generation, correctness checks, JSON transport, printing, JVM startup and compilation are outside the interval.

| Case | n/m | Method | Run 1 | Run 2 | Run 3 | Median | Starts |
|---|---:|---|---:|---:|---:|---:|---:|
| random | 300/30 | naive | 33.520 | 24.730 | 16.055 | 24.730 | 1 |
| random | 300/30 | kmp | 26.540 | 27.195 | 28.190 | 27.195 | 1 |
| random | 300/30 | rk | 25.570 | 41.025 | 22.725 | 25.570 | 1 |
| repetitive | 300/30 | naive | 121.805 | 45.395 | 25.515 | 45.395 | 271 |
| repetitive | 300/30 | kmp | 26.810 | 11.775 | 11.425 | 11.775 | 271 |
| repetitive | 300/30 | rk | 90.120 | 36.295 | 29.870 | 36.295 | 271 |
| random | 1200/120 | naive | 11.325 | 10.945 | 10.640 | 10.945 | 1 |
| random | 1200/120 | kmp | 23.685 | 24.035 | 23.850 | 23.850 | 1 |
| random | 1200/120 | rk | 17.675 | 20.300 | 17.200 | 17.675 | 1 |
| repetitive | 1200/120 | naive | 367.155 | 376.700 | 358.700 | 367.155 | 1081 |
| repetitive | 1200/120 | kmp | 64.755 | 57.900 | 11.745 | 57.900 | 1081 |
| repetitive | 1200/120 | rk | 441.515 | 398.945 | 367.055 | 398.945 | 1081 |
| random | 3000/300 | naive | 50.075 | 83.370 | 36.840 | 50.075 | 1 |
| random | 3000/300 | kmp | 7.945 | 17.295 | 7.790 | 7.945 | 1 |
| random | 3000/300 | rk | 88.790 | 106.735 | 81.305 | 88.790 | 1 |
| repetitive | 3000/300 | naive | 1192.175 | 1519.525 | 1239.245 | 1239.245 | 2701 |
| repetitive | 3000/300 | kmp | 24.845 | 24.670 | 26.760 | 24.845 | 2701 |
| repetitive | 3000/300 | rk | 3040.855 | 3239.340 | 3533.255 | 3239.340 | 2701 |

For the 3,000-character repetitive text, every one of the 2,701 windows matches. Both naive search and exact RK compare every character in every matching window. KMP reuses the 299-character border between matches. On random text, naive usually exits a comparison early, while RK still updates every fingerprint and sometimes verifies collisions. The deliberately small modulus makes collisions easy to test. These times describe this input family and JVM run. Warm-up, compilation inside the JIT, allocation and scheduling can affect the three samples.

## Sources and assistance

The [course template](https://github.com/RZhaniya/daa-assignment-4-archive), its contracts, lesson and practice examples supplied the interfaces, algorithm requirements and public checks. An AI assistant explained the requirements, implemented the Java methods, wrote the tests and analysis, and ran the checks and benchmark.
