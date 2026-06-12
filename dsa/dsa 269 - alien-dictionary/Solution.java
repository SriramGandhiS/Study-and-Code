/*
 * LeetCode 269: Alien Dictionary
 * Difficulty: Hard
 *
 * Derive character ordering from a sorted alien dictionary.
 */

import java.util.*;

class Solution {
    public String alienOrder(String[] words) {
        Map<Character,Set<Character>> adj=new HashMap<>(); Map<Character,Integer> indegree=new HashMap<>();
        for(String w:words) for(char c:w.toCharArray()){adj.putIfAbsent(c,new HashSet<>());indegree.putIfAbsent(c,0);}
        for(int i=0;i<words.length-1;i++){String a=words[i],b=words[i+1]; if(a.length()>b.length()&&a.startsWith(b)) return "";
            for(int j=0;j<Math.min(a.length(),b.length());j++) if(a.charAt(j)!=b.charAt(j)){if(!adj.get(a.charAt(j)).contains(b.charAt(j))){adj.get(a.charAt(j)).add(b.charAt(j));indegree.merge(b.charAt(j),1,Integer::sum);} break;}}
        Queue<Character> q=new LinkedList<>(); for(char c:indegree.keySet()) if(indegree.get(c)==0) q.offer(c);
        StringBuilder sb=new StringBuilder();
        while(!q.isEmpty()){char c=q.poll();sb.append(c); for(char nb:adj.get(c)){indegree.merge(nb,-1,Integer::sum); if(indegree.get(nb)==0) q.offer(nb);}}
        return sb.length()==indegree.size()?sb.toString():"";
    }
}
