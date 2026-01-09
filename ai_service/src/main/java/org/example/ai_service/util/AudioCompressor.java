package org.example.ai_service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Audio Compressor Utility
 * 
 * OpenAI Whisper 25 MB limitine sığdırmak için ses dosyalarını sıkıştırır.
 * 
 * Stratejiler:
 * 1. Sample rate düşürme (44100Hz → 16000Hz)
 * 2. Bit depth düşürme (16-bit → 8-bit if needed)
 * 3. Mono'ya çevirme (stereo → mono)
 */
@Slf4j
@Component
public class AudioCompressor {

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024;
    private static final float TARGET_SAMPLE_RATE = 8000f;
    private static final int TARGET_BIT_DEPTH = 16;

    /**
     * Dosya boyutunu kontrol et ve gerekirse sıkıştır
     * 
     * @param inputFile Orijinal audio dosyası
     * @return Sıkıştırılmış dosya (veya orijinal dosya eğer 25MB altındaysa)
     */
    public File compressIfNeeded(File inputFile) throws IOException {
        long fileSize = inputFile.length();
        
        log.info("Audio file size: {} MB", fileSize / (1024.0 * 1024.0));
        
        if (fileSize <= MAX_FILE_SIZE) {
            log.info("File size is within limit, no compression needed");
            return inputFile;
        }
        
        log.info("File exceeds 25 MB limit, compression required");
        
        try {
            return compressAudio(inputFile);
        } catch (UnsupportedAudioFileException e) {
            log.error("Unsupported audio format: {}", inputFile.getName(), e);
            throw new IOException("Cannot compress audio file: unsupported format", e);
        }
    }

    /**
     * Audio dosyasını sıkıştır
     */
    private File compressAudio(File inputFile) throws IOException, UnsupportedAudioFileException {
        log.info("Starting audio compression for: {}", inputFile.getName());
        
        AudioInputStream originalStream = AudioSystem.getAudioInputStream(inputFile);
        AudioFormat originalFormat = originalStream.getFormat();
        
        log.info("Original format: {} Hz, {} bit, {} channels",
                originalFormat.getSampleRate(),
                originalFormat.getSampleSizeInBits(),
                originalFormat.getChannels());
        
        AudioFormat targetFormat = new AudioFormat(
                TARGET_SAMPLE_RATE,
                TARGET_BIT_DEPTH,
                1,
                true,
                false
        );
        
        log.info("Target format: {} Hz, {} bit, {} channels",
                targetFormat.getSampleRate(),
                targetFormat.getSampleSizeInBits(),
                targetFormat.getChannels());
        
        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);
        
        String outputFileName = inputFile.getName().replaceFirst("[.][^.]+$", "") + "_compressed.wav";
        Path outputPath = Files.createTempFile("audio_compressed_", ".wav");
        File outputFile = outputPath.toFile();
        
        AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, outputFile);
        
        long compressedSize = outputFile.length();
        double compressionRatio = (1.0 - ((double) compressedSize / inputFile.length())) * 100;
        
        log.info("Compression completed: {} MB → {} MB ({:.1f}% reduction)",
                inputFile.length() / (1024.0 * 1024.0),
                compressedSize / (1024.0 * 1024.0),
                compressionRatio);
        
        if (compressedSize > MAX_FILE_SIZE) {
            log.warn("Compressed file still exceeds 25 MB! Consider splitting the file.");
        }
        
        originalStream.close();
        convertedStream.close();
        
        return outputFile;
    }

    /**
     * Dosya boyutunu hesapla (MB)
     */
    public double getFileSizeInMB(File file) {
        return file.length() / (1024.0 * 1024.0);
    }

    /**
     * Dosyanın sıkıştırmaya ihtiyacı var mı?
     */
    public boolean needsCompression(File file) {
        return file.length() > MAX_FILE_SIZE;
    }

    /**
     * Tahmini sıkıştırma oranı
     */
    public String estimateCompression(File file, AudioFormat format) {
        double currentSizeInMB = file.length() / (1024.0 * 1024.0);
        double sampleRateFactor = TARGET_SAMPLE_RATE / format.getSampleRate();
        double channelFactor = 1.0 / format.getChannels();
        double estimatedSizeInMB = currentSizeInMB * sampleRateFactor * channelFactor;
        
        return String.format("%.2f MB → %.2f MB (estimated)", currentSizeInMB, estimatedSizeInMB);
    }
}
