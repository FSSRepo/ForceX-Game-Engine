package com.forcex.utils;

public class ImageProcessing
{
	public static void createImageAndAdd(String[][] paths,int width,int height,String out){
		Image out_img = new Image(new byte[width*height*4],width,height);
		int 
		offsetX = 0,
		offsetY = 0;
		int curX = 0;
		int curY = 0;
		for(byte yaxis = 0;yaxis < paths.length;yaxis++){
			for(byte xaxis = 0;xaxis < paths[yaxis].length;xaxis++){
				Image img = new Image(paths[yaxis][xaxis]);
				offsetX = curX;
				for(int x = 0;x < img.width;x++){
					for(int y = 0;y < img.height;y++){
						byte[] d = img.getRGBA(x,y);
						out_img.setRGBA(offsetX,offsetY,d[0] & 0xff,d[1] & 0xff,d[2] & 0xff,d[3] & 0xff);
						offsetY++;
					}
					offsetX++;
					offsetY = curY;
				}
				curX += 1536;
			}
			curX = 0;
			curY += 256;
		}
		out_img.save(out);
	}
	
	public static void mixImage(String src,String add,String out,int pixel_x,int pixel_y){
		Image img_src = new Image(src);
		Image img_add = new Image(add);
		if(img_src.width < img_add.width || img_src.height < img_add.height){
			Logger.log("Error: cannot mix image.");
			return;
		}
		int offsetX = pixel_x,offsetY = pixel_y;
		for(int x = 0;x < img_add.width;x++){
			for(int y = 0;y < img_add.height;y++){
				byte[] d = img_add.getRGBA(x,y);
				img_src.setRGBA(offsetX,offsetY,d[0] & 0xff,d[1] & 0xff,d[2] & 0xff,d[3] & 0xff);
				offsetY++;
			}
			offsetY = pixel_y;
			offsetX += img_add.width;
		}
		img_src.save(out);
	}
}
