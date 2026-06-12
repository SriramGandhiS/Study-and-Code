/*
 * LeetCode 765: Couples Holding Hands
 * Difficulty: Hard
 *
 * Find minimum swaps to seat all couples next to each other.
 */

class Solution {
    public int minSwapsCouples(int[] row) {
        int res=0,n=row.length; int[] pos=new int[n];
        for(int i=0;i<n;i++) pos[row[i]]=i;
        for(int i=0;i<n;i+=2){int p=row[i]%2==0?row[i]+1:row[i]-1; if(row[i+1]!=p){int j=pos[p]; pos[row[i+1]]=j; row[j]=row[i+1]; pos[p]=i+1; row[i+1]=p; res++;}}
        return res;
    }
}
