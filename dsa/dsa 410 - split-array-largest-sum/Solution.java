/*
 * LeetCode 410: Split Array Largest Sum
 * Difficulty: Hard
 *
 * Split an array into m parts minimizing the largest subarray sum.
 */

class Solution {
    public int splitArray(int[] nums, int m) {
        long lo=0,hi=0;
        for(int n:nums){lo=Math.max(lo,n);hi+=n;}
        while(lo<hi){long mid=(lo+hi)/2; if(canSplit(nums,m,mid)) hi=mid; else lo=mid+1;}
        return (int)lo;
    }
    boolean canSplit(int[] nums,int m,long limit){int parts=1; long cur=0; for(int n:nums){if(cur+n>limit){parts++;cur=0;} cur+=n;} return parts<=m;}
}
