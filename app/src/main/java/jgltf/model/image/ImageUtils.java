/*
 * www.javagl.de - JglTF
 *
 * Copyright 2015-2016 Marco Hutter - http://www.javagl.de
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package jgltf.model.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.logging.Logger;

/**
 * Utility methods related to images.<br>
 * <br>
 * This class should not be considered to be part of the public API.
 */
public class ImageUtils {
    /**
     * The logger used in this class
     */
    private static final Logger logger =
            Logger.getLogger(ImageUtils.class.getName());

    /**
     * Private constructor to prevent instantiation
     */
    private ImageUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns the contents of the given buffer as a <code>BufferedImage</code>.
     * Returns <code>null</code> if the given buffer is <code>null</code>.
     * If the data can not be converted into a buffered image, then an error
     * message is printed and <code>null</code> is returned.
     *
     * @param byteBuffer The byte buffer
     * @return The buffered image
     */
    static Bitmap readAsBufferedImage(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        byte[] buffer = new byte[byteBuffer.remaining()];
        byteBuffer.get(buffer);
        return BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
    }

    /**
     * Returns a direct byte buffer that contains the ARGB pixel values of
     * the given image. <br>
     * <br>
     * The given image might become unmanaged/untrackable by this operation.
     *
     * @param inputImage     The input image
     * @param flipVertically Whether the contents of the image should be
     *                       flipped vertically. This is always a hassle.
     * @return The byte buffer containing the ARGB pixel values
     */
    static ByteBuffer getImagePixelsARGB(Bitmap inputImage, boolean flipVertically) {
        Bitmap image = inputImage;
        if (flipVertically) {
            Matrix matrix = new Matrix();
            matrix.preScale(1.0f, -1.0f);
            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        }
        if (image.getConfig() != Bitmap.Config.ARGB_8888) {
            image = image.copy(Bitmap.Config.ARGB_8888, false);
        }
        int[] imageBuffer = new int[image.getWidth() * image.getHeight()];
        image.getPixels(imageBuffer, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        ByteBuffer outputByteBuffer = ByteBuffer.allocateDirect(imageBuffer.length * Integer.BYTES);
        outputByteBuffer.asIntBuffer().put(imageBuffer);
        return outputByteBuffer;
    }

    /**
     * Interpret the given byte buffer as ARGB pixels, and convert it into
     * a direct byte buffer containing the corresponding RGBA pixels
     *
     * @param pixels The input pixels
     * @return The output pixels
     */
    static ByteBuffer swizzleARGBtoRGBA(ByteBuffer pixels) {
        return swizzle(pixels, 16, 8, 0, 24);
    }

    /**
     * Interpret the given byte buffer as RGBA pixels, and convert it into
     * a direct byte buffer containing the corresponding ARGB pixels
     *
     * @param pixels The input pixels
     * @return The output pixels
     */
    static ByteBuffer swizzleRGBAtoARGB(ByteBuffer pixels) {
        return swizzle(pixels, 0, 24, 16, 8);
    }

    /**
     * Interpret the given byte buffer as pixels, swizzle the bytes
     * of these pixels according to the given shifts, and return a
     * a direct byte buffer containing the corresponding new pixels
     *
     * @param pixels The input pixels
     * @param s0     The right-shift for byte 0
     * @param s1     The right-shift for byte 1
     * @param s2     The right-shift for byte 2
     * @param s3     The right-shift for byte 3
     * @return The output pixels
     */
    private static ByteBuffer swizzle(ByteBuffer pixels,
                                      int s0, int s1, int s2, int s3) {
        IntBuffer iBuffer = pixels.asIntBuffer();
        ByteBuffer oByteBuffer = ByteBuffer
                .allocateDirect(iBuffer.capacity() * Integer.BYTES)
                .order(pixels.order());
        IntBuffer oBuffer = oByteBuffer.asIntBuffer();
        for (int i = 0; i < iBuffer.capacity(); i++) {
            int input = iBuffer.get(i);
            int output = swizzle(input, s0, s1, s2, s3);
            oBuffer.put(i, output);
        }
        return oByteBuffer;
    }

    /**
     * Convert the given image into a buffered image with the type
     * <code>TYPE_INT_ARGB</code>.
     *
     * @param image The input image
     * @return The converted image
     */
//    static ByteBuffer getImagePixelsARGB(Bitmap inputImage, boolean flipVertically) {
//        Bitmap image = inputImage;
//        if (flipVertically) {
//            Matrix matrix = new Matrix();
//            matrix.preScale(1.0f, -1.0f);
//            image = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
//        }
//        if (image.getConfig() != Bitmap.Config.ARGB_8888) {
//            image = image.copy(Bitmap.Config.ARGB_8888, false);
//        }
//        int[] imageBuffer = new int[image.getWidth() * image.getHeight()];
//        image.getPixels(imageBuffer, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
//        ByteBuffer outputByteBuffer = ByteBuffer.allocateDirect(imageBuffer.length * Integer.BYTES);
//        outputByteBuffer.asIntBuffer().put(imageBuffer);
//        return outputByteBuffer;
//    }

    /**
     * Swizzle the bytes of the given input according to the given shifts
     *
     * @param input The input
     * @param s0    The right-shift for byte 0
     * @param s1    The right-shift for byte 1
     * @param s2    The right-shift for byte 2
     * @param s3    The right-shift for byte 3
     * @return The output
     */
    private static int swizzle(int input, int s0, int s1, int s2, int s3) {
        int b0 = (input >> s0) & 0xFF;
        int b1 = (input >> s1) & 0xFF;
        int b2 = (input >> s2) & 0xFF;
        int b3 = (input >> s3) & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    /**
     * Create a vertically flipped version of the given image.
     *
     * @param image The input image
     * @return The flipped image
     */
    private static Bitmap flipVertically(Bitmap image) {
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    /**
     * Returns the data buffer of the given image as an IntBuffer. The given
     * image will become unmanaged/untrackable by this call.
     *
     * @param image The image
     * @return The data from the image as an IntBuffer
     * @throws IllegalArgumentException If the given image is not
     *                                  backed by a DataBufferInt
     */
    private static IntBuffer getBuffer(Bitmap image) {
        int[] data = new int[image.getWidth() * image.getHeight()];
        image.getPixels(data, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        return IntBuffer.wrap(data);
    }

    /**
     * Create a buffered image from the given {@link PixelData}
     *
     * @param pixelData The {@link PixelData}
     * @return The buffered image
     */
    public static Bitmap createBufferedImage(PixelData pixelData) {
        int w = pixelData.getWidth();
        int h = pixelData.getHeight();
        Bitmap image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] imageBuffer = new int[w * h];
        ByteBuffer pixels = pixelData.getPixelsRGBA();
        ByteBuffer argbBytes = swizzleRGBAtoARGB(pixels);
        argbBytes.asIntBuffer().get(imageBuffer);
        image.setPixels(imageBuffer, 0, w, 0, 0, w, h);
        return image;
    }

    /**
     * Creates the byte buffer containing the image data for the given
     * pixel data, with the given MIME type.<br>
     * <br>
     * The MIME type must be <code>"image/png"</code> or
     * <code>"image/gif"</code> or <code>"image/jpeg"</code> (<b>not</b>
     * <code>"image/jpg"</code>!).<br>
     * <br>
     * If the image data cannot be written, then an error message is
     * printed and <code>null</code> is returned.
     */
    public static ByteBuffer createImageDataBuffer(Bitmap image, String mimeType) {
        Bitmap.CompressFormat format = null;
        if ("image/gif".equals(mimeType)) {
            format = Bitmap.CompressFormat.WEBP;
        } else if ("image/jpeg".equals(mimeType)) {
            format = Bitmap.CompressFormat.JPEG;
        } else if ("image/png".equals(mimeType)) {
            format = Bitmap.CompressFormat.PNG;
        } else {
            throw new IllegalArgumentException("The MIME type string must be \"image/webp\", "
                    + "\"image/jpeg\" or \"image/png\", but is " + mimeType);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        image.compress(format, 100, outputStream);
        return ByteBuffer.wrap(outputStream.toByteArray());
    }

    // Only a basic test for the swizzling
    @SuppressWarnings("javadoc")
    public static void main(String[] args) {
        int input = 0x11223344;
        ByteBuffer b0 = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        b0.asIntBuffer().put(input);
        ByteBuffer b1 = swizzleARGBtoRGBA(b0);
        int rgba = b1.asIntBuffer().get();
        ByteBuffer b2 = swizzleRGBAtoARGB(b1);
        int argb = b2.asIntBuffer().get();

        System.out.println("Input: " + Integer.toHexString(input));
        System.out.println("RGBA : " + Integer.toHexString(rgba));
        System.out.println("ARGB : " + Integer.toHexString(argb));
    }

}
