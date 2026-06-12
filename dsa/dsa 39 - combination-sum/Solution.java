/*
 * LeetCode 39: Combination Sum
 * Difficulty: Medium
 *
 * Find all combinations of candidates that sum to target (reuse allowed).
 */

import java.util.*;

class Solution {
    public List<List<Integer>> combinationSum(int[] candidates, int target) {
        List<List<Integer>> res = new ArrayList<>();
        Arrays.sort(candidates);
        backtrack(res, new ArrayList<>(), candidates, target, 0);
        return res;
    }
    void backtrack(List<List<Integer>> res, List<Integer> cur, int[] cands, int remain, int start) {
        if (remain == 0) { res.add(new ArrayList<>(cur)); return; }
        for (int i = start; i < cands.length && cands[i] <= remain; i++) {
            cur.add(cands[i]);
            backtrack(res, cur, cands, remain-cands[i], i);
            cur.remove(cur.size()-1);
        }
    }
}
