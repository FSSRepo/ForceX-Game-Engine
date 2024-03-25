package com.forcex.io;

import com.forcex.core.AL;

public class WavFile {
    public byte[] data;
    public int sample_rate, format, channel, bitrate;
    public float time;
    float rate_timing = 0.2f;

    public WavFile(String path) {
        byte[] wav = FileUtils.readBinaryData(path);
        int offset = 0;
        if (readString(wav, offset, 4).equals("RIFF")) {
            offset += 8;
            if (readString(wav, offset, 4).equals("WAVE")) {
                offset += 8;
                if (readInt(wav, offset) != 16) {
                    throw new RuntimeException("WavFile: not PCM format, wave format unsupported :(");
                }
                offset += 4;
                if (readShort(wav, offset) != 1) {
                    throw new RuntimeException("WavFile: not PCM format :(");
                }
                offset += 2;
                channel = readShort(wav, offset);
                offset += 2;
                sample_rate = readInt(wav, offset);
                offset += 10;
                bitrate = readShort(wav, offset);
                while (true) {
                    if (wav[offset] == 'd' && wav[offset + 1] == 'a' && wav[offset + 2] == 't') {
                        break;
                    }
                    offset++;
                }
                offset += 4;
                int dataSize = readInt(wav, offset);
                data = new byte[dataSize];
                if (bitrate == 16) {
                    for (int i = offset, j = 0; i < dataSize; i += 2, j += 2) {
                        data[j] = wav[i];
                        data[j + 1] = wav[i + 1];
                    }
                } else {
                    for (int i = offset, j = 0; i < dataSize; i++, j++) {
                        data[j] = wav[i];
                    }
                }
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

    public int readShort(byte[] DATA, int fileoffset) {
        int i = (DATA[fileoffset] & 0xFF) | (DATA[fileoffset + 1] & 0xFF) << 8;
        return i;
    }

    public int readShortLE(byte[] data, int fileoffset) {
        int LSB = data[fileoffset];
        int MSB = data[fileoffset + 1];
        return (MSB << 8) | (0xFF & LSB);
    }

    public String readString(byte[] DATA, int offset, int size) {
        byte[] data = new byte[size];
		System.arraycopy(DATA, offset + 0, data, 0, size);
        return new String(data);
    }

    public int readInt(byte[] DATA, int fileoffset) {
        int i = (DATA[fileoffset] & 0xFF) |
                (DATA[fileoffset + 1] & 0xFF) << 8 |
                (DATA[fileoffset + 2] & 0xFF) << 16 |
                (DATA[fileoffset + 3] & 0xFF) << 24;
        return i;
    }

    public float getSampleLenght(int byteoffset) {
        int num_samples = (int) (sample_rate * rate_timing);
        if (byteoffset - num_samples * 2 < 0) {
            return 0.0f;
        }
        float lenght = 0;
        int active_samples = 1;
        for (int i = 0; i < num_samples; i++) {
            if (byteoffset + i * 2 > data.length) {
                break;
            }
            int sample = readShort(data, byteoffset - (num_samples * 2) + i * 2);
            if (sample > 2000) {
                active_samples++;
                lenght += sample;
            }
        }
        return (1 - (lenght / (active_samples * 65535.0f))) * 2f;
    }

    public float getSample(int offset) {
        int real_offset = offset + (sample_rate / 2 * (bitrate / 8));
        if (real_offset > data.length) {
            return 0;
        }
        short i = (short) ((data[real_offset] & 0xFF) | (data[real_offset + 1] & 0xFF) << 8);
        return (i / 32767.0f) * 0.5f + 0.5f;
    }

    public String getPlayTime(int offset) {
        float timer = (float) offset / (sample_rate * channel * bitrate / 8f);
        int t = (int) timer % 60;
        return ((int) timer / 60) + ":" + (t < 10 ? "0" : "") + t;
    }

    public float getPorcentPlayed(int offset) {
        return offset / data.length;
    }

    public int getOffsetTime(float time) {
        return (byte) (time * (sample_rate * channel * bitrate / 8f));
    }

    public String toTimeReal() {
        int t = (int) time % 60;
        return ((int) time / 60) + ":" + (t < 10 ? "0" : "") + t;
    }
}
