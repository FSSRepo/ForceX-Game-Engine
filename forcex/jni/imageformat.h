#ifndef IMAGEFORMAT_H
#define IMAGEFORMAT_H

#include "rg_etc1.h"

typedef unsigned char uint8;
typedef unsigned short uint16;
typedef unsigned int uint32;

inline int clamp16(int x) { if (x < 0) return 0; if (x > 15) return 15; return x; }
inline int clamp32(int x) { if (x < 0) return 0; if (x > 31) return 31; return x; }
inline int clamp64(int x) { if (x < 0) return 0; if (x > 63) return 63; return x; }

void etc1compress(uint8* input,int width,int height,int etcq,uint8* output){
	rg_etc1::pack_etc1_block_init();
	rg_etc1::etc1_pack_params params;
	params.m_dithering = false;
	switch(etcq){
		case 0:
			params.m_quality = rg_etc1::cLowQuality;
			break;
		case 1:
			params.m_quality = rg_etc1::cMediumQuality;
			break;
		case 2:
			params.m_quality = rg_etc1::cHighQuality;
			break;
	}
	// loop over blocks
	uint32* inp = reinterpret_cast<uint32*>(input);
	for(int y = 0; y < height; y += 4) {
		for(int x = 0; x < width; x += 4) {
			uint32 pixels[16];
			memcpy(pixels, inp + y*width + x, 16);
      		memcpy(pixels + 4, inp + (y+1)*width + x, 16);
        	memcpy(pixels + 8, inp + (y+2)*width + x, 16);
        	memcpy(pixels + 12, inp + (y+3)*width + x, 16);
			rg_etc1::pack_etc1_block(output, pixels, params);
        	output += 8;
		}
	}
}

void convertFormat(uint8* input,int width,int height,bool is565,uint8* output){
	int dith[16] = {
			1, 9, 3, 11,
			13, 5, 15, 7,
			4, 12, 2, 10,
			16, 8, 14, 6
		};
	uint16* dst = reinterpret_cast<uint16*>(output);
	int i = 0;
	for (int y = 0; y < height; y++) {
		for (int x = 0; x < width; x++) {
			int dithval = dith[(x & 0x3)+((y & 0x3)<<2)] - 8;
			if(is565){
				//dithval = 0; please check this
				int r = clamp32((input[i] + dithval/2) >> 3);
				int g = clamp64((input[i + 1] + dithval/4) >> 2);
				int b = clamp32((input[i + 2] + dithval/2) >> 3);
				*dst++ = (r << 11) | (g << 5) | (b);
				i += 4;
			}else{
				int r = clamp16((input[i] + dithval) >> 4);
				int g = clamp16((input[i + 1] + dithval) >> 4);
				int b = clamp16((input[i + 2] + dithval) >> 4);
				int a = clamp16((input[i + 3] + dithval) >> 4);	// really dither alpha?
				*dst++ = (r << 12) | (g << 8) | (b << 4) | (a << 0);
				i += 4;
			}
		}
	}
}
#endif
