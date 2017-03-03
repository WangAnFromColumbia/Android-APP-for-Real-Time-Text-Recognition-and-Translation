package com.dynamsoft.tessocr;

/**
 * Created by AnWang
 */
import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * 图像预处理类
 * @author Administrator
 */
public class BitmapFilter {

    private Bitmap bitmap;
    private int iw;
    private int ih;
    private int[] pixels;

    public BitmapFilter(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.iw = bitmap.getWidth();
        this.ih = bitmap.getHeight();
        pixels = new int[iw * ih];
        // 将bitmap转化为pixel数组
        bitmap.getPixels(pixels, 0, iw, 0, 0, iw, ih);
    }

    public Bitmap getBitmap() {
        // 将pixel数组转化为bitmap
        bitmap = Bitmap.createBitmap(pixels, iw, ih, Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    /**
     * 图像的二值化
     */
    public void changeGrey() {
        // 选取的阈值
        int grey = 120;
        int pixel;
        int alpha, red, green, blue;
        for (int i = 0; i < iw * ih; i++) {
            pixel = pixels[i];
            alpha = Color.alpha(pixel);
            red = Color.red(pixel);
            green = Color.green(pixel);
            blue = Color.blue(pixel);
            red = (red > grey) ? 255 : 0;
            green = (green > grey) ? 255 : 0;
            blue = (blue > grey) ? 255 : 0;
            pixels[i] = alpha << 24 | red << 16 | green << 8 | blue;
        }
    }

    /**
     * 图像的锐化
     */
    public void sharp() {
        // 像素中间变量
        int tempPixels[] = new int[iw * ih];
        for (int i = 0; i < iw * ih; i++) {
            tempPixels[i] = pixels[i];
        }
        int alpha;
        int red6, red5, red8, sharpRed;
        int green5, green6, green8, sharpGreen;
        int blue5, blue6, blue8, sharpBlue;
        for (int i = 1; i < ih - 1; i++) {
            for (int j = 1; j < iw - 1; j++) {
                alpha = Color.alpha(pixels[i * iw + j]);
                // 对图像进行尖锐化
                // 锐化red
                red6 = Color.red(pixels[i * iw + j + 1]);
                red5 = Color.red(pixels[i * iw + j]);
                red8 = Color.red(pixels[(i + 1) * iw + j]);
                sharpRed = Math.abs(red6 - red5) + Math.abs(red8 - red5);
                // 锐化green
                green5 = Color.green(pixels[i * iw + j]);
                green6 = Color.green(pixels[i * iw + j + 1]);
                green8 = Color.green(pixels[(i + 1) * iw + j]);
                sharpGreen = Math.abs(green6 - green5)
                        + Math.abs(green8 - green5);
                // 锐化blue
                blue5 = Color.blue(pixels[i * iw + j]);
                blue6 = Color.blue(pixels[i * iw + j + 1]);
                blue8 = Color.blue(pixels[(i + 1) * iw + j]);
                sharpBlue = Math.abs(blue6 - blue5)
                        + Math.abs(blue8 - blue5);
                // 处理颜色溢出
                if (sharpRed > 255)
                    sharpRed = 255;
                if (sharpGreen > 255)
                    sharpGreen = 255;
                if (sharpBlue > 255)
                    sharpBlue = 255;
                tempPixels[i * iw + j] = alpha << 24 | sharpRed << 16
                        | sharpGreen << 8 | sharpBlue;
            }
        }
        pixels = tempPixels;
    }

    /**
     * 中值滤波
     */
    public void median() {
        // 对图像进行中值滤波
        int alpha, red, green, blue;
        int red4, red5, red6;
        int green4, green5, green6;
        int blue4, blue5, blue6;
        for (int i = 1; i < ih - 1; i++) {
            for (int j = 1; j < iw - 1; j++) {
                // alpha值保持不变
                alpha = Color.alpha(pixels[i * iw + j]);
                // 处理red分量
                red4 = Color.red(pixels[i * iw + j - 1]);
                red5 = Color.red(pixels[i * iw + j]);
                red6 = Color.red(pixels[i * iw + j + 1]);
                // 水平方向进行中值滤波
                if (red4 >= red5) {
                    if (red5 >= red6) {
                        red = red5;
                    } else {
                        if (red4 >= red6) {
                            red = red6;
                        } else {
                            red = red4;
                        }
                    }
                } else {
                    if (red4 > red6) {
                        red = red4;
                    } else {
                        if (red5 > red6) {
                            red = red6;
                        } else {
                            red = red5;
                        }
                    }
                }
                // 处理green分量
                green4 = Color.green(pixels[i * iw + j - 1]);
                green5 = Color.green(pixels[i * iw + j]);
                green6 = Color.green(pixels[i * iw + j + 1]);
                // 水平方向进行中值滤波
                if (green4 >= green5) {
                    if (green5 >= green6) {
                        green = green5;
                    } else {
                        if (green4 >= green6) {
                            green = green6;
                        } else {
                            green = green4;
                        }
                    }
                } else {
                    if (green4 > green6) {
                        green = green4;
                    } else {
                        if (green5 > green6) {
                            green = green6;
                        } else {
                            green = green5;
                        }
                    }
                }
                // 处理blue分量
                blue4 = Color.blue(pixels[i * iw + j - 1]);
                blue5 = Color.blue(pixels[i * iw + j]);
                blue6 = Color.blue(pixels[i * iw + j + 1]);
                // 水平方向进行中值滤波
                if (blue4 >= blue5) {
                    if (blue5 >= blue6) {
                        blue = blue5;
                    } else {
                        if (blue4 >= blue6) {
                            blue = blue6;
                        } else {
                            blue = blue4;
                        }
                    }
                } else {
                    if (blue4 > blue6) {
                        blue = blue4;
                    } else {
                        if (blue5 > blue6) {
                            blue = blue6;
                        } else {
                            blue = blue5;
                        }
                    }
                }
                pixels[i * iw + j] = alpha << 24 | red << 16 | green << 8
                        | blue;
            }
        }
    }

    /**
     * 图像的平滑
     */
    public void smooth() {
        int[] tempPixels = new int[iw * ih];
        //图像的平滑
        int min = -1000;
        int max = 1000;
        for (int i = 0; i < ih; i++) {
            for (int j = 0; j < iw; j++) {
                if (i == 0 || i == 1 || i == ih - 1 || i == ih - 2 || j == 0
                        || j == 1 || j == iw - 1 || j == iw - 2) {
                    tempPixels[i * iw + j] = pixels[i * iw + j];
                } else {
                    // 中心的九个像素点
                    float average = (
                            pixels[i * iw + j]
                                    + pixels[i * iw + j - 1]
                                    + pixels[i * iw + j + 1]
                                    + pixels[(i - 1) * iw + j]
                                    + pixels[(i - 1) * iw + j - 1]
                                    + pixels[(i - 1) * iw + j + 1]
                                    + pixels[(i + 1)]
                                    + pixels[(i + 1) * iw + j]
                                    + pixels[(i + 1) * iw + j - 1]
                    ) / 9;
                    tempPixels[i * iw + j] = (int) (average);
                }
                if (tempPixels[i * iw + j] < min)
                    min = tempPixels[i * iw + j];
                if (tempPixels[i * iw + j] > max)
                    max = tempPixels[i * iw + j];
            }
        }
        for (int i = 0; i < iw * ih; i++) {
            tempPixels[i] = (tempPixels[i] - min) * 255 / (max - min);
        }
        pixels = tempPixels;
    }

}
