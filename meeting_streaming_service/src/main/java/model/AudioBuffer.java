package model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.ByteArrayOutputStream;

@Data
@NoArgsConstructor
public class AudioBuffer {
    private String userId;
    private String userName;

    private ByteArrayOutputStream audioStream = new ByteArrayOutputStream();

    public void addAudioChunk(byte[] chunk){
        if(chunk != null && chunk.length > 0){
            try{
                audioStream.write(chunk);
            }catch(Exception e){
                throw new RuntimeException("Failed to write audio chunk", e);
            }
        }
    }

    public byte[] getCombinedAudio(){
        return audioStream.toByteArray();
    }

    public int getBufferSize(){
        return audioStream.size();
    }

    public void clear(){
        audioStream.reset();
    }
}
