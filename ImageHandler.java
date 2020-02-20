import model.Block4x4;
import model.Block8x8;
import model.RGB;

import java.io.*;

public class ImageHandler {
    private String format;
    private int columns, rows, maxColorValue;
    private RGB[][] pixels;
    private int[][] y, u, v;
    private Block8x8[][] yBlocks8x8, uBlocks8x8, vBlocks8x8, dctyBlocks, dctuBlocks, dctvBlocks;
    private Block8x8 Q;
    private Block4x4[][] uBlocks4x4, vBlocks4x4;
    private File originalImage;

    public ImageHandler(String filePath) throws IOException {
        originalImage = new File(filePath);
        //Importing the image
        System.out.println("Started importing the image");
        importImage();
        //Converting the RGB values to YUV
        System.out.println("Converting RGB values to YUV");
        convertToYUV();
        //Dividing the Y values in 8x8 blocks
        System.out.println("Dividing the Y values into 8x8 blocks");
        yBlocks8x8 = divideY();
//        writeBlocks(yBlocks8x8);
        //Dividing the U values in 4x4 subsampled blocks
        System.out.println("Dividing the U values into 8x8 blocks");
        uBlocks4x4 = divideUV(u);
//        writeBlocks(uBlocks4x4, 'u');
        //Dividing the V values in 4x4 subsampled blocks
        System.out.println("Dividing the V values into 8x8 blocks");
        vBlocks4x4 = divideUV(v);
//        writeBlocks(vBlocks4x4, 'v');
        //Decoding the image
        System.out.println("Decoding image to yuv.ppm");
        writeImage();

        System.out.println("\nPart 2:");
        //Transforming the 4x4 U blocks into 8x8 blocks
        System.out.println("Converting 4x4 U blocks into 8x8 blocks");
        uBlocks8x8 = convertUVto8x8(uBlocks4x4);
        //Transforming the 4x4 V blocks into 8x8 blocks
        System.out.println("Converting 4x4 V blocks into 8x8 blocks");
        vBlocks8x8 = convertUVto8x8(vBlocks4x4);
        //Subtracting 128 from every Y/U/V Block
        System.out.println("Subtracting 128 from each value in the Y/U/V blocks");
        subtract128();
        //performing Forward DCT (Discrete Cosine Transform)
        System.out.println("Started forward DCT");
        forwardDCT();
        //Quantization on an 8x8 pixels block
        System.out.println("Started quantization phase");
        int[][] q = {
                {6, 4, 4, 6, 10, 16, 20, 24},
                {5, 5, 6, 8, 10, 23, 24, 22},
                {6, 5, 6, 10, 16, 23, 28, 22},
                {6, 7, 9, 12, 20, 35, 32, 25},
                {7, 9, 15, 22, 27, 44, 41, 31},
                {10, 14, 22, 26, 32, 42, 45, 37},
                {20, 26, 31, 35, 41, 48, 48, 40},
                {29, 37, 38, 39, 45, 40, 41, 40},
        };
        Q = new Block8x8(q);
        quantizationPhase();
        //performing the DeQuantization
        System.out.println("Started dequantization phase");
        dequantizationPhase();
        //Inverse DCT (Discrete Cosine Transform) on 8x8 pixels blocks
        System.out.println("Started inverse DCT");
        inverseDCT();
        //Adding 128 from every Y/U/V Block
        System.out.println("Adding 128 from each value in the Y/U/V blocks");
        add128();
        //Decoding the image
        System.out.println("Decoding the image");
        writeImageDCT();
    }

