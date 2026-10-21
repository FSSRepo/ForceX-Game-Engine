#include <jni.h>
#include <string.h>
#include <stdlib.h>
#define STB_TRUETYPE_IMPLEMENTATION
#include "stb/stb_truetype.h"

extern "C"
{
    // Bakes a range of codepoints into a 1-channel alpha atlas.
    // outGlyphs: 9 floats per glyph -> ax0, ay0, ax1, ay1 (atlas pixel coords),
    //            xoff, yoff (pixel offset from pen), xadvance, xoff2, yoff2.
    // outMetrics: ascent, descent, lineGap (pixels).
    // Returns the atlas as a byte array of atlasW * atlasH (alpha coverage).
    JNIEXPORT jbyteArray JNICALL Java_com_forcex_core_CoreJni_fontBake(
        JNIEnv* env, jobject thiz,
        jbyteArray ttf, jfloat pixelHeight, jint firstChar, jint numChars,
        jint atlasW, jint atlasH, jfloatArray outGlyphs, jfloatArray outMetrics)
    {
        jsize ttfLen = env->GetArrayLength(ttf);
        unsigned char* ttfBuf = (unsigned char*)malloc(ttfLen);
        {
            unsigned char* src = (unsigned char*)env->GetPrimitiveArrayCritical(ttf, 0);
            memcpy(ttfBuf, src, ttfLen);
            env->ReleasePrimitiveArrayCritical(ttf, src, 0);
        }

        stbtt_fontinfo font;
        if (!stbtt_InitFont(&font, ttfBuf, stbtt_GetFontOffsetForIndex(ttfBuf, 0))) {
            free(ttfBuf);
            return NULL;
        }

        unsigned char* atlas = (unsigned char*)malloc(atlasW * atlasH);
        memset(atlas, 0, atlasW * atlasH);

        stbtt_pack_context spc;
        if (!stbtt_PackBegin(&spc, atlas, atlasW, atlasH, 0, 1, NULL)) {
            free(atlas);
            free(ttfBuf);
            return NULL;
        }

        stbtt_packedchar* ranges = (stbtt_packedchar*)calloc(numChars, sizeof(stbtt_packedchar));
        if (!stbtt_PackFontRange(&spc, ttfBuf, 0, pixelHeight, firstChar, numChars, ranges)) {
            stbtt_PackEnd(&spc);
            free(ranges);
            free(atlas);
            free(ttfBuf);
            return NULL;
        }
        stbtt_PackEnd(&spc);

        float* glyphs = (float*)env->GetPrimitiveArrayCritical(outGlyphs, 0);
        for (int i = 0; i < numChars; i++) {
            float* g = glyphs + i * 9;
            g[0] = ranges[i].x0;    // atlas pixel coords
            g[1] = ranges[i].y0;
            g[2] = ranges[i].x1;
            g[3] = ranges[i].y1;
            g[4] = ranges[i].xoff;  // pixel offset from pen origin
            g[5] = ranges[i].yoff;
            g[6] = ranges[i].xadvance;
            g[7] = ranges[i].xoff2;
            g[8] = ranges[i].yoff2;
        }
        env->ReleasePrimitiveArrayCritical(outGlyphs, glyphs, 0);

        float* metrics = (float*)env->GetPrimitiveArrayCritical(outMetrics, 0);
        float scale = stbtt_ScaleForMappingEmToPixels(&font, pixelHeight);
        int ascent, descent, lineGap;
        stbtt_GetFontVMetrics(&font, &ascent, &descent, &lineGap);
        metrics[0] = ascent * scale;
        metrics[1] = descent * scale;
        metrics[2] = lineGap * scale;
        env->ReleasePrimitiveArrayCritical(outMetrics, metrics, 0);

        jbyteArray outAtlas = env->NewByteArray(atlasW * atlasH);
        unsigned char* dst = (unsigned char*)env->GetPrimitiveArrayCritical(outAtlas, 0);
        memcpy(dst, atlas, atlasW * atlasH);
        env->ReleasePrimitiveArrayCritical(outAtlas, dst, 0);

        free(ranges);
        free(atlas);
        free(ttfBuf);
        return outAtlas;
    }

    // Returns the kerning advance in pixels for a codepoint pair.
    JNIEXPORT jfloat JNICALL Java_com_forcex_core_CoreJni_fontKerning(
        JNIEnv* env, jobject thiz,
        jbyteArray ttf, jint cp1, jint cp2, jfloat pixelHeight)
    {
        jsize ttfLen = env->GetArrayLength(ttf);
        unsigned char* ttfBuf = (unsigned char*)malloc(ttfLen);
        {
            unsigned char* src = (unsigned char*)env->GetPrimitiveArrayCritical(ttf, 0);
            memcpy(ttfBuf, src, ttfLen);
            env->ReleasePrimitiveArrayCritical(ttf, src, 0);
        }

        stbtt_fontinfo font;
        jfloat result = 0.0f;
        if (stbtt_InitFont(&font, ttfBuf, stbtt_GetFontOffsetForIndex(ttfBuf, 0))) {
            float scale = stbtt_ScaleForMappingEmToPixels(&font, pixelHeight);
            result = stbtt_GetCodepointKernAdvance(&font, cp1, cp2) * scale;
        }
        free(ttfBuf);
        return result;
    }
}
