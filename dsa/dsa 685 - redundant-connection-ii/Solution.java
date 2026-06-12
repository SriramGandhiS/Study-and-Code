/*
 * LeetCode 685: Redundant Connection Ii
 * Difficulty: Hard
 *
 * Find the redundant directed connection to remove.
 */

class Solution {
    public int[] findRedundantDirectedConnection(int[][] edges) {
        int n=edges.length; int[] parent=new int[n+1],can1=null,can2=null;
        for(int[] e:edges){if(parent[e[1]]==0) parent[e[1]]=e[0]; else{can1=new int[]{parent[e[1]],e[1]};can2=e.clone();e[1]=0;}}
        int[] uf=new int[n+1]; for(int i=0;i<=n;i++) uf[i]=i;
        for(int[] e:edges){if(e[1]==0) continue; int u=find(uf,e[0]),v=find(uf,e[1]); if(u==v) return can1==null?e:can1; uf[u]=v;}
        return can2;
    }
    int find(int[]uf,int x){return uf[x]==x?x:(uf[x]=find(uf,uf[x]));}
}