    private void importImage() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(originalImage));
        String st;

        //Reading format type
        st = br.readLine().trim();
        if (!st.equals("P6") && !st.equals("P3"))
            System.exit(1);
        else
            format = st;

        //Ignore comments
        st = br.readLine().trim();
        while (st.contains("#")) {
            st = br.readLine().trim();
        }

        //Reading number of columns and rows
        columns = Integer.parseInt(st.split(" ")[0]);
        rows = Integer.parseInt(st.split(" ")[1]);

        //Reading maximum Color value
        st = br.readLine().trim();
        maxColorValue = Integer.parseInt(st);

        System.out.println("format = " + format);
        System.out.println("columns = " + columns);
        System.out.println("rows = " + rows);
        System.out.println("maxColorValue = " + maxColorValue);

        //Reading
        int r, g, b;
        pixels = new RGB[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                r = Integer.parseInt(br.readLine().trim());
                g = Integer.parseInt(br.readLine().trim());
                b = Integer.parseInt(br.readLine().trim());
                pixels[i][j] = new RGB(r, g, b);
            }
        }
    }

    private void convertToYUV() {
        y = new int[rows][columns];
        u = new int[rows][columns];
        v = new int[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                y[i][j] = (int) (0.299 * pixels[i][j].getR() + 0.587 * pixels[i][j].getG() + 0.114 * pixels[i][j].getB());
                u[i][j] = (int) (-0.147 * pixels[i][j].getR() - 0.289 * pixels[i][j].getG() + 0.436 * pixels[i][j].getB());
                v[i][j] = (int) (0.615 * pixels[i][j].getR() - 0.515 * pixels[i][j].getG() - 0.1 * pixels[i][j].getB());
            }
        }
    }

    private Block8x8[][] divideY() {
        System.out.println("Dividing the Y values into 8x8 blocks");
        Block8x8[][] blocks = new Block8x8[rows / 8][columns / 8];
        Block8x8 block;

        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                block = createBlock(i, j, y);
                blocks[i][j] = block;
            }
        }
        return blocks;
    }

    private Block8x8 createBlock(int i, int j, int[][] yuv) {
        Block8x8 block;
        block = new Block8x8();
        for (int k = 0; k < 8; k++) {
            for (int p = 0; p < 8; p++) {
                block.add(yuv[i * 8 + k][j * 8 + p], k, p);
            }
        }
        return block;
    }

    private Block4x4[][] divideUV(int[][] uv) {
        Block4x4[][] blocks = new Block4x4[rows / 8][columns / 8];
        Block8x8 block;
        Block4x4 subsampled;

        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                block = createBlock(i, j, uv);
                subsampled = block.subsample();
                blocks[i][j] = subsampled;
            }
        }
        return blocks;
    }

    private void writeBlocks(Block4x4[][] blocks, char c) {
        StringBuilder output = new StringBuilder();
        PrintWriter writer = null;
        int blockNr = 0;

        //setting up the output
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                output.append("\nBlock nr: ").append(blockNr).append("\n");
                output.append(blocks[i][j].toString());
                blockNr++;
            }
        }

        try {
            //choosing file
            if (c == 'u')
                writer = new PrintWriter(new FileWriter("./src/resources/u.txt", false));
            else if (c == 'v')
                writer = new PrintWriter(new FileWriter("./src/resources/v.txt", false));

            assert writer != null;
            writer.print(output);
            writer.flush();
        } catch (IOException e) {
            System.out.println("File not found!");
        }
    }

    private void writeBlocks(Block8x8[][] blocks) throws IOException {
        StringBuilder output = new StringBuilder();
        PrintWriter writer = new PrintWriter(new FileWriter("./src/resources/y.txt", false));
        int blockNr = 0;

        //setting up the output
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                output.append("\nBlock nr: ").append(blockNr).append("\n");
                output.append(blocks[i][j].toString());
                blockNr++;
            }
        }

        writer.print(output);
        writer.flush();
    }

    private void writeImage() {
        StringBuilder output = new StringBuilder();
        int r, g, b;
        output.append(format).append("\n");
        output.append(columns).append(" ").append(rows).append("\n");
        output.append(maxColorValue).append("\n");

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                r = ((int) (yBlocks8x8[i / 8][j / 8].get(i, j) + (1.140 * vBlocks4x4[i / 8][j / 8].get(i, j))));
                g = ((int) (yBlocks8x8[i / 8][j / 8].get(i, j) - 0.395 * uBlocks4x4[i / 8][j / 8].get(i, j) - 0.581 * vBlocks4x4[i / 8][j / 8].get(i, j)));
                b = ((int) (yBlocks8x8[i / 8][j / 8].get(i, j) + 2.032 * uBlocks4x4[i / 8][j / 8].get(i, j)));

                //sorting out exceptions
                r = r < 0 ? 0 : (Math.min(r, 255));
                g = g < 0 ? 0 : (Math.min(g, 255));
                b = b < 0 ? 0 : (Math.min(b, 255));

                output.append(r).append("\n");
                output.append(g).append("\n");
                output.append(b).append("\n");
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter("./src/resources/yuv.ppm", false))) {
            writer.print(output);
            writer.flush();
        } catch (IOException e) {
            System.out.println("File not found!");
        }
    }

    private Block8x8[][] convertUVto8x8(Block4x4[][] uBlocks4x4) {
        Block8x8[][] blocks8x8 = new Block8x8[rows / 8][columns / 8];
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                blocks8x8[i][j] = uBlocks4x4[i][j].convertTo8x8();
            }
        }
        return blocks8x8;
    }

    private void subtract128() {
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                yBlocks8x8[i][j].subtract128();
                uBlocks8x8[i][j].subtract128();
                vBlocks8x8[i][j].subtract128();
            }
        }
    }

    private void forwardDCT() {
        dctyBlocks = new Block8x8[rows / 8][columns / 8];
        dctuBlocks = new Block8x8[rows / 8][columns / 8];
        dctvBlocks = new Block8x8[rows / 8][columns / 8];
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                dctyBlocks[i][j] = applyForwardDCTFormula(yBlocks8x8[i][j]);
                dctuBlocks[i][j] = applyForwardDCTFormula(uBlocks8x8[i][j]);
                dctvBlocks[i][j] = applyForwardDCTFormula(vBlocks8x8[i][j]);
            }
        }
    }

    private Block8x8 applyForwardDCTFormula(Block8x8 g) {
        Block8x8 G = new Block8x8();
        double d, sum;
        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                d = ((double) 1 / 4) * (u == 0 ? (1 / Math.sqrt(2)) : 1) * (v == 0 ? (1 / Math.sqrt(2)) : 1);
                sum = 0;
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        sum += g.get(x, y) *
                                Math.cos(((2 * x + 1) * u * Math.PI) / 16) *
                                Math.cos(((2 * y + 1) * v * Math.PI) / 16);
                    }
                }
                d *= sum;
                G.add((int) d, u, v);
            }
        }
        return G;
    }

    private void quantizationPhase() {
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                dctyBlocks[i][j] = divideBlock(dctyBlocks[i][j]);
                dctuBlocks[i][j] = divideBlock(dctuBlocks[i][j]);
                dctvBlocks[i][j] = divideBlock(dctvBlocks[i][j]);
            }
        }
    }

    private Block8x8 divideBlock(Block8x8 block8x8) {
        Block8x8 quantizated = new Block8x8();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                quantizated.add(block8x8.get(i, j) / Q.get(i, j), i, j);
            }
        }
        return quantizated;
    }

    private void dequantizationPhase() {
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                dctyBlocks[i][j] = multiplyBlock(dctyBlocks[i][j]);
                dctuBlocks[i][j] = multiplyBlock(dctuBlocks[i][j]);
                dctvBlocks[i][j] = multiplyBlock(dctvBlocks[i][j]);
            }
        }
    }

    private Block8x8 multiplyBlock(Block8x8 block8x8) {
        Block8x8 dequantizated = new Block8x8();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                dequantizated.add(block8x8.get(i, j) * Q.get(i, j), i, j);
            }
        }
        return dequantizated;
    }

    private void inverseDCT() {
        Block8x8[][] f = new Block8x8[rows / 8][columns / 8];
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                yBlocks8x8[i][j] = applyInverseDCTFormula(dctyBlocks[i][j]);
                uBlocks8x8[i][j] = applyInverseDCTFormula(dctuBlocks[i][j]);
                vBlocks8x8[i][j] = applyInverseDCTFormula(dctvBlocks[i][j]);
            }
        }
    }

    private Block8x8 applyInverseDCTFormula(Block8x8 F) {
        Block8x8 f = new Block8x8();
        double d, sum;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                d = ((double) 1 / 4);
                sum = 0;
                for (int u = 0; u < 8; u++) {
                    for (int v = 0; v < 8; v++) {
                        sum += (u == 0 ? 1 / Math.sqrt(2) : 1) *
                                (v == 0 ? 1 / Math.sqrt(2) : 1) *
                                F.get(u, v) *
                                Math.cos(((2 * x + 1) * u * Math.PI) / 16) *
                                Math.cos(((2 * y + 1) * v * Math.PI) / 16);
                    }
                }
                d *= sum;
                f.add((int) d, x, y);
            }
        }
        return f;
    }

    private void add128() {
        for (int i = 0; i < rows / 8; i++) {
            for (int j = 0; j < columns / 8; j++) {
                yBlocks8x8[i][j].add128();
                uBlocks8x8[i][j].add128();
                vBlocks8x8[i][j].add128();
            }
        }
    }

    private void writeImageDCT() {
        StringBuilder output = new StringBuilder();
        int r, g, b;
        output.append(format).append("\n");
        output.append(columns).append(" ").append(rows).append("\n");
        output.append(maxColorValue).append("\n");

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                r = ((int) (yBlocks8x8[i / 8][j / 8].get(i, j) + (1.140 * vBlocks8x8[i / 8][j / 8].get(i, j))));
                g = ((int) (yBlocks8x8[i / 8][j / 8].get(i, j) - 0.395 * uBlocks8x8[i / 8][j / 8].get(i, j) - 0.581 * vBlocks8x8[i / 8][j / 8].get(i, j)));
                b = ((int) (yBlocks8x8[i / 8][j / 8].get(i, j) + 2.032 * uBlocks8x8[i / 8][j / 8].get(i, j)));

                //sorting out exceptions
                r = r < 0 ? 0 : (Math.min(r, 255));
                g = g < 0 ? 0 : (Math.min(g, 255));
                b = b < 0 ? 0 : (Math.min(b, 255));

                output.append(r).append("\n");
                output.append(g).append("\n");
                output.append(b).append("\n");
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter("./src/resources/dct.ppm", false))) {
            writer.print(output);
            writer.flush();
        } catch (IOException e) {
            System.out.println("File not found!");
        }
    }
}
