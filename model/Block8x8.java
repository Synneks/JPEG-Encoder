package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block8x8 {
    private int[][] matrix = new int[8][8];

    public Block8x8() {
    }

    public Block8x8(int [][] matrix) {
        this.matrix = matrix;
    }

    public void add(int element, int row, int column) {
        matrix[row][column] = element;
    }

    public Block4x4 subsample() {
        Block4x4 subsampled = new Block4x4();
        List<Integer> averages = getAverages();
//       System.out.println("averages = " + averages);
        int m = 0;
        //putting the averages in place
        for (int k = 0; k < 4; k++) {
            for (int l = 0; l < 4; l++) {
                subsampled.add(averages.get(m), k, l);
                m++;
            }
        }
        return subsampled;
    }

    private List<Integer> getAverages() {
        List<Integer> list = new ArrayList<>();
        int avg = 0;
        int k = 0;
        for (int i = 0; i < 4; i++) //traversing 4x4 blocks
            for (int j = 0; j < 4; j++)
                for (int l = 0; l < 2; l++) //traversing 2x2 blocks
                    for (int m = 0; m < 2; m++) {
                        avg += matrix[i * 2 + l][j * 2 + m];
                        k++;
                        if (k == 4) {   //after every values i make the avg
                            list.add(avg/4);
                            avg = 0;
                            k = 0;
                        }
                    }
        return list;
    }

    public int get(int i, int j) {
        return matrix[i % 8][j % 8];
    }

    public void subtract128() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                matrix[i][j] -= 128;
            }
        }
    }

    public void add128() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                matrix[i][j] += 128;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                output.append(matrix[i][j]).append(" ");
            }
            output.append("\n");
        }
        return output.toString();
    }
}
