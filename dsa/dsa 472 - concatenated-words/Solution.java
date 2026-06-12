/*
 * LeetCode 472: Concatenated Words
 * Difficulty: Hard
 *
 * Find all words that can be formed by concatenating other words in the list.
 */

import java.util.*;

class Solution {
    public List<String> findAllConcatenatedWordsInADict(String[] words) {
        Set<String> dict=new HashSet<>(Arrays.asList(words)); List<String> res=new ArrayList<>();
        for(String w:words) if(!w.isEmpty()&&canForm(w,dict)) res.add(w);
        return res;
    }
    boolean canForm(String word,Set<String> dict){int n=word.length(); boolean[] dp=new boolean[n+1]; dp[0]=true;
        for(int i=1;i<=n;i++) for(int j=0;j<i;j++) if(dp[j]&&(j>0||i<n)&&dict.contains(word.substring(j,i))){dp[i]=true;break;}
        return dp[n];
    }
}
