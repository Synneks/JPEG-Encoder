package model;

public class Block4x4 {
    private int[][] matrix = new int[4][4];

    public void add(int element, int row, int column) {
        matrix[row][column] = element;
    }

    public double get(int i, int j) {
        return matrix[i % 4][j % 4];
    }

    public Block8x8 convertTo8x8() {
        Block8x8 block = new Block8x8();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                block.add(matrix[i][j], i * 2, j * 2);
                block.add(matrix[i][j], i * 2, j * 2 + 1);
                block.add(matrix[i][j], i * 2 + 1, j * 2);
                block.add(matrix[i][j], i * 2 + 1, j * 2 + 1);
            }
        }
        return block;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                output.append(matrix[i][j]).append(" ");
            }
            output.append("\n");
        }
        return output.toString();
    }
}
