package com.forcex.utils;

public class ImageProcessing {
    public static void mergeImages(String[][] paths, int width, int height, String out) {
        Image out_img = new Image(new byte[width * height * 4], width, height);
        int offsetX = 0, offsetY = 0;
        int curX = 0;
        int curY = 0;
        for (byte row = 0; row < paths.length; row++) {
            for (byte col = 0; col < paths[row].length; col++) {
                Image img = new Image(paths[row][col]);
                offsetX = curX;
                for (int x = 0; x < img.width; x++) {
                    for (int y = 0; y < img.height; y++) {
                        Color d = img.getRGBA(x, y);
                        out_img.setRGBA(offsetX, offsetY, d.r, d.g, d.b, d.a);
                        offsetY++;
                    }
                    offsetX++;
                    offsetY = curY;
                }
                img.clear();
                curX += img.width;
            }
            curX = 0;
            curY += 256;
        }
        out_img.save(out);
    }

    public static void mixImage(String src, String add, String out, int pixel_x, int pixel_y) {
        Image img_src = new Image(src);
        Image img_add = new Image(add);
        if (img_src.width < img_add.width || img_src.height < img_add.height) {
            Logger.log("Error: cannot mix image.");
            return;
        }
        int offsetX = pixel_x, offsetY = pixel_y;
        for (int x = 0; x < img_add.width; x++) {
            for (int y = 0; y < img_add.height; y++) {
                Color d = img_add.getRGBA(x, y);
                img_src.setRGBA(offsetX, offsetY, d.r, d.g, d.b, d.a);
                offsetY++;
            }
            offsetY = pixel_y;
            offsetX += img_add.width;
        }
        img_src.save(out);
    }
}
