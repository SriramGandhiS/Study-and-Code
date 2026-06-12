/*
 * LeetCode 1: Two Sum
 * Difficulty: Easy
 *
 * Description:
 * Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.
 * 
 * You may assume that each input would have exactly one solution, and you may not use the same element twice.
 */

import java.util.HashMap;
import java.util.Map;

class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> prevMap = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int diff = target - nums[i];
            if (prevMap.containsKey(diff)) {
                return new int[] { prevMap.get(diff), i };
            }
            prevMap.put(nums[i], i);
        }
        return new int[] {};
    }
}
