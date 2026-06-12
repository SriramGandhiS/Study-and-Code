/*
 * LeetCode 51: N Queens
 * Difficulty: Hard
 *
 * Place n queens on an n x n board so no two queens attack each other.
 */

import java.util.*;

class Solution {
    public List<List<String>> solveNQueens(int n) {
        List<List<String>> res=new ArrayList<>();
        char[][] board=new char[n][n];
        for(char[] row:board) Arrays.fill(row,'.');
        solve(res,board,0,n,new HashSet<>(),new HashSet<>(),new HashSet<>());
        return res;
    }
    void solve(List<List<String>> res,char[][] board,int row,int n,Set<Integer> cols,Set<Integer> diag,Set<Integer> anti){
        if(row==n){List<String> sol=new ArrayList<>();for(char[] r:board)sol.add(new String(r));res.add(sol);return;}
        for(int col=0;col<n;col++){if(cols.contains(col)||diag.contains(row-col)||anti.contains(row+col)) continue;
            board[row][col]='Q';cols.add(col);diag.add(row-col);anti.add(row+col);
            solve(res,board,row+1,n,cols,diag,anti);
            board[row][col]='.';cols.remove(col);diag.remove(row-col);anti.remove(row+col);}
    }
}
