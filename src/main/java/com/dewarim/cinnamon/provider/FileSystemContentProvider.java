package com.dewarim.cinnamon.provider;

import com.dewarim.cinnamon.application.CinnamonServer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import com.dewarim.cinnamon.model.ContentMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileSystemContentProvider {

    private static final Logger log = LogManager.getLogger(FileSystemContentProvider.class);


    static final String SEP = File.separator;

    private String dataRootPath;

    public FileSystemContentProvider() {
        dataRootPath = CinnamonServer.config.getServerConfig().getDataRoot();
    }



    public InputStream getContentStream(ContentMeta metadata) throws IOException {
        String targetName    = generateTargetName(metadata.getId());
        String subfolderName = getSubFolderName(targetName);
        String subfolderPath = dataRootPath + SEP + subfolderName;
        File   target        = new File(subfolderPath, metadata.getId().toString());
        return new FileInputStream(target);
    }

    public Optional<ContentMeta> getContentMeta(long id) {
        String targetName    = generateTargetName(id);
        String subfolderName = getSubFolderName(targetName);
        String subfolderPath = dataRootPath + SEP + subfolderName;
        File   target        = new File(subfolderPath, id + ".xml");
        if(target.exists()){
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return Optional.of(objectMapper.readValue(new FileInputStream(target), ContentMeta.class));
            }
            catch (Exception e){
                log.warn("Could not load "+target.getAbsolutePath(), e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public ContentMeta writeContentStream(ContentMeta metadata, FileInputStream inputStream) throws IOException {
        Long   id            = metadata.getId();
        String targetName    = generateTargetName(id);
        String subfolderName = getSubFolderName(targetName);
        String subfolderPath = dataRootPath + SEP + subfolderName;
        File   subfolder     = new File(subfolderPath);

        boolean result = subfolder.mkdirs();
        log.debug("created subfolder {}: {}", subfolderPath, result);
        String contentPath  = subfolderPath + SEP + targetName;
        Path   contentFile  = Paths.get(subfolderPath, targetName);
        long   bytesWritten = Files.copy(inputStream, contentFile);

        // we could just update the existing metadata, but that's bad style.
        ContentMeta lightMeta = new ContentMeta();
        lightMeta.setContentSize(bytesWritten);
        lightMeta.setContentPath(subfolderName + SEP + targetName);

        // calculate hash:
        String sha256Hex = DigestUtils.sha256Hex(new FileInputStream(contentFile.toFile()));
        lightMeta.setContentHash(sha256Hex);

        log.info("Stored new content @ {}", contentFile.toAbsolutePath());

        return lightMeta;
    }

    private static String getSubFolderName(String f) {
        return f.substring(0, 2) + SEP + f.substring(2, 4) + SEP + f.substring(4, 6);
    }

    private String generateTargetName(long id) {
        return String.format("%02d%02d%02d%d", id / 97, id / 89, id / 83, id);
    }

    public static void main(String[] args) {
        new FileSystemContentProvider().generateTargetName(13);
    }
}
