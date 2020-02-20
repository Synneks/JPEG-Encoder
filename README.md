# JPEG-Encoder
🎞️ Small video encoder and decoder 

Part 1: 

The encoder part:
- reads the **PPM** image and converts each pixel value from RGB to **YUV**
- forms 3 matrixes: one for Y components, one for U components and one for V components
- divides the Y matrix into blocks of 8x8 values; for each block store: the 64 values/bytes from the block, the type of block (Y) and the position of the block in the image
- divides the U and V matrixes into blocks of 8x8 values; each block stores: 4x4=16 values/bytes from the block (i.e. perform 4:2:0 subsampling, that is for each 2x2 U/V values store only one U/V value which should be the average of those 2x2=4 values), the type of block (U or V) and the position of the block in the image
- stores the list of 8x8 Y blocks and 4x4 U and V blocks
  
The decoder part:
- starting from a list of 8x8 Y-values blocks and subsampled 4x4 U- and V-values blocks composes the final PPM image and displays it on a canvas
  
Part 2:
  
**Forward DCT** (Discrete Cosine Transform) takes as input an 8x8 Y/Cb/Cr values block and transforms this block  into another 8x8 DCT coefficient block.  
The "YCbCr Conversion, Block splitting & Subsampling" phase from *part 1*, produced 8x8 Y blocks and 4x4 Cb/Cr blocks. 
For the DCT, it transform the 4x4 Cb/Cr blocks back to 8x8 matrixes, so that a single Cb/Cr value is placed in 4 distinct places in the 8x8 matrix (i.e. the reverse of subsampling).  
Before applying the Forward DCT, it **substracts 128** from each value of every 8x8 Y/Cb/Cr block.  

The forward DCT is implementeded using the following formula:  
![equation](http://www.cs.ubbcluj.ro/~forest/pdav/FDCT.png)  

where gx,y is the Y/Cb/Cr value from coordinates "x" and "y" in the input 8x8 Y/Cb/Cr block (0 ≤ x ≤ 7, 0 ≤ y ≤ 7)  
and Gu,v is the DCT coefficient from coordinates "u" and "v" in the resulting 8x8 DCT block (0 ≤ u ≤ 7, 0 ≤ v ≤ 7).  
α(u) is 1/sqrt(2) if u=0 and 1 if u > 0.

**Quantization phase** takes as input an 8x8 block of DCT coefficient and **divides** this block to an 8x8 quantization matrix obtaining an 8x8 quantized coefficients block.  
Uses the following quantization matrix:
6   4   4   6   10  16  20  24  
5   5   6   8   10  23  24  22  
6   5   6   10  16  23  28  22  
6   7   9   12  20  35  32  25  
7   9   15  22  27  44  41  31  
10  14  22  26  32  42  45  37  
20  26  31  35  41  48  48  40  
29  37  38  39  45  40  41  40  
The division is performed **component-wise** (i.e. DCT[x][y] is divided to Q[x][y]) and it is integer division - keeps only the quotient, lose the remainder.

**DeQuantization phase** is the opposite of quantization; takes as input an 8x8 quantized block produced by the encoder and it multiplies this block (component-by-component) with the 8x8 quantization matrix outlined above.

**Inverse DCT** (Discrete Cosine Transform) is the opposite of Forward DCT used by the encoder; it takes a 8x8 DCT coefficients block and it produces an 8x8 Y/Cb/Cr block.  

The inverse DCT is implemented using the following formula:  
![equation2](http://www.cs.ubbcluj.ro/~forest/pdav/IDCT.png)  

where fx,y is the Y/Cb/Cr value from coordinates "x" and "y" in the resulting 8x8 Y/Cb/Cr block (0 ≤ x ≤ 7, 0 ≤ y ≤ 7)  
and Fu,v is the DCT coefficient from coordinates "u" and "v" in the input 8x8 DCT block (0 ≤ u ≤ 7, 0 ≤ v ≤ 7).  
α(u) is 1/sqrt(2) if u=0 and 1 if u > 0.  
After applying the Inverse DCT, it **adds 128** to each value of every 8x8 Y/Cb/Cr block obtained.
