/*
 * LeetCode 85: Maximal Rectangle
 * Difficulty: Hard
 *
 * Find the maximal rectangle containing only 1s in a binary matrix.
 */

import java.util.*;

class Solution {
    public int maximalRectangle(char[][] matrix) {
        if(matrix.length==0) return 0;
        int n=matrix[0].length, max=0; int[] heights=new int[n];
        for(char[] row:matrix){for(int j=0;j<n;j++) heights[j]=row[j]=='1'?heights[j]+1:0; max=Math.max(max,largestRect(heights));}
        return max;
    }
    int largestRect(int[] h){Stack<Integer> st=new Stack<>(); int max=0; for(int i=0;i<=h.length;i++){int cur=i==h.length?0:h[i]; while(!st.isEmpty()&&cur<h[st.peek()]){int height=h[st.pop()]; int width=st.isEmpty()?i:i-st.peek()-1; max=Math.max(max,height*width);} st.push(i);} return max;}
}
