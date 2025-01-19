package com.forcex.io;

import com.forcex.FX;
import com.forcex.core.AL;

public class WavFile {
    public byte[] data;
    public int sample_rate, format, channel, bitrate;
    public float time;

//    private final float  rate_timing = 0.2f;

    public WavFile(String path) {
        BinaryStreamReader wav = FX.fs.open(path, FileSystem.ReaderType.MEMORY);
        if (wav.readString(4).equals("RIFF")) {
            wav.skip(4);
            if (wav.readString(4).equals("WAVE")) {
                wav.skip(4);
                if (wav.readInt() != 16) {
                    throw new RuntimeException("WavFile: not PCM format, wave format unsupported :(");
                }
                if (wav.readShort() != 1) {
                    throw new RuntimeException("WavFile: not PCM format :(");
                }
                channel = wav.readShort();
                sample_rate = wav.readInt();
                wav.skip(6);
                bitrate = wav.readShort();
                wav.find(new char[] { 'd', 'a', 't'});
                wav.skip(4);
                int dataSize = wav.readInt();
                data = wav.readByteArray(dataSize);
            }
        }
        time = (float) data.length / (sample_rate * channel * bitrate / 8f);
        if (bitrate == 16) {
            if (channel == 2) {
                format = AL.AL_FORMAT_STEREO16;
            } else {
                format = AL.AL_FORMAT_MONO16;
            }
        } else {
            if (channel == 2) {
                format = AL.AL_FORMAT_STEREO8;
            } else {
                format = AL.AL_FORMAT_MONO8;
            }
        }
    }

//    public float getSampleLenght(int byteoffset) {
//        int num_samples = (int) (sample_rate * rate_timing);
//        if (byteoffset - num_samples * 2 < 0) {
//            return 0.0f;
//        }
//        float length = 0;
//        int active_samples = 1;
//        for (int i = 0; i < num_samples; i++) {
//            if (byteoffset + i * 2 > data.length) {
//                break;
//            }
//            int sample = readShort(data, byteoffset - (num_samples * 2) + i * 2);
//            if (sample > 2000) {
//                active_samples++;
//                length += sample;
//            }
//        }
//        return (1 - (length / (active_samples * 65535.0f))) * 2f;
//    }

//    public float getSample(int offset) {
//        int real_offset = offset + (sample_rate / 2 * (bitrate / 8));
//        if (real_offset > data.length) {
//            return 0;
//        }
//        short i = (short) ((data[real_offset] & 0xFF) | (data[real_offset + 1] & 0xFF) << 8);
//        return (i / 32767.0f) * 0.5f + 0.5f;
//    }
//
//    public String getPlayTime(int offset) {
//        float timer = (float) offset / (sample_rate * channel * bitrate / 8f);
//        int t = (int) timer % 60;
//        return ((int) timer / 60) + ":" + (t < 10 ? "0" : "") + t;
//    }

//    public float getPorcentPlayed(int offset) {
//        return offset / data.length;
//    }
//
//    public int getOffsetTime(float time) {
//        return (byte) (time * (sample_rate * channel * bitrate / 8f));
//    }
//
//    public String toTimeReal() {
//        int t = (int) time % 60;
//        return ((int) time / 60) + ":" + (t < 10 ? "0" : "") + t;
//    }
}
