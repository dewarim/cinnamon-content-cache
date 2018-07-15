package com.dewarim.cinnamon.provider;

import com.dewarim.cinnamon.application.CinnamonCacheServer;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

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
        dataRootPath = CinnamonCacheServer.config.getServerConfig().getDataRoot();
    }


    public InputStream getContentStream(ContentMeta metadata) throws IOException {
        Long   id               = metadata.getId();
        String subfolderName    = getSubFolderName(id);
        String subfolderPath    = dataRootPath + SEP + subfolderName;
        File   target           = new File(subfolderPath, id.toString());
        return new FileInputStream(target);
    }

    public Optional<ContentMeta> getContentMeta(long id) {
        String subfolderName = getSubFolderName(id);
        String subfolderPath = dataRootPath + SEP + subfolderName;
        File   target        = new File(subfolderPath, id + ".xml");
        if (target.exists()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return Optional.of(objectMapper.readValue(new FileInputStream(target), ContentMeta.class));
            } catch (Exception e) {
                log.warn("Could not load " + target.getAbsolutePath(), e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public ContentMeta writeContentStream(ContentMeta metadata, InputStream inputStream) throws IOException {
        Long   id               = metadata.getId();
        String subfolderName    = getSubFolderName(id);
        String subfolderPath    = dataRootPath + SEP + subfolderName;
        File   subfolder        = new File(subfolderPath);

        boolean result = subfolder.mkdirs();
        log.debug("created subfolder {}: {}", subfolderPath, result);
        String contentPath  = subfolderPath + SEP + id ;
        Path   contentFile  = Paths.get(subfolderPath, id.toString());
        long   bytesWritten = Files.copy(inputStream, contentFile, StandardCopyOption.REPLACE_EXISTING);

        ContentMeta lightMeta = new ContentMeta();
        lightMeta.setContentSize(bytesWritten);
        lightMeta.setContentPath(contentPath);
        lightMeta.setName(metadata.getName());
        lightMeta.setId(id);

        // calculate hash:
        String sha256Hex = DigestUtils.sha256Hex(new FileInputStream(contentFile.toFile()));
        lightMeta.setContentHash(sha256Hex);

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(contentPath + ".xml"), lightMeta);

        log.info("Stored new content @ {}", contentFile.toAbsolutePath());
        return lightMeta;
    }

    private static String getSubFolderName(Long id) {
        String f = String.format("%02d%02d%02d", id % 97, id % 89, id % 83);
        return f.substring(0, 2) + SEP + f.substring(2, 4) + SEP + f.substring(4, 6);
    }

}
